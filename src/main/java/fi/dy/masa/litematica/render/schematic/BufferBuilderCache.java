package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;

public class BufferBuilderCache implements AutoCloseable
{
    private final ConcurrentHashMap<RenderLayer, BufferBuilderPatch> blockBufferBuilders = new ConcurrentHashMap<>();
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BufferBuilderPatch> overlayBufferBuilders = new ConcurrentHashMap<>();

    protected BufferBuilderCache() { }

    protected boolean hasBufferByLayer(RenderLayer layer)
    {
        return blockBufferBuilders.containsKey(layer);
    }

    protected boolean hasBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return overlayBufferBuilders.containsKey(type);
    }

    protected BufferBuilderPatch getBufferByLayer(RenderLayer layer, @Nonnull BufferAllocatorCache allocators)
    {
        return blockBufferBuilders.computeIfAbsent(layer, (key) -> new BufferBuilderPatch(allocators.getBufferByLayer(key), key.getDrawMode(), key.getVertexFormat()));
    }

    protected BufferBuilderPatch getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type, @Nonnull BufferAllocatorCache allocators)
    {
        return overlayBufferBuilders.computeIfAbsent(type,(key) -> new BufferBuilderPatch(allocators.getBufferByOverlay(key), key.getDrawMode(), key.getVertexFormat()));
    }

    protected void clearAll()
    {
        ArrayList<BufferBuilderPatch> buffers;
        synchronized (blockBufferBuilders) {
            buffers = new ArrayList<>(blockBufferBuilders.values());
            blockBufferBuilders.clear();
        }
        synchronized (overlayBufferBuilders) {
            buffers.addAll(overlayBufferBuilders.values());
            overlayBufferBuilders.clear();
        }
        for(BufferBuilderPatch buffer:buffers) {
            try{
                BuiltBuffer built = buffer.endNullable();
                if(built!=null)
                    built.close();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void close() throws Exception
    {
        this.clearAll();
    }
}
