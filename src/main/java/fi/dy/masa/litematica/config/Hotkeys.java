package fi.dy.masa.litematica.config;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.litematica.Reference;

public class Hotkeys
{
    private static final String HOTKEYS_KEY = Reference.MOD_ID+".config.hotkeys";

    public static final ConfigHotkey ADD_SELECTION_BOX                  = new ConfigHotkey("addSelectionBox",                   "M,A").apply(HOTKEYS_KEY);
    public static final ConfigHotkey CLONE_SELECTION                    = new ConfigHotkey("cloneSelection",                    "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey DELETE_SELECTION_BOX               = new ConfigHotkey("deleteSelectionBox",                "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey EASY_PLACE_ACTIVATION              = new ConfigHotkey("easyPlaceUseKey",                   "BUTTON_2", KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEYS_KEY);
    public static final ConfigHotkey EASY_PLACE_FIRST                   = new ConfigHotkey("easyPlaceFirst",                    "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey EASY_PLACE_TOGGLE                  = new ConfigHotkey("easyPlaceToggle",                   "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey EXECUTE_OPERATION                  = new ConfigHotkey("executeOperation",                  "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey INVERT_GHOST_BLOCK_RENDER_STATE    = new ConfigHotkey("invertGhostBlockRenderState",       "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey INVERT_OVERLAY_RENDER_STATE        = new ConfigHotkey("invertOverlayRenderState",          "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey LAYER_MODE_NEXT                    = new ConfigHotkey("layerModeNext",                     "M,PAGE_UP").apply(HOTKEYS_KEY);
    public static final ConfigHotkey LAYER_MODE_PREVIOUS                = new ConfigHotkey("layerModePrevious",                 "M,PAGE_DOWN").apply(HOTKEYS_KEY);
    public static final ConfigHotkey LAYER_NEXT                         = new ConfigHotkey("layerNext",                         "PAGE_UP").apply(HOTKEYS_KEY);
    public static final ConfigHotkey LAYER_PREVIOUS                     = new ConfigHotkey("layerPrevious",                     "PAGE_DOWN").apply(HOTKEYS_KEY);
    public static final ConfigHotkey LAYER_SET_HERE                     = new ConfigHotkey("layerSetHere",                      "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey NUDGE_SELECTION_NEGATIVE           = new ConfigHotkey("nudgeSelectionNegative",            "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey NUDGE_SELECTION_POSITIVE           = new ConfigHotkey("nudgeSelectionPositive",            "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey MOVE_ENTIRE_SELECTION              = new ConfigHotkey("moveEntireSelection",               "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_GUI_AREA_SETTINGS             = new ConfigHotkey("openGuiAreaSettings",               "KP_MULTIPLY").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_GUI_LOADED_SCHEMATICS         = new ConfigHotkey("openGuiLoadedSchematics",           "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_GUI_MAIN_MENU                 = new ConfigHotkey("openGuiMainMenu",                   "M",    KeybindSettings.RELEASE_EXCLUSIVE).apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_GUI_MATERIAL_LIST             = new ConfigHotkey("openGuiMaterialList",               "M,L").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_GUI_PLACEMENT_SETTINGS        = new ConfigHotkey("openGuiPlacementSettings",          "KP_SUBTRACT").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_GUI_SCHEMATIC_PLACEMENTS      = new ConfigHotkey("openGuiSchematicPlacements",        "M,P").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_GUI_SCHEMATIC_PROJECTS        = new ConfigHotkey("openGuiSchematicProjects",          "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_GUI_SCHEMATIC_VERIFIER        = new ConfigHotkey("openGuiSchematicVerifier",          "M,V").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_GUI_SELECTION_MANAGER         = new ConfigHotkey("openGuiSelectionManager",           "M,S").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPEN_GUI_SETTINGS                  = new ConfigHotkey("openGuiSettings",                   "M,C").apply(HOTKEYS_KEY);
    public static final ConfigHotkey OPERATION_MODE_CHANGE_MODIFIER     = new ConfigHotkey("operationModeChangeModifier",       "LEFT_CONTROL", KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey PICK_BLOCK_FIRST                   = new ConfigHotkey("pickBlockFirst",                    "BUTTON_3",     KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEYS_KEY);
    public static final ConfigHotkey PICK_BLOCK_LAST                    = new ConfigHotkey("pickBlockLast",                     "",             KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey PICK_BLOCK_TOGGLE                  = new ConfigHotkey("pickBlockToggle",                   "M,BUTTON_3").apply(HOTKEYS_KEY);
    public static final ConfigHotkey RENDER_INFO_OVERLAY                = new ConfigHotkey("renderInfoOverlay",                 "I",             KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEYS_KEY);
    public static final ConfigHotkey RENDER_OVERLAY_THROUGH_BLOCKS      = new ConfigHotkey("renderOverlayThroughBlocks",        "RIGHT_CONTROL", KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEYS_KEY);
    public static final ConfigHotkey RERENDER_SCHEMATIC                 = new ConfigHotkey("rerenderSchematic",                 "F3,M").apply(HOTKEYS_KEY);
    public static final ConfigHotkey SAVE_AREA_AS_IN_MEMORY_SCHEMATIC   = new ConfigHotkey("saveAreaAsInMemorySchematic",       "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey SAVE_AREA_AS_SCHEMATIC_TO_FILE     = new ConfigHotkey("saveAreaAsSchematicToFile",         "LEFT_CONTROL,LEFT_ALT,S").apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_EDIT_BREAK_ALL_EXCEPT    = new ConfigHotkey("schematicEditBreakAllExcept",       "", KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_EDIT_BREAK_ALL           = new ConfigHotkey("schematicEditBreakPlaceAll",        "", KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_EDIT_BREAK_DIRECTION     = new ConfigHotkey("schematicEditBreakPlaceDirection",  "", KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_EDIT_REPLACE_ALL         = new ConfigHotkey("schematicEditReplaceAll",           "", KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_EDIT_REPLACE_BLOCK       = new ConfigHotkey("schematicEditReplaceBlock",         "", KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_EDIT_REPLACE_DIRECTION   = new ConfigHotkey("schematicEditReplaceDirection",     "", KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
	public static final ConfigHotkey SCHEMATIC_EDIT_REPLACE_SELECTION   = new ConfigHotkey("schematicEditReplaceSelection",     "", KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_PLACEMENT_ROTATION       = new ConfigHotkey("schematicPlacementRotation",        "",     KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_PLACEMENT_MIRROR         = new ConfigHotkey("schematicPlacementMirror",          "",     KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_VCS_DELETE_BY_PLACEMENT  = new ConfigHotkey("schematicVCSDeleteBlockByPlacement","").apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_VERSION_CYCLE_MODIFIER   = new ConfigHotkey("schematicVersionCycleModifier",     "",     KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_VERSION_CYCLE_NEXT       = new ConfigHotkey("schematicVersionCycleNext",         "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey SCHEMATIC_VERSION_CYCLE_PREVIOUS   = new ConfigHotkey("schematicVersionCyclePrevious",     "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey SELECTION_GRAB_MODIFIER            = new ConfigHotkey("selectionGrabModifier",             "",     KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SELECTION_GROW_HOTKEY              = new ConfigHotkey("selectionGrow",                     "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey SELECTION_GROW_MODIFIER            = new ConfigHotkey("selectionGrowModifier",             "",     KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SELECTION_NUDGE_MODIFIER           = new ConfigHotkey("selectionNudgeModifier",            "LEFT_ALT", KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey SELECTION_MODE_CYCLE               = new ConfigHotkey("selectionModeCycle",                "LEFT_CONTROL,M").apply(HOTKEYS_KEY);
    public static final ConfigHotkey SELECTION_SHRINK_HOTKEY            = new ConfigHotkey("selectionShrink",                   "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey SET_AREA_ORIGIN                    = new ConfigHotkey("setAreaOrigin",                     "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey SET_SELECTION_BOX_POSITION_1       = new ConfigHotkey("setSelectionBoxPosition1",          "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey SET_SELECTION_BOX_POSITION_2       = new ConfigHotkey("setSelectionBoxPosition2",          "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_ALL_RENDERING               = new ConfigHotkey("toggleAllRendering",                "M,R").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_AREA_SELECTION_RENDERING    = new ConfigHotkey("toggleAreaSelectionBoxesRendering", "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_SCHEMATIC_RENDERING         = new ConfigHotkey("toggleSchematicRendering",          "M,G").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_INFO_OVERLAY_RENDERING      = new ConfigHotkey("toggleInfoOverlayRendering",        "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_OVERLAY_RENDERING           = new ConfigHotkey("toggleOverlayRendering",            "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_OVERLAY_OUTLINE_RENDERING   = new ConfigHotkey("toggleOverlayOutlineRendering",     "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_OVERLAY_SIDE_RENDERING      = new ConfigHotkey("toggleOverlaySideRendering",        "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_PLACEMENT_BOXES_RENDERING   = new ConfigHotkey("togglePlacementBoxesRendering",     "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_PLACEMENT_RESTRICTION       = new ConfigHotkey("togglePlacementRestriction",        "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_SCHEMATIC_BLOCK_RENDERING   = new ConfigHotkey("toggleSchematicBlockRendering",     "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_SIGN_TEXT_PASTE             = new ConfigHotkey("toggleSignTextPaste",               "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_TRANSLUCENT_RENDERING       = new ConfigHotkey("toggleTranslucentRendering",        "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOGGLE_VERIFIER_OVERLAY_RENDERING  = new ConfigHotkey("toggleVerifierOverlayRendering",    "").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOOL_ENABLED_TOGGLE                = new ConfigHotkey("toolEnabledToggle",                 "M,T").apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOOL_PLACE_CORNER_1                = new ConfigHotkey("toolPlaceCorner1",                  "BUTTON_1", KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOOL_PLACE_CORNER_2                = new ConfigHotkey("toolPlaceCorner2",                  "BUTTON_2", KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOOL_SELECT_ELEMENTS               = new ConfigHotkey("toolSelectElements",                "BUTTON_3", KeybindSettings.PRESS_ALLOWEXTRA).apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOOL_SELECT_MODIFIER_BLOCK_1       = new ConfigHotkey("toolSelectModifierBlock1",          "LEFT_ALT", KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey TOOL_SELECT_MODIFIER_BLOCK_2       = new ConfigHotkey("toolSelectModifierBlock2",          "LEFT_SHIFT", KeybindSettings.MODIFIER_INGAME).apply(HOTKEYS_KEY);
    public static final ConfigHotkey UNLOAD_CURRENT_SCHEMATIC           = new ConfigHotkey("unloadCurrentSchematic",            "").apply(HOTKEYS_KEY);

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            ADD_SELECTION_BOX,
            CLONE_SELECTION,
            DELETE_SELECTION_BOX,
            EASY_PLACE_ACTIVATION,
            EASY_PLACE_FIRST,
            EASY_PLACE_TOGGLE,
            EXECUTE_OPERATION,
            INVERT_GHOST_BLOCK_RENDER_STATE,
            INVERT_OVERLAY_RENDER_STATE,
            LAYER_MODE_NEXT,
            LAYER_MODE_PREVIOUS,
            LAYER_NEXT,
            LAYER_PREVIOUS,
            LAYER_SET_HERE,
            NUDGE_SELECTION_NEGATIVE,
            NUDGE_SELECTION_POSITIVE,
            MOVE_ENTIRE_SELECTION,
            OPEN_GUI_AREA_SETTINGS,
            OPEN_GUI_LOADED_SCHEMATICS,
            OPEN_GUI_MAIN_MENU,
            OPEN_GUI_MATERIAL_LIST,
            OPEN_GUI_PLACEMENT_SETTINGS,
            OPEN_GUI_SCHEMATIC_PLACEMENTS,
            OPEN_GUI_SCHEMATIC_PROJECTS,
            OPEN_GUI_SCHEMATIC_VERIFIER,
            OPEN_GUI_SELECTION_MANAGER,
            OPEN_GUI_SETTINGS,
            OPERATION_MODE_CHANGE_MODIFIER,
            PICK_BLOCK_FIRST,
            PICK_BLOCK_LAST,
            PICK_BLOCK_TOGGLE,
            RENDER_INFO_OVERLAY,
            RENDER_OVERLAY_THROUGH_BLOCKS,
            RERENDER_SCHEMATIC,
            SAVE_AREA_AS_IN_MEMORY_SCHEMATIC,
            SAVE_AREA_AS_SCHEMATIC_TO_FILE,
            SCHEMATIC_EDIT_BREAK_ALL,
            SCHEMATIC_EDIT_BREAK_ALL_EXCEPT,
            SCHEMATIC_EDIT_BREAK_DIRECTION,
            SCHEMATIC_EDIT_REPLACE_ALL,
            SCHEMATIC_EDIT_REPLACE_BLOCK,
            SCHEMATIC_EDIT_REPLACE_DIRECTION,
			SCHEMATIC_EDIT_REPLACE_SELECTION,			// Requested to be added by Earthcomputer; from Litemoretica
			SCHEMATIC_PLACEMENT_ROTATION,
            SCHEMATIC_PLACEMENT_MIRROR,
            SCHEMATIC_VCS_DELETE_BY_PLACEMENT,
            SCHEMATIC_VERSION_CYCLE_MODIFIER,
            SCHEMATIC_VERSION_CYCLE_NEXT,
            SCHEMATIC_VERSION_CYCLE_PREVIOUS,
            SELECTION_GRAB_MODIFIER,
            SELECTION_GROW_HOTKEY,
            SELECTION_GROW_MODIFIER,
            SELECTION_NUDGE_MODIFIER,
            SELECTION_MODE_CYCLE,
            SELECTION_SHRINK_HOTKEY,
            SET_AREA_ORIGIN,
            SET_SELECTION_BOX_POSITION_1,
            SET_SELECTION_BOX_POSITION_2,
            TOGGLE_ALL_RENDERING,
            TOGGLE_AREA_SELECTION_RENDERING,
            TOGGLE_INFO_OVERLAY_RENDERING,
            TOGGLE_OVERLAY_RENDERING,
            TOGGLE_OVERLAY_OUTLINE_RENDERING,
            TOGGLE_OVERLAY_SIDE_RENDERING,
            TOGGLE_PLACEMENT_BOXES_RENDERING,
            TOGGLE_PLACEMENT_RESTRICTION,
            TOGGLE_SCHEMATIC_BLOCK_RENDERING,
            TOGGLE_SCHEMATIC_RENDERING,
            TOGGLE_SIGN_TEXT_PASTE,
            TOGGLE_TRANSLUCENT_RENDERING,
            TOGGLE_VERIFIER_OVERLAY_RENDERING,
            TOOL_ENABLED_TOGGLE,
            TOOL_PLACE_CORNER_1,
            TOOL_PLACE_CORNER_2,
            TOOL_SELECT_ELEMENTS,
            TOOL_SELECT_MODIFIER_BLOCK_1,
            TOOL_SELECT_MODIFIER_BLOCK_2,
            UNLOAD_CURRENT_SCHEMATIC
    );
}
