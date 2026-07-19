package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.Schema;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.data.tag.util.DataFileUtils;
import fi.dy.masa.malilib.util.data.tag.util.DataTypeUtils;
import fi.dy.masa.malilib.util.nbt.NbtView;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.LitematicaSchematic.EntityInfo;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionFixers.IStateFixer;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;
import fi.dy.masa.litematica.schematic.conversion.SchematicConverter;
import fi.dy.masa.litematica.util.*;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.ChunkSchematicState;
import fi.dy.masa.litematica.world.WorldSchematic;

public class SchematicaSchematic
{
    public static final String FILE_EXTENSION = ".schematic";

    private final SchematicMetadata metadata = new SchematicMetadata();
    private final SchematicConverter converter;
    private final BlockState[] palette = new BlockState[65536];
    private LitematicaBlockStateContainer blocks;
    private final Map<BlockPos, CompoundData> tiles = new HashMap<>();
    private final List<CompoundData> entities = new ArrayList<>();
    private Vec3i size = Vec3i.ZERO;
    private String fileName;
    private IdentityHashMap<BlockState, IStateFixer> postProcessingFilter;
    private boolean needsConversionPostProcessing;

    protected SchematicaSchematic()
    {
        this.converter = SchematicConverter.createForSchematica();
    }

    public SchematicMetadata getMetadata()
    {
        return this.metadata;
    }

    public Vec3i getSize()
    {
        return this.size;
    }

    public Map<BlockPos, CompoundData> getTiles()
    {
        return this.tiles;
    }

    public List<EntityInfo> getEntities()
    {
        List<EntityInfo> entityList = new ArrayList<>();
        final int size = this.entities.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundData entityData = this.entities.get(i);
//            Vec3 posVec = NbtUtils.getVec3dCodec(entityData, "Pos");
            Vec3 posVec = DataTypeUtils.getVec3dCodec(entityData, "Pos");

            if (posVec != null && entityData.isEmpty() == false)
            {
                entityList.add(new EntityInfo(posVec, entityData));
            }
        }

        return entityList;
    }

    public void placeSchematicToWorld(Level world, BlockPos posStart, StructurePlaceSettings placement, int setBlockStateFlags)
    {
        final int width = this.size.getX();
        final int height = this.size.getY();
        final int length = this.size.getZ();
        final int numBlocks = width * height * length;

        if (this.blocks != null && numBlocks > 0 && this.blocks.getSize().equals(this.size))
        {
            final Rotation rotation = placement.getRotation();
            final Mirror mirror = placement.getMirror();

            // Place blocks and read any TileEntity data
            for (int y = 0; y < height; ++y)
            {
                for (int z = 0; z < length; ++z)
                {
                    for (int x = 0; x < width; ++x)
                    {
                        BlockState state = this.blocks.get(x, y, z);
                        BlockPos pos = new BlockPos(x, y, z);
                        CompoundData teNBT = this.tiles.get(pos);

                        pos = StructureTemplate.calculateRelativePosition(placement, pos).offset(posStart);

                        state = state.mirror(mirror);
                        state = state.rotate(rotation);

                        if (teNBT != null)
                        {
                            BlockEntity te = world.getBlockEntity(pos);

                            if (te != null)
                            {
                                if (te instanceof Container)
                                {
                                    ((Container) te).clearContent();
                                }

                                world.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 0x14);
                            }
                        }

                        if (world.setBlock(pos, state, setBlockStateFlags) && teNBT != null)
                        {
                            BlockEntity te = world.getBlockEntity(pos);

                            if (te != null)
                            {
                                teNBT.putInt("x", pos.getX());
                                teNBT.putInt("y", pos.getY());
                                teNBT.putInt("z", pos.getZ());

                                try
                                {
                                    NbtView view = NbtView.getReader(teNBT, world.registryAccess());
                                    te.loadWithComponents(view.getReader());
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

            if ((setBlockStateFlags & 0x01) != 0)
            {
                // Update blocks
                for (int y = 0; y < height; ++y)
                {
                    for (int z = 0; z < length; ++z)
                    {
                        for (int x = 0; x < width; ++x)
                        {
                            BlockPos pos = new BlockPos(x, y, z);
                            CompoundData teNBT = this.tiles.get(pos);

                            pos = StructureTemplate.calculateRelativePosition(placement, pos).offset(posStart);
                            world.updateNeighborsAt(pos, world.getBlockState(pos).getBlock());

                            if (teNBT != null)
                            {
                                BlockEntity te = world.getBlockEntity(pos);

                                if (te != null)
                                {
                                    te.setChanged();
                                }
                            }
                        }
                    }
                }
            }

            if (placement.isIgnoreEntities() == false)
            {
                this.addEntitiesToWorld(world, posStart, placement);
            }
        }
    }

    public void placeSchematicDirectlyToChunks(WorldSchematic world, BlockPos posStart, StructurePlaceSettings placement)
    {
        final int width = this.size.getX();
        final int height = this.size.getY();
        final int length = this.size.getZ();
        final int numBlocks = width * height * length;
        BlockPos posEnd = posStart.offset(this.size).offset(-1, -1, -1);
        //BlockPos posEnd = Template.transformedBlockPos(placement, (new BlockPos(this.size)).add(-1, -1, -1)).add(posStart);

        if (this.blocks != null && numBlocks > 0 && this.blocks.getSize().equals(this.size) &&
            PositionUtils.arePositionsWithinWorld(world, posStart, posEnd))
        {
            final BlockPos posMin = PositionUtils.getMinCorner(posStart, posEnd);
            final BlockPos posMax = PositionUtils.getMaxCorner(posStart, posEnd);
            final int cxStart = posMin.getX() >> 4;
            final int czStart = posMin.getZ() >> 4;
            final int cxEnd = posMax.getX() >> 4;
            final int czEnd = posMax.getZ() >> 4;
            BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

            for (int cz = czStart; cz <= czEnd; ++cz)
            {
                for (int cx = cxStart; cx <= cxEnd; ++cx)
                {
                    final int xMinChunk = Math.max(cx << 4, posMin.getX());
                    final int zMinChunk = Math.max(cz << 4, posMin.getZ());
                    final int xMaxChunk = Math.min((cx << 4) + 15, posMax.getX());
                    final int zMaxChunk = Math.min((cz << 4) + 15, posMax.getZ());
                    ChunkSchematic chunk = world.getChunk(cx, cz);

                    if (chunk == null)
                    {
                        continue;
                    }

                    for (int y = posMin.getY(), ySrc = 0; ySrc < height; ++y, ++ySrc)
                    {
                        for (int z = zMinChunk, zSrc = zMinChunk - posStart.getZ(); z <= zMaxChunk; ++z, ++zSrc)
                        {
                            for (int x = xMinChunk, xSrc = xMinChunk - posStart.getX(); x <= xMaxChunk; ++x, ++xSrc)
                            {
                                BlockState state = this.blocks.get(xSrc, ySrc, zSrc);

                                posMutable.set(xSrc, ySrc, zSrc);
                                CompoundData teNBT = this.tiles.get(posMutable);

                                // TODO The rotations need to be transformed back to get the correct source position in the schematic...
                                /*
                                pos = Template.transformedBlockPos(placement, pos).add(posStart);

                                state = state.withMirror(mirror);
                                state = state.withRotation(rotation);
                                */

                                BlockPos pos = new BlockPos(x, y, z);

                                if (teNBT != null)
                                {
                                    BlockEntity te = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);

                                    if (te != null)
                                    {
                                        if (te instanceof Container)
                                        {
                                            ((Container) te).clearContent();
                                        }

                                        world.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 0x14);
                                    }
                                }

                                chunk.setBlockState(pos, state, 3);
//                                world.setBlock(pos, state, 0x12);

                                if (teNBT != null)
                                {
                                    BlockEntity te = chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);

                                    if (te != null)
                                    {
                                        teNBT.putInt("x", pos.getX());
                                        teNBT.putInt("y", pos.getY());
                                        teNBT.putInt("z", pos.getZ());

                                        try
                                        {
                                            NbtView view = NbtView.getReader(teNBT, world.registryAccess());
                                            te.loadWithComponents(view.getReader());
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

                    if (!chunk.getState().atLeast(ChunkSchematicState.FILLED))
                    {
                        chunk.setState(ChunkSchematicState.FILLED);
                    }
                }
            }

            if (placement.isIgnoreEntities() == false)
            {
                this.addEntitiesToWorld(world, posStart, placement);
            }
        }
    }

    private void addEntitiesToWorld(Level world, BlockPos posStart, StructurePlaceSettings placement)
    {
        Mirror mirror = placement.getMirror();
        Rotation rotation = placement.getRotation();

        for (CompoundData tag : this.entities)
        {
//            Vec3 relativePos = NbtUtils.getVec3dCodec(tag, "Pos");
            Vec3 relativePos = DataTypeUtils.getVec3dCodec(tag, "Pos");

            if (relativePos != null)
            {
                Vec3 transformedRelativePos = PositionUtils.getTransformedPosition(relativePos, mirror, rotation);
                Vec3 realPos = transformedRelativePos.add(posStart.getX(), posStart.getY(), posStart.getZ());
                Entity entity = EntityUtils.createEntityAndPassengersFromData(tag, world);

                if (entity != null)
                {
                    float rotationYaw = entity.mirror(mirror);
                    rotationYaw = rotationYaw + (entity.getYRot() - entity.rotate(rotation));
                    entity.snapTo(realPos.x, realPos.y, realPos.z, rotationYaw, entity.getXRot());
                    EntityUtils.spawnEntityAndPassengersInWorld(entity, world);
                }
            }
        }
    }

    public Map<BlockPos, String> getDataStructureBlocks(BlockPos posStart, StructurePlaceSettings placement)
    {
        Map<BlockPos, String> map = new HashMap<>();

        for (Map.Entry<BlockPos, CompoundData> entry : this.tiles.entrySet())
        {
            CompoundData tag = entry.getValue();

            if (tag.getStringOrDefault("id", "?").equals("minecraft:structure_block") &&
                StructureMode.valueOf(tag.getStringOrDefault("mode", "?")) == StructureMode.DATA)
            {
                BlockPos pos = entry.getKey();
                pos = StructureTemplate.calculateRelativePosition(placement, pos).offset(posStart);
                map.put(pos, tag.getStringOrDefault("metadata", "?"));
            }
        }

        return map;
    }

    private void readBlocksFromWorld(Level world, BlockPos posStart, BlockPos size)
    {
        final int startX = posStart.getX();
        final int startY = posStart.getY();
        final int startZ = posStart.getZ();
        final int endX = startX + size.getX();
        final int endY = startY + size.getY();
        final int endZ = startZ + size.getZ();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);

        this.blocks = new LitematicaBlockStateContainer(size.getX(), size.getY(), size.getZ());
        this.tiles.clear();
        this.size = size;

        for (int y = startY; y < endY; ++y)
        {
            for (int z = startZ; z < endZ; ++z)
            {
                for (int x = startX; x < endX; ++x)
                {
                    int relX = x - startX;
                    int relY = y - startY;
                    int relZ = z - startZ;

                    posMutable.set(x, y, z);
                    BlockState state = world.getBlockState(posMutable);
                    this.blocks.set(relX, relY, relZ, state);

                    BlockEntity te = world.getBlockEntity(posMutable);

                    if (te != null)
                    {
                        try
                        {
                            CompoundData nbt = DataConverterNbt.fromVanillaCompound(te.saveWithFullMetadata(world.registryAccess()));
                            BlockPos pos = new BlockPos(relX, relY, relZ);
//                            NbtUtils.writeBlockPosToTag(pos, nbt);
                            DataTypeUtils.writeBlockPosToTag(pos, nbt);

                            this.tiles.put(pos, nbt);
                        }
                        catch (Exception e)
                        {
                            Litematica.LOGGER.warn("SchematicaSchematic: Exception while trying to store TileEntity data for block '{}' at {}",
                                                   state, posMutable.toString(), e);
                        }
                    }
                }
            }
        }
    }

    private void readEntitiesFromWorld(Level world, BlockPos posStart, BlockPos size)
    {
        this.entities.clear();
        List<Entity> entities = world.getEntities((Entity) null, PositionUtils.createEnclosingAABB(posStart, posStart.offset(size)), EntityUtils.NOT_PLAYER);

        for (Entity entity : entities)
        {
            if (entity instanceof EnderDragonPart) { continue; }
            NbtView view = NbtView.getWriter(world.registryAccess());
            entity.saveWithoutId(view.getWriter());
            CompoundData nbt = view.readData();
            Identifier id = EntityType.getKey(entity.getType());

            if (nbt != null && id != null)
            {
                Vec3 pos = new Vec3(entity.getX() - posStart.getX(), entity.getY() - posStart.getY(), entity.getZ() - posStart.getZ());

                nbt.putString("id", id.toString());
//                NbtUtils.putVec3dCodec(nbt, pos, "Pos");
                DataTypeUtils.putVec3dCodec(nbt, pos, "Pos");

                this.entities.add(nbt);
            }
        }
    }

    public static SchematicaSchematic createFromWorld(Level world, BlockPos posStart, BlockPos size, boolean ignoreEntities)
    {
        SchematicaSchematic schematic = new SchematicaSchematic();

        schematic.readBlocksFromWorld(world, posStart, size);

        if (ignoreEntities == false)
        {
            schematic.readEntitiesFromWorld(world, posStart, size);
        }

        return schematic;
    }

    @Deprecated
    @Nullable
    public static SchematicaSchematic createFromFile(File file)
    {
        return createFromFile(file.toPath());
    }

    @Nullable
    public static SchematicaSchematic createFromFile(Path file)
    {
        SchematicaSchematic schematic = new SchematicaSchematic();

        if (schematic.readFromFile(file))
        {
            schematic.metadata.setName(file.getFileName().toString());
            return schematic;
        }

        return null;
    }

    /**
     * @deprecated use readFromData()
     */
    @Deprecated(forRemoval = true)
    public boolean readFromNBT(CompoundTag nbt)
    {
        return this.readFromData(DataConverterNbt.fromVanillaCompound(nbt));
    }

    public boolean readFromData(CompoundData nbt)
    {
        if (this.readBlocksFromData(nbt))
        {
            this.readEntitiesFromData(nbt);
            this.readTileEntitiesFromData(nbt);

            try
            {
                this.postProcessBlocks();
            }
            catch (Exception e)
            {
                Litematica.LOGGER.error("SchematicaSchematic: Exception while post-processing blocks for '{}'", this.fileName, e);
            }

            return true;
        }
        else
        {
            Litematica.LOGGER.error("SchematicaSchematic: Missing block data in the schematic '{}'", this.fileName);
            return false;
        }
    }

    /**
     * @deprecated use readPaletteFromData()
     */
    @Deprecated(forRemoval = true)
    private boolean readPaletteFromNBT(CompoundTag nbt)
    {
        return this.readPaletteFromData(DataConverterNbt.fromVanillaCompound(nbt));
    }

    private boolean readPaletteFromData(CompoundData nbt)
    {
        Arrays.fill(this.palette, Blocks.AIR.defaultBlockState());

        // Schematica palette
        if (nbt.contains("SchematicaMapping", Constants.NBT.TAG_COMPOUND))
        {
            CompoundData tag = nbt.getCompound("SchematicaMapping");
            Set<String> keys = tag.getKeys();

            for (String key : keys)
            {
                int id = tag.getShortOrDefault(key, (short) -1);

                if (id < 0 || id >= 4096)
                {
                    String str = String.format("SchematicaSchematic: Invalid ID '%d' in SchematicaMapping for block '%s', range: 0 - 4095", id, key);
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    Litematica.LOGGER.warn(str);
                    return false;
                }

                if (this.converter.getConvertedStatesForBlock(id, key, this.palette) == false)
                {
                    String str = String.format("SchematicaSchematic: Missing/non-existing block '%s' in SchematicaMapping", key);
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    Litematica.LOGGER.warn(str);
                }
            }
        }
        // MCEdit2 palette
        else if (nbt.contains("BlockIDs", Constants.NBT.TAG_COMPOUND))
        {
            CompoundData tag = nbt.getCompound("BlockIDs");
            Set<String> keys = tag.getKeys();

            for (String idStr : keys)
            {
                String key = tag.getStringOrDefault(idStr, "");
                int id;

                try
                {
                    id = Integer.parseInt(idStr);
                }
                catch (NumberFormatException e)
                {
                    String str = String.format("SchematicaSchematic: Invalid ID '%d' (not a number) in MCEdit2 palette for block '%s'", idStr, key);
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    Litematica.LOGGER.warn(str);
                    return false;
                }

                if (id < 0 || id >= 4096)
                {
                    String str = String.format("SchematicaSchematic: Invalid ID '%d' in MCEdit2 palette for block '%s', range: 0 - 4095", id, key);
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    Litematica.LOGGER.warn(str);
                    return false;
                }

                if (this.converter.getConvertedStatesForBlock(id, key, this.palette) == false)
                {
                    String str = String.format("SchematicaSchematic: Missing/non-existing block '%s' in MCEdit2 palette", key);
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    Litematica.LOGGER.warn(str);
                }
            }
        }
        // No palette, use old vanilla IDs
        else
        {
            this.converter.getVanillaBlockPalette(this.palette);
        }

        if (this.converter.createPostProcessStateFilter(this.palette))
        {
            this.postProcessingFilter = this.converter.getPostProcessStateFilter();
            this.needsConversionPostProcessing = true;
        }

        return true;
    }

    /**
     * @deprecated use readBlocksFromDataMetadataOnly()
     */
    @Deprecated(forRemoval = true)
    protected boolean readBlocksFromNBTMetadataOnly(Path file, CompoundTag nbt)
    {
        return this.readBlocksFromDataMetadataOnly(file, DataConverterNbt.fromVanillaCompound(nbt));
    }

    protected boolean readBlocksFromDataMetadataOnly(Path file, CompoundData nbt)
    {
        if (nbt.contains("Blocks", Constants.NBT.TAG_BYTE_ARRAY) == false ||
            nbt.contains("Data", Constants.NBT.TAG_BYTE_ARRAY) == false ||
            nbt.contains("Width", Constants.NBT.TAG_ANY_NUMERIC) == false ||
            nbt.contains("Height", Constants.NBT.TAG_ANY_NUMERIC) == false ||
            nbt.contains("Length", Constants.NBT.TAG_ANY_NUMERIC) == false)
        {
            return false;
        }

        this.fileName = file.getFileName().toString();

        // This method was implemented based on
        // https://minecraft.gamepedia.com/Schematic_file_format
        // as it was on 2018-04-18.

        final int sizeX = nbt.getShortOrDefault("Width", (short) 0);
        final int sizeY = nbt.getShortOrDefault("Height", (short) 0);
        final int sizeZ = nbt.getShortOrDefault("Length", (short) 0);
        final byte[] blockIdsByte = nbt.getByteArrayOrDefault("Blocks", new byte[0]);
        final byte[] metaArr = nbt.getByteArrayOrDefault("Data", new byte[0]);
        final int numBlocks = blockIdsByte.length;
        final int layerSize = sizeX * sizeZ;

        if (numBlocks != (sizeX * sizeY * sizeZ))
        {
            String str = String.format("SchematicaSchematic: Mismatched block array size compared to the width/height/length,\nblocks: %d, W x H x L: %d x %d x %d", numBlocks, sizeX, sizeY, sizeZ);
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
            return false;
        }

        if (numBlocks != metaArr.length)
        {
            String str = String.format("SchematicaSchematic: Mismatched block ID and metadata array sizes, blocks: %d, meta: %d", numBlocks, metaArr.length);
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
            return false;
        }

        this.size = new Vec3i(sizeX, sizeY, sizeZ);
        this.metadata.setEnclosingSize(this.size);
        this.metadata.setTotalBlocks(numBlocks);
        this.metadata.setTotalVolume(sizeX * sizeY * sizeZ);
        this.metadata.setRegionCount(1);
        this.metadata.setFileType(FileType.SCHEMATICA_SCHEMATIC);

        return true;
    }

    /**
     * @deprecated use readBlocksFromData()
     */
    @Deprecated(forRemoval = true)
    private boolean readBlocksFromNBT(CompoundTag nbt)
    {
        return this.readBlocksFromData(DataConverterNbt.fromVanillaCompound(nbt));
    }

    private boolean readBlocksFromData(CompoundData nbt)
    {
        if (nbt.contains("Blocks", Constants.NBT.TAG_BYTE_ARRAY) == false ||
            nbt.contains("Data", Constants.NBT.TAG_BYTE_ARRAY) == false ||
            nbt.contains("Width", Constants.NBT.TAG_ANY_NUMERIC) == false ||
            nbt.contains("Height", Constants.NBT.TAG_ANY_NUMERIC) == false ||
            nbt.contains("Length", Constants.NBT.TAG_ANY_NUMERIC) == false)
        {
            return false;
        }

        // This method was implemented based on
        // https://minecraft.gamepedia.com/Schematic_file_format
        // as it was on 2018-04-18.

        final int sizeX = nbt.getShortOrDefault("Width", (short) 0);
        final int sizeY = nbt.getShortOrDefault("Height", (short) 0);
        final int sizeZ = nbt.getShortOrDefault("Length", (short) 0);
        final byte[] blockIdsByte = nbt.getByteArrayOrDefault("Blocks", new byte[0]);
        final byte[] metaArr = nbt.getByteArrayOrDefault("Data", new byte[0]);
        final int numBlocks = blockIdsByte.length;
        final int layerSize = sizeX * sizeZ;

        if (numBlocks != (sizeX * sizeY * sizeZ))
        {
            String str = String.format("SchematicaSchematic: Mismatched block array size compared to the width/height/length,\nblocks: %d, W x H x L: %d x %d x %d", numBlocks, sizeX, sizeY, sizeZ);
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
            return false;
        }

        if (numBlocks != metaArr.length)
        {
            String str = String.format("SchematicaSchematic: Mismatched block ID and metadata array sizes, blocks: %d, meta: %d", numBlocks, metaArr.length);
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
            return false;
        }

        if (this.readPaletteFromData(nbt) == false)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "SchematicaSchematic: Failed to read the block palette");
            return false;
        }

        this.size = new Vec3i(sizeX, sizeY, sizeZ);
        this.blocks = new LitematicaBlockStateContainer(sizeX, sizeY, sizeZ);
        this.metadata.setEnclosingSize(this.size);
        this.metadata.setTotalBlocks(numBlocks);
        this.metadata.setTotalVolume(sizeX * sizeY * sizeZ);
        this.metadata.setRegionCount(1);
        this.metadata.setFileType(FileType.SCHEMATICA_SCHEMATIC);

        // Old Schematica format
        if (nbt.containsLenient("Add"))
        {
            // FIXME is this array 4 or 8 bits per block?
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "SchematicaSchematic: Old Schematica format detected, not currently implemented...");
            return false;
        }

        byte[] add = null;

        if (nbt.contains("AddBlocks", Constants.NBT.TAG_BYTE_ARRAY))
        {
            add = nbt.getByteArrayOrDefault("AddBlocks", new byte[0]);
            final int expectedAddLength = (int) Math.ceil((double) blockIdsByte.length / 2D);

            if (add.length != expectedAddLength)
            {
                String str = String.format("SchematicaSchematic: Add array size mismatch, blocks: %d, add: %d, expected add: %d", numBlocks, add.length, expectedAddLength);

                if (add.length < expectedAddLength)
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, str);
                    return false;
                }
                else
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, str);
                }
            }
        }

        final int loopMax;

        // Even number of blocks, we can handle two position (meaning one full add byte) at a time
        if ((numBlocks % 2) == 0)
        {
            loopMax = numBlocks - 1;
        }
        else
        {
            loopMax = numBlocks - 2;
        }

        int byteId;
        int bi, ai;
        BlockState state;

        // Handle two positions per iteration, ie. one full byte of the add array
        for (bi = 0, ai = 0; bi < loopMax; bi += 2, ai++)
        {
            final int addValue = add != null ? add[ai] : 0;

            byteId = blockIdsByte[bi    ] & 0xFF;
            state = this.palette[((addValue & 0xF0) << 8) | (byteId << 4) | metaArr[bi    ]];
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            this.blocks.set(x, y, z, state);

            x = (bi + 1) % sizeX;
            y = (bi + 1) / layerSize;
            z = ((bi + 1) % layerSize) / sizeX;
            byteId = blockIdsByte[bi + 1] & 0xFF;
            state = this.palette[((addValue & 0x0F) << 12) | (byteId << 4) | metaArr[bi + 1]];
            this.blocks.set(x, y, z, state);
        }

        // Odd number of blocks, handle the last position
        if ((numBlocks % 2) != 0)
        {
            final int addValue = add != null ? add[ai] : 0;
            byteId = blockIdsByte[bi    ] & 0xFF;
            state = this.palette[((addValue & 0xF0) << 8) | (byteId << 4) | metaArr[bi    ]];
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            this.blocks.set(x, y, z, state);
        }

        return true;
    }

    private void postProcessBlocks()
    {
        if (this.needsConversionPostProcessing)
        {
            SchematicConverter.postProcessBlocks(this.blocks, this.tiles, this.postProcessingFilter);
        }
    }

    /**
     * @deprecated use readEntitiesFromData()
     */
    @Deprecated(forRemoval = true)
    private void readEntitiesFromNBT(CompoundTag nbt)
    {
        this.readEntitiesFromData(DataConverterNbt.fromVanillaCompound(nbt));
    }

    private void readEntitiesFromData(CompoundData nbt)
    {
        this.entities.clear();
        ListData tagList = nbt.getList("Entities");
        int minecraftDataVersion = Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();
        Schema effective = DataFixerMode.getEffectiveSchema(minecraftDataVersion);

        this.metadata.setSchematicVersion(-1);
        this.metadata.setMinecraftDataVersion(minecraftDataVersion);
        this.metadata.setSchema();

        if (effective != null)
        {
            Litematica.LOGGER.info("SchematicaSchematic: executing Vanilla DataFixer for Entities DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION);
        }
        else
        {
            Litematica.LOGGER.warn("SchematicaSchematic: Effective Schema has been bypassed.  Not applying Vanilla Data Fixer for Entities DataVersion {}", minecraftDataVersion);
        }

        for (int i = 0; i < tagList.size(); ++i)
        {
            if (effective != null)
            {
                this.entities.add(SchematicConversionMaps.updateEntity(tagList.getCompoundAt(i), minecraftDataVersion));
            }
            else
            {
                this.entities.add(tagList.getCompoundAt(i));
            }
        }
    }

    /**
     * @deprecated use readTileEntitiesFromData()
     */
    @Deprecated(forRemoval = true)
    private void readTileEntitiesFromNBT(CompoundTag nbt)
    {
        this.readTileEntitiesFromData(DataConverterNbt.fromVanillaCompound(nbt));
    }

    private void readTileEntitiesFromData(CompoundData nbt)
    {
        this.tiles.clear();
        ListData tagList = nbt.getList("TileEntities");
        int minecraftDataVersion = Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue();
        Schema effective = DataFixerMode.getEffectiveSchema(minecraftDataVersion);

        if (effective != null)
        {
            Litematica.LOGGER.info("SchematicaSchematic: executing Vanilla DataFixer for Tile Entities DataVersion {} -> {}", minecraftDataVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION);
        }
        else
        {
            Litematica.LOGGER.warn("SchematicaSchematic: Effective Schema has been bypassed.  Not applying Vanilla Data Fixer for Tile Entities DataVersion {}", minecraftDataVersion);
        }

        for (int i = 0; i < tagList.size(); ++i)
        {
            CompoundData tag = tagList.getCompoundAt(i);
            BlockPos pos = new BlockPos(tag.getIntOrDefault("x", 0), tag.getIntOrDefault("y", 0), tag.getIntOrDefault("z", 0));
            Vec3i size = this.blocks.getSize();

            if (pos.getX() >= 0 && pos.getX() < size.getX() &&
                pos.getY() >= 0 && pos.getY() < size.getY() &&
                pos.getZ() >= 0 && pos.getZ() < size.getZ())
            {
                if (effective != null)
                {
                    this.tiles.put(pos, SchematicConversionMaps.updateBlockEntity(SchematicConversionMaps.checkForIdTag(tag), minecraftDataVersion));
                }
                else
                {
                    this.tiles.put(pos, SchematicConversionMaps.checkForIdTag(tag));
                }
            }
        }
    }

    public boolean readFromFile(Path file)
    {
        if (Files.exists(file) && Files.isRegularFile(file) && Files.isReadable(file))
        {
            this.fileName = file.getFileName().toString();

            try
            {
//                CompoundTag nbt = NbtUtils.readNbtFromFile(file);
//                return this.readFromNBT(nbt);
                CompoundData nbt = DataFileUtils.readCompoundDataFromNbtFile(file);
                return this.readFromData(nbt);
            }
            catch (Exception e)
            {
                Litematica.LOGGER.error("SchematicaSchematic: Failed to read Schematic data from file '{}'", file.toAbsolutePath());
            }
        }

        return false;
    }

    /*
    private void createPalette()
    {
        if (this.palette == null)
        {
            this.palette = new Block[4096];
            ILitematicaBlockStatePalette litematicaPalette = this.blocks.getPalette();
            final int numBlocks = litematicaPalette.getPaletteSize();

            for (int i = 0; i < numBlocks; ++i)
            {
                IBlockState state = litematicaPalette.getBlockState(i);
                Block block = state.getBlock();
                int id = Block.getIdFromBlock(block);

                if (id >= this.palette.length)
                {
                    throw new IllegalArgumentException(String.format("Block id %d for block '%s' is out of range, max allowed = %d!",
                            id, state, this.palette.length - 1));
                }

                this.palette[id] = block;
            }
        }
    }

    private void writePaletteToNBT(NBTTagCompound nbt)
    {
        NBTTagCompound tag = new NBTTagCompound();

        for (int i = 0; i < this.palette.length; ++i)
        {
            Block block = this.palette[i];

            if (block != null)
            {
                ResourceLocation rl = IRegistry.BLOCK.getKey(block);

                if (rl != null)
                {
                    tag.putShort(rl.toString(), (short) (i & 0xFFF));
                }
            }
        }

        nbt.put("SchematicaMapping", tag);
    }

    private void writeBlocksToNBT(NBTTagCompound nbt)
    {
        nbt.putShort("Width", (short) this.size.getX());
        nbt.putShort("Height", (short) this.size.getY());
        nbt.putShort("Length", (short) this.size.getZ());
        nbt.putString("Materials", "Alpha");

        final int numBlocks = this.size.getX() * this.size.getY() * this.size.getZ();
        final int loopMax = (int) Math.floor((double) numBlocks / 2D);
        final int addSize = (int) Math.ceil((double) numBlocks / 2D);
        final byte[] blockIdsArr = new byte[numBlocks];
        final byte[] metaArr = new byte[numBlocks];
        final byte[] addArr = new byte[addSize];
        final int sizeX = this.size.getX();
        final int sizeZ = this.size.getZ();
        final int layerSize = sizeX * sizeZ;
        int numAdd = 0;
        int bi, ai;

        for (bi = 0, ai = 0; ai < loopMax; bi += 2, ++ai)
        {
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            IBlockState state1 = this.blocks.get(x, y, z);

            x = (bi + 1) % sizeX;
            y = (bi + 1) / layerSize;
            z = ((bi + 1) % layerSize) / sizeX;
            IBlockState state2 = this.blocks.get(x, y, z);

            int id1 = Block.getIdFromBlock(state1.getBlock());
            int id2 = Block.getIdFromBlock(state2.getBlock());
            int add = ((id1 >>> 4) & 0xF0) | ((id2 >>> 8) & 0x0F);
            blockIdsArr[bi    ] = (byte) (id1 & 0xFF);
            blockIdsArr[bi + 1] = (byte) (id2 & 0xFF);

            if (add != 0)
            {
                addArr[ai] = (byte) add;
                numAdd++;
            }

            metaArr[bi    ] = (byte) state1.getBlock().getMetaFromState(state1);
            metaArr[bi + 1] = (byte) state2.getBlock().getMetaFromState(state2);
        }

        // Odd number of blocks, handle the last position
        if ((numBlocks % 2) != 0)
        {
            int x = bi % sizeX;
            int y = bi / layerSize;
            int z = (bi % layerSize) / sizeX;
            IBlockState state = this.blocks.get(x, y, z);

            int id = Block.getIdFromBlock(state.getBlock());
            int add = (id >>> 4) & 0xF0;
            blockIdsArr[bi] = (byte) (id & 0xFF);

            if (add != 0)
            {
                addArr[ai] = (byte) add;
                numAdd++;
            }

            metaArr[bi] = (byte) state.getBlock().getMetaFromState(state);
        }

        nbt.putByteArray("Blocks", blockIdsArr);
        nbt.putByteArray("Data", metaArr);

        if (numAdd > 0)
        {
            nbt.putByteArray("AddBlocks", addArr);
        }
    }

    private NBTTagCompound writeToNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        this.createPalette();
        this.writeBlocksToNBT(nbt);
        this.writePaletteToNBT(nbt);

        NBTTagList tagListTiles = new NBTTagList();
        NBTTagList tagListEntities = new NBTTagList();

        for (NBTTagCompound tag : this.entities)
        {
            tagListEntities.add(tag);
        }

        for (NBTTagCompound tag : this.tiles.values())
        {
            tagListTiles.add(tag);
        }

        nbt.put("TileEntities", tagListTiles);
        nbt.put("Entities", tagListEntities);

        return nbt;
    }

    public boolean writeToFile(File dir, String fileNameIn, boolean override, IStringConsumer feedback)
    {
        String fileName = fileNameIn;

        if (fileName.endsWith(FILE_EXTENSION) == false)
        {
            fileName = fileName + FILE_EXTENSION;
        }

        File fileSchematic = new File(dir, fileName);

        try
        {
            if (dir.exists() == false && dir.mkdirs() == false)
            {
                feedback.setString(StringUtils.translate("litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.getAbsolutePath()));
                return false;
            }

            if (override == false && fileSchematic.exists())
            {
                feedback.setString(StringUtils.translate("litematica.error.schematic_write_to_file_failed.exists", fileSchematic.getAbsolutePath()));
                return false;
            }

            FileOutputStream os = new FileOutputStream(fileSchematic);
            CompressedStreamTools.writeCompressed(this.writeToNBT(), os);
            os.close();

            return true;
        }
        catch (Exception e)
        {
            feedback.setString(StringUtils.translate("litematica.error.schematic_write_to_file_failed.exception", fileSchematic.getAbsolutePath()));
        }

        return false;
    }
    */
}
