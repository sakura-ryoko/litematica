package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class ChunkMeshCache implements AutoCloseable
{
    private final ConcurrentHashMap<ChunkSectionLayer, MeshData> blockMeshData;
    private final ConcurrentHashMap<OverlayRenderType, MeshData> overlayMeshData;

    protected ChunkMeshCache()
    {
	    this.blockMeshData = new ConcurrentHashMap<>(ByteBufferBuilderCache.BLOCK_LAYERS.size(), 0.9f, 1);
	    this.overlayMeshData = new ConcurrentHashMap<>(ByteBufferBuilderCache.TYPES.size(), 0.9f, 1);
    }

    protected boolean hasMeshData(ChunkSectionLayer layer)
    {
        return this.blockMeshData.containsKey(layer);
    }

    protected boolean hasMeshData(OverlayRenderType type)
    {
        return this.overlayMeshData.containsKey(type);
    }

    protected void saveMeshData(ChunkSectionLayer layer, @Nonnull MeshData newBuffer)
    {
        if (this.hasMeshData(layer))
        {
            this.blockMeshData.get(layer).close();
        }
        synchronized (this.blockMeshData)
        {
            this.blockMeshData.put(layer, newBuffer);
        }
    }

    protected void saveMeshData(OverlayRenderType type, @Nonnull MeshData newBuffer)
    {
        if (this.hasMeshData(type))
        {
            this.overlayMeshData.get(type).close();
        }
        synchronized (this.overlayMeshData)
        {
            this.overlayMeshData.put(type, newBuffer);
        }
    }

    @Nullable
    protected MeshData getMeshDataOrNull(ChunkSectionLayer layer)
    {
        return this.blockMeshData.get(layer);
    }

    @Nullable
    protected MeshData getMeshDataOrNull(OverlayRenderType type)
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
