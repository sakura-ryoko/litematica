package fi.dy.masa.litematica.render.schematic;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;

import fi.dy.masa.malilib.render.MaLiLibPipelines;

public enum OverlayRenderType
{
    OUTLINE     (MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_OFFSET_2,
                 MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL,
                 PrimitiveTopology.DEBUG_LINES, 0, false
    ),
    QUAD        (MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH_OFFSET_2,
                 MaLiLibPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL,
                 PrimitiveTopology.QUADS, 0, true
    ),
    ;

    private final RenderPipeline pipeline;
    private final RenderPipeline renderThrough;
    private final PrimitiveTopology topology;
    private final int index;
    private final boolean translucent;

    OverlayRenderType(RenderPipeline pipeline, RenderPipeline renderThrough, PrimitiveTopology topology, int index, boolean translucent)
    {
        this.pipeline = pipeline;
        this.renderThrough = renderThrough;
        this.translucent = translucent;
        this.topology = topology;
        this.index = index;
        this.ensurePipelines();
    }

    public RenderPipeline pipeline() { return this.pipeline; }

    public RenderPipeline renderThrough() { return this.renderThrough; }

    public PrimitiveTopology topology()
    {
        return this.topology;
    }

    public VertexFormat vertexFormat()
    {
        return this.pipeline.getVertexFormatBinding(this.index);
    }

    public boolean translucent() { return this.translucent; }

    public int expectedBufferSize() { return this.vertexFormat().getVertexSize() * 4; }

    private void ensurePipelines() throws RuntimeException
    {
        if (this.renderThrough.getVertexFormatBinding(this.index) != this.pipeline.getVertexFormatBinding(this.index))
        {
            throw new RuntimeException("VertexFormat does not match between Pipelines!");
        }
    }
}
