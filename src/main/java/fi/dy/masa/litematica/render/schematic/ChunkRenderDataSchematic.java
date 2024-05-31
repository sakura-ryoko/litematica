package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class ChunkRenderDataSchematic
{
    public static final ChunkRenderDataSchematic EMPTY = new ChunkRenderDataSchematic() {
        @Override
        public void setBlockLayerUsed(RenderLayer layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setBlockLayerStarted(RenderLayer layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOverlayTypeUsed(OverlayRenderType layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOverlayTypeStarted(OverlayRenderType layer)
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

    public void setBlockLayerStarted(RenderLayer layer)
    {
        this.blockLayersStarted.add(layer);
    }

    public void setBlockLayerUsed(RenderLayer layer)
    {
        this.empty = false;
        this.blockLayersUsed.add(layer);
    }

    public boolean isOverlayEmpty()
    {
        return this.overlayEmpty;
    }

    public boolean isOverlayTypeEmpty(OverlayRenderType type)
    {
        return !this.overlayLayersUsed.contains(type);
    }

    public void setOverlayTypeStarted(OverlayRenderType type)
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

    public List<BlockEntity> getBlockEntities()
    {
        return this.blockEntities;
    }

    public void addBlockEntity(BlockEntity be)
    {
        this.blockEntities.add(be);
    }

    public BuiltBufferCache getBuiltBufferCache()
    {
        return this.builtBufferCache;
    }

    public void closeBuiltBufferCache()
    {
        this.builtBufferCache.closeAll();
    }

    public boolean hasTransparentSortingData()
    {
        return this.transparentSortingData != null;
    }

    public void setTransparentSortingData(@Nonnull BuiltBuffer.SortState transparentSortingData)
    {
        this.transparentSortingData = transparentSortingData;
    }

    public BuiltBuffer.SortState getTransparentSortingData()
    {
        return this.transparentSortingData;
    }

    public long getTimeBuilt()
    {
        return this.timeBuilt;
    }

    public void setTimeBuilt(long time)
    {
        this.timeBuilt = time;
    }
}
