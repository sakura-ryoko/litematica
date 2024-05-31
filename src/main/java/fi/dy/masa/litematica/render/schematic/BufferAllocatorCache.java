package fi.dy.masa.litematica.render.schematic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import fi.dy.masa.litematica.Litematica;

public class BufferAllocatorCache implements AutoCloseable
{
    protected static final List<RenderLayer> LAYERS = ChunkRenderLayers.LAYERS;
    protected static final List<ChunkRendererSchematicVbo.OverlayRenderType> TYPES = ChunkRenderLayers.TYPES;
    protected static final int EXPECTED_TOTAL_SIZE;
    private Map<RenderLayer, BufferAllocator> layerCache = new HashMap<>();
    private Map<ChunkRendererSchematicVbo.OverlayRenderType, BufferAllocator> overlayCache = new HashMap<>();

    public BufferAllocatorCache()
    {
        //Litematica.logger.error("BufferAllocatorCache(): INIT");
    }

    public boolean hasBufferByLayer(RenderLayer layer)
    {
        return this.layerCache.containsKey(layer);
    }

    public boolean hasBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayCache.containsKey(type);
    }

    public BufferAllocator getBufferByLayer(RenderLayer layer)
    {
        if (this.layerCache.containsKey(layer) == false)
        {
            this.layerCache.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
        }

        return this.layerCache.get(layer);
    }

    public BufferAllocator getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        if (this.overlayCache.containsKey(type) == false)
        {
            this.overlayCache.put(type, new BufferAllocator(type.getExpectedBufferSize()));
        }

        return this.overlayCache.get(type);
    }

    public void closeByLayer(RenderLayer layer)
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

    public void closeByType(ChunkRendererSchematicVbo.OverlayRenderType type)
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

    public void resetAll()
    {
        //Litematica.logger.error("BufferAllocatorCache: resetAll()");

        try
        {
            this.layerCache.values().forEach(BufferAllocator::reset);
            this.overlayCache.values().forEach(BufferAllocator::reset);
        }
        catch (Exception ignored) { }
    }

    public void clearAll()
    {
        //Litematica.logger.error("BufferAllocatorCache: clearAll()");

        try
        {
            this.layerCache.values().forEach(BufferAllocator::clear);
            this.overlayCache.values().forEach(BufferAllocator::clear);
        }
        catch (Exception ignored) { }
    }

    public void closeAll()
    {
        //Litematica.logger.error("BufferAllocatorCache: closeAll()");

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
        //Litematica.logger.error("BufferAllocatorCache: close()");

        this.closeAll();
    }

    static
    {
        EXPECTED_TOTAL_SIZE = LAYERS.stream().mapToInt(RenderLayer::getExpectedBufferSize).sum() + TYPES.stream().mapToInt(ChunkRendererSchematicVbo.OverlayRenderType::getExpectedBufferSize).sum();
    }
}
