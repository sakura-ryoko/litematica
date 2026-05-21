package fi.dy.masa.litematica.render.schematic;

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
        MeshData remove = this.blockMeshData.put(layer, newBuffer);

        if (remove != null)
        {
            remove.close();
        }
    }

    protected void saveMeshData(OverlayRenderType type, @Nonnull MeshData newBuffer)
    {
        MeshData remove = this.overlayMeshData.put(type, newBuffer);

        if (remove != null)
        {
            remove.close();
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
        for (MeshData mesh : this.blockMeshData.values())
        {
            try
            {
                mesh.close();
            }
            catch (Exception ignored) {}
        }
        this.blockMeshData.clear();

        for (MeshData mesh : this.overlayMeshData.values())
        {
            try
            {
                mesh.close();
            }
            catch (Exception ignored) {}
        }
        this.overlayMeshData.clear();
    }

    @Override
    public void close() throws Exception
    {
        this.closeAll();
    }
}
