package fi.dy.masa.litematica.util;

import javax.annotation.Nullable;

import fi.dy.masa.litematica.render.schematic.ChunkCacheSchematic;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightSourceView;
import net.minecraft.world.chunk.light.LightingProvider;

public class ChunkCacheSchematicIgnoreWrapper implements BlockRenderView, ChunkProvider
{
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    protected final ChunkCacheSchematic chunkCacheSchematic;
    protected final IgnoreBlockRegistry ignoreBlockRegistry;

    public ChunkCacheSchematicIgnoreWrapper(ChunkCacheSchematic chunkCacheSchematic, IgnoreBlockRegistry ignoreBlockRegistry)
    {
        this.chunkCacheSchematic = chunkCacheSchematic;
        this.ignoreBlockRegistry = ignoreBlockRegistry;
    }

    @Override
    public BlockView getWorld()
    {
        return chunkCacheSchematic.getWorld();
    }

    @Override
    public LightSourceView getChunk(int chunkX, int chunkZ)
    {
        return chunkCacheSchematic.getChunk(chunkX, chunkZ);
    }

    public boolean isEmpty()
    {
        return chunkCacheSchematic.isEmpty();
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        BlockState stateSchematic = chunkCacheSchematic.getBlockState(pos);
        if(ignoreBlockRegistry.hasBlock(stateSchematic.getBlock())) {
            return AIR;
        } else {
            return stateSchematic;
        }
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos)
    {
        return chunkCacheSchematic.getBlockEntity(pos);
    }

    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos, WorldChunk.CreationType type)
    {
        return chunkCacheSchematic.getBlockEntity(pos, type);
    }


    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return chunkCacheSchematic.getFluidState(pos);
    }

    @Override
    public LightingProvider getLightingProvider()
    {
        return chunkCacheSchematic.getLightingProvider();
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver)
    {
        return getColor(pos, colorResolver);
    }

    @Override
    public float getBrightness(Direction direction, boolean bl)
    {
        return chunkCacheSchematic.getBrightness(direction, bl);
    }

    @Override
    public int getHeight()
    {
        return chunkCacheSchematic.getHeight();
    }

    @Override
    public int getBottomY()
    {
        return chunkCacheSchematic.getBottomY();
    }
}
