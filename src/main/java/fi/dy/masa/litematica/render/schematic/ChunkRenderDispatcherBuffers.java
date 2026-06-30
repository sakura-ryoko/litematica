package fi.dy.masa.litematica.render.schematic;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class ChunkRenderDispatcherBuffers
{
	private final ByteBufferBuilderCache allocatorCache;
	private final BufferBuilderCache builderCache;

	protected ChunkRenderDispatcherBuffers()
	{
		this.allocatorCache = new ByteBufferBuilderCache();
		this.builderCache = new BufferBuilderCache();
	}

	protected ByteBufferBuilderCache allocatorCache()
	{
		return this.allocatorCache;
	}

	protected BufferBuilderCache builderCache()
	{
		return this.builderCache;
	}

	protected BufferBuilder getBuilder(ChunkSectionLayer layer)
	{
		return this.builderCache().getBuilder(layer, this.allocatorCache.getAllocator(layer));
	}

	protected BufferBuilder getBuilder(OverlayRenderType type)
	{
		return this.builderCache().getBuilder(type, this.allocatorCache.getAllocator(type));
	}

	protected void reset()
	{
		if (!this.allocatorCache.isClear())
		{
			this.allocatorCache.resetAll();
		}

		this.builderCache.clearAll();
	}
}
