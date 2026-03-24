package fi.dy.masa.litematica.render.schematic;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class ChunkRenderDataSchematic implements AutoCloseable
{
	public static final ChunkRenderDataSchematic EMPTY = new ChunkRenderDataSchematic()
	{
		@Override
		protected void setBlockLayerUsed(ChunkSectionLayer layer)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setBlockLayerStarted(ChunkSectionLayer layer)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setOverlayTypeUsed(OverlayRenderType layer)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setOverlayTypeStarted(OverlayRenderType layer)
		{
			throw new UnsupportedOperationException();
		}
	};

	private final AtomicReference<ChunkMeshDataSchematic> meshDataCache;
	private final Set<ChunkSectionLayer> blockLayersUsed;
	private final Set<ChunkSectionLayer> blockLayersStarted;
	private final Set<OverlayRenderType> overlayLayersUsed;
	private final Set<OverlayRenderType> overlayLayersStarted;
	private boolean blocksEmpty;
	private boolean overlayEmpty;
	private long timeBuilt;

	public ChunkRenderDataSchematic()
	{
		this.meshDataCache = new AtomicReference<>(ChunkMeshDataSchematic.EMPTY);
		this.blockLayersUsed = new ObjectArraySet<>();
		this.blockLayersStarted = new ObjectArraySet<>();
		this.overlayLayersUsed = new ObjectArraySet<>();
		this.overlayLayersStarted = new ObjectArraySet<>();
		this.blocksEmpty = true;
		this.overlayEmpty = true;
	}

	public ChunkMeshDataSchematic getMeshDataCache()
	{
		return this.meshDataCache.get();
	}

	protected void updateMeshDataCache(ChunkMeshDataSchematic meshData)
	{
		ChunkMeshDataSchematic oldMesh = this.meshDataCache.getAndSet(meshData);

		if (oldMesh != null)
		{
			oldMesh.clearAll();
		}
	}

	public boolean isBlockLayerEmpty()
	{
		return this.blocksEmpty;
	}

	public int getStartedSize()
	{
		return this.blockLayersStarted.size() + this.overlayLayersStarted.size();
	}

	public int getUsedSize()
	{
		return this.blockLayersUsed.size() + this.overlayLayersUsed.size();
	}

	public int getSize()
	{
		return Math.max(this.getStartedSize(), this.getUsedSize());
	}

	public boolean isBlockLayerEmpty(ChunkSectionLayer layer)
	{
		return !this.blockLayersUsed.contains(layer);
	}

	public boolean isOverlayEmpty()
	{
		return this.overlayEmpty;
	}

	public boolean isOverlayTypeEmpty(OverlayRenderType type)
	{
		return !this.overlayLayersUsed.contains(type);
	}

	public boolean isBlockLayerStarted(ChunkSectionLayer layer)
	{
		return this.blockLayersStarted.contains(layer);
	}

	public boolean isOverlayTypeStarted(OverlayRenderType type)
	{
		return this.overlayLayersStarted.contains(type);
	}

	protected void setBlockLayerStarted(ChunkSectionLayer layer)
	{
		this.blockLayersStarted.add(layer);
	}

	protected void setBlockLayerUsed(ChunkSectionLayer layer)
	{
		this.blocksEmpty = false;
		this.blockLayersUsed.add(layer);
	}

	protected void setBlockLayerUnused(ChunkSectionLayer layer)
	{
		this.blockLayersStarted.remove(layer);
		this.blockLayersUsed.remove(layer);
	}

	protected void setOverlayTypeStarted(OverlayRenderType type)
	{
		this.overlayLayersStarted.add(type);
	}

	protected void setOverlayTypeUsed(OverlayRenderType type)
	{
		this.overlayEmpty = false;
		this.overlayLayersUsed.add(type);
	}

	protected void setOverlayTypeUnused(OverlayRenderType type)
	{
		this.overlayLayersStarted.remove(type);
		this.overlayLayersUsed.remove(type);
	}

	public long getTimeBuilt()
	{
		return this.timeBuilt;
	}

	protected void setTimeBuilt(long time)
	{
		this.timeBuilt = time;
	}

	protected void clearAll()
	{
		this.updateMeshDataCache(ChunkMeshDataSchematic.EMPTY);
		this.timeBuilt = 0L;
		this.blockLayersUsed.clear();
		this.overlayLayersUsed.clear();
		this.blockLayersStarted.clear();
		this.overlayLayersStarted.clear();
		this.overlayEmpty = true;
		this.blocksEmpty = true;
	}

	@Override
	public void close() throws Exception
	{
		this.clearAll();
	}
}
