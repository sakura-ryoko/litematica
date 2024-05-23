package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.class_9801;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
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

    private final Set<RenderLayer> blockLayersUsed = new ObjectArraySet<>();
    private final Set<RenderLayer> blockLayersStarted = new ObjectArraySet<>();
    private final List<BlockEntity> blockEntities = new ArrayList<>();

    private final boolean[] overlayLayersUsed = new boolean[OverlayRenderType.values().length];
    private final boolean[] overlayLayersStarted = new boolean[OverlayRenderType.values().length];
    // FIXME MeshData.SortState? (was BufferBuilder.TransparentSortingData)
    private final Map<RenderLayer, class_9801.class_9802> blockBufferStates = new HashMap<>();
    private final Map<RenderLayer, VertexBuffer> vertexBuffers = new HashMap<>();
    private final class_9801.class_9802[] overlayBufferStates = new class_9801.class_9802[OverlayRenderType.values().length];
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

    public class_9801.class_9802 getBlockBufferState(RenderLayer layer)
    {
        return this.blockBufferStates.get(layer);
    }

    public void setBlockBufferState(RenderLayer layer, class_9801.class_9802 state)
    {
        this.blockBufferStates.put(layer, state);
    }

    public class_9801.class_9802 getOverlayBufferState(OverlayRenderType type)
    {
        return this.overlayBufferStates[type.ordinal()];
    }

    public void setOverlayBufferState(OverlayRenderType type, class_9801.class_9802 state)
    {
        this.overlayBufferStates[type.ordinal()] = state;
    }

    /*
    public VertexBuffer getVertexBuffer(RenderLayer layer)
    {
        if (this.vertexBuffers.containsKey(layer) == false)
        {
            this.setVertexBuffer(layer, new VertexBuffer(VertexBuffer.Usage.STATIC));
        }

        return this.vertexBuffers.get(layer);
    }

    public void setVertexBuffer(RenderLayer layer, VertexBuffer vertexBuffer)
    {
        this.vertexBuffers.put(layer, vertexBuffer);
    }

    public void closeVertexBuffers()
    {
        this.vertexBuffers.values().forEach(VertexBuffer::close);
    }
     */

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
