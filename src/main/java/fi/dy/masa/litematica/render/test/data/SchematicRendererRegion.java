package fi.dy.masa.litematica.render.test.data;

import org.jetbrains.annotations.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.light.LightingProvider;
import fi.dy.masa.litematica.world.WorldSchematic;

public class SchematicRendererRegion implements BlockRenderView
{
    private final int chunkXOffset;
    private final int chunkZOffset;
    protected final SchematicRenderedChunk[] chunks;
    protected final WorldSchematic world;

    public SchematicRendererRegion(WorldSchematic world, int chunkX, int chunkZ, SchematicRenderedChunk[] chunks)
    {
        this.world = world;
        this.chunkXOffset = chunkX;
        this.chunkZOffset = chunkZ;
        this.chunks = chunks;
    }

    @Override
    public BlockState getBlockState(BlockPos pos)
    {
        return this.getRenderedChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ())).getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos)
    {
        return this.getRenderedChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ())).getBlockState(pos).getFluidState();
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded)
    {
        return this.world.getBrightness(direction, shaded);
    }

    @Override
    public LightingProvider getLightingProvider()
    {
        return this.world.getLightingProvider();
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos)
    {
        return this.getRenderedChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ())).getBlockEntity(pos);
    }

    private SchematicRenderedChunk getRenderedChunk(int x, int z)
    {
        return this.chunks[SchematicRendererRegion.getIndex(this.chunkXOffset, this.chunkZOffset, x, z)];
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver)
    {
        return this.world.getColor(pos, colorResolver);
    }

    @Override
    public int getBottomY()
    {
        return this.world.getBottomY();
    }

    @Override
    public int getHeight()
    {
        return this.world.getHeight();
    }

    public static int getIndex(int xOffset, int zOffset, int x, int z)
    {
        return x - xOffset + (z - zOffset) * 3;
    }
}
