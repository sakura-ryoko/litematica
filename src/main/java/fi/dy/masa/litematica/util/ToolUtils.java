package fi.dy.masa.litematica.util;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.util.DataOps;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntityDataManager;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.info_hud.InfoHudSync;
import fi.dy.masa.litematica.scheduler.tasks.TaskDeleteArea;
import fi.dy.masa.litematica.scheduler.tasks.TaskDeleteBlocksByPlacement;
import fi.dy.masa.litematica.scheduler.tasks.TaskFillArea;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.tool.ToolModeData;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.ICompletionListener;
import fi.dy.masa.malilib.util.InfoUtils;

public class ToolUtils
{
    public static void fillSelectionVolumes(Minecraft mc, BlockState state, @Nullable BlockState stateToReplace)
    {
        if (mc.player != null && EntityUtils.isCreativeMode(mc.player))
        {
            final AreaSelection area = DataManager.getSelectionManager().getCurrentSelection();

            if (area == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
                return;
            }

            if (area.getAllSubRegionBoxes().size() > 0)
            {
                Box currentBox = area.getSelectedSubRegionBox();
                final ImmutableList<Box> boxes = currentBox != null ? ImmutableList.of(currentBox) : ImmutableList.copyOf(area.getAllSubRegionBoxes());

                if (EntityDataManager.getInstance().hasServuxServer())
                {
                    CompoundData data = new CompoundData();
                    ListData list = new ListData();

                    data.putString("Task", "Fill");
                    data.putBoolean("RemoveEntities", false);
                    data.putInt("Interval", 1);
                    data.putCodec("FillState", BlockState.CODEC, state);

                    if (stateToReplace != null)
                    {
                        data.putCodec("ReplaceState", BlockState.CODEC, stateToReplace);
                    }

                    for (Box box : boxes)
                    {
                        list.add(Box.CODEC.encodeStart(DataOps.INSTANCE, box).getPartialOrThrow());
                    }

                    if (!list.isEmpty())
                    {
                        data.put("Boxes", list);
                        EntityDataManager.getInstance().sendServuxTaskRequest(data, new InfoHudSync(null));
                        InfoUtils.showGuiOrInGameMessage(MessageType.INFO, 2500, "litematica.message.scheduled_task_to_servux");
                    }
                    else
                    {
                        InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.empty_area_selection");
                    }
                }
                else
                {
                    final int interval = Configs.Generic.COMMAND_TASK_INTERVAL.getIntegerValue();
                    TaskFillArea task = new TaskFillArea(boxes, state, stateToReplace, false);
                    TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, interval);
                    InfoUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.empty_area_selection");
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
        }
    }

    public static void deleteSelectionVolumes(boolean removeEntities, Minecraft mc)
    {
        AreaSelection area = null;

        if (DataManager.getToolMode() == ToolMode.DELETE && ToolModeData.DELETE.getUsePlacement())
        {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (placement != null)
            {
                area = AreaSelection.fromPlacement(placement);
            }
        }
        else
        {
            area = DataManager.getSelectionManager().getCurrentSelection();
        }

        deleteSelectionVolumes(area, removeEntities, mc);
    }

    public static void deleteSelectionVolumes(@Nullable final AreaSelection area, boolean removeEntities, Minecraft mc)
    {
        deleteSelectionVolumes(area, removeEntities, null, mc);
    }

    public static void deleteSelectionVolumes(@Nullable final AreaSelection area, boolean removeEntities,
            @Nullable ICompletionListener listener, Minecraft mc)
    {
        if (mc.player != null && EntityUtils.isCreativeMode(mc.player))
        {
            if (area == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
                return;
            }

            if (area.getAllSubRegionBoxes().size() > 0)
            {
                Box currentBox = area.getSelectedSubRegionBox();
                final ImmutableList<Box> boxes = currentBox != null ? ImmutableList.of(currentBox) : ImmutableList.copyOf(area.getAllSubRegionBoxes());

                if (EntityDataManager.getInstance().hasServuxServer())
                {
                    CompoundData data = new CompoundData();
                    ListData list = new ListData();

                    data.putString("Task", "Delete");
                    data.putBoolean("RemoveEntities", removeEntities);
                    data.putInt("Interval", 1);

                    for (Box box : boxes)
                    {
                        list.add(Box.CODEC.encodeStart(DataOps.INSTANCE, box).getPartialOrThrow());
                    }

                    if (!list.isEmpty())
                    {
                        data.put("Boxes", list);
                        EntityDataManager.getInstance().sendServuxTaskRequest(data, new InfoHudSync(listener));
                        InfoUtils.showGuiOrInGameMessage(MessageType.INFO, 2500, "litematica.message.scheduled_task_to_servux");
                    }
                    else
                    {
                        InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.empty_area_selection");
                    }
                }
                else
                {
                    final int interval = Configs.Generic.COMMAND_TASK_INTERVAL.getIntegerValue();
                    TaskDeleteArea task = new TaskDeleteArea(boxes, removeEntities);

                    if (listener != null)
                    {
                        task.setCompletionListener(listener);
                    }

                    TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, interval);
                    InfoUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.empty_area_selection");
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
        }
    }

    public static void deleteBlocksByPlacement()
    {
        if (DataManager.getToolMode() == ToolMode.DELETE &&
            ToolModeData.DELETE.getUsePlacement())
        {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (placement != null)
            {
                PlacementDeletionMode mode = (PlacementDeletionMode) Configs.Generic.SCHEMATIC_VCS_DELETE_MODE.getOptionListValue();
                deleteBlocksByPlacement(placement, mode, null);
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
                InfoUtils.showGuiOrInGameMessage(MessageType.INFO, "Select a placement either by looking at one in the world, holding the tool item and using the §etoolSelectElements§r hotkey (middle click by default), or alternatively by clicking on a placement in the Schematic Placements menu, so that it has the white outline indicating it's selected. The selected placement's name will also appear on the Tool HUD.");
            }
        }
    }

    public static void deleteBlocksByPlacement(SchematicPlacement placement,
                                               PlacementDeletionMode mode,
                                               @Nullable ICompletionListener listener)
    {
        int interval = Configs.Generic.COMMAND_TASK_INTERVAL.getIntegerValue();
        TaskDeleteBlocksByPlacement task = new TaskDeleteBlocksByPlacement(ImmutableList.of(placement), mode, DataManager.getRenderLayerRange());

        if (listener != null)
        {
            task.setCompletionListener(listener);
        }

        TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, interval);
        InfoUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
    }
}
