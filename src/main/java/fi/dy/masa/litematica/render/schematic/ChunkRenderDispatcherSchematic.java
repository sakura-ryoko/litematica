package fi.dy.masa.litematica.render.schematic;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;

import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.render.IWorldSchematicRenderer;
import fi.dy.masa.litematica.world.WorldSchematic;

public class ChunkRenderDispatcherSchematic
{
    protected final ConcurrentHashMap<Long, ChunkRendererSchematicVbo> chunkRenderers;
    protected final IWorldSchematicRenderer renderer;
    protected final IChunkRendererFactory chunkRendererFactory;
    protected final WorldSchematic world;
    protected int viewDistanceChunks;
    protected int viewDistanceBlocksSq;
    private final ReentrantLock lock;

    protected ChunkRenderDispatcherSchematic(WorldSchematic world, int viewDistanceChunks,
                                             IWorldSchematicRenderer worldRenderer,
                                             IChunkRendererFactory factory)
    {
        this.chunkRendererFactory = factory;
        this.chunkRenderers = new ConcurrentHashMap<>(4096, 0.9f, 2);
        this.renderer = worldRenderer;
        this.world = world;
        this.setViewDistanceChunks(viewDistanceChunks);
        this.lock = new ReentrantLock();
    }

    protected void setViewDistanceChunks(int viewDistanceChunks)
    {
        this.viewDistanceChunks = viewDistanceChunks;
        this.viewDistanceBlocksSq = (viewDistanceChunks + 2) << 4; // Add like one extra chunk of margin just in case
        this.viewDistanceBlocksSq *= this.viewDistanceBlocksSq;
    }

    protected void delete()
    {
        this.lock.lock();

        try
        {
            for (Long key : this.chunkRenderers.keySet())
            {
                ChunkRendererSchematicVbo chunkRenderer = this.chunkRenderers.get(key);

                if (chunkRenderer != null)
                {
                    chunkRenderer.deleteGlResources();
                }
            }
        }
        finally
        {
            this.chunkRenderers.clear();
            this.lock.unlock();
        }
    }

    private boolean rendererOutOfRange(ChunkRendererSchematicVbo cr)
    {
        if (cr == null) return false;

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
        if (!this.chunkRenderers.isEmpty())
        {
            int prevCount = this.chunkRenderers.size();

            try
            {
                for (Long key : this.chunkRenderers.keySet())
                {
                    this.lock.lock();

                    try
                    {
                        synchronized (this.chunkRenderers)
                        {
                            ChunkRendererSchematicVbo cr = this.chunkRenderers.get(key);

                            if (this.rendererOutOfRange(cr))
                            {
                                try (ChunkRendererSchematicVbo cx = this.chunkRenderers.remove(key))
                                {
                                    cr.close();
                                    cx.close();
                                }
                                catch (Exception e)
                                {
                                    if (Reference.DEBUG_MODE)
                                    {
                                        Litematica.debugLogError("removeOutOfRangeRenderers: mapRemove() threw an exception; {}", e.getLocalizedMessage());
                                    }
                                }
                            }
                        }
                    }
                    finally
                    {
                        this.lock.unlock();
                    }
                }
            }
            catch (Exception e)
            {
                if (Reference.DEBUG_MODE)
                {
                    Litematica.debugLogError("removeOutOfRangeRenderers: keySet() threw an exception; {}", e.getLocalizedMessage());
                }
            }

            if (Reference.DEBUG_MODE && prevCount != this.chunkRenderers.size())
            {
                Litematica.LOGGER.warn("[Dispatch] removeOutOfRangeRenderers: [{}] -> [{}]", prevCount, this.chunkRenderers.size());
            }
        }
    }

    // `immediate` is only to be used with 'setBlockDirty()`
    protected void scheduleChunkRender(int chunkX, int chunkZ, boolean immediate)
    {
        this.getOrCreateChunkRenderer(chunkX, chunkZ).ifPresent(cr -> cr.setNeedsUpdate(immediate));
    }

    protected int getRendererCount()
    {
        return this.chunkRenderers.size();
    }

    protected Optional<ChunkRendererSchematicVbo> getOrCreateChunkRenderer(int chunkX, int chunkZ)
    {
        long index = ChunkPos.asLong(chunkX, chunkZ);

        try
        {
            this.lock.lock();

            try
            {
                if (!this.chunkRenderers.containsKey(index))
                {
                    ChunkRendererSchematicVbo renderer = this.chunkRendererFactory.create(this.world, this.renderer);

                    renderer.setPosition(chunkX << 4, this.world.getMinY(), chunkZ << 4);
                    renderer.setChunkPosition(chunkX, chunkZ);
                    renderer.setNeedsUpdate(false);         // Not an immediate update

                    synchronized (this.chunkRenderers)
                    {
                        this.chunkRenderers.put(index, renderer);
                    }
                }
            }
            finally
            {
                this.lock.unlock();
            }

            return Optional.of(this.chunkRenderers.get(index));
        }
        catch (Exception e)
        {
            if (Reference.DEBUG_MODE)
            {
                Litematica.debugLogError("getOrCreateChunkRenderer: Exception obtaining a Chunk Renderer; {}", e.getLocalizedMessage());
            }
        }

        return Optional.empty();
    }

    @Nullable
    protected ChunkRendererSchematicVbo getChunkRenderer(int chunkX, int chunkZ)
    {
        return this.getOrCreateChunkRenderer(chunkX, chunkZ).orElse(null);
    }
}
