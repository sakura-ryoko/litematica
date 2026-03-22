package fi.dy.masa.litematica.render.schematic;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ChunkMeshDataSchematic implements AutoCloseable
{
    public static final ChunkMeshDataSchematic EMPTY = new ChunkMeshDataSchematic()
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
        protected void setLayerUsed(RenderType layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void setLayerStarted(RenderType layer)
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

    private final List<BlockEntity> blockEntities;
    private final List<BlockEntity> noCullBlockEntities;
    private final Set<ChunkSectionLayer> blockLayersUsed;
    private final Set<ChunkSectionLayer> blockLayersStarted;
    private final Set<RenderType> layersUsed;
    private final Set<RenderType> layersStarted;
    private final Set<OverlayRenderType> overlayLayersUsed;
    private final Set<OverlayRenderType> overlayLayersStarted;
    private final ChunkMeshCache chunkMeshCache;
    private final HashMap<ChunkSectionLayer, DrawState> drawStates;
    private final Map<ChunkSectionLayer, MeshData.SortState> blockSortingData;
    private final Map<RenderType, MeshData.SortState> layerSortingData;
    private final Map<OverlayRenderType, MeshData.SortState> overlaySortingData;
    private boolean blocksEmpty;
    private boolean layerEmpty;
    private boolean overlayEmpty;
    private long timeBuilt;

	public ChunkMeshDataSchematic()
	{
		this.blockEntities = new ArrayList<>();
		this.noCullBlockEntities = new ArrayList<>();
		this.blockLayersUsed = new ObjectArraySet<>();
		this.blockLayersStarted = new ObjectArraySet<>();
		this.layersUsed = new ObjectArraySet<>();
		this.layersStarted = new ObjectArraySet<>();
		this.overlayLayersUsed = new ObjectArraySet<>();
		this.overlayLayersStarted = new ObjectArraySet<>();
		this.chunkMeshCache = new ChunkMeshCache();
        this.drawStates = new HashMap<>();
		this.blockSortingData = new HashMap<>();
		this.layerSortingData = new HashMap<>();
		this.overlaySortingData = new HashMap<>();
		this.blocksEmpty = true;
		this.layerEmpty = true;
		this.overlayEmpty = true;
	}

    public boolean isBlockLayerEmpty()
    {
        return this.blocksEmpty;
    }

    public boolean isLayerEmpty()
    {
        return this.layerEmpty;
    }

    public int getStartedSize()
    {
        return this.blockLayersStarted.size() + this.layersStarted.size() + this.overlayLayersStarted.size();
    }

    public int getUsedSize()
    {
        return this.blockLayersUsed.size() + this.layersUsed.size() + this.overlayLayersUsed.size();
    }

    public int getSize()
    {
        return Math.max(this.getStartedSize(), this.getUsedSize());
    }

    public boolean isBlockLayerEmpty(ChunkSectionLayer layer)
    {
        return !this.blockLayersUsed.contains(layer);
    }

    public boolean isLayerEmpty(RenderType layer)
    {
        return !this.layersUsed.contains(layer);
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

    public boolean isLayerStarted(RenderType layer)
    {
        return this.layersStarted.contains(layer);
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

    protected void setLayerStarted(RenderType layer)
    {
        this.layersStarted.add(layer);
    }

    protected void setLayerUsed(RenderType layer)
    {
        this.layerEmpty = false;
        this.layersUsed.add(layer);
    }

    protected void setBlockLayerUnused(RenderType layer)
    {
        this.layersStarted.remove(layer);
        this.layersUsed.remove(layer);
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
        this.blockEntities.add(be);
    }

    protected void addNoCullBlockEntity(BlockEntity be)
    {
        this.noCullBlockEntities.add(be);
    }

    protected ChunkMeshCache getChunkMeshCache()
    {
        return this.chunkMeshCache;
    }

    protected void closeChunkMeshCache()
    {
        this.chunkMeshCache.closeAll();
    }

    public boolean hasTransparentSortingDataForBlockLayer(ChunkSectionLayer layer)
    {
        return this.blockSortingData.get(layer) != null;
    }

    public boolean hasTransparentSortingDataForLayer(RenderType layer)
    {
        return this.layerSortingData.get(layer) != null;
    }

    public boolean hasTransparentSortingDataForOverlay(OverlayRenderType type)
    {
        return this.overlaySortingData.get(type) != null;
    }

    protected void setTransparentSortingDataForBlockLayer(ChunkSectionLayer layer, @Nonnull MeshData.SortState transparentSortingData)
    {
        this.blockSortingData.put(layer, transparentSortingData);
    }

    protected void setTransparentSortingDataForLayer(RenderType layer, @Nonnull MeshData.SortState transparentSortingData)
    {
        this.layerSortingData.put(layer, transparentSortingData);
    }

    protected void setTransparentSortingDataForOverlay(OverlayRenderType type, @Nonnull MeshData.SortState transparentSortingData)
    {
        this.overlaySortingData.put(type, transparentSortingData);
    }

    protected MeshData.SortState getTransparentSortingDataForBlockLayer(ChunkSectionLayer layer)
    {
        return this.blockSortingData.get(layer);
    }

    protected MeshData.SortState getTransparentSortingDataForLayer(RenderType layer)
    {
        return this.layerSortingData.get(layer);
    }

    @Nullable
    protected MeshData.SortState getTransparentSortingDataForOverlay(OverlayRenderType type)
    {
        return this.overlaySortingData.get(type);
    }

    protected void compileDrawStates()
    {
        this.drawStates.clear();

        for (ChunkSectionLayer layer : this.blockLayersUsed)
        {
            MeshData mesh = this.chunkMeshCache.getMeshByBlockLayer(layer);

            if (mesh != null)
            {
                this.drawStates.put(layer, new DrawState(mesh.drawState().indexCount(), mesh.drawState().indexType(), mesh.indexBuffer() != null));
            }
        }
    }

    @Nullable
    public DrawState getDrawState(ChunkSectionLayer layer)
    {
        return this.drawStates.get(layer);
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
        this.closeChunkMeshCache();
        this.timeBuilt = 0;
        this.drawStates.clear();
        this.overlaySortingData.clear();
        this.layerSortingData.clear();
        this.blockSortingData.clear();
        this.blockLayersUsed.clear();
        this.layersUsed.clear();
        this.overlayLayersUsed.clear();
        this.blockLayersStarted.clear();
        this.layersStarted.clear();
        this.overlayLayersStarted.clear();
        this.blockEntities.clear();
        this.noCullBlockEntities.clear();
        this.overlayEmpty = true;
        this.layerEmpty = true;
        this.blocksEmpty = true;
    }

    @Override
    public void close() throws Exception
    {
        this.clearAll();
    }

    public record DrawState(int indexCount, VertexFormat.IndexType indexType, boolean hasIndexBuffer) {}
}
