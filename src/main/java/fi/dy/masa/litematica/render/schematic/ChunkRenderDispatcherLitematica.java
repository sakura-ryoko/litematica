package fi.dy.masa.litematica.render.schematic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.phys.Vec3;

import java.util.Queue;
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

    private final PriorityBlockingQueue<ChunkRenderTaskSchematic> queueChunkUpdates;
    private final Queue<PendingUpload> queueChunkUploads;
    private final Queue<ChunkRenderDispatcherBuffers> freeBufferPacks;
    private final ChunkRenderWorkerLitematica renderWorker;
    private Vec3 cameraPos;

    public ChunkRenderDispatcherLitematica()
    {
        this.queueChunkUpdates = Queues.newPriorityBlockingQueue();
        this.queueChunkUploads = Queues.newPriorityQueue();
        this.freeBufferPacks = Queues.newConcurrentLinkedQueue();
        this.cameraPos = Vec3.ZERO;
        this.renderWorker = new ChunkRenderWorkerLitematica(this);
    }

    protected ChunkRenderDispatcherBuffers allocateBufferPack()
    {
        ChunkRenderDispatcherBuffers pack = this.freeBufferPacks.poll();
        return pack != null ? pack : new ChunkRenderDispatcherBuffers();
    }

    protected void freeBufferPack(ChunkRenderDispatcherBuffers pack)
    {
        pack.reset();
        this.freeBufferPacks.offer(pack);
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
        return String.format("pC: %03d, aB: %1d", this.queueChunkUpdates.size(), this.freeBufferPacks.size());
    }

    protected boolean runChunkUploads(long finishTimeNano)
    {
        boolean ranTasks = false;
        //LOGGER.warn("[Dispatch] runChunkUploads() - queueChunkUpdates({}) // queueChunkUploads({})", this.queueChunkUpdates.size(), this.queueChunkUploads.size());
//        profiler.push("run_chunk_uploads");

        while (true)
        {
            boolean processedTask = false;

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

            synchronized (this.queueChunkUploads)
            {
                if (!this.queueChunkUploads.isEmpty())
                {
//                    (this.queueChunkUploads.poll()).uploadTask.run();
                    PendingUpload upload = this.queueChunkUploads.poll();

                    if (upload != null)
                    {
                        upload.uploadTask.run();
                        processedTask = true;
                    }

                    ranTasks = true;
                }
            }

            if (finishTimeNano == 0L || processedTask == false || finishTimeNano < System.nanoTime())
            {
                break;
            }
        }

//        profiler.pop();
        return ranTasks;
    }
    protected boolean updateChunkLater(ChunkRendererSchematicVbo renderChunk)
    {
        //LOGGER.warn("[Dispatch] updateChunkLater()");
//        profiler.push("update_chunk_later");

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

//        profiler.pop();
        return flag1;
    }

    protected boolean updateChunkNow(ChunkRendererSchematicVbo chunkRenderer)
    {
        //LOGGER.warn("[Dispatch] updateChunkNow()");
//        profiler.push("update_chunk_now");

        chunkRenderer.getLockCompileTask().lock();
        boolean flag;

        try
        {
            ChunkRenderTaskSchematic generator = chunkRenderer.makeCompileTaskChunkSchematic(this::getCameraPos);

            try
            {
                this.renderWorker.processTask(generator);
            }
            catch (InterruptedException ignored) { }

            flag = true;
        }
        finally
        {
            chunkRenderer.getLockCompileTask().unlock();
        }

//        profiler.pop();
        return flag;
    }

    protected void stopChunkUpdates()
    {
        //LOGGER.warn("[Dispatch] stopChunkUpdates()");
//        profiler.push("stop_chunk_updates");
        this.clearChunkUpdates();
        this.runChunkUploads(Long.MAX_VALUE);
//        profiler.pop();
    }

    protected ChunkRenderTaskSchematic getNextChunkUpdate() throws InterruptedException
    {
        return this.queueChunkUpdates.take();
    }

    protected boolean updateTransparencyLater(ChunkRendererSchematicVbo renderChunk)
    {
//        profiler.push("update_transparency_later");

        renderChunk.getLockCompileTask().lock();
        boolean flag;

        try
        {
            final ChunkRenderTaskSchematic generator = renderChunk.makeCompileTaskTransparencySchematic(this::getCameraPos);

            if (generator == null)
            {
                flag = true;
//                profiler.pop();
                return flag;
            }

            generator.addFinishRunnable(() -> ChunkRenderDispatcherLitematica.this.queueChunkUpdates.remove(generator));

            flag = this.queueChunkUpdates.offer(generator);
        }
        finally
        {
            renderChunk.getLockCompileTask().unlock();
        }

//        profiler.pop();
        return flag;
    }

    protected ListenableFuture<Object> uploadChunkBlocks(final ChunkSectionLayer layer, final ChunkRenderDataSchematic compiledChunk,
                                                         ChunkRenderDispatcherBuffers pack, ChunkRenderGpuUploader uploader,
                                                         final double distanceSq, boolean resortOnly)
    {
        //LOGGER.warn("[Dispatch] uploadChunkBlocks layer [{}]", layer.name());
//        profiler.push("upload_chunk_blocks");

        if (Minecraft.getInstance().isSameThread())
        {
            try
            {
                this.uploadVertexBufferByBlockLayer(layer, compiledChunk, pack, uploader, uploader.createVertexSorter(this.getCameraPos(), uploader.origin()), resortOnly);
            }
            catch (Exception e)
            {
                LOGGER.warn("uploadChunkBlocks(): [Dispatch] Error uploading Vertex Buffer for layer [{}], Caught error: [{}]", layer.label(), e.toString());
            }

//            profiler.pop();
            return Futures.immediateFuture(null);
        }
        else
        {
//            profiler.popPush("upload_chunk_blocks_later");

            /*  Threaded Code

            ListenableFutureTask<Object> futureTask = ListenableFutureTask.create(
                    () -> uploadChunkBlocks(layer, allocators, renderChunk, chunkRenderData, distanceSq, resortOnly),
                    null);
             */

            ListenableFutureTask<Object> futureTask = ListenableFutureTask.create(() ->
                                                                                  {
                                                                                      try
                                                                                      {
                                                                                          this.uploadVertexBufferByBlockLayer(layer, compiledChunk, pack, uploader, uploader.createVertexSorter(this.getCameraPos(), uploader.origin()), resortOnly);
                                                                                      }
                                                                                      catch (Exception e)
                                                                                      {
                                                                                          LOGGER.error("uploadChunkBlocks() -> [Dispatch] uploadVertexBufferByBlockLayer: Exception: {}", e.getLocalizedMessage());
                                                                                      }
                                                                                  }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new PendingUpload(futureTask, distanceSq));
//                profiler.pop();
                return futureTask;
            }
        }
    }

    protected ListenableFuture<Object> uploadChunkOverlay(final OverlayRenderType type, final ChunkRenderDataSchematic compiledChunk,
                                                          ChunkRenderDispatcherBuffers pack, ChunkRenderGpuUploader uploader,
                                                          final double distanceSq, boolean resortOnly)
    {
        //LOGGER.warn("[Dispatch] uploadChunkOverlay type [{}]", type.name());
//        profiler.push("upload_chunk_overlay");

        if (Minecraft.getInstance().isSameThread())
        {
            try
            {
                this.uploadVertexBufferByType(type, compiledChunk, pack, uploader, uploader.createVertexSorter(this.getCameraPos(), uploader.origin()), resortOnly);
            }
            catch (Exception e)
            {
                // TODO --> This one will throw if it's not sorted as Translucent,
                //  but it will cause a crash during draw() --> Ignored
                LOGGER.error("uploadChunkOverlay(): [Dispatch] Error uploading Vertex Buffer for overlay type [{}], Caught error: [{}]", type.name(), e.toString());
            }

//            profiler.pop();
            return Futures.immediateFuture(null);
        }
        else
        {
//            profiler.popPush("upload_chunk_overlay_later");
            ListenableFutureTask<Object> futureTask = ListenableFutureTask.<Object>create(() ->
                                                                                          {
                                                                                              try
                                                                                              {
                                                                                                  this.uploadVertexBufferByType(type, compiledChunk, pack, uploader, uploader.createVertexSorter(this.getCameraPos(), uploader.origin()), resortOnly);
                                                                                              }
                                                                                              catch (Exception e)
                                                                                              {
                                                                                                  LOGGER.error("uploadChunkOverlay() -> [Dispatch] uploadVertexBufferByType: Exception: {}", e.getLocalizedMessage());;
                                                                                              }
                                                                                          }, null);

            synchronized (this.queueChunkUploads)
            {
                this.queueChunkUploads.add(new PendingUpload(futureTask, distanceSq));
//                profiler.pop();
                return futureTask;
            }
        }
    }

    private void uploadVertexBufferByBlockLayer(final ChunkSectionLayer layer, final ChunkRenderDataSchematic compiledChunk,
                                                ChunkRenderDispatcherBuffers pack, ChunkRenderGpuUploader uploader,
                                                final VertexSorting sorter, boolean resortOnly)
            throws InterruptedException
    {
        //LOGGER.warn("[Dispatch] uploadVertexBufferByBlockLayer layer [{}]", layer.label());
//        profiler.push("upload_vbo_layer_"+layer.label());

        ByteBufferBuilder allocator = pack.allocatorCache().getAllocator(layer);
        final ChunkMeshDataSchematic chunkMeshData = compiledChunk.getMeshDataCache();
        MeshData builtBuffer = chunkMeshData.getMeshDataOrNull(layer);
        boolean useResorting = Configs.Visuals.RENDER_ENABLE_TRANSLUCENT_RESORTING.getBooleanValue();

        if (allocator == null)
        {
            //LOGGER.error("[Dispatch] uploadVertexBufferByBlockLayer layer [{}] --> ALLOC NULL", layer.label());
            pack.allocatorCache().closeByBlockLayer(layer);
            compiledChunk.setBlockLayerUnused(layer);
//            profiler.pop();
            throw new InterruptedException("BufferAllocators are invalid");
        }

        if (builtBuffer == null)
        {
            //LOGGER.error("[Dispatch] uploadVertexBufferByBlockLayer layer [{}] --> MESHDATA NULL", layer.label());
            compiledChunk.setBlockLayerUnused(layer);
//            profiler.pop();
            return;
        }

        if (resortOnly == false)
        {
            uploader.uploadBuffersByLayer(layer, builtBuffer, useResorting);
        }

        if (layer == ChunkSectionLayer.TRANSLUCENT && useResorting)
        {
            MeshData.SortState sorting = chunkMeshData.getTransparentSortingDataForBlockLayer(layer);

            if (sorting == null)
            {
                sorting = builtBuffer.sortQuads(allocator, sorter);

                if (sorting == null)
                {
//                    profiler.pop();
                    //LOGGER.error("[Dispatch] uploadVertexBufferByBlockLayer layer [{}] --> SORT FAILURE", layer.label());
                    throw new InterruptedException("Sort State failed to sortQuads()");
                }

                chunkMeshData.setTransparentSortingDataForBlockLayer(layer, sorting);
            }

            ByteBufferBuilder.Result result = sorting.buildSortedIndexBuffer(allocator, sorter);

            if (result != null)
            {
                //LOGGER.warn("[Dispatch] uploadVertexBufferByBlockLayer layer [{}] --> UPLOAD INDEX", layer.label());
                uploader.uploadIndexByBlockLayer(layer, result);
                result.close();
            }
        }

        //LOGGER.warn("[Dispatch] uploadVertexBufferByBlockLayer layer [{}] --> DONE", layer.label());
//        profiler.pop();
    }

    private void uploadVertexBufferByType(final OverlayRenderType type, final ChunkRenderDataSchematic compiledChunk,
                                          ChunkRenderDispatcherBuffers pack, ChunkRenderGpuUploader uploader,
                                          final VertexSorting sorter, boolean resortOnly)
            throws InterruptedException
    {
        //LOGGER.warn("[Dispatch] uploadVertexBufferByType type [{}]", type.name());
//        profiler.push("upload_vbo_overlay_"+type.name());

        ByteBufferBuilder allocator = pack.allocatorCache().getAllocator(type);
        final ChunkMeshDataSchematic chunkMeshData = compiledChunk.getMeshDataCache();
        MeshData builtBuffer = chunkMeshData.getMeshDataOrNull(type);
        boolean useResorting = false;

        if (allocator == null)
        {
            //LOGGER.error("[Dispatch] uploadVertexBufferByType type [{}] --> ALLOC NULL", type.name());
            pack.allocatorCache().closeByType(type);
            compiledChunk.setOverlayTypeUnused(type);
//            profiler.pop();
            throw new InterruptedException("BufferAllocators are invalid");
        }

        if (builtBuffer == null)
        {
            //LOGGER.error("[Dispatch] uploadVertexBufferByType type [{}] --> MESHDATA NULL", type.name());
            compiledChunk.setOverlayTypeUnused(type);
//            profiler.pop();
            return;
        }

        if (resortOnly == false)
        {
            //LOGGER.warn("[Dispatch] uploadVertexBufferByType type [{}] --> UPLOAD VERTEX", type.name());
            uploader.uploadBuffersByType(type, builtBuffer, useResorting);
        }

        if (type.isTranslucent() && useResorting)
        {
            MeshData.SortState sorting = chunkMeshData.getTransparentSortingDataForOverlay(type);

            if (sorting == null)
            {
                sorting = builtBuffer.sortQuads(allocator, sorter);

                if (sorting == null)
                {
//                    profiler.pop();
                    //LOGGER.warn("[Dispatch] uploadVertexBufferByType type [{}] --> SORT FAILURE", type.name());
                    throw new InterruptedException("Sort State failed to sortQuads()");
                }

                chunkMeshData.setTransparentSortingDataForOverlay(type, sorting);
            }

            ByteBufferBuilder.Result result = sorting.buildSortedIndexBuffer(allocator, sorter);

            if (result != null)
            {
                //LOGGER.warn("[Dispatch] uploadVertexBufferByType type [{}] --> UPLOAD INDEX", type.name());
                uploader.uploadIndexByType(type, result);
                result.close();
            }
        }

        //LOGGER.warn("[Dispatch] uploadVertexBufferByType type [{}] --> DONE", type.name());
//        profiler.pop();
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
