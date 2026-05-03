package fi.dy.masa.litematica.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.litematica.compat.iris.IrisRenderingFix;
import fi.dy.masa.litematica.mixin.client.IMixinProfilerSystem;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class MixinLevelRenderer
{
	@Shadow @Final private SubmitNodeStorage submitNodeStorage;
	@Shadow private @Nullable GpuSampler chunkLayerSampler;
	@Shadow @Final private LevelRenderState levelRenderState;
	@Unique private ProfilerFiller profiler;

    @Unique
    private void litematica$prepareProfiler()
    {
        if (this.profiler == null)
        {
            this.profiler = Profiler.get();
        }
        if (this.profiler instanceof ActiveProfiler ps && !((IMixinProfilerSystem) ps).litematica_isStarted())
        {
            this.profiler.startTick();
        }
    }

    @Inject(method = "invalidateCompiledGeometry", at = @At("RETURN"))
    private void litematica_onLoadRenderers(ClientLevel level, Options options, Camera camera, BlockColors blockColors, CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (level != null && level == Minecraft.getInstance().level)
        {
            this.litematica$prepareProfiler();
            LitematicaRenderer.getInstance().loadRenderers(this.profiler);
            SchematicWorldRefresher.INSTANCE.updateAll();
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void litematica_onRenderHead(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline,
                                         CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog,
                                         Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci)
    {
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        Frustum frustum = cameraState.cullFrustum;

        this.litematica$prepareProfiler();
        LitematicaRenderer renderer = LitematicaRenderer.getInstance();
        renderer.piecewisePrepare(frustum, this.profiler);
        renderer.piecewisePrepareEntities(camera, frustum, this.levelRenderState, deltaTracker, this.profiler);
        renderer.piecewisePrepareBlockEntities(camera, this.levelRenderState, deltaTracker.getGameTimeDeltaPartialTick(true), this.profiler);

	    if (IrisCompat.isShaderActive())
	    {
		    IrisRenderingFix.INSTANCE.setCamera(camera);
		    IrisRenderingFix.INSTANCE.extractAndCompileSectionsWithShadersOn();
	    }
    }

    @Inject(method = "compileSections",
            at = @At("TAIL")
    )
    private void litematica_onPostUpdateChunks(CameraRenderState cameraState, CallbackInfo ci)
    {
		if (IrisCompat.isShaderActive()) { return; }
        this.litematica$prepareProfiler();
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
	    LitematicaRenderer.getInstance().piecewiseUpdate(camera, this.profiler);

		if (IrisCompat.hasSodium())
		{
			LitematicaRenderer.getInstance().scheduleTranslucentSorting(camera.position(), this.profiler);
		}
    }

    @Inject(method = "scheduleTranslucentSectionResort", at = @At("TAIL"))
    private void litematica_onScheduleTranslucentSort(Vec3 cameraPos, CallbackInfo ci)
    {
        if (!IrisCompat.hasSodium())
        {
	        this.litematica$prepareProfiler();
            LitematicaRenderer.getInstance().scheduleTranslucentSorting(cameraPos, this.profiler);
        }
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V",
                    shift = At.Shift.BEFORE))
    private void litematica_onPreRenderMain(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline,
                                             CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor,
                                             boolean shouldRenderSky, CallbackInfo ci,
                                             @Local(ordinal = 0) ProfilerFiller profiler)
    {
        this.profiler = profiler;
		if (IrisCompat.isShaderActive()) { return; }
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        LitematicaRenderer renderer = LitematicaRenderer.getInstance();
        renderer.capturePreMainValues(cameraState, terrainFog, profiler);
        renderer.uploadRemainingBuffers(camera, deltaTracker, profiler);
        renderer.piecewisePrepareBlockLayers(modelViewMatrix, profiler);
    }

	// BYTECODE (Virtual Method) Mixin for Section Group rendering
	@Inject(method = "lambda$addMainPass$0(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;Lcom/mojang/blaze3d/resource/ResourceHandle;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;)V",
	        at = @At(value = "INVOKE",
	                 target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
	                 ordinal = 0,
	                 shift = At.Shift.AFTER))
	private void litematica_renderMainSection_Opaque(GpuBufferSlice terrainFog, LevelRenderState levelRenderState, ProfilerFiller profiler,
	                                                 ChunkSectionsToRender chunkSectionsToRender, ResourceHandle<RenderTarget> entityOutlineTarget,
	                                                 FeatureRenderDispatcher.PreparedFrame preparedFrame,
	                                                 ResourceHandle<RenderTarget> translucentTarget, ResourceHandle<RenderTarget> mainTarget,
	                                                 ResourceHandle<RenderTarget> itemEntityTarget, ResourceHandle<RenderTarget> particleTarget,
	                                                 CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(ChunkSectionLayerGroup.OPAQUE, this.chunkLayerSampler);
	}

	@Inject(method = "lambda$addMainPass$0(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;Lcom/mojang/blaze3d/resource/ResourceHandle;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;)V",
			at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
					 ordinal = 1,
					 shift = At.Shift.AFTER))
	private void litematica_renderMainSection_Translucent(GpuBufferSlice terrainFog, LevelRenderState levelRenderState, ProfilerFiller profiler,
	                                                      ChunkSectionsToRender chunkSectionsToRender, ResourceHandle<RenderTarget> entityOutlineTarget,
	                                                      FeatureRenderDispatcher.PreparedFrame preparedFrame,
	                                                      ResourceHandle<RenderTarget> translucentTarget, ResourceHandle<RenderTarget> mainTarget,
	                                                      ResourceHandle<RenderTarget> itemEntityTarget, ResourceHandle<RenderTarget> particleTarget,
	                                                      CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(ChunkSectionLayerGroup.TRANSLUCENT, this.chunkLayerSampler);
	}

//	@Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/LevelRenderState;Lnet/minecraft/util/profiling/ProfilerFiller;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;ZLcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;)V",
//			at = @At(value = "INVOKE",
//					 target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
//					 ordinal = 2,
//					 shift = At.Shift.AFTER))
//	private void litematica_renderMainSection_Tripwire(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, ProfilerFiller profiler,
//	                                                   Matrix4f matrix4f, ResourceHandle<RenderTarget> handle, ResourceHandle<RenderTarget> handle2, boolean bl,
//	                                                   ResourceHandle<RenderTarget> handle3, ResourceHandle<RenderTarget> handle4, CallbackInfo ci)
//	{
//		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(ChunkSectionLayerGroup.TRIPWIRE, this.chunkLayerSampler);
//	}

	@Inject(method = "submitEntities", at = @At("RETURN"))
	private void litematica_onPostRenderEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo ci)
	{
        this.litematica$prepareProfiler();
		LitematicaRenderer.getInstance().piecewiseRenderEntities(poseStack, levelRenderState, output, this.profiler);
	}

    @Inject(method = "submitBlockEntities", at = @At(value = "RETURN"))
    private void litematica_onPostRenderBlockEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewiseRenderBlockEntities(poseStack, levelRenderState, this.submitNodeStorage, this.profiler);
    }

	@Inject(method = "endFrame", at = @At("TAIL"))
	private void litematica_onEndFrame(CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().onEndFrame();
	}

	@Inject(method = "close", at = @At("TAIL"))
	private void litematica_onClose(CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().onClose();
	}
}
