package fi.dy.masa.litematica.scheduler;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import net.minecraft.client.Minecraft;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.EasyPlaceUtils;
import fi.dy.masa.litematica.util.LayerUtils;
import fi.dy.masa.litematica.util.WorldUtils;

public class ClientTickHandler implements IClientTickHandler
{
    @Override
    public void onClientTick(Minecraft mc)
    {
        if (mc.level != null && mc.player != null)
        {
            SelectionManager sm = DataManager.getSelectionManager();

            if (sm.hasGrabbedElement())
            {
                sm.moveGrabbedElement(mc.player);
            }

            if (mc.screen == null)
            {
                if (Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue())
                {
                    EasyPlaceUtils.easyPlaceOnUseTick();
                }
                else
                {
                    WorldUtils.easyPlaceOnUseTick(mc);
                }
            }

            LayerUtils.onClientTick(mc);
            DataManager.getSchematicPlacementManager().onClientTick(mc);
            TaskScheduler.getInstanceClient().runTasks();
        }
    }
}
