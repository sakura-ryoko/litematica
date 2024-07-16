package fi.dy.masa.litematica.render.schematic;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public enum OverlayRenderType
{
    OUTLINE     (VertexFormat.DrawMode.DEBUG_LINES, RenderLayer.DEFAULT_BUFFER_SIZE, VertexFormats.POSITION_COLOR, false, false),
    QUAD        (VertexFormat.DrawMode.QUADS,       RenderLayer.DEFAULT_BUFFER_SIZE, VertexFormats.POSITION_COLOR, false, true);

    private final VertexFormat.DrawMode drawMode;
    private final VertexFormat vertexFormat;
    private final int bufferSize;
    private final boolean hasCrumbling;
    private final boolean translucent;

    OverlayRenderType(VertexFormat.DrawMode drawMode, int bufferSize, VertexFormat format, boolean crumbling, boolean translucent)
    {
        this.drawMode = drawMode;
        this.bufferSize = bufferSize;
        this.vertexFormat = format;
        this.hasCrumbling = crumbling;
        this.translucent = translucent;
    }

    public VertexFormat.DrawMode getDrawMode()
    {
        return this.drawMode;
    }

    public int getExpectedBufferSize() { return this.bufferSize; }

    public VertexFormat getVertexFormat() { return this.vertexFormat; }

    public boolean hasCrumbling() { return this.hasCrumbling; }

    public boolean isTranslucent() { return this.translucent; }
}
