package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;

import com.google.common.collect.Queues;
import com.google.common.primitives.Doubles;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.Logger;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;

public class ChunkRenderDispatcherLitematica
{
    private static final Logger LOGGER = Litematica.logger;
    // Threaded Code
    //private static final ThreadFactory THREAD_FACTORY = (new ThreadFactoryBuilder()).setNameFormat("Litematica Chunk Batcher %d").setDaemon(true).build();

    private final List<Thread> listWorkerThreads = new ArrayList<>();
    private final List<ChunkRenderWorkerLitematica> listThreadedWorkers = new ArrayList<>();
    private final PriorityBlockingQueue<ChunkRenderTaskSchematic> queueChunkUpdates = Queues.newPriorityBlockingQueue();
    private final BlockingQueue<BufferAllocatorCache> queueFreeRenderAllocators;
    private final Queue<ChunkRenderDispatcherLitematica.PendingUpload> queueChunkUploads = Queues.newPriorityQueue();
    private final ChunkRenderWorkerLitematica renderWorker;
    private final int countRenderAllocators;
    // Threaded Code
    //private final int countRenderThreads;
    private Vec3d cameraPos;

    public ChunkRenderDispatcherLitematica()
    {
        /* Threaded Code

        int threadLimitMemory = Math.max(1, (int) ((double) Runtime.getRuntime().maxMemory() * 0.3D) / BufferAllocatorCache.EXPECTED_TOTAL_SIZE);
        int threadLimitCPU = Math.max(1, MathHelper.clamp(Runtime.getRuntime().availableProcessors(), 1, threadLimitMemory / 5));
        int maxThreads = Math.max(1, Math.min((Configs.Visuals.RENDER_SCHEMATIC_MAX_THREADS.getIntegerValue()), threadLimitCPU));
        int maxCache = Math.max(1, Math.min((Configs.Visuals.RENDER_SCHEMATIC_MAX_THREADS.getIntegerValue() - 1), threadLimitMemory));
        this.cameraPos = Vec3d.ZERO;

        if (maxThreads > 1)
        {
            LOGGER.info("Creating {} rendering threads", maxThreads);

            for (int i = 0; i < maxThreads; ++i)
            {
                ChunkRenderWorkerLitematica worker = new ChunkRenderWorkerLitematica(this);
                Thread thread = THREAD_FACTORY.newThread(worker);
                thread.start();
                this.listThreadedWorkers.add(worker);
                this.listWorkerThreads.add(thread);
            }
        }

        this.countRenderThreads = maxThreads;
        this.queueFreeRenderAllocators = Queues.newArrayBlockingQueue(maxCache);

        for (int i = 0; i < maxCache; ++i)
        {
            try
            {
                this.queueFreeRenderAllocators.add(new BufferAllocatorCache());
            }
            catch (OutOfMemoryError e)
            {
                LOGGER.warn("Only able to allocate {}/{} BufferAllocator caches", this.queueFreeRenderAllocators.size(), i);
                int adjusted = Math.min(this.queueFreeRenderAllocators.size() * 2 / 3, this.queueFreeRenderAllocators.size() - 1);

                for (int j = 0; j < adjusted; ++j)
                {
                    try
                    {
                        BufferAllocatorCache r = this.queueFreeRenderAllocators.take();
                        r.close();
                    }
                    catch (Exception ignored) { }

                    maxCache = adjusted;
                }
            }
        }

        this.countRenderAllocators = maxCache;
        LOGGER.info("Using {} max total BufferAllocator caches", this.countRenderAllocators + 1);

        this.renderWorker = new ChunkRenderWorkerLitematica(this, new BufferAllocatorCache());
         */

        this.countRenderAllocators = 2;
        this.cameraPos = Vec3d.ZERO;

        LOGGER.info("Using {} total BufferAllocator caches", this.countRenderAllocators + 1);

        this.queueFreeRenderAllocators = Queues.newArrayBlockingQueue(this.countRenderAllocators);

        for (int i = 0; i < this.countRenderAllocators; ++i)
        {
            this.queueFreeRenderAllocators.add(new BufferAllocatorCache());
        }

        this.renderWorker = new ChunkRenderWorkerLitematica(this, new BufferAllocatorCache());
    }

    protected void setCameraPosition(Vec3d cameraPos)
    {
        this.cameraPos = cameraPos;
    }

    public Vec3d getCameraPos()
    {
        return this.cameraPos;
    }

    protected String getDebugInfo()
    {
        // Threaded Code
        //return String.format("T: %02d, pC: %03d, pU: %03d, aB: %02d", this.listThreadedWorkers.size(), this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderAllocators.size());

        return this.listWorkerThreads.isEmpty() ? String.format("pC: %03d, single-threaded", this.queueChunkUpdates.size()) : String.format("pC: %03d, pU: %1d, aB: %1d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderAllocators.size());
    }

    protected boolean runChunkUploads(long finishTimeNano)
    {
        boolean ranTasks = false;

        while (true)
        {
            boolean processedTask = false;

            if (this.listWorkerThreads.isEmpty())
            {
                ChunkRenderTaskSchematic generator = this.queueChunkUpdates.poll();

                if (generator != null)
                {
                    try
                    {
                        this.renderWorker.processTask(generator);
                        processedTask = true;
                    }
                    catch (InterruptedException e)
                    {
                        LOGGER.warn("runChunkUploads(): Process Interrupted; error message: [{}]", e.getLocalizedMessage());
                    }
                }
            }

            synchronized (this.queueChunkUploads)
            {
                if (!this.queueChunkUploads.isEmpty())
                {
                    (this.queueChunkUploads.poll()).uploadTask.run();
                    processedTask = true;
                    ranTasks = true;
                }
            }

            if (finishTimeNano == 0L || processedTask == false || finishTimeNano < System.nanoTime())
            {
                break;
            }
        }

        return ranTasks;
    }

    protected boolean updateChunkLater(ChunkRendererSchematicVbo renderChunk)
    {
        /* Threaded Code
        final ChunkRenderTaskSchematic generator = renderChunk.makeCompileTaskChunkSchematic(this::getCameraPos);
        generator.addFinishRunnable(() -> queueChunkUpdates.remove(generator));
        boolean flag = queueChunkUpdates.offer(generator);

        if (!flag)
        {
            generator.finish();
        }

        return flag;
         */

        renderChunk.getLockCompileTask().lock();
        boolean flag1;

        try
        {
            final ChunkRenderTaskSchematic generator = renderChunk.makeCompileTaskChunkSchematic(this::getCameraPos);

            generator.addFinishRunnable(new Runnable()
            {
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator);
                }
            });

            boolean flag = this.queueChunkUpdates.offer(generator);

            if (!flag)
            {
                generator.finish();
            }

            flag1 = flag;
        }
        finally
        {
            renderChunk.getLockCompileTask().unlock();
        }

        return flag1;
    }

    protected boolean updateChunkNow(ChunkRendererSchematicVbo chunkRenderer)
    {
        /* Threaded Code
        try
        {
            renderWorker.processTask(chunkRenderer.makeCompileTaskChunkSchematic(this::getCameraPos));
            return true;
        }
        catch (InterruptedException e)
        {
            LOGGER.warn("updateChunkNow(): Process Interrupted; error message: [{}]", e.getLocalizedMessage());
            return false;
        }
         */

        chunkRenderer.getLockCompileTask().lock();
        boolean flag;

        try
        {
            ChunkRenderTaskSchematic generator = chunkRenderer.makeCompileTaskChunkSchematic(this::getCameraPos);

            try
            {
                this.renderWorker.processTask(generator);
            }
            catch (InterruptedException e)
            {
            }

            flag = true;
        }
        finally
        {
            chunkRenderer.getLockCompileTask().unlock();
        }

        return flag;
    }

    protected void stopChunkUpdates()
    {
        this.clearChunkUpdates();
        List<BufferAllocatorCache> list = new ArrayList<>();

        while (list.size() != this.countRenderAllocators)
        {
            this.runChunkUploads(Long.MAX_VALUE);

            try
            {
                list.add(this.allocateRenderAllocators());
            }
            catch (InterruptedException e)
            {
                LOGGER.warn("stopChunkUpdates(): Process Interrupted; error message: [{}]", e.getLocalizedMessage());
            }
        }

        this.queueFreeRenderAllocators.addAll(list);
    }

    public void freeRenderAllocators(BufferAllocatorCache allocatorCache)
    {
        this.queueFreeRenderAllocators.add(allocatorCache);
    }

    public BufferAllocatorCache allocateRenderAllocators() throws InterruptedException
    {
        return this.queueFreeRenderAllocators.take();
    }

    protected ChunkRenderTaskSchematic getNextChunkUpdate() throws InterruptedException
    {
        return this.queueChunkUpdates.take();
    }

    protected boolean updateTransparencyLater(ChunkRendererSchematicVbo renderChunk)
    {
        /* Threaded Code
        final ChunkRenderTaskSchematic generator = renderChunk.makeCompileTaskTransparencySchematic(this::getCameraPos);

        if (generator == null)
        {
            return true;
        }
        generator.addFinishRunnable(() -> ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator));
        return queueChunkUpdates.offer(generator);
         */

        renderChunk.getLockCompileTask().lock();
        boolean flag;

        try
        {
            final ChunkRenderTaskSchematic generator = renderChunk.makeCompileTaskTransparencySchematic(this::getCameraPos);

            if (generator == null)
            {
                flag = true;
                return flag;
            }

            generator.addFinishRunnable(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator);
                }
            });

            flag = this.queueChunkUpdates.offer(generator);
        }
        finally
        {
            renderChunk.getLockCompileTask().unlock();
        }

        return flag;
    }

    protected ListenableFuture<Object> uploadChunkBlocks(final RenderLayer layer, final BufferAllocatorCache allocators, final ChunkRendererSchematicVbo renderChunk, final ChunkRenderDataSchematic chunkRenderData, final double distanceSq, boolean resortOnly)
    {
        if (MinecraftClient.getInstance().isOnThread())
        {
            try
            {
                this.uploadVertexBufferByLayer(layer, allocators, renderChunk, chunkRenderData, renderChunk.createVertexSorter(this.getCameraPos(), renderChunk.getOrigin()), resortOnly);
            }
            catch (Exception e)
            {
                LOGGER.warn("uploadChunkBlocks(): [Dispatch] Error uploading Vertex Buffer for layer [{}], Caught error: [{}]", ChunkRenderLayers.getFriendlyName(layer), e.toString());
            }

            return Futures.immediateFuture(null);
        }
        else
        {
            /*  Threaded Code

            ListenableFutureTask<Object> futureTask = ListenableFutureTask.create(
                    () -> uploadChunkBlocks(layer, allocators, renderChunk, chunkRenderData, distanceSq, resortOnly),
                    null);
             */

            ListenableFutureTask<Object> futureTask = ListenableFutureTask.<Object>create(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.uploadChunkBlocks(layer, allocators, renderChunk, chunkRenderData, distanceSq, resortOnly);
                }
            }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new ChunkRenderDispatcherLitematica.PendingUpload(futureTask, distanceSq));
                return futureTask;
            }
        }
    }

    protected ListenableFuture<Object> uploadChunkOverlay(final OverlayRenderType type, final BufferAllocatorCache allocators, final ChunkRendererSchematicVbo renderChunk, final ChunkRenderDataSchematic compiledChunk, final double distanceSq, boolean resortOnly)
    {
        if (MinecraftClient.getInstance().isOnThread())
        {
            try
            {
                this.uploadVertexBufferByType(type, allocators, renderChunk, compiledChunk, renderChunk.createVertexSorter(this.getCameraPos(), renderChunk.getOrigin()), resortOnly);
            }
            catch (Exception e)
            {
                // TODO --> This one will throw if it's not sorted as Translucent,
                //  but it will cause a crash during draw() --> Ignored
                LOGGER.warn("uploadChunkOverlay(): [Dispatch] Error uploading Vertex Buffer for overlay type [{}], Caught error: [{}]", type.getDrawMode().name(), e.toString());
            }
            return Futures.immediateFuture(null);
        }
        else
        {
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.<Object>create(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.uploadChunkOverlay(type, allocators, renderChunk, compiledChunk, distanceSq, resortOnly);
                }
            }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new ChunkRenderDispatcherLitematica.PendingUpload(futureTask, distanceSq));
                return futureTask;
            }
        }
    }

    private void uploadVertexBufferByLayer(RenderLayer layer, @Nonnull BufferAllocatorCache allocators, @Nonnull ChunkRendererSchematicVbo renderChunk, @Nonnull ChunkRenderDataSchematic compiledChunk, @Nonnull VertexSorter sorter, boolean resortOnly)
            throws InterruptedException
    {
        BufferAllocator allocator = allocators.getBufferByLayer(layer);
        BuiltBuffer renderBuffer = compiledChunk.getBuiltBufferCache().getBuiltBufferByLayer(layer);

        if (allocator == null)
        {
            allocators.closeByLayer(layer);
            compiledChunk.setBlockLayerUnused(layer);
            throw new InterruptedException("BufferAllocators are invalid");
        }

        if (renderBuffer == null)
        {
            compiledChunk.setBlockLayerUnused(layer);
            return;
        }

        VertexBuffer vertexBuffer = renderChunk.getBlocksVertexBufferByLayer(layer);

        if (layer == RenderLayer.getTranslucent())
        {
            BuiltBuffer.SortState sorting = compiledChunk.getTransparentSortingData();

            if (sorting == null)
            {
                sorting = renderBuffer.sortQuads(allocator, sorter);

                if (sorting == null)
                {
                    throw new InterruptedException("Sort State failed to sortQuads()");
                }

                compiledChunk.setTransparentSortingData(sorting);
            }

            BufferAllocator.CloseableBuffer result = sorting.sortAndStore(allocator, sorter);

            if (result != null)
            {
                renderChunk.uploadSortingState(result, vertexBuffer);
                result.close();
            }
        }

        if (resortOnly == false)
        {
            renderChunk.uploadBuiltBuffer(renderBuffer, vertexBuffer);
        }
    }

    private void uploadVertexBufferByType(OverlayRenderType type, @Nonnull BufferAllocatorCache allocators, @Nonnull ChunkRendererSchematicVbo renderChunk, @Nonnull ChunkRenderDataSchematic compiledChunk, @Nonnull VertexSorter sorter, boolean resortOnly)
            throws InterruptedException
    {
        BufferAllocator allocator = allocators.getBufferByOverlay(type);
        BuiltBuffer renderBuffer = compiledChunk.getBuiltBufferCache().getBuiltBufferByType(type);

        if (allocator == null)
        {
            allocators.closeByType(type);
            compiledChunk.setOverlayTypeUnused(type);
            throw new InterruptedException("BufferAllocators are invalid");
        }

        if (renderBuffer == null)
        {
            compiledChunk.setOverlayTypeUnused(type);
            return;
        }

        VertexBuffer vertexBuffer = renderChunk.getOverlayVertexBuffer(type);

        if (type.isTranslucent() && Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_RESORTING.getBooleanValue())
        {
            BuiltBuffer.SortState sorting = compiledChunk.getTransparentSortingDataForOverlay(type);

            if (sorting == null)
            {
                sorting = renderBuffer.sortQuads(allocator, sorter);

                if (sorting == null)
                {
                    throw new InterruptedException("Sort State failed to sortQuads()");
                }

                compiledChunk.setTransparentSortingDataForOverlay(type, sorting);
            }

            BufferAllocator.CloseableBuffer result = sorting.sortAndStore(allocator, sorter);

            if (result != null)
            {
                renderChunk.uploadSortingState(result, vertexBuffer);
                result.close();
            }
        }

        if (resortOnly == false)
        {
            renderChunk.uploadBuiltBuffer(renderBuffer, vertexBuffer);
        }
    }

    protected void clearChunkUpdates()
    {
        while (this.queueChunkUpdates.isEmpty() == false)
        {
            ChunkRenderTaskSchematic generator = this.queueChunkUpdates.poll();

            if (generator != null)
            {
                generator.finish();
            }
        }
    }

    public boolean hasChunkUpdates()
    {
        return this.queueChunkUpdates.isEmpty() && this.queueChunkUploads.isEmpty();
    }

    protected void stopWorkerThreads()
    {
        this.clearChunkUpdates();

        for (ChunkRenderWorkerLitematica worker : this.listThreadedWorkers)
        {
            worker.notifyToStop();
        }

        for (Thread thread : this.listWorkerThreads)
        {
            try
            {
                thread.interrupt();
                thread.join();
            }
            catch (InterruptedException interruptedexception)
            {
                LOGGER.warn("Interrupted whilst waiting for worker to die", (Throwable)interruptedexception);
            }
        }

        this.queueFreeRenderAllocators.forEach(BufferAllocatorCache::close);
        this.queueFreeRenderAllocators.clear();
    }

    public boolean hasNoFreeRenderAllocators()
    {
        return this.queueFreeRenderAllocators.isEmpty();
    }

    protected static class PendingUpload implements Comparable<ChunkRenderDispatcherLitematica.PendingUpload>
    {
        private final ListenableFutureTask<Object> uploadTask;
        private final double distanceSq;

        public PendingUpload(ListenableFutureTask<Object> uploadTaskIn, double distanceSqIn)
        {
            this.uploadTask = uploadTaskIn;
            this.distanceSq = distanceSqIn;
        }

        public int compareTo(ChunkRenderDispatcherLitematica.PendingUpload other)
        {
            return Doubles.compare(this.distanceSq, other.distanceSq);
        }
    }
}
