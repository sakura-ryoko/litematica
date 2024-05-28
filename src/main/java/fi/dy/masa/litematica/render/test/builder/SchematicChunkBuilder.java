package fi.dy.masa.litematica.render.test.builder;

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
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.*;
import net.minecraft.util.thread.TaskExecutor;
import net.minecraft.world.chunk.ChunkStatus;
import fi.dy.masa.litematica.render.test.SchematicOverlayType;
import fi.dy.masa.litematica.render.test.SchematicWorldRenderer;
import fi.dy.masa.litematica.render.test.buffer.BlockBufferCache;
import fi.dy.masa.litematica.render.test.buffer.BlockBufferPool;
import fi.dy.masa.litematica.render.test.dispatch.SchematicBlockRenderManager;
import fi.dy.masa.litematica.render.test.dispatch.SchematicEntityRenderDispatch;
import fi.dy.masa.litematica.world.WorldSchematic;

@Environment(value = EnvType.CLIENT)
public class SchematicChunkBuilder
{
    private final PriorityBlockingQueue<SchematicChunkBuilder.SchematicBuiltChunk.SchematicTask> prioritizedTaskQueue = Queues.newPriorityBlockingQueue();
    private final Queue<SchematicChunkBuilder.SchematicBuiltChunk.SchematicTask> taskQueue = Queues.newLinkedBlockingDeque();
    private int processablePrioritizedTaskCount = 2;
    private volatile int queuedTaskCount;
    private final Queue<Runnable> uploadQueue = Queues.newConcurrentLinkedQueue();
    private final TaskExecutor<Runnable> tasks;
    private final Executor executor;
    final BlockBufferCache buffers;
    private final BlockBufferPool buffersPool;
    WorldSchematic schematicWorld;
    SchematicWorldRenderer schematicRenderer;
    private Vec3d cameraPos = Vec3d.ZERO;
    private volatile boolean stopped;
    final SchematicSectionBuilder sectionBuilder;

    public SchematicChunkBuilder(WorldSchematic world, SchematicWorldRenderer worldRenderer, Executor executor,
                                 SchematicBlockRenderManager blockRenderManager, SchematicEntityRenderDispatch schematicEntityRenderDispatch,
                                 BlockBufferCache buffers, BlockBufferPool buffersPool)
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
        if (this.stopped || this.buffersPool.isEmpty())
        {
            return;
        }

        SchematicBuiltChunk.SchematicTask task = this.pollTask();

        if (task == null)
        {
            return;
        }

        BlockBufferCache blockBufferBuilderStorage = Objects.requireNonNull(this.buffersPool.newCache());
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

                this.buffersPool.addCache(blockBufferBuilderStorage);
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

    public String getDebugString()
    {
        return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.queuedTaskCount, this.uploadQueue.size(), this.buffersPool.getCacheCount());
    }

    public int getToBatchCount()
    {
        return this.queuedTaskCount;
    }

    public int getChunksToUpload()
    {
        return this.uploadQueue.size();
    }

    public int getFreeBufferCount()
    {
        return this.buffersPool.getCacheCount();
    }

    public void setCameraVec3d(Vec3d pos)
    {
        this.cameraPos = pos;
    }

    public Vec3d getCameraVec3d()
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

    public void rebuildTask(SchematicChunkBuilder.SchematicBuiltChunk chunk, ChunkRendererRegionBuilder builder)
    {
        chunk.rebuildTask(builder);
    }

    public void reset()
    {
        this.clear();
    }

    public void sendTask(SchematicBuiltChunk.SchematicTask task)
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
        public final AtomicReference<SchematicChunkData> chunkData = new AtomicReference<>(SchematicChunkData.EMPTY_1);
        private final AtomicInteger failures = new AtomicInteger(0);
        @Nullable
        private SchematicRebuildTask rebuildTask;
        @Nullable
        private SchematicSorterTask sorterTask;
        private final Set<BlockEntity> blockEntities = Sets.newHashSet();
        private final Map<RenderLayer, VertexBuffer> layerVertexBuffers = RenderLayer.getBlockLayers().stream().collect(Collectors.toMap(layer -> layer, layer -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
        private final Map<SchematicOverlayType, VertexBuffer> overlayVertexBuffers = Arrays.stream(SchematicOverlayType.values()).toList().stream().collect(Collectors.toMap(type -> type, type -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
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

        private boolean isChunkNonEmpty(BlockPos pos)
        {
            return SchematicChunkBuilder.this.schematicWorld.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()), ChunkStatus.FULL, false) != null;
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

        private VertexBuffer getBufferByLayer(RenderLayer layer)
        {
            return this.layerVertexBuffers.get(layer);
        }

        private VertexBuffer getBufferByOverlay(SchematicOverlayType type)
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

        public SchematicChunkData getData()
        {
            return this.chunkData.get();
        }

        private void clear()
        {
            this.cancel();
            this.chunkData.set(SchematicChunkData.EMPTY_1);
            this.dirty = true;
        }

        public void delete()
        {
            this.clear();
            this.layerVertexBuffers.values().forEach(VertexBuffer::close);
            this.overlayVertexBuffers.values().forEach(VertexBuffer::close);
        }

        public BlockPos getChunkOrigin() { return this.chunkOrigin; }

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

        public boolean markDirtyImportant()
        {
            return this.dirty && this.important;
        }

        public BlockPos getNeighborPos(Direction direction)
        {
            return this.neighbors[direction.ordinal()];
        }

        public boolean scheduleSortForLayer(RenderLayer layer, SchematicChunkBuilder renderer)
        {
            SchematicChunkBuilder.SchematicChunkData chunkData = this.getData();

            if (this.sorterTask != null)
            {
                this.sorterTask.cancel();
            }
            if (!chunkData.usedLayers.contains(layer))
            {
                return false;
            }

            this.sorterTask = new SchematicSorterTask(chunkData, this.getSquaredCameraDistance());
            renderer.sendTask(this.sorterTask);

            return true;
        }

        public boolean scheduleSortForOverlay(SchematicOverlayType type, SchematicChunkBuilder renderer)
        {
            SchematicChunkBuilder.SchematicChunkData chunkData = this.getData();

            if (this.sorterTask != null)
            {
                this.sorterTask.cancel();
            }
            if (!chunkData.usedOverlays.contains(type))
            {
                return false;
            }

            this.sorterTask = new SchematicSorterTask(chunkData, this.getSquaredCameraDistance());
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

        public SchematicTask scheduleRebuildTask(ChunkRendererRegionBuilder builder)
        {
            boolean bl2;
            boolean bl = this.cancel();

            ChunkRendererRegion chunkRendererRegion = builder.build(SchematicChunkBuilder.this.schematicWorld, ChunkSectionPos.from(this.chunkOrigin));
            boolean bl3 = bl2 = this.chunkData.get() == SchematicChunkData.EMPTY_1;

            if (bl2 && bl)
            {
                this.failures.incrementAndGet();
            }
            this.rebuildTask = new SchematicRebuildTask(this.getSquaredCameraDistance(), !bl2 || this.failures.get() > 2, chunkRendererRegion);

            return this.rebuildTask;
        }

        public void scheduleRebuild(SchematicChunkBuilder renderer, ChunkRendererRegionBuilder builder)
        {
            SchematicTask task = this.scheduleRebuildTask(builder);
            renderer.sendTask(task);
        }

        private void setNoCullingBlockEntities(List<BlockEntity> blockEntities)
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

            SchematicChunkBuilder.this.schematicRenderer.updateNoCullingBlockEntities(set2, set);
        }

        public void rebuildTask(ChunkRendererRegionBuilder builder)
        {
            SchematicTask task = this.scheduleRebuildTask(builder);

            task.execute(SchematicChunkBuilder.this.buffers);
        }

        public boolean isAxisAlignedWith(int i, int j, int k)
        {
            BlockPos blockPos = this.getChunkOrigin();

            return i == ChunkSectionPos.getSectionCoord(blockPos.getX()) ||
                   k == ChunkSectionPos.getSectionCoord(blockPos.getZ()) ||
                   j == ChunkSectionPos.getSectionCoord(blockPos.getY());
        }

        void setCompiled(SchematicChunkBuilder.SchematicChunkData chunkData)
        {
            this.chunkData.set(chunkData);
            this.failures.set(0);
            SchematicChunkBuilder.this.schematicRenderer.addBuiltChunk(this);
        }

        private VertexSorter getSorter()
        {
            Vec3d cam = SchematicChunkBuilder.this.getCameraVec3d();

            float x = (float) (cam.x - (double) this.chunkOrigin.getX());
            float y = (float) (cam.y - (double) this.chunkOrigin.getY());
            float z = (float) (cam.z - (double) this.chunkOrigin.getZ());

            return VertexSorter.byDistance(x, y, z);
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

            public abstract CompletableFuture<ChunkResult> execute(BlockBufferCache buffer);

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
            public CompletableFuture<ChunkResult> execute(BlockBufferCache buffer)
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
                if (this.chunkData.isEmpty(RenderLayer.getTranslucent()) && this.chunkData.isEmpty(SchematicOverlayType.QUAD))
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
                    cachedBuffer = buffers.getBufferByOverlay(SchematicOverlayType.QUAD);
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
                            SchematicBuiltChunk.this.getBufferByOverlay(SchematicOverlayType.QUAD)).thenApply(v -> SchematicChunkBuilder.ChunkResult.CANCEL);
                }

                return ((CompletableFuture) completableFuture).handle((result, throwable) ->
                {
                    if (throwable != null && !(throwable instanceof CancellationException) && !(throwable instanceof InterruptedException))
                    {
                        MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create((Throwable) throwable, "Schematic Rendering section"));
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
            public CompletableFuture<ChunkResult> execute(BlockBufferCache buffer)
            {
                if (this.cancelled.get())
                {
                    return CompletableFuture.completedFuture(ChunkResult.CANCEL);
                }
                if (!SchematicChunkBuilder.SchematicBuiltChunk.this.shouldBuild())
                {
                    this.cancel();
                    return CompletableFuture.completedFuture(ChunkResult.CANCEL);
                }
                if (this.cancelled.get())
                {
                    return CompletableFuture.completedFuture(ChunkResult.CANCEL);
                }

                ChunkRendererRegion chunkRendererRegion = this.schematicRegion;
                this.schematicRegion = null;

                if (chunkRendererRegion == null)
                {
                    SchematicChunkBuilder.SchematicBuiltChunk.this.setCompiled(SchematicChunkBuilder.SchematicChunkData.EMPTY_2);
                    return CompletableFuture.completedFuture(ChunkResult.SUCCESS);
                }

                ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(SchematicBuiltChunk.this.chunkOrigin);
                SchematicSectionBuilder.SchematicRenderData renderData = SchematicChunkBuilder.this.sectionBuilder.build(chunkSectionPos, chunkRendererRegion, SchematicChunkBuilder.SchematicBuiltChunk.this.getSorter(), buffers);
                SchematicChunkBuilder.SchematicBuiltChunk.this.setNoCullingBlockEntities(renderData.noCullingBlockEntities);

                if (this.cancelled.get())
                {
                    renderData.close();
                    return CompletableFuture.completedFuture(ChunkResult.CANCEL);
                }
                SchematicChunkBuilder.SchematicChunkData chunkData = new SchematicChunkBuilder.SchematicChunkData();

                chunkData.occlusionData = renderData.occlusionData;
                chunkData.blockEntityList.addAll(renderData.blockEntityList);
                chunkData.sortingData = renderData.sortingData;

                ArrayList<CompletableFuture<Void>> list = new ArrayList<>(renderData.layerBuffers.size() + renderData.overlayBuffers.size());

                renderData.layerBuffers.forEach((renderLayer, layerBuffer) ->
                {
                    list.add(SchematicChunkBuilder.this.uploadMeshData(layerBuffer, SchematicChunkBuilder.SchematicBuiltChunk.this.getBufferByLayer(renderLayer)));
                    chunkData.usedLayers.add(renderLayer);
                });
                renderData.overlayBuffers.forEach((renderOverlay, layerBuffer) ->
                {
                    list.add(SchematicChunkBuilder.this.uploadMeshData(layerBuffer, SchematicChunkBuilder.SchematicBuiltChunk.this.getBufferByOverlay(renderOverlay)));
                    chunkData.usedOverlays.add(renderOverlay);
                });

                return Util.combine(list).handle((results, throwable) ->
                {
                    if (throwable != null && !(throwable instanceof CancellationException) && !(throwable instanceof InterruptedException))
                    {
                        MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create((Throwable) throwable, "Schematic Rendering section"));
                    }
                    if (this.cancelled.get())
                    {
                        return ChunkResult.CANCEL;
                    }
                    SchematicChunkBuilder.SchematicBuiltChunk.this.setCompiled(chunkData);
                    return ChunkResult.SUCCESS;
                });
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
        public static final SchematicChunkData EMPTY_1 = new SchematicChunkData()
        {
            @Override
            public boolean isVisibleThrough(Direction from, Direction to)
            {
                return false;
            }
        };
        public static final SchematicChunkData EMPTY_2 = new SchematicChunkData()
        {
            @Override
            public boolean isVisibleThrough(Direction from, Direction to)
            {
                return true;
            }
        };
        final Set<RenderLayer> usedLayers = new ObjectArraySet<>(RenderLayer.getBlockLayers().size());
        final Set<SchematicOverlayType> usedOverlays = new ObjectArraySet<>(SchematicOverlayType.values().length);
        final List<BlockEntity> blockEntityList = Lists.newArrayList();
        ChunkOcclusionData occlusionData = new ChunkOcclusionData();
        @Nullable
        BuiltBuffer.SortState sortingData;

        public boolean isEmpty() { return this.usedLayers.isEmpty() && this.usedOverlays.isEmpty(); }

        public boolean isEmpty(RenderLayer layer) { return !this.usedLayers.contains(layer); }

        public boolean isEmpty(SchematicOverlayType type) { return !this.usedOverlays.contains(type); }

        public List<BlockEntity> getBlockEntityList() { return this.blockEntityList; }

        public boolean isVisibleThrough(Direction from, Direction to)
        {
            return this.occlusionData.isVisibleThrough(from, to);
        }
    }
}
