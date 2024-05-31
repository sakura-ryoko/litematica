package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.world.WorldSchematic;

public class ChunkRenderDispatcherSchematic
{
    protected final Long2ObjectOpenHashMap<ChunkRendererSchematicVbo> chunkRenderers = new Long2ObjectOpenHashMap<>();
    protected final WorldRendererSchematic renderer;
    protected final IChunkRendererFactory chunkRendererFactory;
    protected final WorldSchematic world;
    protected int viewDistanceChunks;
    protected int viewDistanceBlocksSq;

    public ChunkRenderDispatcherSchematic(WorldSchematic world, int viewDistanceChunks,
            WorldRendererSchematic worldRenderer, IChunkRendererFactory factory)
    {
        this.chunkRendererFactory = factory;
        this.renderer = worldRenderer;
        this.world = world;
        this.setViewDistanceChunks(viewDistanceChunks);
    }

    public void setViewDistanceChunks(int viewDistanceChunks)
    {
        this.viewDistanceChunks = viewDistanceChunks;
        this.viewDistanceBlocksSq = (viewDistanceChunks + 2) << 4; // Add like one extra chunk of margin just in case
        this.viewDistanceBlocksSq *= this.viewDistanceBlocksSq;
        //this.delete();
    }

    public void delete()
    {
        int i = 0;

        for (ChunkRendererSchematicVbo chunkRenderer : this.chunkRenderers.values())
        {
            //Litematica.logger.error("delete() [Schematic Dispatch] for thread {}", i);
            chunkRenderer.deleteGlResources();
            i++;
        }

        this.chunkRenderers.clear();
    }

    private boolean rendererOutOfRange(ChunkRendererSchematicVbo cr)
    {
        if (cr.getDistanceSq() > this.viewDistanceBlocksSq)
        {
            cr.deleteGlResources();

            return true;
        }

        return false;
    }

    public void removeOutOfRangeRenderers()
    {
        // Remove renderers that go out of view distance
        this.chunkRenderers.values().removeIf(this::rendererOutOfRange);
    }

    public void scheduleChunkRender(int chunkX, int chunkZ)
    {
        this.getOrCreateChunkRenderer(chunkX, chunkZ).setNeedsUpdate(false);
    }

    public int getRendererCount()
    {
        return this.chunkRenderers.size();
    }

    protected ChunkRendererSchematicVbo getOrCreateChunkRenderer(int chunkX, int chunkZ)
    {
        long index = ChunkPos.toLong(chunkX, chunkZ);
        ChunkRendererSchematicVbo renderer = this.chunkRenderers.get(index);

        if (renderer == null)
        {
            renderer = this.chunkRendererFactory.create(this.world, this.renderer);
            //Litematica.logger.warn("getOrCreateChunkRenderer(): new index[{}] --> chunkX, chunkZ [{}, {}]", index, chunkX, chunkZ);
            renderer.setPosition(chunkX << 4, this.world.getBottomY(), chunkZ << 4);
            this.chunkRenderers.put(index, renderer);
        }

        return renderer;
    }

    @Nullable
    protected ChunkRendererSchematicVbo getChunkRenderer(int chunkX, int chunkZ)
    {
        return this.getOrCreateChunkRenderer(chunkX, chunkZ);
    }
}
