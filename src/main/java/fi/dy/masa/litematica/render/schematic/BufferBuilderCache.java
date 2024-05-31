package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.Litematica;

public class BufferBuilderCache
{
    private final Map<RenderLayer, BufferBuilderPatch> blockBufferBuilders = new HashMap<>();
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BufferBuilderPatch> overlayBufferBuilders = new HashMap<>();

    public BufferBuilderCache()
    {
        //Litematica.logger.error("BufferBuilderCache(): INIT");
    }

    public boolean hasBufferByLayer(RenderLayer layer)
    {
        return this.blockBufferBuilders.containsKey(layer);
    }

    public boolean hasBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayBufferBuilders.containsKey(type);
    }

    public BufferBuilderPatch getBufferByLayer(RenderLayer layer, @Nonnull BufferAllocatorCache allocators)
    {
        if (this.hasBufferByLayer(layer) == false)
        {
            this.blockBufferBuilders.put(layer, new BufferBuilderPatch(allocators.getBufferByLayer(layer), layer.getDrawMode(), layer.getVertexFormat()));
        }

        return this.blockBufferBuilders.get(layer);
    }

    public BufferBuilderPatch getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type, @Nonnull BufferAllocatorCache allocators)
    {
        if (this.hasBufferByOverlay(type) == false)
        {
            this.overlayBufferBuilders.put(type, new BufferBuilderPatch(allocators.getBufferByOverlay(type), type.getDrawMode(), type.getVertexFormat()));
        }

        return this.overlayBufferBuilders.get(type);
    }

    private void clear(BufferBuilderPatch buffer)
    {
        try
        {
            BuiltBuffer built = buffer.endNullable();

            if (built != null)
            {
                built.close();
            }
        }
        catch (Exception ignored) { }
    }

    public void clearAll()
    {
        //Litematica.logger.error("BufferBuilderCache(): clearAll()");

        this.blockBufferBuilders.forEach((layer, buffer) -> this.clear(buffer));
        this.overlayBufferBuilders.forEach((type, buffer) -> this.clear(buffer));
        this.blockBufferBuilders.clear();
        this.overlayBufferBuilders.clear();
    }
}
