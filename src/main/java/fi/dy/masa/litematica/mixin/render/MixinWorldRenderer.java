package fi.dy.masa.litematica.mixin.render;

import java.util.List;
import com.llamalad7.mixinextras.sugar.Local;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
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

import fi.dy.masa.litematica.mixin.client.IMixinProfilerSystem;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow private net.minecraft.client.world.ClientWorld world;
    @Shadow @Final private MinecraftClient client;
    @Shadow private Frustum frustum;
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

    @Inject(method = "setupTerrain", at = @At("TAIL"))
    private void litematica_onPostSetupTerrain(
            Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(frustum, this.profiler);
    }

    @Inject(method = "updateChunks",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/util/profiler/Profiler;pop()V",
                     ordinal = 1)
    )
    private void litematica_onPostUpdateChunks(Camera camera, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().scheduleTranslucentSorting(camera.getPos(), this.profiler);
    }

//    @Inject(method = "translucencySort", at = @At("TAIL"))
//    private void litematica_onScheduleTranslucentSort(Vec3d cameraPos, CallbackInfo ci)
//    {
//        this.litematica$prepareProfiler();
//        if (!SodiumCompat.hasSodium())
//        {
//            LitematicaRenderer.getInstance().scheduleTranslucentSorting(cameraPos, this.profiler);
//        }
//    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;renderMain(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/Frustum;Lnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;ZZLnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/util/profiler/Profiler;)V",
                    shift = At.Shift.BEFORE))
    private void litematica_onPreRenderMain(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline,
                                            Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix,
                                            GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci,
                                            @Local Profiler profiler)
    {
        this.profiler = profiler;
        LitematicaRenderer.getInstance().capturePreMainValues(fog, profiler);
    }

    @Inject(method = "renderBlockLayers", at = @At("TAIL"))
    private void litematica_onPrepareBlockLayers(Matrix4fc matrix4fc, double d, double e, double f, CallbackInfoReturnable<SectionRenderState> cir)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewisePrepareBlockLayers(matrix4fc, d, e, f, this.profiler);
    }

    // BYTECODE Lambda Mixin for Section Group rendering
    @Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/client/render/Camera;Lnet/minecraft/util/profiler/Profiler;Lorg/joml/Matrix4f;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;ZLnet/minecraft/client/render/Frustum;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/render/SectionRenderState;renderSection(Lnet/minecraft/client/render/BlockRenderLayerGroup;)V",
                     ordinal = 0))
    private void litematica_renderMainSection_Opaque(GpuBufferSlice gpuBufferSlice, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f,
                                                     Handle<Framebuffer> handle, Handle<Framebuffer> handle2, boolean bl,
                                                     Frustum frustum, Handle<Framebuffer> handle3, Handle<Framebuffer> handle4, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.OPAQUE);
    }

    @Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/client/render/Camera;Lnet/minecraft/util/profiler/Profiler;Lorg/joml/Matrix4f;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;ZLnet/minecraft/client/render/Frustum;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/render/SectionRenderState;renderSection(Lnet/minecraft/client/render/BlockRenderLayerGroup;)V",
                     ordinal = 1))
    private void litematica_renderMainSection_Translucent(GpuBufferSlice gpuBufferSlice, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f,
                                                          Handle<Framebuffer> handle, Handle<Framebuffer> handle2, boolean bl,
                                                          Frustum frustum, Handle<Framebuffer> handle3, Handle<Framebuffer> handle4, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.TRANSLUCENT);
    }

    @Inject(method = "method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/client/render/Camera;Lnet/minecraft/util/profiler/Profiler;Lorg/joml/Matrix4f;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;ZLnet/minecraft/client/render/Frustum;Lnet/minecraft/client/util/Handle;Lnet/minecraft/client/util/Handle;)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/render/SectionRenderState;renderSection(Lnet/minecraft/client/render/BlockRenderLayerGroup;)V",
                     ordinal = 2))
    private void litematica_renderMainSection_Tripwire(GpuBufferSlice gpuBufferSlice, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f,
                                                       Handle<Framebuffer> handle, Handle<Framebuffer> handle2, boolean bl,
                                                       Frustum frustum, Handle<Framebuffer> handle3, Handle<Framebuffer> handle4, CallbackInfo ci)
    {
        LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup.TRIPWIRE);
    }

    @Inject(method = "renderEntities",
            at = @At(value = "RETURN"))
    private void litematica_onPostRenderEntities(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Camera camera, RenderTickCounter tickCounter, List<Entity> entities, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewiseRenderEntities(matrices, immediate, tickCounter.getTickProgress(false), this.profiler);
    }

    @Inject(method = "renderBlockEntities",
            at = @At(value = "RETURN"))
    private void litematica_onPostRenderBlockEntities(MatrixStack matrices, VertexConsumerProvider.Immediate entityVertexConsumers, VertexConsumerProvider.Immediate effectVertexConsumers, Camera camera, float tickProgress, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewiseRenderBlockEntities(matrices, entityVertexConsumers, effectVertexConsumers, tickProgress, this.profiler);
    }
}
