package fi.dy.masa.litematica.world;

import javax.annotation.Nonnull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

    private final long timeCreated;
    private final int bottomY;
    private final int topY;
    private boolean isEmpty = true;
    private ChunkSchematicState state;

    public ChunkSchematic(Level worldIn, ChunkPos pos)
    {
        super(worldIn, pos);

        this.state = ChunkSchematicState.NEW;
        this.timeCreated = worldIn.getGameTime();
        this.bottomY = worldIn.getMinY();
        this.topY = worldIn.getMaxY();
    }

    public void setState(ChunkSchematicState state)
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

//        if (!this.getState().atLeast(ChunkSchematicState.PROTO))
//        {
//            return stateOld;
//        }

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
                    BlockEntity te = this.createBlockEntity(pos);

                    if (te == null)
                    {
                        te = ((EntityBlock) blockNew).newBlockEntity(pos, newState);

                        if (te != null)
                        {
//                            this.getLevel().getChunkAt(pos).setBlockEntity(te);
                            this.setBlockEntity(te);
                        }
                    }
                }

//                this.isUnsaved();

                return stateOld;
            }
        }
    }

    @Nullable
    public BlockEntity createBlockEntity(BlockPos pos)
    {
        BlockState state = this.getBlockState(pos);

        return !state.hasBlockEntity()
               ? null
               : ((EntityBlock) state.getBlock()).newBlockEntity(pos, state
        );
    }

    @Override
    public void setBlockEntity(@NonNull BlockEntity te)
    {
        BlockPos pos = te.getBlockPos();
        BlockState currState = this.getBlockState(pos);

        if (!currState.hasBlockEntity())
        {
            Litematica.LOGGER.error("setBlockEntity: Can't set block entity at pos '{}', because the existing state '{}' doesn't accept block entities",
                                    pos.toShortString(), currState.toString());
            return;
        }
        else
        {
            BlockState teState = te.getBlockState();

            if (!teState.equals(currState) &&
                te.getType().isValid(currState))
            {
                if (!currState.getBlock().equals(teState.getBlock()))
                {
                    Litematica.LOGGER.error("setBlockEntity: Can't set block entity at pos '{}', because the Tile Entities' Block '{}' doesn't match '{}'",
                                            pos.toShortString(), currState.getBlock().toString(), teState.getBlock().toString());
                    return;
                }

                te.setBlockState(currState);
            }

            te.setLevel(this.getLevel());
            te.clearRemoved();

            BlockEntity oldTe = this.blockEntities.put(pos.immutable(), te);

            if (oldTe != null && oldTe != te)
            {
                oldTe.setRemoved();
            }
        }
    }

    public AABB getBoundingBox()
    {
        final ChunkPos pos = this.getPos();
        return new AABB(pos.getMinBlockX(), this.getMinY(), pos.getMinBlockZ(), pos.getMaxBlockX(), this.getMaxY(), pos.getMaxBlockZ());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addEntity(@Nonnull Entity entity)
    {
        this.getLevel().addFreshEntity(entity);
    }

    public int getTileEntityCount()
    {
        return this.blockEntities.size();
    }

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
