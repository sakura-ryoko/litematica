package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.Litematica;
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

    private final Set<RenderLayer> blockLayersUsed = new ObjectArraySet<>();
    private final Set<RenderLayer> blockLayersStarted = new ObjectArraySet<>();
    private final List<BlockEntity> blockEntities = new ArrayList<>();

    private final boolean[] overlayLayersUsed = new boolean[OverlayRenderType.values().length];
    private final boolean[] overlayLayersStarted = new boolean[OverlayRenderType.values().length];
    private final Map<RenderLayer, BuiltBuffer.SortState> blockBufferStates = new HashMap<>();
    private final Map<OverlayRenderType, BuiltBuffer.SortState> overlayBufferStates = new HashMap<>();
    private final Map<RenderLayer, BuiltBuffer> blockBuffers = new HashMap<>();
    private final Map<OverlayRenderType, BuiltBuffer> overlayBuffers = new HashMap<>();
    private boolean overlayEmpty = true;
    private boolean empty = true;
    private long timeBuilt;

    public boolean isEmpty()
    {
        return this.empty;
    }

    public boolean isBlockLayerEmpty(RenderLayer layer)
    {
        return ! this.blockLayersUsed.contains(layer);
    }

    public void setBlockLayerUsed(RenderLayer layer)
    {
        this.blockLayersUsed.add(layer);
        this.empty = false;
    }

    public boolean isBlockLayerStarted(RenderLayer layer)
    {
        return this.blockLayersStarted.contains(layer);
    }

    public void setBlockLayerStarted(RenderLayer layer)
    {
        this.blockLayersStarted.add(layer);
    }

    public boolean isOverlayEmpty()
    {
        return this.overlayEmpty;
    }

    protected void setOverlayTypeUsed(OverlayRenderType type)
    {
        this.overlayEmpty = false;
        this.overlayLayersUsed[type.ordinal()] = true;
    }

    public boolean isOverlayTypeEmpty(OverlayRenderType type)
    {
        return ! this.overlayLayersUsed[type.ordinal()];
    }

    public void setOverlayTypeStarted(OverlayRenderType type)
    {
        this.overlayLayersStarted[type.ordinal()] = true;
    }

    public boolean isOverlayTypeStarted(OverlayRenderType type)
    {
        return this.overlayLayersStarted[type.ordinal()];
    }

    public void setBlockSortState(RenderLayer layer, @Nonnull BuiltBuffer.SortState state)
    {
        Litematica.logger.warn("setBlockSortState: [RenderData] layer: [{}]", layer.getDrawMode().name());

        this.blockBufferStates.put(layer, state);
    }

    public void setOverlayBufferState(OverlayRenderType type, @Nonnull BuiltBuffer.SortState state)
    {
        Litematica.logger.warn("setOverlayBufferState: [RenderData] type: [{}]", type.getDrawMode().name());

        this.overlayBufferStates.put(type, state);
    }

    @Nullable
    public BuiltBuffer.SortState getBlockSortState(RenderLayer layer)
    {
        Litematica.logger.warn("getBlockSortState: [RenderData] layer: [{}]", layer.getDrawMode().name());

        return this.blockBufferStates.get(layer);
    }

    @Nullable
    public BuiltBuffer.SortState getOverlayBufferState(OverlayRenderType type)
    {
        Litematica.logger.warn("getOverlayBufferState: [RenderData] type: [{}]", type.getDrawMode().name());

        return this.overlayBufferStates.get(type);
    }

    public void setBlockBuffer(RenderLayer layer, @Nonnull BuiltBuffer meshData)
    {
        Litematica.logger.warn("setBlockBuffer: [RenderData] layer: [{}]", layer.getDrawMode().name());

        this.blockBuffers.put(layer, meshData);
    }

    public void setOverlayBlockBuffer(OverlayRenderType type, @Nonnull BuiltBuffer meshData)
    {
        Litematica.logger.warn("setOverlayBlockBuffer: [RenderData] type: [{}]", type.getDrawMode().name());

        this.overlayBuffers.put(type, meshData);
    }

    @Nullable
    public BuiltBuffer getBlockBuffer(RenderLayer layer)
    {
        Litematica.logger.warn("getBlockBuffer: [RenderData] layer: [{}]", layer.getDrawMode().name());

        return this.blockBuffers.get(layer);
    }

    @Nullable
    public BuiltBuffer getOverlayBlockBuffer(OverlayRenderType type)
    {
        Litematica.logger.warn("getOverlayBlockBuffer: [RenderData] type: [{}]", type.getDrawMode().name());

        return this.overlayBuffers.get(type);
    }

    public List<BlockEntity> getBlockEntities()
    {
        return this.blockEntities;
    }

    public void addBlockEntity(BlockEntity be)
    {
        this.blockEntities.add(be);
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
