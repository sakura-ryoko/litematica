package fi.dy.masa.litematica.render.schematic;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;

import fi.dy.masa.malilib.render.MaLiLibPipelines;

public enum OverlayRenderType
{
    OUTLINE     (MaLiLibPipelines.LINES_TRANSLUCENT_OFFSET_2,
                 MaLiLibPipelines.LINES_TRANSLUCENT_NO_DEPTH_NO_CULL, false
    ),
    QUAD        (MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_LESSER_DEPTH_OFFSET_2,
                 MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL, true
    ),
    ;

    private final RenderPipeline pipeline;
    private final RenderPipeline renderThrough;
    private final boolean translucent;

    OverlayRenderType(RenderPipeline pipeline, RenderPipeline renderThrough, boolean translucent)
    {
        this.translucent = translucent;
        this.pipeline = pipeline;
        this.renderThrough = renderThrough;
        this.ensurePipelines();
    }

    public VertexFormat.DrawMode getDrawMode()
    {
        return this.pipeline.getVertexFormatMode();
    }

    public int getExpectedBufferSize() { return this.pipeline.getVertexFormat().getVertexSize() * 4; }

    public VertexFormat getVertexFormat() { return this.pipeline.getVertexFormat(); }

    public boolean isTranslucent() { return this.translucent; }

    public RenderPipeline getPipeline() { return this.pipeline; }

    public RenderPipeline getRenderThrough() { return this.renderThrough; }

    private void ensurePipelines() throws RuntimeException
    {
        if (this.renderThrough.getVertexFormatMode() != this.pipeline.getVertexFormatMode())
        {
            throw new RuntimeException("DrawMode does not match between Pipelines!");
        }

        if (this.renderThrough.getVertexFormat() != this.pipeline.getVertexFormat())
        {
            throw new RuntimeException("VertexFormat does not match between Pipelines!");
        }
    }
}
