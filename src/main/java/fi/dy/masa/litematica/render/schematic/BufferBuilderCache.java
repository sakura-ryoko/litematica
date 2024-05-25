package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
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
            this.blockBufferBuilders.putIfAbsent(layer, new BufferBuilderPatch(new BufferAllocator(layer.getExpectedBufferSize()), layer.getDrawMode(), layer.getVertexFormat()));
        }
        for (OverlayRenderType type : OverlayRenderType.values())
        {
            this.overlayBlockBufferBuilders.putIfAbsent(type, new BufferBuilderPatch(new BufferAllocator(RenderLayer.DEFAULT_BUFFER_SIZE), type.getDrawMode(), VertexFormats.POSITION_COLOR));
        }
        Litematica.logger.error("BufferBuilderCache: <init>");
    }

    public boolean hasBufferByLayer(RenderLayer layer)
    {
        return this.blockBufferBuilders.containsKey(layer);
    }

    public boolean hasBufferByOverlay(OverlayRenderType type)
    {
        return this.overlayBlockBufferBuilders.containsKey(type);
    }

    public BufferBuilderPatch getBufferByLayer(RenderLayer layer)
    {
        Litematica.logger.warn("getBufferByLayer(): layer [{}] has {}", layer.getDrawMode().name(), this.hasBufferByLayer(layer));

        return this.blockBufferBuilders.get(layer);
    }

    public BufferBuilderPatch getBufferByOverlay(OverlayRenderType type)
    {
        Litematica.logger.warn("getBufferByOverlay(): type [{}] has {}", type.getDrawMode().name(), this.hasBufferByOverlay(type));

        return this.overlayBlockBufferBuilders.get(type);
    }

    public void storeBufferByLayer(RenderLayer layer, @Nonnull BufferBuilderPatch buffer)
    {
        Litematica.logger.warn("storeBufferByLayer(): layer [{}] has {}", layer.getDrawMode().name(), this.hasBufferByLayer(layer));

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
        Litematica.logger.warn("storeBufferByOverlay(): type [{}] has {}", type.getDrawMode().name(), this.hasBufferByOverlay(type));

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
