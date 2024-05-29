package fi.dy.masa.litematica.render.test.buffer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.test.SchematicOverlayType;

public class BlockBufferCache implements AutoCloseable
{
    private static final List<RenderLayer> LAYERS = RenderLayer.getBlockLayers();
    private static final List<SchematicOverlayType> TYPES = Arrays.stream(SchematicOverlayType.values()).toList();
    public static final int TOTAL_SIZE = LAYERS.stream().mapToInt(RenderLayer::getExpectedBufferSize).sum() + TYPES.stream().mapToInt(SchematicOverlayType::getExpectedBufferSize).sum();
    private final Map<RenderLayer, BufferAllocator> layerCache = Util.make(new Reference2ObjectArrayMap<>(LAYERS.size()), refMap ->
    {
        for (RenderLayer layer : LAYERS)
        {
            refMap.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
        }
    });
    private final Map<SchematicOverlayType, BufferAllocator> overlayCache = Util.make(new Reference2ObjectArrayMap<>(TYPES.size()), refMap ->
    {
        for (SchematicOverlayType type : TYPES)
        {
            refMap.put(type, new BufferAllocator(type.getExpectedBufferSize()));
        }
    });

    public BlockBufferCache()
    {
        Litematica.logger.error("BlockBufferCache: init()");
    }

    public BufferAllocator getBufferByLayer(RenderLayer layer)
    {
        Litematica.logger.error("getBufferByLayer: layer {}", layer.getDrawMode().name());

        return this.layerCache.get(layer);
    }

    public BufferAllocator getBufferByOverlay(SchematicOverlayType type)
    {
        Litematica.logger.error("getBufferByOverlay: type {}", type.getDrawMode().name());

        return this.overlayCache.get(type);
    }

    public void clear()
    {
        Litematica.logger.error("BlockBufferCache: clear()");
        this.close();
    }

    public void reset()
    {
        Litematica.logger.error("BlockBufferCache: reset()");

        this.layerCache.values().forEach(BufferAllocator::clear);
        this.overlayCache.values().forEach(BufferAllocator::clear);
    }

    @Override
    public void close()
    {
        Litematica.logger.error("BlockBufferCache: close()");

        this.reset();
    }
}
