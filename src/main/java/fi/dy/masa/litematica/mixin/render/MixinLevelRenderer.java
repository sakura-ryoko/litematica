package fi.dy.masa.litematica.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.mixin.client.IMixinActiveProfiler;
import fi.dy.masa.litematica.render.LitematicaRenderer;

@Mixin(value = LevelRenderer.class, priority = 600)
public abstract class MixinLevelRenderer
{
	@Shadow @Final private SubmitNodeStorage submitNodeStorage;
	@Shadow @Final private GameRenderer gameRenderer;
	@Shadow @Final private LevelRenderState levelRenderState;
	@Shadow @Final private LevelTargetBundle targets;
	@Shadow @Final private boolean multiDrawIndirectAvailable;
	@Unique private ProfilerFiller profiler;

    @Unique
    private void litematica$prepareProfiler()
    {
        if (this.profiler == null)
        {
            this.profiler = Profiler.get();
        }
        if (this.profiler instanceof ActiveProfiler ps && !((IMixinActiveProfiler) ps).litematica_isStarted())
        {
            this.profiler.startTick();
        }
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;Z)V",
                    shift = At.Shift.BEFORE))
    private void litematica_onPreRenderMain(GraphicsResourceAllocator resourceAllocator, boolean renderOutline,
                                            CameraRenderState cameraState, GpuBufferSlice terrainFog, Vector4f fogColor,
                                            boolean shouldRenderSky, boolean consistentDepthRequired, CallbackInfo ci,
                                            @Local(name = "profiler") ProfilerFiller profiler)
    {
        this.profiler = profiler;
//		if (IrisCompat.isShaderActive()) { return; }
        LitematicaRenderer.getInstance().capturePreMainValues(cameraState, terrainFog, profiler);
    }

	@Inject(method = "prepareChunkRenders", at = @At("TAIL"))
    private void litematica_onPrepareBlockLayersPost1(Matrix4fc modelViewMatrix, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> cir)
    {
	    // Why Iris?
//		if (IrisCompat.isShaderActive()) { return; }

	    if (!this.useIndirect())
	    {
		    this.litematica$prepareProfiler();
		    LitematicaRenderer.getInstance().piecewisePrepareBlockLayers(modelViewMatrix, this.profiler);
	    }
    }

	@Inject(method = "prepareChunkRendersIndirect", at = @At("TAIL"))
	private void litematica_onPrepareBlockLayersPost2(Matrix4fc modelViewMatrix, boolean respectTranslucentOrder, CallbackInfoReturnable<ChunkSectionsToRender> cir)
	{
		// Why Iris?
//		if (IrisCompat.isShaderActive()) { return; }

		if (this.useIndirect())
		{
			this.litematica$prepareProfiler();
			LitematicaRenderer.getInstance().piecewisePrepareBlockLayers(modelViewMatrix, this.profiler);
		}
	}

	@Inject(method = "executeOit", at = @At("HEAD"))
	private void litematica_renderMainSection_Oit(ChunkSectionsToRender chunkSectionsToRender, FeatureRenderDispatcher.PreparedFrame featureFrame, CallbackInfo ci)
	{
		if (this.useOIT())
		{
			LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(this.targets.main.get(), ChunkSectionLayerGroup.OPAQUE);
			LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(this.targets.main.get(), ChunkSectionLayerGroup.TRANSLUCENT);
		}
	}

	@Inject(method = "executeOutline", at = @At("HEAD"))
	private void litematica_renderMainSection_Outline1(FeatureRenderDispatcher.PreparedFrame featureFrame, CallbackInfo ci)
	{
		if (!this.useOIT())
		{
			LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(this.targets.main.get(), ChunkSectionLayerGroup.OPAQUE);
			LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(this.targets.main.get(), ChunkSectionLayerGroup.TRANSLUCENT);
		}
	}

	@Inject(method = "executeOutline", at = @At("TAIL"))
	private void litematica_renderMainSection_Outline2(FeatureRenderDispatcher.PreparedFrame featureFrame, CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseRenderOverlay(this.profiler);
	}

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

	@Unique
	private boolean useOIT()
	{
		return this.gameRenderer.useImprovedTransparency();
	}

	@Unique
	private boolean useIndirect()
	{
		return this.multiDrawIndirectAvailable && this.levelRenderState.shouldUseMultiDrawIndirectForTerrain;
	}
}
