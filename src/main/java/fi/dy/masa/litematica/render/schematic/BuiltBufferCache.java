package fi.dy.masa.litematica.render.schematic;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;

public class BuiltBufferCache implements AutoCloseable
{
    private final ConcurrentHashMap<ChunkSectionLayer, MeshData> blockBuffers;
    private final ConcurrentHashMap<RenderType, MeshData> layerBuffers;
    private final ConcurrentHashMap<OverlayRenderType, MeshData> overlayBuffers;

    protected BuiltBufferCache()
    {
	    this.blockBuffers = new ConcurrentHashMap<>(BufferAllocatorCache.BLOCK_LAYERS.size(), 0.9f, 1);
	    this.layerBuffers = new ConcurrentHashMap<>(BufferAllocatorCache.RENDER_LAYERS.size(), 0.9f, 1);
	    this.overlayBuffers = new ConcurrentHashMap<>(BufferAllocatorCache.TYPES.size(), 0.9f, 1);
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
        MeshData remove = this.blockBuffers.put(layer, newBuffer);

        if (remove != null)
        {
            remove.close();
        }
    }

    protected void storeBuiltBufferByLayer(RenderType layer, @Nonnull MeshData newBuffer)
    {
        MeshData remove = this.layerBuffers.put(layer, newBuffer);

        if (remove != null)
        {
            remove.close();
        }
    }

    protected void storeBuiltBufferByType(OverlayRenderType type, @Nonnull MeshData newBuffer)
    {
        MeshData remove = this.overlayBuffers.put(type, newBuffer);

        if (remove != null)
        {
            remove.close();
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
        for (MeshData mesh : this.blockBuffers.values())
        {
            try
            {
                mesh.close();
            }
            catch (Exception ignored) {}
        }
        this.blockBuffers.clear();

        for (MeshData mesh : this.layerBuffers.values())
        {
            try
            {
                mesh.close();
            }
            catch (Exception ignored) {}
        }
        this.layerBuffers.clear();

        for (MeshData mesh : this.overlayBuffers.values())
        {
            try
            {
                mesh.close();
            }
            catch (Exception ignored) {}
        }
        this.overlayBuffers.clear();
    }

    @Override
    public void close() throws Exception
    {
        this.closeAll();
    }
}
