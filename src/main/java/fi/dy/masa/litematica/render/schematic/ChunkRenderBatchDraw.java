package fi.dy.masa.litematica.render.schematic;

import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayerGroup;
import net.minecraft.util.profiler.Profiler;

public record ChunkRenderBatchDraw(
        EnumMap<BlockRenderLayer, List<RenderPass.RenderObject<GpuBufferSlice[]>>> drawData,
        boolean renderCollidingBlocks, boolean renderTranslucent,
        int maxIndicesRequired,
        GpuBufferSlice[] dynamicTransforms)
{
    public void draw(BlockRenderLayerGroup group, Profiler profiler)
    {
        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
        GpuBuffer gpuBuffer = this.maxIndicesRequired == 0 ? null : shapeIndexBuffer.getIndexBuffer(this.maxIndicesRequired);
        VertexFormat.IndexType indexType = this.maxIndicesRequired == 0 ? null : shapeIndexBuffer.getIndexType();
        BlockRenderLayer[] layers = group.getLayers();
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer fb = group.getFramebuffer();

        profiler.push("draw_group");

        try (RenderPass pass = RenderSystem.getDevice()
                                           .createCommandEncoder()
                                           .createRenderPass(
                                                   () -> "litematica:schematic_chunk/"+group.getName(),
                                                   fb.getColorAttachmentView(),
                                                   OptionalInt.empty(),
                                                   fb.getDepthAttachmentView(),
                                                   OptionalDouble.empty()
                                           ))
        {
            RenderSystem.bindDefaultUniforms(pass);
            pass.bindSampler("Sampler2", mc.gameRenderer.getLightmapTextureManager().getGlTextureView());

            for (BlockRenderLayer layer : layers)
            {
                List<RenderPass.RenderObject<GpuBufferSlice[]>> list = this.drawData.get(layer);

                profiler.swap("draw_group_"+layer.getName());
                if (!list.isEmpty())
                {
                    if (layer == BlockRenderLayer.TRANSLUCENT)
                    {
                        list = list.reversed();
                    }

                    pass.setPipeline(
                            this.renderCollidingBlocks() ?
                            ChunkRenderLayers.PIPELINE_MAP.get(layer).getRight() :
                            ChunkRenderLayers.PIPELINE_MAP.get(layer).getLeft()
                    );

                    pass.bindSampler("Sampler0", layer.getTextureView());
                    pass.drawMultipleIndexed(list, gpuBuffer, indexType, List.of("DynamicTransforms"), this.dynamicTransforms);
                }
            }
        }

        profiler.pop();
    }
}
