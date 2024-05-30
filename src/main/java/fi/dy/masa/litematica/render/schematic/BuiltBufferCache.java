package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;

public class BuiltBufferCache implements AutoCloseable
{
    private final Map<RenderLayer, BuiltBuffer> layerBuffers = new HashMap<>();
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BuiltBuffer> overlayBuffers = new HashMap<>();

    public BuiltBufferCache() { }

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
        this.layerBuffers.put(layer, newBuffer);
    }

    public void storeBuiltBufferByType(ChunkRendererSchematicVbo.OverlayRenderType type, @Nonnull BuiltBuffer newBuffer)
    {
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

    public void closeByLayer(RenderLayer layer)
    {
        try
        {
            if (this.layerBuffers.containsKey(layer))
            {
                this.layerBuffers.get(layer).close();
            }
        }
        catch (Exception ignored) {}
        this.layerBuffers.remove(layer);
    }

    public void closeByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        try
        {
            if (this.overlayBuffers.containsKey(type))
            {
                this.overlayBuffers.get(type).close();
            }
        }
        catch (Exception ignored) {}
        this.overlayBuffers.remove(type);
    }

    public void closeALl()
    {
        try
        {
            this.layerBuffers.values().forEach(BuiltBuffer::close);
            this.overlayBuffers.values().forEach(BuiltBuffer::close);
        }
        catch (Exception ignored) {}
        this.layerBuffers.clear();
        this.overlayBuffers.clear();
    }

    @Override
    public void close() throws Exception
    {
        this.closeALl();
    }
}
