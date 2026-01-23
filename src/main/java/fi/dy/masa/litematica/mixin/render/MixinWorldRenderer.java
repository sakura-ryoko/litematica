package fi.dy.masa.litematica.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
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
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.litematica.mixin.client.IMixinProfilerSystem;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

@Mixin(LevelRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow private net.minecraft.client.multiplayer.ClientLevel level;
    @Shadow @Final private Minecraft minecraft;
	@Shadow @Final private SubmitNodeStorage submitNodeStorage;
    @Shadow private @Nullable Frustum capturedFrustum;
	@Shadow
	private @org.jspecify.annotations.Nullable GpuSampler chunkLayerSampler;
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

    @Inject(method = "allChanged()V", at = @At("RETURN"))
    private void litematica_onLoadRenderers(CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.level != null && this.level == this.minecraft.level)
        {
            this.litematica$prepareProfiler();
            LitematicaRenderer.getInstance().loadRenderers(this.profiler);
            SchematicWorldRefresher.INSTANCE.updateAll();
        }
    }

	// cullTerrain -> method_74752 (updateCamera)
    @Inject(method = "cullTerrain", at = @At("TAIL"))
    private void litematica_onPostSetupTerrain(
            Camera camera, Frustum frustum, boolean bl, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewisePrepare(frustum, this.profiler);
    }

    @Inject(method = "compileSections",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V",
                     ordinal = 1)
    )
    private void litematica_onPostUpdateChunks(Camera camera, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
	    LitematicaRenderer.getInstance().piecewiseUpdate(camera, this.profiler);
//	    LitematicaRenderer.getInstance().scheduleChunkUploads(camera.position(), this.profiler);

		if (IrisCompat.hasSodium())
		{
			LitematicaRenderer.getInstance().scheduleTranslucentSorting(camera.position(), this.profiler);
		}
    }

    @Inject(method = "scheduleTranslucentSectionResort", at = @At("TAIL"))
    private void litematica_onScheduleTranslucentSort(Vec3 vec3, CallbackInfo ci)
    {
        if (!IrisCompat.hasSodium())
        {
	        this.litematica$prepareProfiler();
            LitematicaRenderer.getInstance().scheduleTranslucentSorting(vec3, this.profiler);
        }
    }

    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/culling/Frustum;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;ZLnet/minecraft/client/renderer/state/LevelRenderState;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
                    shift = At.Shift.BEFORE))
    private void litematica_onPreRenderMain(GraphicsResourceAllocator allocator, DeltaTracker tickCounter,
                                            boolean renderBlockOutline, Camera camera, Matrix4f matrix4f,
                                            Matrix4f projectionMatrix, Matrix4f matrix4f2,
                                            GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl,
                                            CallbackInfo ci, @Local ProfilerFiller profiler)
    {
        this.profiler = profiler;
        LitematicaRenderer.getInstance().capturePreMainValues(camera, gpuBufferSlice, profiler);
    }

    @Inject(method = "prepareChunkRenders", at = @At("TAIL"))
    private void litematica_onPrepareBlockLayers(Matrix4fc matrix4fc, double d, double e, double f, CallbackInfoReturnable<ChunkSectionsToRender> cir)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewisePrepareBlockLayers(matrix4fc, d, e, f, this.profiler);
    }

	// BYTECODE (Virtual Method) Mixin for Section Group rendering
	@Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/LevelRenderState;Lnet/minecraft/util/profiling/ProfilerFiller;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;ZLcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;)V",
	        at = @At(value = "INVOKE",
	                 target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
	                 ordinal = 0,
	                 shift = At.Shift.AFTER))
	private void litematica_renderMainSection_Opaque(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, ProfilerFiller profiler,
	                                                 Matrix4f matrix4f, ResourceHandle<RenderTarget> handle, ResourceHandle<RenderTarget> handle2, boolean bl,
	                                                 ResourceHandle<RenderTarget> handle3, ResourceHandle<RenderTarget> handle4, CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(ChunkSectionLayerGroup.OPAQUE, this.chunkLayerSampler);
	}

	@Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/LevelRenderState;Lnet/minecraft/util/profiling/ProfilerFiller;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;ZLcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;)V",
			at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
					 ordinal = 1,
					 shift = At.Shift.AFTER))
	private void litematica_renderMainSection_Translucent(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, ProfilerFiller profiler,
	                                                      Matrix4f matrix4f, ResourceHandle<RenderTarget> handle, ResourceHandle<RenderTarget> handle2, boolean bl,
	                                                      ResourceHandle<RenderTarget> handle3, ResourceHandle<RenderTarget> handle4, CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(ChunkSectionLayerGroup.TRANSLUCENT, this.chunkLayerSampler);
	}

	@Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/LevelRenderState;Lnet/minecraft/util/profiling/ProfilerFiller;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;ZLcom/mojang/blaze3d/resource/ResourceHandle;Lcom/mojang/blaze3d/resource/ResourceHandle;)V",
			at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
					 ordinal = 2,
					 shift = At.Shift.AFTER))
	private void litematica_renderMainSection_Tripwire(GpuBufferSlice gpuBufferSlice, LevelRenderState worldRenderState, ProfilerFiller profiler,
	                                                   Matrix4f matrix4f, ResourceHandle<RenderTarget> handle, ResourceHandle<RenderTarget> handle2, boolean bl,
	                                                   ResourceHandle<RenderTarget> handle3, ResourceHandle<RenderTarget> handle4, CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(ChunkSectionLayerGroup.TRIPWIRE, this.chunkLayerSampler);
	}

	@Inject(method = "extractVisibleEntities", at = @At(value = "RETURN"))
    private void litematica_onPostPrepareEntities(Camera camera, Frustum frustum, DeltaTracker tickCounter,
                                                  LevelRenderState renderStates, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewisePrepareEntities(camera, frustum, renderStates, tickCounter, this.profiler);

		// Why Sodium?
		if (IrisCompat.hasSodium())
		{
			LitematicaRenderer.getInstance().piecewisePrepareBlockEntities(camera, frustum, renderStates, tickCounter.getGameTimeDeltaPartialTick(false), this.profiler);
		}
    }

	@Inject(method = "submitEntities", at = @At("RETURN"))
	private void litematica_onPostRenderEntities(PoseStack matrices, LevelRenderState worldRenderState,
                                                 SubmitNodeCollector orderedRenderCommandQueue, CallbackInfo ci)
	{
        this.litematica$prepareProfiler();
		LitematicaRenderer.getInstance().piecewiseRenderEntities(matrices, worldRenderState, orderedRenderCommandQueue, this.profiler);
	}

	@Inject(method = "extractVisibleBlockEntities", at = @At(value = "RETURN"))
    private void litematica_onPostPrepareBlockEntities(Camera camera, float tickProgress, LevelRenderState renderStates,
                                                       CallbackInfo ci)
    {
		// Why Sodium?
		if (!IrisCompat.hasSodium())
		{
			this.litematica$prepareProfiler();
			LitematicaRenderer.getInstance().piecewisePrepareBlockEntities(camera, this.capturedFrustum, renderStates, tickProgress, this.profiler);
		}
    }

    @Inject(method = "submitBlockEntities", at = @At(value = "RETURN"))
    private void litematica_onPostRenderBlockEntities(PoseStack matrices, LevelRenderState worldRenderState,
                                                      SubmitNodeStorage orderedRenderCommandQueueImpl,
                                                      CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewiseRenderBlockEntities(matrices, worldRenderState, this.submitNodeStorage, this.profiler);
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
