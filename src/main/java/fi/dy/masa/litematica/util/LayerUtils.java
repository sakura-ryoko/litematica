package fi.dy.masa.litematica.util;

import net.minecraft.client.Minecraft;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;

public class LayerUtils
{
    private static final long LAYER_REPEAT_DELAY_MS = 400;
    private static final long LAYER_REPEAT_INTERVAL_MS = 50;

    private static int layerRepeatDir = 0;
    private static long layerRepeatStartTime;
    private static long layerLastRepeatTime;

    public static void onClientTick(Minecraft mc)
    {
        if (Configs.Generic.LAYER_MODE_DYNAMIC.getBooleanValue())
        {
            DataManager.getRenderLayerRange().setSingleBoundaryToPosition(EntityUtils.getCameraEntity());
        }
        else if (mc.screen == null)
        {
            layerChangeRepeatTick();
        }
    }

    public static void layerChangeRepeatTick()
    {
        boolean nextHeld = Hotkeys.LAYER_NEXT.getKeybind().isKeybindHeld();
        boolean prevHeld = Hotkeys.LAYER_PREVIOUS.getKeybind().isKeybindHeld();
        int currentDir = nextHeld ? 1 : (prevHeld ? -1 : 0);
        long now = System.currentTimeMillis();

        if (currentDir != 0)
        {
            if (layerRepeatDir != currentDir)
            {
                layerRepeatDir = currentDir;
                layerRepeatStartTime = now;
                layerLastRepeatTime = now;
            }
            else if (now - layerRepeatStartTime >= LAYER_REPEAT_DELAY_MS &&
                     now - layerLastRepeatTime >= LAYER_REPEAT_INTERVAL_MS)
            {
                DataManager.getRenderLayerRange().moveLayer(currentDir);
                layerLastRepeatTime = now;
            }
        }
        else
        {
            layerRepeatDir = 0;
        }
    }
}
