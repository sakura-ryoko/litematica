package fi.dy.masa.litematica.config;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

public class Hotkeys
{
    public static final ConfigHotkey ADD_SELECTION_BOX                  = new ConfigHotkey("addSelectionBox",                   "M,A",  "litematica.config.hotkeys.comment.addSelectionBox").translatedName("litematica.config.hotkeys.name.addSelectionBox");
    public static final ConfigHotkey CLONE_SELECTION                    = new ConfigHotkey("cloneSelection",                    "",     "litematica.config.hotkeys.comment.cloneSelection").translatedName("litematica.config.hotkeys.name.cloneSelection");
    public static final ConfigHotkey DELETE_SELECTION_BOX               = new ConfigHotkey("deleteSelectionBox",                "",     "litematica.config.hotkeys.comment.deleteSelectionBox").translatedName("litematica.config.hotkeys.name.deleteSelectionBox");
    public static final ConfigHotkey EASY_PLACE_ACTIVATION              = new ConfigHotkey("easyPlaceUseKey",                   "BUTTON_2", KeybindSettings.PRESS_ALLOWEXTRA, "litematica.config.hotkeys.comment.easyPlaceUseKey").translatedName("litematica.config.hotkeys.name.easyPlaceUseKey");
    public static final ConfigHotkey EASY_PLACE_TOGGLE                  = new ConfigHotkey("easyPlaceToggle",                   "",     "litematica.config.hotkeys.comment.easyPlaceToggle").translatedName("litematica.config.hotkeys.name.easyPlaceToggle");
    public static final ConfigHotkey EXECUTE_OPERATION                  = new ConfigHotkey("executeOperation",                  "",     "litematica.config.hotkeys.comment.executeOperation").translatedName("litematica.config.hotkeys.name.executeOperation");
    public static final ConfigHotkey INVERT_GHOST_BLOCK_RENDER_STATE    = new ConfigHotkey("invertGhostBlockRenderState",       "",     "litematica.config.hotkeys.comment.invertGhostBlockRenderState").translatedName("litematica.config.hotkeys.name.invertGhostBlockRenderState");
    public static final ConfigHotkey INVERT_OVERLAY_RENDER_STATE        = new ConfigHotkey("invertOverlayRenderState",          "",     "litematica.config.hotkeys.comment.invertOverlayRenderState").translatedName("litematica.config.hotkeys.name.invertOverlayRenderState");
    public static final ConfigHotkey LAYER_MODE_NEXT                    = new ConfigHotkey("layerModeNext",                     "M,PAGE_UP",    "litematica.config.hotkeys.comment.layerModeNext").translatedName("litematica.config.hotkeys.name.layerModeNext");
    public static final ConfigHotkey LAYER_MODE_PREVIOUS                = new ConfigHotkey("layerModePrevious",                 "M,PAGE_DOWN",  "litematica.config.hotkeys.comment.layerModePrevious").translatedName("litematica.config.hotkeys.name.layerModePrevious");
    public static final ConfigHotkey LAYER_NEXT                         = new ConfigHotkey("layerNext",                         "PAGE_UP",      "litematica.config.hotkeys.comment.layerNext").translatedName("litematica.config.hotkeys.name.layerNext");
    public static final ConfigHotkey LAYER_PREVIOUS                     = new ConfigHotkey("layerPrevious",                     "PAGE_DOWN",    "litematica.config.hotkeys.comment.layerPrevious").translatedName("litematica.config.hotkeys.name.layerPrevious");
    public static final ConfigHotkey LAYER_SET_HERE                     = new ConfigHotkey("layerSetHere",                      "",     "litematica.config.hotkeys.comment.layerSetHere").translatedName("litematica.config.hotkeys.name.layerSetHere");
    public static final ConfigHotkey NUDGE_SELECTION_NEGATIVE           = new ConfigHotkey("nudgeSelectionNegative",            "",     "litematica.config.hotkeys.comment.nudgeSelectionNegative").translatedName("litematica.config.hotkeys.name.nudgeSelectionNegative");
    public static final ConfigHotkey NUDGE_SELECTION_POSITIVE           = new ConfigHotkey("nudgeSelectionPositive",            "",     "litematica.config.hotkeys.comment.nudgeSelectionPositive").translatedName("litematica.config.hotkeys.name.nudgeSelectionPositive");
    public static final ConfigHotkey MOVE_ENTIRE_SELECTION              = new ConfigHotkey("moveEntireSelection",               "",     "litematica.config.hotkeys.comment.moveEntireSelection").translatedName("litematica.config.hotkeys.name.moveEntireSelection");
    public static final ConfigHotkey OPEN_GUI_AREA_SETTINGS             = new ConfigHotkey("openGuiAreaSettings",               "KP_MULTIPLY", "litematica.config.hotkeys.comment.openGuiAreaSettings").translatedName("litematica.config.hotkeys.name.openGuiAreaSettings");
    public static final ConfigHotkey OPEN_GUI_LOADED_SCHEMATICS         = new ConfigHotkey("openGuiLoadedSchematics",           "",     "litematica.config.hotkeys.comment.openGuiLoadedSchematics").translatedName("litematica.config.hotkeys.name.openGuiLoadedSchematics");
    public static final ConfigHotkey OPEN_GUI_MAIN_MENU                 = new ConfigHotkey("openGuiMainMenu",                   "M",    KeybindSettings.RELEASE_EXCLUSIVE, "litematica.config.hotkeys.comment.openGuiMainMenu").translatedName("litematica.config.hotkeys.name.openGuiMainMenu");
    public static final ConfigHotkey OPEN_GUI_MATERIAL_LIST             = new ConfigHotkey("openGuiMaterialList",               "M,L",  "litematica.config.hotkeys.comment.openGuiMaterialList").translatedName("litematica.config.hotkeys.name.openGuiMaterialList");
    public static final ConfigHotkey OPEN_GUI_PLACEMENT_SETTINGS        = new ConfigHotkey("openGuiPlacementSettings",          "KP_SUBTRACT", "litematica.config.hotkeys.comment.openGuiPlacementSettings").translatedName("litematica.config.hotkeys.name.openGuiPlacementSettings");
    public static final ConfigHotkey OPEN_GUI_SCHEMATIC_PLACEMENTS      = new ConfigHotkey("openGuiSchematicPlacements",        "M,P",  "litematica.config.hotkeys.comment.openGuiSchematicPlacements").translatedName("litematica.config.hotkeys.name.openGuiSchematicPlacements");
    public static final ConfigHotkey OPEN_GUI_SCHEMATIC_PROJECTS        = new ConfigHotkey("openGuiSchematicProjects",          "",     "litematica.config.hotkeys.comment.openGuiSchematicProjects").translatedName("litematica.config.hotkeys.name.openGuiSchematicProjects");
    public static final ConfigHotkey OPEN_GUI_SCHEMATIC_VERIFIER        = new ConfigHotkey("openGuiSchematicVerifier",          "M,V",  "litematica.config.hotkeys.comment.openGuiSchematicVerifier").translatedName("litematica.config.hotkeys.name.openGuiSchematicVerifier");
    public static final ConfigHotkey OPEN_GUI_SELECTION_MANAGER         = new ConfigHotkey("openGuiSelectionManager",           "M,S",  "litematica.config.hotkeys.comment.openGuiSelectionManager").translatedName("litematica.config.hotkeys.name.openGuiSelectionManager");
    public static final ConfigHotkey OPEN_GUI_SETTINGS                  = new ConfigHotkey("openGuiSettings",                   "M,C",  "litematica.config.hotkeys.comment.openGuiSettings").translatedName("litematica.config.hotkeys.name.openGuiSettings");
    public static final ConfigHotkey OPERATION_MODE_CHANGE_MODIFIER     = new ConfigHotkey("operationModeChangeModifier",       "LEFT_CONTROL", KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.operationModeChangeModifier").translatedName("litematica.config.hotkeys.name.operationModeChangeModifier");
    public static final ConfigHotkey PICK_BLOCK_FIRST                   = new ConfigHotkey("pickBlockFirst",                    "BUTTON_3",     KeybindSettings.PRESS_ALLOWEXTRA, "litematica.config.hotkeys.comment.pickBlockFirst").translatedName("litematica.config.hotkeys.name.pickBlockFirst");
    public static final ConfigHotkey PICK_BLOCK_LAST                    = new ConfigHotkey("pickBlockLast",                     "",             KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.pickBlockLast").translatedName("litematica.config.hotkeys.name.pickBlockLast");
    public static final ConfigHotkey PICK_BLOCK_TOGGLE                  = new ConfigHotkey("pickBlockToggle",                   "M,BUTTON_3",   "litematica.config.hotkeys.comment.pickBlockToggle").translatedName("litematica.config.hotkeys.name.pickBlockToggle");
    public static final ConfigHotkey RENDER_INFO_OVERLAY                = new ConfigHotkey("renderInfoOverlay",                 "I",            KeybindSettings.PRESS_ALLOWEXTRA, "litematica.config.hotkeys.comment.renderInfoOverlay").translatedName("litematica.config.hotkeys.name.renderInfoOverlay");
    public static final ConfigHotkey RENDER_OVERLAY_THROUGH_BLOCKS      = new ConfigHotkey("renderOverlayThroughBlocks",        "RIGHT_CONTROL", KeybindSettings.PRESS_ALLOWEXTRA, "litematica.config.hotkeys.comment.renderOverlayThroughBlocks").translatedName("litematica.config.hotkeys.name.renderOverlayThroughBlocks");
    public static final ConfigHotkey RERENDER_SCHEMATIC                 = new ConfigHotkey("rerenderSchematic",                 "F3,M", "litematica.config.hotkeys.comment.rerenderSchematic").translatedName("litematica.config.hotkeys.name.rerenderSchematic");
    public static final ConfigHotkey SAVE_AREA_AS_IN_MEMORY_SCHEMATIC   = new ConfigHotkey("saveAreaAsInMemorySchematic",       "",     "litematica.config.hotkeys.comment.saveAreaAsInMemorySchematic").translatedName("litematica.config.hotkeys.name.saveAreaAsInMemorySchematic");
    public static final ConfigHotkey SAVE_AREA_AS_SCHEMATIC_TO_FILE     = new ConfigHotkey("saveAreaAsSchematicToFile",         "LEFT_CONTROL,LEFT_ALT,S",  "litematica.config.hotkeys.comment.saveAreaAsSchematicToFile").translatedName("litematica.config.hotkeys.name.saveAreaAsSchematicToFile");
    public static final ConfigHotkey SCHEMATIC_EDIT_BREAK_ALL_EXCEPT    = new ConfigHotkey("schematicEditBreakAllExcept",       "", KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.schematicEditBreakAllExcept").translatedName("litematica.config.hotkeys.name.schematicEditBreakAllExcept");
    public static final ConfigHotkey SCHEMATIC_EDIT_BREAK_ALL           = new ConfigHotkey("schematicEditBreakPlaceAll",        "", KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.schematicEditBreakPlaceAll").translatedName("litematica.config.hotkeys.name.schematicEditBreakPlaceAll");
    public static final ConfigHotkey SCHEMATIC_EDIT_BREAK_DIRECTION     = new ConfigHotkey("schematicEditBreakPlaceDirection",  "", KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.schematicEditBreakPlaceDirection").translatedName("litematica.config.hotkeys.name.schematicEditBreakPlaceDirection");
    public static final ConfigHotkey SCHEMATIC_EDIT_REPLACE_ALL         = new ConfigHotkey("schematicEditReplaceAll",           "", KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.schematicEditReplaceAll").translatedName("litematica.config.hotkeys.name.schematicEditReplaceAll");
    public static final ConfigHotkey SCHEMATIC_EDIT_REPLACE_BLOCK       = new ConfigHotkey("schematicEditReplaceBlock",         "", KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.schematicEditReplaceBlock").translatedName("litematica.config.hotkeys.name.schematicEditReplaceBlock");
    public static final ConfigHotkey SCHEMATIC_EDIT_REPLACE_DIRECTION   = new ConfigHotkey("schematicEditReplaceDirection",     "", KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.schematicEditReplaceDirection").translatedName("litematica.config.hotkeys.name.schematicEditReplaceDirection");
    public static final ConfigHotkey SCHEMATIC_PLACEMENT_ROTATION       = new ConfigHotkey("schematicPlacementRotation",        "",     KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.schematicPlacementRotation").translatedName("litematica.config.hotkeys.name.schematicPlacementRotation");
    public static final ConfigHotkey SCHEMATIC_PLACEMENT_MIRROR         = new ConfigHotkey("schematicPlacementMirror",          "",     KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.schematicPlacementMirror").translatedName("litematica.config.hotkeys.name.schematicPlacementMirror");
    public static final ConfigHotkey SCHEMATIC_VERSION_CYCLE_MODIFIER   = new ConfigHotkey("schematicVersionCycleModifier",     "",     KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.schematicVersionCycleModifier").translatedName("litematica.config.hotkeys.name.schematicVersionCycleModifier");
    public static final ConfigHotkey SCHEMATIC_VERSION_CYCLE_NEXT       = new ConfigHotkey("schematicVersionCycleNext",         "",     "litematica.config.hotkeys.comment.schematicVersionCycleNext").translatedName("litematica.config.hotkeys.name.schematicVersionCycleNext");
    public static final ConfigHotkey SCHEMATIC_VERSION_CYCLE_PREVIOUS   = new ConfigHotkey("schematicVersionCyclePrevious",     "",     "litematica.config.hotkeys.comment.schematicVersionCyclePrevious").translatedName("litematica.config.hotkeys.name.schematicVersionCyclePrevious");
    public static final ConfigHotkey SELECTION_GRAB_MODIFIER            = new ConfigHotkey("selectionGrabModifier",             "",     KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.selectionGrabModifier").translatedName("litematica.config.hotkeys.name.selectionGrabModifier");
    public static final ConfigHotkey SELECTION_GROW_HOTKEY              = new ConfigHotkey("selectionGrow",                     "",     "litematica.config.hotkeys.comment.selectionGrow").translatedName("litematica.config.hotkeys.name.selectionGrow");
    public static final ConfigHotkey SELECTION_GROW_MODIFIER            = new ConfigHotkey("selectionGrowModifier",             "",     KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.selectionGrowModifier").translatedName("litematica.config.hotkeys.name.selectionGrowModifier");
    public static final ConfigHotkey SELECTION_NUDGE_MODIFIER           = new ConfigHotkey("selectionNudgeModifier",            "LEFT_ALT", KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.selectionNudgeModifier").translatedName("litematica.config.hotkeys.name.selectionNudgeModifier");
    public static final ConfigHotkey SELECTION_MODE_CYCLE               = new ConfigHotkey("selectionModeCycle",                "LEFT_CONTROL,M", "litematica.config.hotkeys.comment.selectionModeCycle").translatedName("litematica.config.hotkeys.name.selectionModeCycle");
    public static final ConfigHotkey SELECTION_SHRINK_HOTKEY            = new ConfigHotkey("selectionShrink",                   "",     "litematica.config.hotkeys.comment.selectionShrink").translatedName("litematica.config.hotkeys.name.selectionShrink");
    public static final ConfigHotkey SET_AREA_ORIGIN                    = new ConfigHotkey("setAreaOrigin",                     "",     "litematica.config.hotkeys.comment.setAreaOrigin").translatedName("litematica.config.hotkeys.name.setAreaOrigin");
    public static final ConfigHotkey SET_SELECTION_BOX_POSITION_1       = new ConfigHotkey("setSelectionBoxPosition1",          "",     "litematica.config.hotkeys.comment.setSelectionBoxPosition1").translatedName("litematica.config.hotkeys.name.setSelectionBoxPosition1");
    public static final ConfigHotkey SET_SELECTION_BOX_POSITION_2       = new ConfigHotkey("setSelectionBoxPosition2",          "",     "litematica.config.hotkeys.comment.setSelectionBoxPosition2").translatedName("litematica.config.hotkeys.name.setSelectionBoxPosition2");
    public static final ConfigHotkey TOGGLE_ALL_RENDERING               = new ConfigHotkey("toggleAllRendering",                "M,R",  "litematica.config.hotkeys.comment.toggleAllRendering", "litematica.config.hotkeys.prettyName.toggleAllRendering").translatedName("litematica.config.hotkeys.name.toggleAllRendering");
    public static final ConfigHotkey TOGGLE_AREA_SELECTION_RENDERING    = new ConfigHotkey("toggleAreaSelectionBoxesRendering", "",     "litematica.config.hotkeys.comment.toggleAreaSelectionBoxesRendering").translatedName("litematica.config.hotkeys.name.toggleAreaSelectionBoxesRendering");
    public static final ConfigHotkey TOGGLE_SCHEMATIC_RENDERING         = new ConfigHotkey("toggleSchematicRendering",          "M,G",  "litematica.config.hotkeys.comment.toggleSchematicRendering").translatedName("litematica.config.hotkeys.name.toggleSchematicRendering");
    public static final ConfigHotkey TOGGLE_INFO_OVERLAY_RENDERING      = new ConfigHotkey("toggleInfoOverlayRendering",        "",     "litematica.config.hotkeys.comment.toggleInfoOverlayRendering").translatedName("litematica.config.hotkeys.name.toggleInfoOverlayRendering");
    public static final ConfigHotkey TOGGLE_OVERLAY_RENDERING           = new ConfigHotkey("toggleOverlayRendering",            "",     "litematica.config.hotkeys.comment.toggleOverlayRendering").translatedName("litematica.config.hotkeys.name.toggleOverlayRendering");
    public static final ConfigHotkey TOGGLE_OVERLAY_OUTLINE_RENDERING   = new ConfigHotkey("toggleOverlayOutlineRendering",     "",     "litematica.config.hotkeys.comment.toggleOverlayOutlineRendering").translatedName("litematica.config.hotkeys.name.toggleOverlayOutlineRendering");
    public static final ConfigHotkey TOGGLE_OVERLAY_SIDE_RENDERING      = new ConfigHotkey("toggleOverlaySideRendering",        "",     "litematica.config.hotkeys.comment.toggleOverlaySideRendering").translatedName("litematica.config.hotkeys.name.toggleOverlaySideRendering");
    public static final ConfigHotkey TOGGLE_PLACEMENT_BOXES_RENDERING   = new ConfigHotkey("togglePlacementBoxesRendering",     "",     "litematica.config.hotkeys.comment.togglePlacementBoxesRendering").translatedName("litematica.config.hotkeys.name.togglePlacementBoxesRendering");
    public static final ConfigHotkey TOGGLE_PLACEMENT_RESTRICTION       = new ConfigHotkey("togglePlacementRestriction",        "",     "litematica.config.hotkeys.comment.togglePlacementRestriction").translatedName("litematica.config.hotkeys.name.togglePlacementRestriction");
    public static final ConfigHotkey TOGGLE_SCHEMATIC_BLOCK_RENDERING   = new ConfigHotkey("toggleSchematicBlockRendering",     "",     "litematica.config.hotkeys.comment.toggleSchematicBlockRendering").translatedName("litematica.config.hotkeys.name.toggleSchematicBlockRendering");
    public static final ConfigHotkey TOGGLE_SIGN_TEXT_PASTE             = new ConfigHotkey("toggleSignTextPaste",               "",     "litematica.config.hotkeys.comment.toggleSignTextPaste").translatedName("litematica.config.hotkeys.name.toggleSignTextPaste");
    public static final ConfigHotkey TOGGLE_TRANSLUCENT_RENDERING       = new ConfigHotkey("toggleTranslucentRendering",        "",     "litematica.config.hotkeys.comment.toggleTranslucentRendering").translatedName("litematica.config.hotkeys.name.toggleTranslucentRendering");
    public static final ConfigHotkey TOGGLE_VERIFIER_OVERLAY_RENDERING  = new ConfigHotkey("toggleVerifierOverlayRendering",    "",     "litematica.config.hotkeys.comment.toggleVerifierOverlayRendering").translatedName("litematica.config.hotkeys.name.toggleVerifierOverlayRendering");
    public static final ConfigHotkey TOOL_ENABLED_TOGGLE                = new ConfigHotkey("toolEnabledToggle",                 "M,T",  "litematica.config.hotkeys.comment.toolEnabledToggle").translatedName("litematica.config.hotkeys.name.toolEnabledToggle");
    public static final ConfigHotkey TOOL_PLACE_CORNER_1                = new ConfigHotkey("toolPlaceCorner1",                  "BUTTON_1", KeybindSettings.PRESS_ALLOWEXTRA, "litematica.config.hotkeys.comment.toolPlaceCorner1").translatedName("litematica.config.hotkeys.name.toolPlaceCorner1");
    public static final ConfigHotkey TOOL_PLACE_CORNER_2                = new ConfigHotkey("toolPlaceCorner2",                  "BUTTON_2", KeybindSettings.PRESS_ALLOWEXTRA, "litematica.config.hotkeys.comment.toolPlaceCorner2").translatedName("litematica.config.hotkeys.name.toolPlaceCorner2");
    public static final ConfigHotkey TOOL_SELECT_ELEMENTS               = new ConfigHotkey("toolSelectElements",                "BUTTON_3", KeybindSettings.PRESS_ALLOWEXTRA, "litematica.config.hotkeys.comment.toolSelectElements").translatedName("litematica.config.hotkeys.name.toolSelectElements");
    public static final ConfigHotkey TOOL_SELECT_MODIFIER_BLOCK_1       = new ConfigHotkey("toolSelectModifierBlock1",          "LEFT_ALT", KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.toolSelectModifierBlock1").translatedName("litematica.config.hotkeys.name.toolSelectModifierBlock1");
    public static final ConfigHotkey TOOL_SELECT_MODIFIER_BLOCK_2       = new ConfigHotkey("toolSelectModifierBlock2",          "LEFT_SHIFT", KeybindSettings.MODIFIER_INGAME, "litematica.config.hotkeys.comment.toolSelectModifierBlock2").translatedName("litematica.config.hotkeys.name.toolSelectModifierBlock2");
    public static final ConfigHotkey UNLOAD_CURRENT_SCHEMATIC           = new ConfigHotkey("unloadCurrentSchematic",            "",     "litematica.config.hotkeys.comment.unloadCurrentSchematic").translatedName("litematica.config.hotkeys.name.unloadCurrentSchematic");

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            ADD_SELECTION_BOX,
            CLONE_SELECTION,
            DELETE_SELECTION_BOX,
            EASY_PLACE_ACTIVATION,
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
            SCHEMATIC_PLACEMENT_ROTATION,
            SCHEMATIC_PLACEMENT_MIRROR,
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
