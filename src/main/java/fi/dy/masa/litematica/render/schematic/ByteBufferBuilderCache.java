package fi.dy.masa.litematica.render.schematic;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;

@Environment(EnvType.CLIENT)
public class ByteBufferBuilderCache implements AutoCloseable
{
    protected static final List<ChunkSectionLayer> BLOCK_LAYERS = ChunkRenderLayers.BLOCK_RENDER_LAYERS;
    protected static final List<RenderType> RENDER_LAYERS = ChunkRenderLayers.RENDER_LAYERS;
    protected static final List<OverlayRenderType> TYPES = ChunkRenderLayers.TYPES;
    protected static final int EXPECTED_TOTAL_SIZE;
    private final ConcurrentHashMap<ChunkSectionLayer, ByteBufferBuilder> blockCache;
    private final ConcurrentHashMap<RenderType, ByteBufferBuilder> layerCache;
    private final ConcurrentHashMap<OverlayRenderType, ByteBufferBuilder> overlayCache;
    private boolean clear;

    protected ByteBufferBuilderCache()
    {
	    this.blockCache = new ConcurrentHashMap<>(BLOCK_LAYERS.size(), 0.9f, 1);
		this.layerCache = new ConcurrentHashMap<>(RENDER_LAYERS.size(), 0.9f, 1);
		this.overlayCache = new ConcurrentHashMap<>(TYPES.size(), 0.9f, 1);
        this.clear = true;
    }

    protected void allocateCache()
    {
        for (ChunkSectionLayer layer : BLOCK_LAYERS)
        {
            if (this.blockCache.containsKey(layer))
            {
                this.blockCache.get(layer).close();
            }

            synchronized (this.blockCache)
            {
                this.blockCache.put(layer, new ByteBufferBuilder(layer.bufferSize()));
            }
        }
        for (RenderType layer : RENDER_LAYERS)
        {
            if (this.layerCache.containsKey(layer))
            {
                this.layerCache.get(layer).close();
            }

            synchronized (this.layerCache)
            {
                this.layerCache.put(layer, new ByteBufferBuilder(layer.bufferSize()));
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
                this.overlayCache.put(type, new ByteBufferBuilder(type.getExpectedBufferSize()));
            }
        }

        this.clear = true;
    }

    protected boolean hasBufferByBlockLayer(ChunkSectionLayer layer)
    {
        return this.blockCache.containsKey(layer);
    }

    protected boolean hasBufferByLayer(RenderType layer)
    {
        return this.layerCache.containsKey(layer);
    }

    protected boolean hasBufferByOverlay(OverlayRenderType type)
    {
        return this.overlayCache.containsKey(type);
    }

    protected ByteBufferBuilder getBufferByBlockLayer(ChunkSectionLayer layer)
    {
        this.clear = false;

        synchronized (this.blockCache)
        {
            return this.blockCache.computeIfAbsent(layer, l -> new ByteBufferBuilder(l.bufferSize()));
        }
    }

    protected ByteBufferBuilder getBufferByLayer(RenderType layer)
    {
        this.clear = false;

        synchronized (this.layerCache)
        {
            return this.layerCache.computeIfAbsent(layer, l -> new ByteBufferBuilder(l.bufferSize()));
        }
    }

    protected ByteBufferBuilder getBufferByOverlay(OverlayRenderType type)
    {
        this.clear = false;

        synchronized (this.overlayCache)
        {
            return this.overlayCache.computeIfAbsent(type, t -> new ByteBufferBuilder(t.getExpectedBufferSize()));
        }
    }

    protected void closeByBlockLayer(ChunkSectionLayer layer)
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

    protected void closeByLayer(RenderType layer)
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
            this.blockCache.values().forEach(ByteBufferBuilder::discard);
            this.layerCache.values().forEach(ByteBufferBuilder::discard);
            this.overlayCache.values().forEach(ByteBufferBuilder::discard);
        }
        catch (Exception ignored) { }

        this.clear = true;
    }

    protected void clearAll()
    {
        try
        {
            this.blockCache.values().forEach(ByteBufferBuilder::clear);
            this.layerCache.values().forEach(ByteBufferBuilder::clear);
            this.overlayCache.values().forEach(ByteBufferBuilder::clear);
        }
        catch (Exception ignored) { }

        this.clear = true;
    }

    protected void closeAll()
    {
        ArrayList<ByteBufferBuilder> allocators;

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

        allocators.forEach(ByteBufferBuilder::close);
        this.clear = true;
    }

    @Override
    public void close()
    {
        this.closeAll();
    }

    static
    {
        EXPECTED_TOTAL_SIZE = BLOCK_LAYERS.stream().mapToInt(ChunkSectionLayer::bufferSize).sum() + RENDER_LAYERS.stream().mapToInt(RenderType::bufferSize).sum() + TYPES.stream().mapToInt(OverlayRenderType::getExpectedBufferSize).sum();
    }
}
