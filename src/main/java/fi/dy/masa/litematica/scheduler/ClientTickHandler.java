package fi.dy.masa.litematica.scheduler;

import net.minecraft.client.MinecraftClient;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.WorldUtils;

public class ClientTickHandler implements IClientTickHandler
{
    @Override
    public void onClientTick(MinecraftClient mc)
    {
        if (mc.world != null && mc.player != null)
        {
            SelectionManager sm = DataManager.getSelectionManager();

            if (sm.hasGrabbedElement())
            {
                sm.moveGrabbedElement(mc.player);
            }

            if (mc.currentScreen == null)
            {
                /*
                if (Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue())
                {
                    EasyPlaceUtils.easyPlaceOnUseTick();
                }
                else
                {
                 */
                    WorldUtils.easyPlaceOnUseTick(mc);
                //}
            }

            if (Configs.Generic.LAYER_MODE_DYNAMIC.getBooleanValue())
            {
                DataManager.getRenderLayerRange().setSingleBoundaryToPosition(EntityUtils.getCameraEntity());
            }

            DataManager.getSchematicPlacementManager().processQueuedChunks();
            TaskScheduler.getInstanceClient().runTasks();
        }
    }
}
