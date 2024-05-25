package fi.dy.masa.litematica.render.cache;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class BufferBuilderCache implements AutoCloseable
{
    private final Map<RenderLayer, BufferBuilderPatch> blockBufferBuilders = new HashMap<>();
    private final Map<OverlayRenderType, BufferBuilderPatch> overlayBlockBufferBuilders = new HashMap<>();

    public BufferBuilderCache()
    {
        /*
        for (RenderLayer layer : RenderLayer.getBlockLayers())
        {
            this.blockBufferBuilders.putIfAbsent(layer, new BufferBuilderPatch(new BufferAllocator(layer.getExpectedBufferSize()), layer.getDrawMode(), layer.getVertexFormat()));
        }
        for (OverlayRenderType type : OverlayRenderType.values())
        {
            this.overlayBlockBufferBuilders.putIfAbsent(type, new BufferBuilderPatch(new BufferAllocator(RenderLayer.DEFAULT_BUFFER_SIZE), type.getDrawMode(), VertexFormats.POSITION_COLOR));
        }
         */
        Litematica.logger.error("BufferBuilderCache: <init>");
    }

    public BufferBuilderPatch getBufferByLayer(RenderLayer layer)
    {
        Litematica.logger.error("getBufferByLayer: for layer [{}]", layer.getDrawMode().name());

        return this.blockBufferBuilders.get(layer);
    }

    public BufferBuilderPatch getBufferByOverlay(OverlayRenderType type)
    {
        Litematica.logger.error("getBufferByLayer: for type [{}]", type.getDrawMode().name());

        return this.overlayBlockBufferBuilders.get(type);
    }

    public void storeBufferByLayer(RenderLayer layer, @Nonnull BufferBuilderPatch buffer)
    {
        this.blockBufferBuilders.put(layer, buffer);
    }

    public void storeBufferByOverlay(OverlayRenderType type, @Nonnull BufferBuilderPatch buffer)
    {
        this.overlayBlockBufferBuilders.put(type, buffer);
    }

    public void clear()
    {
        Litematica.logger.error("BufferBuilderCache: clear()");

        this.blockBufferBuilders.clear();
        this.overlayBlockBufferBuilders.clear();
    }

    @Override
    public void close() throws Exception
    {
        this.clear();
    }
}
