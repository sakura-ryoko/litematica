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
    protected final ConcurrentHashMap<Long, Boolean> pendingChunks;
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
        this.chunkRenderers = new ConcurrentHashMap<>(1024, 0.9f, 2);
        this.pendingChunks = new ConcurrentHashMap<>(1024, 0.9f, 2);
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
            try
            {
                cr.deleteGlResources();
            }
            catch (Exception ignored) {}

            return true;
        }

        return false;
    }

    protected synchronized void removeOutOfRangeRenderers()
    {
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
                                    Litematica.debugLog("removeOutOfRangeRenderers: mapRemove() threw an exception; {}", e.getLocalizedMessage());
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
                    Litematica.debugLog("removeOutOfRangeRenderers: keySet() threw an exception; {}", e.getLocalizedMessage());
                }
            }

            if (Reference.DEBUG_MODE && prevCount != this.chunkRenderers.size())
            {
                Litematica.LOGGER.warn("[Dispatch] removeOutOfRangeRenderers: [{}] -> [{}]", prevCount, this.chunkRenderers.size());
            }
        }
    }

    // Do not call getOrCreateChunkRenderer() from the PM Threads.  This is a work-around.
    // `immediate` is only to be used with 'setBlockDirty()`
    protected void scheduleChunkRender(int chunkX, int chunkZ, boolean immediate)
    {
//        this.getOrCreateChunkRenderer(chunkX, chunkZ).ifPresent(cr -> cr.setNeedsUpdate(immediate));
        this.addPendingChunkRender(ChunkPos.pack(chunkX, chunkZ), immediate);
    }

    private synchronized void addPendingChunkRender(final Long chunk, boolean immediate)
    {
        this.pendingChunks.putIfAbsent(chunk, immediate);
    }

    private synchronized boolean getPendingChunk(final Long chunk)
    {
        if (this.pendingChunks.containsKey(chunk))
        {
            return this.pendingChunks.get(chunk);
        }

        return false;
    }

    private synchronized void removePendingChunk(final Long chunk)
    {
        this.pendingChunks.remove(chunk);
    }

    private synchronized boolean matchPendingChunk(final Long chunk)
    {
        return this.pendingChunks.containsKey(chunk);
    }

    protected synchronized int getRendererCount()
    {
        return this.chunkRenderers.size();
    }

    protected synchronized int getPendingChunkCount()
    {
        return this.pendingChunks.size();
    }

    protected synchronized boolean hasRenderer(Long chunk)
    {
        return this.chunkRenderers.containsKey(chunk);
    }

    protected synchronized Optional<ChunkRendererSchematicVbo> getOrCreateChunkRenderer(int chunkX, int chunkZ)
    {
        final long index = ChunkPos.pack(chunkX, chunkZ);

        try
        {
            if (!this.chunkRenderers.containsKey(index))
            {
//                Litematica.LOGGER.warn("[Dispatch] chunkRenderer[{}, {}] does not exist, factory create -->", chunkX, chunkZ);
                ChunkRendererSchematicVbo renderer = this.chunkRendererFactory.create(this.world, this.renderer);

                renderer.setPosition(chunkX << 4, this.world.getMinY(), chunkZ << 4);
                renderer.setChunkPosition(chunkX, chunkZ);

                if (this.matchPendingChunk(index))
                {
                    renderer.setNeedsUpdate(this.getPendingChunk(index));
                    this.removePendingChunk(index);
                }
                else
                {
                    renderer.setNeedsUpdate(false);         // Not an immediate update
                }

                this.chunkRenderers.put(index, renderer);
            }

            ChunkRendererSchematicVbo renderer = this.chunkRenderers.get(index);

            if (renderer != null && this.matchPendingChunk(index))
            {
                renderer.setNeedsUpdate(this.getPendingChunk(index));
                this.removePendingChunk(index);
            }

            return Optional.ofNullable(renderer);
        }
        catch (Exception e)
        {
            if (Reference.DEBUG_MODE)
            {
                Litematica.debugLog("getOrCreateChunkRenderer: Exception obtaining a Chunk Renderer; {}", e.getLocalizedMessage());
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
