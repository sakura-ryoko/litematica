package fi.dy.masa.litematica.render.schematic;

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
import org.apache.logging.log4j.Logger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.cache.BufferAllocatorCache;
import fi.dy.masa.litematica.render.cache.BufferBuilderCache;
import fi.dy.masa.litematica.render.cache.BufferBuilderPatch;
import fi.dy.masa.litematica.render.cache.BuiltBufferCache;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class ChunkRenderDispatcherLitematica
{
    private static final Logger LOGGER = Litematica.logger;
    private static final ThreadFactory THREAD_FACTORY = (new ThreadFactoryBuilder()).setNameFormat("Litematica Chunk Batcher %d").setDaemon(true).build();

    private final List<Thread> listWorkerThreads = new ArrayList<>();
    private final List<ChunkRenderWorkerLitematica> listThreadedWorkers = new ArrayList<>();
    private final PriorityBlockingQueue<ChunkRenderTaskSchematic> queueChunkUpdates = Queues.newPriorityBlockingQueue();
    private final BlockingQueue<BufferAllocatorCache> queueFreeRenderAllocators;
    private final BlockingQueue<BufferBuilderCache> queueFreeRenderBuilders;
    private final BlockingQueue<BuiltBufferCache> queueFreeRenderBuiltCache;
    private final Queue<ChunkRenderDispatcherLitematica.PendingUpload> queueChunkUploads = Queues.newPriorityQueue();
    private final ChunkRenderWorkerLitematica renderWorker;
    private final int countRenderAllocators;
    private final int countRenderBuilders;
    private final int countRenderBuiltBuffers;
    private Vec3d cameraPos;

    public ChunkRenderDispatcherLitematica()
    {
        // TODO/FIXME 1.17
        //int threadLimitMemory = Math.max(1, (int)((double)Runtime.getRuntime().maxMemory() * 0.3D) / 10485760);
        //int threadLimitCPU = Math.max(1, MathHelper.clamp(Runtime.getRuntime().availableProcessors(), 1, threadLimitMemory / 5));
        //this.countRenderBuilders = MathHelper.clamp(threadLimitCPU * 10, 1, threadLimitMemory);
        this.countRenderAllocators = 2;
        this.countRenderBuilders = 2;
        this.countRenderBuiltBuffers = 2;
        this.cameraPos = Vec3d.ZERO;

        /*
        if (threadLimitCPU > 1)
        {
            Litematica.logger.info("Creating {} render threads", threadLimitCPU);

            for (int i = 0; i < threadLimitCPU; ++i)
            {
                ChunkRenderWorkerLitematica worker = new ChunkRenderWorkerLitematica(this);
                Thread thread = THREAD_FACTORY.newThread(worker);
                thread.start();
                this.listThreadedWorkers.add(worker);
                this.listWorkerThreads.add(thread);
            }
        }
        */

        Litematica.logger.info("Using {} total BufferAllocator caches", this.countRenderAllocators + 1);
        Litematica.logger.info("Using {} total BufferBuilder caches", this.countRenderBuilders + 1);
        Litematica.logger.info("Using {} total BuiltBuffer caches", this.countRenderBuiltBuffers + 1);

        this.queueFreeRenderAllocators = Queues.newArrayBlockingQueue(this.countRenderAllocators);
        this.queueFreeRenderBuilders = Queues.newArrayBlockingQueue(this.countRenderBuilders);
        this.queueFreeRenderBuiltCache = Queues.newArrayBlockingQueue(this.countRenderBuiltBuffers);

        for (int i = 0; i < this.countRenderAllocators; ++i)
        {
            this.queueFreeRenderAllocators.add(new BufferAllocatorCache());
        }
        for (int i = 0; i < this.countRenderBuilders; ++i)
        {
            this.queueFreeRenderBuilders.add(new BufferBuilderCache());
        }
        for (int i = 0; i < this.countRenderBuiltBuffers; ++i)
        {
            this.queueFreeRenderBuiltCache.add(new BuiltBufferCache());
        }

        this.renderWorker = new ChunkRenderWorkerLitematica(this, new BufferAllocatorCache(), new BufferBuilderCache(), new BuiltBufferCache());
    }

    public void setCameraPosition(Vec3d cameraPos)
    {
        this.cameraPos = cameraPos;
    }

    public Vec3d getCameraPos()
    {
        return this.cameraPos;
    }

    public String getDebugInfo()
    {
        return this.listWorkerThreads.isEmpty() ? String.format("pC: %03d, single-threaded", this.queueChunkUpdates.size()) : String.format("pC: %03d, pU: %1d, aB: %1d, bB: %1d, cB: %1d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderAllocators.size(), this.queueFreeRenderBuilders.size(), this.queueFreeRenderBuiltCache.size());
    }

    public boolean runChunkUploads(long finishTimeNano)
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
                    catch (InterruptedException var8)
                    {
                        LOGGER.warn("Skipped task due to interrupt");
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

    public boolean updateChunkLater(ChunkRendererSchematicVbo renderChunk)
    {
        //if (GuiBase.isCtrlDown()) System.out.printf("updateChunkLater()\n");
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

    public boolean updateChunkNow(ChunkRendererSchematicVbo chunkRenderer)
    {
        //if (GuiBase.isCtrlDown()) System.out.printf("updateChunkNow()\n");
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

    public void stopChunkUpdates()
    {
        this.clearChunkUpdates();
        List<BufferBuilderCache> list = new ArrayList<>();

        while (list.size() != this.countRenderBuilders)
        {
            this.runChunkUploads(Long.MAX_VALUE);

            try
            {
                list.add(this.allocateRenderBuilder());
            }
            catch (InterruptedException e)
            {
            }
        }

        this.queueFreeRenderBuilders.addAll(list);
    }

    public void freeRenderAllocators(BufferAllocatorCache allocatorCache)
    {
        this.queueFreeRenderAllocators.add(allocatorCache);
    }

    public void freeRenderBuilder(BufferBuilderCache builderCache)
    {
        this.queueFreeRenderBuilders.add(builderCache);
    }

    public void freeRenderBuiltCache(BuiltBufferCache builtBufferCache)
    {
        this.queueFreeRenderBuiltCache.add(builtBufferCache);
    }

    public BufferAllocatorCache allocateRenderAllocators() throws InterruptedException
    {
        return this.queueFreeRenderAllocators.take();
    }

    public BufferBuilderCache allocateRenderBuilder() throws InterruptedException
    {
        return this.queueFreeRenderBuilders.take();
    }

    public BuiltBufferCache allocateRenderBuiltCache() throws InterruptedException
    {
        return this.queueFreeRenderBuiltCache.take();
    }

    public ChunkRenderTaskSchematic getNextChunkUpdate() throws InterruptedException
    {
        return this.queueChunkUpdates.take();
    }

    public boolean updateTransparencyLater(ChunkRendererSchematicVbo renderChunk)
    {
        //if (GuiBase.isCtrlDown()) System.out.printf("updateTransparencyLater()\n");
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

    public ListenableFuture<Object> uploadChunkBlocks(final RenderLayer layer, final BufferAllocator allocator, final BufferBuilder buffer, final BuiltBufferCache builtBuffers,
            final ChunkRendererSchematicVbo renderChunk, final ChunkRenderDataSchematic chunkRenderData, final double distanceSq)
    {
        Litematica.logger.warn("uploadChunkBlocks() [Dispatch] for layer [{}]", layer.getDrawMode().name());

        if (MinecraftClient.getInstance().isOnThread())
        {
            //if (GuiBase.isCtrlDown()) System.out.printf("uploadChunkBlocks()\n");
            this.uploadVertexBufferByLayer(layer, allocator, buffer, builtBuffers, renderChunk.getBlocksVertexBufferByLayer(layer));
            return Futures.<Object>immediateFuture(null);
        }
        else
        {
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.<Object>create(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.uploadChunkBlocks(layer, allocator, buffer, builtBuffers, renderChunk, chunkRenderData, distanceSq);
                }
            }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new ChunkRenderDispatcherLitematica.PendingUpload(futureTask, distanceSq));
                return futureTask;
            }
        }
    }

    public ListenableFuture<Object> uploadChunkOverlay(final OverlayRenderType type, final BufferAllocator allocator, final BufferBuilder buffer, final BuiltBufferCache builtBuffers,
            final ChunkRendererSchematicVbo renderChunk, final ChunkRenderDataSchematic compiledChunk, final double distanceSq)
    {
        Litematica.logger.warn("uploadChunkOverlay() [Dispatch] for overlay type [{}]", type.getDrawMode().name());

        if (MinecraftClient.getInstance().isOnThread())
        {
            //if (GuiBase.isCtrlDown()) System.out.printf("uploadChunkOverlay()\n");
            this.uploadVertexBufferByType(type, allocator, buffer, builtBuffers, renderChunk.getOverlayVertexBuffer(type));
            return Futures.<Object>immediateFuture(null);
        }
        else
        {
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.<Object>create(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.uploadChunkOverlay(type, allocator, buffer, builtBuffers, renderChunk, compiledChunk, distanceSq);
                }
            }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new ChunkRenderDispatcherLitematica.PendingUpload(futureTask, distanceSq));
                return futureTask;
            }
        }
    }

    private void uploadVertexBufferByLayer(RenderLayer layer, BufferAllocator allocator, BufferBuilder buffer, BuiltBufferCache builtBuffers, VertexBuffer vertexBuffer)
    {
        Litematica.logger.warn("uploadVertexBufferByType() [Dispatch] for layer [{}] - INIT", layer.getDrawMode().name());

        BuiltBuffer renderBuffer = builtBuffers.getBuiltBufferByLayer(layer);
        //BuiltBuffer renderBuffer = buffer.end();

        if (renderBuffer == null)
        {
            renderBuffer = buffer.endNullable();

            if (renderBuffer == null)
            {
                Litematica.logger.error("uploadVertexBufferByType() [Dispatch] for layer [{}] - throw for NULL", layer.getDrawMode().name());

                return;
            }
            builtBuffers.storeBuiltBufferByLayer(layer, renderBuffer);
        }

        if (layer.isTranslucent())
        {
            // NO-OP
        }

        if (vertexBuffer.isClosed())
        {
            renderBuffer.close();
            return;
        }
        vertexBuffer.bind();
        vertexBuffer.upload(renderBuffer);
        VertexBuffer.unbind();

        Litematica.logger.error("uploadVertexBufferByType() [Dispatch] for layer [{}] - DONE", layer.getDrawMode().name());
    }

    private void uploadVertexBufferByType(OverlayRenderType type, BufferAllocator allocator, BufferBuilder buffer, BuiltBufferCache builtBuffers, VertexBuffer vertexBuffer)
    {
        Litematica.logger.warn("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - INIT", type.getDrawMode().name());

        BuiltBuffer renderBuffer = builtBuffers.getBuiltBufferByType(type);
        //BuiltBuffer renderBuffer = buffer.end();

        if (renderBuffer == null)
        {
            renderBuffer = buffer.endNullable();

            if (renderBuffer == null)
            {
                Litematica.logger.error("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - throw for NULL", type.getDrawMode().name());

                return;
            }
            builtBuffers.storeBuiltBufferByType(type, renderBuffer);
        }

        if (type.isTranslucent())
        {
            // NO-OP
        }

        if (vertexBuffer.isClosed())
        {
            renderBuffer.close();
            return;
        }
        vertexBuffer.bind();
        vertexBuffer.upload(renderBuffer);
        VertexBuffer.unbind();

        Litematica.logger.error("uploadVertexBufferByType() [Dispatch] for overlay type [{}] - DONE", type.getDrawMode().name());
    }

    public void clearChunkUpdates()
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

    public void stopWorkerThreads()
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

        this.queueFreeRenderBuilders.clear();
    }

    public boolean hasNoFreeRenderBuilders()
    {
        return this.queueFreeRenderBuilders.isEmpty();
    }

    public static class PendingUpload implements Comparable<ChunkRenderDispatcherLitematica.PendingUpload>
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
