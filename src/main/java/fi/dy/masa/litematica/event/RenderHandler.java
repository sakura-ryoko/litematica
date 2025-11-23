package fi.dy.masa.litematica.event;

import org.joml.Matrix4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicManager;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.render.infohud.ToolHud;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;

public class RenderHandler implements IRenderer
{
    protected int oopsCount;

    @Override
    public void onRenderWorldLast(MatrixStack matrices, Matrix4f projMatrix)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            OverlayRenderer.getInstance().renderBoxes(matrices);

            if (Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED.getBooleanValue())
            {
                OverlayRenderer.getInstance().renderSchematicVerifierMismatches(matrices);
            }

            if (DataManager.getToolMode() == ToolMode.REBUILD)
            {
                OverlayRenderer.getInstance().renderSchematicRebuildTargetingOverlay(matrices);
            }
        }
    }

    @Override
    public void onRenderGameOverlayPost(DrawContext drawContext)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() == false || mc.player == null)
        {
            return;
        }

        // The Info HUD renderers can decide if they want to be rendered in GUIs
        InfoHud.getInstance().renderHud(drawContext);

        if (GuiUtils.getCurrentScreen() != null)
        {
            return;
        }

        try
        {
            ToolHud.getInstance().renderHud(drawContext);
            OverlayRenderer.getInstance().renderHoverInfo(mc, drawContext);
        }
        catch (Exception e)
        {
            if (this.oopsCount < 10)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Exception from Info Overlay render: " + e.getMessage());
                Litematica.logger.error("Exception from Info Overlay render", e);
                ++this.oopsCount;
            }
        }

        if (GuiSchematicManager.hasPendingPreviewTask())
        {
            OverlayRenderer.getInstance().renderPreviewFrame(mc);
        }
    }
}
