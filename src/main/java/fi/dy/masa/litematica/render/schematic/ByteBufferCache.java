package fi.dy.masa.litematica.render.schematic;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_9799;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

/**
 * Same as BuilderResultCache, but for the new ByteBufferBuilder's that everything now uses
 */
public class ByteBufferCache
{
    private final Map<RenderLayer, class_9799> byteBufferBuilders = new HashMap<>();
    private final class_9799[] overlayByteBufferBuilders;

    public ByteBufferCache()
    {
        for (RenderLayer layer : RenderLayer.getBlockLayers())
        {
            this.byteBufferBuilders.put(layer, new class_9799(layer.getExpectedBufferSize()));
        }

        this.overlayByteBufferBuilders = new class_9799[OverlayRenderType.values().length];

        for (int i = 0; i < this.overlayByteBufferBuilders.length; ++i)
        {
            this.overlayByteBufferBuilders[i] = new class_9799(262144);
        }
    }

    public class_9799 getBlockBufferByLayer(RenderLayer layer)
    {
        return this.byteBufferBuilders.get(layer);
    }

    public class_9799 getOverlayBuffer(OverlayRenderType type)
    {
        return this.overlayByteBufferBuilders[type.ordinal()];
    }

    public void clear()
    {
        this.byteBufferBuilders.values().forEach(class_9799::close);

        for (class_9799 buffer : this.overlayByteBufferBuilders)
        {
            buffer.close();
        }
    }
}
