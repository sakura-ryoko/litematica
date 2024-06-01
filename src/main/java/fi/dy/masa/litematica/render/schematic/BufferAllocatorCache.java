package fi.dy.masa.litematica.render.schematic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;

public class BufferAllocatorCache implements AutoCloseable
{
    protected static final List<RenderLayer> LAYERS = ChunkRenderLayers.LAYERS;
    protected static final List<ChunkRendererSchematicVbo.OverlayRenderType> TYPES = ChunkRenderLayers.TYPES;
    protected static final int EXPECTED_TOTAL_SIZE;
    private final Map<RenderLayer, BufferAllocator> layerCache = new HashMap<>();
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BufferAllocator> overlayCache = new HashMap<>();

    protected BufferAllocatorCache() { }

    protected boolean hasBufferByLayer(RenderLayer layer)
    {
        return this.layerCache.containsKey(layer);
    }

    protected boolean hasBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayCache.containsKey(type);
    }

    protected BufferAllocator getBufferByLayer(RenderLayer layer)
    {
        if (this.layerCache.containsKey(layer) == false)
        {
            this.layerCache.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
        }

        return this.layerCache.get(layer);
    }

    protected BufferAllocator getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        if (this.overlayCache.containsKey(type) == false)
        {
            this.overlayCache.put(type, new BufferAllocator(type.getExpectedBufferSize()));
        }

        return this.overlayCache.get(type);
    }

    protected void closeByLayer(RenderLayer layer)
    {
        try
        {
            if (this.layerCache.containsKey(layer))
            {
                this.layerCache.get(layer).close();
            }
        }
        catch (Exception ignored) { }
        this.layerCache.remove(layer);
    }

    protected void closeByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        try
        {
            if (this.overlayCache.containsKey(type))
            {
                this.overlayCache.get(type).close();
            }
        }
        catch (Exception ignored) { }
        this.overlayCache.remove(type);
    }

    protected void resetAll()
    {
        try
        {
            this.layerCache.values().forEach(BufferAllocator::reset);
            this.overlayCache.values().forEach(BufferAllocator::reset);
        }
        catch (Exception ignored) { }
    }

    protected void clearAll()
    {
        try
        {
            this.layerCache.values().forEach(BufferAllocator::clear);
            this.overlayCache.values().forEach(BufferAllocator::clear);
        }
        catch (Exception ignored) { }
    }

    protected void closeAll()
    {
        try
        {
            this.layerCache.values().forEach(BufferAllocator::close);
            this.overlayCache.values().forEach(BufferAllocator::close);
        }
        catch (Exception ignored) { }
        this.layerCache.clear();
        this.overlayCache.clear();
    }

    @Override
    public void close()
    {
        this.closeAll();
    }

    static
    {
        EXPECTED_TOTAL_SIZE = LAYERS.stream().mapToInt(RenderLayer::getExpectedBufferSize).sum() + TYPES.stream().mapToInt(ChunkRendererSchematicVbo.OverlayRenderType::getExpectedBufferSize).sum();
    }
}
