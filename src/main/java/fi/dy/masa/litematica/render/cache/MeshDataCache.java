package fi.dy.masa.litematica.render.cache;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.schematic.org.ChunkRendererSchematicVbo;

public class MeshDataCache implements AutoCloseable
{
    private final Map<RenderLayer, BuiltBuffer> meshLayerCache = new HashMap<>();
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BuiltBuffer> meshOverlayCache = new HashMap<>();

    public MeshDataCache()
    {
        Litematica.logger.error("MeshDataCache: init()");
    }

    public void storeMeshByLayer(RenderLayer layer, @Nonnull BuiltBuffer newMeshData)
    {
        Litematica.logger.error("storeMeshByLayer: for layer [{}]", layer.getDrawMode().name());

        this.meshLayerCache.put(layer, newMeshData);
    }

    public void storeMeshByType(ChunkRendererSchematicVbo.OverlayRenderType type, @Nonnull BuiltBuffer newMeshData)
    {
        Litematica.logger.error("storeMeshByType: for type [{}]", type.getDrawMode().name());

        this.meshOverlayCache.put(type, newMeshData);
    }

    public BuiltBuffer getMeshByLayer(RenderLayer layer)
    {
        Litematica.logger.error("getMeshByLayer: for layer [{}]", layer.getDrawMode().name());

        return this.meshLayerCache.get(layer);
    }

    public BuiltBuffer getMeshByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        Litematica.logger.error("getMeshByType: for type [{}]", type.getDrawMode().name());

        return this.meshOverlayCache.get(type);
    }

    public void clear()
    {
        Litematica.logger.error("MeshDataCache: clear()");

        this.meshLayerCache.values().forEach(BuiltBuffer::close);
        this.meshOverlayCache.values().forEach(BuiltBuffer::close);
    }

    @Override
    public void close() throws Exception
    {
        this.clear();
    }
}
