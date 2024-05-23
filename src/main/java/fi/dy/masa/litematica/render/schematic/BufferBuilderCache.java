package fi.dy.masa.litematica.render.schematic;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_9799;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class BufferBuilderCache
{
    private final Map<RenderLayer, class_9799> blockBufferBuilders = new HashMap<>();
    private final class_9799[] overlayBufferBuilders;

    public BufferBuilderCache()
    {
        for (RenderLayer layer : RenderLayer.getBlockLayers())
        {
            this.blockBufferBuilders.put(layer, new class_9799(layer.getExpectedBufferSize()));
        }

        this.overlayBufferBuilders = new class_9799[OverlayRenderType.values().length];

        for (int i = 0; i < this.overlayBufferBuilders.length; ++i)
        {
            this.overlayBufferBuilders[i] = new class_9799(262144);
        }
    }

    public class_9799 getBlockBufferByLayer(RenderLayer layer)
    {
        return this.blockBufferBuilders.get(layer);
    }

    public class_9799 getOverlayBuffer(OverlayRenderType type)
    {
        return this.overlayBufferBuilders[type.ordinal()];
    }

    public void clear()
    {
        this.blockBufferBuilders.values().forEach(class_9799::close);

        for (class_9799 buffer : this.overlayBufferBuilders)
        {
            buffer.close();
        }
    }
}
