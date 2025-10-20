package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;

public class ChunkRenderDataSchematic implements AutoCloseable
{
    public static final ChunkRenderDataSchematic EMPTY = new ChunkRenderDataSchematic() {
        @Override
        protected void setBlockLayerUsed(BlockRenderLayer layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void setBlockLayerStarted(BlockRenderLayer layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void setLayerUsed(RenderLayer layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void setLayerStarted(RenderLayer layer)
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
    private final Set<BlockRenderLayer> blockLayersUsed;
    private final Set<BlockRenderLayer> blockLayersStarted;
    private final Set<RenderLayer> layersUsed;
    private final Set<RenderLayer> layersStarted;
    private final Set<OverlayRenderType> overlayLayersUsed;
    private final Set<OverlayRenderType> overlayLayersStarted;
    private final BuiltBufferCache builtBufferCache;
    private final Map<BlockRenderLayer, BuiltBuffer.SortState> blockSortingData;
    private final Map<RenderLayer, BuiltBuffer.SortState> layerSortingData;
    private final Map<OverlayRenderType, BuiltBuffer.SortState> overlaySortingData;
    private boolean blocksEmpty;
    private boolean layerEmpty;
    private boolean overlayEmpty;
    private long timeBuilt;

	public ChunkRenderDataSchematic()
	{
		this.blockEntities = new ArrayList<>();
		this.noCullBlockEntities = new ArrayList<>();
		this.blockLayersUsed = new ObjectArraySet<>();
		this.blockLayersStarted = new ObjectArraySet<>();
		this.layersUsed = new ObjectArraySet<>();
		this.layersStarted = new ObjectArraySet<>();
		this.overlayLayersUsed = new ObjectArraySet<>();
		this.overlayLayersStarted = new ObjectArraySet<>();
		this.builtBufferCache = new BuiltBufferCache();
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

    public boolean isBlockLayerEmpty(BlockRenderLayer layer)
    {
        return !this.blockLayersUsed.contains(layer);
    }

    public boolean isLayerEmpty(RenderLayer layer)
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

    public boolean isBlockLayerStarted(BlockRenderLayer layer)
    {
        return this.blockLayersStarted.contains(layer);
    }

    public boolean isLayerStarted(RenderLayer layer)
    {
        return this.layersStarted.contains(layer);
    }

    public boolean isOverlayTypeStarted(OverlayRenderType type)
    {
        return this.overlayLayersStarted.contains(type);
    }

    protected void setBlockLayerStarted(BlockRenderLayer layer)
    {
        this.blockLayersStarted.add(layer);
    }

    protected void setBlockLayerUsed(BlockRenderLayer layer)
    {
        this.blocksEmpty = false;
        this.blockLayersUsed.add(layer);
    }

    protected void setBlockLayerUnused(BlockRenderLayer layer)
    {
        this.blockLayersStarted.remove(layer);
        this.blockLayersUsed.remove(layer);
    }

    protected void setLayerStarted(RenderLayer layer)
    {
        this.layersStarted.add(layer);
    }

    protected void setLayerUsed(RenderLayer layer)
    {
        this.layerEmpty = false;
        this.layersUsed.add(layer);
    }

    protected void setBlockLayerUnused(RenderLayer layer)
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

    protected BuiltBufferCache getBuiltBufferCache()
    {
        return this.builtBufferCache;
    }

    protected void closeBuiltBufferCache()
    {
        this.builtBufferCache.closeAll();
    }

    public boolean hasTransparentSortingDataForBlockLayer(BlockRenderLayer layer)
    {
        return this.blockSortingData.get(layer) != null;
    }

    public boolean hasTransparentSortingDataForLayer(RenderLayer layer)
    {
        return this.layerSortingData.get(layer) != null;
    }

    public boolean hasTransparentSortingDataForOverlay(OverlayRenderType type)
    {
        return this.overlaySortingData.get(type) != null;
    }

    protected void setTransparentSortingDataForBlockLayer(BlockRenderLayer layer, @Nonnull BuiltBuffer.SortState transparentSortingData)
    {
        this.blockSortingData.put(layer, transparentSortingData);
    }

    protected void setTransparentSortingDataForLayer(RenderLayer layer, @Nonnull BuiltBuffer.SortState transparentSortingData)
    {
        this.layerSortingData.put(layer, transparentSortingData);
    }

    protected void setTransparentSortingDataForOverlay(OverlayRenderType type, @Nonnull BuiltBuffer.SortState transparentSortingData)
    {
        this.overlaySortingData.put(type, transparentSortingData);
    }

    protected BuiltBuffer.SortState getTransparentSortingDataForBlockLayer(BlockRenderLayer layer)
    {
        return this.blockSortingData.get(layer);
    }

    protected BuiltBuffer.SortState getTransparentSortingDataForLayer(RenderLayer layer)
    {
        return this.layerSortingData.get(layer);
    }

    @Nullable
    protected BuiltBuffer.SortState getTransparentSortingDataForOverlay(OverlayRenderType type)
    {
        return this.overlaySortingData.get(type);
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
        this.closeBuiltBufferCache();
        this.timeBuilt = 0;
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
}
