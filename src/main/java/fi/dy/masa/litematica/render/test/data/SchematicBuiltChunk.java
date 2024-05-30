package fi.dy.masa.litematica.render.test.data;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.ChunkStatus;
import fi.dy.masa.litematica.render.test.SchematicOverlayType;
import fi.dy.masa.litematica.render.test.buffer.SchematicBlockAllocatorStorage;
import fi.dy.masa.litematica.render.test.builder.SchematicChunkBuilder;
import fi.dy.masa.litematica.render.test.builder.SchematicRegionBuilder;
import fi.dy.masa.litematica.render.test.task.SchematicRebuildTask;
import fi.dy.masa.litematica.render.test.task.SchematicSorterTask;
import fi.dy.masa.litematica.render.test.task.SchematicTask;

public class SchematicBuiltChunk
{
    public final int index;
    public final AtomicReference<SchematicChunkRenderData> chunkData = new AtomicReference<>(SchematicChunkRenderData.EMPTY_1);
    private final AtomicInteger failures = new AtomicInteger(0);
    @Nullable
    private SchematicRebuildTask rebuildTask;
    @Nullable
    private SchematicSorterTask sorterTask;
    private SchematicChunkBuilder builder;
    private final Set<BlockEntity> blockEntities = Sets.newHashSet();
    private final Map<RenderLayer, VertexBuffer> layerVertexBuffers = RenderLayer.getBlockLayers().stream().collect(Collectors.toMap(layer -> layer, layer -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
    private final Map<SchematicOverlayType, VertexBuffer> overlayVertexBuffers = Arrays.stream(SchematicOverlayType.values()).toList().stream().collect(Collectors.toMap(type -> type, type -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
    private Box box;
    private boolean dirty = true;
    final BlockPos.Mutable chunkOrigin = new BlockPos.Mutable(-1, -1, -1);
    private final BlockPos.Mutable[] neighbors = Util.make(new BlockPos.Mutable[6], neighborPositions ->
    {
        for (int i = 0; i < ((BlockPos.Mutable[]) neighborPositions).length; ++i)
        {
            neighborPositions[i] = new BlockPos.Mutable();
        }
    });
    private boolean important;

    public SchematicBuiltChunk(SchematicChunkBuilder builder, int index, int x, int y, int z)
    {
        this.index = index;
        this.setChunkOrigin(x, y, z);
        this.builder = builder;
    }

    private boolean isChunkNonEmpty(BlockPos pos)
    {
        return this.builder.getSchematicWorld().getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.FULL, false) != null;
    }

    public boolean shouldBuild()
    {
        if (this.getSquaredCameraDistance() > 576.0)
        {
            return this.isChunkNonEmpty(this.neighbors[Direction.WEST.ordinal()]) && this.isChunkNonEmpty(this.neighbors[Direction.NORTH.ordinal()]) && this.isChunkNonEmpty(this.neighbors[Direction.EAST.ordinal()]) && this.isChunkNonEmpty(this.neighbors[Direction.SOUTH.ordinal()]);
        }

        return true;
    }

    public Box getBox()
    {
        return this.box;
    }

    public VertexBuffer getBufferByLayer(RenderLayer layer)
    {
        return this.layerVertexBuffers.get(layer);
    }

    public VertexBuffer getBufferByOverlay(SchematicOverlayType type)
    {
        return this.overlayVertexBuffers.get(type);
    }

    public void setChunkOrigin(int x, int y, int z)
    {
        this.clear();
        this.chunkOrigin.set(x, y, z);
    }

    private double getSquaredCameraDistance()
    {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

        double d = this.box.minX + 8.0 - camera.getPos().x;
        double e = this.box.minY + 8.0 - camera.getPos().y;
        double f = this.box.minZ + 8.0 - camera.getPos().z;

        return d * d + e * e + f * f;
    }

    public SchematicChunkRenderData getData()
    {
        return this.chunkData.get();
    }

    private void clear()
    {
        this.cancel();
        this.chunkData.set(SchematicChunkRenderData.EMPTY_1);
        this.dirty = true;
    }

    public void delete()
    {
        this.clear();
        this.layerVertexBuffers.values().forEach(VertexBuffer::close);
        this.overlayVertexBuffers.values().forEach(VertexBuffer::close);
    }

    public BlockPos getChunkOrigin() {return this.chunkOrigin;}

    public void scheduleRebuild(boolean important)
    {
        boolean bl = this.dirty;

        this.dirty = true;
        this.important = important | (bl && this.important);
    }

    public void markClean()
    {
        this.dirty = false;
        this.important = false;
    }

    public boolean isDirty()
    {
        return this.dirty;
    }

    public boolean isDirtyImportant()
    {
        return this.dirty && this.important;
    }

    public BlockPos getNeighborPos(Direction direction)
    {
        return this.neighbors[direction.ordinal()];
    }

    public boolean scheduleSortForLayer(RenderLayer layer, SchematicChunkBuilder renderer)
    {
        SchematicChunkRenderData chunkData = this.getData();

        if (this.sorterTask != null)
        {
            this.sorterTask.cancel();
        }
        if (!chunkData.usedLayers.contains(layer))
        {
            return false;
        }

        this.sorterTask = new SchematicSorterTask(this, this.getSquaredCameraDistance(), false, chunkData);
        renderer.sendTask(this.sorterTask);

        return true;
    }

    public boolean scheduleSortForOverlay(SchematicOverlayType type, SchematicChunkBuilder renderer)
    {
        SchematicChunkRenderData chunkData = this.getData();

        if (this.sorterTask != null)
        {
            this.sorterTask.cancel();
        }
        if (!chunkData.usedOverlays.contains(type))
        {
            return false;
        }

        this.sorterTask = new SchematicSorterTask(this, this.getSquaredCameraDistance(), false, chunkData);
        renderer.sendTask(this.sorterTask);

        return true;
    }

    protected boolean cancel()
    {
        boolean bl = false;

        if (this.rebuildTask != null)
        {
            this.rebuildTask.cancel();
            this.rebuildTask = null;
            bl = true;
        }
        if (this.sorterTask != null)
        {
            this.sorterTask.cancel();
            this.sorterTask = null;
        }

        return bl;
    }

    public SchematicTask scheduleRebuildTask(SchematicRegionBuilder builder)
    {
        boolean bl2;
        boolean bl = this.cancel();

        SchematicRendererRegion chunkRendererRegion = builder.build(this.builder.getSchematicWorld(), ChunkSectionPos.from(this.chunkOrigin));
        boolean bl3 = bl2 = this.chunkData.get() == SchematicChunkRenderData.EMPTY_1;

        if (bl2 && bl)
        {
            this.failures.incrementAndGet();
        }
        this.rebuildTask = new SchematicRebuildTask(this, this.getSquaredCameraDistance(), !bl2 || this.failures.get() > 2, chunkRendererRegion);

        return this.rebuildTask;
    }

    public void scheduleRebuild(SchematicChunkBuilder renderer, SchematicRegionBuilder builder)
    {
        SchematicTask task = this.scheduleRebuildTask(builder);
        renderer.sendTask(task);
    }

    public void setNoCullingBlockEntities(List<BlockEntity> blockEntities)
    {
        HashSet<BlockEntity> set2;
        HashSet<BlockEntity> set = Sets.newHashSet(blockEntities);
        Set<BlockEntity> set3 = this.blockEntities;

        synchronized (set3)
        {
            set2 = Sets.newHashSet(this.blockEntities);
            set.removeAll(this.blockEntities);
            blockEntities.forEach(set2::remove);
            this.blockEntities.clear();
            this.blockEntities.addAll(blockEntities);
        }

        this.builder.schematicRenderer.updateNoCullingBlockEntities(set2, set);
    }

    public void rebuildTask(SchematicRegionBuilder regionBuilder, SchematicBlockAllocatorStorage buffers)
    {
        SchematicTask task = this.scheduleRebuildTask(regionBuilder);

        task.execute(this.builder, buffers);
    }

    public boolean isAxisAlignedWith(int i, int j, int k)
    {
        BlockPos blockPos = this.getChunkOrigin();

        return i == ChunkSectionPos.getSectionCoord(blockPos.getX()) ||
               k == ChunkSectionPos.getSectionCoord(blockPos.getZ()) ||
               j == ChunkSectionPos.getSectionCoord(blockPos.getY());
    }

    public void setCompiled(SchematicChunkRenderData chunkData)
    {
        this.chunkData.set(chunkData);
        this.failures.set(0);
        //this.builder.schematicRenderer.addBuiltChunk(this);
    }

    public VertexSorter getSorter()
    {
        Vec3d cam = this.builder.getCameraVec3d();

        float x = (float) (cam.x - (double) this.chunkOrigin.getX());
        float y = (float) (cam.y - (double) this.chunkOrigin.getY());
        float z = (float) (cam.z - (double) this.chunkOrigin.getZ());

        return VertexSorter.byDistance(x, y, z);
    }
}
