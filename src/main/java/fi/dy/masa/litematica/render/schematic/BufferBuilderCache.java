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
		this.blockBufferBuilders = new ConcurrentHashMap<>(BufferAllocatorCache.BLOCK_LAYERS.size(), 0.9f, 1);
		this.layerBufferBuilders = new ConcurrentHashMap<>(BufferAllocatorCache.RENDER_LAYERS.size(), 0.9f, 1);
		this.overlayBufferBuilders = new ConcurrentHashMap<>(BufferAllocatorCache.TYPES.size(), 0.9f, 1);
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

    protected BufferBuilder getBufferByBlockLayer(ChunkSectionLayer layer, @Nonnull BufferAllocatorCache allocators)
    {
        return this.blockBufferBuilders.computeIfAbsent(layer, (key) -> new BufferBuilder(allocators.getBufferByBlockLayer(key), key.pipeline().getVertexFormatMode(), key.pipeline().getVertexFormat()));
    }

    protected BufferBuilder getBufferByLayer(RenderType layer, @Nonnull BufferAllocatorCache allocators)
    {
        return this.layerBufferBuilders.computeIfAbsent(layer, (key) -> new BufferBuilder(allocators.getBufferByLayer(key), key.mode(), key.format()));
    }

    protected BufferBuilder getBufferByOverlay(OverlayRenderType type, @Nonnull BufferAllocatorCache allocators)
    {
        return this.overlayBufferBuilders.computeIfAbsent(type, (key) -> new BufferBuilder(allocators.getBufferByOverlay(key), key.getDrawMode(), key.getVertexFormat()));
    }

    protected void clearAll()
    {
        for (BufferBuilder buffer : this.blockBufferBuilders.values())
        {
            if (((IMixinBufferBuilder) buffer).malilib_isBuilding())
            {
                MeshData built = buffer.build();
                if (built != null) { built.close(); }
            }
        }
        this.blockBufferBuilders.clear();

        for (BufferBuilder buffer : this.layerBufferBuilders.values())
        {
            if (((IMixinBufferBuilder) buffer).malilib_isBuilding())
            {
                MeshData built = buffer.build();
                if (built != null) { built.close(); }
            }
        }
        this.layerBufferBuilders.clear();

        for (BufferBuilder buffer : this.overlayBufferBuilders.values())
        {
            if (((IMixinBufferBuilder) buffer).malilib_isBuilding())
            {
                MeshData built = buffer.build();
                if (built != null) { built.close(); }
            }
        }
        this.overlayBufferBuilders.clear();
    }

    @Override
    public void close() throws Exception
    {
        this.clearAll();
    }
}
