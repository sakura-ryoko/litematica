package fi.dy.masa.litematica.render.cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;

public class BufferAllocatorCache implements AutoCloseable
{
    private static final List<RenderLayer> LAYERS = RenderLayer.getBlockLayers();
    private static final List<ChunkRendererSchematicVbo.OverlayRenderType> TYPES = Arrays.stream(ChunkRendererSchematicVbo.OverlayRenderType.values()).toList();
    private Map<RenderLayer, BufferAllocator> layerCache = new HashMap<>();
    private Map<ChunkRendererSchematicVbo.OverlayRenderType, BufferAllocator> overlayCache = new HashMap<>();

    public BufferAllocatorCache()
    {
        Litematica.logger.error("BufferAllocatorCache: init()");
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
        //Litematica.logger.error("getBufferByLayer: layer {}", layer.getDrawMode().name());

        return this.layerCache.get(layer);
    }

    public BufferAllocator getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        //Litematica.logger.error("getBufferByOverlay: type {}", type.getDrawMode().name());

        return this.overlayCache.get(type);
    }

    public BufferAllocator recycleBufferByLayer(RenderLayer layer)
    {
        BufferAllocator newBuf = new BufferAllocator(layer.getExpectedBufferSize());

        this.layerCache.get(layer).close();
        this.layerCache.put(layer, newBuf);

        return newBuf;
    }

    public BufferAllocator recycleBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        BufferAllocator newBuf = new BufferAllocator(type.getExpectedBufferSize());

        this.overlayCache.get(type).close();
        this.overlayCache.put(type, newBuf);

        return newBuf;
    }

    public void reset()
    {
        Litematica.logger.warn("BufferAllocatorCache: reset()");

        this.layerCache.values().forEach(BufferAllocator::reset);
        this.overlayCache.values().forEach(BufferAllocator::reset);
        this.clear();
    }

    public void clear()
    {
        Litematica.logger.warn("BufferAllocatorCache: clear()");

        this.layerCache.values().forEach(BufferAllocator::clear);
        this.overlayCache.values().forEach(BufferAllocator::clear);
        this.layerCache.clear();
        this.overlayCache.clear();

        this.allocateBuffers();
    }

    public void closeAll()
    {
        this.layerCache.values().forEach(BufferAllocator::close);
        this.overlayCache.values().forEach(BufferAllocator::close);
        this.layerCache.clear();
        this.overlayCache.clear();

        this.allocateBuffers();
    }

    @Override
    public void close()
    {
        Litematica.logger.error("BufferAllocatorCache: close()");
        this.closeAll();
    }
}
