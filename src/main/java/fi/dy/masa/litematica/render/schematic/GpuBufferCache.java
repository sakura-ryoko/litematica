package fi.dy.masa.litematica.render.schematic;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class GpuBufferCache implements AutoCloseable
{
    private final ConcurrentHashMap<ChunkSectionLayer, ChunkRenderBuffers> blockBuffers;
    private final ConcurrentHashMap<OverlayRenderType, ChunkRenderBuffers> overlayBuffers;

    protected GpuBufferCache()
    {
	    this.blockBuffers = new ConcurrentHashMap<>(ByteBufferBuilderCache.BLOCK_LAYERS.size(), 0.9f, 1);
	    this.overlayBuffers = new ConcurrentHashMap<>(ByteBufferBuilderCache.TYPES.size(), 0.9f, 1);
    }

    protected boolean hasBuffers(ChunkSectionLayer layer)
    {
        return this.blockBuffers.containsKey(layer);
    }

    protected boolean hasBuffers(OverlayRenderType type)
    {
        return this.overlayBuffers.containsKey(type);
    }

    protected void saveBuffers(ChunkSectionLayer layer, @Nonnull ChunkRenderBuffers newBuffer)
    {
        if (this.hasBuffers(layer))
        {
            ChunkRenderBuffers remove = this.blockBuffers.remove(layer);

            try
            {
                remove.close();
            }
            catch (Exception err)
            {
                throw new RuntimeException("Exception closing Block Layer "+layer.label()+" Buffers; "+ err.getMessage());
            }
        }

        synchronized (this.blockBuffers)
        {
            this.blockBuffers.put(layer, newBuffer);
        }
    }

    protected void saveBuffers(OverlayRenderType type, @Nonnull ChunkRenderBuffers newBuffer)
    {
        if (this.hasBuffers(type))
        {
            ChunkRenderBuffers remove = this.overlayBuffers.remove(type);

            try
            {
                remove.close();
            }
            catch (Exception err)
            {
                throw new RuntimeException("Exception closing Overlay Type "+type.name()+" Buffers; "+ err.getMessage());
            }
        }

        synchronized (this.overlayBuffers)
        {
            this.overlayBuffers.put(type, newBuffer);
        }
    }

    @Nullable
    protected ChunkRenderBuffers getBuffersOrNull(ChunkSectionLayer layer)
    {
        return this.blockBuffers.get(layer);
    }

    @Nullable
    protected ChunkRenderBuffers getBuffersOrNull(OverlayRenderType type)
    {
        return this.overlayBuffers.get(type);
    }

    protected void clearAll()
    {
//        Litematica.LOGGER.warn("GpuBufferCache clearAll()");

        synchronized (this.blockBuffers)
        {
            this.blockBuffers.forEach(
                    (layer, buffers) ->
                    {
                        try
                        {
                            buffers.close();
                        }
                        catch (Exception err)
                        {
                            throw new RuntimeException("Exception closing Block Layer "+layer.label()+" Buffers; "+ err.getMessage());
                        }
                    }
            );

            this.blockBuffers.clear();
        }

        synchronized (this.overlayBuffers)
        {
            this.overlayBuffers.forEach(
                    (type, buffers) ->
                    {
                        try
                        {
                            buffers.close();
                        }
                        catch (Exception err)
                        {
                            throw new RuntimeException("Exception closing Overlay Type "+type.name()+" Buffers; "+ err.getMessage());
                        }
                    }
            );

            this.overlayBuffers.clear();
        }
    }

    @Override
    public void close() throws Exception
    {
        this.clearAll();
    }
}
