package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jspecify.annotations.NonNull;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

public class ChunkCacheSchematic implements BlockAndTintGetter, LightChunkGetter
{
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    protected final Level world;
    protected final ClientLevel worldClient;
    protected int chunkStartX;
    protected int chunkStartZ;
    protected LevelChunk[][] chunkArray;
    protected boolean empty;

    public ChunkCacheSchematic(@Nonnull Level worldIn, @Nonnull ClientLevel clientWorld, @Nonnull BlockPos pos, int expand)
    {
        this.world = worldIn;
        //this.lightingProvider = new FakeLightingProvider(this);

        this.worldClient = clientWorld;
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        this.chunkStartX = (pos.getX() - expand) >> 4;
        this.chunkStartZ = (pos.getZ() - expand) >> 4;
        int chunkEndX = (pos.getX() + expand + 15) >> 4;
        int chunkEndZ = (pos.getZ() + expand + 15) >> 4;
        this.chunkArray = new LevelChunk[chunkEndX - this.chunkStartX + 1][chunkEndZ - this.chunkStartZ + 1];
        this.empty = true;

        for (int cx = this.chunkStartX; cx <= chunkEndX; ++cx)
        {
            for (int cz = this.chunkStartZ; cz <= chunkEndZ; ++cz)
            {
                LevelChunk chunk = worldIn.getChunk(cx, cz);
                this.chunkArray[cx - this.chunkStartX][cz - this.chunkStartZ] = chunk;

                if (cx == chunkX && cz == chunkZ &&
                    chunk.isYSpaceEmpty(worldIn.getMinY(), worldIn.getMaxY() - 1) == false)
                {
                    this.empty = false;
                }
            }
        }
    }

    @Override
    public @Nonnull BlockGetter getLevel()
    {
        return this.world;
    }

    @Override
    public LightChunk getChunkForLighting(int chunkX, int chunkZ)
    {
        //return null; // TODO 1.17 this shouldn't be needed since the lighting provider does nothing
        return this.worldClient.getChunk(chunkX, chunkZ);
    }

    public boolean isEmpty()
    {
        return this.empty;
    }

    @Override
    public @Nonnull BlockState getBlockState(BlockPos pos)
    {
        int cx = (pos.getX() >> 4) - this.chunkStartX;
        int cz = (pos.getZ() >> 4) - this.chunkStartZ;

        if (cx >= 0 && cx < this.chunkArray.length &&
            cz >= 0 && cz < this.chunkArray[cx].length)
        {
            ChunkAccess chunk = this.chunkArray[cx][cz];

            if (chunk != null)
            {
                return chunk.getBlockState(pos);
            }
        }

        return AIR;
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(@NonNull BlockPos pos)
    {
        return this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos, LevelChunk.EntityCreationType type)
    {
        int i = (pos.getX() >> 4) - this.chunkStartX;
        int j = (pos.getZ() >> 4) - this.chunkStartZ;

        return this.chunkArray[i][j].getBlockEntity(pos, type);
    }

    @Override
    public @Nonnull FluidState getFluidState(@Nonnull BlockPos pos)
    {
        // TODO change when fluids become separate
        return this.getBlockState(pos).getFluidState();
    }

    @Override
    public @Nonnull LevelLightEngine getLightEngine()
    {
        //return this.lightingProvider;
        return this.world.getLightEngine();
    }

    @Override
    public int getBlockTint(@Nonnull BlockPos pos, ColorResolver colorResolver)
    {
        return colorResolver.getColor(this.worldClient.getBiome(pos).value(), pos.getX(), pos.getZ());
    }

    @Override
    public float getShade(@Nonnull Direction direction, boolean bl)
    {
        return this.worldClient.getShade(direction, bl); // AO brightness on face
    }

    @Override
    public int getHeight()
    {
        return this.world.getHeight();
    }

    @Override
    public int getMinY()
    {
        return this.world.getMinY();
    }
}
