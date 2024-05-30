package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.RenderLayer;

public class BufferBuilderCache
{
    private final Map<RenderLayer, BufferBuilderPatch> blockBufferBuilders = new HashMap<>();
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BufferBuilderPatch> overlayBufferBuilders = new HashMap<>();

    public BufferBuilderCache() { }

    public boolean hasBufferByLayer(RenderLayer layer)
    {
        return this.blockBufferBuilders.containsKey(layer);
    }

    public boolean hasBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayBufferBuilders.containsKey(type);
    }

    public BufferBuilderPatch getBufferByLayer(RenderLayer layer)
    {
        return this.blockBufferBuilders.get(layer);
    }

    public BufferBuilderPatch getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayBufferBuilders.get(type);
    }

    public void storeBufferByLayer(RenderLayer layer, @Nonnull BufferBuilderPatch buffer)
    {
        this.blockBufferBuilders.put(layer, buffer);
    }

    public void storeBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type, @Nonnull BufferBuilderPatch buffer)
    {
        this.overlayBufferBuilders.put(type, buffer);
    }

    public void clearByLayer(RenderLayer layer)
    {
        this.blockBufferBuilders.remove(layer);
    }

    public void clearByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        this.overlayBufferBuilders.remove(type);
    }

    public void clearAll()
    {
        this.blockBufferBuilders.clear();
        this.overlayBufferBuilders.clear();
    }

    public void close()
    {
        this.clearAll();
    }
}
