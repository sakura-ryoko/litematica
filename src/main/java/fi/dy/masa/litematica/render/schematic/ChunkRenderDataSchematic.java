package fi.dy.masa.litematica.render.schematic;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import fi.dy.masa.litematica.Litematica;

public class ChunkRenderDataSchematic implements AutoCloseable
{
	private static final Logger LOGGER = Litematica.LOGGER;
	public static final Comparator<ChunkRenderDataSchematic> COMPARATOR = new RenderDataComparator();
	public static final ChunkRenderDataSchematic EMPTY = new ChunkRenderDataSchematic()
	{
		@Override
		protected void setBlockLayerUsed(ChunkSectionLayer layer)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setBlockLayerUnused(ChunkSectionLayer layer)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected @Nullable ChunkMeshDataSchematic updateMeshDataCache(ChunkMeshDataSchematic meshData)
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
		protected void setOverlayTypeUnused(OverlayRenderType type)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setTimeBuilt(long time)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isEmpty()
		{
			return true;
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
		this.meshDataCache = new ChunkMeshDataSchematic();
		this.blockLayersUsed = EnumSet.noneOf(ChunkSectionLayer.class);
		this.blockLayersStarted = EnumSet.noneOf(ChunkSectionLayer.class);
		this.overlayLayersUsed = EnumSet.noneOf(OverlayRenderType.class);
		this.overlayLayersStarted = EnumSet.noneOf(OverlayRenderType.class);
		this.blocksEmpty = true;
		this.overlayEmpty = true;
	}

	public ChunkMeshDataSchematic getMeshDataCache()
	{
		return this.meshDataCache;
	}

	@Nullable
	protected ChunkMeshDataSchematic updateMeshDataCache(ChunkMeshDataSchematic meshData)
	{
//		LOGGER.warn("[RD] updateMeshDataCache()");
		ChunkMeshDataSchematic oldData = null;

		if (this.meshDataCache != null || !this.meshDataCache.isEmpty())
		{
			int comparator = ChunkMeshDataSchematic.COMPARATOR.compare(this.meshDataCache, meshData);
//			LOGGER.error("[RD] updateMeshDataCache() compare: [{}] // oldData DUMP -->", comparator);
//			this.meshDataCache.dumpMeshDataDebug();

			if (comparator > 0)
			{
//				LOGGER.error("[RD] updateMeshDataCache() oldData CLEAR");
				oldData = this.meshDataCache;
//				this.meshDataCache.clearAll();
				this.meshDataCache = meshData;
			}
//			else
//			{
//				// Don't update
//				LOGGER.error("[RD] updateMeshDataCache() oldData SAVE");
//			}
		}
		else
		{
//			LOGGER.error("[RD] updateMeshDataCache() oldData EMPTY/NULL --> newData");
			this.meshDataCache = meshData;
		}

//		LOGGER.error("[RD] updateMeshDataCache() newData DUMP -->");
//		this.meshDataCache.dumpMeshDataDebug();

		return oldData;
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

	public boolean isEmpty()
	{
		return (this.blocksEmpty && this.overlayEmpty) && this.meshDataCache.isEmpty() && this.timeBuilt < 1L;
	}

	protected void clearAll()
	{
		if (this.meshDataCache != null)
		{
			this.meshDataCache.clearAll();
		}

		this.meshDataCache = new ChunkMeshDataSchematic();
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
		if (this.isEmpty())
		{
			System.out.print("[RD] ChunkRenderDataSchematic --> EMPTY\n");
		}
		else
		{
			System.out.printf("[RD] ChunkRenderDataSchematic; timeBuilt: [%d]\n", this.getTimeBuilt());
		}

		if (this.meshDataCache != null)
		{
			this.meshDataCache.dumpMeshDataDebug();
		}
		else
		{
			System.out.print("[RD] ChunkRenderDataSchematic // ChunkMeshDataSchematic --> NULL\n");
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

	public static class RenderDataComparator implements Comparator<ChunkRenderDataSchematic>
	{
		@Override
		public int compare(ChunkRenderDataSchematic o1, ChunkRenderDataSchematic o2)
		{
			if (o1.isEmpty()) { return 1; }
			else if (o2.isEmpty()) { return -1; }
			final int timeCompare = Long.compare(o1.timeBuilt, o2.timeBuilt);
//			System.out.printf("[RDC] timeBuilt: [%d] vs [%d] --> [%d]\n", o1.timeBuilt, o2.timeBuilt, -timeCompare);

			if (timeCompare != 0)
			{
				return -timeCompare;
			}

			final int layersStartedCompare = Integer.compare(o1.blockLayersStarted.size(), o2.blockLayersStarted.size());
			final int overlaysStartedCompare = Integer.compare(o1.overlayLayersStarted.size(), o2.overlayLayersStarted.size());

			if (layersStartedCompare != 0 || overlaysStartedCompare != 0)
			{
				return layersStartedCompare > 0 ? 1 : overlaysStartedCompare;
			}

			final int layersUsedCompare = Integer.compare(o1.blockLayersUsed.size(), o2.blockLayersUsed.size());
			final int overlaysUsedCompare = Integer.compare(o1.overlayLayersUsed.size(), o2.overlayLayersUsed.size());

			if (layersUsedCompare != 0 || overlaysUsedCompare != 0)
			{
				return layersUsedCompare > 0 ? 1 : overlaysUsedCompare;
			}

			return ChunkMeshDataSchematic.COMPARATOR.compare(o1.meshDataCache, o2.meshDataCache);
		}
	}
}
