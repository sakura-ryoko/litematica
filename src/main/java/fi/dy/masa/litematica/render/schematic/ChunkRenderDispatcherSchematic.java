package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.WorldSchematic;

public class ChunkRenderDispatcherSchematic
{
    protected final Long2ObjectOpenHashMap<ChunkRendererSchematicVbo> chunkRenderers;
    protected final WorldRendererSchematic renderer;
    protected final IChunkRendererFactory chunkRendererFactory;
    protected final WorldSchematic world;
    protected int viewDistanceChunks;
    protected int viewDistanceBlocksSq;

    protected ChunkRenderDispatcherSchematic(WorldSchematic world, int viewDistanceChunks,
                                             WorldRendererSchematic worldRenderer, IChunkRendererFactory factory)
    {
        this.chunkRendererFactory = factory;
		this.chunkRenderers = new Long2ObjectOpenHashMap<>();
        this.renderer = worldRenderer;
        this.world = world;
        this.setViewDistanceChunks(viewDistanceChunks);
    }

    protected void setViewDistanceChunks(int viewDistanceChunks)
    {
        this.viewDistanceChunks = viewDistanceChunks;
        this.viewDistanceBlocksSq = (viewDistanceChunks + 2) << 4; // Add like one extra chunk of margin just in case
        this.viewDistanceBlocksSq *= this.viewDistanceBlocksSq;
    }

    protected void delete()
    {
        for (ChunkRendererSchematicVbo chunkRenderer : this.chunkRenderers.values())
        {
            chunkRenderer.deleteGlResources();
        }

        this.chunkRenderers.clear();
    }

    private boolean rendererOutOfRange(ChunkRendererSchematicVbo cr)
    {
        if (cr.getDistanceSq() > this.viewDistanceBlocksSq || cr.isEmpty())     // Also remove "Empty" chunks, and clear resources.
        {
            cr.deleteGlResources();
            return true;
        }

        return false;
    }

    protected synchronized void removeOutOfRangeRenderers()
    {
        // Remove renderers that go out of view distance
//        this.chunkRenderers.values().removeIf(this::rendererOutOfRange);
        Long2ObjectOpenHashMap<ChunkRendererSchematicVbo> newList = new Long2ObjectOpenHashMap<>();

        if (!this.chunkRenderers.isEmpty())
        {
            int prevCount = this.chunkRenderers.size();

            try
            {
                final LongSet keys = this.chunkRenderers.keySet();

                for (long key : keys)
                {
                    try
                    {
                        ChunkRendererSchematicVbo cr = this.chunkRenderers.get(key);

                        if (cr != null)
                        {
                            if (!this.rendererOutOfRange(cr))
                            {
                                newList.put(key, cr);
                            }
                            else
                            {
                                ChunkPos pos = cr.getChunkPos();
                                cr.deleteGlResources();

                                // Because sometimes they aren't unloaded
                                // properly when not actively rendering a placement
                                if (this.world.getChunkProvider().hasChunk(pos.x, pos.z))
                                {
                                    DataManager.getSchematicPlacementManager().markChunkForUnload(pos);
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        Litematica.LOGGER.error("removeOutOfRangeRenderers: get() threw an exception; {}", e.getMessage());
                    }
                }
            }
            catch (Exception e)
            {
                Litematica.LOGGER.error("removeOutOfRangeRenderers: keySet() threw an exception; {}", e.getMessage());
            }

            if (Reference.DEBUG_MODE && prevCount != newList.size())
            {
                Litematica.LOGGER.warn("[Dispatch] removeOutOfRangeRenderers: [{}] -> [{}}", prevCount, newList.size());
            }
        }

        this.chunkRenderers.clear();
        this.chunkRenderers.putAll(newList);
    }

    protected void scheduleChunkRender(int chunkX, int chunkZ)
    {
        this.getOrCreateChunkRenderer(chunkX, chunkZ).setNeedsUpdate(false);
    }

    protected int getRendererCount()
    {
        return this.chunkRenderers.size();
    }

    protected ChunkRendererSchematicVbo getOrCreateChunkRenderer(int chunkX, int chunkZ)
    {
        long index = ChunkPos.asLong(chunkX, chunkZ);

        if (!this.chunkRenderers.containsKey(index))
        {
            ChunkRendererSchematicVbo renderer = this.chunkRendererFactory.create(this.world, this.renderer);

            renderer.setPosition(chunkX << 4, this.world.getMinY(), chunkZ << 4);
            renderer.setChunkPosition(chunkX, chunkZ);
            this.chunkRenderers.put(index, renderer);
        }

        return this.chunkRenderers.get(index);
    }

    @Nullable
    protected ChunkRendererSchematicVbo getChunkRenderer(int chunkX, int chunkZ)
    {
        return this.getOrCreateChunkRenderer(chunkX, chunkZ);
    }
}
