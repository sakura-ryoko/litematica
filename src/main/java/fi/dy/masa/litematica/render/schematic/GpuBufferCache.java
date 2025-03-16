package fi.dy.masa.litematica.render.schematic;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.render.RenderLayer;

import fi.dy.masa.litematica.Litematica;

public class GpuBufferCache implements AutoCloseable
{
    private final ConcurrentHashMap<RenderLayer, ChunkRenderObjectBuffers> layerBuffers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<OverlayRenderType, ChunkRenderObjectBuffers> overlayBuffers = new ConcurrentHashMap<>();

    protected GpuBufferCache() { }

    protected boolean hasBuffersByLayer(RenderLayer layer)
    {
        return this.layerBuffers.containsKey(layer);
    }

    protected boolean hasBuffersByType(OverlayRenderType type)
    {
        return this.overlayBuffers.containsKey(type);
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
                throw new RuntimeException("Exception closing Block Layer "+ChunkRenderLayers.getFriendlyName(layer)+" Buffers; "+ err.getMessage());
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
        Litematica.LOGGER.warn("GpuBufferCache clearAll()");

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
                            throw new RuntimeException("Exception closing Block Layer "+ChunkRenderLayers.getFriendlyName(layer)+" Buffers; "+ err.getMessage());
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
