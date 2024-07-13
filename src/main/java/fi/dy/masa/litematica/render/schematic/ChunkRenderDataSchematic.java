package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;

public class ChunkRenderDataSchematic implements AutoCloseable
{
    public static final ChunkRenderDataSchematic EMPTY = new ChunkRenderDataSchematic() {
        @Override
        protected void setBlockLayerUsed(RenderLayer layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void setBlockLayerStarted(RenderLayer layer)
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

    private final List<BlockEntity> blockEntities = new ArrayList<>();
    private final Set<RenderLayer> blockLayersUsed = new ObjectArraySet<>();
    private final Set<RenderLayer> blockLayersStarted = new ObjectArraySet<>();
    private final Set<OverlayRenderType> overlayLayersUsed = new ObjectArraySet<>();
    private final Set<OverlayRenderType> overlayLayersStarted = new ObjectArraySet<>();
    private final BuiltBufferCache builtBufferCache = new BuiltBufferCache();
    private final Map<OverlayRenderType, BuiltBuffer.SortState> overlaySortingData = new HashMap<>();
    private BuiltBuffer.SortState transparentSortingData = null;
    private boolean overlayEmpty = true;
    private boolean empty = true;
    private long timeBuilt;

    public boolean isEmpty()
    {
        return this.empty;
    }

    public boolean isBlockLayerEmpty(RenderLayer layer)
    {
        return !this.blockLayersUsed.contains(layer);
    }

    public boolean isBlockLayerStarted(RenderLayer layer)
    {
        return this.blockLayersStarted.contains(layer);
    }

    protected void setBlockLayerStarted(RenderLayer layer)
    {
        this.blockLayersStarted.add(layer);
    }

    protected void setBlockLayerUsed(RenderLayer layer)
    {
        this.empty = false;
        this.blockLayersUsed.add(layer);
    }

    protected void setBlockLayerUnused(RenderLayer layer)
    {
        this.blockLayersStarted.remove(layer);
        this.blockLayersUsed.remove(layer);
    }

    public boolean isOverlayEmpty()
    {
        return this.overlayEmpty;
    }

    public boolean isOverlayTypeEmpty(OverlayRenderType type)
    {
        return !this.overlayLayersUsed.contains(type);
    }

    protected void setOverlayTypeStarted(OverlayRenderType type)
    {
        this.overlayLayersStarted.add(type);
    }

    public boolean isOverlayTypeStarted(OverlayRenderType type)
    {
        return this.overlayLayersStarted.contains(type);
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

    protected void addBlockEntity(BlockEntity be)
    {
        this.blockEntities.add(be);
    }

    protected BuiltBufferCache getBuiltBufferCache()
    {
        return this.builtBufferCache;
    }

    protected void closeBuiltBufferCache()
    {
        this.builtBufferCache.closeAll();
    }

    public boolean hasTransparentSortingData()
    {
        return this.transparentSortingData != null;
    }

    public boolean hasTransparentSortingDataForOverlay(OverlayRenderType type)
    {
        return this.overlaySortingData.get(type) != null;
    }

    protected void setTransparentSortingData(@Nonnull BuiltBuffer.SortState transparentSortingData)
    {
        this.transparentSortingData = transparentSortingData;
    }

    protected void setTransparentSortingDataForOverlay(OverlayRenderType type, @Nonnull BuiltBuffer.SortState transparentSortingData)
    {
        this.overlaySortingData.put(type, transparentSortingData);
    }

    protected BuiltBuffer.SortState getTransparentSortingData()
    {
        return this.transparentSortingData;
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
        this.transparentSortingData = null;
        this.blockLayersUsed.clear();
        this.overlayLayersUsed.clear();
        this.blockLayersStarted.clear();
        this.overlayLayersStarted.clear();
        this.blockEntities.clear();
        this.overlayEmpty = true;
        this.empty = true;
    }

    @Override
    public void close() throws Exception
    {
        this.clearAll();
    }
}
