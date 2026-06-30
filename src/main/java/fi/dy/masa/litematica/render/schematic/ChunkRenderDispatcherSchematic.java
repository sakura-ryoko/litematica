package fi.dy.masa.litematica.render.schematic;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
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
    }

    protected void setViewDistanceChunks(int viewDistanceChunks)
    {
        this.viewDistanceChunks = viewDistanceChunks;
        this.viewDistanceBlocksSq = (viewDistanceChunks + 2) << 4; // Add like one extra chunk of margin just in case
        this.viewDistanceBlocksSq *= this.viewDistanceBlocksSq;
    }

    protected void delete()
    {
        RenderSystem.assertOnRenderThread();

        for (ChunkRendererSchematicVbo cr : this.chunkRenderers.values())
        {
//            ChunkRendererSchematicVbo chunkRenderer = this.chunkRenderers.get(key);
            if (cr != null)
            {
                cr.deleteGlResources();
            }
        }

        this.chunkRenderers.clear();
    }

//    private boolean rendererOutOfRange(ChunkRendererSchematicVbo cr)
//    {
//        if (cr == null) return false;
//
//        if (cr.getDistanceSq() > this.viewDistanceBlocksSq || cr.isEmpty())     // Also remove "Empty" chunks, and clear resources.
//        {
//            try
//            {
//                cr.deleteGlResources();
//            }
//            catch (Exception ignored) {}
//
//            return true;
//        }
//
//        return false;
//    }

    protected void removeOutOfRangeRenderers()
    {
//        RenderSystem.assertOnRenderThread();

        // Remove renderers that go out of view distance
        if (!this.chunkRenderers.isEmpty())
        {
            int prevCount = this.chunkRenderers.size();

            try
            {
                this.chunkRenderers.entrySet()
                                   .removeIf(entry ->
                                    {
                                        ChunkRendererSchematicVbo cr = entry.getValue();

                                        if (cr != null && (cr.getDistanceSq() > this.viewDistanceBlocksSq || cr.isEmpty()))
                                        {
                                            try
                                            {
                                                cr.close();
                                            }
                                            catch (Exception e)
                                            {
                                                if (Reference.DEBUG_MODE)
                                                {
                                                    Litematica.debugLog("removeOutOfRangeRenderers: cr.close() threw an exception; {}", e.getLocalizedMessage());
                                                }
                                            }

                                            return true;
                                        }
                                        else
                                        {
                                            return false;
                                        }
                                    });
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
        this.addPendingChunkRender(ChunkPos.asLong(chunkX, chunkZ), immediate);
    }

    private void addPendingChunkRender(final Long chunk, boolean immediate)
    {
        this.pendingChunks.putIfAbsent(chunk, immediate);
    }

    private boolean getPendingChunk(final Long chunk)
    {
        if (this.pendingChunks.containsKey(chunk))
        {
            return this.pendingChunks.get(chunk);
        }

        return false;
    }

    private void removePendingChunk(final Long chunk)
    {
	    this.pendingChunks.remove(chunk);
    }

    private boolean matchPendingChunk(final Long chunk)
    {
        return this.pendingChunks.containsKey(chunk);
    }

    protected int getRendererCount()
    {
        return this.chunkRenderers.size();
    }

    protected int getPendingChunkCount()
    {
        return this.pendingChunks.size();
    }

    protected boolean hasRenderer(Long chunk)
    {
        return this.chunkRenderers.containsKey(chunk);
    }

    protected Optional<ChunkRendererSchematicVbo> getOrCreateChunkRenderer(int chunkX, int chunkZ)
    {
        final long index = ChunkPos.asLong(chunkX, chunkZ);

        try
        {
            if (!this.chunkRenderers.containsKey(index))
            {
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
    protected  ChunkRendererSchematicVbo getChunkRenderer(int chunkX, int chunkZ)
    {
        return this.getOrCreateChunkRenderer(chunkX, chunkZ).orElse(null);
    }
}
