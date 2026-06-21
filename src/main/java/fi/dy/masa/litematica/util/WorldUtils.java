package fi.dy.masa.litematica.util;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.MessageOutputType;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.game.wrap.GameWrap;
import fi.dy.masa.malilib.util.position.IntBoundingBox;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
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
import fi.dy.masa.litematica.util.invoker.IWorldUpdateSuppressor;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class WorldUtils
{
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

    @Deprecated(forRemoval = true)
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
     * @param closest -
     * @param mc -
     * @return true if the correct item was or is in the player's hand after the pick block
     */
    public static boolean doSchematicWorldPickBlock(boolean closest, Minecraft mc)
    {
        BlockPos pos;

        SchematicPickBlockEventHandler.getInstance().resetCancelled();

		if (SchematicPickBlockEventHandler.getInstance().onSchematicPickBlockStart(closest))
		{
			return true;
		}

        if (closest)
        {
            pos = RayTraceUtils.getSchematicWorldTraceIfClosestNoFluids(mc.level, mc.player, getValidBlockRange(mc));
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

            if (traceWrapper == null && EasyPlaceUtils.placementRestrictionInEffect(mc))
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
                if (EasyPlaceUtils.easyPlaceBlockChecksCancel(stateSchematic, stateClient, mc.player, traceVanilla, stack))
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

//                System.out.printf("doEasyPlaceAction - stateSchematic [%s] // sideOrig [%s]\n", stateSchematic.toString(), sideOrig.getName());

                Direction side = EasyPlaceUtils.applyPlacementFacing(stateSchematic, sideOrig, stateClient);

                // Support for special cases
                EasyPlaceUtils.PlacementProtocolData placementData = EasyPlaceUtils.applyPlacementProtocolAll(pos, stateSchematic, hitPos);

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
                    hitPos = EasyPlaceUtils.applyPlacementProtocolV3(pos, stateSchematic, hitPos);
                }
                else if (protocol == EasyPlaceProtocol.V2)
                {
                    // Carpet Accurate Block Placement protocol support, plus slab support
                    hitPos = EasyPlaceUtils.applyCarpetProtocolHitVec(pos, stateSchematic, hitPos);
                }
                else if (protocol == EasyPlaceProtocol.SLAB_ONLY)
                {
                    // Slab support only
                    hitPos = EasyPlaceUtils.applyBlockSlabProtocol(pos, stateSchematic, hitPos);
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
                        side = EasyPlaceUtils.applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                        hitResult = new BlockHitResult(hitPos, side, pos, false);
                        mc.gameMode.useItemOn(mc.player, hand, hitResult);
                    }
                }
            }

            return InteractionResult.SUCCESS;
        }
        else if (traceWrapper.getHitType() == HitType.VANILLA_BLOCK)
        {
            return EasyPlaceUtils.placementRestrictionInEffect(mc) ? InteractionResult.FAIL : InteractionResult.PASS;
        }

        return InteractionResult.PASS;
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
     * @param axis -
     * @param pos1 -
     * @param pos2 -
     * @return -
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
}
