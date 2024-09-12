package fi.dy.masa.litematica.event;

import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LayeredDrawer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicManager;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.render.infohud.ToolHud;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.util.GuiUtils;

public class RenderHandler implements IRenderer
{
    @Override
    public void onRenderWorldPreWeather(Matrix4f posMatrix, Matrix4f projMatrix, Frustum frustum, Camera camera, Fog fog, Profiler profiler)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            profiler.push(this.getProfilerSectionSupplier()+"_overlay_boxes");
            OverlayRenderer.getInstance().renderBoxes(posMatrix);

            if (Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED.getBooleanValue())
            {
                profiler.swap(this.getProfilerSectionSupplier()+"_overlay_mismatches");
                OverlayRenderer.getInstance().renderSchematicVerifierMismatches(posMatrix);
            }

            if (DataManager.getToolMode() == ToolMode.REBUILD)
            {
                profiler.swap(this.getProfilerSectionSupplier()+"_overlay_targeting");
                OverlayRenderer.getInstance().renderSchematicRebuildTargetingOverlay(posMatrix);
            }

            profiler.pop();
        }
    }

    @Override
    public void onRenderGameOverlayPostAdvanced(DrawContext drawContext, float partialTicks, LayeredDrawer layeredDrawer, Profiler profiler, MinecraftClient mc)
    {
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            profiler.push(this.getProfilerSectionSupplier()+"_overlay_hud");
            // The Info HUD renderers can decide if they want to be rendered in GUIs
            InfoHud.getInstance().renderHud(drawContext);

            if (GuiUtils.getCurrentScreen() == null)
            {
                if (mc.options.hudHidden == false)
                {
                    ToolHud.getInstance().renderHud(drawContext);
                    profiler.swap(this.getProfilerSectionSupplier()+"_overlay_hoverinfo");
                    OverlayRenderer.getInstance().renderHoverInfo(mc, drawContext);
                }

                if (GuiSchematicManager.hasPendingPreviewTask())
                {
                    profiler.swap(this.getProfilerSectionSupplier()+"_overlay_previewframe");
                    OverlayRenderer.getInstance().renderPreviewFrame(mc);
                }
            }

            profiler.pop();
        }
    }
}
