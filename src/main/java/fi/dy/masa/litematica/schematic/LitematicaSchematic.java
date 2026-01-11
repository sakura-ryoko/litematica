package fi.dy.masa.litematica.schematic;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.Schema;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.malilib.util.nbt.NbtView;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.mixin.world.IMixinWorldTickScheduler;
import fi.dy.masa.litematica.network.ServuxLitematicaHandler;
import fi.dy.masa.litematica.network.ServuxLitematicaPacket;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkDirect;
import fi.dy.masa.litematica.scheduler.tasks.TaskProcessChunkBase;
import fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStatePalette;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionFixers;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;
import fi.dy.masa.litematica.schematic.conversion.SchematicConverter;
import fi.dy.masa.litematica.schematic.conversion.SchematicDowngradeConverter;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.schematic.transmit.SchematicBuffer;
import fi.dy.masa.litematica.schematic.transmit.SchematicBufferManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.*;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class LitematicaSchematic
{
    public static final String FILE_EXTENSION = ".litematic";
    public static final int SCHEMATIC_VERSION_1_13_2 = 5;
    public static final int MINECRAFT_DATA_VERSION_1_12   = 1139; // MC 1.12
    public static final int MINECRAFT_DATA_VERSION_1_13_2 = 1631; // MC 1.13.2
    public static final int MINECRAFT_DATA_VERSION_1_20_4 = 3700; // MC 1.20.4
    public static final int MINECRAFT_DATA_VERSION = SharedConstants.getCurrentVersion().dataVersion().version();
    public static final int SCHEMATIC_VERSION = 7;
    // This is basically a "sub-version" for the schematic version,
    // intended to help with possible data fix needs that are discovered.
    public static final int SCHEMATIC_VERSION_SUB = 1; // Bump to one after the sleeping entity position fix

    private final Map<String, LitematicaBlockStateContainer> blockContainers = new HashMap<>();
    private final Map<String, Map<BlockPos, CompoundTag>> tileEntities = new HashMap<>();
    private final Map<String, Map<BlockPos, ScheduledTick<Block>>> pendingBlockTicks = new HashMap<>();
    private final Map<String, Map<BlockPos, ScheduledTick<Fluid>>> pendingFluidTicks = new HashMap<>();
    private final Map<String, List<EntityInfo>> entities = new HashMap<>();
    private final Map<String, BlockPos> subRegionPositions = new HashMap<>();
    private final Map<String, BlockPos> subRegionSizes = new HashMap<>();
    private final SchematicMetadata metadata = new SchematicMetadata();
    private final SchematicConverter converter;
    private int totalBlocksReadFromWorld;
    @Nullable private final Path schematicFile;
    private final FileType schematicType;

    public LitematicaSchematic(Path file, CompoundTag nbt, FileType type)
    {
        this.readFromNBT(nbt);
        this.schematicFile = file;
        this.schematicType = type;
        this.converter = SchematicConverter.createForLitematica();
    }

    private LitematicaSchematic(@Nullable Path file)
    {
        this(file, FileType.LITEMATICA_SCHEMATIC);
    }

    private LitematicaSchematic(@Nullable Path file, FileType schematicType)
    {
        this.schematicFile = file;
        this.schematicType = schematicType;
        this.converter = SchematicConverter.createForLitematica();
    }

    @Nullable
    public Path getFile()
    {
        return this.schematicFile;
    }

    public Vec3i getTotalSize()
    {
        return this.metadata.getEnclosingSize();
    }

    public int getTotalBlocksReadFromWorld()
    {
        return this.totalBlocksReadFromWorld;
    }

    public SchematicMetadata getMetadata()
    {
        return this.metadata;
    }

    public int getSubRegionCount()
    {
        return this.blockContainers.size();
    }

    @Nullable
    public BlockPos getSubRegionPosition(String areaName)
    {
        return this.subRegionPositions.get(areaName);
    }

    public Map<String, BlockPos> getAreaPositions()
    {
        ImmutableMap.Builder<String, BlockPos> builder = ImmutableMap.builder();

        for (String name : this.subRegionPositions.keySet())
        {
            BlockPos pos = this.subRegionPositions.get(name);
            builder.put(name, pos);
        }

        return builder.build();
    }

    public Map<String, BlockPos> getAreaSizes()
    {
        ImmutableMap.Builder<String, BlockPos> builder = ImmutableMap.builder();

        for (String name : this.subRegionSizes.keySet())
        {
            BlockPos pos = this.subRegionSizes.get(name);
            builder.put(name, pos);
        }

        return builder.build();
    }

    @Nullable
    public BlockPos getAreaSize(String regionName)
    {
        return this.subRegionSizes.get(regionName);
    }

    @Nullable
    public Vec3i getAreaSizeAsVec3i(String regionName)
    {
        return this.subRegionSizes.get(regionName);
    }

    public Map<String, Box> getAreas()
    {
        ImmutableMap.Builder<String, Box> builder = ImmutableMap.builder();

        for (String name : this.subRegionPositions.keySet())
        {
            BlockPos pos = this.subRegionPositions.get(name);
            BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(this.subRegionSizes.get(name));
            Box box = new Box(pos, pos.offset(posEndRel), name);
            builder.put(name, box);
        }

        return builder.build();
    }

    /**
     * This version is not Chunk-Specific.  It is only used by {@link WorldUtils} during conversions.
     */
    @Nullable
    public static LitematicaSchematic createFromWorld(Level world, AreaSelection area, SchematicSaveInfo info,
                                                      String author, IStringConsumer feedback)
    {
        List<Box> boxes = PositionUtils.getValidBoxes(area);

        if (boxes.isEmpty())
        {
            feedback.setString(StringUtils.translate("litematica.error.schematic.create.no_selections"));
            return null;
        }

        LitematicaSchematic schematic = new LitematicaSchematic(null);
        long time = System.currentTimeMillis();

        BlockPos origin = area.getEffectiveOrigin();
        schematic.setSubRegionPositions(boxes, origin);
        schematic.setSubRegionSizes(boxes);

        schematic.takeBlocksFromWorld(world, boxes, info);

        if (info.ignoreEntities == false)
        {
            schematic.takeEntitiesFromWorld(world, boxes, origin);
        }

        schematic.metadata.setAuthor(author);
        schematic.metadata.setName(area.getName());
        schematic.metadata.setTimeCreated(time);
        schematic.metadata.setTimeModified(time);
        schematic.metadata.setRegionCount(boxes.size());
        schematic.metadata.setTotalVolume(PositionUtils.getTotalVolume(boxes));
        schematic.metadata.setEnclosingSize(PositionUtils.getEnclosingAreaSize(boxes));
        schematic.metadata.setTotalBlocks(schematic.totalBlocksReadFromWorld);
        schematic.metadata.setSchematicVersion(SCHEMATIC_VERSION);
        schematic.metadata.setMinecraftDataVersion(MINECRAFT_DATA_VERSION);
        schematic.metadata.setFileType(FileType.LITEMATICA_SCHEMATIC);

        // todo testing only
        /*
        for (String entry : schematic.blockContainers.keySet())
        {
            System.out.printf("BlockStateContainer[%s] --> [%s]\n", entry, LitematicaBlockStateContainer.CODEC.encodeStart(NbtOps.INSTANCE, schematic.blockContainers.get(entry)).resultOrPartial().orElse(null));
        }
        System.out.printf("MetaData --> [%s]\n", SchematicMetadata.CODEC.encodeStart(NbtOps.INSTANCE, schematic.metadata).resultOrPartial().orElse(null));
         */

        return schematic;
    }

    /**
     * Creates an empty schematic with all the maps and lists and containers already created.
     * This is intended to be used for the chunk-wise schematic creation.
     * @param area
     * @param author
     * @return
     */
    public static LitematicaSchematic createEmptySchematic(AreaSelection area, String author)
    {
        List<Box> boxes = PositionUtils.getValidBoxes(area);

        if (boxes.isEmpty())
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, StringUtils.translate("litematica.error.schematic.create.no_selections"));
            return null;
        }

        LitematicaSchematic schematic = new LitematicaSchematic(null);
        schematic.setSubRegionPositions(boxes, area.getEffectiveOrigin());
        schematic.setSubRegionSizes(boxes);
        schematic.metadata.setAuthor(author);
        schematic.metadata.setName(area.getName());
        schematic.metadata.setRegionCount(boxes.size());
        schematic.metadata.setTotalVolume(PositionUtils.getTotalVolume(boxes));
        schematic.metadata.setEnclosingSize(PositionUtils.getEnclosingAreaSize(boxes));
        schematic.metadata.setSchematicVersion(SCHEMATIC_VERSION);
        schematic.metadata.setMinecraftDataVersion(MINECRAFT_DATA_VERSION);
        schematic.metadata.setFileType(FileType.LITEMATICA_SCHEMATIC);

        for (Box box : boxes)
        {
            String regionName = box.getName();
            BlockPos size = box.getSize();
            final int sizeX = Math.abs(size.getX());
            final int sizeY = Math.abs(size.getY());
            final int sizeZ = Math.abs(size.getZ());
            LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(sizeX, sizeY, sizeZ);
            schematic.blockContainers.put(regionName, container);
            schematic.tileEntities.put(regionName, new HashMap<>());
            schematic.entities.put(regionName, new ArrayList<>());
            schematic.pendingBlockTicks.put(regionName, new HashMap<>());
            schematic.pendingFluidTicks.put(regionName, new HashMap<>());
        }

        return schematic;
    }

    /**
     * Copy an existing Litematic to a new object to make a "copy" for file export.
     *
     * @param existing (Existing Litematic object)
     * @param newAuthor
     * @return
     */
    public static LitematicaSchematic createEmptySchematicFromExisting(@Nonnull LitematicaSchematic existing, String newAuthor)
    {
        LitematicaSchematic newSchematic = new LitematicaSchematic(null, existing.schematicType);

        if (newAuthor.isEmpty() == false)
        {
            newSchematic.metadata.setAuthor(newAuthor);
        }
        else
        {
            newSchematic.metadata.setAuthor(existing.getMetadata().getAuthor());
        }
        newSchematic.metadata.setName(existing.getMetadata().getName());
        newSchematic.metadata.setDescription(existing.getMetadata().getDescription());
        newSchematic.metadata.setTimeCreated(existing.getMetadata().getTimeCreated());
        newSchematic.metadata.setTimeModifiedToNow();
        newSchematic.metadata.setRegionCount(existing.getMetadata().getRegionCount());
        newSchematic.metadata.setTotalVolume(existing.getMetadata().getTotalVolume());
        newSchematic.metadata.setTotalBlocks(existing.getMetadata().getTotalBlocks());
        newSchematic.metadata.setEnclosingSize(existing.getMetadata().getEnclosingSize());
        newSchematic.metadata.setSchematicVersion(existing.getMetadata().getSchematicVersion());
        newSchematic.metadata.setMinecraftDataVersion(existing.getMetadata().getMinecraftDataVersion());
        newSchematic.metadata.setFileType(existing.getMetadata().getFileType());

        return newSchematic;
    }

    public boolean downgradeV7toV6Schematic(LitematicaSchematic v7Schematic)
    {
        Map<String, Box> areas = v7Schematic.getAreas();

        for (Box box : areas.values())
        {
            String regionName = box.getName();
            BlockPos size = box.getSize();
            final int sizeX = Math.abs(size.getX());
            final int sizeY = Math.abs(size.getY());
            final int sizeZ = Math.abs(size.getZ());
            this.blockContainers.put(regionName, v7Schematic.blockContainers.get(regionName));

            this.tileEntities.put(regionName, this.downgradeTileEntities_to_1_20_4(v7Schematic.tileEntities.get(regionName), MINECRAFT_DATA_VERSION));
            ListTag list = this.writeEntitiesToNBT(v7Schematic.entities.get(regionName));
            list = this.downgradeEntities_to_1_20_4(list, MINECRAFT_DATA_VERSION);
            this.entities.put(regionName, this.readEntitiesFromNBT(list));

            this.pendingBlockTicks.put(regionName, v7Schematic.pendingBlockTicks.get(regionName));
            this.pendingFluidTicks.put(regionName, v7Schematic.pendingFluidTicks.get(regionName));
            this.subRegionPositions.put(regionName, v7Schematic.subRegionPositions.get(regionName));
            this.subRegionSizes.put(regionName, v7Schematic.subRegionSizes.get(regionName));
        }

        return false;
    }

    public void takeEntityDataFromSchematicaSchematic(SchematicaSchematic schematic, String subRegionName)
    {
        this.tileEntities.put(subRegionName, schematic.getTiles());
        this.entities.put(subRegionName, schematic.getEntities());
    }

    /**
     * This version is not Chunk-Specific.  It is only used by {@link WorldUtils} during conversions.
     */
    public boolean placeToWorld(Level world, SchematicPlacement schematicPlacement, boolean notifyNeighbors)
    {
        return this.placeToWorld(world, schematicPlacement, notifyNeighbors, false);
    }

    /**
     * This version is not Chunk-Specific.  It is only used by {@link WorldUtils} during conversions.
     */
    public boolean placeToWorld(Level world, SchematicPlacement schematicPlacement, boolean notifyNeighbors, boolean ignoreEntities)
    {
        WorldUtils.setShouldPreventBlockUpdates(world, true);

        ImmutableMap<String, SubRegionPlacement> relativePlacements = schematicPlacement.getEnabledRelativeSubRegionPlacements();
        BlockPos origin = schematicPlacement.getOrigin();

        for (String regionName : relativePlacements.keySet())
        {
            SubRegionPlacement placement = relativePlacements.get(regionName);

			if (placement == null)
			{
				continue;
			}

            if (placement.isEnabled())
            {
                BlockPos regionPos = placement.getPos();
                BlockPos regionSize = this.subRegionSizes.get(regionName);
                LitematicaBlockStateContainer container = this.blockContainers.get(regionName);
                Map<BlockPos, CompoundTag> tileMap = this.tileEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);
                Map<BlockPos, ScheduledTick<Block>> scheduledBlockTicks = this.pendingBlockTicks.get(regionName);
                Map<BlockPos, ScheduledTick<Fluid>> scheduledFluidTicks = this.pendingFluidTicks.get(regionName);

                if (regionPos != null && regionSize != null && container != null && tileMap != null)
                {
                    this.placeBlocksToWorld(world, origin, regionPos, regionSize, schematicPlacement, placement, container, tileMap, scheduledBlockTicks, scheduledFluidTicks, notifyNeighbors);
                }
                else
                {
                    Litematica.LOGGER.warn("Invalid/missing schematic data in schematic '{}' for sub-region '{}'", this.metadata.getName(), regionName);
                }

                if (ignoreEntities == false && schematicPlacement.ignoreEntities() == false &&
                    placement.ignoreEntities() == false && entityList != null)
                {
                    this.placeEntitiesToWorld(world, origin, regionPos, regionSize, schematicPlacement, placement, entityList);
                }
                else
                {
                    Litematica.LOGGER.error("[Schem] Unable to place entites to world. (Ignore entities on?)");
                }
            }
        }

        WorldUtils.setShouldPreventBlockUpdates(world, false);

        return true;
    }

    /**
     * This version is not Chunk-Specific.  It is only used by {@link WorldUtils} during conversions.
     */
    private boolean placeBlocksToWorld(Level world, BlockPos origin, BlockPos regionPos, BlockPos regionSize,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement,
            LitematicaBlockStateContainer container, Map<BlockPos, CompoundTag> tileMap,
            @Nullable Map<BlockPos, ScheduledTick<Block>> scheduledBlockTicks,
            @Nullable Map<BlockPos, ScheduledTick<Fluid>> scheduledFluidTicks, boolean notifyNeighbors)
    {
        // These are the untransformed relative positions
        BlockPos posEndRelSub = PositionUtils.getRelativeEndPositionFromAreaSize(regionSize);
        BlockPos posEndRel = posEndRelSub.offset(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);

        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        //BlockPos posEndAbs = PositionUtils.getTransformedBlockPos(posEndRelSub, placement.getMirror(), placement.getRotation()).add(regionPosTransformed).add(origin);
        BlockPos regionPosAbs = regionPosTransformed.offset(origin);

        /*
        if (PositionUtils.arePositionsWithinWorld(world, regionPosAbs, posEndAbs) == false)
        {
            return false;
        }
        */

        final int sizeX = Math.abs(regionSize.getX());
        final int sizeY = Math.abs(regionSize.getY());
        final int sizeZ = Math.abs(regionSize.getZ());
        final BlockState barrier = Blocks.BARRIER.defaultBlockState();
        final boolean ignoreInventories = Configs.Generic.PASTE_IGNORE_INVENTORY.getBooleanValue();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();
        ReplaceBehavior replace = (ReplaceBehavior) Configs.Generic.PASTE_REPLACE_BEHAVIOR.getOptionListValue();

        final Rotation rotationCombined = schematicPlacement.getRotation().getRotated(placement.getRotation());
        final Mirror mirrorMain = schematicPlacement.getMirror();
        Mirror mirrorSub = placement.getMirror();

        if (mirrorSub != Mirror.NONE &&
            (schematicPlacement.getRotation() == Rotation.CLOCKWISE_90 ||
             schematicPlacement.getRotation() == Rotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
        }

        int bottomY = world.getMinY();
        int topY = world.getMaxY() + 1;
        int tmp = posMinRel.getY() - regionPos.getY() + regionPosTransformed.getY() + origin.getY();
        int startY = 0;
        int endY = sizeY;

        if (tmp < bottomY)
        {
            startY += (bottomY - tmp);
        }

        tmp = posMinRel.getY() - regionPos.getY() + regionPosTransformed.getY() + origin.getY() + (endY - 1);

        if (tmp > topY)
        {
            endY -= (tmp - topY);
        }

        for (int y = startY; y < endY; ++y)
        {
            for (int z = 0; z < sizeZ; ++z)
            {
                for (int x = 0; x < sizeX; ++x)
                {
                    BlockState state = container.get(x, y, z);

                    if (state.getBlock() == Blocks.STRUCTURE_VOID)
                    {
                        continue;
                    }

                    posMutable.set(x, y, z);
                    CompoundTag teNBT = tileMap.get(posMutable);

                    posMutable.set( posMinRel.getX() + x - regionPos.getX(),
                                    posMinRel.getY() + y - regionPos.getY(),
                                    posMinRel.getZ() + z - regionPos.getZ());

                    BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement);
                    pos = pos.offset(regionPosTransformed).offset(origin);

                    BlockState stateOld = world.getBlockState(pos);

                    if ((replace == ReplaceBehavior.NONE && stateOld.isAir() == false) ||
                        (replace == ReplaceBehavior.WITH_NON_AIR && state.isAir()))
                    {
                        continue;
                    }

                    if (mirrorMain != Mirror.NONE) { state = state.mirror(mirrorMain); }
                    if (mirrorSub != Mirror.NONE)  { state = state.mirror(mirrorSub); }
                    if (rotationCombined != Rotation.NONE) { state = state.rotate(rotationCombined); }

                    if (stateOld == state && state.hasBlockEntity() == false)
                    {
                        continue;
                    }

                    BlockEntity teOld = world.getBlockEntity(pos);

                    if (teOld != null)
                    {
                        if (teOld instanceof Container)
                        {
                            ((Container) teOld).clearContent();
                        }

                        world.setBlock(pos, barrier, 0x14);
                    }

                    if (world.setBlock(pos, state, 0x12) && teNBT != null)
                    {
                        BlockEntity te = world.getBlockEntity(pos);

                        if (te != null)
                        {
                            teNBT = teNBT.copy();
                            teNBT.putInt("x", pos.getX());
                            teNBT.putInt("y", pos.getY());
                            teNBT.putInt("z", pos.getZ());

                            if (ignoreInventories)
                            {
                                teNBT.remove("Items");
                            }

                            try
                            {
                                NbtView view = NbtView.getReader(teNBT, world.registryAccess());
                                te.loadWithComponents(view.getReader());

                                if (ignoreInventories && te instanceof Container)
                                {
                                    ((Container) te).clearContent();
                                }
                            }
                            catch (Exception e)
                            {
                                Litematica.LOGGER.warn("Failed to load TileEntity data for {} @ {}", state, pos);
                            }
                        }
                    }
                }
            }
        }

        /*
        if (notifyNeighbors)
        {
            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        posMutable.set( posMinRel.getX() + x - regionPos.getX(),
                                        posMinRel.getY() + y - regionPos.getY(),
                                        posMinRel.getZ() + z - regionPos.getZ());
                        BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement).add(origin);
                        world.updateNeighbors(pos, world.getBlockState(pos).getBlock());
                    }
                }
            }
        }

        if (world instanceof ServerWorld serverWorld)
        {
            if (scheduledBlockTicks != null && scheduledBlockTicks.isEmpty() == false)
            {
                for (Map.Entry<BlockPos, OrderedTick<Block>> entry : scheduledBlockTicks.entrySet())
                {
                    BlockPos pos = entry.getKey().add(regionPosAbs);
                    OrderedTick<Block> tick = entry.getValue();
                    serverWorld.getBlockTickScheduler().scheduleTick(new OrderedTick<>(tick.type(), pos, (int) tick.triggerTick(), tick.priority(), tick.subTickOrder()));
                }
            }

            if (scheduledFluidTicks != null && scheduledFluidTicks.isEmpty() == false)
            {
                for (Map.Entry<BlockPos, OrderedTick<Fluid>> entry : scheduledFluidTicks.entrySet())
                {
                    BlockPos pos = entry.getKey().add(regionPosAbs);
                    BlockState state = world.getBlockState(pos);

                    if (state.getFluidState().isEmpty() == false)
                    {
                        OrderedTick<Fluid> tick = entry.getValue();
                        serverWorld.getFluidTickScheduler().scheduleTick(new OrderedTick<>(tick.type(), pos, (int) tick.triggerTick(), tick.priority(), tick.subTickOrder()));
                    }
                }
            }
        }
        */

        return true;
    }

    /**
     * This version is not Chunk-Specific.  It is only used by {@link WorldUtils} during conversions.
     */
    private void placeEntitiesToWorld(Level world, BlockPos origin, BlockPos regionPos, BlockPos regionSize, SchematicPlacement schematicPlacement, SubRegionPlacement placement, List<EntityInfo> entityList)
    {
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        final int offX = regionPosRelTransformed.getX() + origin.getX();
        final int offY = regionPosRelTransformed.getY() + origin.getY();
        final int offZ = regionPosRelTransformed.getZ() + origin.getZ();

        final Rotation rotationCombined = schematicPlacement.getRotation().getRotated(placement.getRotation());
        final Mirror mirrorMain = schematicPlacement.getMirror();
        Mirror mirrorSub = placement.getMirror();

        if (mirrorSub != Mirror.NONE &&
            (schematicPlacement.getRotation() == Rotation.CLOCKWISE_90 ||
             schematicPlacement.getRotation() == Rotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
        }

        Litematica.LOGGER.warn("[Schem] placeEntitiesToWorld: entityList size [{}]", entityList.size());

        for (EntityInfo info : entityList)
        {
            Entity entity = EntityUtils.createEntityAndPassengersFromNBT(info.nbt, world);

            if (entity != null)
            {
                Vec3 pos = info.posVec;
                pos = PositionUtils.getTransformedPosition(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
                pos = PositionUtils.getTransformedPosition(pos, placement.getMirror(), placement.getRotation());
                double x = pos.x + offX;
                double y = pos.y + offY;
                double z = pos.z + offZ;

                Litematica.LOGGER.warn("[Schem] placeEntitiesToWorld: entity [{}]", entity.getType().getDescription().getString());

                SchematicPlacingUtils.rotateEntity(entity, x, y, z, rotationCombined, mirrorMain, mirrorSub);
                EntityUtils.spawnEntityAndPassengersInWorld(entity, world);
            }
            else
            {
                Litematica.LOGGER.error("[Schem] placeEntitiesToWorld: entity == null!");
            }
        }
    }

    /**
     * This version is not Chunk-Specific.  It is only used by {@link WorldUtils} during conversions.
     */
    private void takeEntitiesFromWorld(Level world, List<Box> boxes, BlockPos origin)
    {
        for (Box box : boxes)
        {
            AABB bb = PositionUtils.createEnclosingAABB(box.getPos1(), box.getPos2());
            BlockPos regionPosAbs = box.getPos1();
            List<EntityInfo> list = new ArrayList<>();
            List<Entity> entities = world.getEntities((Entity) null, bb, EntityUtils.NOT_PLAYER);

            for (Entity entity : entities)
            {
                NbtView view = NbtView.getWriter(world.registryAccess());

                entity.saveWithoutId(view.getWriter());
                CompoundTag tag = view.readNbt();
                Identifier id = EntityType.getKey(entity.getType());

                if (tag != null && id != null)
                {
                    Vec3 posVec = new Vec3(entity.getX() - regionPosAbs.getX(), entity.getY() - regionPosAbs.getY(), entity.getZ() - regionPosAbs.getZ());

                    tag.putString("id", id.toString());
//                    NbtUtils.writeEntityPositionToTag(posVec, tag);
                    NbtUtils.putVec3dCodec(tag, posVec, "Pos");
                    list.add(new EntityInfo(posVec, tag));
                }
            }

            this.entities.put(box.getName(), list);
        }
    }

    /**
     * This is used by {@link TaskSaveSchematic}
     */
    public void takeEntitiesFromWorldWithinChunk(Level world, int chunkX, int chunkZ,
            ImmutableMap<String, IntBoundingBox> volumes, ImmutableMap<String, Box> boxes,
            Set<UUID> existingEntities, BlockPos origin)
    {
        for (Map.Entry<String, IntBoundingBox> entry : volumes.entrySet())
        {
            String regionName = entry.getKey();
            List<EntityInfo> list = this.entities.get(regionName);
            Box box = boxes.get(regionName);

            if (box == null || list == null)
            {
                continue;
            }

            AABB bb = PositionUtils.createAABBFrom(entry.getValue());
            List<Entity> entities = world.getEntities((Entity) null, bb, EntityUtils.NOT_PLAYER);
            BlockPos regionPosAbs = box.getPos1();

            for (Entity entity : entities)
            {
                UUID uuid = entity.getUUID();
                /*
                if (entity.posX >= bb.minX && entity.posX < bb.maxX &&
                    entity.posY >= bb.minY && entity.posY < bb.maxY &&
                    entity.posZ >= bb.minZ && entity.posZ < bb.maxZ)
                */
                if (existingEntities.contains(uuid) == false)
                {
                    CompoundTag tag = new CompoundTag();

                    if (EntitiesDataStorage.getInstance().hasServuxServer())
                    {
                        CompoundTag serverTags = EntitiesDataStorage.getInstance().getFromEntityCacheNbt(entity.getId());

                        if (serverTags != null && !serverTags.isEmpty())
                        {
                            tag.merge(serverTags);
                        }
                    }
                    else
                    {
                        NbtView view = NbtView.getWriter(world.registryAccess());

                        // Checks for Vehicle
                        if (entity.save(view.getWriter()))
                        {
                            tag = view.readNbt() != null ? view.readNbt() : new CompoundTag();
                            Identifier id = EntityType.getKey(entity.getType());
                            tag.putString("id", id.toString());
                            tag.putInt("LastEntityID", entity.getId());
                        }
                    }

//                    boolean hasVehicle = entity.hasVehicle();
//                    boolean fixPassengers = tag.contains("Passengers");
//                    Litematica.LOGGER.error("takeEntitiesFromWorldWithinChunk(): UUID: [{}], HasVehicle({}), HasPassengers({}), NBT: [{}]", uuid.toString(), hasVehicle, fixPassengers, tag.toString());

//                    if (fixPassengers)
//                    {
//                        tag.put("Passengers", EntityUtils.updatePassengersToRelativeRegionPos(tag.getListOrEmpty("Passengers"), regionPosAbs));
//                    }

                    if (!tag.isEmpty())
                    {
                        Vec3 posVec = new Vec3(entity.getX() - regionPosAbs.getX(), entity.getY() - regionPosAbs.getY(), entity.getZ() - regionPosAbs.getZ());

                        // Annoying special case for any hanging/decoration entities, to avoid the console
                        // warning about invalid hanging position when loading the entity from NBT
                        if (entity instanceof HangingEntity decorationEntity)
                        {
                            BlockPos p = decorationEntity.blockPosition();
                            tag.putInt("TileX", p.getX() - regionPosAbs.getX());
                            tag.putInt("TileY", p.getY() - regionPosAbs.getY());
                            tag.putInt("TileZ", p.getZ() - regionPosAbs.getZ());
                        }

                        if (entity instanceof BlockAttachedEntity bae)
                        {
                            BlockPos p = bae.getPos();
                            BlockPos pAdj = new BlockPos(p.getX() - regionPosAbs.getX(), p.getY() - regionPosAbs.getY(), p.getZ() - regionPosAbs.getZ());

                            tag.store("block_pos", BlockPos.CODEC, pAdj);
                        }

//                        NbtUtils.writeEntityPositionToTag(posVec, tag);
                        NbtUtils.putVec3dCodec(tag, posVec, "Pos");
                        list.add(new EntityInfo(posVec, tag));
                        existingEntities.add(uuid);
                    }
                }
            }
        }
    }

    /**
     * This version is not Chunk-Specific.  It is only used by {@link WorldUtils} during conversions.
     */
    @SuppressWarnings("unchecked")
    private void takeBlocksFromWorld(Level world, List<Box> boxes, SchematicSaveInfo info)
    {
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);

        for (Box box : boxes)
        {
            BlockPos size = box.getSize();
            final int sizeX = Math.abs(size.getX());
            final int sizeY = Math.abs(size.getY());
            final int sizeZ = Math.abs(size.getZ());
            LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(sizeX, sizeY, sizeZ);
            Map<BlockPos, CompoundTag> tileEntityMap = new HashMap<>();
            Map<BlockPos, ScheduledTick<Block>> blockTickMap = new HashMap<>();
            Map<BlockPos, ScheduledTick<Fluid>> fluidTickMap = new HashMap<>();

            // We want to loop nice & easy from 0 to n here, but the per-sub-region pos1 can be at
            // any corner of the area. Thus we need to offset from the total area origin
            // to the minimum/negative corner (ie. 0,0 in the loop) corner here.
            final BlockPos minCorner = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            final int startX = minCorner.getX();
            final int startY = minCorner.getY();
            final int startZ = minCorner.getZ();
            final boolean visibleOnly = info.visibleOnly;
            final boolean includeSupport = info.includeSupportBlocks;

            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        posMutable.set(x + startX, y + startY, z + startZ);

                        if (visibleOnly &&
                            isExposed(world, posMutable) == false &&
                            (includeSupport == false || isSupport(world, posMutable) == false))
                        {
                            continue;
                        }

                        BlockState state = world.getBlockState(posMutable);
                        container.set(x, y, z, state);

                        if (state.isAir() == false)
                        {
                            this.totalBlocksReadFromWorld++;
                        }

                        if (state.hasBlockEntity())
                        {
                            BlockEntity te = world.getBlockEntity(posMutable);

                            if (te != null)
                            {
                                // TODO Add a TileEntity NBT cache from the Chunk packets, to get the original synced data (too)
                                BlockPos pos = new BlockPos(x, y, z);
                                CompoundTag tag = te.saveWithFullMetadata(world.registryAccess());
                                NbtUtils.writeBlockPosToTag(pos, tag);
                                tileEntityMap.put(pos, tag);
                            }
                        }
                    }
                }
            }

            if (world instanceof ServerLevel serverWorld)
            {
                IntBoundingBox tickBox = IntBoundingBox.createProper(
                        startX,         startY,         startZ,
                        startX + sizeX, startY + sizeY, startZ + sizeZ);
                long currentTick = world.getGameTime();

                this.getTicksFromScheduler(((IMixinWorldTickScheduler<Block>) serverWorld.getBlockTicks()).litematica_getChunkTickSchedulers(),
                                           blockTickMap, tickBox, minCorner, currentTick);

                this.getTicksFromScheduler(((IMixinWorldTickScheduler<Fluid>) serverWorld.getFluidTicks()).litematica_getChunkTickSchedulers(),
                                           fluidTickMap, tickBox, minCorner, currentTick);
            }

            this.blockContainers.put(box.getName(), container);
            this.tileEntities.put(box.getName(), tileEntityMap);
            this.pendingBlockTicks.put(box.getName(), blockTickMap);
            this.pendingFluidTicks.put(box.getName(), fluidTickMap);
        }
    }

    private <T> void getTicksFromScheduler(Long2ObjectMap<LevelChunkTicks<T>> chunkTickSchedulers,
                                           Map<BlockPos, ScheduledTick<T>> outputMap,
                                           IntBoundingBox box,
                                           BlockPos minCorner,
                                           final long currentTick)
    {
        int minCX = SectionPos.blockToSectionCoord(box.minX());
        int minCZ = SectionPos.blockToSectionCoord(box.minZ());
        int maxCX = SectionPos.blockToSectionCoord(box.maxX());
        int maxCZ = SectionPos.blockToSectionCoord(box.maxZ());

        for (int cx = minCX; cx <= maxCX; ++cx)
        {
            for (int cz = minCZ; cz <= maxCZ; ++cz)
            {
                long cp = ChunkPos.asLong(cx, cz);

                LevelChunkTicks<T> chunkTickScheduler = chunkTickSchedulers.get(cp);

                if (chunkTickScheduler != null)
                {
                    chunkTickScheduler.getAll()
                            .filter((t) -> box.containsPos(t.pos()))
                            .forEach((t) -> this.addRelativeTickToMap(outputMap, t, minCorner, currentTick));
                }
            }
        }
    }

    private <T> void addRelativeTickToMap(Map<BlockPos, ScheduledTick<T>> outputMap, ScheduledTick<T> tick,
                                          BlockPos minCorner, long currentTick)
    {
        BlockPos pos = tick.pos();
        BlockPos relativePos = new BlockPos(pos.getX() - minCorner.getX(),
                                            pos.getY() - minCorner.getY(),
                                            pos.getZ() - minCorner.getZ());

        ScheduledTick<T> newTick = new ScheduledTick<>(tick.type(), relativePos, tick.triggerTick() - currentTick,
                                                   tick.priority(), tick.subTickOrder());

        outputMap.put(relativePos, newTick);
    }

    public static boolean isExposed(Level world, BlockPos pos)
    {
        for (Direction dir : fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS)
        {
            BlockPos posAdj = pos.relative(dir);
            BlockState stateAdj = world.getBlockState(posAdj);

            if (stateAdj.canOcclude() == false ||
                stateAdj.isFaceSturdy(world, posAdj, dir.getOpposite()) == false)
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isGravityBlock(BlockState state)
    {
        return state.is(BlockTags.SAND) ||
               state.is(BlockTags.CONCRETE_POWDER) ||
               state.getBlock() == Blocks.GRAVEL;
    }

    public static boolean isGravityBlock(Level world, BlockPos pos)
    {
        return isGravityBlock(world.getBlockState(pos));
    }

    public static boolean supportsExposedBlocks(Level world, BlockPos pos)
    {
        BlockPos posUp = pos.relative(Direction.UP);
        BlockState stateUp = world.getBlockState(posUp);

        while (true)
        {
            if (needsSupportNonGravity(stateUp))
            {
                return true;
            }
            else if (isGravityBlock(stateUp))
            {
                if (isExposed(world, posUp))
                {
                    return true;
                }
            }
            else
            {
                break;
            }

            posUp = posUp.relative(Direction.UP);

            if (posUp.getY() >= world.getMaxY() + 1)
            {
                break;
            }

            stateUp = world.getBlockState(posUp);
        }

        return false;
    }

    public static boolean needsSupportNonGravity(BlockState state)
    {
        Block block = state.getBlock();

        return block == Blocks.REPEATER ||
               block == Blocks.COMPARATOR ||
               block == Blocks.SNOW ||
               block instanceof CarpetBlock; // Moss Carpet is not in the WOOL_CARPETS tag
    }

    public static boolean isSupport(Level world, BlockPos pos)
    {
        // This only needs to return true for blocks that are needed support for another block,
        // and that other block would possibly block visibility to this block, i.e. its side
        // facing this block position is a full opaque square.
        // Apparently there is no method that indicates blocks that need support...
        // so hard coding a bunch of stuff here it is then :<
        BlockPos posUp = pos.relative(Direction.UP);
        BlockState stateUp = world.getBlockState(posUp);

        if (needsSupportNonGravity(stateUp))
        {
            return true;
        }

        return isGravityBlock(stateUp) &&
               (isExposed(world, posUp) || supportsExposedBlocks(world, posUp));
    }

    /**
     * This is used by both {@link TaskProcessChunkBase} and {@link TaskPasteSchematicPerChunkDirect}
     */
    @SuppressWarnings("unchecked")
    public void takeBlocksFromWorldWithinChunk(Level world, ImmutableMap<String, IntBoundingBox> volumes,
                                               ImmutableMap<String, Box> boxes, SchematicSaveInfo info)
    {
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);

        for (Map.Entry<String, IntBoundingBox> volumeEntry : volumes.entrySet())
        {
            String regionName = volumeEntry.getKey();
            IntBoundingBox bb = volumeEntry.getValue();
            Box box = boxes.get(regionName);

            if (box == null)
            {
                Litematica.LOGGER.error("null Box for sub-region '{}' while trying to save chunk-wise schematic", regionName);
                continue;
            }

            LitematicaBlockStateContainer container = this.blockContainers.get(regionName);
            Map<BlockPos, CompoundTag> tileEntityMap = this.tileEntities.get(regionName);
            Map<BlockPos, ScheduledTick<Block>> blockTickMap = this.pendingBlockTicks.get(regionName);
            Map<BlockPos, ScheduledTick<Fluid>> fluidTickMap = this.pendingFluidTicks.get(regionName);

            if (container == null || tileEntityMap == null || blockTickMap == null || fluidTickMap == null)
            {
                Litematica.LOGGER.error("null map(s) for sub-region '{}' while trying to save chunk-wise schematic", regionName);
                continue;
            }

            // We want to loop nice & easy from 0 to n here, but the per-sub-region pos1 can be at
            // any corner of the area. Thus we need to offset from the total area origin
            // to the minimum/negative corner (ie. 0,0 in the loop) corner here.
            final BlockPos minCorner = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            final int offsetX = minCorner.getX();
            final int offsetY = minCorner.getY();
            final int offsetZ = minCorner.getZ();
            // Relative coordinates within the sub-region container:
            final int startX = bb.minX() - minCorner.getX();
            final int startY = bb.minY() - minCorner.getY();
            final int startZ = bb.minZ() - minCorner.getZ();
            final int endX = startX + (bb.maxX() - bb.minX());
            final int endY = startY + (bb.maxY() - bb.minY());
            final int endZ = startZ + (bb.maxZ() - bb.minZ());
            final boolean visibleOnly = info.visibleOnly;
            final boolean includeSupport = info.includeSupportBlocks;

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.set(x + offsetX, y + offsetY, z + offsetZ);

                        if (visibleOnly &&
                            isExposed(world, posMutable) == false &&
                            (includeSupport == false || isSupport(world, posMutable) == false))
                        {
                            continue;
                        }

                        BlockState state = world.getBlockState(posMutable);
                        container.set(x, y, z, state);

                        if (state.isAir() == false)
                        {
                            this.totalBlocksReadFromWorld++;
                        }

                        if (state.hasBlockEntity())
                        {
                            BlockEntity te = world.getBlockEntity(posMutable);

                            if (te != null)
                            {
                                BlockPos pos = new BlockPos(x, y, z);
                                CompoundTag tag = te.saveWithFullMetadata(world.registryAccess());
                                NbtUtils.writeBlockPosToTag(pos, tag);
                                tileEntityMap.put(pos, tag);
                            }
                            else if (EntitiesDataStorage.getInstance().hasServuxServer())
                            {
                                CompoundTag tag = EntitiesDataStorage.getInstance().getFromBlockEntityCacheNbt(posMutable);

                                if (tag != null && tag.isEmpty() == false)
                                {
                                    BlockPos pos = new BlockPos(x, y, z);
                                    NbtUtils.writeBlockPosToTag(pos, tag);
                                    tileEntityMap.put(pos, tag);
                                }
                            }
                        }
                    }
                }
            }

            if (world instanceof ServerLevel serverWorld)
            {
                IntBoundingBox tickBox = IntBoundingBox.createProper(
                        offsetX + startX  , offsetY + startY  , offsetZ + startZ  ,
                        offsetX + endX + 1, offsetY + endY + 1, offsetZ + endZ + 1);

                long currentTick = world.getGameTime();

                this.getTicksFromScheduler(((IMixinWorldTickScheduler<Block>) serverWorld.getBlockTicks()).litematica_getChunkTickSchedulers(),
                                           blockTickMap, tickBox, minCorner, currentTick);

                this.getTicksFromScheduler(((IMixinWorldTickScheduler<Fluid>) serverWorld.getFluidTicks()).litematica_getChunkTickSchedulers(),
                                           fluidTickMap, tickBox, minCorner, currentTick);
            }
        }
    }

    private void setSubRegionPositions(List<Box> boxes, BlockPos areaOrigin)
    {
        for (Box box : boxes)
        {
            this.subRegionPositions.put(box.getName(), box.getPos1().subtract(areaOrigin));
        }
    }

    private void setSubRegionSizes(List<Box> boxes)
    {
        for (Box box : boxes)
        {
            this.subRegionSizes.put(box.getName(), box.getSize());
        }
    }

    @Nullable
    public LitematicaBlockStateContainer getSubRegionContainer(String regionName)
    {
        return this.blockContainers.get(regionName);
    }

    @Nullable
    public Map<BlockPos, CompoundTag> getBlockEntityMapForRegion(String regionName)
    {
        return this.tileEntities.get(regionName);
    }

    @Nullable
    public List<EntityInfo> getEntityListForRegion(String regionName)
    {
        return this.entities.get(regionName);
    }

    @Nullable
    public Map<BlockPos, ScheduledTick<Block>> getScheduledBlockTicksForRegion(String regionName)
    {
        return this.pendingBlockTicks.get(regionName);
    }

    @Nullable
    public Map<BlockPos, ScheduledTick<Fluid>> getScheduledFluidTicksForRegion(String regionName)
    {
        return this.pendingFluidTicks.get(regionName);
    }

    public CompoundTag writeToNBT()
    {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("MinecraftDataVersion", MINECRAFT_DATA_VERSION);
        nbt.putInt("Version", SCHEMATIC_VERSION);
        nbt.putInt("SubVersion", SCHEMATIC_VERSION_SUB);
        nbt.put("Metadata", this.metadata.writeToNBT());
        nbt.put("Regions", this.writeSubRegionsToNBT());

        return nbt;
    }

    public CompoundTag writeToNBT_v6()
    {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("MinecraftDataVersion", MINECRAFT_DATA_VERSION_1_20_4);
        nbt.putInt("Version", 6);
        nbt.putInt("SubVersion", SCHEMATIC_VERSION_SUB);
        nbt.put("Metadata", this.metadata.writeToNBT());
        nbt.put("Regions", this.writeSubRegionsToNBT());

        return nbt;
    }

    private CompoundTag writeSubRegionsToNBT()
    {
        CompoundTag wrapper = new CompoundTag();

        if (this.blockContainers.isEmpty() == false)
        {
            for (String regionName : this.blockContainers.keySet())
            {
                LitematicaBlockStateContainer blockContainer = this.blockContainers.get(regionName);
                Map<BlockPos, CompoundTag> tileMap = this.tileEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);
                Map<BlockPos, ScheduledTick<Block>> pendingBlockTicks = this.pendingBlockTicks.get(regionName);
                Map<BlockPos, ScheduledTick<Fluid>> pendingFluidTicks = this.pendingFluidTicks.get(regionName);

                CompoundTag tag = new CompoundTag();

                tag.put("BlockStatePalette", blockContainer.getPalette().writeToNBT());
                tag.put("BlockStates", new LongArrayTag(blockContainer.getBackingLongArray()));
                tag.put("TileEntities", this.writeTileEntitiesToNBT(tileMap));

                if (pendingBlockTicks != null)
                {
                    tag.put("PendingBlockTicks", this.writePendingTicksToNBT(pendingBlockTicks, BuiltInRegistries.BLOCK, "Block"));
                }

                if (pendingFluidTicks != null)
                {
                    tag.put("PendingFluidTicks", this.writePendingTicksToNBT(pendingFluidTicks, BuiltInRegistries.FLUID, "Fluid"));
                }

                // The entity list will not exist, if takeEntities is false when creating the schematic
                if (entityList != null)
                {
                    tag.put("Entities", this.writeEntitiesToNBT(entityList));
                }

                BlockPos pos = this.subRegionPositions.get(regionName);
                tag.put("Position", NbtUtils.createBlockPosTag(pos));

                pos = this.subRegionSizes.get(regionName);
                tag.put("Size", NbtUtils.createBlockPosTag(pos));

                wrapper.put(regionName, tag);
            }
        }

        return wrapper;
    }

    private ListTag writeEntitiesToNBT(List<EntityInfo> entityList)
    {
        ListTag tagList = new ListTag();

        if (entityList.isEmpty() == false)
        {
            for (EntityInfo info : entityList)
            {
                tagList.add(info.nbt);
            }
        }

        return tagList;
    }

    private <T> ListTag writePendingTicksToNBT(Map<BlockPos, ScheduledTick<T>> tickMap, Registry<T> registry, String tagName)
    {
        ListTag tagList = new ListTag();

        if (tickMap.isEmpty() == false)
        {
            for (ScheduledTick<T> entry : tickMap.values())
            {
                T target = entry.type();
                Identifier id = registry.getKey(target);

                if (id != null)
                {
                    CompoundTag tag = new CompoundTag();

                    tag.putString(tagName, id.toString());
                    tag.putInt("Priority", entry.priority().getValue());
                    tag.putLong("SubTick", entry.subTickOrder());
                    tag.putInt("Time", (int) entry.triggerTick());
                    tag.putInt("x", entry.pos().getX());
                    tag.putInt("y", entry.pos().getY());
                    tag.putInt("z", entry.pos().getZ());

                    tagList.add(tag);
                }
            }
        }

        return tagList;
    }

    private ListTag writeTileEntitiesToNBT(Map<BlockPos, CompoundTag> tileMap)
    {
        ListTag tagList = new ListTag();

        if (tileMap.isEmpty() == false)
        {
            tagList.addAll(tileMap.values());
        }

        return tagList;
    }

    public void sendTransmitFile(CompoundTag nbtIn, final long sessionKey, boolean printMessage)
    {
        if (EntitiesDataStorage.getInstance().hasServuxServer() == false)
        {
            if (printMessage)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.schematic_transmit_not_available");
            }

            Litematica.LOGGER.error("transmitFileToServux: Cannot transmit a Litematic without having Servux present.");
            return;
        }

        if (printMessage)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.schematic_transmit_start");
        }

        Path file = this.getFile();
        CompoundTag output = new CompoundTag();

        output.putString("Task", "Litematic-TransmitStart");
        output.putString("FileName", file.getFileName().toString());
        output.store("FileType", FileType.CODEC, this.schematicType);
        output.putLong("SliceKey", sessionKey);

        if (!nbtIn.isEmpty())
        {
            output.put("PlacementData", nbtIn);
        }

        ServuxLitematicaHandler.getInstance().encodeClientData(ServuxLitematicaPacket.ResponseC2SStart(output));

        // File Stream
        final int bufferSize = SchematicBuffer.BUFFER_SIZE;
        byte[] buffer = new byte[bufferSize];
        int totalBytes = 0;
        int totalSlices = 0;
        output.putLong("SliceKey", sessionKey);

        try (InputStream is = Files.newInputStream(file))
        {
            int bytesRead = 0;
            output.putString("Task", "Litematic-TransmitData");

            while (bytesRead != -1)
            {
                output.remove("Slice");
                output.remove("Size");
                output.remove("Data");

                bytesRead = is.read(buffer, 0, bufferSize);
                output.putInt("Slice", totalSlices);
                output.putInt("Size", bytesRead);
                output.putByteArray("Data", buffer);
                ServuxLitematicaHandler.getInstance().encodeClientData(ServuxLitematicaPacket.ResponseC2SStart(output));
                totalBytes += bytesRead;
                totalSlices++;
            }
        }
        catch (Exception err)
        {
            if (printMessage)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.schematic_transmit_fail");
            }

            output = new CompoundTag();
            output.putLong("SliceKey", sessionKey);
            output.putString("Task", "Litematic-TransmitCancel");
            ServuxLitematicaHandler.getInstance().encodeClientData(ServuxLitematicaPacket.ResponseC2SStart(output));
            Litematica.LOGGER.error("sliceForServux: Exception reading file; {}", err.getLocalizedMessage());
            return;
        }

        // End Slice
        output.remove("Slice");
        output.remove("Size");
        output.remove("Data");

        output.putInt("TotalSize", totalBytes);
        output.putInt("TotalSlices", totalSlices);
        output.putString("Task", "Litematic-TransmitEnd");
        ServuxLitematicaHandler.getInstance().encodeClientData(ServuxLitematicaPacket.ResponseC2SStart(output));
        Litematica.debugLog("receiveFileTransmit: Treansmitted file '{}', [tS: {}, tB: {}]", file.toAbsolutePath().toString(), totalSlices, totalBytes);

        if (printMessage)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.INFO, "litematica.message.schematic_transmit_complete", totalBytes);
        }
    }

    public static @Nullable Pair<LitematicaSchematic, CompoundTag> receiveFileTransmit(CompoundTag nbt)
    {
        SchematicBufferManager manager = DataManager.getSchematicBufferManager();
        String task = nbt.getStringOr("Task", "");
        final long key = nbt.getLongOr("SliceKey", -1L);

        if (task.isEmpty() || key == -1L)
        {
            Litematica.LOGGER.error("receiveFileTransmit: Invalid sessionKey or Task received.");
            return null;
        }

        switch (task)
        {
            case "Litematic-TransmitStart" ->
            {
                FileType type = nbt.read("FileType", FileType.CODEC).orElse(FileType.LITEMATICA_SCHEMATIC);
                String name = nbt.getStringOr("FileName", "default_file");

                manager.createBuffer(name, type, key, nbt.getCompoundOrEmpty("PlacementData"));
            }
            case "Litematic-TransmitData" ->
            {
                final int slice = nbt.getIntOr("Slice", -1);
                final int size = nbt.getIntOr("Size", -1);
                final byte[] data = nbt.getByteArray("Data").orElse(new byte[0]);

                if (slice < 0 || size < 0 || data.length == 0)
                {
                    Litematica.LOGGER.error("receiveFileTransmit: Invalid Slice Data received for session key [{}]", key);
                    return null;
                }

                manager.receiveSlice(key, slice, data, size);
            }
            case "Litematic-TransmitCancel" ->
            {
                Litematica.LOGGER.warn("receiveFileTransmit: Cancel received for session key [{}]", key);
                manager.cancelBuffer(key);
            }
            case "Litematic-TransmitEnd" ->
            {
                final int totalSize = nbt.getIntOr("TotalSize", -1);
                final int totalSlices = nbt.getIntOr("TotalSlices", -1);
                Path dir = DataManager.getSchematicTransmitDirectory();
                CompoundTag optional = manager.getOptionalNbt(key);
                LitematicaSchematic schematic = manager.finishBuffer(key, dir);

                if (schematic == null)
                {
                    Litematica.LOGGER.warn("receiveFileTransmit: Failed to create Schematic for finishing session key [{}]", key);
                    return null;
                }

                // Successful transmission
                Litematica.debugLog("receiveFileTransmit: Received file '{}', [tS: {}, tB: {}]", schematic.getFile().toAbsolutePath().toString(), totalSlices, totalSize);
                return Pair.of(schematic, optional);
            }
            default ->
            {
                Litematica.LOGGER.error("receiveFileTransmit: Invalid sessionKey or Task received.");
            }
        }

        return null;
    }

    private boolean readFromNBT(CompoundTag nbt)
    {
        this.blockContainers.clear();
        this.tileEntities.clear();
        this.entities.clear();
        this.pendingBlockTicks.clear();
        this.subRegionPositions.clear();
        this.subRegionSizes.clear();
        //this.metadata.clearModifiedSinceSaved();

        if (nbt.contains("Version"))
        {
            final int version = nbt.getIntOr("Version", -1);
            final int minecraftDataVersion = nbt.contains("MinecraftDataVersion") ? nbt.getIntOr("MinecraftDataVersion", Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue()) : Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();

            if (version >= 1 && version <= SCHEMATIC_VERSION)
            {
                if (minecraftDataVersion - this.MINECRAFT_DATA_VERSION > 100)
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.error.schematic_load.newer_minecraft_version", minecraftDataVersion, this.MINECRAFT_DATA_VERSION);
                }

                this.metadata.readFromNBT(nbt.getCompoundOrEmpty("Metadata"));
                this.metadata.setSchematicVersion(version);
                this.metadata.setMinecraftDataVersion(minecraftDataVersion);
                this.metadata.setFileType(FileType.LITEMATICA_SCHEMATIC);
                this.readSubRegionsFromNBT(nbt.getCompoundOrEmpty("Regions"), version, minecraftDataVersion);

                return true;
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_load.unsupported_schematic_version", version);
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_version_information");
        }

        return false;
    }

    private void readSubRegionsFromNBT(CompoundTag tag, int version, int minecraftDataVersion)
    {
        for (String regionName : tag.keySet())
        {
            if (tag.get(regionName).getId() == Constants.NBT.TAG_COMPOUND)
            {
                CompoundTag regionTag = tag.getCompoundOrEmpty(regionName);
                BlockPos regionPos = NbtUtils.readBlockPos(regionTag.getCompoundOrEmpty("Position"));
                BlockPos regionSize = NbtUtils.readBlockPos(regionTag.getCompoundOrEmpty("Size"));
                Map<BlockPos, CompoundTag> tiles = null;

                if (regionPos != null && regionSize != null)
                {
                    this.subRegionPositions.put(regionName, regionPos);
                    this.subRegionSizes.put(regionName, regionSize);

                    if (version >= 2)
                    {
                        tiles = this.readTileEntitiesFromNBT(regionTag.getListOrEmpty("TileEntities"));
                        tiles = this.convertTileEntities_to_1_20_5(tiles, minecraftDataVersion);
                        this.tileEntities.put(regionName, tiles);

                        ListTag entities = regionTag.getListOrEmpty("Entities");
                        entities = this.convertEntities_to_1_20_5(entities, minecraftDataVersion);
                        this.entities.put(regionName, this.readEntitiesFromNBT(entities));
                    }
                    else if (version == 1)
                    {
                        tiles = this.readTileEntitiesFromNBT_v1(regionTag.getListOrEmpty("TileEntities"));
                        this.tileEntities.put(regionName, tiles);
                        this.entities.put(regionName, this.readEntitiesFromNBT_v1(regionTag.getListOrEmpty("Entities")));
                    }

                    if (version >= 3)
                    {
                        ListTag list = regionTag.getListOrEmpty("PendingBlockTicks");
                        this.pendingBlockTicks.put(regionName, this.readPendingTicksFromNBT(list, BuiltInRegistries.BLOCK, "Block", Blocks.AIR));
                    }

                    if (version >= 5)
                    {
                        ListTag list = regionTag.getListOrEmpty("PendingFluidTicks");
                        this.pendingFluidTicks.put(regionName, this.readPendingTicksFromNBT(list, BuiltInRegistries.FLUID, "Fluid", Fluids.EMPTY));
                    }

                    Tag nbtBase = regionTag.get("BlockStates");

                    // There are no convenience methods in NBTTagCompound yet in 1.12, so we'll have to do it the ugly way...
                    if (nbtBase != null && nbtBase.getId() == Constants.NBT.TAG_LONG_ARRAY)
                    {
                        ListTag palette = regionTag.getListOrEmpty("BlockStatePalette");
                        long[] blockStateArr = ((LongArrayTag) nbtBase).getAsLongArray();

                        BlockPos posEndRel = PositionUtils.getRelativeEndPositionFromAreaSize(regionSize).offset(regionPos);
                        BlockPos posMin = PositionUtils.getMinCorner(regionPos, posEndRel);
                        BlockPos posMax = PositionUtils.getMaxCorner(regionPos, posEndRel);
                        BlockPos size = posMax.subtract(posMin).offset(1, 1, 1);

                        palette = this.convertBlockStatePalette_1_12_to_1_13_2(palette, version, minecraftDataVersion);
                        palette = this.convertBlockStatePalette_to_1_20_5(palette, minecraftDataVersion);

                        LitematicaBlockStateContainer container = LitematicaBlockStateContainer.createFrom(palette, blockStateArr, size);

                        if (minecraftDataVersion < MINECRAFT_DATA_VERSION)
                        {
                            this.postProcessContainerIfNeeded(palette, container, tiles);
                        }

                        this.blockContainers.put(regionName, container);
                    }
                }
            }
        }
    }

    public static boolean isSizeValid(@Nullable Vec3i size)
    {
        return size != null && size.getX() > 0 && size.getY() > 0 && size.getZ() > 0;
    }

    @Nullable
    private static Vec3i readSizeFromTagImpl(CompoundTag tag)
    {
        if (tag.contains("size"))
        {
            ListTag tagList = tag.getListOrEmpty("size");

            if (tagList.size() == 3)
            {
                return new Vec3i(tagList.getIntOr(0, 0), tagList.getIntOr(1, 0), tagList.getIntOr(2, 0));
            }
        }

        return null;
    }

    @Nullable
    public static BlockPos readBlockPosFromNbtList(CompoundTag tag, String tagName)
    {
        if (tag.contains(tagName))
        {
            ListTag tagList = tag.getListOrEmpty(tagName);

            if (tagList.size() == 3)
            {
                return new BlockPos(tagList.getIntOr(0, 0), tagList.getIntOr(1, 0), tagList.getIntOr(2, 0));
            }
        }

        return null;
    }

    protected boolean readPaletteFromLitematicaFormatTag(ListTag tagList, ILitematicaBlockStatePalette palette)
    {
        final int size = tagList.size();
        List<BlockState> list = new ArrayList<>(size);
        //RegistryEntryLookup<Block> lookup = Registries.createEntryLookup(Registries.BLOCK);
        HolderGetter<Block> lookup = SchematicWorldHandler.INSTANCE.getRegistryManager().lookupOrThrow(Registries.BLOCK);

        for (int id = 0; id < size; ++id)
        {
            CompoundTag tag = tagList.getCompoundOrEmpty(id);
            BlockState state = net.minecraft.nbt.NbtUtils.readBlockState(lookup, tag);
            list.add(state);
        }

        return palette.setMapping(list);
    }

    public static boolean isValidSpongeSchematic(CompoundTag tag)
    {
        // v2 Sponge Schematic
        if (tag.contains("Width") &&
            tag.contains("Height") &&
            tag.contains("Length") &&
            tag.contains("Version") &&
            tag.contains("Palette") &&
            tag.contains("BlockData"))
        {
            return isSizeValid(readSizeFromTagSponge(tag));
        }

        return false;
    }

    public static boolean isValidSpongeSchematicv3(CompoundTag tag)
    {
        // v3 Sponge Schematic
        if (tag.contains("Schematic"))
        {
            CompoundTag nbtV3 = tag.getCompoundOrEmpty("Schematic");

            if (nbtV3.contains("Width") &&
                nbtV3.contains("Height") &&
                nbtV3.contains("Length") &&
                nbtV3.contains("Version") &&
                nbtV3.getIntOr("Version", -1) >= 3 &&
                nbtV3.contains("Blocks") &&
                nbtV3.contains("DataVersion"))
            {
                return isSizeValid(readSizeFromTagSponge(nbtV3));
            }
        }

        return false;
    }

    public static Vec3i readSizeFromTagSponge(CompoundTag tag)
    {
        return new Vec3i(tag.getIntOr("Width", 0), tag.getIntOr("Height", 0), tag.getIntOr("Length", 0));
    }

    protected boolean readSpongePaletteFromTag(CompoundTag tag, ILitematicaBlockStatePalette palette, int minecraftDataVersion)
    {
        final int size = tag.keySet().size();
        List<BlockState> list = new ArrayList<>(size);
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int i = 0; i < size; ++i)
        {
            list.add(air);
        }

        for (String key : tag.keySet())
        {
            int id = tag.getIntOr(key, 0);
	        // Also updates Block Names ... Now.
	        Optional<BlockState> stateOptional = BlockUtils.getBlockStateFromString(key, minecraftDataVersion);
            BlockState state;

            if (stateOptional.isPresent())
            {
                state = stateOptional.get();
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "Unknown block in the Sponge schematic palette: '" + key + "'");
                state = LitematicaBlockStateContainer.AIR_BLOCK_STATE;
            }

            if (id < 0 || id >= size)
            {
                String msg = "Invalid ID in the Sponge schematic palette: '" + id + "'";
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, msg);
                Litematica.LOGGER.error(msg);
                return false;
            }

            list.set(id, state);
        }

        return palette.setMapping(list);
    }

    protected boolean readSpongeBlocksFromTagMetadataOnly(CompoundTag tag, String schematicName, Vec3i size, int minecraftDataVersion, int spongeVersion)
    {
        CompoundTag blocksTag = new CompoundTag();
        byte[] blockData;

        if (spongeVersion >= 3 && tag.contains("Blocks"))
        {
            blocksTag = tag.getCompoundOrEmpty("Blocks");

            if (blocksTag.contains("Palette") &&
                blocksTag.contains("Data"))
            {
                blockData = blocksTag.getByteArray("Data").orElse(new byte[0]);
                this.totalBlocksReadFromWorld = blockData.length;
            }
            else
            {
                return false;
            }
        }
        else
        {
            if (tag.contains("Palette") &&
                tag.contains("BlockData"))
            {
                blockData = tag.getByteArray("BlockData").orElse(new byte[0]);
                this.totalBlocksReadFromWorld = blockData.length;
            }
            else
            {
                return false;
            }
        }

        return true;
    }

    protected boolean readSpongeBlocksFromTag(CompoundTag tag, String schematicName, Vec3i size, int minecraftDataVersion, int spongeVersion)
    {
        CompoundTag blocksTag = new CompoundTag();
        CompoundTag paletteTag;
        byte[] blockData;
        int paletteSize;

        if (spongeVersion >= 3 && tag.contains("Blocks"))
        {
            blocksTag = tag.getCompoundOrEmpty("Blocks");

            if (blocksTag.contains("Palette") &&
                blocksTag.contains("Data"))
            {
                paletteTag = blocksTag.getCompoundOrEmpty("Palette");
                blockData = blocksTag.getByteArray("Data").orElse(new byte[0]);
                paletteSize = paletteTag.keySet().size();
            }
            else
            {
                return false;
            }
        }
        else
        {
            if (tag.contains("Palette") &&
                tag.contains("BlockData"))
            {
                paletteTag = tag.getCompoundOrEmpty("Palette");
                blockData = tag.getByteArray("BlockData").orElse(new byte[0]);
                paletteSize = paletteTag.keySet().size();
            }
            else
            {
                return false;
            }
        }

        LitematicaBlockStateContainer container = LitematicaBlockStateContainer.createContainer(paletteSize, blockData, size);

        if (container == null)
        {
            String msg = "Failed to read blocks from Sponge schematic";
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, msg);
            Litematica.LOGGER.error(msg);
            return false;
        }

        this.blockContainers.put(schematicName, container);

        if (this.readSpongePaletteFromTag(paletteTag, container.getPalette(), minecraftDataVersion) == false)
        {
            return false;
        }

        if (spongeVersion >= 3)
        {
            if (blocksTag.isEmpty() == false)
            {
                // tileEntities list moved to "Blocks" tag for V3
                Map<BlockPos, CompoundTag> tileEntities = this.readSpongeBlockEntitiesFromTag(blocksTag, spongeVersion);
                tileEntities = this.convertTileEntities_to_1_20_5(tileEntities, minecraftDataVersion);
                this.tileEntities.put(schematicName, tileEntities);
            }
            else
            {
                return false;
            }
        }

        return true;
    }

    protected Map<BlockPos, CompoundTag> readSpongeBlockEntitiesFromTag(CompoundTag tag, int spongeVersion)
    {
        Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();
        String tagName = spongeVersion == 1 ? "TileEntities" : "BlockEntities";
        
        if (tag.contains(tagName) == false)
        {
            return blockEntities;
        }
        
        ListTag tagList = tag.getListOrEmpty(tagName);

        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag beTag = tagList.getCompoundOrEmpty(i);
            BlockPos pos = NbtUtils.readBlockPosFromArrayTag(beTag, "Pos");

            if (pos != null && beTag.isEmpty() == false)
            {
                beTag.putString("id", beTag.getStringOr("Id", ""));

                // Remove the Sponge tags from the data that is kept in memory
                beTag.remove("Id");
                beTag.remove("Pos");

                if (spongeVersion == 1)
                {
                    beTag.remove("ContentVersion");
                }

                if (spongeVersion >= 3)
                {
                    CompoundTag beData = beTag.getCompoundOrEmpty("Data");
                    blockEntities.put(pos, beData);
                }
                else
                {
                    blockEntities.put(pos, beTag);
                }
            }
        }

        return blockEntities;
    }

    protected List<EntityInfo> readSpongeEntitiesFromTag(CompoundTag tag, Vec3i offset, int spongeVersion)
    {
        List<EntityInfo> entities = new ArrayList<>();
        ListTag tagList = tag.getListOrEmpty("Entities");
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag entityEntry = tagList.getCompoundOrEmpty(i);
            // pos = NbtUtils.readVec3dFromListTag(entityEntry);
            Vec3 pos = NbtUtils.getVec3dCodec(entityEntry, "Pos");

            if (pos != null && entityEntry.isEmpty() == false)
            {
                entityEntry.putString("id", entityEntry.getStringOr("Id", ""));

                // Remove the Sponge tags from the data that is kept in memory
                entityEntry.remove("Id");

                if (spongeVersion >= 3)
                {
                    CompoundTag entityData = entityEntry.getCompoundOrEmpty("Data");

                    if (entityData.contains("id") == false)
                    {
                        entityData.putString("id", entityEntry.getStringOr("id", ""));
                    }
                    entities.add(new EntityInfo(pos, entityData));
//                    Litematica.LOGGER.error("[SPONGE-V3] Pos[{}] / entity: {}", pos.toString(), entityData.toString());
                }
                else
                {
                    pos = new Vec3(pos.x - offset.getX(), pos.y - offset.getY(), pos.z - offset.getZ());
                    entities.add(new EntityInfo(pos, entityEntry));
//                    Litematica.LOGGER.error("[SPONGE-V2] Pos[{}] / entity: {}", pos.toString(), entityEntry.toString());
                }
            }
        }

        return entities;
    }

    public boolean readFromSpongeSchematicMetadataOnly(String name, CompoundTag tag)
    {
        if (isValidSpongeSchematicv3(tag))
        {
            // Probably not the "best" solution, but it works
            CompoundTag spongeTag = tag.getCompoundOrEmpty("Schematic");
            tag.remove("Schematic");
            tag.merge(spongeTag);
        }
        else if (isValidSpongeSchematic(tag) == false)
        {
            return false;
        }

        final int spongeVersion = tag.contains("Version") ? tag.getIntOr("Version", -1) : -1;
        final int minecraftDataVersion = tag.contains("DataVersion") ? tag.getIntOr("DataVersion", Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue()) : Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();
        Vec3i size = readSizeFromTagSponge(tag);

        if (this.readSpongeBlocksFromTagMetadataOnly(tag, name, size, minecraftDataVersion, spongeVersion) == false)
        {
            return false;
        }

        if (tag.contains("Metadata"))
        {
            CompoundTag metadata = tag.getCompoundOrEmpty("Metadata");

            this.metadata.setName(metadata.contains("Name") ? metadata.getStringOr("Name", "?") : name);
            this.metadata.setAuthor(metadata.contains("Author") ? metadata.getStringOr("Author", "?") : "unknown");
            this.metadata.setTimeCreated(metadata.contains("Date") ? metadata.getLongOr("Date", System.currentTimeMillis()) : System.currentTimeMillis());
        }
        else
        {
            this.metadata.setAuthor("unknown");
            this.metadata.setName(name);
            this.metadata.setTimeCreated(System.currentTimeMillis());
        }
        if (tag.contains("author"))
        {
            this.metadata.setAuthor(tag.getStringOr("author", "?"));
        }

        this.metadata.setRegionCount(1);
        this.metadata.setTotalVolume(size.getX() * size.getY() * size.getZ());
        this.metadata.setEnclosingSize(size);
        this.metadata.setTimeModified(this.metadata.getTimeCreated());
        this.metadata.setTotalBlocks(this.totalBlocksReadFromWorld);
        this.metadata.setSchematicVersion(spongeVersion);
        this.metadata.setMinecraftDataVersion(minecraftDataVersion);
        this.metadata.setFileType(FileType.SPONGE_SCHEMATIC);

        return true;
    }

    public boolean readFromSpongeSchematic(String name, CompoundTag tag)
    {
        if (isValidSpongeSchematicv3(tag))
        {
            // Probably not the "best" solution, but it works
            CompoundTag spongeTag = tag.getCompoundOrEmpty("Schematic");
            tag.remove("Schematic");
            tag.merge(spongeTag);
        }
        else if (isValidSpongeSchematic(tag) == false)
        {
            return false;
        }

        final int spongeVersion = tag.contains("Version") ? tag.getIntOr("Version", -1) : -1;
        final int minecraftDataVersion = tag.contains("DataVersion") ? tag.getIntOr("DataVersion", Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue()) : Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();
        Vec3i size = readSizeFromTagSponge(tag);

        // Can't really use the Data Fixer for the Block State Palette in this format,
        // so we're just going to ignore it, as long as we fix the Tile/Entities.
        if (this.readSpongeBlocksFromTag(tag, name, size, minecraftDataVersion, spongeVersion) == false)
        {
            return false;
        }

        Vec3i offset = NbtUtils.readVec3iFromIntArray(tag, "Offset");

        if (offset == null)
        {
            offset = Vec3i.ZERO;
        }

        if (spongeVersion < 3)
        {
            Map<BlockPos, CompoundTag> tileEntities = this.readSpongeBlockEntitiesFromTag(tag, spongeVersion);
            tileEntities = this.convertTileEntities_to_1_20_5(tileEntities, minecraftDataVersion);
            this.tileEntities.put(name, tileEntities);
        }

        List<EntityInfo> entities = this.readSpongeEntitiesFromTag(tag, offset, spongeVersion);
        entities = this.convertSpongeEntities_to_1_20_5(entities, minecraftDataVersion);
        this.entities.put(name, entities);

        if (tag.contains("Metadata"))
        {
            CompoundTag metadata = tag.getCompoundOrEmpty("Metadata");

            this.metadata.setName(metadata.contains("Name") ? metadata.getStringOr("Name", "?") : name);
            this.metadata.setAuthor(metadata.contains("Author") ? metadata.getStringOr("Author", "?") : "unknown");
            this.metadata.setTimeCreated(metadata.contains("Date") ? metadata.getLongOr("Date", System.currentTimeMillis()) : System.currentTimeMillis());
        }
        else
        {
            this.metadata.setAuthor("unknown");
            this.metadata.setName(name);
            this.metadata.setTimeCreated(System.currentTimeMillis());
        }
        if (tag.contains("author"))
        {
            this.metadata.setAuthor(tag.getStringOr("author", "?"));
        }

        this.subRegionPositions.put(name, BlockPos.ZERO);
        this.subRegionSizes.put(name, new BlockPos(size));
        this.metadata.setRegionCount(1);
        this.metadata.setTotalVolume(size.getX() * size.getY() * size.getZ());
        this.metadata.setEnclosingSize(size);
        this.metadata.setTimeModified(this.metadata.getTimeCreated());
        this.metadata.setTotalBlocks(this.totalBlocksReadFromWorld);
        this.metadata.setSchematicVersion(spongeVersion);
        this.metadata.setMinecraftDataVersion(minecraftDataVersion);
        this.metadata.setFileType(FileType.SPONGE_SCHEMATIC);

        return true;
    }

    public boolean readFromVanillaStructureMetadataOnly(String name, CompoundTag tag)
    {
        Vec3i size = readSizeFromTagImpl(tag);

        if (tag.contains("palette") &&
            tag.contains("blocks") &&
            isSizeValid(size))
        {
            int minecraftDataVersion = tag.contains("DataVersion") ? tag.getIntOr("DataVersion", Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue()) : Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();
            if (tag.contains("author"))
            {
                this.getMetadata().setAuthor(tag.getStringOr("author", "?"));
            }

            this.metadata.setName(name);
            this.metadata.setRegionCount(1);
            this.metadata.setTotalVolume(size.getX() * size.getY() * size.getZ());
            this.metadata.setEnclosingSize(size);
            this.metadata.setTimeCreated(System.currentTimeMillis());
            this.metadata.setTimeModified(this.metadata.getTimeCreated());
            this.metadata.setSchematicVersion(0);
            this.metadata.setMinecraftDataVersion(minecraftDataVersion);
            this.metadata.setFileType(FileType.VANILLA_STRUCTURE);

            ListTag blockList = tag.getListOrEmpty("blocks");
            this.metadata.setTotalBlocks(blockList.size());

            return true;
        }

        return false;
    }

    public boolean readFromVanillaStructure(String name, CompoundTag tag)
    {
        Vec3i size = readSizeFromTagImpl(tag);

        if (tag.contains("palette") &&
            tag.contains("blocks") &&
            isSizeValid(size))
        {
            ListTag paletteTag = tag.getListOrEmpty("palette");
            int minecraftDataVersion = tag.contains("DataVersion") ? tag.getIntOr("DataVersion", Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue()) : Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();

            Map<BlockPos, CompoundTag> tileMap = new HashMap<>();
            this.tileEntities.put(name, tileMap);

            BlockState air = Blocks.AIR.defaultBlockState();
            int paletteSize = paletteTag.size();
            List<BlockState> list = new ArrayList<>(paletteSize);
            HolderGetter<Block> lookup = SchematicWorldHandler.INSTANCE.getRegistryManager().lookupOrThrow(Registries.BLOCK);

            Schema effective = DataFixerMode.getEffectiveSchema(minecraftDataVersion);
            if (minecraftDataVersion < LitematicaSchematic.MINECRAFT_DATA_VERSION && effective != null)
            {
                Litematica.LOGGER.info("VanillaStructure: executing Vanilla DataFixer for Block State Palette DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION);
            }
            else if (effective == null)
            {
                Litematica.LOGGER.warn("readFromVanillaStructure(): Effective Schema has been bypassed.  Not applying Vanilla Data Fixer for Block State Palette DataVersion {}", minecraftDataVersion);
            }

            for (int id = 0; id < paletteSize; ++id)
            {
                CompoundTag t = paletteTag.getCompoundOrEmpty(id);
                if (minecraftDataVersion < LitematicaSchematic.MINECRAFT_DATA_VERSION && effective != null)
                {
					// Also updates Block Names
                    t = SchematicConversionMaps.updateBlockStates(t, minecraftDataVersion);
                }

                BlockState state = net.minecraft.nbt.NbtUtils.readBlockState(lookup, t);
                list.add(state);
            }

            BlockState zeroState = list.get(0);
            int airId = -1;

            // If air is not ID 0, then we need to re-map the palette such that air is ID 0,
            // due to how it's currently handled in the Litematica container.
            for (int i = 0; i < paletteSize; ++i)
            {
                if (list.get(i) == air)
                {
                    airId = i;
                    break;
                }
            }

            if (airId != 0)
            {
                // No air in the palette, insert it
                if (airId == -1)
                {
                    list.add(0, air);
                    ++paletteSize;
                }
                // Air as some other ID, swap the entries
                else
                {
                    list.set(0, air);
                    list.set(airId, zeroState);
                }
            }

            int bits = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));
            LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(size.getX(), size.getY(), size.getZ(), bits, null);
            ILitematicaBlockStatePalette palette = container.getPalette();
            palette.setMapping(list);
            this.blockContainers.put(name, container);

            if (tag.contains("author"))
            {
                this.getMetadata().setAuthor(tag.getStringOr("author", "?"));
            }

            this.subRegionPositions.put(name, BlockPos.ZERO);
            this.subRegionSizes.put(name, new BlockPos(size));
            this.metadata.setName(name);
            this.metadata.setRegionCount(1);
            this.metadata.setTotalVolume(size.getX() * size.getY() * size.getZ());
            this.metadata.setEnclosingSize(size);
            this.metadata.setTimeCreated(System.currentTimeMillis());
            this.metadata.setTimeModified(this.metadata.getTimeCreated());
            this.metadata.setSchematicVersion(0);
            this.metadata.setMinecraftDataVersion(minecraftDataVersion);
            this.metadata.setFileType(FileType.VANILLA_STRUCTURE);

            ListTag blockList = tag.getListOrEmpty("blocks");
            final int count = blockList.size();
            int totalBlocks = 0;

            for (int i = 0; i < count; ++i)
            {
                CompoundTag blockTag = blockList.getCompoundOrEmpty(i);
                BlockPos pos = readBlockPosFromNbtList(blockTag, "pos");

                if (pos == null)
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Failed to read block position for vanilla structure");
                    return false;
                }

                int id = blockTag.getIntOr("state", 0);
                BlockState state;

                // Air was inserted as ID 0, so the other IDs need to shift
                if (airId == -1)
                {
                    state = palette.getBlockState(id + 1);
                }
                else if (airId != 0)
                {
                    // re-mapping air and ID 0 state
                    if (id == 0)
                    {
                        state = zeroState;
                    }
                    else if (id == airId)
                    {
                        state = air;
                    }
                    else
                    {
                        state = palette.getBlockState(id);
                    }
                }
                else
                {
                    state = palette.getBlockState(id);
                }

                if (state == null)
                {
                    state = air;
                }
                else if (state != air)
                {
                    ++totalBlocks;
                }

                container.set(pos.getX(), pos.getY(), pos.getZ(), state);

                if (blockTag.contains("nbt"))
                {
                    tileMap.put(pos, blockTag.getCompoundOrEmpty("nbt"));
                }
            }

            this.metadata.setTotalBlocks(totalBlocks);
            this.entities.put(name, this.readEntitiesFromVanillaStructure(tag, minecraftDataVersion));

            return true;
        }

        return false;
    }

    protected List<EntityInfo> readEntitiesFromVanillaStructure(CompoundTag tag, int minecraftDataVersion)
    {
        List<EntityInfo> entities = new ArrayList<>();
        ListTag tagList = tag.getListOrEmpty("entities");
        final int size = tagList.size();
        Schema effective = DataFixerMode.getEffectiveSchema(minecraftDataVersion);

        if (minecraftDataVersion < LitematicaSchematic.MINECRAFT_DATA_VERSION && effective != null)
        {
            Litematica.LOGGER.info("VanillaStructure: executing Vanilla DataFixer for Entities DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION);
        }
        else if (effective == null)
        {
            Litematica.LOGGER.warn("readEntitiesFromVanillaStructure(): Effective Schema has been bypassed.  Not applying Vanilla Data Fixer for Entities DataVersion {}", minecraftDataVersion);
        }
        for (int i = 0; i < size; ++i)
        {
            CompoundTag entityData = tagList.getCompoundOrEmpty(i);
            CompoundTag nbtData = entityData.getCompoundOrEmpty("nbt");
            if (minecraftDataVersion < LitematicaSchematic.MINECRAFT_DATA_VERSION && effective != null)
            {
                nbtData = SchematicConversionMaps.updateEntity(nbtData, minecraftDataVersion);
            }
//            Vec3 pos = readVec3dFromNbtList(entityData, "pos");
            Vec3 pos = NbtUtils.getVec3dCodec(entityData, "pos");

            if (pos != null && !nbtData.isEmpty())
            {
                entities.add(new EntityInfo(pos, nbtData));
            }
        }

        return entities;
    }

    @Nullable
    public static Vec3 readVec3dFromNbtList(@Nullable CompoundTag tag, String tagName)
    {
        if (tag != null && tag.contains(tagName))
        {
            ListTag tagList = tag.getListOrEmpty(tagName);

            if (tagList.getId() == Constants.NBT.TAG_DOUBLE && tagList.size() == 3)
            {
                return new Vec3(tagList.getDoubleOr(0, 0d), tagList.getDoubleOr(1, 0d), tagList.getDoubleOr(2, 0d));
            }
        }

        return null;
    }

    private void postProcessContainerIfNeeded(ListTag palette, LitematicaBlockStateContainer container, @Nullable Map<BlockPos, CompoundTag> tiles)
    {
        List<BlockState> states = getStatesFromPaletteTag(palette);

        if (this.converter.createPostProcessStateFilter(states))
        {
            IdentityHashMap<BlockState, SchematicConversionFixers.IStateFixer> postProcessingFilter = this.converter.getPostProcessStateFilter();
            SchematicConverter.postProcessBlocks(container, tiles, postProcessingFilter);
        }
    }

    public static List<BlockState> getStatesFromPaletteTag(ListTag palette)
    {
        List<BlockState> states = new ArrayList<>();
        HolderGetter<Block> lookup = SchematicWorldHandler.INSTANCE.getRegistryManager().lookupOrThrow(Registries.BLOCK);
        final int size = palette.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = palette.getCompoundOrEmpty(i);
            BlockState state = net.minecraft.nbt.NbtUtils.readBlockState(lookup, tag);

            if (i > 0 || state != LitematicaBlockStateContainer.AIR_BLOCK_STATE)
            {
                states.add(state);
            }
        }

        return states;
    }

    private ListTag convertBlockStatePalette_1_12_to_1_13_2(ListTag oldPalette, int version, int minecraftDataVersion)
    {
        // The Minecraft data version didn't yet exist in the first 1.13.2 builds, so only
        // consider it if it actually exists in the file, ie. is larger than the default value of 0.
        if (version < SCHEMATIC_VERSION_1_13_2 || (minecraftDataVersion < MINECRAFT_DATA_VERSION_1_13_2 && minecraftDataVersion > 0))
        {
            ListTag newPalette = new ListTag();
            final int count = oldPalette.size();

            for (int i = 0; i < count; ++i)
            {
                newPalette.add(SchematicConversionMaps.get_1_13_2_StateTagFor_1_12_Tag(oldPalette.getCompoundOrEmpty(i)));
            }

            return newPalette;
        }

        return oldPalette;
    }

    private ListTag convertBlockStatePalette_to_1_20_5(ListTag oldPalette, int minecraftDataVersion)
    {
        if (minecraftDataVersion < Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue())
        {
            minecraftDataVersion = Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();
        }
        if (minecraftDataVersion < LitematicaSchematic.MINECRAFT_DATA_VERSION)
        {
            Schema effective = DataFixerMode.getEffectiveSchema(minecraftDataVersion);

            if (effective == null)
            {
                Litematica.LOGGER.warn("LitematicaSchematic: Effective Schema has been bypassed.  Not applying Vanilla Data Fixer for Block State Palette DataVersion {}", minecraftDataVersion);
                return oldPalette;
            }

            ListTag newPalette = new ListTag();
            final int count = oldPalette.size();
            Litematica.LOGGER.info("LitematicaSchematic: executing Vanilla DataFixer for Block State Palette DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION);

            for (int i = 0; i < count; ++i)
            {
                newPalette.add(SchematicConversionMaps.updateBlockStates(oldPalette.getCompoundOrEmpty(i), minecraftDataVersion));
            }

            return newPalette;
        }

        return oldPalette;
    }

    private Map<BlockPos, CompoundTag> convertTileEntities_to_1_20_5(Map<BlockPos, CompoundTag> oldTE, int minecraftDataVersion)
    {
        if (minecraftDataVersion < Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue())
        {
            minecraftDataVersion = Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();
        }
        if (minecraftDataVersion < LitematicaSchematic.MINECRAFT_DATA_VERSION)
        {
            Schema effective = DataFixerMode.getEffectiveSchema(minecraftDataVersion);

            if (effective == null)
            {
                Litematica.LOGGER.warn("LitematicaSchematic: Effective Schema has been bypassed.  Not applying Vanilla Data Fixer for Tile Entities DataVersion {}", minecraftDataVersion);
                return oldTE;
            }

            Map<BlockPos, CompoundTag> newTE = new HashMap<>();

            Litematica.LOGGER.info("LitematicaSchematic: executing Vanilla DataFixer for Tile Entities DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION);

            for (BlockPos key : oldTE.keySet())
            {
                newTE.put(key, SchematicConversionMaps.updateBlockEntity(SchematicConversionMaps.checkForIdTag(oldTE.get(key)), minecraftDataVersion));
            }

            return newTE;
        }

        return oldTE;
    }

    private ListTag convertEntities_to_1_20_5(ListTag oldEntitiesList, int minecraftDataVersion)
    {
        if (minecraftDataVersion < Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue())
        {
            minecraftDataVersion = Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();
        }
        if (minecraftDataVersion < LitematicaSchematic.MINECRAFT_DATA_VERSION)
        {
            Schema effective = DataFixerMode.getEffectiveSchema(minecraftDataVersion);

            if (effective == null)
            {
                Litematica.LOGGER.warn("LitematicaSchematic: Effective Schema has been bypassed.  Not applying Vanilla Data Fixer for Entities DataVersion {}", minecraftDataVersion);
                return oldEntitiesList;
            }

            ListTag newEntitiesList = new ListTag();
            final int size = oldEntitiesList.size();

            Litematica.LOGGER.info("LitematicaSchematic: executing Vanilla DataFixer for Entities DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION);

            for (int i = 0; i < size; i++)
            {
                newEntitiesList.add(SchematicConversionMaps.updateEntity(oldEntitiesList.getCompoundOrEmpty(i), minecraftDataVersion));
            }

            return newEntitiesList;
        }

        return oldEntitiesList;
    }

    private List<EntityInfo> convertSpongeEntities_to_1_20_5(List<EntityInfo> oldEntitiesList, int minecraftDataVersion)
    {
        if (minecraftDataVersion < Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue())
        {
            minecraftDataVersion = Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();
        }

        if (minecraftDataVersion < LitematicaSchematic.MINECRAFT_DATA_VERSION)
        {
            Schema effective = DataFixerMode.getEffectiveSchema(minecraftDataVersion);

            if (effective == null)
            {
                Litematica.LOGGER.warn("SpongeSchematic: Effective Schema has been bypassed.  Not applying Vanilla Data Fixer for Entities DataVersion {}", minecraftDataVersion);
                return oldEntitiesList;
            }

            List<EntityInfo> newEntitiesList = new ArrayList<>();

            Litematica.LOGGER.info("SpongeSchematic: executing Vanilla DataFixer for Entities DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION);

            for (EntityInfo oldEntityInfo : oldEntitiesList)
            {
                newEntitiesList.add(new EntityInfo(oldEntityInfo.posVec, SchematicConversionMaps.updateEntity(oldEntityInfo.nbt, minecraftDataVersion)));
            }

            return newEntitiesList;
        }

        return oldEntitiesList;
    }

    private Map<BlockPos, CompoundTag> downgradeTileEntities_to_1_20_4(Map<BlockPos, CompoundTag> oldTE, int minecraftDataVersion)
    {
        Map<BlockPos, CompoundTag> newTE = new HashMap<>();

        Litematica.LOGGER.info("LitematicaSchematic: Downgrade Tile Entities from DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION_1_20_4);

        for (BlockPos key : oldTE.keySet())
        {
            newTE.put(key, SchematicDowngradeConverter.downgradeBlockEntity_to_1_20_4(oldTE.get(key), minecraftDataVersion, Minecraft.getInstance().level.registryAccess()));
        }

        return newTE;
    }

    private ListTag downgradeEntities_to_1_20_4(ListTag oldEntitiesList, int minecraftDataVersion)
    {
        ListTag newEntitiesList = new ListTag();
        final int size = oldEntitiesList.size();

        Litematica.LOGGER.info("LitematicaSchematic: Downgrade Entities from DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION_1_20_4);

        for (int i = 0; i < size; i++)
        {
            newEntitiesList.add(SchematicDowngradeConverter.downgradeEntity_to_1_20_4(SchematicConversionMaps.fixEntityTypesFrom1_21_2(oldEntitiesList.getCompoundOrEmpty(i)), minecraftDataVersion, Minecraft.getInstance().level.registryAccess()));
        }

        return newEntitiesList;
    }

    private List<EntityInfo> readEntitiesFromNBT(ListTag tagList)
    {
        List<EntityInfo> entityList = new ArrayList<>();
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag entityData = tagList.getCompoundOrEmpty(i);
//            Vec3d posVec = NbtUtils.readEntityPositionFromTag(entityData);
            Vec3 posVec = NbtUtils.getVec3dCodec(entityData, "Pos");

//            Litematica.LOGGER.error("readEntitiesFromNBT: posVec [{}], NBT [{}]", posVec.toString(), entityData.toString());

            if (posVec != null && entityData.isEmpty() == false)
            {
                entityList.add(new EntityInfo(posVec, entityData));
            }
        }

        return entityList;
    }

    private Map<BlockPos, CompoundTag> readTileEntitiesFromNBT(ListTag tagList)
    {
        Map<BlockPos, CompoundTag> tileMap = new HashMap<>();
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = tagList.getCompoundOrEmpty(i);
            BlockPos pos = NbtUtils.readBlockPos(tag);

            if (pos != null && tag.isEmpty() == false)
            {
                tileMap.put(pos, tag);
            }
        }

        return tileMap;
    }

    private <T> Map<BlockPos, ScheduledTick<T>> readPendingTicksFromNBT(ListTag tagList, Registry<T> registry,
                                                                      String tagName, T emptyValue)
    {
        Map<BlockPos, ScheduledTick<T>> tickMap = new HashMap<>();
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = tagList.getCompoundOrEmpty(i);

            if (tag.contains("Time")) // XXX these were accidentally saved as longs in version 3
            {
                T target = null;

                // Don't crash on invalid ResourceLocation in 1.13+
                try
                {
                    Optional<Holder.Reference<T>> opt = registry.get(Identifier.tryParse(tag.getStringOr(tagName, "")));

                    //target = registry.get(Identifier.tryParse(tag.getString(tagName)));
                    //if (target == null || target == emptyValue)
                    //{
                        //continue;
                    //}
                    if (opt.isPresent())
                    {
                        if (opt.get().isBound())
                        {
                            target = opt.get().value();
                        }
                        else
                        {
                            continue;
                        }
                    }
                    else
                    {
                        continue;
                    }
                }
                catch (Exception ignore) {}

                if (target != null)
                {
                    BlockPos pos = new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0));
                    TickPriority priority = TickPriority.byValue(tag.getIntOr("Priority", 0));
                    // Note: the time is a relative delay at this point
                    int scheduledTime = tag.getIntOr("Time", 0);
                    long subTick = tag.getLongOr("SubTick", 0L);
                    tickMap.put(pos, new ScheduledTick<>(target, pos, scheduledTime, priority, subTick));
                }
            }
        }

        return tickMap;
    }

    private List<EntityInfo> readEntitiesFromNBT_v1(ListTag tagList)
    {
        List<EntityInfo> entityList = new ArrayList<>();
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = tagList.getCompoundOrEmpty(i);
            Vec3 posVec = NbtUtils.readVec3d(tag);
//            Vec3d posVec = NbtUtils.getVec3dCodec(tag, "Pos");
            CompoundTag entityData = tag.getCompoundOrEmpty("EntityData");

            if (posVec != null && entityData.isEmpty() == false)
            {
                // Update the correct position to the TileEntity NBT, where it is stored in version 2
//                NbtUtils.writeEntityPositionToTag(posVec, entityData);
                NbtUtils.putVec3dCodec(entityData, posVec, "Pos");
                entityList.add(new EntityInfo(posVec, entityData));
            }
        }

        return entityList;
    }

    private Map<BlockPos, CompoundTag> readTileEntitiesFromNBT_v1(ListTag tagList)
    {
        Map<BlockPos, CompoundTag> tileMap = new HashMap<>();
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundTag tag = tagList.getCompoundOrEmpty(i);
            CompoundTag tileNbt = tag.getCompoundOrEmpty("TileNBT");

            // Note: This within-schematic relative position is not inside the tile tag!
            BlockPos pos = NbtUtils.readBlockPos(tag);

            if (pos != null && tileNbt.isEmpty() == false)
            {
                // Update the correct position to the entity NBT, where it is stored in version 2
                NbtUtils.writeBlockPos(pos, tileNbt);
                tileMap.put(pos, tileNbt);
            }
        }

        return tileMap;
    }

    public boolean writeToFile(Path dir, String fileNameIn, boolean override)
    {
        return this.writeToFile(dir, fileNameIn, override, false);
    }

    public boolean writeToFile(Path dir, String fileNameIn, boolean override, boolean downgrade)
    {
        String fileName = fileNameIn;

        if (fileName.endsWith(FILE_EXTENSION) == false)
        {
            fileName = fileName + FILE_EXTENSION;
        }

        Path fileSchematic = dir.resolve(fileName);

        try
        {
            if (!Files.exists(dir))
            {
                FileUtils.createDirectoriesIfMissing(dir);
            }

            if (!Files.isDirectory(dir))
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.toAbsolutePath());
                return false;
            }

            if (override == false && Files.exists(fileSchematic))
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.exists", fileSchematic.toAbsolutePath());
                return false;
            }

            //FileOutputStream os = new FileOutputStream(fileSchematic);
            if (downgrade)
            {
                //NbtIo.writeCompressed(this.writeToNBT_v6(), os);
                NbtUtils.writeCompressed(this.writeToNBT_v6(), fileSchematic);
            }
            else
            {
                //NbtIo.writeCompressed(this.writeToNBT(), os);
                NbtUtils.writeCompressed(this.writeToNBT(), fileSchematic);
            }
            //os.close();

            return true;
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_write_to_file_failed.exception", fileSchematic.toAbsolutePath());
            Litematica.LOGGER.error(StringUtils.translate("litematica.error.schematic_write_to_file_failed.exception", fileSchematic.toAbsolutePath()), e);
            Litematica.LOGGER.error(e.getMessage());
        }

        return false;
    }

    public boolean readFromFile()
    {
        return this.readFromFile(this.schematicType);
    }

    private boolean readFromFile(FileType schematicType)
    {
        try
        {
            CompoundTag nbt = readNbtFromFile(this.schematicFile);

            if (nbt != null)
            {
                if (schematicType == FileType.SPONGE_SCHEMATIC)
                {
                    String name = FileUtils.getNameWithoutExtension(this.schematicFile.getFileName().toString()) + " (Converted Sponge)";
                    return this.readFromSpongeSchematic(name, nbt);
                }
                else if (schematicType == FileType.VANILLA_STRUCTURE)
                {
                    String name = FileUtils.getNameWithoutExtension(this.schematicFile.getFileName().toString()) + " (Converted Structure)";
                    return this.readFromVanillaStructure(name, nbt);
                }
                else if (schematicType == FileType.LITEMATICA_SCHEMATIC)
                {
                    return this.readFromNBT(nbt);
                }

                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.cant_read", this.schematicFile.toAbsolutePath());
            }
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.exception", this.schematicFile.toAbsolutePath());
            Litematica.LOGGER.error(e);
        }

        return false;
    }

    public static CompoundTag readNbtFromFile(Path file)
    {
        if (file == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.no_file");
            return null;
        }

        if (Files.exists(file) == false || Files.isReadable(file) == false)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_read_from_file_failed.cant_read", file.toAbsolutePath());
            return null;
        }

        return NbtUtils.readNbtFromFileAsPath(file);
    }

    public static Path fileFromDirAndName(Path dir, String fileName, FileType schematicType)
    {
        if (fileName.endsWith(FILE_EXTENSION) == false && schematicType == FileType.LITEMATICA_SCHEMATIC)
        {
            fileName = fileName + FILE_EXTENSION;
        }

        return dir.resolve(fileName);
    }

    public static void updateMetadataWithFileTime(Path file, SchematicMetadata metadata)
    {
        try
        {
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);

            metadata.setTimeCreated(attr.creationTime().toMillis());
            metadata.setTimeModified(attr.lastModifiedTime().toMillis());
        }
        catch (Exception err)
        {
            Litematica.LOGGER.error("getFileCreatedTime(): Exception reading file '{}'; {}", file.getFileName().toString(), err.getLocalizedMessage());
        }
    }

    @Nullable
    public static SchematicMetadata readMetadataFromFile(Path dir, String fileName)
    {
		if (dir == null)
		{
			Litematica.LOGGER.error("LitematicaSchematic#readMetadataFromFile(): dir is NULL; please correct this when loading files.");
			return null;
		}

		Path file = dir.resolve(fileName);
        FileType type = FileType.fromFile(file);

        if (type == FileType.INVALID)
        {
            file = fileFromDirAndName(dir, fileName, FileType.LITEMATICA_SCHEMATIC);
            type = FileType.fromFile(file);
        }

        if (type == FileType.INVALID)
        {
            return null;
        }

        CompoundTag nbt = readNbtFromFile(file);

        if (nbt != null)
        {
            switch (type)
            {
                case LITEMATICA_SCHEMATIC ->
                {
                    SchematicMetadata metadata = new SchematicMetadata();

                    if (nbt.contains("Version"))
                    {
                        final int version = nbt.getIntOr("Version", -1);

                        if (version >= 1 && version <= SCHEMATIC_VERSION)
                        {
                            metadata.readFromNBT(nbt.getCompoundOrEmpty("Metadata"));
                            metadata.setFileType(type);
                            return metadata;
                        }
                    }
                }
                case SPONGE_SCHEMATIC ->
                {
                    LitematicaSchematic schem = new LitematicaSchematic(file, type);

                    if (schem.readFromSpongeSchematicMetadataOnly(fileName, nbt))
                    {
                        SchematicMetadata meta = schem.getMetadata();
                        updateMetadataWithFileTime(file, meta);
                        return meta;
                    }
                }
                case VANILLA_STRUCTURE ->
                {
                    LitematicaSchematic schem = new LitematicaSchematic(file, type);

                    if (schem.readFromVanillaStructureMetadataOnly(fileName, nbt))
                    {
                        SchematicMetadata meta = schem.getMetadata();
                        updateMetadataWithFileTime(file, meta);
                        return meta;
                    }
                }
                case SCHEMATICA_SCHEMATIC ->
                {
                    SchematicaSchematic schem = new SchematicaSchematic();

                    if (schem.readBlocksFromNBTMetadataOnly(file, nbt))
                    {
                        SchematicMetadata meta = schem.getMetadata();
                        updateMetadataWithFileTime(file, meta);
                        return meta;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    public static Pair<SchematicSchema, SchematicMetadata> readMetadataAndVersionFromFile(Path dir, String fileName)
    {
		if (dir == null)
		{
			Litematica.LOGGER.error("LitematicaSchematic#readMetadataAndVersionFromFile(): dir is NULL; please correct this when loading files.");
			return null;
		}

		Path file = dir.resolve(fileName);
        FileType type = FileType.fromFile(file);

        if (type == FileType.INVALID)
        {
            file = fileFromDirAndName(dir, fileName, FileType.LITEMATICA_SCHEMATIC);
            type = FileType.fromFile(file);
        }

        if (type == FileType.INVALID)
        {
            return null;
        }

        CompoundTag nbt = readNbtFromFile(file);

        //System.out.printf("readMetadataAndVersionFromFile(): file [%s] // name [%s] // type [%s] // nbt? [%s]\n", file.getPath(), fileName, FileType.getString(type), nbt == null ? "null" : "has_tags");

        if (nbt != null)
        {
            switch (type)
            {
                case LITEMATICA_SCHEMATIC ->
                {
                    SchematicMetadata metadata = new SchematicMetadata();

                    if (nbt.contains("Version"))
                    {
                        final int version = nbt.getIntOr("Version", -1);
                        final int dataVersion = nbt.contains("MinecraftDataVersion") ? nbt.getIntOr("MinecraftDataVersion", -1) : -1;

                        if (version >= 1 && version <= SCHEMATIC_VERSION)
                        {
                            metadata.readFromNBT(nbt.getCompoundOrEmpty("Metadata"));
                            metadata.setFileType(type);
                            return Pair.of(new SchematicSchema(version, dataVersion), metadata);
                        }
                    }
                }
                case SPONGE_SCHEMATIC ->
                {
                    LitematicaSchematic schem = new LitematicaSchematic(file, type);
//                    DataFixerMode dataFixer = (DataFixerMode) Configs.Generic.DATAFIXER_MODE.getOptionListValue();
//                    Configs.Generic.DATAFIXER_MODE.setOptionListValue(DataFixerMode.NEVER);

                    if (schem.readFromSpongeSchematicMetadataOnly(fileName, nbt))
                    {
//                        Configs.Generic.DATAFIXER_MODE.setOptionListValue(dataFixer);
                        SchematicMetadata meta = schem.getMetadata();
                        updateMetadataWithFileTime(file, meta);
                        return Pair.of(meta.getSchematicSchema(), meta);
                    }
//                    else
//                    {
//                        Configs.Generic.DATAFIXER_MODE.setOptionListValue(dataFixer);
//                    }
                }
                case VANILLA_STRUCTURE ->
                {
                    LitematicaSchematic schem = new LitematicaSchematic(file, type);
//                    DataFixerMode dataFixer = (DataFixerMode) Configs.Generic.DATAFIXER_MODE.getOptionListValue();
//                    Configs.Generic.DATAFIXER_MODE.setOptionListValue(DataFixerMode.NEVER);

                    if (schem.readFromVanillaStructureMetadataOnly(fileName, nbt))
                    {
//                        Configs.Generic.DATAFIXER_MODE.setOptionListValue(dataFixer);
                        SchematicMetadata meta = schem.getMetadata();
                        updateMetadataWithFileTime(file, meta);
                        return Pair.of(meta.getSchematicSchema(), meta);
                    }
//                    else
//                    {
//                        Configs.Generic.DATAFIXER_MODE.setOptionListValue(dataFixer);
//                    }
                }
                case SCHEMATICA_SCHEMATIC ->
                {
                    SchematicaSchematic schem = new SchematicaSchematic();

                    if (schem.readBlocksFromNBTMetadataOnly(file, nbt))
                    {
//                        Configs.Generic.DATAFIXER_MODE.setOptionListValue(dataFixer);
                        SchematicMetadata meta = schem.getMetadata();
                        updateMetadataWithFileTime(file, meta);
                        return Pair.of(meta.getSchematicSchema(), meta);
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    public static SchematicSchema readDataVersionFromFile(Path dir, String fileName)
    {
		if (dir == null)
		{
			Litematica.LOGGER.error("LitematicaSchematic#readDataVersionFromFile(): dir is NULL; please correct this when loading files.");
			return null;
		}

		Path file = dir.resolve(fileName);
        FileType type = FileType.fromFile(file);

        if (type == FileType.INVALID)
        {
            file = fileFromDirAndName(dir, fileName, FileType.LITEMATICA_SCHEMATIC);
            type = FileType.fromFile(file);
        }

        if (type == FileType.INVALID)
        {
            return null;
        }

        CompoundTag nbt = readNbtFromFile(file);

        if (nbt != null)
        {
            switch (type)
            {
                case LITEMATICA_SCHEMATIC ->
                {
                    if (nbt.contains("Version"))
                    {
                        final int version = nbt.getIntOr("Version", -1);
                        final int dataVersion = nbt.contains("MinecraftDataVersion") ? nbt.getIntOr("MinecraftDataVersion", Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue()) : Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();

                        if (version >= 1)
                        {
                            return new SchematicSchema(version, dataVersion);
                        }
                    }
                }
                case SPONGE_SCHEMATIC ->
                {
                    CompoundTag spongeTag = new CompoundTag();

                    if (isValidSpongeSchematicv3(nbt))
                    {
                        spongeTag.merge(nbt.getCompoundOrEmpty("Schematic"));
                    }
                    else if (isValidSpongeSchematic(nbt))
                    {
                        spongeTag.merge(nbt);
                    }

                    final int spongeVersion = spongeTag.contains("Version") ? spongeTag.getIntOr("Version", -1) : -1;
                    final int minecraftDataVersion = spongeTag.contains("DataVersion") ? spongeTag.getIntOr("DataVersion", Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue()) : Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();

                    return new SchematicSchema(spongeVersion, minecraftDataVersion);
                }
                case VANILLA_STRUCTURE ->
                {
                    int minecraftDataVersion = nbt.contains("DataVersion") ? nbt.getIntOr("DataVersion", Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue()) : Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();

                    return new SchematicSchema(0, minecraftDataVersion);
                }
            }
        }

        return null;
    }

    @Nullable
    public static LitematicaSchematic createFromFile(Path dir, String fileName)
    {
        return createFromFile(dir, fileName, FileType.LITEMATICA_SCHEMATIC);
    }

    @Nullable
    public static LitematicaSchematic createFromFile(Path dir, String fileName, FileType schematicType)
    {
		if (dir == null)
		{
			Litematica.LOGGER.error("LitematicaSchematic#createFromFile(): dir is NULL; please correct this when loading files.");
			return null;
		}

        Path file = fileFromDirAndName(dir, fileName, schematicType);
        LitematicaSchematic schematic = new LitematicaSchematic(file, schematicType);

        return schematic.readFromFile(schematicType) ? schematic : null;
    }

    public static class EntityInfo
    {
        public final Vec3 posVec;
        public final CompoundTag nbt;

        public EntityInfo(Vec3 posVec, CompoundTag nbt)
        {
            this.posVec = posVec;

            if (nbt.contains("SleepingX")) { nbt.putInt("SleepingX", Mth.floor(posVec.x)); }
            if (nbt.contains("SleepingY")) { nbt.putInt("SleepingY", Mth.floor(posVec.y)); }
            if (nbt.contains("SleepingZ")) { nbt.putInt("SleepingZ", Mth.floor(posVec.z)); }

            this.nbt = nbt;
        }

        public Vec3 toVanilla()
        {
            return this.posVec;
        }
    }

    public static class SchematicSaveInfo
    {
        public final boolean visibleOnly;
        public final boolean includeSupportBlocks;
        public final boolean ignoreEntities;
        public final boolean fromSchematicWorld;

        public SchematicSaveInfo(boolean visibleOnly,
                                 boolean ignoreEntities)
        {
            this (visibleOnly, false, ignoreEntities, false);
        }

        public SchematicSaveInfo(boolean visibleOnly,
                                 boolean includeSupportBlocks,
                                 boolean ignoreEntities,
                                 boolean fromSchematicWorld)
        {
            this.visibleOnly = visibleOnly;
            this.includeSupportBlocks = includeSupportBlocks;
            this.ignoreEntities = ignoreEntities;
            this.fromSchematicWorld = fromSchematicWorld;
        }
    }

    /**
     * FOR DEBUGGING PURPOSES ONLY
     *
     * @return ()
     */
    @Override
    public String toString()
    {
        CompoundTag nbt = new CompoundTag();
        CompoundTag list = new CompoundTag();

        if (this.schematicFile != null)
        {
            nbt.putString("FileName", this.schematicFile.toAbsolutePath().toString());
        }
        if (this.schematicType != null)
        {
            nbt.putString("FileType", this.schematicType.name());
        }

        nbt.putInt("TotalBlocksRead", this.totalBlocksReadFromWorld);

        if (this.blockContainers != null)
        {
            for (String key : this.blockContainers.keySet())
            {
                CompoundTag sub = new CompoundTag();

                if (this.blockContainers.get(key) != null)
                {
                    sub.store("BlockStateContainerSize", Vec3i.CODEC, this.blockContainers.get(key).getSize());
                }
                else
                {
                    sub.store("BlockStateContainerSize", Vec3i.CODEC, Vec3i.ZERO);
                }
                if (this.subRegionPositions.get(key) != null)
                {
                    sub.store("SubRegionPositions", BlockPos.CODEC, this.subRegionPositions.get(key));
                }
                else
                {
                    sub.store("SubRegionPositions", BlockPos.CODEC, BlockPos.ZERO);
                }
                if (this.subRegionSizes.get(key) != null)
                {
                    sub.store("SubRegionSizes", BlockPos.CODEC, this.subRegionSizes.get(key));
                }
                else
                {
                    sub.store("SubRegionSizes", BlockPos.CODEC, BlockPos.ZERO);
                }

                if (this.tileEntities.get(key) != null)
                {
                    sub.putInt("TileEntityCount", this.tileEntities.get(key).size());
                }
                if (this.entities.get(key) != null)
                {
                    sub.putInt("EntityCount", this.entities.get(key).size());
                }
                if (this.pendingBlockTicks.get(key) != null)
                {
                    sub.putInt("PendingBlockTicks", this.pendingBlockTicks.get(key).size());
                }
                if (this.pendingFluidTicks.get(key) != null)
                {
                    sub.putInt("PendingFluidTicks", this.pendingFluidTicks.get(key).size());
                }

                list.store(key, CompoundTag.CODEC, sub);
            }
        }

        nbt.store("Regions", CompoundTag.CODEC, list);
        nbt.store("Metadata", CompoundTag.CODEC, this.metadata.writeToNbtExtra());

        return "LitematicaSchematic[" + nbt.toString() + "]";
    }
}
