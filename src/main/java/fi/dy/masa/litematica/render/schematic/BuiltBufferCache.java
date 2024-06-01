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

    protected BuiltBufferCache() { }

    protected boolean hasBuiltBufferByLayer(RenderLayer layer)
    {
        return this.layerBuffers.containsKey(layer);
    }

    protected boolean hasBuiltBufferByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayBuffers.containsKey(type);
    }

    protected void storeBuiltBufferByLayer(RenderLayer layer, @Nonnull BuiltBuffer newBuffer)
    {
        if (this.hasBuiltBufferByLayer(layer))
        {
            this.layerBuffers.get(layer).close();
        }
        this.layerBuffers.put(layer, newBuffer);
    }

    protected void storeBuiltBufferByType(ChunkRendererSchematicVbo.OverlayRenderType type, @Nonnull BuiltBuffer newBuffer)
    {
        if (this.hasBuiltBufferByType(type))
        {
            this.overlayBuffers.get(type).close();
        }
        this.overlayBuffers.put(type, newBuffer);
    }

    public BuiltBuffer getBuiltBufferByLayer(RenderLayer layer)
    {
        return this.layerBuffers.get(layer);
    }

    protected BuiltBuffer getBuiltBufferByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayBuffers.get(type);
    }

    protected void closeByLayer(RenderLayer layer)
    {
        try
        {
            if (this.layerBuffers.containsKey(layer))
            {
                this.layerBuffers.get(layer).close();
            }
        }
        catch (Exception ignored) { }
        this.layerBuffers.remove(layer);
    }

    protected void closeByType(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        try
        {
            if (this.overlayBuffers.containsKey(type))
            {
                this.overlayBuffers.get(type).close();
            }
        }
        catch (Exception ignored) { }
        this.overlayBuffers.remove(type);
    }

    protected void closeAll()
    {
        try
        {
            this.layerBuffers.values().forEach(BuiltBuffer::close);
            this.overlayBuffers.values().forEach(BuiltBuffer::close);
        }
        catch (Exception ignored) { }
        this.layerBuffers.clear();
        this.overlayBuffers.clear();
    }

    @Override
    public void close() throws Exception
    {
        this.closeAll();
    }
}
