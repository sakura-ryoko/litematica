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
    protected static final int EXPECTED_TOTAL_SIZE = LAYERS.stream().mapToInt(RenderLayer::getExpectedBufferSize).sum() + TYPES.stream().mapToInt(ChunkRendererSchematicVbo.OverlayRenderType::getExpectedBufferSize).sum();
    private Map<RenderLayer, BufferAllocator> layerCache = new HashMap<>();
    private Map<ChunkRendererSchematicVbo.OverlayRenderType, BufferAllocator> overlayCache = new HashMap<>();

    public BufferAllocatorCache()
    {
        this.allocateBuffers();
    }

    public void allocateBuffers()
    {
        if (!this.layerCache.isEmpty())
        {
            this.layerCache.values().forEach(BufferAllocator::close);
            this.layerCache.clear();
        }
        if (!this.overlayCache.isEmpty())
        {
            this.overlayCache.values().forEach(BufferAllocator::close);
            this.overlayCache.clear();
        }

        this.layerCache = Util.make(new Reference2ObjectArrayMap<>(LAYERS.size()), refMap ->
        {
            for (RenderLayer layer : LAYERS)
            {
                refMap.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
            }
        });
        this.overlayCache = Util.make(new Reference2ObjectArrayMap<>(TYPES.size()), refMap ->
        {
            for (ChunkRendererSchematicVbo.OverlayRenderType type : TYPES)
            {
                refMap.put(type, new BufferAllocator(type.getExpectedBufferSize()));
            }
        });
    }

    public BufferAllocator getBufferByLayer(RenderLayer layer)
    {
        return this.layerCache.get(layer);
    }

    public BufferAllocator getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayCache.get(type);
    }

    public BufferAllocator recycleBufferByLayer(RenderLayer layer)
    {
        BufferAllocator newBuf = new BufferAllocator(layer.getExpectedBufferSize());

        try
        {
            if (this.layerCache.containsKey(layer))
            {
                this.layerCache.get(layer).reset();
                this.layerCache.get(layer).close();
            }
        }
        catch (Exception ignored) {}
        this.layerCache.put(layer, newBuf);

        return newBuf;
    }

    public BufferAllocator recycleBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        BufferAllocator newBuf = new BufferAllocator(type.getExpectedBufferSize());

        try
        {
            if (this.overlayCache.containsKey(type))
            {
                this.overlayCache.get(type).reset();
                this.overlayCache.get(type).close();
            }
        }
        catch (Exception ignored) {}
        this.overlayCache.put(type, newBuf);

        return newBuf;
    }

    public void resetByLayer(RenderLayer layer)
    {
        try
        {
            if (this.layerCache.containsKey(layer))
            {
                this.layerCache.get(layer).reset();
            }
        }
        catch (Exception ignored) { }
    }

    public void resetByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        try
        {
            if (this.overlayCache.containsKey(type))
            {
                this.overlayCache.get(type).reset();
            }
        }
        catch (Exception ignored) { }
    }

    public void clearByLayer(RenderLayer layer)
    {
        try
        {
            if (this.layerCache.containsKey(layer))
            {
                this.layerCache.get(layer).clear();
            }
        }
        catch (Exception ignored) { }
    }

    public void clearByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        try
        {
            if (this.overlayCache.containsKey(type))
            {
                this.overlayCache.get(type).clear();
            }
        }
        catch (Exception ignored) { }
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
        catch (Exception e)
        {
            this.layerCache.remove(layer);
        }
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
        catch (Exception e)
        {
            this.overlayCache.remove(type);
        }
    }

    public void resetAll()
    {
        try
        {
            this.layerCache.values().forEach(BufferAllocator::reset);
            this.overlayCache.values().forEach(BufferAllocator::reset);
        }
        catch (Exception ignored) {}
        this.clearAll();
    }

    public void clearAll()
    {
        try
        {
            this.layerCache.values().forEach(BufferAllocator::clear);
            this.overlayCache.values().forEach(BufferAllocator::clear);
        }
        catch (Exception ignored) {}
        this.layerCache.clear();
        this.overlayCache.clear();
    }

    public void closeAll()
    {
        try
        {
            this.layerCache.values().forEach(BufferAllocator::close);
            this.overlayCache.values().forEach(BufferAllocator::close);
        }
        catch (Exception ignored) {}
        this.layerCache.clear();
        this.overlayCache.clear();
    }

    @Override
    public void close()
    {
        Litematica.debugLog("BufferAllocatorCache: close()");

        this.closeAll();
    }
}
