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
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.cache.ChunkRenderCache;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class ChunkRenderDispatcherLitematica
{
    private static final Logger LOGGER = Litematica.logger;
    private static final ThreadFactory THREAD_FACTORY = (new ThreadFactoryBuilder()).setNameFormat("Litematica Chunk Batcher %d").setDaemon(true).build();

    private final List<Thread> listWorkerThreads = new ArrayList<>();
    private final List<ChunkRenderWorkerLitematica> listThreadedWorkers = new ArrayList<>();
    private final PriorityBlockingQueue<ChunkRenderTaskSchematic> queueChunkUpdates = Queues.newPriorityBlockingQueue();
    private final BlockingQueue<ChunkRenderCache> queueFreeRenderCache;
    private final Queue<ChunkRenderDispatcherLitematica.PendingUpload> queueChunkUploads = Queues.newPriorityQueue();
    private final ChunkRenderWorkerLitematica renderWorker;
    private final int countRenderCache;
    private Vec3d cameraPos;

    public ChunkRenderDispatcherLitematica()
    {
        // TODO/FIXME 1.17
        //int threadLimitMemory = Math.max(1, (int)((double)Runtime.getRuntime().maxMemory() * 0.3D) / 10485760);
        //int threadLimitCPU = Math.max(1, MathHelper.clamp(Runtime.getRuntime().availableProcessors(), 1, threadLimitMemory / 5));
        //this.countRenderBuilders = MathHelper.clamp(threadLimitCPU * 10, 1, threadLimitMemory);
        this.countRenderCache = 2;
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

        Litematica.logger.info("[Dispatch] Using {} total BufferBuilder caches", this.countRenderCache + 1);

        this.queueFreeRenderCache = Queues.newArrayBlockingQueue(this.countRenderCache);

        for (int i = 0; i < this.countRenderCache; ++i)
        {
            this.queueFreeRenderCache.add(new ChunkRenderCache());
        }

        this.renderWorker = new ChunkRenderWorkerLitematica(this, new ChunkRenderCache());

        Litematica.logger.error("ChunkRenderDispatcherLitematica: [Dispatch] [DONE]");
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
        return this.listWorkerThreads.isEmpty() ? String.format("pC: %03d, single-threaded", this.queueChunkUpdates.size()) : String.format("pC: %03d, pU: %1d, aB: %1d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), this.queueFreeRenderCache.size());
    }

    public boolean runChunkUploads(long finishTimeNano)
    {
        boolean ranTasks = false;

        Litematica.logger.warn("runChunkUploads() [Dispatch]");

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
        Litematica.logger.warn("updateChunkLater() [Dispatch]");

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
        Litematica.logger.warn("updateChunkNow() [Dispatch]");

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
        List<ChunkRenderCache> list = new ArrayList<>();

        while (list.size() != this.countRenderCache)
        {
            this.runChunkUploads(Long.MAX_VALUE);

            try
            {
                list.add(this.allocateRenderCache());
            }
            catch (InterruptedException e)
            {
            }
        }

        this.queueFreeRenderCache.addAll(list);
    }

    public void freeRenderCache(ChunkRenderCache renderCache)
    {
        this.queueFreeRenderCache.add(renderCache);
    }

    public ChunkRenderCache allocateRenderCache() throws InterruptedException
    {
        return this.queueFreeRenderCache.take();
    }

    public ChunkRenderTaskSchematic getNextChunkUpdate() throws InterruptedException
    {
        return this.queueChunkUpdates.take();
    }

    public boolean updateTransparencyLater(ChunkRendererSchematicVbo renderChunk)
    {
        Litematica.logger.warn("updateTransparencyLater() [Dispatch]");

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

    public ListenableFuture<Object> uploadChunkBlocks(final RenderLayer layer, final ChunkRenderCache renderCache, final ChunkRendererSchematicVbo renderChunk,
                                                      final ChunkRenderDataSchematic chunkRenderData, final double distanceSq)
    {
        Litematica.logger.warn("uploadChunkBlocks() [Dispatch] start for layer [{}]", layer.getDrawMode().name());

        if (MinecraftClient.getInstance().isOnThread())
        {
            //if (GuiBase.isCtrlDown()) System.out.printf("uploadChunkBlocks()\n");
            //this.uploadVertexBuffer(buffer, renderChunk.getBlocksVertexBufferByLayer(layer));

            Litematica.logger.warn("uploadChunkBlocks() [Dispatch] getMeshData for layer [{}]", layer.getDrawMode().name());
            //BuiltBuffer meshData = renderChunk.getMeshDataCache().getMeshByLayer(layer);
            BuiltBuffer meshData = renderCache.getBuiltBufferByLayer(layer);

            if (meshData == null)
            {
                Litematica.logger.warn("uploadChunkBlocks() [Dispatch] meshData = null for layer [{}]", layer.getDrawMode().name());
                return Futures.<Object>immediateFuture(null);
            }
            // FIXME
            if (layer == RenderLayer.getTranslucent())
            {
                Litematica.logger.warn("uploadChunkBlocks() [Dispatch] get Allocator results for layer [{}]", layer.getDrawMode().name());
                BufferAllocator.CloseableBuffer result = renderCache.getAllocatorByLayer(layer).getAllocated();

                if (result != null)
                {
                    try
                    {
                        Litematica.logger.warn("uploadChunkBlocks() [Dispatch] upload results for layer [{}]", layer.getDrawMode().name());
                        //this.uploadSectionIndex(result, renderChunk.getBlocksVertexBufferByLayer(layer));
                        //renderCache.uploadSectionSortedIndex(result, renderCache.getVertexBufferByLayer(layer));
                        renderCache.uploadSectionSortedIndex(result, renderChunk.getBlocksVertexBufferByLayer(layer));
                    }
                    catch (Exception e)
                    {
                        Litematica.logger.fatal("uploadChunkBlocks: [Dispatch] failed to upload results for Translucent layer");
                        //throw new ArithmeticException("see logs");
                    }
                }
                else
                {
                    Litematica.logger.error("uploadChunkBlocks: [Dispatch] failed; result is NULL (layer: {})", layer.getDrawMode().name());
                }
            }

            try
            {
                Litematica.logger.warn("uploadChunkBlocks() [Dispatch] upload meshData for layer [{}]", layer.getDrawMode().name());
                //this.uploadSectionLayer(meshData, renderChunk.getBlocksVertexBufferByLayer(layer));
                //renderCache.uploadSectionMesh(meshData, renderCache.getVertexBufferByLayer(layer));
                renderCache.uploadSectionMesh(meshData, renderChunk.getBlocksVertexBufferByLayer(layer));
            }
            catch (Exception e)
            {
                Litematica.logger.fatal("uploadChunkBlocks: [Dispatch] failed to upload MeshData for layer [{}]", layer.getDrawMode().name());
                //throw new ArithmeticException("see logs");
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
                    ChunkRenderDispatcherLitematica.this.uploadChunkBlocks(layer, renderCache, renderChunk, chunkRenderData, distanceSq);
                }
            }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new ChunkRenderDispatcherLitematica.PendingUpload(futureTask, distanceSq));
                return futureTask;
            }
        }
    }

    public ListenableFuture<Object> uploadChunkOverlay(final OverlayRenderType type, final ChunkRenderCache renderCache, final ChunkRendererSchematicVbo renderChunk,
                                                       final ChunkRenderDataSchematic compiledChunk, final double distanceSq)
    {
        Litematica.logger.warn("uploadChunkOverlay() [Dispatch] for type [{}]", type.getDrawMode().name());

        if (MinecraftClient.getInstance().isOnThread())
        {
            //if (GuiBase.isCtrlDown()) System.out.printf("uploadChunkOverlay()\n");
            // FIXME
            //this.uploadSectionIndex(result, renderChunk.getOverlayVertexBuffer(type));
            Litematica.logger.warn("uploadChunkOverlay() [Dispatch] getMeshData for type [{}]", type.getDrawMode().name());
            BuiltBuffer meshData = renderCache.getBuiltBufferByOverlay(type);

            if (meshData == null)
            {
                Litematica.logger.warn("uploadChunkOverlay() [Dispatch] meshData = NULL for type [{}]", type.getDrawMode().name());
                return Futures.<Object>immediateFuture(null);
            }
            if (type.isTranslucent())
            {
                Litematica.logger.warn("uploadChunkOverlay() [Dispatch] get Allocator results for type [{}]", type.getDrawMode().name());
                BufferAllocator.CloseableBuffer result = renderCache.getAllocatorByOverlay(type).getAllocated();

                if (result != null)
                {
                    try
                    {
                        Litematica.logger.warn("uploadChunkOverlay() [Dispatch] upload results for type [{}]", type.getDrawMode().name());
                        //this.uploadSectionIndex(result, renderChunk.getOverlayVertexBuffer(type));
                        //renderCache.uploadSectionSortedIndex(result, renderCache.getVertexBufferByOverlayType(type));
                        renderCache.uploadSectionSortedIndex(result, renderChunk.getOverlayVertexBuffer(type));
                    }
                    catch (Exception e)
                    {
                        Litematica.logger.fatal("uploadChunkOverlay: [Dispatch] failed to upload results for Translucent type");
                        //throw new ArithmeticException("see logs");
                    }
                }
                else
                {
                    Litematica.logger.error("uploadChunkOverlay: [Dispatch] failed; result is NULL (type: {})", type.getDrawMode().name());
                }
            }

            try
            {
                Litematica.logger.warn("uploadChunkOverlay() [Dispatch] upload meshData for type [{}]", type.getDrawMode().name());
                //this.uploadSectionLayer(meshData, renderChunk.getOverlayVertexBuffer(type));
                //renderCache.uploadSectionMesh(meshData, renderCache.getVertexBufferByOverlayType(type));
                renderCache.uploadSectionMesh(meshData, renderChunk.getOverlayVertexBuffer(type));
            }
            catch (Exception e)
            {
                Litematica.logger.fatal("uploadChunkOverlay: [Dispatch] failed to upload MeshData for type [{}]", type.getDrawMode().name());
                //throw new ArithmeticException("see logs");
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
                    ChunkRenderDispatcherLitematica.this.uploadChunkOverlay(type, renderCache, renderChunk, compiledChunk, distanceSq);
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

    // TODO
    /*
    private void uploadSectionLayer(@Nonnull BuiltBuffer meshData, @Nonnull VertexBuffer vertexBuffer)
    {
        Litematica.logger.error("uploadSectionLayer() [Dispatch] start (buffer size: {}) // MeshDraw [{}]", vertexBuffer.getVertexFormat().getVertexSizeByte(), meshData.getDrawParameters().mode().name());

        if (vertexBuffer.isClosed())
        {
            Litematica.logger.error("uploadSectionLayer(): [Dispatch] error uploading MeshData (VertexBuffer is closed)");
            meshData.close();
            return;
        }

        vertexBuffer.bind();
        vertexBuffer.upload(meshData);
        VertexBuffer.unbind();
        Litematica.logger.error("uploadSectionLayer() [Dispatch] END");
    }

    private void uploadSectionIndex(@Nonnull BufferAllocator.CloseableBuffer result, @Nonnull VertexBuffer vertexBuffer)
    {
        Litematica.logger.error("uploadSectionIndex() [Dispatch] start (buffer size: {})", vertexBuffer.getVertexFormat().getVertexSizeByte());

        if (vertexBuffer.isClosed())
        {
            Litematica.logger.error("uploadSectionIndex(): [Dispatch] error uploading MeshData SortState (VertexBuffer is closed)");
            result.close();
        }

        vertexBuffer.bind();
        vertexBuffer.uploadIndexBuffer(result);
        VertexBuffer.unbind();
        Litematica.logger.error("uploadSectionIndex() [Dispatch] END");
    }
     */

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
        Litematica.logger.warn("stopWorkerThreads() [Dispatch]");

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

        this.queueFreeRenderCache.clear();
    }

    public boolean hasNoFreeRenderBuilders()
    {
        return this.queueFreeRenderCache.isEmpty();
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
