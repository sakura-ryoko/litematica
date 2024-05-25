package fi.dy.masa.litematica.render.schematic;

import java.util.List;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import fi.dy.masa.litematica.Litematica;

public class SectionBufferCache implements AutoCloseable
{
    private static final List<RenderLayer> TYPES_1 = RenderLayer.getBlockLayers();
    private static final ChunkRendererSchematicVbo.OverlayRenderType[] TYPES_2 = ChunkRendererSchematicVbo.OverlayRenderType.values();
    private final Map<RenderLayer, BufferAllocator> layerCache;
    private final BufferAllocator[] overlayCache = new BufferAllocator[TYPES_2.length];

    public SectionBufferCache()
    {
        this.layerCache = Util.make(new Reference2ObjectArrayMap<>(TYPES_1.size()), refMap ->
        {
            for (RenderLayer layer : TYPES_1)
            {
                refMap.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
            }
        });

        for (int i = 0; i < TYPES_2.length; i++)
        {
            this.overlayCache[i] = new BufferAllocator(RenderLayer.DEFAULT_BUFFER_SIZE);
        }

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

        return this.overlayCache[type.ordinal()];
    }

    public void closeAll()
    {
        Litematica.logger.error("SectionBufferCache: closeAll()");
        this.close();
    }

    public void discardAll()
    {
        Litematica.logger.error("SectionBufferCache: discardAll()");

        this.layerCache.values().forEach(BufferAllocator::clear);
        for (int i = 0; i < TYPES_2.length; i++)
        {
            this.overlayCache[i].clear();
        }
    }

    @Override
    public void close()
    {
        Litematica.logger.error("SectionBufferCache: close()");

        this.layerCache.values().forEach(BufferAllocator::close);
        for (int i = 0; i < TYPES_2.length; i++)
        {
            this.overlayCache[i].close();
        }
    }
}
