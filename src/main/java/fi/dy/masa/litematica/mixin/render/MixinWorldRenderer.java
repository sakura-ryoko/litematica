package fi.dy.masa.litematica.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.ProfilerSystem;
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

@Mixin(net.minecraft.client.render.WorldRenderer.class)
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
            this.profiler = this.client.getProfiler();
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

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/BackgroundRenderer;applyFog(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/BackgroundRenderer$FogType;FZF)V",
                     shift = At.Shift.AFTER,
                     ordinal = 0))
    private void litematica_onPreRenderMain(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera,
                                            GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager,
                                            Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci,
                                            @Local Profiler profiler)
    {
//        this.posMatrix = positionMatrix;
//        this.ticks = tickCounter;
        this.profiler = profiler;
    }

    @Inject(method = "renderLayer", at = @At("TAIL"))
    private void litematica_onRenderLayer(RenderLayer renderLayer, double x, double y, double z, Matrix4f matrix4f, Matrix4f projMatrix, CallbackInfo ci)
    {
        this.litematica$prepareProfiler();

        if (renderLayer == RenderLayer.getSolid())
        {
            LitematicaRenderer.getInstance().piecewiseRenderSolid(matrix4f, projMatrix, this.profiler);
        }
        else if (renderLayer == RenderLayer.getCutoutMipped())
        {
            LitematicaRenderer.getInstance().piecewiseRenderCutoutMipped(matrix4f, projMatrix, this.profiler);
        }
        else if (renderLayer == RenderLayer.getCutout())
        {
            LitematicaRenderer.getInstance().piecewiseRenderCutout(matrix4f, projMatrix, this.profiler);
        }
        else if (renderLayer == RenderLayer.getTranslucent())
        {
            LitematicaRenderer.getInstance().piecewiseRenderTranslucent(matrix4f, projMatrix, this.profiler);
            LitematicaRenderer.getInstance().piecewiseRenderOverlay(matrix4f, projMatrix, this.profiler);
        }
        else if (renderLayer == RenderLayer.getTripwire())
        {
            LitematicaRenderer.getInstance().piecewiseRenderTripwire(matrix4f, projMatrix, this.profiler);
        }
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE_STRING", args = "ldc=blockentities",
                     target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V"))
//            at = @At(value = "INVOKE",
//                     target = "Lnet/minecraft/client/render/WorldRenderer;checkEmpty(Lnet/minecraft/client/util/math/MatrixStack;)V",
//                     shift = At.Shift.AFTER,
//                     ordinal = 0))
    private void litematica_onPostRenderEntities(RenderTickCounter tickCounter, boolean renderBlockOutline,
                                                 Camera camera, GameRenderer gameRenderer,
                                                 LightmapTextureManager lightmapTextureManager,
                                                 Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci,
                                                 @Local MatrixStack matrixStack,
                                                 @Local VertexConsumerProvider.Immediate immediate,
                                                 @Local Profiler profiler)
//    @Inject(method = "renderEntities",
//            at = @At(value = "RETURN"))
//    private void litematica_onPostRenderEntities(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Camera camera, RenderTickCounter tickCounter, List<Entity> entities, CallbackInfo ci)
    {
        this.profiler = profiler;
        LitematicaRenderer.getInstance().piecewiseRenderEntities(matrixStack, immediate, tickCounter.getTickDelta(false), this.profiler);
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/render/WorldRenderer;checkEmpty(Lnet/minecraft/client/util/math/MatrixStack;)V",
                     shift = At.Shift.AFTER,
                     ordinal = 1))
    private void litematica_onPostRenderBlockEntities(RenderTickCounter tickCounter, boolean renderBlockOutline,
                                                      Camera camera, GameRenderer gameRenderer,
                                                      LightmapTextureManager lightmapTextureManager,
                                                      Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci,
                                                      @Local MatrixStack matrixStack,
                                                      @Local VertexConsumerProvider.Immediate immediate,
                                                      @Local Profiler profiler)
    {
        this.profiler = profiler;
        LitematicaRenderer.getInstance().piecewiseRenderBlockEntities(matrixStack, immediate, immediate, tickCounter.getTickDelta(false), this.profiler);
    }
}
