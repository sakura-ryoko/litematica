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
import org.apache.logging.log4j.Logger;
import net.minecraft.class_9799;
import net.minecraft.class_9801;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
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
    private final BlockingQueue<BufferBuilderCache> queueFreeRenderResults;
    private final Queue<ChunkRenderDispatcherLitematica.PendingUpload> queueChunkUploads = Queues.newPriorityQueue();
    private final ChunkRenderWorkerLitematica renderWorker;
    private final int countRenderResults;
    private Vec3d cameraPos;

    public ChunkRenderDispatcherLitematica()
    {
        // TODO/FIXME 1.17
        //int threadLimitMemory = Math.max(1, (int)((double)Runtime.getRuntime().maxMemory() * 0.3D) / 10485760);
        //int threadLimitCPU = Math.max(1, MathHelper.clamp(Runtime.getRuntime().availableProcessors(), 1, threadLimitMemory / 5));
        //this.countRenderBuilders = MathHelper.clamp(threadLimitCPU * 10, 1, threadLimitMemory);
        this.countRenderResults = 2;
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

        Litematica.logger.info("Using {} total BufferBuilder (Results) caches", this.countRenderResults + 1);

        this.queueFreeRenderResults = Queues.newArrayBlockingQueue(this.countRenderResults);

        for (int i = 0; i < this.countRenderResults; ++i)
        {
            Litematica.logger.error("BufferBuilderCache[{}]", i);
            this.queueFreeRenderResults.add(new BufferBuilderCache());
        }

        Litematica.logger.error("ChunkRenderDispatcherLitematica: assign renderWorker");

        this.renderWorker = new ChunkRenderWorkerLitematica(this, new BufferBuilderCache());

        Litematica.logger.error("ChunkRenderDispatcherLitematica: [DONE]");
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
        return this.listWorkerThreads.isEmpty() ? String.format("pC: %03d, single-threaded", this.queueChunkUpdates.size()) : String.format("pC: %03d, pU: %1d, aB: %1d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderResults.size());
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
        List<BufferBuilderCache> resultList = new ArrayList<>();

        while (resultList.size() != this.countRenderResults)
        {
            this.runChunkUploads(Long.MAX_VALUE);

            try
            {
                resultList.add(this.allocateRenderResults());
            }
            catch (InterruptedException e)
            {
            }
        }

        this.queueFreeRenderResults.addAll(resultList);
    }

    public void freeRenderResults(BufferBuilderCache resultCache)
    {
        this.queueFreeRenderResults.add(resultCache);
    }

    public BufferBuilderCache allocateRenderResults() throws InterruptedException
    {
        return this.queueFreeRenderResults.take();
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

    public ListenableFuture<Object> uploadChunkBlocks(final RenderLayer layer, final ChunkRendererSchematicVbo renderChunk,
                                                      final ChunkRenderDataSchematic chunkRenderData, final double distanceSq)
    {
        if (MinecraftClient.getInstance().isOnThread())
        {
            //if (GuiBase.isCtrlDown()) System.out.printf("uploadChunkBlocks()\n");
            // FIXME
            if (layer == RenderLayer.getTranslucent())
            {
                class_9799.class_9800 result = renderChunk.getSectionBufferCache().getBufferByLayer(layer).method_60807();

                try
                {
                    if (result != null)
                    {
                        this.uploadSectionIndex(result, renderChunk.getBlocksVertexBufferByLayer(layer));
                    }
                    else
                    {
                        Litematica.logger.error("uploadChunkBlocks: failed to uploadSectionIndex for Translucent layer");
                    }
                }
                catch (Exception e)
                {
                    Litematica.logger.error("uploadChunkBlocks: failed to upload results for Translucent layer");
                }
            }
            else
            {
                class_9801 meshData = renderChunk.getMeshDataCache().getMeshByLayer(layer);

                try
                {
                    this.uploadSectionLayer(meshData, renderChunk.getBlocksVertexBufferByLayer(layer));
                }
                catch (Exception e)
                {
                    Litematica.logger.error("uploadChunkBlocks: failed to upload MeshData");
                }
            }
            return Futures.<Object>immediateFuture(null);
        }
        else
        {
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.<Object>create(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.uploadChunkBlocks(layer, renderChunk, chunkRenderData, distanceSq);
                }
            }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new ChunkRenderDispatcherLitematica.PendingUpload(futureTask, distanceSq));
                return futureTask;
            }
        }
    }

    public ListenableFuture<Object> uploadChunkOverlay(final OverlayRenderType type, final ChunkRendererSchematicVbo renderChunk,
                                                       final ChunkRenderDataSchematic compiledChunk, final double distanceSq)
    {
        if (MinecraftClient.getInstance().isOnThread())
        {
            //if (GuiBase.isCtrlDown()) System.out.printf("uploadChunkOverlay()\n");
            // FIXME
            //this.uploadSectionIndex(result, renderChunk.getOverlayVertexBuffer(type));
            class_9801 meshData = renderChunk.getMeshDataCache().getMeshByType(type);

            try
            {
                this.uploadSectionLayer(meshData, renderChunk.getOverlayVertexBuffer(type));
            }
            catch (Exception e)
            {
                Litematica.logger.error("uploadChunkOverlay: failed to upload MeshData");
            }

            return Futures.<Object>immediateFuture(null);
        }
        else
        {
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.<Object>create(new Runnable()
            {
                @Override
                public void run()
                {
                    ChunkRenderDispatcherLitematica.this.uploadChunkOverlay(type, renderChunk, compiledChunk, distanceSq);
                }
            }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new ChunkRenderDispatcherLitematica.PendingUpload(futureTask, distanceSq));
                return futureTask;
            }
        }
    }

    // FIXME
    /*
    private void uploadVertexBuffer(BufferBuilder buffer, VertexBuffer vertexBuffer)
    {
        BufferBuilder.BuiltBuffer renderBuffer;

        if (buffer instanceof OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder compatBuffer)
        {
            if (compatBuffer.lastRenderBuildBuffer != null)
            {
                renderBuffer = compatBuffer.lastRenderBuildBuffer;
            }
            else
            {
                renderBuffer = compatBuffer.end();
            }
        }
        else
        {
            renderBuffer = buffer.end();
        }

        vertexBuffer.bind();
        vertexBuffer.upload(renderBuffer);
        VertexBuffer.unbind();
    }
     */

    private void uploadSectionLayer(@Nonnull class_9801 meshData, @Nonnull VertexBuffer vertexBuffer)
    {
        if (vertexBuffer.isClosed())
        {
            meshData.close();
            return;
        }
        vertexBuffer.bind();
        vertexBuffer.upload(meshData);
        VertexBuffer.unbind();
    }

    private void uploadSectionIndex(@Nonnull class_9799.class_9800 result, @Nonnull VertexBuffer vertexBuffer)
    {
        if (vertexBuffer.isClosed())
        {
            result.close();
        }
        else
        {
            vertexBuffer.bind();
            vertexBuffer.method_60829(result);
            VertexBuffer.unbind();
        }
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

        this.queueFreeRenderResults.clear();
    }

    public boolean hasNoFreeRenderBuilders()
    {
        return this.queueFreeRenderResults.isEmpty();
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
