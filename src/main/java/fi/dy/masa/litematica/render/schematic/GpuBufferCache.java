package fi.dy.masa.litematica.render.schematic;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;

public class GpuBufferCache implements AutoCloseable
{
    private final ConcurrentHashMap<ChunkSectionLayer, ChunkRenderObjectBuffers> blockBuffers;
    private final ConcurrentHashMap<RenderType, ChunkRenderObjectBuffers> layerBuffers;
    private final ConcurrentHashMap<OverlayRenderType, ChunkRenderObjectBuffers> overlayBuffers;

    protected GpuBufferCache()
    {
	    this.blockBuffers = new ConcurrentHashMap<>(BufferAllocatorCache.BLOCK_LAYERS.size(), 0.9f, 1);
	    this.layerBuffers = new ConcurrentHashMap<>(BufferAllocatorCache.RENDER_LAYERS.size(), 0.9f, 1);
	    this.overlayBuffers = new ConcurrentHashMap<>(BufferAllocatorCache.TYPES.size(), 0.9f, 1);
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

    protected void storeBuffersByBlockLayer(ChunkSectionLayer layer, @Nonnull ChunkRenderObjectBuffers newBuffer)
    {
        ChunkRenderObjectBuffers remove = this.blockBuffers.put(layer, newBuffer);

        if (remove != null)
        {
            try
            {
                remove.close();
            }
            catch (Exception err)
            {
                throw new RuntimeException("Exception closing Block Layer "+layer.label()+" Buffers; "+ err.getMessage());
            }
        }
    }

    protected void storeBuffersByLayer(RenderType layer, @Nonnull ChunkRenderObjectBuffers newBuffer)
    {
        ChunkRenderObjectBuffers remove = this.layerBuffers.put(layer, newBuffer);

        if (remove != null)
        {
            try
            {
                remove.close();
            }
            catch (Exception err)
            {
                throw new RuntimeException("Exception closing Layer "+ChunkRenderLayers.getFriendlyName(layer)+" Buffers; "+ err.getMessage());
            }
        }
    }

    protected void storeBuffersByType(OverlayRenderType type, @Nonnull ChunkRenderObjectBuffers newBuffer)
    {
        ChunkRenderObjectBuffers remove = this.overlayBuffers.put(type, newBuffer);

        if (remove != null)
        {
            try
            {
                remove.close();
            }
            catch (Exception err)
            {
                throw new RuntimeException("Exception closing Overlay Type "+type.name()+" Buffers; "+ err.getMessage());
            }
        }
    }

    @Nullable
    protected ChunkRenderObjectBuffers getBuffersByBlockLayer(ChunkSectionLayer layer)
    {
        return this.blockBuffers.get(layer);
    }

    @Nullable
    protected ChunkRenderObjectBuffers getBuffersByLayer(RenderType layer)
    {
        return this.layerBuffers.get(layer);
    }

    @Nullable
    protected ChunkRenderObjectBuffers getBuffersByType(OverlayRenderType type)
    {
        return this.overlayBuffers.get(type);
    }

    protected void clearAll()
    {
//        Litematica.LOGGER.warn("GpuBufferCache clearAll()");
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

        this.layerBuffers.forEach(
                (layer, buffers) ->
                {
                    try
                    {
                        buffers.close();
                    }
                    catch (Exception err)
                    {
                        throw new RuntimeException("Exception closing Layer "+ChunkRenderLayers.getFriendlyName(layer)+" Buffers; "+ err.getMessage());
                    }
                }
        );
        this.layerBuffers.clear();

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

    @Override
    public void close() throws Exception
    {
        this.clearAll();
    }
}
