package fi.dy.masa.litematica.render.cache;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.schematic.ChunkRenderLayers;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;

public class BuiltBufferCache implements AutoCloseable
{
    private final Map<RenderLayer, BuiltBuffer> layerBuffers = new HashMap<>();
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BuiltBuffer> overlayBuffers = new HashMap<>();

    public BuiltBufferCache()
    {
        Litematica.logger.error("BuiltBufferCache: init()");
    }

    public boolean hasBuiltBufferByLayer(RenderLayer layer)
    {
        return this.layerBuffers.containsKey(layer);
    }

    public boolean hasBuiltBufferByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayBuffers.containsKey(type);
    }

    public void storeBuiltBufferByLayer(RenderLayer layer, @Nonnull BuiltBuffer newBuffer)
    {
        Litematica.logger.error("storeBuiltBufferByLayer: for layer [{}]", ChunkRenderLayers.getFriendlyName(layer));

        this.layerBuffers.put(layer, newBuffer);
    }

    public void storeBuiltBufferByType(ChunkRendererSchematicVbo.OverlayRenderType type, @Nonnull BuiltBuffer newBuffer)
    {
        Litematica.logger.error("storeBuiltBufferByType: for type [{}]", type.getDrawMode().name());

        this.overlayBuffers.put(type, newBuffer);
    }

    public BuiltBuffer getBuiltBufferByLayer(RenderLayer layer)
    {
        return this.layerBuffers.get(layer);
    }

    public BuiltBuffer getBuiltBufferByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayBuffers.get(type);
    }

    public void clearByLayer(RenderLayer layer)
    {
        this.layerBuffers.remove(layer);
    }

    public void clearByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        this.overlayBuffers.remove(type);
    }

    public void clear()
    {
        Litematica.logger.warn("BuiltBufferCache: clear()");

        this.layerBuffers.values().forEach(BuiltBuffer::close);
        this.overlayBuffers.values().forEach(BuiltBuffer::close);
        this.layerBuffers.clear();
        this.overlayBuffers.clear();
    }

    @Override
    public void close() throws Exception
    {
        this.clear();
    }
}
