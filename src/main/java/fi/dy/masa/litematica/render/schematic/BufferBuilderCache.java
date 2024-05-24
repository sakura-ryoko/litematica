package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_9799;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class BufferBuilderCache
{
    private final Map<RenderLayer, BufferBuilderPatch> blockBufferBuilders = new HashMap<>();
    private final Map<OverlayRenderType, BufferBuilderPatch> overlayBlockBufferBuilders = new HashMap<>();

    public BufferBuilderCache()
    {
        for (RenderLayer layer : RenderLayer.getBlockLayers())
        {
            this.blockBufferBuilders.putIfAbsent(layer, new BufferBuilderPatch(new class_9799(layer.getExpectedBufferSize()), layer.getDrawMode(), layer.getVertexFormat()));
        }
        for (OverlayRenderType type : OverlayRenderType.values())
        {
            this.overlayBlockBufferBuilders.putIfAbsent(type, new BufferBuilderPatch(new class_9799(RenderLayer.DEFAULT_BUFFER_SIZE), type.getDrawMode(), VertexFormats.POSITION_COLOR));
        }
        Litematica.logger.error("BufferBuilderCache: <init>");
    }

    public BufferBuilderPatch getBufferByLayer(RenderLayer layer)
    {
        if (!this.blockBufferBuilders.containsKey(layer))
        {
            Litematica.logger.error("getResultByLayer: layer [size: {}] entry is empty", layer.getExpectedBufferSize());
            this.storeBufferByLayer(layer, new BufferBuilderPatch(new class_9799(RenderLayer.DEFAULT_BUFFER_SIZE), layer.getDrawMode(), layer.getVertexFormat()));
        }

        return this.blockBufferBuilders.get(layer);
    }

    public BufferBuilderPatch getBufferByOverlay(OverlayRenderType type)
    {
        if (!this.overlayBlockBufferBuilders.containsKey(type))
        {
            Litematica.logger.error("getResultByOverlay: type [{}] entry is empty", type.getDrawMode().name());
            this.storeBufferByOverlay(type, new BufferBuilderPatch(new class_9799(RenderLayer.DEFAULT_BUFFER_SIZE), type.getDrawMode(), VertexFormats.POSITION_COLOR));
        }

        return this.overlayBlockBufferBuilders.get(type);
    }

    public void storeBufferByLayer(RenderLayer layer, @Nonnull BufferBuilderPatch buffer)
    {
        if (this.blockBufferBuilders.containsKey(layer))
        {
            this.blockBufferBuilders.replace(layer, buffer);
        }
        else
        {
            this.blockBufferBuilders.put(layer, buffer);
        }
    }

    public void storeBufferByOverlay(OverlayRenderType type, @Nonnull BufferBuilderPatch buffer)
    {
        if (this.overlayBlockBufferBuilders.containsKey(type))
        {
            this.overlayBlockBufferBuilders.replace(type, buffer);
        }
        else
        {
            this.overlayBlockBufferBuilders.put(type, buffer);
        }
    }

    public void clear()
    {
        Litematica.logger.error("BufferBuilderCache: clear()");

        this.blockBufferBuilders.clear();
        this.overlayBlockBufferBuilders.clear();
    }
}
