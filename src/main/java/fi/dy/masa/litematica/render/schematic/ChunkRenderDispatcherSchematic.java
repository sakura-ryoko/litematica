package fi.dy.masa.litematica.render.schematic;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

    protected ChunkRenderDispatcherSchematic(WorldSchematic world, int viewDistanceChunks,
                                             IWorldSchematicRenderer worldRenderer,
                                             IChunkRendererFactory factory)
    {
        this.chunkRendererFactory = factory;
        this.chunkRenderers = new ConcurrentHashMap<>(4096, 0.9f, 2);
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
        for (Long key : this.chunkRenderers.keySet())
        {
            ChunkRendererSchematicVbo chunkRenderer = this.chunkRenderers.get(key);

            if (chunkRenderer != null)
            {
                chunkRenderer.deleteGlResources();
            }
        }

        this.chunkRenderers.clear();
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
//        this.chunkRenderers.values().removeIf(this::rendererOutOfRange);
//        Long2ObjectOpenHashMap<ChunkRendererSchematicVbo> newList = new Long2ObjectOpenHashMap<>();

        if (!this.chunkRenderers.isEmpty())
        {
            int prevCount = this.chunkRenderers.size();

            try
            {
//                final LongSet keys = this.chunkRenderers.keySet();

                for (Long key : this.chunkRenderers.keySet())
                {
//                    try
//                    {
//                        ChunkRendererSchematicVbo cr = this.chunkRenderers.get(key);
//
//                        if (cr != null)
//                        {
//                            if (!this.rendererOutOfRange(cr))
//                            {
//                                newList.put(key, cr);
//                            }
//                            else
//                            {
//                                ChunkPos pos = cr.getChunkPos();
//                                cr.deleteGlResources();
//
//                                // Because sometimes they aren't unloaded
//                                // properly when not actively rendering a placement
//                                if (this.world.getChunkSource().hasChunk(pos.x, pos.z))
//                                {
//                                    this.world.getChunkSource().unloadChunk(pos.x, pos.z);
//                                }
//                            }
//                        }
//                    }
//                    catch (Exception e)
//                    {
////                        if (Reference.DEBUG_MODE)
////                        {
//                            Litematica.debugLogError("removeOutOfRangeRenderers: get() threw an exception; {}", e.getMessage());
////                        }
//                    }

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

//        this.chunkRenderers.clear();
//        this.chunkRenderers.putAll(newList);
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
        long index = ChunkPos.pack(chunkX, chunkZ);

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
