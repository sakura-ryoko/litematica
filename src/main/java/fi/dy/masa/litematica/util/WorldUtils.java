package fi.dy.masa.litematica.util;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.*;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.game.wrap.GameWrap;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.mixin.block.IMixinWallMountedBlock;
import fi.dy.masa.litematica.mixin.entity.IMixinSignBlockEntity;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.schematic.pickblock.SchematicPickBlockEventHandler;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.schematic.placement.TemporaryWorldHolder;
import fi.dy.masa.litematica.schematic.placement.TemporaryWorldManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class WorldUtils
{
	/**
	 * Moved to {@link EasyPlaceUtils}
	 */
	@Deprecated(forRemoval = true)
    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();
	/**
	 * Moved to {@link EasyPlaceUtils}
	 */
	@Deprecated(forRemoval = true)
    private static long easyPlaceLastPickBlockTime = System.nanoTime();

    public static double getValidBlockRange(Minecraft mc)
    {
        return Configs.Generic.EASY_PLACE_VANILLA_REACH.getBooleanValue() ? mc.player.blockInteractionRange() : mc.player.blockInteractionRange() + 1.0;
    }

    public static boolean shouldPreventBlockUpdates(Level world)
    {
        return ((IWorldUpdateSuppressor) world).litematica_getShouldPreventBlockUpdates();
    }

    public static void setShouldPreventBlockUpdates(Level world, boolean preventUpdates)
    {
        ((IWorldUpdateSuppressor) world).litematica_setShouldPreventBlockUpdates(preventUpdates);
    }

    public static boolean convertLitematicaSchematicToLitematicaSchematic(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertLitematicaSchematicToLitematicaSchematic(inputDir, inputFileName, outputFileName, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
    }

    public static boolean convertSpongeSchematicToLitematicaSchematic(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        DataFixerMode oldMode = (DataFixerMode) Configs.Generic.DATAFIXER_MODE.getOptionListValue();
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(DataFixerMode.ALWAYS);
        LitematicaSchematic origSchematic = convertSpongeSchematicToLitematicaSchematic(inputDir, inputFileName);

        if (origSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.sponge_to_litematica.failed_to_read_sponge");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return false;
        }

//        WorldSchematic world = SchematicWorldHandler.createSchematicWorld(null);
        BlockPos size = new BlockPos(origSchematic.getTotalSize());
        TemporaryWorldHolder holder = TemporaryWorldManager.INSTANCE.getTemporaryWorld("sponge_to_litematica", BlockPos.ZERO, size);
//        List<Pair<Integer, Integer>> tempChunks = loadChunksSchematicWorld(world, BlockPos.ZERO, size);
        SchematicPlacement schematicPlacement = SchematicPlacement.createForSchematicConversion(origSchematic, BlockPos.ZERO);
        origSchematic.placeToWorld(holder.world(), schematicPlacement, false);      // TODO FIXME

        String subRegionName = FileUtils.getNameWithoutExtension(inputFileName);
        AreaSelection area = new AreaSelection();
        area.setName(subRegionName);
        subRegionName = area.createNewSubRegionBox(BlockPos.ZERO, subRegionName);
        area.setSelectedSubRegionBox(subRegionName);
        Box box = area.getSelectedSubRegionBox();
        area.setSubRegionCornerPos(box, Corner.CORNER_1, BlockPos.ZERO);
        area.setSubRegionCornerPos(box, Corner.CORNER_2, size.offset(-1, -1, -1));
        LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);

        LitematicaSchematic newSchem = LitematicaSchematic.createFromWorld(holder.world(), area, info, "?", feedback);

        if (newSchem == null)
        {
            feedback.setString("litematica.error.schematic_conversion.sponge_to_litematica.failed_to_create_litematic");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return false;
        }

        SchematicMetadata origMetadata = origSchematic.getMetadata();

        if (origMetadata.getAuthor().isEmpty() || origMetadata.getAuthor() == "?")
        {
            newSchem.getMetadata().setAuthor(GameWrap.getPlayerName());
        }
        else
        {
            newSchem.getMetadata().setAuthor(origMetadata.getAuthor());
        }

        if (origMetadata.getName().isEmpty() || origMetadata.getName() == "?")
        {
            newSchem.getMetadata().setName(subRegionName);
        }
        else
        {
            newSchem.getMetadata().setName(origMetadata.getName());
        }

        newSchem.getMetadata().setDescription("Converted Sponge V"+origMetadata.getSchematicVersion()+", Schema "+origMetadata.getSchemaString());
        newSchem.getMetadata().setTimeCreated(origMetadata.getTimeCreated());
        newSchem.getMetadata().setTimeModifiedToNow();

//        world.clearEntities();
        TemporaryWorldManager.INSTANCE.removeTemporaryWorld("sponge_to_litematica");
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
        return newSchem.writeToFile(outputDir, outputFileName, override);
    }

    public static boolean convertSchematicaSchematicToLitematicaSchematic(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertSchematicaSchematicToLitematicaSchematic(inputDir, inputFileName, ignoreEntities, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
    }

    @Nullable
    public static LitematicaSchematic convertLitematicaSchematicToLitematicaSchematic(Path inputDir, String inputFileName,
                                                                                      String outputFilename,
                                                                                      IStringConsumer feedback)
    {
        DataFixerMode oldMode = (DataFixerMode) Configs.Generic.DATAFIXER_MODE.getOptionListValue();
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(DataFixerMode.ALWAYS);
        LitematicaSchematic newSchematic = LitematicaSchematic.createFromFile(inputDir, inputFileName, FileType.LITEMATICA_SCHEMATIC);

        if (newSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.litematic_to_litematica.failed_to_read_litematic");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return null;
        }

        SchematicMetadata origMetadata = newSchematic.getMetadata();

        if (origMetadata.getAuthor().isEmpty() || origMetadata.getAuthor() == "?")
        {
            newSchematic.getMetadata().setAuthor(GameWrap.getPlayerName());
        }
        else
        {
            newSchematic.getMetadata().setAuthor(origMetadata.getAuthor());
        }

        if (origMetadata.getName().isEmpty() || origMetadata.getName() == "?")
        {
            newSchematic.getMetadata().setName(outputFilename);
        }
        else
        {
            newSchematic.getMetadata().setName(origMetadata.getName());
        }

        newSchematic.getMetadata().setDescription("Converted Litematic V"+origMetadata.getSchematicVersion()+", Schema "+origMetadata.getSchemaString());
        newSchematic.getMetadata().setTimeCreated(origMetadata.getTimeCreated());
        newSchematic.getMetadata().setTimeModifiedToNow();

        Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
        return newSchematic;
    }

    @Nullable
    public static LitematicaSchematic convertSchematicaSchematicToLitematicaSchematic(Path inputDir, String inputFileName,
            boolean ignoreEntities, IStringConsumer feedback)
    {
        DataFixerMode oldMode = (DataFixerMode) Configs.Generic.DATAFIXER_MODE.getOptionListValue();
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(DataFixerMode.ALWAYS);
        SchematicaSchematic schematic = SchematicaSchematic.createFromFile(inputDir.resolve(inputFileName));

        if (schematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_read_schematic");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return null;
        }

//        WorldSchematic world = SchematicWorldHandler.createSchematicWorld(null);

        TemporaryWorldHolder holder = TemporaryWorldManager.INSTANCE.getTemporaryWorld("schematic_to_litematica", BlockPos.ZERO, schematic.getSize());
//        List<Pair<Integer, Integer>> tempChunks = loadChunksSchematicWorld(world, BlockPos.ZERO, schematic.getSize());
        StructurePlaceSettings placementSettings = new StructurePlaceSettings();
        placementSettings.setIgnoreEntities(ignoreEntities);
        schematic.placeSchematicDirectlyToChunks(holder.world(), BlockPos.ZERO, placementSettings);      // TODO FIXME

        String subRegionName = FileUtils.getNameWithoutExtension(inputFileName) + " (Converted Schematic)";
        AreaSelection area = new AreaSelection();
        area.setName(subRegionName);
        subRegionName = area.createNewSubRegionBox(BlockPos.ZERO, subRegionName);
        area.setSelectedSubRegionBox(subRegionName);
        Box box = area.getSelectedSubRegionBox();
        area.setSubRegionCornerPos(box, Corner.CORNER_1, BlockPos.ZERO);
        area.setSubRegionCornerPos(box, Corner.CORNER_2, (new BlockPos(schematic.getSize())).offset(-1, -1, -1));
        LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);

        LitematicaSchematic newSchematic = LitematicaSchematic.createFromWorld(holder.world(), area, info, "?", feedback);

        if (newSchematic != null && ignoreEntities == false)
        {
            newSchematic.takeEntityDataFromSchematicaSchematic(schematic, subRegionName);
        }
        else
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_create_schematic");
        }

        newSchematic.getMetadata().setName(subRegionName);
        newSchematic.getMetadata().setAuthor(GameWrap.getPlayerName());
        newSchematic.getMetadata().setDescription("Converted Schematica Schematic, Schema "+schematic.getMetadata().getSchema());
        newSchematic.getMetadata().setTimeCreated(System.currentTimeMillis());
        newSchematic.getMetadata().setTimeModifiedToNow();

//        world.clearEntities();
        TemporaryWorldManager.INSTANCE.removeTemporaryWorld("schematic_to_litematica");
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
        return newSchematic;
    }

    public static boolean convertStructureToLitematicaSchematic(Path structureDir, String structureFileName,
                                                                Path outputDir, String outputFileName, boolean override)
    {
        LitematicaSchematic litematicaSchematic = convertStructureToLitematicaSchematic(structureDir, structureFileName);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
    }

    public static boolean convertStructureToLitematicaSchematic(Path structureDir, String structureFileName,
            Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        DataFixerMode oldMode = (DataFixerMode) Configs.Generic.DATAFIXER_MODE.getOptionListValue();
        Configs.Generic.DATAFIXER_MODE.setOptionListValue(DataFixerMode.ALWAYS);
        LitematicaSchematic origStructure = convertStructureToLitematicaSchematic(structureDir, structureFileName);

        if (origStructure == null)
        {
            feedback.setString("litematica.error.schematic_conversion.structure_to_litematica.failed_to_read_structure");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return false;
        }

//        WorldSchematic world = SchematicWorldHandler.createSchematicWorld(null);
        BlockPos size = new BlockPos(origStructure.getTotalSize());
//        List<Pair<Integer, Integer>> tempChunks = loadChunksSchematicWorld(world, BlockPos.ZERO, size);
        TemporaryWorldHolder holder = TemporaryWorldManager.INSTANCE.getTemporaryWorld("structure_to_litematica", BlockPos.ZERO, size);
        SchematicPlacement schematicPlacement = SchematicPlacement.createForSchematicConversion(origStructure, BlockPos.ZERO);
        origStructure.placeToWorld(holder.world(), schematicPlacement, false);             // TODO FIXME

        String subRegionName = FileUtils.getNameWithoutExtension(structureFileName);
        AreaSelection area = new AreaSelection();
        area.setName(subRegionName);
        subRegionName = area.createNewSubRegionBox(BlockPos.ZERO, subRegionName);
        area.setSelectedSubRegionBox(subRegionName);
        Box box = area.getSelectedSubRegionBox();
        area.setSubRegionCornerPos(box, Corner.CORNER_1, BlockPos.ZERO);
        area.setSubRegionCornerPos(box, Corner.CORNER_2, size.offset(-1, -1, -1));
        LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);

        LitematicaSchematic newSchem = LitematicaSchematic.createFromWorld(holder.world(), area, info, "?", feedback);

        if (newSchem == null)
        {
            feedback.setString("litematica.error.schematic_conversion.structure_to_litematica.failed_to_create_litematic");
            Configs.Generic.DATAFIXER_MODE.setOptionListValue(oldMode);
            return false;
        }

        SchematicMetadata origMetadata = origStructure.getMetadata();

        if (origMetadata.getAuthor().isEmpty() || origMetadata.getAuthor() == "?")
        {
            newSchem.getMetadata().setAuthor(GameWrap.getPlayerName());
        }
        else
        {
            newSchem.getMetadata().setAuthor(origMetadata.getAuthor());
        }

        if (origMetadata.getName().isEmpty() || origMetadata.getName() == "?")
        {
            newSchem.getMetadata().setName(subRegionName);
        }
        else
        {
            newSchem.getMetadata().setName(origMetadata.getName());
        }

        newSchem.getMetadata().setDescription("Converted Vanilla Strucutre, Schema "+origMetadata.getSchemaString());
        newSchem.getMetadata().setTimeCreated(origMetadata.getTimeCreated());
        newSchem.getMetadata().setTimeModifiedToNow();

        boolean result = newSchem.writeToFile(outputDir, outputFileName, override);
//        System.out.printf("Vanilla IMPORT DUMP (OUT-2) -->\n%s\n", newSchem.toString());

        TemporaryWorldManager.INSTANCE.removeTemporaryWorld("structure_to_litematica");
//        world.clearEntities();
        return result;
    }

    @Nullable
    public static LitematicaSchematic convertSpongeSchematicToLitematicaSchematic(Path dir, String fileName)
    {
        try
        {
            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(dir, fileName, FileType.SPONGE_SCHEMATIC);

            if (schematic == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Failed to read the Sponge schematic from '" + fileName + '"');
            }

            return schematic;
        }
        catch (Exception e)
        {
            String msg = "Exception while trying to load the Sponge schematic: " + e.getMessage();
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, msg);
            Litematica.LOGGER.error(msg);
        }

        return null;
    }

    @Nullable
    public static LitematicaSchematic convertStructureToLitematicaSchematic(Path structureDir, String structureFileName)
    {
        try
        {
            LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromFile(structureDir, structureFileName, FileType.VANILLA_STRUCTURE);

            if (litematicaSchematic == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Failed to read the vanilla structure template from '" + structureFileName + '"');
            }

            return litematicaSchematic;
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Exception while trying to load the vanilla structure: " + e.getMessage());
            Litematica.LOGGER.error("Exception while trying to load the vanilla structure: " + e.getMessage());
        }

        return null;
    }

    public static boolean convertLitematicaSchematicToSchematicaSchematic(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        //SchematicaSchematic schematic = convertLitematicaSchematicToSchematicaSchematic(inputDir, inputFileName, ignoreEntities, feedback);
        //return schematic != null && schematic.writeToFile(outputDir, outputFileName, override, feedback);
        // TODO 1.13
        return false;
    }

    public static boolean convertLitematicaSchematicToV6LitematicaSchematic(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic v7LitematicaSchematic = LitematicaSchematic.createFromFile(inputDir, inputFileName, FileType.LITEMATICA_SCHEMATIC);

        if (v7LitematicaSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.litematic_to_litematica.failed_to_read_schematic");
            return false;
        }

        LitematicaSchematic v6LitematicaSchematic = LitematicaSchematic.createEmptySchematicFromExisting(v7LitematicaSchematic, Minecraft.getInstance().player.getName().getString());
        v6LitematicaSchematic.downgradeV7toV6Schematic(v7LitematicaSchematic);

        if (v6LitematicaSchematic.writeToFile(outputDir, outputFileName, override, true))
        {
            return true;
        }
        else
        {
            feedback.setString("litematica.error.schematic_conversion.litematic_to_litematica.failed_to_downgrade_litematic");
            return false;
        }
    }

    public static boolean convertLitematicaSchematicToVanillaStructure(
            Path inputDir, String inputFileName, Path outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        StructureTemplate template = convertLitematicaSchematicToVanillaStructure(inputDir, inputFileName, ignoreEntities, feedback);
        return writeVanillaStructureToFile(template, outputDir, outputFileName, override, feedback);
    }

    @Nullable
    public static StructureTemplate convertLitematicaSchematicToVanillaStructure(Path inputDir, String inputFileName, boolean ignoreEntities, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromFile(inputDir, inputFileName);

        if (litematicaSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.litematic_to_structure.failed_to_read_litematic");
            return null;
        }

//        WorldSchematic world = SchematicWorldHandler.createSchematicWorld(null);

        BlockPos size = new BlockPos(litematicaSchematic.getTotalSize());
//        List<Pair<Integer, Integer>> tempChunks = loadChunksSchematicWorld(world, BlockPos.ZERO, size);
        SchematicPlacement schematicPlacement = SchematicPlacement.createForSchematicConversion(litematicaSchematic, BlockPos.ZERO);
        TemporaryWorldHolder holder = TemporaryWorldManager.INSTANCE.getTemporaryWorld("litematic_to_structure", BlockPos.ZERO, size);
        litematicaSchematic.placeToWorld(holder.world(), schematicPlacement, false);         // TODO FIXME

        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(holder.world(), BlockPos.ZERO, size, ignoreEntities == false, List.of(Blocks.STRUCTURE_VOID));

//        world.clearEntities();
        TemporaryWorldManager.INSTANCE.removeTemporaryWorld("litematic_to_structure");
        return template;
    }

    private static boolean writeVanillaStructureToFile(StructureTemplate template, Path dir, String fileNameIn, boolean override, IStringConsumer feedback)
    {
        String fileName = fileNameIn;
        String extension = ".nbt";

        if (fileName.endsWith(extension) == false)
        {
            fileName = fileName + extension;
        }

        Path file = dir.resolve(fileName);
        FileOutputStream os = null;

        try
        {
            if (!Files.exists(dir))
            {
                FileUtils.createDirectoriesIfMissing(dir);
            }

            if (!Files.isDirectory(dir))
            {
                feedback.setString(StringUtils.translate("litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.toAbsolutePath()));
                return false;
            }

            if (override == false && Files.exists(file))
            {
                feedback.setString(StringUtils.translate("litematica.error.structure_write_to_file_failed.exists", file.toAbsolutePath()));
                return false;
            }

            /*
            NbtCompound tag = template.writeNbt(new NbtCompound());
            os = new FileOutputStream(file);
            NbtIo.writeCompressed(tag, os);
            os.close();
             */

            NbtIo.writeCompressed(template.save(new CompoundTag()), file);

            return true;
        }
        catch (Exception e)
        {
            feedback.setString(StringUtils.translate("litematica.error.structure_write_to_file_failed.exception", file.toAbsolutePath()));
        }

        return false;
    }

    public static boolean isClientChunkLoaded(ClientLevel world, int chunkX, int chunkZ)
    {
        boolean test = ((ClientChunkCache) world.getChunkSource()).getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;

        //System.out.printf("isClientChunkLoaded(): World: [%s] / ChunkPos[%d, %d] --> [%s]\n", world.toString(), chunkX, chunkZ, test);

        return test;
    }

    @Deprecated
    public static List<Pair<Integer, Integer>> loadChunksSchematicWorld(WorldSchematic world, BlockPos origin, Vec3i areaSize)
    {
        List<Pair<Integer, Integer>> chunks = new ArrayList<>();
        BlockPos posEnd = origin.offset(PositionUtils.getRelativeEndPositionFromAreaSize(areaSize));
        BlockPos posMin = PositionUtils.getMinCorner(origin, posEnd);
        BlockPos posMax = PositionUtils.getMaxCorner(origin, posEnd);
        final int cxMin = posMin.getX() >> 4;
        final int czMin = posMin.getZ() >> 4;
        final int cxMax = posMax.getX() >> 4;
        final int czMax = posMax.getZ() >> 4;

        for (int cz = czMin; cz <= czMax; ++cz)
        {
            for (int cx = cxMin; cx <= cxMax; ++cx)
            {
                world.getChunkSource().loadChunk(cx, cz);         // TODO FIXME
                chunks.add(Pair.of(cx, cz));
            }
        }

        return chunks;
    }

    public static void setToolModeBlockState(ToolMode mode, boolean primary, Minecraft mc)
    {
        BlockState state = Blocks.AIR.defaultBlockState();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(mc.level, entity, getValidBlockRange(mc));

        if (wrapper != null)
        {
            BlockHitResult trace = wrapper.getBlockHitResult();

            if (trace != null && trace.getType() == HitResult.Type.BLOCK)
            {
                BlockPos pos = trace.getBlockPos();

                if (wrapper.getHitType() == HitType.SCHEMATIC_BLOCK)
                {
                    state = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
                }
                else if (wrapper.getHitType() == HitType.VANILLA_BLOCK)
                {
                    state = mc.level.getBlockState(pos);
                }
            }
        }

        if (primary)
        {
            mode.setPrimaryBlock(state);
        }
        else
        {
            mode.setSecondaryBlock(state);
        }
    }

    /**
     * Does a ray trace to the schematic world, and returns either the closest or the furthest hit block.
     * @param closest
     * @param mc
     * @return true if the correct item was or is in the player's hand after the pick block
     */
    public static boolean doSchematicWorldPickBlock(boolean closest, Minecraft mc)
    {
        BlockPos pos;

		if (SchematicPickBlockEventHandler.getInstance().onSchematicPickBlockStart(closest))
		{
			return true;
		}

        if (closest)
        {
            pos = RayTraceUtils.getSchematicWorldTraceIfClosest(mc.level, mc.player, getValidBlockRange(mc));
        }
        else
        {
            pos = RayTraceUtils.getFurthestSchematicWorldBlockBeforeVanilla(mc.level, mc.player, getValidBlockRange(mc), true);
        }

        if (pos != null)
        {
            Level world = SchematicWorldHandler.getSchematicWorld();

			if (world != null)
			{
				BlockState state = world.getBlockState(pos);

				if (SchematicPickBlockEventHandler.getInstance().onSchematicPickBlockPreGather(world, pos, state))
				{
					return true;
				}

				ItemStack stack;

				if (SchematicPickBlockEventHandler.getInstance().hasPickStack())
				{
					stack = SchematicPickBlockEventHandler.getInstance().getPickStack();
				}
				else
				{
					stack = MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);
				}

				if (SchematicPickBlockEventHandler.getInstance().onSchematicPickBlockPrePick(world, pos, state, stack))
				{
					return true;
				}

				if (SchematicPickBlockEventHandler.getInstance().hasSlotHandler())
				{
					if (SchematicPickBlockEventHandler.getInstance().executePickBlockHandler(world, pos, stack))
					{
						SchematicPickBlockEventHandler.getInstance().onSchematicPickBlockSuccess();
						return true;
					}
				}

				InventoryUtils.schematicWorldPickBlock(stack, pos, world, mc);
				SchematicPickBlockEventHandler.getInstance().onSchematicPickBlockSuccess();

				return true;
			}
        }

        return false;
    }

    public static void insertSignTextFromSchematic(SignBlockEntity beClient, String[] screenTextArr, boolean front)
    {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();

        if (worldSchematic != null)
        {
            BlockEntity beSchem = worldSchematic.getBlockEntity(beClient.getBlockPos());

            if (beSchem instanceof SignBlockEntity)
            {
                IMixinSignBlockEntity beMixinSchem = (IMixinSignBlockEntity) beSchem;
                SignText textSchematic = front ? beMixinSchem.litematica_getFrontText() : beMixinSchem.litematica_getBackText();

                if (textSchematic != null)
                {
                    for (int i = 0; i < screenTextArr.length; ++i)
                    {
                        screenTextArr[i] = textSchematic.getMessage(i, false).getString();
                    }
                    beClient.setText(textSchematic, front);
                }
            }
        }
    }

    /**
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    public static void easyPlaceOnUseTick(Minecraft mc)
    {
        if (mc.player != null && DataManager.getToolMode() != ToolMode.REBUILD &&
            Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() &&
            Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld() &&
			Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue() == false)
        {
            WorldUtils.doEasyPlaceAction(mc);
        }
    }

    /**
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    public static boolean handleEasyPlace(Minecraft mc)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_POST_REWRITE.getBooleanValue() == false &&
            DataManager.getToolMode() != ToolMode.REBUILD)
        {
            InteractionResult result = doEasyPlaceAction(mc);

            if (result == InteractionResult.FAIL)
            {
                MessageOutputType type = (MessageOutputType) Configs.Generic.PLACEMENT_RESTRICTION_WARN.getOptionListValue();

                if (type == MessageOutputType.MESSAGE)
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.easy_place_fail");
                }
                else if (type == MessageOutputType.ACTIONBAR)
                {
                    InfoUtils.printActionbarMessage("litematica.message.easy_place_fail");
                }

                return true;
            }

            return result != InteractionResult.PASS;
        }

        return false;
    }

    /**
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    private static InteractionResult doEasyPlaceAction(Minecraft mc)
    {
        RayTraceWrapper traceWrapper;
        double traceMaxRange = getValidBlockRange(mc);

        if (Configs.Generic.EASY_PLACE_FIRST.getBooleanValue())
        {
            // Temporary hack, using this same config here
            boolean targetFluids = Configs.InfoOverlays.INFO_OVERLAYS_TARGET_FLUIDS.getBooleanValue();
            traceWrapper = RayTraceUtils.getGenericTrace(mc.level, mc.player, traceMaxRange, true, targetFluids, false);
        }
        else
        {
            traceWrapper = RayTraceUtils.getFurthestSchematicWorldTraceBeforeVanilla(mc.level, mc.player, traceMaxRange);

            if (traceWrapper == null && placementRestrictionInEffect(mc))
            {
                return InteractionResult.FAIL;
            }
        }

        if (traceWrapper == null)
        {
            return InteractionResult.PASS;
        }

        if (traceWrapper.getHitType() == HitType.SCHEMATIC_BLOCK)
        {
            BlockHitResult trace = traceWrapper.getBlockHitResult();
            HitResult traceVanilla = RayTraceUtils.getRayTraceFromEntity(mc.level, mc.player, false, traceMaxRange);
            BlockPos pos = trace.getBlockPos();
            Level world = SchematicWorldHandler.getSchematicWorld();
            BlockState stateSchematic = world.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic, world, pos);

            // Already placed to that position, possible server sync delay
            if (EasyPlaceUtils.easyPlaceIsPositionCached(pos))
            {
                return InteractionResult.FAIL;
            }

            // Ignore action if too fast
            if (EasyPlaceUtils.easyPlaceIsTooFast())
            {
                return InteractionResult.FAIL;
            }

            if (stack.isEmpty() == false)
            {
                BlockState stateClient = mc.level.getBlockState(pos);

                if (stateSchematic == stateClient)
                {
                    return InteractionResult.FAIL;
                }

                // Abort if there is already a block in the target position
                if (easyPlaceBlockChecksCancel(stateSchematic, stateClient, mc.player, traceVanilla, stack))
                {
                    return InteractionResult.FAIL;
                }

                InventoryUtils.schematicWorldPickBlock(stack, pos, world, mc);
                InteractionHand hand = EntityUtils.getUsedHandForItem(mc.player, stack);

                // Abort if a wrong item is in the player's hand
                if (hand == null)
                {
                    return InteractionResult.FAIL;
                }

                Vec3 hitPos = trace.getLocation();
                Direction sideOrig = trace.getDirection();
                EasyPlaceProtocol protocol = PlacementHandler.getEffectiveProtocolVersion();

                if (protocol == EasyPlaceProtocol.NONE || protocol == EasyPlaceProtocol.SLAB_ONLY)
                {
                    // If there is a block in the world right behind the targeted schematic block, then use
                    // that block as the click position
                    if (traceVanilla != null && traceVanilla.getType() == HitResult.Type.BLOCK)
                    {
                        BlockHitResult hitResult = (BlockHitResult) traceVanilla;
                        BlockPos posVanilla = hitResult.getBlockPos();
                        Direction sideVanilla = hitResult.getDirection();
                        BlockState stateVanilla = mc.level.getBlockState(posVanilla);
                        Vec3 hit = traceVanilla.getLocation();
                        BlockPlaceContext ctx = new BlockPlaceContext(new UseOnContext(mc.player, hand, hitResult));

                        if (stateVanilla.canBeReplaced(ctx) == false)
                        {
                            posVanilla = posVanilla.relative(sideVanilla);

                            if (pos.equals(posVanilla))
                            {
                                hitPos = hit;
                                sideOrig = sideVanilla;
                            }
                        }
                    }
                }

                //System.out.printf("doEasyPlaceAction - stateSchematic [%s] // sideOrig [%s]\n", stateSchematic.toString(), sideOrig.getName());

                Direction side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);

                // Support for special cases
                PlacementProtocolData placementData = applyPlacementProtocolAll(pos, stateSchematic, hitPos);

                if (placementData.mustFail)
                {
                    return InteractionResult.FAIL; //disallowed cases (e.g. trying to place torch with no support block)
                }

                if (placementData.handled)
                {
                    pos = placementData.pos;
                    side = placementData.side;
                    hitPos = placementData.hitVec;
                }

                if (protocol == EasyPlaceProtocol.V3)
                {
                    hitPos = applyPlacementProtocolV3(pos, stateSchematic, hitPos);
                }
                else if (protocol == EasyPlaceProtocol.V2)
                {
                    // Carpet Accurate Block Placement protocol support, plus slab support
                    hitPos = applyCarpetProtocolHitVec(pos, stateSchematic, hitPos);
                }
                else if (protocol == EasyPlaceProtocol.SLAB_ONLY)
                {
                    // Slab support only
                    hitPos = applyBlockSlabProtocol(pos, stateSchematic, hitPos);
                }

                // Mark that this position has been handled (use the non-offset position that is checked above)
                EasyPlaceUtils.cacheEasyPlacePosition(pos);

                BlockHitResult hitResult = new BlockHitResult(hitPos, side, pos, false);

                //System.out.printf("interact -> pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                // pos, side, hitPos
                InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hitResult);

                // swing hand fix, see MinecraftClient#doItemUse
                if (InteractionResult.SUCCESS.swingSource().equals(InteractionResult.SwingSource.CLIENT) &&
                    Configs.Generic.EASY_PLACE_SWING_HAND.getBooleanValue())
                {
                    mc.player.swing(hand);
                }

                if (stateSchematic.getBlock() instanceof SlabBlock && stateSchematic.getValue(SlabBlock.TYPE) == SlabType.DOUBLE)
                {
                    stateClient = mc.level.getBlockState(pos);

                    if (stateClient.getBlock() instanceof SlabBlock && stateClient.getValue(SlabBlock.TYPE) != SlabType.DOUBLE)
                    {
                        side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                        hitResult = new BlockHitResult(hitPos, side, pos, false);
                        mc.gameMode.useItemOn(mc.player, hand, hitResult);
                    }
                }
            }

            return InteractionResult.SUCCESS;
        }
        else if (traceWrapper.getHitType() == HitType.VANILLA_BLOCK)
        {
            return placementRestrictionInEffect(mc) ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        return InteractionResult.PASS;
    }

    /**
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    private static boolean easyPlaceBlockChecksCancel(BlockState stateSchematic, BlockState stateClient,
            Player player, HitResult trace, ItemStack stack)
    {
        Block blockSchematic = stateSchematic.getBlock();

        if (blockSchematic instanceof SlabBlock && stateSchematic.getValue(SlabBlock.TYPE) == SlabType.DOUBLE)
        {
            Block blockClient = stateClient.getBlock();

            if (blockClient instanceof SlabBlock && stateClient.getValue(SlabBlock.TYPE) != SlabType.DOUBLE)
            {
                return blockSchematic != blockClient;
            }
        }

        if (trace.getType() != HitResult.Type.BLOCK)
        {
            return false;
        }

        BlockHitResult hitResult = (BlockHitResult) trace;
        BlockPlaceContext ctx = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, hitResult));

        return !stateClient.canBeReplaced(ctx);
    }

    /**
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    public static class PlacementProtocolData
    {
        boolean handled;
        boolean mustFail;
        BlockPos pos;
        Direction side;
        Vec3 hitVec;
    }

    /**
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    public static PlacementProtocolData applyPlacementProtocolAll(BlockPos pos, BlockState stateSchematic, Vec3 hitVecIn)
    {
        PlacementProtocolData placementData = new PlacementProtocolData();

        Block stateBlock = stateSchematic.getBlock();
        final Level world = Minecraft.getInstance().level;

        //Wall-mountable blocks
        if (stateBlock instanceof BaseTorchBlock ||
            stateBlock instanceof AbstractBannerBlock ||
            stateBlock instanceof SignBlock ||
            stateBlock instanceof AbstractSkullBlock)
        {
            placementData.handled = true;
            placementData.hitVec = hitVecIn;

            if (stateBlock instanceof WallTorchBlock ||
                stateBlock instanceof RedstoneWallTorchBlock ||
                stateBlock instanceof WallBannerBlock ||
                stateBlock instanceof WallSignBlock ||
                stateBlock instanceof WallSkullBlock)
            {
                placementData.side = stateSchematic.getValue(BlockStateProperties.HORIZONTAL_FACING);
                placementData.pos = pos.relative(placementData.side.getOpposite());
            }
            else
            {
                placementData.side = Direction.UP;
                placementData.pos = pos.below();
            }

            //If the supporting block doesn't exist, fail
            BlockState stateFacing = world.getBlockState(placementData.pos);
            if (stateFacing == null || stateFacing.isAir())
            {
                placementData.mustFail = true;
            }
        }
        else if (stateBlock instanceof FaceAttachedHorizontalDirectionalBlock)
        {
            //If the supporting block doesn't exist, fail
            if (!((IMixinWallMountedBlock)stateBlock).litematica_invokeCanPlaceAt(stateSchematic, world, pos))
                placementData.mustFail = true;
        }

        return placementData;
    }

    /**
     * Apply the Carpet-Extra mod accurate block placement protocol support
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    public static Vec3 applyCarpetProtocolHitVec(BlockPos pos, BlockState state, Vec3 hitVecIn)
    {
        double x = hitVecIn.x;
        double y = hitVecIn.y;
        double z = hitVecIn.z;
        Block block = state.getBlock();
        Optional<Direction> facing = BlockUtils.getFirstPropertyFacingValue(state);
        final int propertyIncrement = 16;
        boolean hasData = false;
        int protocolValue = 0;

        if (facing.isPresent())
        {
            //System.out.printf("(WorldUtils):v2: applying: 0x%08X (getFirstDirectionProperty() -> %s)\n", protocolValue, facing.get().getName());

            protocolValue = facing.get().get3DDataValue();
            hasData = true; // without this down rotation would not be detected >_>
        }
        else if (state.hasProperty(BlockStateProperties.AXIS))
        {
            Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
            //System.out.printf("(WorldUtils):v2: 0x%08X (current axis %s)\n", protocolValue, axis.getName());

            protocolValue = axis.ordinal();
            hasData = true; // without this id 0 would not be detected >_>
            //System.out.printf("(WorldUtils):v2: axis current state: %s, protocolValue 0x%08X\n", state.toString(), protocolValue);
        }

        if (block instanceof RepeaterBlock)
        {
            protocolValue += state.getValue(RepeaterBlock.DELAY) * propertyIncrement;
        }
        else if (block instanceof ComparatorBlock && state.getValue(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT)
        {
            protocolValue += propertyIncrement;
        }
        else if (state.hasProperty(BlockStateProperties.HALF) && state.getValue(BlockStateProperties.HALF) == Half.TOP)
        {
            protocolValue += propertyIncrement;
        }
        else if (state.hasProperty(BlockStateProperties.SLAB_TYPE) && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.TOP)
        {
            protocolValue += propertyIncrement;
        }

        y = applySlabOrStairHitVecY(y, pos, state);

        if (protocolValue != 0 || hasData)
        {
            x += (protocolValue * 2) + 2;
        }

        //System.out.printf("(WorldUtils):v2: stateIn: %s // Vec3d Out [%s]\n", state.toString(), new Vec3d(x, y, z).toString());

        return new Vec3(x, y, z);
    }

    /**
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    private static double applySlabOrStairHitVecY(double origY, BlockPos pos, BlockState state)
    {
        double y = origY;

        if (state.hasProperty(BlockStateProperties.SLAB_TYPE))
        {
            y = pos.getY();

            if (state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.TOP)
            {
                y += 0.99;
            }
        }
        else if (state.hasProperty(BlockStateProperties.HALF))
        {
            y = pos.getY();

            if (state.getValue(BlockStateProperties.HALF) == Half.TOP)
            {
                y += 0.99;
            }
        }

        return y;
    }

    /**
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    public static Vec3 applyBlockSlabProtocol(BlockPos pos, BlockState state, Vec3 hitVecIn)
    {
        double newY = applySlabOrStairHitVecY(hitVecIn.y, pos, state);
        return newY != hitVecIn.y ? new Vec3(hitVecIn.x, newY, hitVecIn.z) : hitVecIn;
    }

    /**
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    public static <T extends Comparable<T>> Vec3 applyPlacementProtocolV3(BlockPos pos, BlockState state, Vec3 hitVecIn)
    {
        Collection<Property<?>> props = state.getBlock().getStateDefinition().getProperties();

        if (props.isEmpty())
        {
            return hitVecIn;
        }

        double relX = hitVecIn.x - pos.getX();
        int protocolValue = 0;
        int shiftAmount = 1;
        int propCount = 0;

        //System.out.printf("(WorldUtils):v3: hit vec.x %s, pos.x: %s\n", hitVecIn.getX(), pos.getX());
        //System.out.printf("(WorldUtils):v3: raw protocol value in: 0x%08X\n", protocolValue);

        Optional<EnumProperty<Direction>> property = BlockUtils.getFirstDirectionProperty(state);

        // DirectionProperty - allow all except: VERTICAL_DIRECTION (PointedDripstone)
        if (property.isPresent() && property.get() != BlockStateProperties.VERTICAL_DIRECTION)
        {
            Direction direction = state.getValue(property.get());
            protocolValue |= direction.get3DDataValue() << shiftAmount;
            //System.out.printf("(WorldUtils):v3: applying: 0x%08X (getFirstDirection %s)\n", protocolValue, property.get().getName());
            shiftAmount += 3;
            ++propCount;
        }

        List<Property<?>> propList = new ArrayList<>(props);
        propList.sort(Comparator.comparing(Property::getName));

        try
        {
            for (Property<?> p : propList)
            {
                //System.out.printf("(WorldUtils):v3: check property [%s], whitelisted [%s], blacklisted [%s]\n", p.getName(), PlacementHandler.WHITELISTED_PROPERTIES.contains(p), PlacementHandler.BLACKLISTED_PROPERTIES.contains(p));

                if (property.isPresent() && property.get().equals(p))
                {
                    //System.out.printf("(WorldUtils):v3: skipping prot val: 0x%08X [Property %s]\n", protocolValue, p.getName());
                    continue;
                }
                if (PlacementHandler.WHITELISTED_PROPERTIES.contains(p) &&
                    !PlacementHandler.BLACKLISTED_PROPERTIES.contains(p))
                {
                    @SuppressWarnings("unchecked")
                    Property<T> prop = (Property<T>) p;
                    List<T> list = new ArrayList<>(prop.getPossibleValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = Mth.log2(Mth.smallestEncompassingPowerOfTwo(list.size()));
                    int valueIndex = list.indexOf(state.getValue(prop));

                    //System.out.printf("(WorldUtils):v3: trying to apply valInd: %d, bits: %d, prot val: 0x%08X [Property %s]\n", valueIndex, requiredBits, protocolValue, prop.getName());

                    if (valueIndex != -1)
                    {
                        //System.out.printf("(WorldUtils):v3: requesting: %s = %s, index: %d\n", prop.getName(), state.get(prop), valueIndex);
                        protocolValue |= (valueIndex << shiftAmount);
                        shiftAmount += requiredBits;
                        ++propCount;
                    }
                }
                /*
                else
                {
                    System.out.printf("(WorldUtils):v3: skipping prot val: 0x%08X [Property %s]\n", protocolValue, p.getName());
                }
                 */
            }
        }
        catch (Exception e)
        {
            Litematica.LOGGER.warn("Exception trying to request placement protocol value", e);
        }

        if (propCount > 0)
        {
            double x = pos.getX() + relX + 2 + protocolValue;
            //System.out.printf("(WorldUtils):v3: request prot value 0x%08X\n", protocolValue + 2);
            return new Vec3(x, hitVecIn.y, hitVecIn.z);
        }

        return hitVecIn;
    }

    /**
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    public static Direction applyPlacementFacing(BlockState stateSchematic, Direction side, BlockState stateClient)
    {
        Block blockSchematic = stateSchematic.getBlock();
        Block blockClient = stateClient.getBlock();

        if (blockSchematic instanceof SlabBlock)
        {
            if (stateSchematic.getValue(SlabBlock.TYPE) == SlabType.DOUBLE &&
                blockClient instanceof SlabBlock &&
                stateClient.getValue(SlabBlock.TYPE) != SlabType.DOUBLE)
            {
                if (stateClient.getValue(SlabBlock.TYPE) == SlabType.TOP)
                {
                    return Direction.DOWN;
                }
                else
                {
                    return Direction.UP;
                }
            }
            // Single slab
            else
            {
                return Direction.NORTH;
            }
        }
        else if (stateSchematic.hasProperty(BlockStateProperties.HALF))
        {
            side = stateSchematic.getValue(BlockStateProperties.HALF) == Half.TOP ? Direction.DOWN : Direction.UP;
        }

        return side;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @return
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    public static boolean handlePlacementRestriction(Minecraft mc)
    {
        boolean cancel = placementRestrictionInEffect(mc);

        if (cancel)
        {
            MessageOutputType type = (MessageOutputType) Configs.Generic.PLACEMENT_RESTRICTION_WARN.getOptionListValue();

            if (type == MessageOutputType.MESSAGE)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.placement_restriction_fail");
            }
            else if (type == MessageOutputType.ACTIONBAR)
            {
                InfoUtils.printActionbarMessage("litematica.message.placement_restriction_fail");
            }
        }

        return cancel;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @return true if the use action should be cancelled
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    private static boolean placementRestrictionInEffect(Minecraft mc)
    {
        HitResult trace = mc.hitResult;

        ItemStack stack = mc.player.getMainHandItem();

        if (stack.isEmpty())
        {
            stack = mc.player.getOffhandItem();
        }

        if (stack.isEmpty())
        {
            return false;
        }

        if (trace != null && trace.getType() == HitResult.Type.BLOCK)
        {
            BlockHitResult blockHitResult = (BlockHitResult) trace;
            BlockPlaceContext ctx = new BlockPlaceContext(new UseOnContext(mc.player, InteractionHand.MAIN_HAND, blockHitResult));

            // Get the possibly offset position, if the targeted block is not replaceable
            BlockPos pos = ctx.getClickedPos();

            BlockState stateClient = mc.level.getBlockState(pos);

            Level worldSchematic = SchematicWorldHandler.getSchematicWorld();
            LayerRange range = DataManager.getRenderLayerRange();
            boolean schematicHasAir = worldSchematic.isEmptyBlock(pos);

            // The targeted position is outside the current render range
            if (schematicHasAir == false && range.isPositionWithinRange(pos) == false)
            {
                return true;
            }

            // There should not be anything in the targeted position,
            // and the position is within or close to a schematic sub-region
            if (schematicHasAir && isPositionWithinRangeOfSchematicRegions(pos, 2))
            {
                return true;
            }

            blockHitResult = new BlockHitResult(blockHitResult.getLocation(), blockHitResult.getDirection(), pos, false);
            ctx = new BlockPlaceContext(new UseOnContext(mc.player, InteractionHand.MAIN_HAND, blockHitResult));

            // Placement position is already occupied
            if (stateClient.canBeReplaced(ctx) == false)
            {
                return true;
            }

            BlockState stateSchematic = worldSchematic.getBlockState(pos);
            stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic, worldSchematic, pos);

            // The player is holding the wrong item for the targeted position
            if (stack.isEmpty() == false && EntityUtils.getUsedHandForItem(mc.player, stack) == null)
            {
                return true;
            }

            // Ignore if schematic block is wall-mountable and orientation is wrong
            Block schematicBlock = stateSchematic.getBlock();
            if (schematicBlock instanceof WallTorchBlock ||
                schematicBlock instanceof RedstoneWallTorchBlock ||
                schematicBlock instanceof WallBannerBlock ||
                schematicBlock instanceof WallSignBlock ||
                schematicBlock instanceof WallSkullBlock)
            {
                if (blockHitResult.getDirection() != stateSchematic.getValue(BlockStateProperties.HORIZONTAL_FACING))
                    return true;
            }

            // Orientation is wrong
            BlockState attemptState = schematicBlock.getStateForPlacement(ctx);
            return !isMatchingStatePlacementRestriction (attemptState, stateSchematic);
        }

        return false;
    }

    /**
     * @deprecated Moving to {@link EasyPlaceUtils}
     */
    @Deprecated
    private static boolean isMatchingStatePlacementRestriction (BlockState state1, BlockState state2)
    {
        if (state1 == null || state2 == null)
            return false;
        if (state1 == state2)
            return true;

        Property<?>[] orientationProperties = new Property<?>[] {
                BlockStateProperties.FACING, //pistons
                BlockStateProperties.HALF, //stairs, trapdoors
                BlockStateProperties.FACING_HOPPER,
                BlockStateProperties.DOOR_HINGE,
                BlockStateProperties.HORIZONTAL_FACING, //small dripleaf
                BlockStateProperties.AXIS, //logs
                BlockStateProperties.SLAB_TYPE,
                BlockStateProperties.VERTICAL_DIRECTION,
                BlockStateProperties.ROTATION_16, //banners
                BlockStateProperties.HANGING, //lanterns
                BlockStateProperties.ATTACH_FACE, //lever, button, grindstone
                BlockStateProperties.BELL_ATTACHMENT, //bell (double-check for single-wall / double-wall)
                //Properties.HORIZONTAL_AXIS, //Nether portals, though they aren't directly placeable
                //Properties.ORIENTATION, //jigsaw blocks, Crafters
        };

        for (Property<?> property : orientationProperties)
        {
            boolean hasProperty1 = state1.hasProperty(property);
            boolean hasProperty2 = state2.hasProperty(property);

            if (hasProperty1 != hasProperty2)
                return false;
            if (!hasProperty1)
                continue;

            if (state1.getValue(property) != state2.getValue(property))
                return false;
        }

        //Other properties are considered as matching
        return true;
    }

    public static boolean isPositionWithinRangeOfSchematicRegions(BlockPos pos, int range)
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        final int minCX = (x - range) >> 4;
        final int minCZ = (z - range) >> 4;
        final int maxCX = (x + range) >> 4;
        final int maxCZ = (z + range) >> 4;

        for (int cz = minCZ; cz <= maxCZ; ++cz)
        {
            for (int cx = minCX; cx <= maxCX; ++cx)
            {
                List<PlacementPart> parts = manager.getPlacementPartsInChunk(cx, cz);

                for (PlacementPart part : parts)
                {
                    IntBoundingBox box = part.bb;

                    if (x >= box.minX() - range && x <= box.maxX() + range &&
                        y >= box.minY() - range && y <= box.maxY() + range &&
                        z >= box.minZ() - range && z <= box.maxZ() + range)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if the given one block thick slice has non-air blocks or not.
     * NOTE: The axis is the perpendicular axis (that goes through the plane).
     * @param axis
     * @param pos1
     * @param pos2
     * @return
     */
    public static boolean isSliceEmpty(Level world, Direction.Axis axis, BlockPos pos1, BlockPos pos2)
    {
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        switch (axis)
        {
            case Z:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int z = pos1.getZ();
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);

                for (int cx = cxMin; cx <= cxMax; ++cx)
                {
                    ChunkAccess chunk = world.getChunk(cx, z >> 4);
                    int xMin = Math.max(x1,  cx << 4      );
                    int xMax = Math.min(x2, (cx << 4) + 15);
                    int yMax = Math.min(y2, fi.dy.masa.malilib.util.WorldUtils.getHighestSectionYOffset(chunk) + 15);

                    for (int x = xMin; x <= xMax; ++x)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }

            case Y:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y = pos1.getY();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    for (int cx = cxMin; cx <= cxMax; ++cx)
                    {
                        ChunkAccess chunk = world.getChunk(cx, cz);

                        if (y > fi.dy.masa.malilib.util.WorldUtils.getHighestSectionYOffset(chunk) + 15)
                        {
                            continue;
                        }

                        int xMin = Math.max(x1,  cx << 4      );
                        int xMax = Math.min(x2, (cx << 4) + 15);
                        int zMin = Math.max(z1,  cz << 4      );
                        int zMax = Math.min(z2, (cz << 4) + 15);

                        for (int z = zMin; z <= zMax; ++z)
                        {
                            for (int x = xMin; x <= xMax; ++x)
                            {
                                if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                                {
                                    return false;
                                }
                            }
                        }
                    }
                }

                break;
            }

            case X:
            {
                int x = pos1.getX();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    ChunkAccess chunk = world.getChunk(x >> 4, cz);
                    int zMin = Math.max(z1,  cz << 4      );
                    int zMax = Math.min(z2, (cz << 4) + 15);
                    int yMax = Math.min(y2, fi.dy.masa.malilib.util.WorldUtils.getHighestSectionYOffset(chunk) + 15);

                    for (int z = zMin; z <= zMax; ++z)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }
        }

        return true;
    }

	/**
	 * Moved to {@link EasyPlaceUtils}
	 */
	@Deprecated(forRemoval = true)
    private static boolean easyPlaceIsPositionCached(BlockPos pos)
    {
        long currentTime = System.nanoTime();
        boolean cached = false;

        for (int i = 0; i < EASY_PLACE_POSITIONS.size(); ++i)
        {
            PositionCache val = EASY_PLACE_POSITIONS.get(i);
            boolean expired = val.hasExpired(currentTime);

            if (expired)
            {
                EASY_PLACE_POSITIONS.remove(i);
                --i;
            }
            else if (val.getPos().equals(pos))
            {
                cached = true;

                // Keep checking and removing old entries if there are a fair amount
                if (EASY_PLACE_POSITIONS.size() < 16)
                {
                    break;
                }
            }
        }

        return cached;
    }

	/**
	 * Moved to {@link EasyPlaceUtils}
	 */
	@Deprecated(forRemoval = true)
	private static void cacheEasyPlacePosition(BlockPos pos)
    {
        EASY_PLACE_POSITIONS.add(new PositionCache(pos, System.nanoTime(), 2000000000));
    }

	/**
	 * Moved to {@link EasyPlaceUtils}
	 */
	@Deprecated(forRemoval = true)
    public static class PositionCache
    {
        private final BlockPos pos;
        private final long time;
        private final long timeout;

        private PositionCache(BlockPos pos, long time, long timeout)
        {
            this.pos = pos;
            this.time = time;
            this.timeout = timeout;
        }

        public BlockPos getPos()
        {
            return this.pos;
        }

        public boolean hasExpired(long currentTime)
        {
            return currentTime - this.time > this.timeout;
        }
    }

	/**
	 * Moved to {@link EasyPlaceUtils}
	 */
	@Deprecated(forRemoval = true)
    private static boolean easyPlaceIsTooFast()
    {
        return System.nanoTime() - easyPlaceLastPickBlockTime < 1000000L * Configs.Generic.EASY_PLACE_SWAP_INTERVAL.getIntegerValue();
    }

	/**
	 * Moved to {@link EasyPlaceUtils}
	 */
	@Deprecated(forRemoval = true)
    public static void setEasyPlaceLastPickBlockTime()
    {
        easyPlaceLastPickBlockTime = System.nanoTime();
    }
}
