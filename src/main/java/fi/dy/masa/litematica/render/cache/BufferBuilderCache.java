package fi.dy.masa.litematica.render.cache;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;

public class BufferBuilderCache implements AutoCloseable
{
    private final Map<RenderLayer, BufferBuilderPatch> blockBufferBuilders = new HashMap<>();
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BufferBuilderPatch> overlayBufferBuilders = new HashMap<>();

    public BufferBuilderCache()
    {
        Litematica.logger.error("BufferBuilderCache: <init>");
        this.allocateBuffers();
    }

    public void allocateBuffers()
    {
        if (!this.blockBufferBuilders.isEmpty())
        {
            this.blockBufferBuilders.clear();
        }
        if (!this.overlayBufferBuilders.isEmpty())
        {
            this.overlayBufferBuilders.clear();
        }

        /*
        for (RenderLayer layer : RenderLayer.getBlockLayers())
        {
            this.blockBufferBuilders.put(layer, new BufferBuilderPatch(new BufferAllocator(layer.getExpectedBufferSize()), layer.getDrawMode(), layer.getVertexFormat()));
        }
        for (ChunkRendererSchematicVbo.OverlayRenderType type : ChunkRendererSchematicVbo.OverlayRenderType.values())
        {
            this.overlayBufferBuilders.put(type, new BufferBuilderPatch(new BufferAllocator(type.getExpectedBufferSize()), type.getDrawMode(), type.getVertexFormat()));
        }
         */
    }

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
        //Litematica.logger.error("getBufferByLayer: for layer [{}]", layer.getDrawMode().name());

        return this.blockBufferBuilders.get(layer);
    }

    public BufferBuilderPatch getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        //Litematica.logger.error("getBufferByLayer: for type [{}]", type.getDrawMode().name());

        return this.overlayBufferBuilders.get(type);
    }

    public void storeBufferByLayer(RenderLayer layer, @Nonnull BufferBuilderPatch buffer)
    {
        Litematica.logger.error("storeBufferByLayer: for layer [{}]", layer.getDrawMode().name());

        this.blockBufferBuilders.put(layer, buffer);
    }

    public void storeBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type, @Nonnull BufferBuilderPatch buffer)
    {
        Litematica.logger.error("storeBufferByOverlay: for overlay type [{}]", type.getDrawMode().name());

        this.overlayBufferBuilders.put(type, buffer);
    }

    public BufferBuilderPatch recycleBufferByLayer(RenderLayer layer, @Nonnull BufferAllocator allocator)
    {
        BufferBuilderPatch newBuf = new BufferBuilderPatch(allocator, layer.getDrawMode(), layer.getVertexFormat());

        if (this.hasBufferByLayer(layer))
        {
            this.blockBufferBuilders.remove(layer);
        }

        this.storeBufferByLayer(layer, newBuf);

        return newBuf;
    }

    public BufferBuilderPatch recycleBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type, @Nonnull BufferAllocator allocator)
    {
        BufferBuilderPatch newBuf = new BufferBuilderPatch(allocator, type.getDrawMode(), type.getVertexFormat());

        if (this.hasBufferByOverlay(type))
        {
            this.overlayBufferBuilders.remove(type);
        }

        this.storeBufferByOverlay(type, newBuf);

        return newBuf;
    }

    public void clear()
    {
        Litematica.logger.error("BufferBuilderCache: clear()");

        this.blockBufferBuilders.clear();
        this.overlayBufferBuilders.clear();

        this.allocateBuffers();
    }

    @Override
    public void close() throws Exception
    {
        this.clear();
    }
}
