package fi.dy.masa.litematica.event;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigString;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.hotkeys.*;
import fi.dy.masa.malilib.interfaces.IValueChangeCallback;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.LayerMode;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntityDataManager;
import fi.dy.masa.litematica.gui.*;
import fi.dy.masa.litematica.gui.GuiConfigs.ConfigGuiTab;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.schematic.placement.PlacementManagerDaemonHandler;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.CornerSelectionMode;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.tool.ToolModeData;
import fi.dy.masa.litematica.util.*;
import fi.dy.masa.litematica.util.PositionUtils.Corner;

public class KeyCallbacks
{
    public static void init(Minecraft mc)
    {
        IHotkeyCallback callbackHotkeys = new KeyCallbackHotkeys(mc);
        IHotkeyCallback callbackMessage = new KeyCallbackToggleMessage(mc);
        ValueChangeCallback valueChangeCallback = new ValueChangeCallback();
        ThreadCountChangeCallback threadCountChangeCallback = new ThreadCountChangeCallback();
        RenderChangeCallback renderChangeCallback = new RenderChangeCallback();

        Configs.Generic.PLACEMENT_MANAGER_THREAD_COUNT.setValueChangeCallback(threadCountChangeCallback);
        Configs.Generic.PICK_BLOCKABLE_SLOTS.setValueChangeCallback(valueChangeCallback);
        Configs.Generic.ENTITY_DATA_SYNC.setValueChangeCallback((config) -> EntityDataManager.getInstance().onEntityDataSyncToggled(config));

        Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.ENABLE_PLACEMENT_BOXES_RENDERING.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.ENABLE_RENDERING.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.ENABLE_SCHEMATIC_FLUIDS.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY_CULLING.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.setValueChangeCallback(renderChangeCallback);
//        Configs.Visuals.ENABLE_SCHEMATIC_ENTITY_HITBOXES.setValueChangeCallback(renderChangeCallback); // Not needed
        Configs.Visuals.ENABLE_SCHEMATIC_FAKE_LIGHTING.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.RENDER_AO_MODERN_ENABLE.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.RENDER_SCHEMATIC_ENTITIES.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.RENDER_SCHEMATIC_TILE_ENTITIES.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_DIFF_BLOCK.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_EXTRA.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_MISSING.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK.setValueChangeCallback(renderChangeCallback);
        Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_STATE.setValueChangeCallback(renderChangeCallback);

        Hotkeys.CLONE_SELECTION.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.EXECUTE_OPERATION.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.LAYER_MODE_NEXT.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.LAYER_MODE_PREVIOUS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.LAYER_NEXT.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.LAYER_PREVIOUS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.LAYER_SET_HERE.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.NUDGE_SELECTION_NEGATIVE.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.NUDGE_SELECTION_POSITIVE.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_AREA_SETTINGS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_LOADED_SCHEMATICS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_SCHEMATIC_PLACEMENTS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_SCHEMATIC_PROJECTS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_SCHEMATIC_VERIFIER.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.OPEN_GUI_SETTINGS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.PICK_BLOCK_FIRST.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.PICK_BLOCK_LAST.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.PICK_BLOCK_TOGGLE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.PICK_BLOCK_ENABLED));
        Hotkeys.RERENDER_SCHEMATIC.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.SAVE_AREA_AS_IN_MEMORY_SCHEMATIC.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.SAVE_AREA_AS_SCHEMATIC_TO_FILE.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.SCHEMATIC_VCS_DELETE_BY_PLACEMENT.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.SCHEMATIC_VERSION_CYCLE_NEXT.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.SCHEMATIC_VERSION_CYCLE_PREVIOUS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.SELECTION_GROW_HOTKEY.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.SELECTION_SHRINK_HOTKEY.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.TOOL_PLACE_CORNER_1.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.TOOL_PLACE_CORNER_2.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.TOOL_SELECT_ELEMENTS.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_1.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_2.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.UNLOAD_CURRENT_SCHEMATIC.getKeybind().setCallback(callbackHotkeys);
        Hotkeys.ADD_SELECTION_BOX.getKeybind().setCallback(callbackMessage);
        Hotkeys.DELETE_SELECTION_BOX.getKeybind().setCallback(callbackMessage);
        Hotkeys.EASY_PLACE_FIRST.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.EASY_PLACE_FIRST));
        Hotkeys.EASY_PLACE_TOGGLE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.EASY_PLACE_MODE));
        Hotkeys.MOVE_ENTIRE_SELECTION.getKeybind().setCallback(callbackMessage);
        Hotkeys.SELECTION_MODE_CYCLE.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_AREA_ORIGIN.getKeybind().setCallback(callbackMessage);
        Hotkeys.SCHEMATIC_PLACEMENT_ROTATION.getKeybind().setCallback(callbackMessage);
        Hotkeys.SCHEMATIC_PLACEMENT_MIRROR.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_ALL_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.ENABLE_RENDERING));
        Hotkeys.TOGGLE_AREA_SELECTION_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.ENABLE_AREA_SELECTION_RENDERING));
        Hotkeys.TOGGLE_SCHEMATIC_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.ENABLE_SCHEMATIC_RENDERING));
        Hotkeys.TOGGLE_INFO_OVERLAY_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.InfoOverlays.BLOCK_INFO_OVERLAY_ENABLED));
        Hotkeys.TOGGLE_OVERLAY_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY));
        Hotkeys.TOGGLE_OVERLAY_OUTLINE_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES));
        Hotkeys.TOGGLE_OVERLAY_SIDE_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES));
        Hotkeys.TOGGLE_PLACEMENT_RESTRICTION.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.PLACEMENT_RESTRICTION));
        Hotkeys.TOGGLE_PLACEMENT_BOXES_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.ENABLE_PLACEMENT_BOXES_RENDERING));
        Hotkeys.TOGGLE_SCHEMATIC_BLOCK_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS));
        Hotkeys.TOGGLE_SIGN_TEXT_PASTE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.SIGN_TEXT_PASTE));
        Hotkeys.TOGGLE_TRANSLUCENT_RENDERING.getKeybind().setCallback(new RenderToggle(Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT));
        Hotkeys.TOGGLE_VERIFIER_OVERLAY_RENDERING.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED));
        Hotkeys.TOOL_ENABLED_TOGGLE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(Configs.Generic.TOOL_ITEM_ENABLED));
		Hotkeys.SCHEMATIC_EDIT_REPLACE_SELECTION.getKeybind().setCallback(callbackMessage);
    }

    private static class ValueChangeCallback implements IValueChangeCallback<ConfigString>
    {
        @Override
        public void onValueChanged(ConfigString config)
        {
            if (config == Configs.Generic.PICK_BLOCKABLE_SLOTS)
            {
                InventoryUtils.setPickBlockableSlots(Configs.Generic.PICK_BLOCKABLE_SLOTS.getStringValue());
            }
        }
    }

    private static class ThreadCountChangeCallback implements IValueChangeCallback<ConfigInteger>
    {
        @Override
        public void onValueChanged(ConfigInteger config)
        {
            PlacementManagerDaemonHandler.INSTANCE.resetThreadCount(config, Minecraft.getInstance().level == null);
        }
    }

    private static class RenderChangeCallback implements IValueChangeCallback<ConfigBoolean>
    {
        @Override
        public void onValueChanged(ConfigBoolean config)
        {
            SchematicWorldRefresher.INSTANCE.updateAll();
        }
    }

    private static class RenderToggle extends KeyCallbackToggleBooleanConfigWithMessage
    {
        private final boolean isMainToggle;

        public RenderToggle(IConfigBoolean config)
        {
            super(config);
	        this.isMainToggle = config == Configs.Visuals.ENABLE_RENDERING;
        }

        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            super.onKeyAction(action, key);

            if (this.isMainToggle)
            {
                DataManager.getSchematicPlacementManager().onToggleMainRendering(this.config.getBooleanValue());
            }

            if (this.config.getBooleanValue())
            {
                SchematicWorldRefresher.INSTANCE.updateAll();
            }

            return true;
        }
    }

    private record KeyCallbackHotkeys(Minecraft mc) implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            if (this.mc.player == null || this.mc.level == null)
            {
                return false;
            }

            ToolMode mode = DataManager.getToolMode();

            boolean toolEnabled = Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();
            boolean hasTool = EntityUtils.hasToolItem(this.mc.player);
            boolean isToolPrimary = key == Hotkeys.TOOL_PLACE_CORNER_1.getKeybind();
            boolean isToolSecondary = key == Hotkeys.TOOL_PLACE_CORNER_2.getKeybind();
            boolean isToolSelect = key == Hotkeys.TOOL_SELECT_ELEMENTS.getKeybind();

            if (toolEnabled && isToolSelect)
            {
                if (mode.getUsesBlockPrimary() && Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_1.getKeybind().isKeybindHeld())
                {
                    WorldUtils.setToolModeBlockState(mode, true, this.mc);
                    return true;
                }
                else if (mode.getUsesBlockSecondary() && Hotkeys.TOOL_SELECT_MODIFIER_BLOCK_2.getKeybind().isKeybindHeld())
                {
                    WorldUtils.setToolModeBlockState(mode, false, this.mc);
                    return true;
                }
            }

            if (toolEnabled && hasTool)
            {
                int maxDistance = 200;
                boolean projectMode = DataManager.getSchematicProjectsManager().hasProjectOpen();

                if (isToolPrimary || isToolSecondary)
                {
                    if (mode.getUsesAreaSelection() || projectMode)
                    {
                        SelectionManager sm = DataManager.getSelectionManager();
                        boolean grabModifier = Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld();
                        boolean moveEverything = grabModifier;

                        if (grabModifier && mode == ToolMode.MOVE)
                        {
                            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                            BlockPos pos = RayTraceUtils.getTargetedPosition(this.mc.level, entity, maxDistance, false);

                            if (pos != null)
                            {
                                SchematicUtils.moveCurrentlySelectedWorldRegionTo(pos, this.mc);
                            }
                        }
                        else if (Configs.Generic.SELECTION_CORNERS_MODE.getOptionListValue() == CornerSelectionMode.CORNERS)
                        {
                            Corner corner = isToolPrimary ? Corner.CORNER_1 : Corner.CORNER_2;
                            sm.setPositionOfCurrentSelectionToRayTrace(this.mc, corner, moveEverything, maxDistance);
                        }
                        else if (Configs.Generic.SELECTION_CORNERS_MODE.getOptionListValue() == CornerSelectionMode.EXPAND)
                        {
                            sm.handleCuboidModeMouseClick(this.mc, maxDistance, isToolSecondary, moveEverything);
                        }
                    }
                    else if (mode.getUsesSchematic())
                    {
                        DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionToRayTrace(this.mc, maxDistance);
                    }

                    return true;
                }
                else if (isToolSelect)
                {
                    if (mode.getUsesAreaSelection() || projectMode)
                    {
                        SelectionManager sm = DataManager.getSelectionManager();

                        if (Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld())
                        {
                            if (sm.hasGrabbedElement())
                            {
                                sm.releaseGrabbedElement();
                            }
                            else
                            {
                                sm.grabElement(this.mc, maxDistance);
                            }
                        }
                        else
                        {
                            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                            sm.changeSelection(this.mc.level, entity, maxDistance);
                        }
                    }
                    else if (mode.getUsesSchematic())
                    {
                        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                        DataManager.getSchematicPlacementManager().changeSelection(this.mc.level, entity, maxDistance);
                    }

                    return true;
                }
            }

            if (key == Hotkeys.EASY_PLACE_ACTIVATION.getKeybind())
            {
                if (Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue())
                {
                    return EasyPlaceUtils.handleEasyPlaceWithMessage();
                }
                else
                {
                    return WorldUtils.handleEasyPlace(this.mc);
                }
            }
            else if (key == Hotkeys.OPEN_GUI_MAIN_MENU.getKeybind())
            {
                GuiBase.openGui(new GuiMainMenu());
                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_LOADED_SCHEMATICS.getKeybind())
            {
                GuiBase.openGui(new GuiSchematicLoadedList());
                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind())
            {
                if (DataManager.getSchematicProjectsManager().hasProjectOpen() == false)
                {
                    GuiBase.openGui(new GuiAreaSelectionManager());
                }
                else
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.gui.button.hover.schematic_projects.area_browser_disabled_currently_in_projects_mode");
                }

                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_PLACEMENTS.getKeybind())
            {
                GuiBase.openGui(new GuiSchematicPlacementsList());
                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_PROJECTS.getKeybind())
            {
                DataManager.getSchematicProjectsManager().openSchematicProjectsGui();
                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_SETTINGS.getKeybind())
            {
                if (DataManager.getConfigGuiTab() == ConfigGuiTab.RENDER_LAYERS)
                {
                    GuiBase.openGui(new GuiRenderLayer());
                }
                else
                {
                    GuiBase.openGui(new GuiConfigs());
                }

                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeybind())
            {
                SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

                if (schematicPlacement != null)
                {
                    SubRegionPlacement placement = schematicPlacement.getSelectedSubRegionPlacement();

                    if (placement != null)
                    {
                        GuiBase.openGui(new GuiSubRegionConfiguration(schematicPlacement, placement));
                    }
                    else
                    {
                        GuiBase.openGui(new GuiPlacementConfiguration(schematicPlacement));
                    }
                }
                else
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
                }

                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_VERIFIER.getKeybind())
            {
                SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

                if (schematicPlacement != null)
                {
                    GuiBase.openGui(new GuiSchematicVerifier(schematicPlacement));
                }
                else
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
                }

                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_MATERIAL_LIST.getKeybind())
            {
                MaterialListBase materialList = DataManager.getMaterialList();

                // No last-viewed material list currently stored, try to get one for the currently selected placement, if any
                if (materialList == null)
                {
                    SchematicPlacement schematicPlacement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

                    if (schematicPlacement != null)
                    {
                        materialList = schematicPlacement.getMaterialList();
                        materialList.reCreateMaterialList();
                    }
                    else
                    {
                        InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
                    }
                }

                if (materialList != null)
                {
                    GuiBase.openGui(new GuiMaterialList(materialList));
                }

                return true;
            }
            else if (key == Hotkeys.OPEN_GUI_AREA_SETTINGS.getKeybind())
            {
                SelectionManager manager = DataManager.getSelectionManager();

                if (manager.getCurrentSelection() != null)
                {
                    manager.openEditGui(null);
                }
                else
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_area_selected");
                }

                return true;
            }
            else if (key == Hotkeys.RERENDER_SCHEMATIC.getKeybind())
            {
                SchematicWorldRefresher.INSTANCE.updateAll();
                InfoUtils.printActionbarMessage("litematica.message.schematic_rendering_refreshed");
                return true;
            }
            else if (key == Hotkeys.LAYER_NEXT.getKeybind())
            {
                DataManager.getRenderLayerRange().moveLayer(1);
                return true;
            }
            else if (key == Hotkeys.LAYER_PREVIOUS.getKeybind())
            {
                DataManager.getRenderLayerRange().moveLayer(-1);
                return true;
            }
            else if (key == Hotkeys.LAYER_SET_HERE.getKeybind())
            {
                DataManager.getRenderLayerRange().setSingleBoundaryToPosition(fi.dy.masa.malilib.util.EntityUtils.getCameraEntity());
                return true;
            }
            else if (key == Hotkeys.LAYER_MODE_NEXT.getKeybind())
            {
                DataManager.getRenderLayerRange().setLayerMode((LayerMode) DataManager.getRenderLayerRange().getLayerMode().cycle(true));
                return true;
            }
            else if (key == Hotkeys.LAYER_MODE_PREVIOUS.getKeybind())
            {
                DataManager.getRenderLayerRange().setLayerMode((LayerMode) DataManager.getRenderLayerRange().getLayerMode().cycle(false));
                return true;
            }
            else if (key == Hotkeys.PICK_BLOCK_FIRST.getKeybind())
            {
                if (EntityUtils.shouldPickBlock(this.mc.player))
                {
                    return WorldUtils.doSchematicWorldPickBlock(true, this.mc);
                }

                return false;
            }
            else if (key == Hotkeys.PICK_BLOCK_LAST.getKeybind())
            {
                if (EntityUtils.shouldPickBlock(this.mc.player))
                {
                    // Only do the pick block here, if it's not bound to the use button.
                    // If it's bound to the use button, then it will be done from the input handling.
                    if (KeybindMulti.hotkeyMatchesKeybind(Hotkeys.PICK_BLOCK_LAST, this.mc.options.keyUse) == false)
                    {
                        WorldUtils.doSchematicWorldPickBlock(false, this.mc);
                    }
                }

                return false;
            }
            else if (key == Hotkeys.SAVE_AREA_AS_SCHEMATIC_TO_FILE.getKeybind())
            {
                return SchematicUtils.saveSchematic(false);
            }
            else if (key == Hotkeys.SAVE_AREA_AS_IN_MEMORY_SCHEMATIC.getKeybind())
            {
                return SchematicUtils.saveSchematic(true);
            }
            else if (key == Hotkeys.SCHEMATIC_VERSION_CYCLE_NEXT.getKeybind())
            {
                if (DataManager.getSchematicProjectsManager().hasProjectOpen())
                {
                    DataManager.getSchematicProjectsManager().cycleVersion(1);
                }
                return true;
            }
            else if (key == Hotkeys.SCHEMATIC_VERSION_CYCLE_PREVIOUS.getKeybind())
            {
                if (DataManager.getSchematicProjectsManager().hasProjectOpen())
                {
                    DataManager.getSchematicProjectsManager().cycleVersion(-1);
                }
                return true;
            }
            else if (key == Hotkeys.CLONE_SELECTION.getKeybind())
            {
                SchematicUtils.cloneSelectionArea(this.mc);
                return true;
            }
            else if (key == Hotkeys.EXECUTE_OPERATION.getKeybind() && ((hasTool && toolEnabled) || Configs.Generic.EXECUTE_REQUIRE_TOOL.getBooleanValue() == false))
            {
                if (DataManager.getSchematicProjectsManager().hasProjectOpen())
                {
                    DataManager.getSchematicProjectsManager().pasteCurrentVersionToWorld();
                    return true;
                }
                else if (mode == ToolMode.PASTE_SCHEMATIC)
                {
                    DataManager.getSchematicPlacementManager().pasteCurrentPlacementToWorld(this.mc);
                    return true;
                }
                else if (mode == ToolMode.FILL && mode.getPrimaryBlock() != null)
                {
                    ToolUtils.fillSelectionVolumes(this.mc, mode.getPrimaryBlock(), null);
                    return true;
                }
                else if (mode == ToolMode.REPLACE_BLOCK && mode.getPrimaryBlock() != null && mode.getSecondaryBlock() != null)
                {
                    ToolUtils.fillSelectionVolumes(this.mc, mode.getPrimaryBlock(), mode.getSecondaryBlock());
                    return true;
                }
                else if (mode == ToolMode.DELETE)
                {
                    boolean removeEntities = true; // TODO
                    ToolUtils.deleteSelectionVolumes(removeEntities, this.mc);
                    return true;
                }
            }
            else if (key == Hotkeys.SCHEMATIC_VCS_DELETE_BY_PLACEMENT.getKeybind())
            {
                if (DataManager.getSchematicProjectsManager().hasProjectOpen())
                {
                    DataManager.getSchematicProjectsManager().deleteBlocksByPlacement();
                    return true;
                }
            }
            else if (key == Hotkeys.NUDGE_SELECTION_NEGATIVE.getKeybind() ||
                     key == Hotkeys.NUDGE_SELECTION_POSITIVE.getKeybind())
            {
                int amount = key == Hotkeys.NUDGE_SELECTION_POSITIVE.getKeybind() ? 1 : -1;
                InputHandler.nudgeSelection(amount, mode, this.mc.player);
                return true;
            }
            else if (key == Hotkeys.SELECTION_GROW_HOTKEY.getKeybind())
            {
                if (mode.getUsesAreaSelection())
                {
                    PositionUtils.growOrShrinkCurrentSelection(true);
                    return true;
                }
            }
            else if (key == Hotkeys.SELECTION_SHRINK_HOTKEY.getKeybind())
            {
                if (mode.getUsesAreaSelection())
                {
                    PositionUtils.growOrShrinkCurrentSelection(false);
                    return true;
                }
            }
            else if (key == Hotkeys.UNLOAD_CURRENT_SCHEMATIC.getKeybind())
            {
                SchematicUtils.unloadCurrentlySelectedSchematic();
                return true;
            }

            return false;
        }
    }

    private record KeyCallbackToggleMessage(Minecraft mc) implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            ToolMode mode = DataManager.getToolMode();

            if (key == Hotkeys.ADD_SELECTION_BOX.getKeybind())
            {
                if (mode.getUsesAreaSelection())
                {
                    return DataManager.getSelectionManager().createNewSubRegion(this.mc, true);
                }
            }
            else if (key == Hotkeys.DELETE_SELECTION_BOX.getKeybind())
            {
                if (mode.getUsesAreaSelection())
                {
                    SelectionManager sm = DataManager.getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        if (selection.isOriginSelected())
                        {
                            selection.setExplicitOrigin(null);
                            selection.setOriginSelected(false);
                            InfoUtils.printActionbarMessage("litematica.message.removed_area_origin");
                        }
                        else
                        {
                            String name = selection.getCurrentSubRegionBoxName();

                            if (name != null && selection.removeSelectedSubRegionBox())
                            {
                                InfoUtils.printActionbarMessage("litematica.message.removed_selection_box", name);
                                return true;
                            }
                        }
                    }
                }
            }
            else if (key == Hotkeys.MOVE_ENTIRE_SELECTION.getKeybind())
            {
                if (this.mc.player == null)
                {
                    return false;
                }

                if (mode.getUsesAreaSelection())
                {
                    SelectionManager sm = DataManager.getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        BlockPos pos = BlockPos.containing(this.mc.player.position());

                        if (mode == ToolMode.MOVE)
                        {
                            SchematicUtils.moveCurrentlySelectedWorldRegionTo(pos, this.mc);
                        }
                        else
                        {
                            selection.moveEntireSelectionTo(pos, true);
                        }

                        return true;
                    }
                }
                else if (mode.getUsesSchematic())
                {
                    BlockPos pos = BlockPos.containing(this.mc.player.position());
                    DataManager.getSchematicPlacementManager().setPositionOfCurrentSelectionTo(pos, this.mc);
                    return true;
                }
            }
            else if (key == Hotkeys.SCHEMATIC_PLACEMENT_ROTATION.getKeybind())
            {
                SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

                if (placement != null)
                {
                    Rotation rotation = PositionUtils.cycleRotation(placement.getRotation(), false);
                    if (placement.isLocked())
                    {
                        InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
                    }
                    else
                    {
                        placement.setRotation(rotation, null);
                        InfoUtils.printActionbarMessage("litematica.message.placement.rotation_set_to", PositionUtils.getRotationNameShort(rotation));
                    }
                    return true;
                }
            }
            else if (key == Hotkeys.SCHEMATIC_PLACEMENT_MIRROR.getKeybind())
            {
                SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

                if (placement != null)
                {
                    Mirror mirror = PositionUtils.cycleMirror(placement.getMirror(), false);
                    if (placement.isLocked())
                    {
                        InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
                    }
                    else
                    {
                        placement.setMirror(mirror, null);
                        InfoUtils.printActionbarMessage("litematica.message.placement.mirror_set_to", PositionUtils.getMirrorName(mirror));
                    }
                    return true;
                }
            }
            else if (key == Hotkeys.SELECTION_MODE_CYCLE.getKeybind())
            {
                if (mode == ToolMode.DELETE)
                {
                    ToolModeData.DELETE.toggleUsePlacement();
                }
                else if (mode == ToolMode.PASTE_SCHEMATIC)
                {
                    Configs.Generic.PASTE_REPLACE_BEHAVIOR.setOptionListValue(Configs.Generic.PASTE_REPLACE_BEHAVIOR.getOptionListValue().cycle(false));
                }
                else if (mode.getUsesAreaSelection())
                {
                    Configs.Generic.SELECTION_CORNERS_MODE.setOptionListValue(Configs.Generic.SELECTION_CORNERS_MODE.getOptionListValue().cycle(false));
                }

                return true;
            }
            else if (key == Hotkeys.SET_AREA_ORIGIN.getKeybind())
            {
                if (mode.getUsesAreaSelection() && this.mc.player != null)
                {
                    SelectionManager sm = DataManager.getSelectionManager();
                    AreaSelection area = sm.getCurrentSelection();

                    if (area != null)
                    {
                        BlockPos pos = BlockPos.containing(this.mc.player.position());
                        area.setExplicitOrigin(pos);
                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        InfoUtils.printActionbarMessage("litematica.message.set_area_origin", posStr);
                        return true;
                    }
                }
            }
            else if (key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() ||
                     key == Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind())
            {
                if (mode.getUsesAreaSelection() && this.mc.player != null)
                {
                    SelectionManager sm = DataManager.getSelectionManager();
                    AreaSelection area = sm.getCurrentSelection();

                    if (area != null && area.getSelectedSubRegionBox() != null)
                    {
                        BlockPos pos = BlockPos.containing(this.mc.player.position());
                        Corner corner = key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() ? Corner.CORNER_1 : Corner.CORNER_2;
                        area.setSelectedSubRegionCornerPos(pos, corner);

                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        InfoUtils.printActionbarMessage("litematica.message.set_selection_box_point", corner.ordinal(), posStr);
                        return true;
                    }
                }
            }
            // Requested to be added by Earthcomputer; from Litemoretica
            else if (key == Hotkeys.SCHEMATIC_EDIT_REPLACE_SELECTION.getKeybind())
            {
                AreaSelection selection = DataManager.getSelectionManager().getCurrentSelection();

                if (SchematicUtils.saveAreaSelectionToSchematic(selection, this.mc.level))
                {
                    BlockPos pos = selection.getEffectiveOrigin();

                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    InfoUtils.showInGameMessage(MessageType.SUCCESS,
                                                "litematica.message.schematic_edit_replace_selection", posStr
                    );
                    return true;
                }
            }

            return false;
        }
    }
}
