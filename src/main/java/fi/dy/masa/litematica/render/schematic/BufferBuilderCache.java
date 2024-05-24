package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_9799;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class BufferBuilderCache
{
    private final Map<RenderLayer, BufferBuilder> blockBufferBuilders = new HashMap<>();
    private final Map<RenderLayer, class_9799> byteBufferBuilders = new HashMap<>();
    private final Map<RenderLayer, Double> offsetsLayers = new HashMap<>();
    private final Map<OverlayRenderType, BufferBuilder> overlayBlockBufferBuilders = new HashMap<>();
    private final Map<OverlayRenderType, class_9799> overlayByteBufferBuilders = new HashMap<>();
    private final Map<OverlayRenderType, Double> offsetsTypes = new HashMap<>();

    public BufferBuilderCache()
    {
        for (RenderLayer layer : RenderLayer.getBlockLayers())
        {
            this.byteBufferBuilders.putIfAbsent(layer, new class_9799(layer.getExpectedBufferSize()));
            this.blockBufferBuilders.putIfAbsent(layer, new BufferBuilder(new class_9799(layer.getExpectedBufferSize()), layer.getDrawMode(), layer.getVertexFormat()));
            this.offsetsLayers.putIfAbsent(layer, (double) 0);
        }
        for (OverlayRenderType type : OverlayRenderType.values())
        {
            this.overlayByteBufferBuilders.putIfAbsent(type, new class_9799(RenderLayer.DEFAULT_BUFFER_SIZE));
            this.overlayBlockBufferBuilders.putIfAbsent(type, new BufferBuilder(new class_9799(RenderLayer.DEFAULT_BUFFER_SIZE), type.getDrawMode(), VertexFormats.POSITION_COLOR));
            this.offsetsTypes.putIfAbsent(type, (double) 0);
        }
        Litematica.logger.error("BufferBuilderCache: <init>");
    }

    public boolean hasBlockBufferByLayer(RenderLayer layer)
    {
        return this.blockBufferBuilders.containsKey(layer);
    }

    public boolean hasBlockBufferByOverlay(OverlayRenderType type)
    {
        return this.overlayBlockBufferBuilders.containsKey(type);
    }

    public boolean hasByteBufferByLayer(RenderLayer layer)
    {
        return this.byteBufferBuilders.containsKey(layer);
    }

    public boolean hasByteBufferByOverlay(OverlayRenderType type)
    {
        return this.overlayByteBufferBuilders.containsKey(type);
    }

    public BufferBuilder getResultByLayer(RenderLayer layer)
    {
        if (!this.blockBufferBuilders.containsKey(layer))
        {
            Litematica.logger.error("getResultByLayer: layer [size: {}] entry is empty", layer.getExpectedBufferSize());
            this.storeResultByLayer(layer, new BufferBuilder(new class_9799(RenderLayer.DEFAULT_BUFFER_SIZE), layer.getDrawMode(), layer.getVertexFormat()));
        }

        return this.blockBufferBuilders.get(layer);
    }

    public BufferBuilder getResultByOverlay(OverlayRenderType type)
    {
        if (!this.overlayBlockBufferBuilders.containsKey(type))
        {
            Litematica.logger.error("getResultByOverlay: type [{}] entry is empty", type.getDrawMode().name());
            this.storeResultByOverlay(type, new BufferBuilder(new class_9799(RenderLayer.DEFAULT_BUFFER_SIZE), type.getDrawMode(), VertexFormats.POSITION_COLOR));
        }

        return this.overlayBlockBufferBuilders.get(type);
    }

    public class_9799 getByteBufferByLayer(RenderLayer layer)
    {
        if (!this.byteBufferBuilders.containsKey(layer))
        {
            Litematica.logger.error("getByteBufferByLayer: layer [size: {}] entry is empty", layer.getExpectedBufferSize());
            this.byteBufferBuilders.putIfAbsent(layer, new class_9799(RenderLayer.DEFAULT_BUFFER_SIZE));
        }

        return this.byteBufferBuilders.get(layer);
    }

    public class_9799 getByteBufferByOverlay(OverlayRenderType type)
    {
        if (!this.overlayByteBufferBuilders.containsKey(type))
        {
            Litematica.logger.error("getByteBufferByOverlay: type [{}] entry is empty", type.getDrawMode().name());
            this.overlayByteBufferBuilders.putIfAbsent(type, new class_9799(RenderLayer.DEFAULT_BUFFER_SIZE));
        }

        return this.overlayByteBufferBuilders.get(type);
    }


    public void storeResultByLayer(RenderLayer layer, @Nonnull BufferBuilder buffer)
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

    public void storeResultByOverlay(OverlayRenderType type, @Nonnull BufferBuilder buffer)
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

    public void setOffsetByLayer(RenderLayer layer, double offset)
    {
        this.offsetsLayers.putIfAbsent(layer, offset);
    }

    public void setOffsetByType(OverlayRenderType type, double offset)
    {
        this.offsetsTypes.putIfAbsent(type, offset);
    }

    public double getOffsetByLayer(RenderLayer layer)
    {
        return this.offsetsLayers.get(layer);
    }

    public double getOffsetByType(OverlayRenderType type)
    {
        return this.offsetsTypes.get(type);
    }

    public void clear()
    {
        Litematica.logger.error("BufferBuilderCache: clear()");

        this.byteBufferBuilders.values().forEach(class_9799::close);
        this.overlayByteBufferBuilders.values().forEach(class_9799::close);
        this.blockBufferBuilders.clear();
        this.overlayBlockBufferBuilders.clear();
        this.offsetsLayers.clear();
        this.offsetsTypes.clear();
    }
}
