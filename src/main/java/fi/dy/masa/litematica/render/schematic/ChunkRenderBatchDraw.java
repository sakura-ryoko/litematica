package fi.dy.masa.litematica.render.schematic;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.util.profiling.ProfilerFiller;

public record ChunkRenderBatchDraw(
		GpuTextureView atlasTexture,
		EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> drawData,
        boolean renderCollidingBlocks,
		boolean renderTranslucent,
        int maxIndicesRequired,
		GpuBufferSlice[] dynamicTransforms,
		GpuBufferSlice chunkFixUBO)
{
    public void draw(final ChunkSectionLayerGroup group, final GpuSampler sampler, ProfilerFiller profiler)
    {
//	    Litematica.LOGGER.error("ChunkRenderBatchDraw::draw({})", group.label());
        RenderSystem.AutoStorageIndexBuffer defaultIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer defaultIBO = this.maxIndicesRequired() == 0 ? null : defaultIndices.getBuffer(this.maxIndicesRequired());
        IndexType indexType = this.maxIndicesRequired() == 0 ? null : defaultIndices.type();
        ChunkSectionLayer[] layers = group.layers();
        Minecraft mc = Minecraft.getInstance();
	    boolean wf = SharedConstants.DEBUG_HOTKEYS && mc.wireframe;
        RenderTarget fb = mc.gameRenderer.mainRenderTarget();

        profiler.push("draw_group");
		try (RenderPass pass = RenderSystem.getDevice()
		                                   .createCommandEncoder()
		                                   .createRenderPass(
				                                   () -> "litematica:schematic_chunk/" + group.label(),
				                                   fb.getColorTextureView(),
				                                   Optional.empty(),
				                                   fb.getDepthTextureView(),
				                                   OptionalDouble.empty()
		                                   ))
		{
			RenderSystem.bindDefaultUniforms(pass);

//			if (renderTranslucent() && this.dynamicTransform() != null)
//			{
//				pass.setUniform("DynamicTransforms", this.dynamicTransform());
//			}

			pass.setUniform("LegacyTerrainFix", this.chunkFixUBO);
			pass.setUniform("Sampler0", this.atlasTexture, sampler);
			pass.setUniform("Sampler2", mc.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

			for (ChunkSectionLayer layer : layers)
			{
				List<RenderPass.Draw<GpuBufferSlice[]>> draws = this.drawData().get(layer);

				profiler.popPush("draw_group_" + layer.label());
				if (!draws.isEmpty())
				{
					if (layer == ChunkSectionLayer.TRANSLUCENT)
					{
						draws = draws.reversed();
					}

					RenderPipeline pipeline;

					if (wf)
					{
						pipeline = this.renderCollidingBlocks()
						           ? ChunkRenderLayers.getWireframe().getRight()
						           : ChunkRenderLayers.getWireframe().getLeft();
					}
					else
					{
						if (this.renderTranslucent())
						{
							pipeline = this.renderCollidingBlocks()
							           ? ChunkRenderLayers.PIPELINE_MAP.get(ChunkSectionLayer.TRANSLUCENT).getRight()
							           : ChunkRenderLayers.PIPELINE_MAP.get(ChunkSectionLayer.TRANSLUCENT).getLeft();
						}
						else
						{
							pipeline = this.renderCollidingBlocks()
							           ? ChunkRenderLayers.PIPELINE_MAP.get(layer).getRight()
							           : ChunkRenderLayers.PIPELINE_MAP.get(layer).getLeft();
						}

						pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
						pass.drawMultipleIndexed(draws, defaultIBO, indexType, List.of("DynamicTransforms"), this.dynamicTransforms());
					}
				}
			}
		}
		catch (Exception ignored) { }

        profiler.pop();
    }
}
