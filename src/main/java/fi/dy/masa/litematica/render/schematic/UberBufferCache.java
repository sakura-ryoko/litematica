package fi.dy.masa.litematica.render.schematic;

import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;

import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.UberGpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.Util;

import fi.dy.masa.litematica.Litematica;

/**
 * @deprecated -- sort of new thing that Mojang uses; but we're not.
 */
@Deprecated
public class UberBufferCache implements AutoCloseable
{
	private final Map<ChunkSectionLayer, ChunkRenderUberBuffers> blockBuffers;
	private final Map<OverlayRenderType, ChunkRenderUberBuffers> overlayBuffers;
	private boolean clear;

	protected UberBufferCache()
	{
		int vboHeapSize = 134217728;
		int iboHeapSize = 33554432;
		int vboStageSize = 33554432;
		int iboStageSize = 2097152;
		GpuDevice gpuDevice = RenderSystem.getDevice();
		GraphicsWorkarounds workarounds = GraphicsWorkarounds.get(gpuDevice);
		this.blockBuffers = Util.makeEnumMap(
				ChunkSectionLayer.class,
				layer ->
				{
					VertexFormat vertexFormat = layer.pipeline().getVertexFormat();
					UberGpuBuffer<ChunkMeshDataSchematic> vertexUberBuffer = new UberGpuBuffer<>(
							layer.label(), 32, vboHeapSize, vertexFormat.getVertexSize(), gpuDevice, vboStageSize, workarounds
					);
					UberGpuBuffer<ChunkMeshDataSchematic> indexUberBuffer = layer == ChunkSectionLayer.TRANSLUCENT
					                                                        ? new UberGpuBuffer<>(layer.label(), 64, iboHeapSize, 8, gpuDevice, iboStageSize, workarounds)
					                                                        : null;
					return new ChunkRenderUberBuffers(vertexUberBuffer, indexUberBuffer);
				}
		);
		this.overlayBuffers = Util.makeEnumMap(
				OverlayRenderType.class,
				type ->
				{
					VertexFormat vertexFormat = type.getPipeline().getVertexFormat();
					UberGpuBuffer<ChunkMeshDataSchematic> vertexUberBuffer = new UberGpuBuffer<>(
							type.name(), 32, vboHeapSize, vertexFormat.getVertexSize(), gpuDevice, vboStageSize, workarounds
					);
					UberGpuBuffer<ChunkMeshDataSchematic> indexUberBuffer = type.isTranslucent()
					                                                        ? new UberGpuBuffer<>(type.name(), 64, iboHeapSize, 8, gpuDevice, iboStageSize, workarounds)
					                                                        : null;
					return new ChunkRenderUberBuffers(vertexUberBuffer, indexUberBuffer);
				}
		);
		this.clear = true;
	}

	protected boolean hasUberBuffers(ChunkSectionLayer layer)
	{
		return this.blockBuffers.containsKey(layer);
	}

	protected boolean hasUberBuffers(OverlayRenderType type)
	{
		return this.overlayBuffers.containsKey(type);
	}

	@Nullable
	protected ChunkRenderUberBuffers getUberBuffersOrNull(ChunkSectionLayer layer)
	{
		this.clear = false;

		synchronized (this.blockBuffers)
		{
			return this.blockBuffers.get(layer);
		}
	}

	@Nullable
	protected ChunkRenderUberBuffers getUberBuffersOrNull(OverlayRenderType type)
	{
		this.clear = false;

		synchronized (this.overlayBuffers)
		{
			return this.overlayBuffers.get(type);
		}
	}

	protected Collection<ChunkRenderUberBuffers> getBlockValues()
	{
		synchronized (this.blockBuffers)
		{
			return this.blockBuffers.values();
		}
	}

	protected Collection<ChunkRenderUberBuffers> getOverlayValues()
	{
		synchronized (this.overlayBuffers)
		{
			return this.overlayBuffers.values();
		}
	}

	protected boolean isClear() {return this.clear;}

	protected void clearAll()
	{
		Litematica.LOGGER.warn("UberBufferCache clearAll()");

		synchronized (this.blockBuffers)
		{
			this.blockBuffers.forEach(
					(layer, buffers) ->
					{
						try
						{
							buffers.vertexBuffer().close();

							if (buffers.indexBuffer() != null)
							{
								buffers.indexBuffer().close();
							}
						}
						catch (Exception err)
						{
							throw new RuntimeException("Exception closing Block Layer " + layer.label() + " Buffers; " + err.getMessage());
						}
					}
			);

			this.blockBuffers.clear();
		}

		synchronized (this.overlayBuffers)
		{
			this.overlayBuffers.forEach(
					(type, buffers) ->
					{
						try
						{
							buffers.vertexBuffer().close();

							if (buffers.indexBuffer() != null)
							{
								buffers.indexBuffer().close();
							}
						}
						catch (Exception err)
						{
							throw new RuntimeException("Exception closing Overlay Type " + type.name() + " Buffers; " + err.getMessage());
						}
					}
			);

			this.overlayBuffers.clear();
		}

		this.clear = true;
	}

	@Override
	public void close() throws Exception
	{
		this.clearAll();
	}
}
