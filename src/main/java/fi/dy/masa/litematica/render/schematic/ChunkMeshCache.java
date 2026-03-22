package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkMeshCache implements AutoCloseable
{
    private final ConcurrentHashMap<ChunkSectionLayer, MeshData> blockBuffers;
    private final ConcurrentHashMap<RenderType, MeshData> layerBuffers;
    private final ConcurrentHashMap<OverlayRenderType, MeshData> overlayBuffers;

    protected ChunkMeshCache()
    {
	    this.blockBuffers = new ConcurrentHashMap<>(ByteBufferBuilderCache.BLOCK_LAYERS.size(), 0.9f, 1);
	    this.layerBuffers = new ConcurrentHashMap<>(ByteBufferBuilderCache.RENDER_LAYERS.size(), 0.9f, 1);
	    this.overlayBuffers = new ConcurrentHashMap<>(ByteBufferBuilderCache.TYPES.size(), 0.9f, 1);
    }

    protected boolean hasBuiltBufferByBlockLayer(ChunkSectionLayer layer)
    {
        return this.blockBuffers.containsKey(layer);
    }

    protected boolean hasBuiltBufferByLayer(RenderType layer)
    {
        return this.layerBuffers.containsKey(layer);
    }

    protected boolean hasBuiltBufferByType(OverlayRenderType type)
    {
        return this.overlayBuffers.containsKey(type);
    }

    protected void storeBuiltBufferByBlockLayer(ChunkSectionLayer layer, @Nonnull MeshData newBuffer)
    {
        if (this.hasBuiltBufferByBlockLayer(layer))
        {
            this.blockBuffers.get(layer).close();
        }
        synchronized (this.blockBuffers)
        {
            this.blockBuffers.put(layer, newBuffer);
        }
    }

    protected void storeBuiltBufferByLayer(RenderType layer, @Nonnull MeshData newBuffer)
    {
        if (this.hasBuiltBufferByLayer(layer))
        {
            this.layerBuffers.get(layer).close();
        }
        synchronized (this.layerBuffers)
        {
            this.layerBuffers.put(layer, newBuffer);
        }
    }

    protected void storeBuiltBufferByType(OverlayRenderType type, @Nonnull MeshData newBuffer)
    {
        if (this.hasBuiltBufferByType(type))
        {
            this.overlayBuffers.get(type).close();
        }
        synchronized (this.overlayBuffers)
        {
            this.overlayBuffers.put(type, newBuffer);
        }
    }

    @Nullable
    protected MeshData getBuiltBufferByBlockLayer(ChunkSectionLayer layer)
    {
        return this.blockBuffers.get(layer);
    }

    @Nullable
    protected MeshData getBuiltBufferByLayer(RenderType layer)
    {
        return this.layerBuffers.get(layer);
    }

    @Nullable
    protected MeshData getBuiltBufferByType(OverlayRenderType type)
    {
        return this.overlayBuffers.get(type);
    }

    protected void closeAll()
    {
        ArrayList<MeshData> builtBuffers;

        synchronized (this.blockBuffers)
        {
            builtBuffers = new ArrayList<>(this.blockBuffers.values());
            this.blockBuffers.clear();
        }
        synchronized (this.layerBuffers)
        {
            builtBuffers.addAll(this.layerBuffers.values());
            this.layerBuffers.clear();
        }
        synchronized (this.overlayBuffers)
        {
            builtBuffers.addAll(this.overlayBuffers.values());
            this.overlayBuffers.clear();
        }
        try
        {
            builtBuffers.forEach(MeshData::close);
        }
        catch (Exception ignored) { }
    }

    @Override
    public void close() throws Exception
    {
        this.closeAll();
    }
}
