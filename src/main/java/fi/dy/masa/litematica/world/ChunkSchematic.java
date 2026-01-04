package fi.dy.masa.litematica.world;

import javax.annotation.Nonnull;
import org.jspecify.annotations.NonNull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;

import fi.dy.masa.litematica.Litematica;

public class ChunkSchematic extends LevelChunk
{
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

//    private final List<Entity> entityList = new ArrayList<>();
    private final long timeCreated;
    private final int bottomY;
    private final int topY;
//    private int entityCount;
    private boolean isEmpty = true;
    private ChunkSchematicState state;

    public ChunkSchematic(Level worldIn, ChunkPos pos)
    {
        super(worldIn, pos);

        this.state = ChunkSchematicState.NEW;
        this.timeCreated = worldIn.getGameTime();
        this.bottomY = worldIn.getMinY();
        this.topY = worldIn.getMaxY();
//        this.entityCount = 0;
    }

    protected void setState(ChunkSchematicState state)
    {
        this.state = state;
    }

    public ChunkSchematicState getState()
    {
        return this.state;
    }

    @Override
    public @Nonnull BlockState getBlockState(BlockPos pos)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int cy = this.getSectionIndex(y);
//        y &= 0xF;

        LevelChunkSection[] sections = this.getSections();

        if (cy >= 0 && cy < sections.length)
        {
            LevelChunkSection chunkSection = sections[cy];

            if (!chunkSection.hasOnlyAir())
            {
                return chunkSection.getBlockState(x & 0xF, y & 0xF, z & 0xF);
            }
         }

         return AIR;
    }

    @Override
    public BlockState setBlockState(@Nonnull BlockPos pos, @Nonnull BlockState newState, @Block.UpdateFlags int flags)
    {
        BlockState stateOld = this.getBlockState(pos);
        int y = pos.getY();

        if (stateOld == newState || y >= this.topY || y < this.bottomY)
        {
            return null;
        }
        else
        {
            int x = pos.getX() & 15;
            int z = pos.getZ() & 15;
            int cy = this.getSectionIndex(y);

            Block blockNew = newState.getBlock();
            Block blockOld = stateOld.getBlock();
            LevelChunkSection section = this.getSections()[cy];

            if (section.hasOnlyAir() && newState.isAir())
            {
                return null;
            }

            y &= 0xF;

            if (newState.isAir() == false)
            {
                this.isEmpty = false;
            }

            section.setBlockState(x, y, z, newState);

            if (blockOld != blockNew)
            {
                this.getLevel().removeBlockEntity(pos);
            }

            if (section.getBlockState(x, y, z).getBlock() != blockNew)
            {
                return null;
            }
            else
            {
                if (newState.hasBlockEntity() && blockNew instanceof EntityBlock)
                {
                    BlockEntity te = this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);

                    if (te == null)
                    {
                        te = ((EntityBlock) blockNew).newBlockEntity(pos, newState);

                        if (te != null)
                        {
                            this.getLevel().getChunkAt(pos).setBlockEntity(te);
                        }
                    }
                }

//                this.isUnsaved();

                return stateOld;
            }
        }
    }

    public AABB getBoundingBox()
    {
        final ChunkPos pos = this.getPos();
//        AABB bb = new AABB(pos.getMinBlockX(), this.getMinY(), pos.getMinBlockZ(), pos.getMaxBlockX(), this.getMaxY(), pos.getMaxBlockZ());
//        Litematica.debugLog("ChunkSchematic#getBoundingBox(): --> {}", bb.toString());
        return new AABB(pos.getMinBlockX(), this.getMinY(), pos.getMinBlockZ(), pos.getMaxBlockX(), this.getMaxY(), pos.getMaxBlockZ());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addEntity(@Nonnull Entity entity)
    {
//        this.entityList.forEach(
//                (ent ->
//                {
//                    if (ent.getUUID() == entity.getUUID() || ent.getId() == entity.getId())
//                    {
//                        return;
//                    }
//                })
//        );
//
//        this.entityList.add(entity);
//        ++this.entityCount;

        this.getLevel().addFreshEntity(entity);
    }

    // todo --> MOVED TO EntityLookup
//    public List<Entity> getEntityList()
//    {
//        return this.entityList;
//    }

//    public int getEntityCount()
//    {
//        return this.entityCount;
//    }

    public int getTileEntityCount()
    {
        return this.blockEntities.size();
    }

//    protected void clearEntities()
//    {
//        this.entityList.clear();
//        this.entityCount = 0;
//    }

    public long getTimeCreated()
    {
        return this.timeCreated;
    }

    @Override
    public boolean isEmpty()
    {
        return this.isEmpty;
    }
}
