package fi.dy.masa.litematica.render.schematic;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_9801;
import net.minecraft.client.render.RenderLayer;

public class MeshDataCache
{
    private final Map<RenderLayer, class_9801> meshLayerCache;
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, class_9801> meshOverlayCache;

    public MeshDataCache()
    {
        this.meshLayerCache = new HashMap<>();
        this.meshOverlayCache = new HashMap<>();
    }

    public boolean hasMeshByLayer(RenderLayer layer)
    {
        return this.meshLayerCache.containsKey(layer);
    }

    public void storeMeshByLayer(RenderLayer layer, class_9801 newMeshData)
    {
        if (this.meshLayerCache.containsKey(layer))
        {
            this.meshLayerCache.get(layer).close();
            this.meshLayerCache.replace(layer, newMeshData);
        }

        this.meshLayerCache.put(layer, newMeshData);
    }

    public class_9801 getMeshByLayer(RenderLayer layer)
    {
        return this.meshLayerCache.get(layer);
    }

    public boolean hasMeshByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.meshOverlayCache.containsKey(type);
    }

    public void storeMeshByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type, class_9801 newMeshData)
    {
        if (this.meshOverlayCache.containsKey(type))
        {
            this.meshOverlayCache.get(type).close();
            this.meshOverlayCache.replace(type, newMeshData);
        }

        this.meshOverlayCache.put(type, newMeshData);
    }

    public class_9801 getMeshByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.meshOverlayCache.get(type);
    }
}
