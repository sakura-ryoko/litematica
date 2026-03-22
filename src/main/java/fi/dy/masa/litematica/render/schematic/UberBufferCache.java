package fi.dy.masa.litematica.render.schematic;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;

public class UberBufferCache implements AutoCloseable
{
    private final ConcurrentHashMap<ChunkSectionLayer, ChunkRenderUberBuffers> blockBuffers;
    private final ConcurrentHashMap<RenderType, ChunkRenderUberBuffers> layerBuffers;
    private final ConcurrentHashMap<OverlayRenderType, ChunkRenderUberBuffers> overlayBuffers;

    protected UberBufferCache()
    {
	    this.blockBuffers = new ConcurrentHashMap<>(ByteBufferBuilderCache.BLOCK_LAYERS.size(), 0.9f, 1);
	    this.layerBuffers = new ConcurrentHashMap<>(ByteBufferBuilderCache.RENDER_LAYERS.size(), 0.9f, 1);
	    this.overlayBuffers = new ConcurrentHashMap<>(ByteBufferBuilderCache.TYPES.size(), 0.9f, 1);
    }

    protected boolean hasBuffersByBlockLayer(ChunkSectionLayer layer)
    {
        return this.blockBuffers.containsKey(layer);
    }

    protected boolean hasBuffersByLayer(RenderType layer)
    {
        return this.layerBuffers.containsKey(layer);
    }

    protected boolean hasBuffersByType(OverlayRenderType type)
    {
        return this.overlayBuffers.containsKey(type);
    }

    protected void storeBuffersByBlockLayer(ChunkSectionLayer layer, @Nonnull ChunkRenderUberBuffers newBuffer)
    {
        if (this.hasBuffersByBlockLayer(layer))
        {
            ChunkRenderUberBuffers remove = this.blockBuffers.remove(layer);

            try
            {
                remove.vertexBuffer().close();
                remove.indexBuffer().close();
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

    protected void storeBuffersByLayer(RenderType layer, @Nonnull ChunkRenderUberBuffers newBuffer)
    {
        if (this.hasBuffersByLayer(layer))
        {
            ChunkRenderUberBuffers remove = this.layerBuffers.remove(layer);

            try
            {
                remove.vertexBuffer().close();
                remove.indexBuffer().close();
            }
            catch (Exception err)
            {
                throw new RuntimeException("Exception closing Layer "+ ChunkRenderLayers.getFriendlyName(layer)+" Buffers; "+ err.getMessage());
            }
        }

        synchronized (this.layerBuffers)
        {
            this.layerBuffers.put(layer, newBuffer);
        }
    }

    protected void storeBuffersByType(OverlayRenderType type, @Nonnull ChunkRenderUberBuffers newBuffer)
    {
        if (this.hasBuffersByType(type))
        {
            ChunkRenderUberBuffers remove = this.overlayBuffers.remove(type);

            try
            {
                remove.vertexBuffer().close();
                remove.indexBuffer().close();
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
    protected ChunkRenderUberBuffers getBuffersByBlockLayer(ChunkSectionLayer layer)
    {
        return this.blockBuffers.get(layer);
    }

    @Nullable
    protected ChunkRenderUberBuffers getBuffersByLayer(RenderType layer)
    {
        return this.layerBuffers.get(layer);
    }

    @Nullable
    protected ChunkRenderUberBuffers getBuffersByType(OverlayRenderType type)
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
                            buffers.vertexBuffer().close();
                            buffers.indexBuffer().close();
                        }
                        catch (Exception err)
                        {
                            throw new RuntimeException("Exception closing Block Layer "+layer.label()+" Buffers; "+ err.getMessage());
                        }
                    }
            );

            this.blockBuffers.clear();
        }

        synchronized (this.layerBuffers)
        {
            this.layerBuffers.forEach(
                    (layer, buffers) ->
                    {
                        try
                        {
                            buffers.vertexBuffer().close();
                            buffers.indexBuffer().close();
                        }
                        catch (Exception err)
                        {
                            throw new RuntimeException("Exception closing Layer "+ChunkRenderLayers.getFriendlyName(layer)+" Buffers; "+ err.getMessage());
                        }
                    }
            );

            this.layerBuffers.clear();
        }

        synchronized (this.overlayBuffers)
        {
            this.overlayBuffers.forEach(
                    (type, buffers) ->
                    {
                        try
                        {
                            buffers.vertexBuffer().close();
                            buffers.indexBuffer().close();
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
