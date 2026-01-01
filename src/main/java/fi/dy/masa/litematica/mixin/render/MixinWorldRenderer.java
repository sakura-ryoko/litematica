package fi.dy.masa.litematica.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.ProfilerSystem;
import net.minecraft.util.profiler.Profilers;
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

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow private net.minecraft.client.world.ClientWorld world;
    @Shadow @Final private MinecraftClient client;
	@Shadow @Final private OrderedRenderCommandQueueImpl entityRenderCommandQueue;
    @Shadow private @Nullable Frustum capturedFrustum;
    @Unique private Profiler profiler;

    @Unique
    private void litematica$prepareProfiler()
    {
        if (this.profiler == null)
        {
            this.profiler = Profilers.get();
        }
        if (this.profiler instanceof ProfilerSystem ps && !((IMixinProfilerSystem) ps).litematica_isStarted())
        {
            this.profiler.startTick();
        }
    }

    @Inject(method = "reload()V", at = @At("RETURN"))
    private void litematica_onLoadRenderers(CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.world != null && this.world == this.client.world)
        {
            this.litematica$prepareProfiler();
            LitematicaRenderer.getInstance().loadRenderers(this.profiler);
            SchematicWorldRefresher.INSTANCE.updateAll();
        }
    }

	// cullTerrain -> method_74752
    @Inject(method = "method_74752", at = @At("TAIL"))
    private void litematica_onPostSetupTerrain(Camera camera, Frustum frustum, boolean bl, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewisePrepare(frustum, this.profiler);
    }

    @Inject(method = "updateChunks",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/util/profiler/Profiler;pop()V",
                     ordinal = 1)
    )
    private void litematica_onPostUpdateChunks(Camera camera, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewiseUpdate(camera, this.profiler);

        if (IrisCompat.hasSodium())
        {
            LitematicaRenderer.getInstance().scheduleTranslucentSorting(camera.getPos(), this.profiler);
        }
    }

    // Sodium Compat issue
    @Inject(method = "translucencySort", at = @At("TAIL"))
    private void litematica_onScheduleTranslucentSort(Vec3d cameraPos, CallbackInfo ci)
    {
        if (!IrisCompat.hasSodium())
        {
            this.litematica$prepareProfiler();
            LitematicaRenderer.getInstance().scheduleTranslucentSorting(cameraPos, this.profiler);
        }
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;renderMain(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/Frustum;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;ZLnet/minecraft/client/render/state/WorldRenderState;Lnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/util/profiler/Profiler;)V",
                    shift = At.Shift.BEFORE))
    private void litematica_onPreRenderMain(ObjectAllocator allocator, RenderTickCounter tickCounter,
                                            boolean renderBlockOutline, Camera camera, Matrix4f matrix4f,
                                            Matrix4f projectionMatrix, Matrix4f matrix4f2,
                                            GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl,
                                            CallbackInfo ci, @Local Profiler profiler)
    {
        this.profiler = profiler;
        LitematicaRenderer.getInstance().capturePreMainValues(camera, gpuBufferSlice, profiler);
    }

    @Inject(method = "renderBlockLayers", at = @At("TAIL"))
    private void litematica_onPrepareBlockLayers(Matrix4fc matrix4fc, double d, double e, double f, CallbackInfoReturnable<SectionRenderState> cir)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewisePrepareBlockLayers(matrix4fc, d, e, f, this.profiler);
    }

	// BYTECODE Lamba Mixin for Section Group rendering
	@Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/render/state/WorldRenderState;Lnet/minecraft/util/profiler/Profiler;Lorg/joml/Matrix4f;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;ZLnet/minecraft/client/render/Frustum;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;)V",
			at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/client/render/SectionRenderState;renderSection(Lnet/minecraft/client/render/BlockRenderLayerGroup;)V",
					 ordinal = 0))
		private void litematica_renderMainSection_Opaque(GpuBufferSlice gpuBufferSlice,
                                                         WorldRenderState worldRenderState, Profiler profiler,
                                                         Matrix4f matrix4f, Handle<Framebuffer> handle, Handle<Framebuffer> handle2, boolean bl,
                                                         Frustum frustum, Handle<Framebuffer> handle3, Handle<Framebuffer> handle4,
                                                         CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.OPAQUE);
	}

	@Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/render/state/WorldRenderState;Lnet/minecraft/util/profiler/Profiler;Lorg/joml/Matrix4f;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;ZLnet/minecraft/client/render/Frustum;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;)V",
			at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/client/render/SectionRenderState;renderSection(Lnet/minecraft/client/render/BlockRenderLayerGroup;)V",
					 ordinal = 1))
	private void litematica_renderMainSection_Translucent(GpuBufferSlice gpuBufferSlice,
                                                          WorldRenderState worldRenderState, Profiler profiler,
                                                          Matrix4f matrix4f, Handle<Framebuffer> handle, Handle<Framebuffer> handle2, boolean bl,
                                                          Frustum frustum, Handle<Framebuffer> handle3, Handle<Framebuffer> handle4,
                                                          CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.TRANSLUCENT);
	}

	@Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/render/state/WorldRenderState;Lnet/minecraft/util/profiler/Profiler;Lorg/joml/Matrix4f;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;ZLnet/minecraft/client/render/Frustum;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;)V",
			at = @At(value = "INVOKE",
					 target = "Lnet/minecraft/client/render/SectionRenderState;renderSection(Lnet/minecraft/client/render/BlockRenderLayerGroup;)V",
					 ordinal = 2))
	private void litematica_renderMainSection_Tripwire(GpuBufferSlice gpuBufferSlice, WorldRenderState worldRenderState,
                                                       Profiler profiler, Matrix4f matrix4f, Handle<Framebuffer> handle,
                                                       Handle<Framebuffer> handle2, boolean bl, Frustum frustum, Handle<Framebuffer> handle3,
                                                       Handle<Framebuffer> handle4, CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.TRIPWIRE);
	}

	@Inject(method = "fillEntityRenderStates", at = @At(value = "RETURN"))
    private void litematica_onPostPrepareEntities(Camera camera, Frustum frustum, RenderTickCounter tickCounter,
                                                  WorldRenderState renderStates, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewisePrepareEntities(camera, frustum, renderStates, tickCounter, this.profiler);

		// Why Sodium?
		if (IrisCompat.hasSodium())
		{
			LitematicaRenderer.getInstance().piecewisePrepareBlockEntities(camera, frustum, renderStates, tickCounter.getTickProgress(false), this.profiler);
		}
    }

	@Inject(method = "pushEntityRenders", at = @At("RETURN"))
	private void litematica_onPostRenderEntities(MatrixStack matrices, WorldRenderState worldRenderState,
                                                 OrderedRenderCommandQueue orderedRenderCommandQueue, CallbackInfo ci)
	{
        this.litematica$prepareProfiler();
		LitematicaRenderer.getInstance().piecewiseRenderEntities(matrices, worldRenderState, orderedRenderCommandQueue, this.profiler);
	}

	@Inject(method = "fillBlockEntityRenderStates", at = @At(value = "RETURN"))
    private void litematica_onPostPrepareBlockEntities(Camera camera, float tickProgress, WorldRenderState renderStates,
                                                       CallbackInfo ci)
    {
		// Why Sodium?
		if (!IrisCompat.hasSodium())
		{
			this.litematica$prepareProfiler();
			LitematicaRenderer.getInstance().piecewisePrepareBlockEntities(camera, this.capturedFrustum, renderStates, tickProgress, this.profiler);
		}
    }

    @Inject(method = "renderBlockEntities", at = @At(value = "RETURN"))
    private void litematica_onPostRenderBlockEntities(MatrixStack matrices, WorldRenderState worldRenderState,
                                                      OrderedRenderCommandQueueImpl orderedRenderCommandQueueImpl,
                                                      CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewiseRenderBlockEntities(matrices, worldRenderState, this.entityRenderCommandQueue, this.profiler);
    }
}
