package fi.dy.masa.litematica.mixin.render;

import java.util.List;
import com.llamalad7.mixinextras.sugar.Local;
import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
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

import fi.dy.masa.litematica.mixin.client.IMixinProfilerSystem;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow private net.minecraft.client.world.ClientWorld world;
    @Shadow @Final private MinecraftClient client;
//    @Unique private Matrix4f posMatrix = null;
//    @Unique private RenderTickCounter ticks = null;
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

    // We can't grab the local Profiler here because of Sodium.
    @Inject(method = "setupTerrain", at = @At("TAIL"))
    private void litematica_onPostSetupTerrain(
            Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewisePrepare(frustum, this.profiler);
    }

    @Inject(method = "updateChunks", at = @At("TAIL"))
    private void litematica_onPostSetupChunks(Camera camera, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewiseUpdate(camera, this.profiler);
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;renderMain(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/Frustum;Lnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/render/Fog;ZZLnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/util/profiler/Profiler;)V",
                    shift = At.Shift.BEFORE))
    private void litematica_onPreRenderMain(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline,
                                 Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f matrix4f2, CallbackInfo ci,
                                 @Local Profiler profiler)
    {
//        this.posMatrix = positionMatrix;
//        this.ticks = tickCounter;
        this.profiler = profiler;
    }

    @Inject(method = "renderLayer", at = @At("TAIL"))
    private void litematica_onRenderLayer(RenderLayer renderLayer, double x, double y, double z,
                                          Matrix4f viewMatrix, Matrix4f posMatrix, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();

        if (renderLayer == RenderLayer.getSolid())
        {
            LitematicaRenderer.getInstance().piecewiseRenderSolid(viewMatrix, posMatrix, this.profiler);
        }
        else if (renderLayer == RenderLayer.getCutoutMipped())
        {
            LitematicaRenderer.getInstance().piecewiseRenderCutoutMipped(viewMatrix, posMatrix, this.profiler);
        }
        else if (renderLayer == RenderLayer.getCutout())
        {
            LitematicaRenderer.getInstance().piecewiseRenderCutout(viewMatrix, posMatrix, this.profiler);
        }
        else if (renderLayer == RenderLayer.getTranslucent())
        {
            LitematicaRenderer.getInstance().piecewiseRenderTranslucent(viewMatrix, posMatrix, this.profiler);
        }
        else if (renderLayer == RenderLayer.getTripwire())
        {
            LitematicaRenderer.getInstance().piecewiseRenderTripwire(viewMatrix, posMatrix, this.profiler);
            LitematicaRenderer.getInstance().piecewiseRenderOverlay(viewMatrix, posMatrix, this.profiler);
        }
    }

    @Inject(method = "renderEntities",
            at = @At(value = "RETURN"))
    private void litematica_onPostRenderEntities(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Camera camera, RenderTickCounter tickCounter, List<Entity> entities, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewiseRenderEntities(matrices, immediate, tickCounter.getTickDelta(false), this.profiler);
    }

    @Inject(method = "renderBlockEntities",
            at = @At(value = "RETURN"))
    private void litematica_onPostRenderBlockEntities(MatrixStack matrices, VertexConsumerProvider.Immediate entityVertexConsumers, VertexConsumerProvider.Immediate effectVertexConsumers, Camera camera, float tickDelta, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();
        LitematicaRenderer.getInstance().piecewiseRenderBlockEntities(matrices, entityVertexConsumers, effectVertexConsumers, tickDelta, this.profiler);
    }
}
