package fi.dy.masa.litematica.render.schematic;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayer;

public class GpuBufferCache implements AutoCloseable
{
    private final ConcurrentHashMap<BlockRenderLayer, ChunkRenderObjectBuffers> blockBuffers;
    private final ConcurrentHashMap<RenderLayer, ChunkRenderObjectBuffers> layerBuffers;
    private final ConcurrentHashMap<OverlayRenderType, ChunkRenderObjectBuffers> overlayBuffers;

    protected GpuBufferCache()
    {
	    this.blockBuffers = new ConcurrentHashMap<>();
	    this.layerBuffers = new ConcurrentHashMap<>();
	    this.overlayBuffers = new ConcurrentHashMap<>();
    }

    protected boolean hasBuffersByBlockLayer(BlockRenderLayer layer)
    {
        return this.blockBuffers.containsKey(layer);
    }

    protected boolean hasBuffersByLayer(RenderLayer layer)
    {
        return this.layerBuffers.containsKey(layer);
    }

    protected boolean hasBuffersByType(OverlayRenderType type)
    {
        return this.overlayBuffers.containsKey(type);
    }

    protected void storeBuffersByBlockLayer(BlockRenderLayer layer, @Nonnull ChunkRenderObjectBuffers newBuffer)
    {
        if (this.hasBuffersByBlockLayer(layer))
        {
            ChunkRenderObjectBuffers remove = this.blockBuffers.remove(layer);

            try
            {
                remove.close();
            }
            catch (Exception err)
            {
                throw new RuntimeException("Exception closing Block Layer "+layer.getName()+" Buffers; "+ err.getMessage());
            }
        }

        synchronized (this.blockBuffers)
        {
            this.blockBuffers.put(layer, newBuffer);
        }
    }

    protected void storeBuffersByLayer(RenderLayer layer, @Nonnull ChunkRenderObjectBuffers newBuffer)
    {
        if (this.hasBuffersByLayer(layer))
        {
            ChunkRenderObjectBuffers remove = this.layerBuffers.remove(layer);

            try
            {
                remove.close();
            }
            catch (Exception err)
            {
                throw new RuntimeException("Exception closing Layer "+ChunkRenderLayers.getFriendlyName(layer)+" Buffers; "+ err.getMessage());
            }
        }

        synchronized (this.layerBuffers)
        {
            this.layerBuffers.put(layer, newBuffer);
        }
    }

    protected void storeBuffersByType(OverlayRenderType type, @Nonnull ChunkRenderObjectBuffers newBuffer)
    {
        if (this.hasBuffersByType(type))
        {
            ChunkRenderObjectBuffers remove = this.overlayBuffers.remove(type);

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
    protected ChunkRenderObjectBuffers getBuffersByBlockLayer(BlockRenderLayer layer)
    {
        return this.blockBuffers.get(layer);
    }

    @Nullable
    protected ChunkRenderObjectBuffers getBuffersByLayer(RenderLayer layer)
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
                            throw new RuntimeException("Exception closing Block Layer "+layer.getName()+" Buffers; "+ err.getMessage());
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
                            buffers.close();
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
