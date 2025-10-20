package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BufferAllocatorCache implements AutoCloseable
{
    protected static final List<BlockRenderLayer> BLOCK_LAYERS = ChunkRenderLayers.BLOCK_RENDER_LAYERS;
    protected static final List<RenderLayer> RENDER_LAYERS = ChunkRenderLayers.RENDER_LAYERS;
    protected static final List<OverlayRenderType> TYPES = ChunkRenderLayers.TYPES;
    protected static final int EXPECTED_TOTAL_SIZE;
    private final ConcurrentHashMap<BlockRenderLayer, BufferAllocator> blockCache;
    private final ConcurrentHashMap<RenderLayer, BufferAllocator> layerCache;
    private final ConcurrentHashMap<OverlayRenderType, BufferAllocator> overlayCache;
    private boolean clear;

    protected BufferAllocatorCache()
    {
	    this.blockCache = new ConcurrentHashMap<>();
		this.layerCache = new ConcurrentHashMap<>();
		this.overlayCache = new ConcurrentHashMap<>();
        this.clear = true;
    }

    protected void allocateCache()
    {
        for (BlockRenderLayer layer : BLOCK_LAYERS)
        {
            if (this.blockCache.containsKey(layer))
            {
                this.blockCache.get(layer).close();
            }

            synchronized (this.blockCache)
            {
                this.blockCache.put(layer, new BufferAllocator(layer.getBufferSize()));
            }
        }
        for (RenderLayer layer : RENDER_LAYERS)
        {
            if (this.layerCache.containsKey(layer))
            {
                this.layerCache.get(layer).close();
            }

            synchronized (this.layerCache)
            {
                this.layerCache.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
            }
        }
        for (OverlayRenderType type : TYPES)
        {
            if (this.overlayCache.containsKey(type))
            {
                this.overlayCache.get(type).close();
            }

            synchronized (this.overlayCache)
            {
                this.overlayCache.put(type, new BufferAllocator(type.getExpectedBufferSize()));
            }
        }

        this.clear = true;
    }

    protected boolean hasBufferByBlockLayer(BlockRenderLayer layer)
    {
        return this.blockCache.containsKey(layer);
    }

    protected boolean hasBufferByLayer(RenderLayer layer)
    {
        return this.layerCache.containsKey(layer);
    }

    protected boolean hasBufferByOverlay(OverlayRenderType type)
    {
        return this.overlayCache.containsKey(type);
    }

    protected BufferAllocator getBufferByBlockLayer(BlockRenderLayer layer)
    {
        this.clear = false;

        synchronized (this.blockCache)
        {
            return this.blockCache.computeIfAbsent(layer, l -> new BufferAllocator(l.getBufferSize()));
        }
    }

    protected BufferAllocator getBufferByLayer(RenderLayer layer)
    {
        this.clear = false;

        synchronized (this.layerCache)
        {
            return this.layerCache.computeIfAbsent(layer, l -> new BufferAllocator(l.getExpectedBufferSize()));
        }
    }

    protected BufferAllocator getBufferByOverlay(OverlayRenderType type)
    {
        this.clear = false;

        synchronized (this.overlayCache)
        {
            return this.overlayCache.computeIfAbsent(type, t -> new BufferAllocator(t.getExpectedBufferSize()));
        }
    }

    protected void closeByBlockLayer(BlockRenderLayer layer)
    {
        try
        {
            synchronized (this.blockCache)
            {
                this.blockCache.remove(layer).close();
            }
        }
        catch (Exception ignored) { }
    }

    protected void closeByLayer(RenderLayer layer)
    {
        try
        {
            synchronized (this.layerCache)
            {
                this.layerCache.remove(layer).close();
            }
        }
        catch (Exception ignored) { }
    }

    protected void closeByType(OverlayRenderType type)
    {
        try
        {
            synchronized (this.overlayCache)
            {
                this.overlayCache.remove(type).close();
            }
        }
        catch (Exception ignored) { }
    }

    protected boolean isClear() { return this.clear; }

    protected void resetAll()
    {
        try
        {
            this.blockCache.values().forEach(BufferAllocator::reset);
            this.layerCache.values().forEach(BufferAllocator::reset);
            this.overlayCache.values().forEach(BufferAllocator::reset);
        }
        catch (Exception ignored) { }

        this.clear = true;
    }

    protected void clearAll()
    {
        try
        {
            this.blockCache.values().forEach(BufferAllocator::clear);
            this.layerCache.values().forEach(BufferAllocator::clear);
            this.overlayCache.values().forEach(BufferAllocator::clear);
        }
        catch (Exception ignored) { }

        this.clear = true;
    }

    protected void closeAll()
    {
        ArrayList<BufferAllocator> allocators;

        synchronized (this.blockCache)
        {
            allocators = new ArrayList<>(this.blockCache.values());
            this.blockCache.clear();
        }
        synchronized (this.layerCache)
        {
            allocators.addAll(this.layerCache.values());
            this.layerCache.clear();
        }
        synchronized (this.overlayCache)
        {
            allocators.addAll(this.overlayCache.values());
            this.overlayCache.clear();
        }

        allocators.forEach(BufferAllocator::close);
        this.clear = true;
    }

    @Override
    public void close()
    {
        this.closeAll();
    }

    static
    {
        EXPECTED_TOTAL_SIZE = BLOCK_LAYERS.stream().mapToInt(BlockRenderLayer::getBufferSize).sum() + RENDER_LAYERS.stream().mapToInt(RenderLayer::getExpectedBufferSize).sum() + TYPES.stream().mapToInt(OverlayRenderType::getExpectedBufferSize).sum();
    }
}
