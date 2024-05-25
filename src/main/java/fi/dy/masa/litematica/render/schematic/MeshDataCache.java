package fi.dy.masa.litematica.render.schematic;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.Litematica;

public class MeshDataCache
{
    private final Map<RenderLayer, BuiltBuffer> meshLayerCache;
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BuiltBuffer> meshOverlayCache;

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

    public void storeMeshByLayer(RenderLayer layer, BuiltBuffer newMeshData)
    {
        Litematica.logger.warn("storeMeshByLayer: {} has {}", layer.getDrawMode().name(), this.hasMeshByLayer(layer));

        if (this.meshLayerCache.containsKey(layer))
        {
            this.meshLayerCache.get(layer).close();
            this.meshLayerCache.replace(layer, newMeshData);
        }
        else
        {
            this.meshLayerCache.put(layer, newMeshData);
        }
    }

    public BuiltBuffer getMeshByLayer(RenderLayer layer)
    {
        Litematica.logger.warn("getMeshByLayer: {} has {}", layer.getDrawMode().name(), this.hasMeshByLayer(layer));

        return this.meshLayerCache.get(layer);
    }

    public boolean hasMeshByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.meshOverlayCache.containsKey(type);
    }

    public void storeMeshByType(ChunkRendererSchematicVbo.OverlayRenderType type, BuiltBuffer newMeshData)
    {
        Litematica.logger.warn("storeMeshByOverlay: {} has {}", type.getDrawMode().name(), this.hasMeshByType(type));

        if (this.meshOverlayCache.containsKey(type))
        {
            this.meshOverlayCache.get(type).close();
            this.meshOverlayCache.replace(type, newMeshData);
        }
        else
        {
            this.meshOverlayCache.put(type, newMeshData);
        }
    }

    public BuiltBuffer getMeshByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        Litematica.logger.warn("getMeshByType: {} has {}", type.getDrawMode().name(), this.hasMeshByType(type));

        return this.meshOverlayCache.get(type);
    }

    public void clear()
    {
        Litematica.logger.error("MeshDataCache: clear()");

        this.meshLayerCache.values().forEach(BuiltBuffer::close);
        this.meshOverlayCache.values().forEach(BuiltBuffer::close);
    }
}
