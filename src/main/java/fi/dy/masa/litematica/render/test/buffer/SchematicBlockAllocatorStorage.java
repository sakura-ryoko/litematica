package fi.dy.masa.litematica.render.test.buffer;

import java.util.*;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.test.SchematicOverlayType;

public class SchematicBlockAllocatorStorage implements AutoCloseable
{
    private static final List<RenderLayer> LAYERS = RenderLayer.getBlockLayers();
    private static final List<SchematicOverlayType> TYPES = Arrays.stream(SchematicOverlayType.values()).toList();
    public static final int EXPECTED_TOTAL_SIZE;
    private final Map<RenderLayer, BufferAllocator> layerAllocators;
    private final Map<SchematicOverlayType, BufferAllocator> overlayAllocators;

    public SchematicBlockAllocatorStorage()
    {
        this.layerAllocators = Util.make(new Reference2ObjectArrayMap<>(LAYERS.size()), (refMap) ->
        {

            for (RenderLayer layer : LAYERS)
            {
                refMap.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
            }
        });
        this.overlayAllocators = Util.make(new Reference2ObjectArrayMap<>(TYPES.size()), (refMap) ->
        {

            for (SchematicOverlayType type : TYPES)
            {
                refMap.put(type, new BufferAllocator(type.getExpectedBufferSize()));
            }
        });
    }

    public BufferAllocator getBufferByLayer(RenderLayer layer)
    {
        return this.layerAllocators.get(layer);
    }

    public BufferAllocator getBufferByOverlay(SchematicOverlayType type)
    {
        return this.overlayAllocators.get(type);
    }

    public void clear()
    {
        Litematica.logger.error("SchematicBlockAllocatorStorage: clear()");

        this.layerAllocators.values().forEach(BufferAllocator::clear);
        this.overlayAllocators.values().forEach(BufferAllocator::clear);
    }

    public void reset()
    {
        Litematica.logger.error("SchematicBlockAllocatorStorage: reset()");

        this.layerAllocators.values().forEach(BufferAllocator::reset);
        this.overlayAllocators.values().forEach(BufferAllocator::reset);
    }

    public void close()
    {
        Litematica.logger.error("SchematicBlockAllocatorStorage: close()");

        this.layerAllocators.values().forEach(BufferAllocator::close);
        this.overlayAllocators.values().forEach(BufferAllocator::close);
    }

    static
    {
        EXPECTED_TOTAL_SIZE = LAYERS.stream().mapToInt(RenderLayer::getExpectedBufferSize).sum() + TYPES.stream().mapToInt(SchematicOverlayType::getExpectedBufferSize).sum();
    }
}
