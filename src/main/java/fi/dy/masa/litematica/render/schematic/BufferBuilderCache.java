package fi.dy.masa.litematica.render.schematic;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import fi.dy.masa.malilib.mixin.render.IMixinBufferBuilder;

public class BufferBuilderCache implements AutoCloseable
{
    private final ConcurrentHashMap<ChunkSectionLayer, BufferBuilder> blockBufferBuilders;
    private final ConcurrentHashMap<OverlayRenderType, BufferBuilder> overlayBufferBuilders;

    protected BufferBuilderCache()
    {
		this.blockBufferBuilders = new ConcurrentHashMap<>(ByteBufferBuilderCache.BLOCK_LAYERS.size(), 0.9f, 1);
		this.overlayBufferBuilders = new ConcurrentHashMap<>(ByteBufferBuilderCache.TYPES.size(), 0.9f, 1);
    }

    protected boolean hasBuilder(ChunkSectionLayer layer)
    {
        return this.blockBufferBuilders.containsKey(layer);
    }

    protected boolean hasBuilder(OverlayRenderType type)
    {
        return this.overlayBufferBuilders.containsKey(type);
    }

    protected BufferBuilder getBuilder(ChunkSectionLayer layer, @Nonnull ByteBufferBuilder alloc)
    {
        return this.blockBufferBuilders.computeIfAbsent(layer, (key) -> new BufferBuilder(alloc, key.pipeline().getVertexFormatMode(), key.pipeline().getVertexFormat()));
    }

    protected BufferBuilder getBuilder(OverlayRenderType type, @Nonnull ByteBufferBuilder alloc)
    {
        return this.overlayBufferBuilders.computeIfAbsent(type, (key) -> new BufferBuilder(alloc, key.getDrawMode(), key.getVertexFormat()));
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
