package fi.dy.masa.litematica.render.schematic.test;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.*;
import net.minecraft.util.thread.TaskExecutor;
import net.minecraft.world.chunk.ChunkStatus;
import fi.dy.masa.litematica.render.cache.SectionBufferCache;
import fi.dy.masa.litematica.render.schematic.org.ChunkRendererSchematicVbo;
import fi.dy.masa.litematica.world.WorldSchematic;

@Environment(value = EnvType.CLIENT)
public class SchematicChunkBuilder
{
    final SectionBufferCache buffers;
    private final BlockBufferBuilderPool buffersPool;
    WorldSchematic schematicWorld;
    SchematicWorldRenderer schematicRenderer;
    private Vec3d cameraPos = Vec3d.ZERO;
    private volatile int queuedTaskCount;
    private volatile boolean stopped;
    private final Queue<Runnable> uploadQueue = Queues.newConcurrentLinkedQueue();
    private final TaskExecutor<Runnable> tasks;
    private final Executor executor;
    final SchematicSectionBuilder sectionBuilder;
    private final PriorityBlockingQueue<SchematicChunkBuilder.SchematicBuiltChunk.SchematicTask> prioritizedTaskQueue = Queues.newPriorityBlockingQueue();
    private final Queue<SchematicChunkBuilder.SchematicBuiltChunk.SchematicTask> taskQueue = Queues.newLinkedBlockingDeque();
    private int processablePrioritizedTaskCount = 2;

    public SchematicChunkBuilder(WorldSchematic world, SchematicWorldRenderer worldRenderer, Executor executor,
                                 SchematicBlockRenderManager blockRenderManager, SchematicEntityRenderDispatch schematicEntityRenderDispatch,
                                 SectionBufferCache buffers, BlockBufferBuilderPool buffersPool)
    {
        this.schematicWorld = world;
        this.schematicRenderer = worldRenderer;
        this.buffers = buffers;
        this.buffersPool = buffersPool;
        this.executor = executor;
        this.tasks = TaskExecutor.create(executor, "Schematic Section Renderer");
        this.tasks.send(this::scheduleTasks);
        this.sectionBuilder = new SchematicSectionBuilder(blockRenderManager, schematicEntityRenderDispatch);
    }

    public void setSchematicWorld(WorldSchematic newWorld)
    {
        this.schematicWorld = newWorld;
    }

    private void scheduleTasks()
    {
        if (this.stopped || this.buffersPool.hasNoAvailableBuilder())
        {
            return;
        }

        SchematicBuiltChunk.SchematicTask task = this.pollTask();

        if (task == null)
        {
            return;
        }

        BlockBufferBuilderStorage blockBufferBuilderStorage = Objects.requireNonNull(this.buffersPool.acquire());
        this.queuedTaskCount = this.prioritizedTaskQueue.size() + this.taskQueue.size();

        ((CompletableFuture) CompletableFuture.supplyAsync(Util.debugSupplier(task.getName(), () -> task.execute(blockBufferBuilderStorage)), this.executor).thenCompose(future -> future)).whenComplete((result, throwable) ->
        {
            if (throwable != null)
            {
                MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create((Throwable) throwable, "Schematic Batching sections"));
                return;
            }
            this.tasks.send(() ->
            {
                if (result == ChunkResult.SUCCESS)
                {
                    blockBufferBuilderStorage.clear();
                }
                else
                {
                    blockBufferBuilderStorage.reset();
                }

                this.buffersPool.release(blockBufferBuilderStorage);
                this.scheduleTasks();
            });
        });
    }

    @Nullable
    private SchematicBuiltChunk.SchematicTask pollTask()
    {
        SchematicBuiltChunk.SchematicTask newTask;

        if (this.processablePrioritizedTaskCount <= 0 && (newTask = this.taskQueue.poll()) != null)
        {
            this.processablePrioritizedTaskCount = 2;
            return newTask;
        }

        newTask = this.prioritizedTaskQueue.poll();

        if (newTask != null)
        {
            --this.processablePrioritizedTaskCount;
            return newTask;
        }

        this.processablePrioritizedTaskCount = 2;
        return this.taskQueue.poll();
    }

    public int getToBatchCount() {
        return this.queuedTaskCount;
    }

    public int getChunksToUpload() {
        return this.uploadQueue.size();
    }

    public int getFreeBufferCount() {
        return this.buffersPool.getAvailableBuilderCount();
    }

    public String getDebugString()
    {
        return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.queuedTaskCount, this.uploadQueue.size(), this.buffersPool.getAvailableBuilderCount());
    }

    private void setCameraVec3d(Vec3d pos)
    {
        this.cameraPos = pos;
    }

    private Vec3d getCameraVec3d()
    {
        return this.cameraPos;
    }

    public void uploadTask()
    {
        Runnable runnable;

        while ((runnable = this.uploadQueue.poll()) != null)
        {
            runnable.run();
        }
    }

    public void rebuildTask(SchematicChunkBuilder.SchematicBuiltChunk chunk, ChunkRendererRegionBuilder builder) {
        chunk.rebuild(builder);
    }

    public void reset() {
        this.clear();
    }

    public void sendTasks(SchematicBuiltChunk.SchematicTask task)
    {
        if (this.stopped)
        {
            return;
        }

        this.tasks.send(() ->
        {
            if (this.stopped)
            {
                return;
            }
            if (task.priorityTask)
            {
                this.prioritizedTaskQueue.offer(task);
            }
            else
            {
                this.taskQueue.offer(task);
            }
            this.queuedTaskCount = this.prioritizedTaskQueue.size() + this.taskQueue.size();
            this.scheduleTasks();
        });
    }

    public CompletableFuture<Void> uploadMeshData(BuiltBuffer meshData, VertexBuffer vertexBuffer)
    {
        if (this.stopped)
        {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() ->
        {
            if (vertexBuffer.isClosed())
            {
                meshData.close();
                return;
            }
            vertexBuffer.bind();
            vertexBuffer.upload(meshData);
            VertexBuffer.unbind();
        }, this.uploadQueue::add);
    }

    private CompletableFuture<Void> uploadSortingData(BufferAllocator.CloseableBuffer resultBuffer, VertexBuffer vertexBuffer)
    {
        if (this.stopped)
        {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() ->
        {
            if (vertexBuffer.isClosed()) {
                resultBuffer.close();
                return;
            }
            vertexBuffer.bind();
            vertexBuffer.uploadIndexBuffer(resultBuffer);
            VertexBuffer.unbind();
        }, this.uploadQueue::add);
    }

    private void clear()
    {
        SchematicBuiltChunk.SchematicTask task;

        while (!this.prioritizedTaskQueue.isEmpty())
        {
            task = this.prioritizedTaskQueue.poll();
            if (task == null) continue;
            task.cancel();
        }

        while (!this.taskQueue.isEmpty())
        {
            task = this.taskQueue.poll();
            if (task == null) continue;
            task.cancel();
        }

        this.queuedTaskCount = 0;
    }

    public boolean isEmpty()
    {
        return this.queuedTaskCount == 0 && this.uploadQueue.isEmpty();
    }

    public void stop()
    {
        this.stopped = true;
        this.clear();
        this.uploadTask();
    }

    public class SchematicBuiltChunk
    {
        public final int index;
        public final AtomicReference<SchematicChunkData> chunkData = new AtomicReference<>(SchematicChunkData.EMPTY);
        private final AtomicInteger failures = new AtomicInteger(0);
        @Nullable
        private SchematicRebuildTask rebuildTask;
        @Nullable
        private SchematicSorterTask sorterTask;
        private final Set<BlockEntity> blockEntities = Sets.newHashSet();
        private final Map<RenderLayer, VertexBuffer> layerVertexBuffers = RenderLayer.getBlockLayers().stream().collect(Collectors.toMap(layer -> layer, layer -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
        private final Map<ChunkRendererSchematicVbo.OverlayRenderType, VertexBuffer> overlayVertexBuffers = Arrays.stream(ChunkRendererSchematicVbo.OverlayRenderType.values()).toList().stream().collect(Collectors.toMap(type -> type, type -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
        private Box box;
        private boolean dirty = true;
        final BlockPos.Mutable chunkOrigin = new BlockPos.Mutable(-1, -1, -1);
        private final BlockPos.Mutable[] neighbors = Util.make(new BlockPos.Mutable[6], neighborPositions -> {
            for (int i = 0; i < ((BlockPos.Mutable[])neighborPositions).length; ++i) {
                neighborPositions[i] = new BlockPos.Mutable();
            }
        });
        private boolean important;

        public SchematicBuiltChunk(int index, int x, int y, int z)
        {
            this.index = index;
            this.setChunkOrigin(x, y, z);
        }

        public void setChunkOrigin(int x, int y, int z)
        {
            this.close();
            this.chunkOrigin.set(x, y, z);
        }

        public BlockPos getChunkOrigin() { return this.chunkOrigin; }

        private boolean shouldBuild()
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

        private boolean isChunkNonEmpty(BlockPos pos)
        {
            return SchematicChunkBuilder.this.schematicWorld.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.FULL, false) != null;
        }

        private double getSquaredCameraDistance()
        {
            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

            double d = this.box.minX + 8.0 - camera.getPos().x;
            double e = this.box.minY + 8.0 - camera.getPos().y;
            double f = this.box.minZ + 8.0 - camera.getPos().z;

            return d * d + e * e + f * f;
        }

        public SchematicChunkData getData()
        {
            return this.chunkData.get();
        }

        private VertexSorter getSorter()
        {
            Vec3d cam = SchematicChunkBuilder.this.getCameraVec3d();

            float x = (float) (cam.x - (double) this.chunkOrigin.getX());
            float y = (float) (cam.y - (double) this.chunkOrigin.getY());
            float z = (float) (cam.z - (double) this.chunkOrigin.getZ());

            return VertexSorter.byDistance(x, y, z);
        }

        private VertexBuffer getBufferByLayer(RenderLayer layer)
        {
            return this.layerVertexBuffers.get(layer);
        }

        private VertexBuffer getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
        {
            return this.overlayVertexBuffers.get(type);
        }

        private void close()
        {
            this.cancel();
            this.chunkData.set(SchematicChunkData.EMPTY);
            this.dirty = true;
        }

        public void delete() {
            this.close();
            this.layerVertexBuffers.values().forEach(VertexBuffer::close);
            this.overlayVertexBuffers.values().forEach(VertexBuffer::close);
        }

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

        public boolean markDirty()
        {
            return this.dirty;
        }

        public boolean needsImportantRebuild()
        {
            return this.dirty && this.important;
        }

        public BlockPos getNeighborPos(Direction direction)
        {
            return this.neighbors[direction.ordinal()];
        }

        public void rebuild(ChunkRendererRegionBuilder builder)
        {
            // NO-OP
        }

        private void cancel()
        {
            boolean bl = false;
            if (this.rebuildTask != null) {
                this.rebuildTask.cancel();
                this.rebuildTask = null;
                bl = true;
            }
            if (this.sortTask != null) {
                this.sortTask.cancel();
                this.sortTask = null;
            }
            return bl;
        }

        abstract class SchematicTask implements Comparable<SchematicTask>
        {
            protected final double dist;
            protected final AtomicBoolean cancelled = new AtomicBoolean(false);
            protected final boolean priorityTask;

            public SchematicTask(SchematicBuiltChunk builtChunk, double dist, boolean priority)
            {
                this.dist = dist;
                this.priorityTask = priority;
            }

            public abstract CompletableFuture<ChunkResult> execute(BlockBufferBuilderStorage buffer);

            public abstract void cancel();

            protected abstract String getName();

            @Override
            public int compareTo(SchematicTask task)
            {
                return Doubles.compare(this.dist, task.dist);
            }
        }

        class SchematicSorterTask extends SchematicTask
        {
            private final SchematicChunkData chunkData;

            SchematicSorterTask(SchematicChunkData chunkData, double dist)
            {
                super(SchematicBuiltChunk.this, dist, true);
                this.chunkData = chunkData;
            }

            @Override
            protected String getName()
            {
                return "rend_schem_chk_sort";
            }

            @Override
            public CompletableFuture<ChunkResult> execute(BlockBufferBuilderStorage buffer)
            {
                if (this.cancelled.get())
                {
                    return CompletableFuture.completedFuture(SchematicChunkBuilder.ChunkResult.CANCEL);
                }
                if (!SchematicBuiltChunk.this.shouldBuild())
                {
                    this.cancelled.set(true);

                    return CompletableFuture.completedFuture(SchematicChunkBuilder.ChunkResult.CANCEL);
                }
                if (this.cancelled.get())
                {
                    return CompletableFuture.completedFuture(SchematicChunkBuilder.ChunkResult.CANCEL);
                }

                BuiltBuffer.SortState sortState = this.chunkData.sortingData;

                if (sortState == null)
                {
                    return CompletableFuture.completedFuture(SchematicChunkBuilder.ChunkResult.CANCEL);
                }
                if (this.chunkData.isEmpty(RenderLayer.getTranslucent()) && this.chunkData.isEmpty(ChunkRendererSchematicVbo.OverlayRenderType.QUAD))
                {
                    return CompletableFuture.completedFuture(SchematicChunkBuilder.ChunkResult.CANCEL);
                }

                BufferAllocator cachedBuffer;
                VertexSorter vertexSorter = SchematicBuiltChunk.this.getSorter();
                CompletionStage completableFuture;

                if (!this.chunkData.isEmpty(RenderLayer.getTranslucent()))
                {
                    cachedBuffer = buffers.getBufferByLayer(RenderLayer.getTranslucent());
                    BufferAllocator.CloseableBuffer closeableBuffer = sortState.sortAndStore(cachedBuffer, vertexSorter);

                    if (closeableBuffer == null)
                    {
                        return CompletableFuture.completedFuture(SchematicChunkBuilder.ChunkResult.CANCEL);
                    }
                    if (this.cancelled.get())
                    {
                        closeableBuffer.close();
                        return CompletableFuture.completedFuture(SchematicChunkBuilder.ChunkResult.CANCEL);
                    }

                    completableFuture = SchematicChunkBuilder.this.uploadSortingData(closeableBuffer,
                            SchematicBuiltChunk.this.getBufferByLayer(RenderLayer.getTranslucent())).thenApply(v -> SchematicChunkBuilder.ChunkResult.CANCEL);
                }
                else
                {
                    cachedBuffer = buffers.getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType.QUAD);
                    BufferAllocator.CloseableBuffer closeableBuffer = sortState.sortAndStore(cachedBuffer, vertexSorter);

                    if (closeableBuffer == null)
                    {
                        return CompletableFuture.completedFuture(SchematicChunkBuilder.ChunkResult.CANCEL);
                    }
                    if (this.cancelled.get())
                    {
                        closeableBuffer.close();
                        return CompletableFuture.completedFuture(SchematicChunkBuilder.ChunkResult.CANCEL);
                    }

                    completableFuture = SchematicChunkBuilder.this.uploadSortingData(closeableBuffer,
                            SchematicBuiltChunk.this.getBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType.QUAD)).thenApply(v -> SchematicChunkBuilder.ChunkResult.CANCEL);
                }

                return ((CompletableFuture) completableFuture).handle((result, throwable) ->
                {
                    if (throwable != null && !(throwable instanceof CancellationException) && !(throwable instanceof InterruptedException))
                    {
                        MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create((Throwable) throwable, "Rendering section"));
                    }
                    return this.cancelled.get() ? ChunkResult.CANCEL : SchematicChunkBuilder.ChunkResult.SUCCESS;
                });
            }

            @Override
            public void cancel()
            {
                this.cancelled.set(true);
            }
        }

        class SchematicRebuildTask extends SchematicTask
        {
            @Nullable
            protected ChunkRendererRegion schematicRegion;

            public SchematicRebuildTask(double dist, boolean priority, ChunkRendererRegion region)
            {
                super(SchematicBuiltChunk.this, dist, priority);
                this.schematicRegion = region;
            }

            @Override
            protected String getName()
            {
                return "rend_schem_chk_rebuild";
            }

            @Override
            public CompletableFuture<ChunkResult> execute(BlockBufferBuilderStorage buffer)
            {
                return CompletableFuture.completedFuture(ChunkResult.SUCCESS);
            }

            @Override
            public void cancel()
            {
                this.schematicRegion = null;
                if (this.cancelled.compareAndSet(false, true))
                {
                    SchematicBuiltChunk.this.scheduleRebuild(false);
                }
            }
        }
    }

    enum ChunkResult
    {
        SUCCESS,
        CANCEL;
    }

    public static class SchematicChunkData
    {
        public static final SchematicChunkData EMPTY = new SchematicChunkData()
        {
            @Override
            public boolean isVisibleThrough(Direction from, Direction to)
            {
                return false;
            }
        };
        public static final SchematicChunkData EMPTY2 = new SchematicChunkData()
        {
            @Override
            public boolean isVisibleThrough(Direction from, Direction to)
            {
                return true;
            }
        };
        final Set<RenderLayer> usedLayers = new ObjectArraySet<>(RenderLayer.getBlockLayers().size());
        final Set<ChunkRendererSchematicVbo.OverlayRenderType> usedOverlays = new ObjectArraySet<>(ChunkRendererSchematicVbo.OverlayRenderType.values().length);
        final List<BlockEntity> blockEntityList = Lists.newArrayList();
        ChunkOcclusionData occlusionData = new ChunkOcclusionData();
        @Nullable
        BuiltBuffer.SortState sortingData;

        public boolean isEmpty() { return this.usedLayers.isEmpty() && this.usedOverlays.isEmpty(); }

        public boolean isEmpty(RenderLayer layer) { return !this.usedLayers.contains(layer); }

        public boolean isEmpty(ChunkRendererSchematicVbo.OverlayRenderType type) { return !this.usedOverlays.contains(type); }

        public List<BlockEntity> getBlockEntityList() { return this.blockEntityList; }

        public boolean isVisibleThrough(Direction from, Direction to)
        {
            return this.occlusionData.isVisibleThrough(from, to);
        }
    }
}
