package fi.dy.masa.litematica.render.schematic;

import java.util.List;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.class_9799;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Util;

public class SectionBufferCache implements AutoCloseable
{
    private static final List<RenderLayer> TYPES_1 = RenderLayer.getBlockLayers();
    private static final ChunkRendererSchematicVbo.OverlayRenderType[] TYPES_2 = ChunkRendererSchematicVbo.OverlayRenderType.values();
    private final Map<RenderLayer, class_9799> layerCache;
    private final class_9799[] overlayCache = new class_9799[TYPES_2.length];

    public SectionBufferCache()
    {
        this.layerCache = Util.make(new Reference2ObjectArrayMap<>(TYPES_1.size()), refMap ->
        {
            for (RenderLayer layer : TYPES_1)
            {
                refMap.put(layer, new class_9799(layer.getExpectedBufferSize()));
            }
        });

        for (int i = 0; i < TYPES_2.length; i++)
        {
            this.overlayCache[i] = new class_9799(RenderLayer.DEFAULT_BUFFER_SIZE);
        }
    }

    public class_9799 getBufferByLayer(RenderLayer layer)
    {
        return this.layerCache.get(layer);
    }

    public class_9799 getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayCache[type.ordinal()];
    }

    public void closeAll()
    {
        this.layerCache.values().forEach(class_9799::close);
        for (int i = 0; i < TYPES_2.length; i++)
        {
            this.overlayCache[i].close();
        }
    }

    public void discardAll()
    {
        this.layerCache.values().forEach(class_9799::method_60811);
        for (int i = 0; i < TYPES_2.length; i++)
        {
            this.overlayCache[i].method_60811();
        }
    }

    @Override
    public void close()
    {
        this.layerCache.values().forEach(class_9799::close);
        for (int i = 0; i < TYPES_2.length; i++)
        {
            this.overlayCache[i].close();
        }
    }
}
