package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class BuilderResultCache
{
    private final Map<RenderLayer, BufferBuilder> blockBufferBuilders = new HashMap<>();
    private final Map<OverlayRenderType, BufferBuilder> overlayBlockBufferBuilders = new HashMap<>();
    private final Map<RenderLayer, Double> offsetsLayers = new HashMap<>();
    private final Map<OverlayRenderType, Double> offsetsTypes = new HashMap<>();

    public BuilderResultCache() { }

    public boolean hasResultByLayer(RenderLayer layer)
    {
        return this.blockBufferBuilders.containsKey(layer);
    }

    public boolean hasResultByOverlayType(OverlayRenderType type)
    {
        return this.overlayBlockBufferBuilders.containsKey(type);
    }

    public BufferBuilder getResultByLayer(RenderLayer layer)
    {
        return this.blockBufferBuilders.get(layer);
    }

    public BufferBuilder getResultByOverlayType(OverlayRenderType type)
    {
        return this.overlayBlockBufferBuilders.get(type);
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

    public void storeResultByOverlayType(OverlayRenderType type, @Nonnull BufferBuilder buffer)
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
        this.blockBufferBuilders.clear();
        this.overlayBlockBufferBuilders.clear();
        this.offsetsLayers.clear();
        this.offsetsTypes.clear();
    }
}
