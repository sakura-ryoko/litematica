package fi.dy.masa.litematica.render.cache;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.schematic.org.ChunkRendererSchematicVbo;

public class SectionBufferCache implements AutoCloseable
{
    private static final List<RenderLayer> LAYERS = RenderLayer.getBlockLayers();
    private static final List<ChunkRendererSchematicVbo.OverlayRenderType> TYPES = Arrays.stream(ChunkRendererSchematicVbo.OverlayRenderType.values()).toList();
    private final Map<RenderLayer, BufferAllocator> layerCache = Util.make(new Reference2ObjectArrayMap<>(LAYERS.size()), refMap ->
    {
        for (RenderLayer layer : LAYERS)
        {
            refMap.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
        }
    });
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BufferAllocator> overlayCache = Util.make(new Reference2ObjectArrayMap<>(TYPES.size()), refMap ->
    {
        for (ChunkRendererSchematicVbo.OverlayRenderType type : TYPES)
        {
            refMap.put(type, new BufferAllocator(type.getExpectedBufferSize()));
        }
    });

    public SectionBufferCache()
    {
        Litematica.logger.error("SectionBufferCache: init()");
    }

    public BufferAllocator getBufferByLayer(RenderLayer layer)
    {
        Litematica.logger.error("getBufferByLayer: layer {}", layer.getDrawMode().name());

        return this.layerCache.get(layer);
    }

    public BufferAllocator getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        Litematica.logger.error("getBufferByOverlay: type {}", type.getDrawMode().name());

        return this.overlayCache.get(type);
    }

    public void closeAll()
    {
        Litematica.logger.error("SectionBufferCache: closeAll()");
        this.close();
    }

    public void clearAll()
    {
        Litematica.logger.error("SectionBufferCache: clearAll()");

        this.layerCache.values().forEach(BufferAllocator::clear);
        this.overlayCache.values().forEach(BufferAllocator::clear);
    }

    @Override
    public void close()
    {
        Litematica.logger.error("SectionBufferCache: close()");

        this.clearAll();
    }
}
