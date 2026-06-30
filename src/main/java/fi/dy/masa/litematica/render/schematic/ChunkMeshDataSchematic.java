package fi.dy.masa.litematica.render.schematic;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.entity.BlockEntity;

import fi.dy.masa.litematica.Litematica;

public class ChunkMeshDataSchematic implements AutoCloseable
{
	private static final Logger LOGGER = Litematica.LOGGER;
	public static final Comparator<ChunkMeshDataSchematic> COMPARATOR = new MeshDataComparator();
	public static final ChunkMeshDataSchematic UNCOMPILED = new ChunkMeshDataSchematic()
	{
		@Override
		public boolean canSeeEachOther(final Direction direction1, final Direction direction2)
		{
			return false;
		}
	};
	public static final ChunkMeshDataSchematic EMPTY = new ChunkMeshDataSchematic()
	{
		@Override
		protected void saveMeshData(ChunkSectionLayer layer, @NonNull MeshData meshData)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void saveMeshData(OverlayRenderType type, @NonNull MeshData meshData)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean canSeeEachOther(final Direction direction1, final Direction direction2)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isEmpty()
		{
			return true;
		}

		@Override
		protected void addBlockEntity(BlockEntity be)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void addNoCullBlockEntity(BlockEntity be)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setTransparentSortingDataForBlockLayer(ChunkSectionLayer layer, MeshData.@NonNull SortState transparentSortingData)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setTransparentSortingDataForOverlay(OverlayRenderType type, MeshData.@NonNull SortState transparentSortingData)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void compileLayerDrawStates(Set<ChunkSectionLayer> blockLayersUsed)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void compileOverlayDrawStates(Set<OverlayRenderType> overlaysUsed)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected void updateVisibility(VisGraph visGraph)
		{
			throw new UnsupportedOperationException();
		}
	};

	private final ChunkMeshCache chunkMeshCache;
	private final HashMap<ChunkSectionLayer, DrawState> blockDrawStates;
	private final HashMap<OverlayRenderType, DrawState> overlayDrawStates;
	private final Map<ChunkSectionLayer, AtomicBoolean> blockVboUploaded;
	private final Map<ChunkSectionLayer, AtomicBoolean> blockIboUploaded;
	private final Map<OverlayRenderType, AtomicBoolean> overlayVboUploaded;
	private final Map<OverlayRenderType, AtomicBoolean> overlayIboUploaded;
	private final Map<ChunkSectionLayer, MeshData.SortState> blockSortingData;
	private final Map<OverlayRenderType, MeshData.SortState> overlaySortingData;
	private final List<BlockEntity> blockEntities;
	private final List<BlockEntity> noCullBlockEntities;
	private VisibilitySet visibility;
//    private TranslucencyPointOfView translucentPov;
	private long timeBuilt;

	protected ChunkMeshDataSchematic()
	{
		this.timeBuilt = 0L;
		this.chunkMeshCache = new ChunkMeshCache();
		this.blockEntities = new ArrayList<>();
		this.noCullBlockEntities = new ArrayList<>();
		this.visibility = new VisibilitySet();
//        this.translucentPov = new TranslucencyPointOfView();

		this.blockSortingData = new HashMap<>();
		this.blockDrawStates = new HashMap<>();
		this.blockVboUploaded = Util.makeEnumMap(ChunkSectionLayer.class, layer -> new AtomicBoolean());
		this.blockIboUploaded = Util.makeEnumMap(ChunkSectionLayer.class, layer -> new AtomicBoolean());

		this.overlaySortingData = new HashMap<>();
		this.overlayDrawStates = new HashMap<>();
		this.overlayVboUploaded = Util.makeEnumMap(OverlayRenderType.class, type -> new AtomicBoolean());
		this.overlayIboUploaded = Util.makeEnumMap(OverlayRenderType.class, type -> new AtomicBoolean());
	}

	protected ChunkMeshCache getChunkMeshCache()
	{
		return this.chunkMeshCache;
	}

	protected void saveMeshData(ChunkSectionLayer layer, @Nonnull MeshData meshData)
	{
		this.chunkMeshCache.saveMeshData(layer, meshData);
//		LOGGER.warn("[Mesh] saveMeshData(): layer: [{}] --> VBO-POS: [{}]", layer.label(), meshData.vertexBuffer().position());
	}

	protected void saveMeshData(OverlayRenderType type, @Nonnull MeshData meshData)
	{
		this.chunkMeshCache.saveMeshData(type, meshData);
//		LOGGER.warn("[Mesh] saveMeshData(): type: [{}] --> VBO-POS: [{}]", type.name(), meshData.vertexBuffer().position());
	}

	protected boolean hasMeshData(ChunkSectionLayer layer)
	{
		return this.chunkMeshCache.hasMeshData(layer);
	}

	protected boolean hasMeshData(OverlayRenderType type)
	{
		return this.chunkMeshCache.hasMeshData(type);
	}

	@Nullable
	protected MeshData getMeshDataOrNull(ChunkSectionLayer layer)
	{
		return this.chunkMeshCache.getMeshDataOrNull(layer);
	}

	@Nullable
	protected MeshData getMeshDataOrNull(OverlayRenderType type)
	{
		return this.chunkMeshCache.getMeshDataOrNull(type);
	}

	private void closeChunkMeshCache()
	{
		this.chunkMeshCache.closeAll();
	}

	public boolean canSeeEachOther(final Direction direction1, final Direction direction2)
	{
		return this.visibility.visibilityBetween(direction1, direction2);
	}

	public boolean isEmpty()
	{
		return  this.blockDrawStates.isEmpty() && this.overlayDrawStates.isEmpty() &&
				this.blockEntities.isEmpty() && this.noCullBlockEntities.isEmpty() &&
				this.timeBuilt < 1L;
	}

	public List<BlockEntity> getBlockEntities()
	{
		return this.blockEntities;
	}

	public List<BlockEntity> getNoCullBlockEntities()
	{
		return this.noCullBlockEntities;
	}

	protected void addBlockEntity(BlockEntity be)
	{
//		LOGGER.warn("[Mesh] addBlockEntity(): type: [{}], pos: [{}], level: [{}]", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString(), be.getBlockPos(), be.getLevel().dimension().identifier().toString());
		this.blockEntities.add(be);
	}

	protected void addNoCullBlockEntity(BlockEntity be)
	{
//		LOGGER.warn("[Mesh] addNoCullBlockEntity(): type: [{}], pos: [{}], level: [{}]", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString(), be.getBlockPos(), be.getLevel().dimension().identifier().toString());
		this.noCullBlockEntities.add(be);
	}

	public boolean hasTransparentSortingDataForBlockLayer(ChunkSectionLayer layer)
	{
		return this.blockSortingData.get(layer) != null;
	}

	protected void setTransparentSortingDataForBlockLayer(ChunkSectionLayer layer, @Nonnull MeshData.SortState transparentSortingData)
	{
		this.blockSortingData.put(layer, transparentSortingData);
	}

	protected MeshData.SortState getTransparentSortingDataForBlockLayer(ChunkSectionLayer layer)
	{
		return this.blockSortingData.get(layer);
	}

	public boolean hasTransparentSortingDataForOverlay(OverlayRenderType type)
	{
		return this.overlaySortingData.get(type) != null;
	}

	protected void setTransparentSortingDataForOverlay(OverlayRenderType type, @Nonnull MeshData.SortState transparentSortingData)
	{
		this.overlaySortingData.put(type, transparentSortingData);
	}

	@Nullable
	protected MeshData.SortState getTransparentSortingDataForOverlay(OverlayRenderType type)
	{
		return this.overlaySortingData.get(type);
	}

	protected void compileLayerDrawStates(Set<ChunkSectionLayer> blockLayersUsed)
	{
//		LOGGER.warn("[Mesh] compileLayerDrawStates() --> {}", blockLayersUsed.toString());
		this.blockDrawStates.clear();

		for (ChunkSectionLayer layer : blockLayersUsed)
		{
			MeshData meshData = this.getMeshDataOrNull(layer);

			if (meshData != null)
			{
//				LOGGER.warn("[Mesh] compileLayerDrawStates(): layer: [{}] --> STORE", layer.label());
				this.blockDrawStates.put(layer, new ChunkMeshDataSchematic.DrawState(meshData.drawState().indexCount(), meshData.drawState().indexType(), meshData.indexBuffer() != null));
			}
		}
	}

	protected void compileOverlayDrawStates(Set<OverlayRenderType> overlaysUsed)
	{
//		LOGGER.warn("[Mesh] compileOverlayDrawStates() --> {}", overlaysUsed.toString());
		this.overlayDrawStates.clear();

		for (OverlayRenderType type : overlaysUsed)
		{
			MeshData meshData = this.getMeshDataOrNull(type);

			if (meshData != null)
			{
//				LOGGER.warn("[Mesh] compileLayerDrawStates(): type: [{}] --> STORE", type.name());
				this.overlayDrawStates.put(type, new ChunkMeshDataSchematic.DrawState(meshData.drawState().indexCount(), meshData.drawState().indexType(), meshData.indexBuffer() != null));
			}
		}
	}

	@Nullable
	public ChunkMeshDataSchematic.DrawState getDrawState(ChunkSectionLayer layer)
	{
		return this.blockDrawStates.get(layer);
	}

	@Nullable
	public ChunkMeshDataSchematic.DrawState getDrawState(OverlayRenderType type)
	{
		return this.overlayDrawStates.get(type);
	}

	public boolean hasVBOUpload(final ChunkSectionLayer layer)
	{
		return this.blockVboUploaded.get(layer).get();
	}

	public boolean hasIBOUpload(final ChunkSectionLayer layer)
	{
		return this.blockIboUploaded.get(layer).get();
	}

	public boolean hasVBOUpload(final OverlayRenderType type)
	{
		return this.overlayVboUploaded.get(type).get();
	}

	public boolean hasIBOUpload(final OverlayRenderType type)
	{
		return this.overlayIboUploaded.get(type).get();
	}

	public void markVBOUploaded(final ChunkSectionLayer layer)
	{
		this.blockVboUploaded.get(layer).set(true);
	}

	public void markIBOUploaded(final ChunkSectionLayer layer)
	{
		this.blockIboUploaded.get(layer).set(true);
	}

	public void markVBOUploaded(final OverlayRenderType type)
	{
		this.overlayVboUploaded.get(type).set(true);
	}

	public void markIBOUploaded(final OverlayRenderType type)
	{
		this.overlayIboUploaded.get(type).set(true);
	}

	public VisibilitySet getVisibility()
	{
		return this.visibility;
	}

	protected void updateVisibility(VisGraph visGraph)
	{
		this.visibility = visGraph.resolve();
	}

//    public TranslucencyPointOfView getTranslucencyPointOfView()
//    {
//        return this.translucentPov;
//    }
//
//    protected void updateTranslucencyPointOfView(TranslucencyPointOfView pov)
//    {
//        this.translucentPov = pov;
//    }

	protected void setTimeBuilt(long time)
	{
		this.timeBuilt = time;
	}

	public long getTimeBuilt()
	{
		return this.timeBuilt;
	}

	protected void clearTileCache()
	{
		this.blockEntities.clear();
		this.noCullBlockEntities.clear();
	}

	protected void clearAll()
	{
		this.closeChunkMeshCache();

		this.blockDrawStates.clear();
		this.blockSortingData.clear();
		this.blockVboUploaded.clear();
		this.blockIboUploaded.clear();

		this.overlayDrawStates.clear();
		this.overlaySortingData.clear();
		this.overlayVboUploaded.clear();
		this.overlayIboUploaded.clear();

		this.blockEntities.clear();
		this.noCullBlockEntities.clear();
		this.timeBuilt = 0L;
	}

	protected void dumpMeshDataDebug()
	{
		if (this.isEmpty())
		{
			System.out.print("[Mesh] ChunkMeshDataSchematic --> EMPTY\n");
		}
		else
		{
			System.out.printf("[Mesh] ChunkMeshDataSchematic; timeBuilt: [%d]\n", this.getTimeBuilt());
		}

		System.out.printf("  [BLOCK_STATES]  : %d\n", this.blockDrawStates.size());
		System.out.printf("  [OVERLAY_STATES]: %d\n", this.overlayDrawStates.size());
		System.out.printf("  [TILE_COUNT]   : %d\n", this.blockEntities.size());
		System.out.printf("  [TILES_NO_CULL]: %d\n", this.noCullBlockEntities.size());
	}

	@Override
	public void close() throws Exception
	{
		this.clearAll();
	}

	public record DrawState(int indexCount, VertexFormat.IndexType indexType, boolean hasIndexBuffer)
	{
	}

	public static class MeshDataComparator implements Comparator<ChunkMeshDataSchematic>
	{
		@Override
		public int compare(ChunkMeshDataSchematic o1, ChunkMeshDataSchematic o2)
		{
			if (o1.isEmpty()) { return 1; }
			if (o2.isEmpty()) { return -1; }
			final int timeCompare = Long.compare(o1.timeBuilt, o2.timeBuilt);
//			System.out.printf("[Mesh] timeBuilt: [%d] vs [%d] --> [%d]\n", o1.timeBuilt, o2.timeBuilt, -timeCompare);

			if (timeCompare != 0)
			{
				return -timeCompare;
			}

			final int blockStates = Integer.compare(o1.blockDrawStates.size(), o2.blockDrawStates.size());
			final int overlayStates = Integer.compare(o1.overlayDrawStates.size(), o2.overlayDrawStates.size());

			if (blockStates != 0 || overlayStates != 0)
			{
				return blockStates > 0 ? 1 : overlayStates;
			}

			final int tileEntities = Integer.compare(o1.blockEntities.size(), o2.blockEntities.size());
			final int noCullBlocks = Integer.compare(o1.noCullBlockEntities.size(), o2.noCullBlockEntities.size());

			if (tileEntities != 0 || noCullBlocks != 0)
			{
				return tileEntities > 0 ? 1 : noCullBlocks;
			}

			return 0;
		}
	}
}
