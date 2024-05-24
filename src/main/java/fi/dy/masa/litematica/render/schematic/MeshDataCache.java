package fi.dy.masa.litematica.render.schematic;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_9801;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.Litematica;

public class MeshDataCache
{
    private final Map<RenderLayer, class_9801> meshLayerCache;
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, class_9801> meshOverlayCache;

    public MeshDataCache()
    {
        this.meshLayerCache = new HashMap<>();
        this.meshOverlayCache = new HashMap<>();

        Litematica.logger.error("MeshDataCache: init()");
    }

    public boolean hasMeshByLayer(RenderLayer layer)
    {
        return this.meshLayerCache.containsKey(layer);
    }

    public void storeMeshByLayer(RenderLayer layer, class_9801 newMeshData)
    {
        Litematica.logger.warn("storeMeshByLayer: {}", layer.getDrawMode().name());

        if (this.meshLayerCache.containsKey(layer))
        {
            this.meshLayerCache.get(layer).close();
            this.meshLayerCache.replace(layer, newMeshData);
        }

        this.meshLayerCache.put(layer, newMeshData);
    }

    public class_9801 getMeshByLayer(RenderLayer layer)
    {
        Litematica.logger.warn("getMeshByLayer: {}", layer.getDrawMode().name());

        return this.meshLayerCache.get(layer);
    }

    public boolean hasMeshByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.meshOverlayCache.containsKey(type);
    }

    public void storeMeshByType(ChunkRendererSchematicVbo.OverlayRenderType type, class_9801 newMeshData)
    {
        Litematica.logger.warn("storeMeshByOverlay: {}", type.getDrawMode().name());

        if (this.meshOverlayCache.containsKey(type))
        {
            this.meshOverlayCache.get(type).close();
            this.meshOverlayCache.replace(type, newMeshData);
        }

        this.meshOverlayCache.put(type, newMeshData);
    }

    public class_9801 getMeshByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        Litematica.logger.warn("getMeshByType: {}", type.getDrawMode().name());

        return this.meshOverlayCache.get(type);
    }
}
