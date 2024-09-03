package fi.dy.masa.litematica.config;

import java.io.File;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.MinecraftClient;

import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.MessageOutputType;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.CornerSelectionMode;
import fi.dy.masa.litematica.util.*;

public class Configs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

    private static final String GENERIC_KEY = Reference.MOD_ID+".config.generic";
    private static final String VISUALS_KEY = Reference.MOD_ID+".config.visuals";
    private static final String INFO_OVERLAYS_KEY = Reference.MOD_ID+".config.info_overlays";
    private static final String COLORS_KEY = Reference.MOD_ID+".config.colors";

    public static class Generic
    {
        public static final ConfigOptionList    EASY_PLACE_PROTOCOL         = new ConfigOptionList("easyPlaceProtocolVersion", EasyPlaceProtocol.AUTO).apply(GENERIC_KEY);
        public static final ConfigOptionList    PASTE_NBT_BEHAVIOR          = new ConfigOptionList("pasteNbtRestoreBehavior", PasteNbtBehavior.NONE).apply(GENERIC_KEY);
        public static final ConfigOptionList    PASTE_REPLACE_BEHAVIOR      = new ConfigOptionList("pasteReplaceBehavior", ReplaceBehavior.NONE).apply(GENERIC_KEY);
        public static final ConfigOptionList    PLACEMENT_REPLACE_BEHAVIOR  = new ConfigOptionList("placementReplaceBehavior", ReplaceBehavior.ALL).apply(GENERIC_KEY);
        public static final ConfigOptionList    PLACEMENT_RESTRICTION_WARN  = new ConfigOptionList("placementRestrictionWarn", MessageOutputType.ACTIONBAR).apply(GENERIC_KEY);
        public static final ConfigOptionList    SELECTION_CORNERS_MODE      = new ConfigOptionList("selectionCornersMode", CornerSelectionMode.CORNERS).apply(GENERIC_KEY);

        public static final ConfigBoolean       CUSTOM_SCHEMATIC_BASE_DIRECTORY_ENABLED = new ConfigBoolean("customSchematicBaseDirectoryEnabled", false).apply(GENERIC_KEY);
        public static final ConfigString        CUSTOM_SCHEMATIC_BASE_DIRECTORY         = new ConfigString( "customSchematicBaseDirectory", DataManager.getDefaultBaseSchematicDirectory().getAbsolutePath()).apply(GENERIC_KEY);

        public static final ConfigBoolean       AREAS_PER_WORLD             = new ConfigBoolean("areaSelectionsPerWorld", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       BETTER_RENDER_ORDER         = new ConfigBoolean("betterRenderOrder", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       CHANGE_SELECTED_CORNER      = new ConfigBoolean("changeSelectedCornerOnMove", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       CLONE_AT_ORIGINAL_POS       = new ConfigBoolean("cloneAtOriginalPosition", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       COMMAND_DISABLE_FEEDBACK    = new ConfigBoolean("commandDisableFeedback", true).apply(GENERIC_KEY);
        public static final ConfigInteger       COMMAND_FILL_MAX_VOLUME     = new ConfigInteger("commandFillMaxVolume", 32768, 256, 10000000).apply(GENERIC_KEY);
        public static final ConfigBoolean       COMMAND_FILL_NO_CHUNK_CLAMP = new ConfigBoolean("commandFillNoChunkClamp", false).apply(GENERIC_KEY);
        public static final ConfigInteger       COMMAND_LIMIT               = new ConfigInteger("commandLimitPerTick", 24, 1, 256).apply(GENERIC_KEY);
        public static final ConfigString        COMMAND_NAME_CLONE          = new ConfigString( "commandNameClone", "clone").apply(GENERIC_KEY);
        public static final ConfigString        COMMAND_NAME_FILL           = new ConfigString( "commandNameFill", "fill").apply(GENERIC_KEY);
        public static final ConfigString        COMMAND_NAME_SETBLOCK       = new ConfigString( "commandNameSetblock", "setblock").apply(GENERIC_KEY);
        public static final ConfigString        COMMAND_NAME_SUMMON         = new ConfigString( "commandNameSummon", "summon").apply(GENERIC_KEY);
        public static final ConfigInteger       COMMAND_TASK_INTERVAL       = new ConfigInteger("commandTaskInterval", 1, 1, 1000).apply(GENERIC_KEY);
        public static final ConfigBoolean       COMMAND_USE_WORLDEDIT       = new ConfigBoolean("commandUseWorldEdit", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       DEBUG_LOGGING               = new ConfigBoolean("debugLogging", false).apply(GENERIC_KEY);
        public static final ConfigOptionList    DATAFIXER_MODE              = new ConfigOptionList("datafixerMode", DataFixerMode.ALWAYS).apply(GENERIC_KEY);
        public static final ConfigInteger       DATAFIXER_DEFAULT_SCHEMA    = new ConfigInteger("datafixerDefaultSchema", 1139, 99, 2724, true).apply(GENERIC_KEY);
        public static final ConfigBoolean       EASY_PLACE_FIRST            = new ConfigBoolean("easyPlaceFirst", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       EASY_PLACE_HOLD_ENABLED     = new ConfigBoolean("easyPlaceHoldEnabled", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       EASY_PLACE_MODE             = new ConfigBoolean("easyPlaceMode", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       EASY_PLACE_SP_HANDLING      = new ConfigBoolean("easyPlaceSinglePlayerHandling", true).apply(GENERIC_KEY);
        public static final ConfigInteger       EASY_PLACE_SWAP_INTERVAL    = new ConfigInteger("easyPlaceSwapInterval", 0, 0, 10000).apply(GENERIC_KEY);
        public static final ConfigBoolean       EASY_PLACE_SWING_HAND       = new ConfigBoolean("easyPlaceSwingHand", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       EASY_PLACE_VANILLA_REACH    = new ConfigBoolean("easyPlaceVanillaReach", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       ENTITY_DATA_SYNC            = new ConfigBoolean("entityDataSync", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       ENTITY_DATA_SYNC_BACKUP     = new ConfigBoolean("entityDataSyncBackup", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       EXECUTE_REQUIRE_TOOL        = new ConfigBoolean("executeRequireHoldingTool", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       FIX_CHEST_MIRROR            = new ConfigBoolean("fixChestMirror", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       FIX_RAIL_ROTATION           = new ConfigBoolean("fixRailRotation", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       GENERATE_LOWERCASE_NAMES    = new ConfigBoolean("generateLowercaseNames", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       HIGHLIGHT_BLOCK_IN_INV      = new ConfigBoolean("highlightBlockInInventory", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       ITEM_USE_PACKET_CHECK_BYPASS= new ConfigBoolean("itemUsePacketCheckBypass", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       LAYER_MODE_DYNAMIC          = new ConfigBoolean("layerModeFollowsPlayer", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       LOAD_ENTIRE_SCHEMATICS      = new ConfigBoolean("loadEntireSchematics", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       MATERIAL_LIST_IGNORE_STATE  = new ConfigBoolean("materialListIgnoreState", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       PASTE_ALWAYS_USE_FILL       = new ConfigBoolean("pasteAlwaysUseFill", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       PASTE_IGNORE_BE_ENTIRELY    = new ConfigBoolean("pasteIgnoreBlockEntitiesEntirely", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       PASTE_IGNORE_BE_IN_FILL     = new ConfigBoolean("pasteIgnoreBlockEntitiesFromFill", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       PASTE_IGNORE_CMD_LIMIT      = new ConfigBoolean("pasteIgnoreCommandLimitWithNbtRestore", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       PASTE_IGNORE_ENTITIES       = new ConfigBoolean("pasteIgnoreEntities", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       PASTE_IGNORE_INVENTORY      = new ConfigBoolean("pasteIgnoreInventories", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       PASTE_TO_MCFUNCTION         = new ConfigBoolean("pasteToMcFunctionFiles", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       PASTE_USE_FILL_COMMAND      = new ConfigBoolean("pasteUseFillCommand", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       PASTE_USING_COMMANDS_IN_SP  = new ConfigBoolean("pasteUsingCommandsInSp", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       PASTE_USING_SERVUX          = new ConfigBoolean("pasteUsingServux", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       PICK_BLOCK_AVOID_DAMAGEABLE = new ConfigBoolean("pickBlockAvoidDamageable", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       PICK_BLOCK_AVOID_TOOLS      = new ConfigBoolean("pickBlockAvoidTools", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       PICK_BLOCK_ENABLED          = new ConfigBoolean("pickBlockEnabled", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       PICK_BLOCK_SHULKERS         = new ConfigBoolean("pickBlockShulkers", false).apply(GENERIC_KEY);
        public static final ConfigString        PICK_BLOCKABLE_SLOTS        = new ConfigString( "pickBlockableSlots", "1,2,3,4,5").apply(GENERIC_KEY);
        public static final ConfigBoolean       PLACEMENT_RESTRICTION       = new ConfigBoolean("placementRestriction", false).apply(GENERIC_KEY);
        public static final ConfigBoolean       RENDER_MATERIALS_IN_GUI     = new ConfigBoolean("renderMaterialListInGuis", true).apply(GENERIC_KEY);
        public static final ConfigBoolean       RENDER_THREAD_NO_TIMEOUT    = new ConfigBoolean("renderThreadNoTimeout", true).apply(GENERIC_KEY);
        public static final ConfigInteger       SERVER_NBT_REQUEST_RATE     = new ConfigInteger("serverNbtRequestRate", 2).apply(GENERIC_KEY);
        public static final ConfigBoolean       SIGN_TEXT_PASTE             = new ConfigBoolean("signTextPaste", true).apply(GENERIC_KEY);
        public static final ConfigString        TOOL_ITEM                   = new ConfigString( "toolItem", "minecraft:stick").apply(GENERIC_KEY);
        public static final ConfigBoolean       TOOL_ITEM_ENABLED           = new ConfigBoolean("toolItemEnabled", true).apply(GENERIC_KEY);
        public static final ConfigString        TOOL_ITEM_COMPONENTS        = new ConfigString( "toolItemComponents", "empty").apply(GENERIC_KEY);
        public static final ConfigBoolean       UNHIDE_SCHEMATIC_PROJECTS   = new ConfigBoolean("unhideSchematicVCS", false).apply(GENERIC_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                AREAS_PER_WORLD,
                //BETTER_RENDER_ORDER,
                CHANGE_SELECTED_CORNER,
                CLONE_AT_ORIGINAL_POS,
                COMMAND_DISABLE_FEEDBACK,
                COMMAND_FILL_NO_CHUNK_CLAMP,
                COMMAND_USE_WORLDEDIT,
                CUSTOM_SCHEMATIC_BASE_DIRECTORY_ENABLED,
                DEBUG_LOGGING,
                DATAFIXER_MODE,
                DATAFIXER_DEFAULT_SCHEMA,
                EASY_PLACE_FIRST,
                EASY_PLACE_HOLD_ENABLED,
                EASY_PLACE_MODE,
                EASY_PLACE_SP_HANDLING,
                EASY_PLACE_PROTOCOL,
                EASY_PLACE_SWING_HAND,
                EASY_PLACE_VANILLA_REACH,
                ENTITY_DATA_SYNC,
                ENTITY_DATA_SYNC_BACKUP,
                EXECUTE_REQUIRE_TOOL,
                FIX_CHEST_MIRROR,
                FIX_RAIL_ROTATION,
                GENERATE_LOWERCASE_NAMES,
                HIGHLIGHT_BLOCK_IN_INV,
                ITEM_USE_PACKET_CHECK_BYPASS,
                LAYER_MODE_DYNAMIC,
                //LOAD_ENTIRE_SCHEMATICS,
                MATERIAL_LIST_IGNORE_STATE,
                PASTE_ALWAYS_USE_FILL,
                PASTE_IGNORE_BE_ENTIRELY,
                PASTE_IGNORE_BE_IN_FILL,
                PASTE_IGNORE_CMD_LIMIT,
                PASTE_IGNORE_ENTITIES,
                PASTE_IGNORE_INVENTORY,
                PASTE_NBT_BEHAVIOR,
                PASTE_TO_MCFUNCTION,
                PASTE_USE_FILL_COMMAND,
                PASTE_USING_COMMANDS_IN_SP,
                PASTE_USING_SERVUX,
                PICK_BLOCK_AVOID_DAMAGEABLE,
                PICK_BLOCK_AVOID_TOOLS,
                PICK_BLOCK_ENABLED,
                PICK_BLOCK_SHULKERS,
                PLACEMENT_REPLACE_BEHAVIOR,
                PLACEMENT_RESTRICTION,
                PLACEMENT_RESTRICTION_WARN,
                RENDER_MATERIALS_IN_GUI,
                RENDER_THREAD_NO_TIMEOUT,
                SERVER_NBT_REQUEST_RATE,
                SIGN_TEXT_PASTE,
                TOOL_ITEM_ENABLED,
                UNHIDE_SCHEMATIC_PROJECTS,

                PASTE_REPLACE_BEHAVIOR,
                SELECTION_CORNERS_MODE,

                COMMAND_FILL_MAX_VOLUME,
                COMMAND_LIMIT,
                COMMAND_NAME_CLONE,
                COMMAND_NAME_FILL,
                COMMAND_NAME_SETBLOCK,
                COMMAND_NAME_SUMMON,
                COMMAND_TASK_INTERVAL,
                CUSTOM_SCHEMATIC_BASE_DIRECTORY,
                EASY_PLACE_SWAP_INTERVAL,
                PICK_BLOCKABLE_SLOTS,
                TOOL_ITEM,
                TOOL_ITEM_COMPONENTS
        );
    }

    public static class Visuals
    {
        public static final ConfigBoolean       ENABLE_AREA_SELECTION_RENDERING     = new ConfigBoolean("enableAreaSelectionBoxesRendering", true).apply(VISUALS_KEY);
        public static final ConfigBoolean       ENABLE_PLACEMENT_BOXES_RENDERING    = new ConfigBoolean("enablePlacementBoxesRendering", true).apply(VISUALS_KEY);
        public static final ConfigBoolean       ENABLE_RENDERING                    = new ConfigBoolean("enableRendering", true).apply(VISUALS_KEY);
        public static final ConfigBoolean       ENABLE_SCHEMATIC_BLOCKS             = new ConfigBoolean("enableSchematicBlocksRendering",  true).apply(VISUALS_KEY);
        public static final ConfigBoolean       ENABLE_SCHEMATIC_FLUIDS             = new ConfigBoolean("enableSchematicFluidRendering", true).apply(VISUALS_KEY);
        public static final ConfigBoolean       ENABLE_SCHEMATIC_OVERLAY            = new ConfigBoolean("enableSchematicOverlay",  true).apply(VISUALS_KEY);
        public static final ConfigBoolean       ENABLE_SCHEMATIC_RENDERING          = new ConfigBoolean("enableSchematicRendering", true).apply(VISUALS_KEY);
        //public static final ConfigInteger       RENDER_SCHEMATIC_MAX_THREADS        = new ConfigInteger("renderSchematicMaxThreads", 4, 1, 16).apply(VISUALS_KEY);
        public static final ConfigDouble        GHOST_BLOCK_ALPHA                   = new ConfigDouble( "ghostBlockAlpha", 0.5, 0, 1).apply(VISUALS_KEY);
        public static final ConfigBoolean       IGNORE_EXISTING_FLUIDS              = new ConfigBoolean("ignoreExistingFluids", false).apply(VISUALS_KEY);
        public static final ConfigBoolean       OVERLAY_REDUCED_INNER_SIDES         = new ConfigBoolean("overlayReducedInnerSides", false).apply(VISUALS_KEY);
        public static final ConfigDouble        PLACEMENT_BOX_SIDE_ALPHA            = new ConfigDouble( "placementBoxSideAlpha", 0.2, 0, 1).apply(VISUALS_KEY);
        public static final ConfigBoolean       RENDER_AREA_SELECTION_BOX_SIDES     = new ConfigBoolean("renderAreaSelectionBoxSides", true).apply(VISUALS_KEY);
        public static final ConfigBoolean       RENDER_BLOCKS_AS_TRANSLUCENT        = new ConfigBoolean("renderBlocksAsTranslucent", false).apply(VISUALS_KEY);
        public static final ConfigBoolean       RENDER_COLLIDING_SCHEMATIC_BLOCKS   = new ConfigBoolean("renderCollidingSchematicBlocks", false).apply(VISUALS_KEY);
        public static final ConfigBoolean       RENDER_ERROR_MARKER_CONNECTIONS     = new ConfigBoolean("renderErrorMarkerConnections", false).apply(VISUALS_KEY);
        public static final ConfigBoolean       RENDER_ERROR_MARKER_SIDES           = new ConfigBoolean("renderErrorMarkerSides", true).apply(VISUALS_KEY);
        public static final ConfigBoolean       RENDER_PLACEMENT_BOX_SIDES          = new ConfigBoolean("renderPlacementBoxSides", false).apply(VISUALS_KEY);
        public static final ConfigBoolean       RENDER_PLACEMENT_ENCLOSING_BOX      = new ConfigBoolean("renderPlacementEnclosingBox", true).apply(VISUALS_KEY);
        public static final ConfigBoolean       RENDER_PLACEMENT_ENCLOSING_BOX_SIDES= new ConfigBoolean("renderPlacementEnclosingBoxSides", false).apply(VISUALS_KEY);
        public static final ConfigBoolean       RENDER_TRANSLUCENT_INNER_SIDES      = new ConfigBoolean("renderTranslucentBlockInnerSides", false).apply(VISUALS_KEY);
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_ENABLE_OUTLINES   = new ConfigBoolean("schematicOverlayEnableOutlines",  true).apply(VISUALS_KEY);
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_ENABLE_RESORTING  = new ConfigBoolean("schematicOverlayEnableResorting",  false).apply(VISUALS_KEY);
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_ENABLE_SIDES      = new ConfigBoolean("schematicOverlayEnableSides",     true).apply(VISUALS_KEY);
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_MODEL_OUTLINE     = new ConfigBoolean("schematicOverlayModelOutline",    true).apply(VISUALS_KEY);
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_MODEL_SIDES       = new ConfigBoolean("schematicOverlayModelSides",      false).apply(VISUALS_KEY);
        public static final ConfigDouble        SCHEMATIC_OVERLAY_OUTLINE_WIDTH     = new ConfigDouble( "schematicOverlayOutlineWidth",  1.0, 0, 64).apply(VISUALS_KEY);
        public static final ConfigDouble        SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH = new ConfigDouble("schematicOverlayOutlineWidthThrough",  3.0, 0, 64).apply(VISUALS_KEY);
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_RENDER_THROUGH    = new ConfigBoolean("schematicOverlayRenderThroughBlocks", false).apply(VISUALS_KEY);
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_TYPE_EXTRA        = new ConfigBoolean("schematicOverlayTypeExtra",       true).apply(VISUALS_KEY);
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_TYPE_MISSING      = new ConfigBoolean("schematicOverlayTypeMissing",     true).apply(VISUALS_KEY);
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK  = new ConfigBoolean("schematicOverlayTypeWrongBlock",  true).apply(VISUALS_KEY);
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_TYPE_WRONG_STATE  = new ConfigBoolean("schematicOverlayTypeWrongState",  true).apply(VISUALS_KEY);
        public static final ConfigBoolean       SCHEMATIC_VERIFIER_BLOCK_MODELS     = new ConfigBoolean("schematicVerifierUseBlockModels", false).apply(VISUALS_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_RENDERING,
                ENABLE_SCHEMATIC_RENDERING,
                //RENDER_SCHEMATIC_MAX_THREADS,

                ENABLE_AREA_SELECTION_RENDERING,
                ENABLE_PLACEMENT_BOXES_RENDERING,
                ENABLE_SCHEMATIC_BLOCKS,
                ENABLE_SCHEMATIC_FLUIDS,
                ENABLE_SCHEMATIC_OVERLAY,
                IGNORE_EXISTING_FLUIDS,
                OVERLAY_REDUCED_INNER_SIDES,
                RENDER_AREA_SELECTION_BOX_SIDES,
                RENDER_BLOCKS_AS_TRANSLUCENT,
                RENDER_COLLIDING_SCHEMATIC_BLOCKS,
                RENDER_ERROR_MARKER_CONNECTIONS,
                RENDER_ERROR_MARKER_SIDES,
                RENDER_PLACEMENT_BOX_SIDES,
                RENDER_PLACEMENT_ENCLOSING_BOX,
                RENDER_PLACEMENT_ENCLOSING_BOX_SIDES,
                RENDER_TRANSLUCENT_INNER_SIDES,
                SCHEMATIC_OVERLAY_ENABLE_OUTLINES,
                SCHEMATIC_OVERLAY_ENABLE_RESORTING,
                SCHEMATIC_OVERLAY_ENABLE_SIDES,
                SCHEMATIC_OVERLAY_MODEL_OUTLINE,
                SCHEMATIC_OVERLAY_MODEL_SIDES,
                SCHEMATIC_OVERLAY_RENDER_THROUGH,
                SCHEMATIC_OVERLAY_TYPE_EXTRA,
                SCHEMATIC_OVERLAY_TYPE_MISSING,
                SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK,
                SCHEMATIC_OVERLAY_TYPE_WRONG_STATE,
                SCHEMATIC_VERIFIER_BLOCK_MODELS,

                GHOST_BLOCK_ALPHA,
                PLACEMENT_BOX_SIDE_ALPHA,
                SCHEMATIC_OVERLAY_OUTLINE_WIDTH,
                SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH
        );
    }

    public static class InfoOverlays
    {
        public static final ConfigOptionList    BLOCK_INFO_LINES_ALIGNMENT          = new ConfigOptionList("blockInfoLinesAlignment", HudAlignment.TOP_RIGHT).apply(INFO_OVERLAYS_KEY);
        public static final ConfigOptionList    BLOCK_INFO_OVERLAY_ALIGNMENT        = new ConfigOptionList("blockInfoOverlayAlignment", BlockInfoAlignment.TOP_CENTER).apply(INFO_OVERLAYS_KEY);
        public static final ConfigOptionList    INFO_HUD_ALIGNMENT                  = new ConfigOptionList("infoHudAlignment", HudAlignment.BOTTOM_RIGHT).apply(INFO_OVERLAYS_KEY);
        public static final ConfigOptionList    TOOL_HUD_ALIGNMENT                  = new ConfigOptionList("toolHudAlignment", HudAlignment.BOTTOM_LEFT).apply(INFO_OVERLAYS_KEY);

        public static final ConfigBoolean       BLOCK_INFO_LINES_ENABLED            = new ConfigBoolean("blockInfoLinesEnabled", true).apply(INFO_OVERLAYS_KEY);
        public static final ConfigDouble        BLOCK_INFO_LINES_FONT_SCALE         = new ConfigDouble( "blockInfoLinesFontScale", 0.5, 0, 10).apply(INFO_OVERLAYS_KEY);
        public static final ConfigInteger       BLOCK_INFO_LINES_OFFSET_X           = new ConfigInteger("blockInfoLinesOffsetX", 4, 0, 2000).apply(INFO_OVERLAYS_KEY);
        public static final ConfigInteger       BLOCK_INFO_LINES_OFFSET_Y           = new ConfigInteger("blockInfoLinesOffsetY", 4, 0, 2000).apply(INFO_OVERLAYS_KEY);
        public static final ConfigInteger       BLOCK_INFO_OVERLAY_OFFSET_Y         = new ConfigInteger("blockInfoOverlayOffsetY", 6, -2000, 2000).apply(INFO_OVERLAYS_KEY);
        public static final ConfigBoolean       BLOCK_INFO_OVERLAY_ENABLED          = new ConfigBoolean("blockInfoOverlayEnabled", true).apply(INFO_OVERLAYS_KEY);
        public static final ConfigInteger       INFO_HUD_MAX_LINES                  = new ConfigInteger("infoHudMaxLines", 10, 1, 128).apply(INFO_OVERLAYS_KEY);
        public static final ConfigInteger       INFO_HUD_OFFSET_X                   = new ConfigInteger("infoHudOffsetX", 1, 0, 32000).apply(INFO_OVERLAYS_KEY);
        public static final ConfigInteger       INFO_HUD_OFFSET_Y                   = new ConfigInteger("infoHudOffsetY", 1, 0, 32000).apply(INFO_OVERLAYS_KEY);
        public static final ConfigDouble        INFO_HUD_SCALE                      = new ConfigDouble( "infoHudScale", 1, 0.1, 4).apply(INFO_OVERLAYS_KEY);
        public static final ConfigBoolean       INFO_OVERLAYS_TARGET_FLUIDS         = new ConfigBoolean("infoOverlaysTargetFluids", false).apply(INFO_OVERLAYS_KEY);
        public static final ConfigInteger       MATERIAL_LIST_HUD_MAX_LINES         = new ConfigInteger("materialListHudMaxLines", 10, 1, 128).apply(INFO_OVERLAYS_KEY);
        public static final ConfigDouble        MATERIAL_LIST_HUD_SCALE             = new ConfigDouble( "materialListHudScale", 1, 0.1, 4).apply(INFO_OVERLAYS_KEY);
        public static final ConfigBoolean       STATUS_INFO_HUD                     = new ConfigBoolean("statusInfoHud", false).apply(INFO_OVERLAYS_KEY);
        public static final ConfigBoolean       STATUS_INFO_HUD_AUTO                = new ConfigBoolean("statusInfoHudAuto", true).apply(INFO_OVERLAYS_KEY);
        public static final ConfigInteger       TOOL_HUD_OFFSET_X                   = new ConfigInteger("toolHudOffsetX", 1, 0, 32000).apply(INFO_OVERLAYS_KEY);
        public static final ConfigInteger       TOOL_HUD_OFFSET_Y                   = new ConfigInteger("toolHudOffsetY", 1, 0, 32000).apply(INFO_OVERLAYS_KEY);
        public static final ConfigDouble        TOOL_HUD_SCALE                      = new ConfigDouble( "toolHudScale", 1, 0.1, 4).apply(INFO_OVERLAYS_KEY);
        public static final ConfigDouble        VERIFIER_ERROR_HILIGHT_ALPHA        = new ConfigDouble( "verifierErrorHilightAlpha", 0.2, 0, 1).apply(INFO_OVERLAYS_KEY);
        public static final ConfigInteger       VERIFIER_ERROR_HILIGHT_MAX_POSITIONS= new ConfigInteger("verifierErrorHilightMaxPositions", 1000, 1, 1000000).apply(INFO_OVERLAYS_KEY);
        public static final ConfigBoolean       VERIFIER_OVERLAY_ENABLED            = new ConfigBoolean("verifierOverlayEnabled", true).apply(INFO_OVERLAYS_KEY);
        public static final ConfigBoolean       WARN_DISABLED_RENDERING             = new ConfigBoolean("warnDisabledRendering", true).apply(INFO_OVERLAYS_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                BLOCK_INFO_LINES_ENABLED,
                BLOCK_INFO_OVERLAY_ENABLED,
                INFO_OVERLAYS_TARGET_FLUIDS,
                STATUS_INFO_HUD,
                STATUS_INFO_HUD_AUTO,
                VERIFIER_OVERLAY_ENABLED,
                WARN_DISABLED_RENDERING,

                BLOCK_INFO_LINES_ALIGNMENT,
                BLOCK_INFO_OVERLAY_ALIGNMENT,
                INFO_HUD_ALIGNMENT,
                TOOL_HUD_ALIGNMENT,

                BLOCK_INFO_LINES_OFFSET_X,
                BLOCK_INFO_LINES_OFFSET_Y,
                BLOCK_INFO_LINES_FONT_SCALE,
                BLOCK_INFO_OVERLAY_OFFSET_Y,
                INFO_HUD_MAX_LINES,
                INFO_HUD_OFFSET_X,
                INFO_HUD_OFFSET_Y,
                INFO_HUD_SCALE,
                MATERIAL_LIST_HUD_MAX_LINES,
                MATERIAL_LIST_HUD_SCALE,
                TOOL_HUD_OFFSET_X,
                TOOL_HUD_OFFSET_Y,
                TOOL_HUD_SCALE,
                VERIFIER_ERROR_HILIGHT_ALPHA,
                VERIFIER_ERROR_HILIGHT_MAX_POSITIONS
        );
    }

    public static class Colors
    {
        public static final ConfigColor AREA_SELECTION_BOX_SIDE_COLOR       = new ConfigColor("areaSelectionBoxSideColor",          "#30FFFFFF").apply(COLORS_KEY);
        public static final ConfigColor HIGHTLIGHT_BLOCK_IN_INV_COLOR       = new ConfigColor("hightlightBlockInInventoryColor",    "#30FF30FF").apply(COLORS_KEY);
        public static final ConfigColor MATERIAL_LIST_HUD_ITEM_COUNTS       = new ConfigColor("materialListHudItemCountsColor",     "#FFFFAA00").apply(COLORS_KEY);
        public static final ConfigColor REBUILD_BREAK_OVERLAY_COLOR         = new ConfigColor("schematicRebuildBreakPlaceOverlayColor", "#4C33CC33").apply(COLORS_KEY);
        public static final ConfigColor REBUILD_BREAK_EXCEPT_OVERLAY_COLOR  = new ConfigColor("schematicRebuildBreakExceptPlaceOverlayColor", "#4CF03030").apply(COLORS_KEY);
        public static final ConfigColor REBUILD_REPLACE_OVERLAY_COLOR       = new ConfigColor("schematicRebuildReplaceOverlayColor","#4CF0A010").apply(COLORS_KEY);
        public static final ConfigColor SCHEMATIC_OVERLAY_COLOR_EXTRA       = new ConfigColor("schematicOverlayColorExtra",         "#4CFF4CE6").apply(COLORS_KEY);
        public static final ConfigColor SCHEMATIC_OVERLAY_COLOR_MISSING     = new ConfigColor("schematicOverlayColorMissing",       "#2C33B3E6").apply(COLORS_KEY);
        public static final ConfigColor SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK = new ConfigColor("schematicOverlayColorWrongBlock",    "#4CFF3333").apply(COLORS_KEY);
        public static final ConfigColor SCHEMATIC_OVERLAY_COLOR_WRONG_STATE = new ConfigColor("schematicOverlayColorWrongState",    "#4CFF9010").apply(COLORS_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                AREA_SELECTION_BOX_SIDE_COLOR,
                HIGHTLIGHT_BLOCK_IN_INV_COLOR,
                MATERIAL_LIST_HUD_ITEM_COUNTS,
                REBUILD_BREAK_OVERLAY_COLOR,
                REBUILD_BREAK_EXCEPT_OVERLAY_COLOR,
                REBUILD_REPLACE_OVERLAY_COLOR,
                SCHEMATIC_OVERLAY_COLOR_EXTRA,
                SCHEMATIC_OVERLAY_COLOR_MISSING,
                SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK,
                SCHEMATIC_OVERLAY_COLOR_WRONG_STATE
        );
    }

    public static class Test
    {
        public static final ConfigBooleanHotkeyed EASY_PLACE_MODE           = new ConfigBooleanHotkeyed("easyPlaceMode",          false, "", "Easy Place Mode");
        public static final ConfigBooleanHotkeyed PLACEMENT_RESTRICTION     = new ConfigBooleanHotkeyed("placementRestriction",   false, "", "Placement Restriction");
        public static final ConfigBoolean         EASY_PLACE_CLICK_ADJACENT = new ConfigBoolean(        "easyPlaceClickAdjacent", false, "Easy Place Click Adjacent");
        public static final ConfigBoolean         EASY_PLACE_HOLD_ENABLED   = new ConfigBoolean(        "easyPlaceHold",          false, "Easy Place Hold Enabled");
        public static final ConfigBoolean         PICK_BLOCK_IGNORE_NBT     = new ConfigBoolean(        "pickBlockIgnoreNBT",     true, "Pick Block Ignore NBT");

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                EASY_PLACE_MODE,
                PLACEMENT_RESTRICTION,
                EASY_PLACE_CLICK_ADJACENT,
                EASY_PLACE_HOLD_ENABLED,
                PICK_BLOCK_IGNORE_NBT
        );
    }

    public static void loadFromFile()
    {
        File configFile = new File(FileUtils.getConfigDirectory(), CONFIG_FILE_NAME);

        if (configFile.exists() && configFile.isFile() && configFile.canRead())
        {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();

                ConfigUtils.readConfigBase(root, "Colors", Colors.OPTIONS);
                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
                ConfigUtils.readConfigBase(root, "InfoOverlays", InfoOverlays.OPTIONS);
                ConfigUtils.readConfigBase(root, "Visuals", Visuals.OPTIONS);
                ConfigUtils.readConfigBase(root, "Test", Test.OPTIONS);
            }
        }

        DataManager.setToolItem(Generic.TOOL_ITEM.getStringValue());
        if (MinecraftClient.getInstance().world != null)
        {
            DataManager.getInstance().setToolItemComponents(Generic.TOOL_ITEM_COMPONENTS.getStringValue(), MinecraftClient.getInstance().world.getRegistryManager());
        }
        InventoryUtils.setPickBlockableSlots(Generic.PICK_BLOCKABLE_SLOTS.getStringValue());
    }

    public static void saveToFile()
    {
        File dir = FileUtils.getConfigDirectory();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs())
        {
            JsonObject root = new JsonObject();

            ConfigUtils.writeConfigBase(root, "Colors", Colors.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            ConfigUtils.writeConfigBase(root, "InfoOverlays", InfoOverlays.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Visuals", Visuals.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Test", Test.OPTIONS);

            JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
        }
    }

    @Override
    public void load()
    {
        loadFromFile();
    }

    @Override
    public void save()
    {
        saveToFile();
    }
}
