package fi.dy.masa.litematica.render.schematic;

import java.util.Set;
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

	private volatile ChunkMeshDataSchematic meshDataCache;
	private final Set<ChunkSectionLayer> blockLayersUsed;
	private final Set<ChunkSectionLayer> blockLayersStarted;
	private final Set<OverlayRenderType> overlayLayersUsed;
	private final Set<OverlayRenderType> overlayLayersStarted;
	private boolean blocksEmpty;
	private boolean overlayEmpty;
	private long timeBuilt;

	public ChunkRenderDataSchematic()
	{
		this.meshDataCache = ChunkMeshDataSchematic.EMPTY;
		this.blockLayersUsed = new ObjectArraySet<>();
		this.blockLayersStarted = new ObjectArraySet<>();
		this.overlayLayersUsed = new ObjectArraySet<>();
		this.overlayLayersStarted = new ObjectArraySet<>();
		this.blocksEmpty = true;
		this.overlayEmpty = true;
	}

	public ChunkMeshDataSchematic getMeshDataCache()
	{
		return this.meshDataCache;
	}

	protected void updateMeshDataCache(ChunkMeshDataSchematic meshData)
	{
		if (this.meshDataCache != null && !this.meshDataCache.equals(meshData))
		{
			this.meshDataCache.clearAll();
		}

		this.meshDataCache = meshData;
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

	protected void dumpRenderDataDebug()
	{
		if (this.equals(ChunkRenderDataSchematic.EMPTY))
		{
			System.out.print("[RD] ChunkRenderDataSchematic --> EMPTY\n");
		}
		else
		{
			System.out.printf("[RD] ChunkRenderDataSchematic; timeBuilt: [%d]\n", this.getTimeBuilt());
		}

		if (this.meshDataCache != null && this.meshDataCache.equals(ChunkMeshDataSchematic.EMPTY))
		{
			System.out.print("[RD] ChunkMeshDataCache --> EMPTY\n");
		}
		else
		{
			System.out.print("[RD] ChunkMeshDataCache --> NOT EMPTY\n");
			this.meshDataCache.dumpMeshDataDebug();
		}

		System.out.printf("  LAYERS_STARTED  : [%s]\n", this.blockLayersStarted.toString());
		System.out.printf("  LAYERS_USED     : [%s]\n", this.blockLayersUsed.toString());
		System.out.printf("  OVERLAYS_STARTED: [%s]\n", this.overlayLayersStarted.toString());
		System.out.printf("  OVERLAYS_USED   : [%s]\n", this.overlayLayersUsed.toString());
	}

	@Override
	public void close() throws Exception
	{
		this.clearAll();
	}
}
