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
    private final ConcurrentHashMap<ChunkSectionLayer, MeshData> blockMeshData;
    private final ConcurrentHashMap<RenderType, MeshData> layerMeshData;
    private final ConcurrentHashMap<OverlayRenderType, MeshData> overlayMeshData;

    protected ChunkMeshCache()
    {
	    this.blockMeshData = new ConcurrentHashMap<>(ByteBufferBuilderCache.BLOCK_LAYERS.size(), 0.9f, 1);
	    this.layerMeshData = new ConcurrentHashMap<>(ByteBufferBuilderCache.RENDER_LAYERS.size(), 0.9f, 1);
	    this.overlayMeshData = new ConcurrentHashMap<>(ByteBufferBuilderCache.TYPES.size(), 0.9f, 1);
    }

    protected boolean hasMeshByBlockLayer(ChunkSectionLayer layer)
    {
        return this.blockMeshData.containsKey(layer);
    }

    protected boolean hasMeshByLayer(RenderType layer)
    {
        return this.layerMeshData.containsKey(layer);
    }

    protected boolean hasMeshByType(OverlayRenderType type)
    {
        return this.overlayMeshData.containsKey(type);
    }

    protected void storeMeshByBlockLayer(ChunkSectionLayer layer, @Nonnull MeshData newBuffer)
    {
        if (this.hasMeshByBlockLayer(layer))
        {
            this.blockMeshData.get(layer).close();
        }
        synchronized (this.blockMeshData)
        {
            this.blockMeshData.put(layer, newBuffer);
        }
    }

    protected void storeMeshByLayer(RenderType layer, @Nonnull MeshData newBuffer)
    {
        if (this.hasMeshByLayer(layer))
        {
            this.layerMeshData.get(layer).close();
        }
        synchronized (this.layerMeshData)
        {
            this.layerMeshData.put(layer, newBuffer);
        }
    }

    protected void storeMeshByType(OverlayRenderType type, @Nonnull MeshData newBuffer)
    {
        if (this.hasMeshByType(type))
        {
            this.overlayMeshData.get(type).close();
        }
        synchronized (this.overlayMeshData)
        {
            this.overlayMeshData.put(type, newBuffer);
        }
    }

    @Nullable
    protected MeshData getMeshByBlockLayer(ChunkSectionLayer layer)
    {
        return this.blockMeshData.get(layer);
    }

    @Nullable
    protected MeshData getMeshByLayer(RenderType layer)
    {
        return this.layerMeshData.get(layer);
    }

    @Nullable
    protected MeshData getMeshByType(OverlayRenderType type)
    {
        return this.overlayMeshData.get(type);
    }

    protected void closeAll()
    {
        ArrayList<MeshData> list;

        synchronized (this.blockMeshData)
        {
            list = new ArrayList<>(this.blockMeshData.values());
            this.blockMeshData.clear();
        }
        synchronized (this.layerMeshData)
        {
            list.addAll(this.layerMeshData.values());
            this.layerMeshData.clear();
        }
        synchronized (this.overlayMeshData)
        {
            list.addAll(this.overlayMeshData.values());
            this.overlayMeshData.clear();
        }
        try
        {
            list.forEach(MeshData::close);
        }
        catch (Exception ignored) { }
    }

    @Override
    public void close() throws Exception
    {
        this.closeAll();
    }
}
