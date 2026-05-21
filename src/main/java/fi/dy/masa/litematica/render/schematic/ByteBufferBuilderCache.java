package fi.dy.masa.litematica.render.schematic;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ByteBufferBuilderCache implements AutoCloseable
{
    protected static final List<ChunkSectionLayer> BLOCK_LAYERS = ChunkRenderLayers.BLOCK_RENDER_LAYERS;
    protected static final List<OverlayRenderType> TYPES = ChunkRenderLayers.TYPES;
    protected static final int EXPECTED_TOTAL_SIZE;
    private final ConcurrentHashMap<ChunkSectionLayer, ByteBufferBuilder> blockCache;
    private final ConcurrentHashMap<OverlayRenderType, ByteBufferBuilder> overlayCache;
    private boolean clear;

    protected ByteBufferBuilderCache()
    {
	    this.blockCache = new ConcurrentHashMap<>(BLOCK_LAYERS.size(), 0.9f, 1);
		this.overlayCache = new ConcurrentHashMap<>(TYPES.size(), 0.9f, 1);
        this.clear = true;
    }

    private void allocateCache()
    {
        for (ChunkSectionLayer layer : BLOCK_LAYERS)
        {
            if (this.blockCache.containsKey(layer))
            {
                this.blockCache.get(layer).close();
            }

            this.blockCache.put(layer, new ByteBufferBuilder(layer.bufferSize()));
        }
        for (OverlayRenderType type : TYPES)
        {
            if (this.overlayCache.containsKey(type))
            {
                this.overlayCache.get(type).close();
            }

            this.overlayCache.put(type, new ByteBufferBuilder(type.getExpectedBufferSize()));
        }

        this.clear = true;
    }

    protected boolean hasBuffer(ChunkSectionLayer layer)
    {
        return this.blockCache.containsKey(layer);
    }

    protected boolean hasBuffer(OverlayRenderType type)
    {
        return this.overlayCache.containsKey(type);
    }

    protected ByteBufferBuilder getAllocator(ChunkSectionLayer layer)
    {
        this.clear = false;
        return this.blockCache.computeIfAbsent(layer, l -> new ByteBufferBuilder(l.bufferSize()));
    }

    protected ByteBufferBuilder getAllocator(OverlayRenderType type)
    {
        this.clear = false;
        return this.overlayCache.computeIfAbsent(type, t -> new ByteBufferBuilder(t.getExpectedBufferSize()));
    }

    protected void closeByBlockLayer(ChunkSectionLayer layer)
    {
        try
        {
            ByteBufferBuilder remove = this.blockCache.remove(layer);

            if (remove != null)
            {
                remove.close();
            }
        }
        catch (Exception ignored) {}
    }

    protected void closeByType(OverlayRenderType type)
    {
        try
        {
            ByteBufferBuilder remove = this.overlayCache.remove(type);

            if (remove != null)
            {
                remove.close();
            }
        }
        catch (Exception ignored) {}
    }

    protected boolean isClear() { return this.clear; }

    protected void resetAll()
    {
        try
        {
            this.blockCache.values().forEach(ByteBufferBuilder::discard);
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
            this.overlayCache.values().forEach(ByteBufferBuilder::clear);
        }
        catch (Exception ignored) { }

        this.clear = true;
    }

    protected void closeAll()
    {
        for (ByteBufferBuilder alloc : this.blockCache.values())
        {
            try
            {
                alloc.close();
            }
            catch (Exception ignored) { }
        }
        this.blockCache.clear();

        for (ByteBufferBuilder alloc : this.overlayCache.values())
        {
            try
            {
                alloc.close();
            }
            catch (Exception ignored) { }
        }
        this.overlayCache.clear();
        this.clear = true;
    }

    @Override
    public void close() throws Exception
    {
        this.closeAll();
    }

    static
    {
        EXPECTED_TOTAL_SIZE = BLOCK_LAYERS.stream().mapToInt(ChunkSectionLayer::bufferSize).sum() + TYPES.stream().mapToInt(OverlayRenderType::getExpectedBufferSize).sum();
    }
}
