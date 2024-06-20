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
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class ChunkRenderDispatcherLitematica
{
    private static final Logger LOGGER = Litematica.logger;
    private static final ThreadFactory THREAD_FACTORY = (new ThreadFactoryBuilder()).setNameFormat("Litematica Chunk Batcher %d").setDaemon(true).build();

    private final List<Thread> listWorkerThreads = new ArrayList<>();
    private final List<ChunkRenderWorkerLitematica> listThreadedWorkers = new ArrayList<>();
    private final PriorityBlockingQueue<ChunkRenderTaskSchematic> queueChunkUpdates = Queues.newPriorityBlockingQueue();
    private final BlockingQueue<BufferAllocatorCache> queueFreeRenderAllocators;
    private final Queue<ChunkRenderDispatcherLitematica.PendingUpload> queueChunkUploads = Queues.newPriorityQueue();
    private final ChunkRenderWorkerLitematica renderWorker;
    private final int countRenderAllocators;
    private Vec3d cameraPos;

    public ChunkRenderDispatcherLitematica()
    {
        // TODO/FIXME 1.17
        int threadLimitMemory = Math.max(1, (int)((double)Runtime.getRuntime().maxMemory() * 0.3D) / 10485760);
        int threadLimitCPU = Math.max(1, MathHelper.clamp(Runtime.getRuntime().availableProcessors(), 1, threadLimitMemory / 5));

        this.countRenderAllocators = 2;
        this.cameraPos = Vec3d.ZERO;


        if (threadLimitCPU > 1)
        {
            LOGGER.info("Creating {} render threads", threadLimitCPU);

            for (int i = 0; i < threadLimitCPU; ++i)
            {
                ChunkRenderWorkerLitematica worker = new ChunkRenderWorkerLitematica(this);
                Thread thread = THREAD_FACTORY.newThread(worker);
                thread.start();
                this.listThreadedWorkers.add(worker);
                this.listWorkerThreads.add(thread);
            }
        }

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
        //return this.listWorkerThreads.isEmpty() ? String.format("pC: %03d, single-threaded", this.queueChunkUpdates.size()) : String.format("pC: %03d, pU: %1d, aB: %1d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderAllocators.size());
        return String.format("pC: %03d, pU: %1d, aB: %1d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderAllocators.size());
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
        final ChunkRenderTaskSchematic generator = renderChunk.makeCompileTaskChunkSchematic(this::getCameraPos);
        generator.addFinishRunnable(() -> queueChunkUpdates.remove(generator));
        boolean flag = queueChunkUpdates.offer(generator);
        if(!flag)
            generator.finish();
        return flag;
    }

    protected boolean updateChunkNow(ChunkRendererSchematicVbo chunkRenderer)
    {
        try {
            renderWorker.processTask(chunkRenderer.makeCompileTaskChunkSchematic(this::getCameraPos));
            return true;
        } catch (InterruptedException e) {
            LOGGER.warn("updateChunkNow(): Process Interrupted; error message: [{}]", e.getLocalizedMessage());
            return false;
        }
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
        final ChunkRenderTaskSchematic generator = renderChunk.makeCompileTaskTransparencySchematic(this::getCameraPos);
        if(generator==null)
            return true;
        generator.addFinishRunnable(() -> ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator));
        return queueChunkUpdates.offer(generator);
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
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.create(
                    () -> uploadChunkBlocks(layer, allocators, renderChunk, chunkRenderData, distanceSq, resortOnly),
                    null);

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
                // TODO --> This one will throw since it's no longer sorted as Translucent,
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
        //LOGGER.warn("uploadVertexBufferByLayer() [Dispatch] for layer [{}] - INIT", ChunkRenderLayers.getFriendlyName(layer));

        BufferAllocator allocator = allocators.getBufferByLayer(layer);
        BuiltBuffer renderBuffer = compiledChunk.getBuiltBufferCache().getBuiltBufferByLayer(layer);

        if (allocator == null)
        {
            //LOGGER.error("uploadVertexBufferByLayer() [Dispatch] for overlay type [{}] - INVALID BUFFERS", ChunkRenderLayers.getFriendlyName(layer));
            allocators.closeByLayer(layer);
            throw new InterruptedException("BufferAllocators are invalid");
        }

        if (renderBuffer == null)
        {
            //LOGGER.error("uploadVertexBufferByLayer() [Dispatch] for layer [{}] - NOT BUILT", ChunkRenderLayers.getFriendlyName(layer));
            throw new InterruptedException("BuiltBuffer was not built");
        }

        VertexBuffer vertexBuffer = renderChunk.getBlocksVertexBufferByLayer(layer);

        if (layer == RenderLayer.getTranslucent())
        {
            //LOGGER.warn("uploadVertexBufferByLayer() [Dispatch] for layer [{}] - Translucent START", ChunkRenderLayers.getFriendlyName(layer));
            BuiltBuffer.SortState sorting = compiledChunk.getTransparentSortingData();

            if (sorting == null)
            {
                sorting = renderBuffer.sortQuads(allocator, sorter);

                if (sorting == null)
                {
                    //LOGGER.error("uploadVertexBufferByLayer() [Dispatch] for layer [{}] - Sort State FAILED", ChunkRenderLayers.getFriendlyName(layer));
                    throw new InterruptedException("Sort State failed to sortQuads()");
                }

                compiledChunk.setTransparentSortingData(sorting);
                //LOGGER.warn("uploadVertexBufferByLayer() [Dispatch] for layer [{}] - Sort State built", ChunkRenderLayers.getFriendlyName(layer));
            }

            BufferAllocator.CloseableBuffer result = sorting.sortAndStore(allocator, sorter);

            if (result != null)
            {
                //LOGGER.error("uploadVertexBufferByLayer() [Dispatch] for layer [{}] - Result Buffer built -> UPLOADING", ChunkRenderLayers.getFriendlyName(layer));
                renderChunk.uploadSortingState(result, vertexBuffer);
                result.close();

                //LOGGER.warn("uploadVertexBufferByLayer() [Dispatch] for layer [{}] - Translucent Sort State UPLOADED", ChunkRenderLayers.getFriendlyName(layer));
            }
        }

        if (resortOnly == false)
        {
            renderChunk.uploadBuiltBuffer(renderBuffer, vertexBuffer);
            //LOGGER.warn("uploadVertexBufferByLayer() [Dispatch] for layer [{}] - Built Buffer UPLOADED", ChunkRenderLayers.getFriendlyName(layer));
        }
        //renderBuffer.close();

        //LOGGER.error("uploadVertexBufferByLayer() [Dispatch] for layer [{}] - DONE", ChunkRenderLayers.getFriendlyName(layer));
    }

    private void uploadVertexBufferByType(OverlayRenderType type, @Nonnull BufferAllocatorCache allocators, @Nonnull ChunkRendererSchematicVbo renderChunk, @Nonnull ChunkRenderDataSchematic compiledChunk, @Nonnull VertexSorter sorter, boolean resortOnly)
            throws InterruptedException
    {
        //LOGGER.warn("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - INIT", type.getDrawMode().name());

        BufferAllocator allocator = allocators.getBufferByOverlay(type);
        BuiltBuffer renderBuffer = compiledChunk.getBuiltBufferCache().getBuiltBufferByType(type);

        if (allocator == null)
        {
            //LOGGER.error("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - INVALID BUFFERS", type.getDrawMode().name());
            allocators.closeByType(type);
            throw new InterruptedException("BufferAllocators are invalid");
        }

        if (renderBuffer == null)
        {
            //LOGGER.error("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - NOT BUILT", type.getDrawMode().name());
            throw new InterruptedException("BuiltBuffer was not built");
        }

        VertexBuffer vertexBuffer = renderChunk.getOverlayVertexBuffer(type);

        /**/
        if (type.isTranslucent())
        {
            //LOGGER.warn("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - Translucent START", type.getDrawMode().name());
            BuiltBuffer.SortState sorting = compiledChunk.getTransparentSortingDataForOverlay(type);

            if (sorting == null)
            {
                sorting = renderBuffer.sortQuads(allocator, sorter);

                if (sorting == null)
                {
                    //LOGGER.error("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - Sort State FAILED", type.getDrawMode().name());
                    throw new InterruptedException("Sort State failed to sortQuads()");
                }

                compiledChunk.setTransparentSortingDataForOverlay(type, sorting);
                //LOGGER.warn("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - Sort State built", type.getDrawMode().name());
            }

            BufferAllocator.CloseableBuffer result = sorting.sortAndStore(allocator, sorter);

            if (result != null)
            {
                //LOGGER.error("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - Result Buffer built -> UPLOADING", type.getDrawMode().name());
                renderChunk.uploadSortingState(result, vertexBuffer);
                result.close();

                //LOGGER.warn("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - Translucent Sort State UPLOADED", type.getDrawMode().name());
            }
        }
      
        /**/

        if (resortOnly == false)
        {
            renderChunk.uploadBuiltBuffer(renderBuffer, vertexBuffer);
            //LOGGER.warn("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - Built Buffer UPLOADED", type.getDrawMode().name());
        }
        //renderBuffer.close();

        //LOGGER.error("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - DONE", type.getDrawMode().name());
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
