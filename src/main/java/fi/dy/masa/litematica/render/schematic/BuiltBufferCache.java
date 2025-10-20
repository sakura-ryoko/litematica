package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;

public class BuiltBufferCache implements AutoCloseable
{
    private final ConcurrentHashMap<BlockRenderLayer, BuiltBuffer> blockBuffers;
    private final ConcurrentHashMap<RenderLayer, BuiltBuffer> layerBuffers;
    private final ConcurrentHashMap<OverlayRenderType, BuiltBuffer> overlayBuffers;

    protected BuiltBufferCache()
    {
	    this.blockBuffers = new ConcurrentHashMap<>();
	    this.layerBuffers = new ConcurrentHashMap<>();
	    this.overlayBuffers = new ConcurrentHashMap<>();
    }

    protected boolean hasBuiltBufferByBlockLayer(BlockRenderLayer layer)
    {
        return this.blockBuffers.containsKey(layer);
    }

    protected boolean hasBuiltBufferByLayer(RenderLayer layer)
    {
        return this.layerBuffers.containsKey(layer);
    }

    protected boolean hasBuiltBufferByType(OverlayRenderType type)
    {
        return this.overlayBuffers.containsKey(type);
    }

    protected void storeBuiltBufferByBlockLayer(BlockRenderLayer layer, @Nonnull BuiltBuffer newBuffer)
    {
        if (this.hasBuiltBufferByBlockLayer(layer))
        {
            this.blockBuffers.get(layer).close();
        }
        synchronized (this.blockBuffers)
        {
            this.blockBuffers.put(layer, newBuffer);
        }
    }

    protected void storeBuiltBufferByLayer(RenderLayer layer, @Nonnull BuiltBuffer newBuffer)
    {
        if (this.hasBuiltBufferByLayer(layer))
        {
            this.layerBuffers.get(layer).close();
        }
        synchronized (this.layerBuffers)
        {
            this.layerBuffers.put(layer, newBuffer);
        }
    }

    protected void storeBuiltBufferByType(OverlayRenderType type, @Nonnull BuiltBuffer newBuffer)
    {
        if (this.hasBuiltBufferByType(type))
        {
            this.overlayBuffers.get(type).close();
        }
        synchronized (this.overlayBuffers)
        {
            this.overlayBuffers.put(type, newBuffer);
        }
    }

    @Nullable
    protected BuiltBuffer getBuiltBufferByBlockLayer(BlockRenderLayer layer)
    {
        return this.blockBuffers.get(layer);
    }

    @Nullable
    protected BuiltBuffer getBuiltBufferByLayer(RenderLayer layer)
    {
        return this.layerBuffers.get(layer);
    }

    @Nullable
    protected BuiltBuffer getBuiltBufferByType(OverlayRenderType type)
    {
        return this.overlayBuffers.get(type);
    }

    protected void closeAll()
    {
        ArrayList<BuiltBuffer> builtBuffers;

        synchronized (this.blockBuffers)
        {
            builtBuffers = new ArrayList<>(this.blockBuffers.values());
            this.blockBuffers.clear();
        }
        synchronized (this.layerBuffers)
        {
            builtBuffers.addAll(this.layerBuffers.values());
            this.layerBuffers.clear();
        }
        synchronized (this.overlayBuffers)
        {
            builtBuffers.addAll(this.overlayBuffers.values());
            this.overlayBuffers.clear();
        }
        try
        {
            builtBuffers.forEach(BuiltBuffer::close);
        }
        catch (Exception ignored) { }
    }

    @Override
    public void close() throws Exception
    {
        this.closeAll();
    }
}
