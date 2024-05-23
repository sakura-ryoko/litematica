package fi.dy.masa.litematica.render.schematic;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class BufferBuilderCache
{
    private final Map<RenderLayer, OmegaBufferBuilder> blockBufferBuilders = new HashMap<>();
    private final OmegaBufferBuilder[] overlayBufferBuilders;

    public BufferBuilderCache()
    {
        for (RenderLayer layer : RenderLayer.getBlockLayers())
        {
            this.blockBufferBuilders.put(layer, new OmegaBufferBuilder(layer.getExpectedBufferSize()));
        }

        this.overlayBufferBuilders = new OmegaBufferBuilder[OverlayRenderType.values().length];

        for (int i = 0; i < this.overlayBufferBuilders.length; ++i)
        {
            this.overlayBufferBuilders[i] = new OmegaBufferBuilder(262144);
        }
    }

    public OmegaBufferBuilder getBlockBufferByLayer(RenderLayer layer)
    {
        return this.blockBufferBuilders.get(layer);
    }

    public OmegaBufferBuilder getOverlayBuffer(OverlayRenderType type)
    {
        return this.overlayBufferBuilders[type.ordinal()];
    }

    public void clear()
    {
        this.blockBufferBuilders.values().forEach(BufferBuilder::reset);

        for (BufferBuilder buffer : this.overlayBufferBuilders)
        {
            buffer.reset();
        }
    }
}
