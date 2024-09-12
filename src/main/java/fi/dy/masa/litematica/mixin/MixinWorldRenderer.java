package fi.dy.masa.litematica.mixin;

import java.util.List;
import com.llamalad7.mixinextras.sugar.Local;
import org.joml.Matrix4f;

import net.minecraft.class_10209;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer
{
    @Shadow private net.minecraft.client.world.ClientWorld world;
    @Shadow @Final private MinecraftClient client;
    @Unique private Matrix4f posMatrix = null;
    @Unique private RenderTickCounter ticks = null;
    @Unique private Profiler profiler;

    @Inject(method = "reload()V", at = @At("RETURN"))
    private void onLoadRenderers(CallbackInfo ci)
    {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.world != null && this.world == this.client.world)
        {
            if (this.profiler == null)
            {
                this.profiler = class_10209.method_64146();
            }

            LitematicaRenderer.getInstance().loadRenderers(this.profiler);
            SchematicWorldRefresher.INSTANCE.updateAll();
        }
    }

    @Inject(method = "setupTerrain", at = @At("TAIL"))
    private void onPostSetupTerrain(
            Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator, CallbackInfo ci,
            @Local Profiler profiler)
    {
        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(frustum, profiler);
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;renderMain(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/Frustum;Lnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/render/Fog;ZZLnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/util/profiler/Profiler;)V",
                    shift = At.Shift.BEFORE))
    private void onPreRenderMain(ObjectAllocator objectAllocator, RenderTickCounter tickCounter, boolean bl,
                                 Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager,
                                 Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci,
                                 @Local Profiler profiler)
    {
        this.posMatrix = positionMatrix;
        this.ticks = tickCounter;
        this.profiler = profiler;
    }

    @Inject(method = "renderLayer", at = @At("TAIL"))
    private void onRenderLayer(RenderLayer renderLayer, double x, double y, double z,
                               Matrix4f viewMatrix, Matrix4f posMatrix, CallbackInfo ci)
    {
        if (this.profiler == null)
        {
            this.profiler = class_10209.method_64146();
        }
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
            LitematicaRenderer.getInstance().piecewiseRenderOverlay(viewMatrix, posMatrix, this.profiler);
        }
    }

    @Inject(method = "renderEntities",
            at = @At(value = "RETURN"))
    private void onPostRenderEntities(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Camera camera, RenderTickCounter tickCounter, List<Entity> entities, CallbackInfo ci)
    {
        if (this.posMatrix != null &&
            this.ticks != null)
        {
            LitematicaRenderer.getInstance().piecewiseRenderEntities(this.posMatrix, this.ticks.getTickDelta(false), this.profiler);
            this.posMatrix = null;
            this.ticks = null;
            this.profiler = null;
        }
    }

    /*
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderWorldLast(
            net.minecraft.client.util.math.MatrixStack matrices,
            float tickDelta, long limitTime, boolean renderBlockOutline,
            net.minecraft.client.render.Camera camera,
            net.minecraft.client.render.GameRenderer gameRenderer,
            net.minecraft.client.render.LightmapTextureManager lightmapTextureManager,
            net.minecraft.client.util.math.Matrix4f matrix4f,
            CallbackInfo ci)
    {
        boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();

        if (Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert &&
            Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() == false)
        {
            LitematicaRenderer.getInstance().renderSchematicWorld(matrices, matrix4f, tickDelta);
        }
    }
    */
}
