package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.mixin.render.IMixinBufferBuilder;

public class BufferBuilderCache implements AutoCloseable
{
    private final ConcurrentHashMap<ChunkSectionLayer, BufferBuilder> blockBufferBuilders;
    private final ConcurrentHashMap<RenderType, BufferBuilder> layerBufferBuilders;
    private final ConcurrentHashMap<OverlayRenderType, BufferBuilder> overlayBufferBuilders;

    protected BufferBuilderCache()
    {
		this.blockBufferBuilders = new ConcurrentHashMap<>(ByteBufferBuilderCache.BLOCK_LAYERS.size(), 0.9f, 1);
		this.layerBufferBuilders = new ConcurrentHashMap<>(ByteBufferBuilderCache.RENDER_LAYERS.size(), 0.9f, 1);
		this.overlayBufferBuilders = new ConcurrentHashMap<>(ByteBufferBuilderCache.TYPES.size(), 0.9f, 1);
    }

    protected boolean hasBufferByBlockLayer(ChunkSectionLayer layer)
    {
        return this.blockBufferBuilders.containsKey(layer);
    }

    protected boolean hasBufferByLayer(RenderType layer)
    {
        return this.layerBufferBuilders.containsKey(layer);
    }

    protected boolean hasBufferByOverlay(OverlayRenderType type)
    {
        return this.overlayBufferBuilders.containsKey(type);
    }

    protected BufferBuilder getBufferByBlockLayer(ChunkSectionLayer layer, @Nonnull ByteBufferBuilderCache allocators)
    {
        synchronized (this.blockBufferBuilders)
        {
            return this.blockBufferBuilders.computeIfAbsent(layer, (key) -> new BufferBuilder(allocators.getBufferByBlockLayer(key), key.pipeline().getVertexFormatMode(), key.pipeline().getVertexFormat()));
        }
    }

    protected BufferBuilder getBufferByLayer(RenderType layer, @Nonnull ByteBufferBuilderCache allocators)
    {
        synchronized (this.layerBufferBuilders)
        {
            return this.layerBufferBuilders.computeIfAbsent(layer, (key) -> new BufferBuilder(allocators.getBufferByLayer(key), key.mode(), key.format()));
        }
    }

    protected BufferBuilder getBufferByOverlay(OverlayRenderType type, @Nonnull ByteBufferBuilderCache allocators)
    {
        synchronized (this.overlayBufferBuilders)
        {
            return this.overlayBufferBuilders.computeIfAbsent(type, (key) -> new BufferBuilder(allocators.getBufferByOverlay(key), key.getDrawMode(), key.getVertexFormat()));
        }
    }

    protected void clearAll()
    {
        ArrayList<BufferBuilder> buffers;

        synchronized (this.blockBufferBuilders)
        {
            buffers = new ArrayList<>(this.blockBufferBuilders.values());
            this.blockBufferBuilders.clear();
        }
        synchronized (this.layerBufferBuilders)
        {
            buffers.addAll(this.layerBufferBuilders.values());
            this.layerBufferBuilders.clear();
        }
        synchronized (this.overlayBufferBuilders)
        {
            buffers.addAll(this.overlayBufferBuilders.values());
            this.overlayBufferBuilders.clear();
        }
        for (BufferBuilder buffer : buffers)
        {
            if (!((IMixinBufferBuilder) buffer).malilib_isBuilding())
			{
                continue;
			}
			
            MeshData built = buffer.build();
			
            if (built != null)
            {
                built.close();
            }
        }
    }

    @Override
    public void close() throws Exception
    {
        this.clearAll();
    }
}
