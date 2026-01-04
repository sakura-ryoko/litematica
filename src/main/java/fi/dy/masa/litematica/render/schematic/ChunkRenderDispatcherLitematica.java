package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import com.google.common.collect.Queues;
import com.google.common.primitives.Doubles;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.apache.logging.log4j.Logger;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;

public class ChunkRenderDispatcherLitematica
{
    private static final Logger LOGGER = Litematica.LOGGER;
    // Threaded Code
    //private static final ThreadFactory THREAD_FACTORY = (new ThreadFactoryBuilder()).setNameFormat("Litematica Chunk Batcher %d").setDaemon(true).build();

    private final List<Thread> listWorkerThreads;
    private final List<ChunkRenderWorkerLitematica> listThreadedWorkers;
    private final PriorityBlockingQueue<ChunkRenderTaskSchematic> queueChunkUpdates;
    private final BlockingQueue<BufferAllocatorCache> queueFreeRenderAllocators;
    private final Queue<PendingUpload> queueChunkUploads;
//    final Queue<Runnable> queueChunkUploads = Queues.newConcurrentLinkedQueue();
    private final ChunkRenderWorkerLitematica renderWorker;
    private final int countRenderAllocators;
    // Threaded Code
    //private final int countRenderThreads;
    private Vec3 cameraPos;

    public ChunkRenderDispatcherLitematica(ProfilerFiller profiler)
    {
	    this.listWorkerThreads = new ArrayList<>();
	    this.listThreadedWorkers = new ArrayList<>();
		this.queueChunkUpdates = Queues.newPriorityBlockingQueue();
		this.queueChunkUploads = Queues.newPriorityQueue();

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
        this.cameraPos = Vec3.ZERO;

        LOGGER.info("Using {} total BufferAllocator caches", this.countRenderAllocators + 1);

        this.queueFreeRenderAllocators = Queues.newArrayBlockingQueue(this.countRenderAllocators);

        for (int i = 0; i < this.countRenderAllocators; ++i)
        {
            this.queueFreeRenderAllocators.add(new BufferAllocatorCache());
        }

        this.renderWorker = new ChunkRenderWorkerLitematica(this, new BufferAllocatorCache(), profiler);
    }

    protected void setCameraPosition(Vec3 cameraPos)
    {
        this.cameraPos = cameraPos;
    }

    public Vec3 getCameraPos()
    {
        return this.cameraPos;
    }

    protected String getDebugInfo()
    {
        // Threaded Code
        //return String.format("T: %02d, pC: %03d, pU: %03d, aB: %02d", this.listThreadedWorkers.size(), this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderAllocators.size());

        return this.listWorkerThreads.isEmpty() ? String.format("pC: %03d, single-threaded", this.queueChunkUpdates.size()) : String.format("pC: %03d, pU: %1d, aB: %1d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderAllocators.size());
    }

    protected boolean runChunkUploads(long finishTimeNano, ProfilerFiller profiler)
    {
        boolean ranTasks = false;
        //LOGGER.warn("[Dispatch] runChunkUploads() - queueChunkUpdates({}) // queueChunkUploads({})", this.queueChunkUpdates.size(), this.queueChunkUploads.size());

        profiler.push("run_chunk_uploads");
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
                        this.renderWorker.processTask(generator, profiler);
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

        profiler.pop();
        return ranTasks;
    }

    /*
    protected void runAsyncUploads()
    {
        LOGGER.warn("[Dispatch] runAsyncUploads() size []", this.queueChunkUploads.size());

        synchronized (this.queueChunkUploads)
        {
            Runnable upload;

            while((upload = this.queueChunkUploads.poll()) != null)
            {
                upload.run();
            }
        }
    }
     */

    protected boolean updateChunkLater(ChunkRendererSchematicVbo renderChunk, ProfilerFiller profiler)
    {
        //LOGGER.warn("[Dispatch] updateChunkLater()");

        profiler.push("update_chunk_later");
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

            generator.addFinishRunnable(() -> ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator));

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

        profiler.pop();
        return flag1;
    }

    protected boolean updateChunkNow(ChunkRendererSchematicVbo chunkRenderer, ProfilerFiller profiler)
    {
        //LOGGER.warn("[Dispatch] updateChunkNow()");

        profiler.push("update_chunk_now");
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
                this.renderWorker.processTask(generator, profiler);
            }
            catch (InterruptedException ignored) { }

            flag = true;
        }
        finally
        {
            chunkRenderer.getLockCompileTask().unlock();
        }

        profiler.pop();
        return flag;
    }

//    protected boolean uploadChunkNow(ChunkRendererSchematicVbo renderChunk, ProfilerFiller profiler)
//    {
//        LOGGER.warn("[Dispatch] uploadChunkNow()");
//        profiler.push("upload_chunks_now");
//        renderChunk.getLockCompileTask().lock();
//        boolean flag = false;
//
//        try
//        {
//            final ChunkRenderTaskSchematic generator = renderChunk.makeUploadTaskChunkSchematic(this::getCameraPos);
//
//            if (generator == null)
//            {
//                LOGGER.error("[Dispatch] uploadChunkNow() -- Make Task returned null");
//                flag = false;
//                profiler.pop();
//                return flag;
//            }
//
//            try
//            {
//                this.renderWorker.processTask(generator, profiler);
//            }
//            catch (InterruptedException ignored) {}
//
//            flag = true;
//        }
//        finally
//        {
//            renderChunk.getLockCompileTask().unlock();
//        }
//
//        profiler.pop();
//        return flag;
//    }
//
//    protected boolean uploadChunkLater(ChunkRendererSchematicVbo renderChunk, ProfilerFiller profiler)
//    {
//        LOGGER.warn("[Dispatch] uploadChunkLater()");
//        profiler.push("upload_chunks_later");
//        renderChunk.getLockCompileTask().lock();
//        boolean flag;
//
//        try
//        {
//            final ChunkRenderTaskSchematic generator = renderChunk.makeUploadTaskChunkSchematic(this::getCameraPos);
//
//            if (generator == null)
//            {
//                flag = true;
//                profiler.pop();
//                return flag;
//            }
//
//            generator.addFinishRunnable(() -> ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator));
//
//            flag = this.queueChunkUpdates.offer(generator);
//        }
//        finally
//        {
//            renderChunk.getLockCompileTask().unlock();
//        }
//
//        profiler.pop();
//        return flag;
//    }

    protected void stopChunkUpdates(ProfilerFiller profiler)
    {
        //LOGGER.warn("[Dispatch] stopChunkUpdates()");
        profiler.push("stop_chunk_updates");
        this.clearChunkUpdates();
        List<BufferAllocatorCache> list = new ArrayList<>();

        while (list.size() != this.countRenderAllocators)
        {
            this.runChunkUploads(Long.MAX_VALUE, profiler);

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
        profiler.pop();
    }

    public void freeRenderAllocators(BufferAllocatorCache allocatorCache)
    {
        if (allocatorCache != null)
        {
            try
            {
                allocatorCache.close();
            }
            catch (Exception ignored) { }
        }

        allocatorCache = new BufferAllocatorCache();
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

    protected boolean updateTransparencyLater(ChunkRendererSchematicVbo renderChunk, ProfilerFiller profiler)
    {
        profiler.push("update_transparency_later");
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
                profiler.pop();
                return flag;
            }

            generator.addFinishRunnable(() -> ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator));

            flag = this.queueChunkUpdates.offer(generator);
        }
        finally
        {
            renderChunk.getLockCompileTask().unlock();
        }

        profiler.pop();
        return flag;
    }

//    protected ListenableFuture<Object> scheduleChunkUploads(final ChunkRendererSchematicVbo renderChunk, final ChunkRenderDataSchematic chunkRenderData, final double distanceSq, ProfilerFiller profiler)
//    {
//        LOGGER.warn("[Dispatch] scheduleChunkUploads()");
//
//        profiler.push("schedule_chunk_uploads");
//        if (Minecraft.getInstance().isSameThread())
//        {
//            if (!chunkRenderData.isBlockLayerEmpty())
//            {
//                this.uploadChunkNow(renderChunk, profiler);
//            }
//
//            profiler.pop();
//            return Futures.immediateFuture(null);
//        }
//        else
//        {
//            ListenableFutureTask<Object> futureTask = ListenableFutureTask.create(() -> ChunkRenderDispatcherLitematica.this.scheduleChunkUploads(renderChunk, chunkRenderData, distanceSq, profiler), null);
//
//            synchronized (this.queueChunkUploads)
//            {
//                this.queueChunkUploads.add(new PendingUpload(futureTask, distanceSq));
//                profiler.pop();
//                return futureTask;
//            }
//        }
//    }

    protected ListenableFuture<Object> uploadChunkBlocks(final ChunkSectionLayer layer, final BufferAllocatorCache allocators, final ChunkRendererSchematicVbo renderChunk, final ChunkRenderDataSchematic chunkRenderData, final double distanceSq, boolean resortOnly, ProfilerFiller profiler)
    {
        //LOGGER.warn("[Dispatch] uploadChunkBlocks layer [{}]", layer.name());

        profiler.push("upload_chunk_blocks");
        if (Minecraft.getInstance().isSameThread())
        {
            try
            {
                this.uploadVertexBufferByBlockLayer(layer, allocators, renderChunk, chunkRenderData, renderChunk.createVertexSorter(this.getCameraPos(), renderChunk.getOrigin()), resortOnly, profiler);
            }
            catch (Exception e)
            {
                LOGGER.warn("uploadChunkBlocks(): [Dispatch] Error uploading Vertex Buffer for layer [{}], Caught error: [{}]", layer.label(), e.toString());
            }

            profiler.pop();
            return Futures.immediateFuture(null);
        }
        else
        {
            profiler.popPush("upload_chunk_blocks_later");
            /*  Threaded Code

            ListenableFutureTask<Object> futureTask = ListenableFutureTask.create(
                    () -> uploadChunkBlocks(layer, allocators, renderChunk, chunkRenderData, distanceSq, resortOnly),
                    null);
             */

            ListenableFutureTask<Object> futureTask = ListenableFutureTask.create(() -> ChunkRenderDispatcherLitematica.this.uploadChunkBlocks(layer, allocators, renderChunk, chunkRenderData, distanceSq, resortOnly, profiler), null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new PendingUpload(futureTask, distanceSq));
                profiler.pop();
                return futureTask;
            }
        }
    }

    protected ListenableFuture<Object> uploadChunkOverlay(final OverlayRenderType type, final BufferAllocatorCache allocators, final ChunkRendererSchematicVbo renderChunk, final ChunkRenderDataSchematic compiledChunk, final double distanceSq, boolean resortOnly, ProfilerFiller profiler)
    {
        //LOGGER.warn("[Dispatch] uploadChunkOverlay type [{}]", type.name());

        profiler.push("upload_chunk_overlay");
        if (Minecraft.getInstance().isSameThread())
        {
            try
            {
                this.uploadVertexBufferByType(type, allocators, renderChunk, compiledChunk, renderChunk.createVertexSorter(this.getCameraPos(), renderChunk.getOrigin()), resortOnly, profiler);
            }
            catch (Exception e)
            {
                // TODO --> This one will throw if it's not sorted as Translucent,
                //  but it will cause a crash during draw() --> Ignored
                LOGGER.error("uploadChunkOverlay(): [Dispatch] Error uploading Vertex Buffer for overlay type [{}], Caught error: [{}]", type.name(), e.toString());
            }

            profiler.pop();
            return Futures.immediateFuture(null);
        }
        else
        {
            profiler.popPush("upload_chunk_overlay_later");
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.<Object>create(() -> ChunkRenderDispatcherLitematica.this.uploadChunkOverlay(type, allocators, renderChunk, compiledChunk, distanceSq, resortOnly, profiler), null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new PendingUpload(futureTask, distanceSq));
                profiler.pop();
                return futureTask;
            }
        }
    }

    private void uploadVertexBufferByBlockLayer(ChunkSectionLayer layer, @Nonnull BufferAllocatorCache allocators, @Nonnull ChunkRendererSchematicVbo renderChunk, @Nonnull ChunkRenderDataSchematic compiledChunk, @Nonnull VertexSorting sorter, boolean resortOnly, ProfilerFiller profiler)
            throws InterruptedException
    {
        //LOGGER.warn("[Dispatch] uploadVertexBufferByBlockLayer layer [{}]", layer.label());

        profiler.push("upload_vbo_layer_"+layer.label());
        ByteBufferBuilder allocator = allocators.getBufferByBlockLayer(layer);
        MeshData builtBuffer = compiledChunk.getBuiltBufferCache().getBuiltBufferByBlockLayer(layer);

        if (allocator == null)
        {
            //LOGGER.error("[Dispatch] uploadVertexBufferByBlockLayer layer [{}] --> ALLOC NULL", layer.label());
            allocators.closeByBlockLayer(layer);
            compiledChunk.setBlockLayerUnused(layer);
            profiler.pop();
            throw new InterruptedException("BufferAllocators are invalid");
        }

        if (builtBuffer == null)
        {
            //LOGGER.error("[Dispatch] uploadVertexBufferByBlockLayer layer [{}] --> MESHDATA NULL", layer.label());
            compiledChunk.setBlockLayerUnused(layer);
            profiler.pop();
            return;
        }

        if (resortOnly == false)
        {
            renderChunk.uploadBuffersByLayer(layer, builtBuffer);
        }

        if (layer == ChunkSectionLayer.TRANSLUCENT && Configs.Visuals.RENDER_ENABLE_TRANSLUCENT_RESORTING.getBooleanValue())
        {
            MeshData.SortState sorting = compiledChunk.getTransparentSortingDataForBlockLayer(layer);

            if (sorting == null)
            {
                sorting = builtBuffer.sortQuads(allocator, sorter);

                if (sorting == null)
                {
                    profiler.pop();
                    //LOGGER.error("[Dispatch] uploadVertexBufferByBlockLayer layer [{}] --> SORT FAILURE", layer.label());
                    throw new InterruptedException("Sort State failed to sortQuads()");
                }

                compiledChunk.setTransparentSortingDataForBlockLayer(layer, sorting);
            }

            ByteBufferBuilder.Result result = sorting.buildSortedIndexBuffer(allocator, sorter);

            if (result != null)
            {
                //LOGGER.warn("[Dispatch] uploadVertexBufferByBlockLayer layer [{}] --> UPLOAD INDEX", layer.label());
                renderChunk.uploadIndexByBlockLayer(layer, result);
                result.close();
            }
        }

        //LOGGER.warn("[Dispatch] uploadVertexBufferByBlockLayer layer [{}] --> DONE", layer.label());
        profiler.pop();
    }

    private void uploadVertexBufferByType(OverlayRenderType type, @Nonnull BufferAllocatorCache allocators, @Nonnull ChunkRendererSchematicVbo renderChunk, @Nonnull ChunkRenderDataSchematic compiledChunk, @Nonnull VertexSorting sorter, boolean resortOnly, ProfilerFiller profiler)
            throws InterruptedException
    {
        //LOGGER.warn("[Dispatch] uploadVertexBufferByType type [{}]", type.name());

        profiler.push("upload_vbo_overlay_"+type.name());
        ByteBufferBuilder allocator = allocators.getBufferByOverlay(type);
        MeshData builtBuffer = compiledChunk.getBuiltBufferCache().getBuiltBufferByType(type);

        if (allocator == null)
        {
            //LOGGER.error("[Dispatch] uploadVertexBufferByType type [{}] --> ALLOC NULL", type.name());
            allocators.closeByType(type);
            compiledChunk.setOverlayTypeUnused(type);
            profiler.pop();
            throw new InterruptedException("BufferAllocators are invalid");
        }

        if (builtBuffer == null)
        {
            //LOGGER.error("[Dispatch] uploadVertexBufferByType type [{}] --> MESHDATA NULL", type.name());
            compiledChunk.setOverlayTypeUnused(type);
            profiler.pop();
            return;
        }

        if (resortOnly == false)
        {
            //LOGGER.warn("[Dispatch] uploadVertexBufferByType type [{}] --> UPLOAD VERTEX", type.name());
            renderChunk.uploadBuffersByType(type, builtBuffer);
        }

//        if (type.isTranslucent() && Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_RESORTING.getBooleanValue())
//        {
//            BuiltBuffer.SortState sorting = compiledChunk.getTransparentSortingDataForOverlay(type);
//
//            if (sorting == null)
//            {
//                sorting = builtBuffer.sortQuads(allocator, sorter);
//
//                if (sorting == null)
//                {
//                    profiler.pop();
//                    //LOGGER.warn("[Dispatch] uploadVertexBufferByType type [{}] --> SORT FAILURE", type.name());
//                    throw new InterruptedException("Sort State failed to sortQuads()");
//                }
//
//                compiledChunk.setTransparentSortingDataForOverlay(type, sorting);
//            }
//
//            BufferAllocator.CloseableBuffer result = sorting.sortAndStore(allocator, sorter);
//
//            if (result != null)
//            {
//                //LOGGER.warn("[Dispatch] uploadVertexBufferByType type [{}] --> UPLOAD INDEX", type.name());
//                renderChunk.uploadIndexByType(type, result);
//                result.close();
//            }
//        }

        //LOGGER.warn("[Dispatch] uploadVertexBufferByType type [{}] --> DONE", type.name());
        profiler.pop();
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
//        LOGGER.warn("[Dispatch] stopWorkerThreads()");

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

    protected static class PendingUpload implements Comparable<PendingUpload>
    {
        private final ListenableFutureTask<Object> uploadTask;
        private final double distanceSq;

        public PendingUpload(ListenableFutureTask<Object> uploadTaskIn, double distanceSqIn)
        {
            this.uploadTask = uploadTaskIn;
            this.distanceSq = distanceSqIn;
        }

        public int compareTo(PendingUpload other)
        {
            return Doubles.compare(this.distanceSq, other.distanceSq);
        }
    }
}
