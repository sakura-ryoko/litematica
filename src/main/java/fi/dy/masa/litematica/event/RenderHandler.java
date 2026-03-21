package fi.dy.masa.litematica.event;

import java.util.function.Supplier;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.ProfilerFiller;

import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicManager;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.render.infohud.ToolHud;
import fi.dy.masa.litematica.tool.ToolMode;

public class RenderHandler implements IRenderer
{
    @Override
    public void onExtractWorldLast(DeltaTracker deltaTracker, Camera camera, float ticks, ProfilerFiller profiler)
    {
        // TODO
    }

    @Override
    public void onRenderWorldLast(RenderTarget fb, Matrix4fc modelViewMatrix, CameraRenderState cameraState, Frustum culling, RenderBuffers buffers, GpuBufferSlice terrainFog, Vector4f fogColor, ProfilerFiller profiler)
    {
        Minecraft mc = Minecraft.getInstance();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            profiler.push("overlay_boxes");
            OverlayRenderer.getInstance().renderBoxes(profiler);

            if (Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED.getBooleanValue())
            {
                profiler.popPush("overlay_mismatches");
                OverlayRenderer.getInstance().renderSchematicVerifierMismatches(profiler);
            }

            if (DataManager.getToolMode() == ToolMode.REBUILD)
            {
                profiler.popPush("overlay_targeting");
                OverlayRenderer.getInstance().renderSchematicRebuildTargetingOverlay(profiler);
            }

            // Schematic Overlay Rendering
            profiler.popPush("schematic_overlay");
            LitematicaRenderer.getInstance().piecewiseRenderOverlay(profiler);
            profiler.pop();
        }
    }

    @Override
    public Supplier<String> getProfilerSectionSupplier()
    {
        return () -> Reference.MOD_ID+"_render_handler";
    }

    @Override
    public void onExtractGuiOverlayPost(GuiContext ctx, float partialTicks, ProfilerFiller profiler)
    {
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && ctx.mc().player != null)
        {
            profiler.push("overlay_hud");
            // The Info HUD renderers can decide if they want to be rendered in GUIs
            InfoHud.getInstance().renderHud(ctx);

            if (GuiUtils.getCurrentScreen() == null)
            {
                if (ctx.mc().options.hideGui == false)
                {
                    ToolHud.getInstance().renderHud(ctx);
                    profiler.popPush("overlay_hover_info");
                    OverlayRenderer.getInstance().renderHoverInfo(ctx, profiler);
                }

                if (GuiSchematicManager.hasPendingPreviewTask())
                {
                    profiler.popPush("overlay_preview_frame");
                    OverlayRenderer.getInstance().renderPreviewFrame(ctx, profiler);
                }
            }

            profiler.pop();
        }
    }
}
