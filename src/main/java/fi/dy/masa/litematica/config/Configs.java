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

    public static class Generic
    {
        public static final ConfigOptionList    EASY_PLACE_PROTOCOL         = new ConfigOptionList("easyPlaceProtocolVersion", EasyPlaceProtocol.AUTO, "litematica.config.generic.comment.easyPlaceProtocolVersion").translatedName("litematica.config.generic.name.easyPlaceProtocolVersion");
        public static final ConfigOptionList    PASTE_NBT_BEHAVIOR          = new ConfigOptionList("pasteNbtRestoreBehavior", PasteNbtBehavior.NONE, "litematica.config.generic.comment.pasteNbtRestoreBehavior").translatedName("litematica.config.generic.name.pasteNbtRestoreBehavior");
        public static final ConfigOptionList    PASTE_REPLACE_BEHAVIOR      = new ConfigOptionList("pasteReplaceBehavior", ReplaceBehavior.NONE, "litematica.config.generic.comment.pasteReplaceBehavior").translatedName("litematica.config.generic.name.pasteReplaceBehavior");
        public static final ConfigOptionList    PLACEMENT_REPLACE_BEHAVIOR  = new ConfigOptionList("placementReplaceBehavior", ReplaceBehavior.ALL, "litematica.config.generic.comment.placementReplaceBehavior").translatedName("litematica.config.generic.name.placementReplaceBehavior");
        public static final ConfigOptionList    PLACEMENT_RESTRICTION_WARN  = new ConfigOptionList("placementRestrictionWarn", MessageOutputType.ACTIONBAR, "litematica.config.generic.comment.placementRestrictionWarn").translatedName("litematica.config.generic.name.placementRestrictionWarn");
        public static final ConfigOptionList    SELECTION_CORNERS_MODE      = new ConfigOptionList("selectionCornersMode", CornerSelectionMode.CORNERS, "litematica.config.generic.comment.selectionCornersMode").translatedName("litematica.config.generic.name.selectionCornersMode");

        public static final ConfigBoolean       CUSTOM_SCHEMATIC_BASE_DIRECTORY_ENABLED = new ConfigBoolean("customSchematicBaseDirectoryEnabled", false, "litematica.config.generic.comment.customSchematicBaseDirectoryEnabled").translatedName("litematica.config.generic.name.customSchematicBaseDirectoryEnabled");
        public static final ConfigString        CUSTOM_SCHEMATIC_BASE_DIRECTORY         = new ConfigString( "customSchematicBaseDirectory", DataManager.getDefaultBaseSchematicDirectory().getAbsolutePath(), "litematica.config.generic.comment.customSchematicBaseDirectory").translatedName("litematica.config.generic.name.customSchematicBaseDirectory");

        public static final ConfigBoolean       AREAS_PER_WORLD             = new ConfigBoolean("areaSelectionsPerWorld", true, "litematica.config.generic.comment.areaSelectionsPerWorld").translatedName("litematica.config.generic.name.areaSelectionsPerWorld");
        public static final ConfigBoolean       BETTER_RENDER_ORDER         = new ConfigBoolean("betterRenderOrder", true, "litematica.config.generic.comment.betterRenderOrder").translatedName("litematica.config.generic.name.betterRenderOrder");
        public static final ConfigBoolean       CHANGE_SELECTED_CORNER      = new ConfigBoolean("changeSelectedCornerOnMove", true, "litematica.config.generic.comment.changeSelectedCornerOnMove").translatedName("litematica.config.generic.name.changeSelectedCornerOnMove");
        public static final ConfigBoolean       CLONE_AT_ORIGINAL_POS       = new ConfigBoolean("cloneAtOriginalPosition", false, "litematica.config.generic.comment.cloneAtOriginalPosition").translatedName("litematica.config.generic.name.cloneAtOriginalPosition");
        public static final ConfigBoolean       COMMAND_DISABLE_FEEDBACK    = new ConfigBoolean("commandDisableFeedback", true, "litematica.config.generic.comment.commandDisableFeedback").translatedName("litematica.config.generic.name.commandDisableFeedback");
        public static final ConfigInteger       COMMAND_FILL_MAX_VOLUME     = new ConfigInteger("commandFillMaxVolume", 32768, 256, 10000000, "litematica.config.generic.comment.commandFillMaxVolume").translatedName("litematica.config.generic.name.commandFillMaxVolume");
        public static final ConfigBoolean       COMMAND_FILL_NO_CHUNK_CLAMP = new ConfigBoolean("commandFillNoChunkClamp", false, "litematica.config.generic.comment.commandFillNoChunkClamp").translatedName("litematica.config.generic.name.commandFillNoChunkClamp");
        public static final ConfigInteger       COMMAND_LIMIT               = new ConfigInteger("commandLimitPerTick", 24, 1, 256, "litematica.config.generic.comment.commandLimitPerTick").translatedName("litematica.config.generic.name.commandLimitPerTick");
        public static final ConfigString        COMMAND_NAME_CLONE          = new ConfigString( "commandNameClone", "clone", "litematica.config.generic.comment.commandNameClone").translatedName("litematica.config.generic.name.commandNameClone");
        public static final ConfigString        COMMAND_NAME_FILL           = new ConfigString( "commandNameFill", "fill", "litematica.config.generic.comment.commandNameFill").translatedName("litematica.config.generic.name.commandNameFill");
        public static final ConfigString        COMMAND_NAME_SETBLOCK       = new ConfigString( "commandNameSetblock", "setblock", "litematica.config.generic.comment.commandNameSetblock").translatedName("litematica.config.generic.name.commandNameSetblock");
        public static final ConfigString        COMMAND_NAME_SUMMON         = new ConfigString( "commandNameSummon", "summon", "litematica.config.generic.comment.commandNameSummon").translatedName("litematica.config.generic.name.commandNameSummon");
        public static final ConfigInteger       COMMAND_TASK_INTERVAL       = new ConfigInteger("commandTaskInterval", 1, 1, 1000, "litematica.config.generic.comment.commandTaskInterval").translatedName("litematica.config.generic.name.commandTaskInterval");
        public static final ConfigBoolean       COMMAND_USE_WORLDEDIT       = new ConfigBoolean("commandUseWorldEdit", false, "litematica.config.generic.comment.commandUseWorldEdit").translatedName("litematica.config.generic.name.commandUseWorldEdit");
        public static final ConfigBoolean       DEBUG_LOGGING               = new ConfigBoolean("debugLogging", false, "litematica.config.generic.comment.debugLogging").translatedName("litematica.config.generic.name.debugLogging");
        public static final ConfigOptionList    DATAFIXER_MODE              = new ConfigOptionList("datafixerMode", DataFixerMode.ALWAYS, "litematica.config.generic.comment.datafixerMode").translatedName("litematica.config.generic.name.datafixerMode");
        public static final ConfigInteger       DATAFIXER_DEFAULT_SCHEMA    = new ConfigInteger("datafixerDefaultSchema", 1139, 99, 2724, true, "litematica.config.generic.comment.datafixerDefaultSchema").translatedName("litematica.config.generic.name.datafixerDefaultSchema");
        public static final ConfigBoolean       EASY_PLACE_FIRST            = new ConfigBoolean("easyPlaceFirst", true, "litematica.config.generic.comment.easyPlaceFirst").translatedName("litematica.config.generic.name.easyPlaceFirst");
        public static final ConfigBoolean       EASY_PLACE_HOLD_ENABLED     = new ConfigBoolean("easyPlaceHoldEnabled", true, "litematica.config.generic.comment.easyPlaceHoldEnabled").translatedName("litematica.config.generic.name.easyPlaceHoldEnabled");
        public static final ConfigBoolean       EASY_PLACE_MODE             = new ConfigBoolean("easyPlaceMode", false, "litematica.config.generic.comment.easyPlaceMode", "litematica.config.generic.prettyName.easyPlaceMode").translatedName("litematica.config.generic.name.easyPlaceMode");
        public static final ConfigBoolean       EASY_PLACE_SP_HANDLING      = new ConfigBoolean("easyPlaceSinglePlayerHandling", true, "litematica.config.generic.comment.easyPlaceSinglePlayerHandling").translatedName("litematica.config.generic.name.easyPlaceSinglePlayerHandling");
        public static final ConfigInteger       EASY_PLACE_SWAP_INTERVAL    = new ConfigInteger("easyPlaceSwapInterval", 0, 0, 10000, "litematica.config.generic.comment.easyPlaceSwapInterval").translatedName("litematica.config.generic.name.easyPlaceSwapInterval");
        public static final ConfigBoolean       EASY_PLACE_SWING_HAND       = new ConfigBoolean("easyPlaceSwingHand", true, "litematica.config.generic.comment.easyPlaceSwingHand").translatedName("litematica.config.generic.name.easyPlaceSwingHand");
        public static final ConfigBoolean       EASY_PLACE_VANILLA_REACH    = new ConfigBoolean("easyPlaceVanillaReach", false, "litematica.config.generic.comment.easyPlaceVanillaReach").translatedName("litematica.config.generic.name.easyPlaceVanillaReach");
        public static final ConfigBoolean       ENTITY_DATA_SYNC            = new ConfigBoolean("entityDataSync", true, "litematica.config.generic.comment.entityDataSync").translatedName("litematica.config.generic.name.entityDataSync");
        public static final ConfigBoolean       ENTITY_DATA_SYNC_BACKUP     = new ConfigBoolean("entityDataSyncBackup", true, "litematica.config.generic.comment.entityDataSyncBackup").translatedName("litematica.config.generic.name.entityDataSyncBackup");
        public static final ConfigBoolean       EXECUTE_REQUIRE_TOOL        = new ConfigBoolean("executeRequireHoldingTool", true, "litematica.config.generic.comment.executeRequireHoldingTool").translatedName("litematica.config.generic.name.executeRequireHoldingTool");
        public static final ConfigBoolean       FIX_CHEST_MIRROR            = new ConfigBoolean("fixChestMirror", true, "litematica.config.generic.comment.fixChestMirror").translatedName("litematica.config.generic.name.fixChestMirror");
        public static final ConfigBoolean       FIX_RAIL_ROTATION           = new ConfigBoolean("fixRailRotation", true, "litematica.config.generic.comment.fixRailRotation").translatedName("litematica.config.generic.name.fixRailRotation");
        public static final ConfigBoolean       GENERATE_LOWERCASE_NAMES    = new ConfigBoolean("generateLowercaseNames", false, "litematica.config.generic.comment.generateLowercaseNames").translatedName("litematica.config.generic.name.generateLowercaseNames");
        public static final ConfigBoolean       HIGHLIGHT_BLOCK_IN_INV      = new ConfigBoolean("highlightBlockInInventory", false, "litematica.config.generic.comment.highlightBlockInInventory").translatedName("litematica.config.generic.name.highlightBlockInInventory");
        public static final ConfigBoolean       ITEM_USE_PACKET_CHECK_BYPASS= new ConfigBoolean("itemUsePacketCheckBypass", true, "litematica.config.generic.comment.itemUsePacketCheckBypass").translatedName("litematica.config.generic.name.itemUsePacketCheckBypass");
        public static final ConfigBoolean       LAYER_MODE_DYNAMIC          = new ConfigBoolean("layerModeFollowsPlayer", false, "litematica.config.generic.comment.layerModeFollowsPlayer").translatedName("litematica.config.generic.name.layerModeFollowsPlayer");
        public static final ConfigBoolean       LOAD_ENTIRE_SCHEMATICS      = new ConfigBoolean("loadEntireSchematics", false, "litematica.config.generic.comment.loadEntireSchematics").translatedName("litematica.config.generic.name.loadEntireSchematics");
        public static final ConfigBoolean       MATERIAL_LIST_IGNORE_STATE  = new ConfigBoolean("materialListIgnoreState", false, "litematica.config.generic.comment.materialListIgnoreState").translatedName("litematica.config.generic.name.materialListIgnoreState");
        public static final ConfigBoolean       PASTE_ALWAYS_USE_FILL       = new ConfigBoolean("pasteAlwaysUseFill", false, "litematica.config.generic.comment.pasteAlwaysUseFill").translatedName("litematica.config.generic.name.pasteAlwaysUseFill");
        public static final ConfigBoolean       PASTE_IGNORE_BE_ENTIRELY    = new ConfigBoolean("pasteIgnoreBlockEntitiesEntirely", false, "litematica.config.generic.comment.pasteIgnoreBlockEntitiesEntirely").translatedName("litematica.config.generic.name.pasteIgnoreBlockEntitiesEntirely");
        public static final ConfigBoolean       PASTE_IGNORE_BE_IN_FILL     = new ConfigBoolean("pasteIgnoreBlockEntitiesFromFill", true, "litematica.config.generic.comment.pasteIgnoreBlockEntitiesFromFill").translatedName("litematica.config.generic.name.pasteIgnoreBlockEntitiesFromFill");
        public static final ConfigBoolean       PASTE_IGNORE_CMD_LIMIT      = new ConfigBoolean("pasteIgnoreCommandLimitWithNbtRestore", true, "litematica.config.generic.comment.pasteIgnoreCommandLimitWithNbtRestore").translatedName("litematica.config.generic.name.pasteIgnoreCommandLimitWithNbtRestore");
        public static final ConfigBoolean       PASTE_IGNORE_ENTITIES       = new ConfigBoolean("pasteIgnoreEntities", false, "litematica.config.generic.comment.pasteIgnoreEntities").translatedName("litematica.config.generic.name.pasteIgnoreEntities");
        public static final ConfigBoolean       PASTE_IGNORE_INVENTORY      = new ConfigBoolean("pasteIgnoreInventories", false, "litematica.config.generic.comment.pasteIgnoreInventories").translatedName("litematica.config.generic.name.pasteIgnoreInventories");
        public static final ConfigBoolean       PASTE_TO_MCFUNCTION         = new ConfigBoolean("pasteToMcFunctionFiles", false, "litematica.config.generic.comment.pasteToMcFunctionFiles").translatedName("litematica.config.generic.name.pasteToMcFunctionFiles");
        public static final ConfigBoolean       PASTE_USE_FILL_COMMAND      = new ConfigBoolean("pasteUseFillCommand", true, "litematica.config.generic.comment.pasteUseFillCommand").translatedName("litematica.config.generic.name.pasteUseFillCommand");
        public static final ConfigBoolean       PASTE_USING_COMMANDS_IN_SP  = new ConfigBoolean("pasteUsingCommandsInSp", false, "litematica.config.generic.comment.pasteUsingCommandsInSp").translatedName("litematica.config.generic.name.pasteUsingCommandsInSp");
        public static final ConfigBoolean       PASTE_USING_SERVUX          = new ConfigBoolean("pasteUsingServux", true, "litematica.config.generic.comment.pasteUsingServux").translatedName("litematica.config.generic.name.pasteUsingServux");
        public static final ConfigBoolean       PICK_BLOCK_AVOID_DAMAGEABLE = new ConfigBoolean("pickBlockAvoidDamageable", true, "litematica.config.generic.comment.pickBlockAvoidDamageable").translatedName("litematica.config.generic.name.pickBlockAvoidDamageable");
        public static final ConfigBoolean       PICK_BLOCK_AVOID_TOOLS      = new ConfigBoolean("pickBlockAvoidTools", false, "litematica.config.generic.comment.pickBlockAvoidTools").translatedName("litematica.config.generic.name.pickBlockAvoidTools");
        public static final ConfigBoolean       PICK_BLOCK_ENABLED          = new ConfigBoolean("pickBlockEnabled", true, "litematica.config.generic.comment.pickBlockEnabled", "litematica.config.generic.prettyName.pickBlockEnabled").translatedName("litematica.config.generic.name.pickBlockEnabled");
        public static final ConfigBoolean       PICK_BLOCK_SHULKERS         = new ConfigBoolean("pickBlockShulkers", false, "litematica.config.generic.comment.pickBlockShulkers").translatedName("litematica.config.generic.name.pickBlockShulkers");
        public static final ConfigString        PICK_BLOCKABLE_SLOTS        = new ConfigString( "pickBlockableSlots", "1,2,3,4,5", "litematica.config.generic.comment.pickBlockableSlots").translatedName("litematica.config.generic.name.pickBlockableSlots");
        public static final ConfigBoolean       PLACEMENT_RESTRICTION       = new ConfigBoolean("placementRestriction", false, "litematica.config.generic.comment.placementRestriction", "litematica.config.generic.prettyName.placementRestriction").translatedName("litematica.config.generic.name.placementRestriction");
        public static final ConfigBoolean       RENDER_MATERIALS_IN_GUI     = new ConfigBoolean("renderMaterialListInGuis", true, "litematica.config.generic.comment.renderMaterialListInGuis").translatedName("litematica.config.generic.name.renderMaterialListInGuis");
        public static final ConfigBoolean       RENDER_THREAD_NO_TIMEOUT    = new ConfigBoolean("renderThreadNoTimeout", true, "litematica.config.generic.comment.renderThreadNoTimeout").translatedName("litematica.config.generic.name.renderThreadNoTimeout");
        public static final ConfigInteger       SERVER_NBT_REQUEST_RATE     = new ConfigInteger("serverNbtRequestRate", 2, "litematica.config.generic.comment.serverNbtRequestRate").translatedName("litematica.config.generic.name.serverNbtRequestRate");
        public static final ConfigBoolean       SIGN_TEXT_PASTE             = new ConfigBoolean("signTextPaste", true, "litematica.config.generic.comment.signTextPaste", "litematica.config.generic.prettyName.signTextPaste").translatedName("litematica.config.generic.name.signTextPaste");
        public static final ConfigString        TOOL_ITEM                   = new ConfigString( "toolItem", "minecraft:stick", "litematica.config.generic.comment.toolItem").translatedName("litematica.config.generic.name.toolItem");
        public static final ConfigBoolean       TOOL_ITEM_ENABLED           = new ConfigBoolean("toolItemEnabled", true, "litematica.config.generic.comment.toolItemEnabled", "litematica.config.generic.prettyName.toolItemEnabled").translatedName("litematica.config.generic.name.toolItemEnabled");
        public static final ConfigString        TOOL_ITEM_COMPONENTS        = new ConfigString( "toolItemComponents", "empty", "litematica.config.generic.comment.toolItemComponents").translatedName("litematica.config.generic.name.toolItemComponents");
        public static final ConfigBoolean       UNHIDE_SCHEMATIC_PROJECTS   = new ConfigBoolean("unhideSchematicVCS", false, "litematica.config.generic.comment.unhideSchematicVCS").translatedName("litematica.config.generic.name.unhideSchematicVCS");

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
        public static final ConfigBoolean       ENABLE_AREA_SELECTION_RENDERING     = new ConfigBoolean("enableAreaSelectionBoxesRendering", true, "litematica.config.visuals.comment.enableAreaSelectionBoxesRendering", "litematica.config.visuals.prettyName.enableAreaSelectionBoxesRendering").translatedName("litematica.config.visuals.name.enableAreaSelectionBoxesRendering");
        public static final ConfigBoolean       ENABLE_PLACEMENT_BOXES_RENDERING    = new ConfigBoolean("enablePlacementBoxesRendering", true, "litematica.config.visuals.comment.enablePlacementBoxesRendering", "litematica.config.visuals.prettyName.enablePlacementBoxesRendering").translatedName("litematica.config.visuals.name.enablePlacementBoxesRendering");
        public static final ConfigBoolean       ENABLE_RENDERING                    = new ConfigBoolean("enableRendering", true, "litematica.config.visuals.comment.enableRendering", "litematica.config.visuals.prettyName.enableRendering").translatedName("litematica.config.visuals.name.enableRendering");
        public static final ConfigBoolean       ENABLE_SCHEMATIC_BLOCKS             = new ConfigBoolean("enableSchematicBlocksRendering",  true, "litematica.config.visuals.comment.enableSchematicBlocksRendering", "litematica.config.visuals.prettyName.enableSchematicBlocksRendering").translatedName("litematica.config.visuals.name.enableSchematicBlocksRendering");
        public static final ConfigBoolean       ENABLE_SCHEMATIC_OVERLAY            = new ConfigBoolean("enableSchematicOverlay",  true, "litematica.config.visuals.comment.enableSchematicOverlay", "litematica.config.visuals.prettyName.enableSchematicOverlay").translatedName("litematica.config.visuals.name.enableSchematicOverlay");
        public static final ConfigBoolean       ENABLE_SCHEMATIC_RENDERING          = new ConfigBoolean("enableSchematicRendering", true, "litematica.config.visuals.comment.enableSchematicRendering", "litematica.config.visuals.prettyName.enableSchematicRendering").translatedName("litematica.config.visuals.name.enableSchematicRendering");
        //public static final ConfigInteger       RENDER_SCHEMATIC_MAX_THREADS        = new ConfigInteger("renderSchematicMaxThreads", 4, 1, 16, "litematica.config.visuals.comment.renderSchematicMaxThreads").translatedName("litematica.config.visuals.name.renderSchematicMaxThreads");
        public static final ConfigDouble        GHOST_BLOCK_ALPHA                   = new ConfigDouble( "ghostBlockAlpha", 0.5, 0, 1, "litematica.config.visuals.comment.ghostBlockAlpha").translatedName("litematica.config.visuals.name.ghostBlockAlpha");
        public static final ConfigBoolean       IGNORE_EXISTING_FLUIDS              = new ConfigBoolean("ignoreExistingFluids", false, "litematica.config.visuals.comment.ignoreExistingFluids").translatedName("litematica.config.visuals.name.ignoreExistingFluids");
        public static final ConfigBoolean       OVERLAY_REDUCED_INNER_SIDES         = new ConfigBoolean("overlayReducedInnerSides", false, "litematica.config.visuals.comment.overlayReducedInnerSides").translatedName("litematica.config.visuals.name.overlayReducedInnerSides");
        public static final ConfigDouble        PLACEMENT_BOX_SIDE_ALPHA            = new ConfigDouble( "placementBoxSideAlpha", 0.2, 0, 1, "litematica.config.visuals.comment.placementBoxSideAlpha").translatedName("litematica.config.visuals.name.placementBoxSideAlpha");
        public static final ConfigBoolean       RENDER_AREA_SELECTION_BOX_SIDES     = new ConfigBoolean("renderAreaSelectionBoxSides", true, "litematica.config.visuals.comment.renderAreaSelectionBoxSides").translatedName("litematica.config.visuals.name.renderAreaSelectionBoxSides");
        public static final ConfigBoolean       RENDER_BLOCKS_AS_TRANSLUCENT        = new ConfigBoolean("renderBlocksAsTranslucent", false, "litematica.config.visuals.comment.renderBlocksAsTranslucent", "litematica.config.visuals.prettyName.renderBlocksAsTranslucent").translatedName("litematica.config.visuals.name.renderBlocksAsTranslucent");
        public static final ConfigBoolean       RENDER_COLLIDING_SCHEMATIC_BLOCKS   = new ConfigBoolean("renderCollidingSchematicBlocks", false, "litematica.config.visuals.comment.renderCollidingSchematicBlocks").translatedName("litematica.config.visuals.name.renderCollidingSchematicBlocks");
        public static final ConfigBoolean       RENDER_ERROR_MARKER_CONNECTIONS     = new ConfigBoolean("renderErrorMarkerConnections", false, "litematica.config.visuals.comment.renderErrorMarkerConnections").translatedName("litematica.config.visuals.name.renderErrorMarkerConnections");
        public static final ConfigBoolean       RENDER_ERROR_MARKER_SIDES           = new ConfigBoolean("renderErrorMarkerSides", true, "litematica.config.visuals.comment.renderErrorMarkerSides").translatedName("litematica.config.visuals.name.renderErrorMarkerSides");
        public static final ConfigBoolean       RENDER_PLACEMENT_BOX_SIDES          = new ConfigBoolean("renderPlacementBoxSides", false, "litematica.config.visuals.comment.renderPlacementBoxSides").translatedName("litematica.config.visuals.name.renderPlacementBoxSides");
        public static final ConfigBoolean       RENDER_PLACEMENT_ENCLOSING_BOX      = new ConfigBoolean("renderPlacementEnclosingBox", true, "litematica.config.visuals.comment.renderPlacementEnclosingBox").translatedName("litematica.config.visuals.name.renderPlacementEnclosingBox");
        public static final ConfigBoolean       RENDER_PLACEMENT_ENCLOSING_BOX_SIDES= new ConfigBoolean("renderPlacementEnclosingBoxSides", false, "litematica.config.visuals.comment.renderPlacementEnclosingBoxSides").translatedName("litematica.config.visuals.name.renderPlacementEnclosingBoxSides");
        public static final ConfigBoolean       RENDER_TRANSLUCENT_INNER_SIDES      = new ConfigBoolean("renderTranslucentBlockInnerSides", false, "litematica.config.visuals.comment.renderTranslucentBlockInnerSides").translatedName("litematica.config.visuals.name.renderTranslucentBlockInnerSides");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_ENABLE_OUTLINES   = new ConfigBoolean("schematicOverlayEnableOutlines",  true, "litematica.config.visuals.comment.schematicOverlayEnableOutlines", "litematica.config.visuals.prettyName.schematicOverlayEnableOutlines").translatedName("litematica.config.visuals.name.schematicOverlayEnableOutlines");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_ENABLE_RESORTING  = new ConfigBoolean("schematicOverlayEnableResorting",  false, "litematica.config.visuals.comment.schematicOverlayEnableResorting", "litematica.config.visuals.prettyName.schematicOverlayEnableResorting").translatedName("litematica.config.visuals.name.schematicOverlayEnableResorting");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_ENABLE_SIDES      = new ConfigBoolean("schematicOverlayEnableSides",     true, "litematica.config.visuals.comment.schematicOverlayEnableSides", "litematica.config.visuals.prettyName.schematicOverlayEnableSides").translatedName("litematica.config.visuals.name.schematicOverlayEnableSides");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_MODEL_OUTLINE     = new ConfigBoolean("schematicOverlayModelOutline",    true, "litematica.config.visuals.comment.schematicOverlayModelOutline").translatedName("litematica.config.visuals.name.schematicOverlayModelOutline");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_MODEL_SIDES       = new ConfigBoolean("schematicOverlayModelSides",      false, "litematica.config.visuals.comment.schematicOverlayModelSides").translatedName("litematica.config.visuals.name.schematicOverlayModelSides");
        public static final ConfigDouble        SCHEMATIC_OVERLAY_OUTLINE_WIDTH     = new ConfigDouble( "schematicOverlayOutlineWidth",  1.0, 0, 64, "litematica.config.visuals.comment.schematicOverlayOutlineWidth").translatedName("litematica.config.visuals.name.schematicOverlayOutlineWidth");
        public static final ConfigDouble        SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH = new ConfigDouble("schematicOverlayOutlineWidthThrough",  3.0, 0, 64, "litematica.config.visuals.comment.schematicOverlayOutlineWidthThrough").translatedName("litematica.config.visuals.name.schematicOverlayOutlineWidthThrough");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_RENDER_THROUGH    = new ConfigBoolean("schematicOverlayRenderThroughBlocks", false, "litematica.config.visuals.comment.schematicOverlayRenderThroughBlocks").translatedName("litematica.config.visuals.name.schematicOverlayRenderThroughBlocks");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_TYPE_EXTRA        = new ConfigBoolean("schematicOverlayTypeExtra",       true, "litematica.config.visuals.comment.schematicOverlayTypeExtra").translatedName("litematica.config.visuals.name.schematicOverlayTypeExtra");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_TYPE_MISSING      = new ConfigBoolean("schematicOverlayTypeMissing",     true, "litematica.config.visuals.comment.schematicOverlayTypeMissing").translatedName("litematica.config.visuals.name.schematicOverlayTypeMissing");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK  = new ConfigBoolean("schematicOverlayTypeWrongBlock",  true, "litematica.config.visuals.comment.schematicOverlayTypeWrongBlock").translatedName("litematica.config.visuals.name.schematicOverlayTypeWrongBlock");
        public static final ConfigBoolean       SCHEMATIC_OVERLAY_TYPE_WRONG_STATE  = new ConfigBoolean("schematicOverlayTypeWrongState",  true, "litematica.config.visuals.comment.schematicOverlayTypeWrongState").translatedName("litematica.config.visuals.name.schematicOverlayTypeWrongState");
        public static final ConfigBoolean       SCHEMATIC_VERIFIER_BLOCK_MODELS     = new ConfigBoolean("schematicVerifierUseBlockModels", false, "litematica.config.visuals.comment.schematicVerifierUseBlockModels").translatedName("litematica.config.visuals.name.schematicVerifierUseBlockModels");

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_RENDERING,
                ENABLE_SCHEMATIC_RENDERING,
                //RENDER_SCHEMATIC_MAX_THREADS,

                ENABLE_AREA_SELECTION_RENDERING,
                ENABLE_PLACEMENT_BOXES_RENDERING,
                ENABLE_SCHEMATIC_BLOCKS,
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
        public static final ConfigOptionList    BLOCK_INFO_LINES_ALIGNMENT          = new ConfigOptionList("blockInfoLinesAlignment", HudAlignment.TOP_RIGHT, "litematica.config.info_overlays.comment.blockInfoLinesAlignment").translatedName("litematica.config.info_overlays.name.blockInfoLinesAlignment");
        public static final ConfigOptionList    BLOCK_INFO_OVERLAY_ALIGNMENT        = new ConfigOptionList("blockInfoOverlayAlignment", BlockInfoAlignment.TOP_CENTER, "litematica.config.info_overlays.comment.blockInfoOverlayAlignment").translatedName("litematica.config.info_overlays.name.blockInfoOverlayAlignment");
        public static final ConfigOptionList    INFO_HUD_ALIGNMENT                  = new ConfigOptionList("infoHudAlignment", HudAlignment.BOTTOM_RIGHT, "litematica.config.info_overlays.comment.infoHudAlignment").translatedName("litematica.config.info_overlays.name.infoHudAlignment");
        public static final ConfigOptionList    TOOL_HUD_ALIGNMENT                  = new ConfigOptionList("toolHudAlignment", HudAlignment.BOTTOM_LEFT, "litematica.config.info_overlays.comment.toolHudAlignment").translatedName("litematica.config.info_overlays.name.toolHudAlignment");

        public static final ConfigBoolean       BLOCK_INFO_LINES_ENABLED            = new ConfigBoolean("blockInfoLinesEnabled", true, "litematica.config.info_overlays.comment.blockInfoLinesEnabled").translatedName("litematica.config.info_overlays.name.blockInfoLinesEnabled");
        public static final ConfigDouble        BLOCK_INFO_LINES_FONT_SCALE         = new ConfigDouble( "blockInfoLinesFontScale", 0.5, 0, 10, "litematica.config.info_overlays.comment.blockInfoLinesFontScale").translatedName("litematica.config.info_overlays.name.blockInfoLinesFontScale");
        public static final ConfigInteger       BLOCK_INFO_LINES_OFFSET_X           = new ConfigInteger("blockInfoLinesOffsetX", 4, 0, 2000, "litematica.config.info_overlays.comment.blockInfoLinesOffsetX").translatedName("litematica.config.info_overlays.name.blockInfoLinesOffsetX");
        public static final ConfigInteger       BLOCK_INFO_LINES_OFFSET_Y           = new ConfigInteger("blockInfoLinesOffsetY", 4, 0, 2000, "litematica.config.info_overlays.comment.blockInfoLinesOffsetY").translatedName("litematica.config.info_overlays.name.blockInfoLinesOffsetY");
        public static final ConfigInteger       BLOCK_INFO_OVERLAY_OFFSET_Y         = new ConfigInteger("blockInfoOverlayOffsetY", 6, -2000, 2000, "litematica.config.info_overlays.comment.blockInfoOverlayOffsetY").translatedName("litematica.config.info_overlays.name.blockInfoOverlayOffsetY");
        public static final ConfigBoolean       BLOCK_INFO_OVERLAY_ENABLED          = new ConfigBoolean("blockInfoOverlayEnabled", true, "litematica.config.info_overlays.comment.blockInfoOverlayEnabled", "litematica.config.info_overlays.prettyName.blockInfoOverlayEnabled").translatedName("litematica.config.info_overlays.name.blockInfoOverlayEnabled");
        public static final ConfigInteger       INFO_HUD_MAX_LINES                  = new ConfigInteger("infoHudMaxLines", 10, 1, 128, "litematica.config.info_overlays.comment.infoHudMaxLines").translatedName("litematica.config.info_overlays.name.infoHudMaxLines");
        public static final ConfigInteger       INFO_HUD_OFFSET_X                   = new ConfigInteger("infoHudOffsetX", 1, 0, 32000, "litematica.config.info_overlays.comment.infoHudOffsetX").translatedName("litematica.config.info_overlays.name.infoHudOffsetX");
        public static final ConfigInteger       INFO_HUD_OFFSET_Y                   = new ConfigInteger("infoHudOffsetY", 1, 0, 32000, "litematica.config.info_overlays.comment.infoHudOffsetY").translatedName("litematica.config.info_overlays.name.infoHudOffsetY");
        public static final ConfigDouble        INFO_HUD_SCALE                      = new ConfigDouble( "infoHudScale", 1, 0.1, 4, "litematica.config.info_overlays.comment.infoHudScale").translatedName("litematica.config.info_overlays.name.infoHudScale");
        public static final ConfigBoolean       INFO_OVERLAYS_TARGET_FLUIDS         = new ConfigBoolean("infoOverlaysTargetFluids", false, "litematica.config.info_overlays.comment.infoOverlaysTargetFluids").translatedName("litematica.config.info_overlays.name.infoOverlaysTargetFluids");
        public static final ConfigInteger       MATERIAL_LIST_HUD_MAX_LINES         = new ConfigInteger("materialListHudMaxLines", 10, 1, 128, "litematica.config.info_overlays.comment.materialListHudMaxLines").translatedName("litematica.config.info_overlays.name.materialListHudMaxLines");
        public static final ConfigDouble        MATERIAL_LIST_HUD_SCALE             = new ConfigDouble( "materialListHudScale", 1, 0.1, 4, "litematica.config.info_overlays.comment.materialListHudScale").translatedName("litematica.config.info_overlays.name.materialListHudScale");
        public static final ConfigBoolean       STATUS_INFO_HUD                     = new ConfigBoolean("statusInfoHud", false, "litematica.config.info_overlays.comment.statusInfoHud").translatedName("litematica.config.info_overlays.name.statusInfoHud");
        public static final ConfigBoolean       STATUS_INFO_HUD_AUTO                = new ConfigBoolean("statusInfoHudAuto", true, "litematica.config.info_overlays.comment.statusInfoHudAuto").translatedName("litematica.config.info_overlays.name.statusInfoHudAuto");
        public static final ConfigInteger       TOOL_HUD_OFFSET_X                   = new ConfigInteger("toolHudOffsetX", 1, 0, 32000, "litematica.config.info_overlays.comment.toolHudOffsetX").translatedName("litematica.config.info_overlays.name.toolHudOffsetX");
        public static final ConfigInteger       TOOL_HUD_OFFSET_Y                   = new ConfigInteger("toolHudOffsetY", 1, 0, 32000, "litematica.config.info_overlays.comment.toolHudOffsetY").translatedName("litematica.config.info_overlays.name.toolHudOffsetY");
        public static final ConfigDouble        TOOL_HUD_SCALE                      = new ConfigDouble( "toolHudScale", 1, 0.1, 4, "litematica.config.info_overlays.comment.toolHudScale").translatedName("litematica.config.info_overlays.name.toolHudScale");
        public static final ConfigDouble        VERIFIER_ERROR_HILIGHT_ALPHA        = new ConfigDouble( "verifierErrorHilightAlpha", 0.2, 0, 1, "litematica.config.info_overlays.comment.verifierErrorHilightAlpha").translatedName("litematica.config.info_overlays.name.verifierErrorHilightAlpha");
        public static final ConfigInteger       VERIFIER_ERROR_HILIGHT_MAX_POSITIONS= new ConfigInteger("verifierErrorHilightMaxPositions", 1000, 1, 1000000, "litematica.config.info_overlays.comment.verifierErrorHilightMaxPositions").translatedName("litematica.config.info_overlays.name.verifierErrorHilightMaxPositions");
        public static final ConfigBoolean       VERIFIER_OVERLAY_ENABLED            = new ConfigBoolean("verifierOverlayEnabled", true, "litematica.config.info_overlays.comment.verifierOverlayEnabled", "litematica.config.info_overlays.prettyName.verifierOverlayEnabled").translatedName("litematica.config.info_overlays.name.verifierOverlayEnabled");
        public static final ConfigBoolean       WARN_DISABLED_RENDERING             = new ConfigBoolean("warnDisabledRendering", true, "litematica.config.info_overlays.comment.warnDisabledRendering").translatedName("litematica.config.info_overlays.name.warnDisabledRendering");

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
        public static final ConfigColor AREA_SELECTION_BOX_SIDE_COLOR       = new ConfigColor("areaSelectionBoxSideColor",          "#30FFFFFF", "litematica.config.colors.comment.areaSelectionBoxSideColor").translatedName("litematica.config.colors.name.areaSelectionBoxSideColor");
        public static final ConfigColor HIGHTLIGHT_BLOCK_IN_INV_COLOR       = new ConfigColor("hightlightBlockInInventoryColor",    "#30FF30FF", "litematica.config.colors.comment.hightlightBlockInInventoryColor").translatedName("litematica.config.colors.name.hightlightBlockInInventoryColor");
        public static final ConfigColor MATERIAL_LIST_HUD_ITEM_COUNTS       = new ConfigColor("materialListHudItemCountsColor",     "#FFFFAA00", "litematica.config.colors.comment.materialListHudItemCountsColor").translatedName("litematica.config.colors.name.materialListHudItemCountsColor");
        public static final ConfigColor REBUILD_BREAK_OVERLAY_COLOR         = new ConfigColor("schematicRebuildBreakPlaceOverlayColor", "#4C33CC33", "litematica.config.colors.comment.schematicRebuildBreakPlaceOverlayColor").translatedName("litematica.config.colors.name.schematicRebuildBreakPlaceOverlayColor");
        public static final ConfigColor REBUILD_BREAK_EXCEPT_OVERLAY_COLOR  = new ConfigColor("schematicRebuildBreakExceptPlaceOverlayColor", "#4CF03030", "litematica.config.colors.comment.schematicRebuildBreakExceptPlaceOverlayColor").translatedName("litematica.config.colors.name.schematicRebuildBreakExceptPlaceOverlayColor");
        public static final ConfigColor REBUILD_REPLACE_OVERLAY_COLOR       = new ConfigColor("schematicRebuildReplaceOverlayColor","#4CF0A010", "litematica.config.colors.comment.schematicRebuildReplaceOverlayColor").translatedName("litematica.config.colors.name.schematicRebuildReplaceOverlayColor");
        public static final ConfigColor SCHEMATIC_OVERLAY_COLOR_EXTRA       = new ConfigColor("schematicOverlayColorExtra",         "#4CFF4CE6", "litematica.config.colors.comment.schematicOverlayColorExtra").translatedName("litematica.config.colors.name.schematicOverlayColorExtra");
        public static final ConfigColor SCHEMATIC_OVERLAY_COLOR_MISSING     = new ConfigColor("schematicOverlayColorMissing",       "#2C33B3E6", "litematica.config.colors.comment.schematicOverlayColorMissing").translatedName("litematica.config.colors.name.schematicOverlayColorMissing");
        public static final ConfigColor SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK = new ConfigColor("schematicOverlayColorWrongBlock",    "#4CFF3333", "litematica.config.colors.comment.schematicOverlayColorWrongBlock").translatedName("litematica.config.colors.name.schematicOverlayColorWrongBlock");
        public static final ConfigColor SCHEMATIC_OVERLAY_COLOR_WRONG_STATE = new ConfigColor("schematicOverlayColorWrongState",    "#4CFF9010", "litematica.config.colors.comment.schematicOverlayColorWrongState").translatedName("litematica.config.colors.name.schematicOverlayColorWrongState");

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
