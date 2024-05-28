package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import javax.annotation.Nullable;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.Entity;
import net.minecraft.util.crash.CrashReport;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.cache.BufferAllocatorCache;
import fi.dy.masa.litematica.render.cache.BufferBuilderCache;
import fi.dy.masa.litematica.render.cache.BuiltBufferCache;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class ChunkRenderWorkerLitematica implements Runnable
{
    private static final Logger LOGGER = Litematica.logger;

    private final ChunkRenderDispatcherLitematica chunkRenderDispatcher;
    private final BufferAllocatorCache allocatorCache;
    private final BufferBuilderCache bufferCache;
    private final BuiltBufferCache builtBufferCache;
    private boolean shouldRun;

    public ChunkRenderWorkerLitematica(ChunkRenderDispatcherLitematica chunkRenderDispatcherIn)
    {
        this(chunkRenderDispatcherIn, null, null, null);
    }

    public ChunkRenderWorkerLitematica(ChunkRenderDispatcherLitematica chunkRenderDispatcherIn, @Nullable BufferAllocatorCache allocatorCache, @Nullable BufferBuilderCache bufferCache, @Nullable BuiltBufferCache builtBufferCache)
    {
        this.shouldRun = true;
        this.chunkRenderDispatcher = chunkRenderDispatcherIn;
        this.allocatorCache = allocatorCache;
        this.bufferCache = bufferCache;
        this.builtBufferCache = builtBufferCache;
    }

    @Override
    public void run()
    {
        while (this.shouldRun)
        {
            try
            {
                this.processTask(this.chunkRenderDispatcher.getNextChunkUpdate());
            }
            catch (InterruptedException e)
            {
                LOGGER.debug("Stopping chunk worker due to interrupt");
                return;
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.create(throwable, "Batching chunks");
                MinecraftClient.getInstance().setCrashReportSupplier(MinecraftClient.getInstance().addDetailsToCrashReport(crashreport));
                return;
            }
        }
    }

    protected void processTask(final ChunkRenderTaskSchematic task) throws InterruptedException
    {
        task.getLock().lock();

        try
        {
            if (task.getStatus() != ChunkRenderTaskSchematic.Status.PENDING)
            {
                if (task.isFinished() == false)
                {
                    LOGGER.warn("Chunk render task was {} when I expected it to be pending; ignoring task", (Object) task.getStatus());
                }

                return;
            }

            task.setStatus(ChunkRenderTaskSchematic.Status.COMPILING);
        }
        finally
        {
            task.getLock().unlock();
        }

        Entity entity = MinecraftClient.getInstance().getCameraEntity();

        if (entity == null)
        {
            task.finish();
        }
        else
        {
            task.setRegionRenderCacheBuilder(this.getRegionRenderAllocatorCache(), this.getRegionRenderCacheBuilder(), this.getRegionRenderBuiltBuffers());

            ChunkRenderTaskSchematic.Type taskType = task.getType();

            if (taskType == ChunkRenderTaskSchematic.Type.REBUILD_CHUNK)
            {
                task.getRenderChunk().rebuildChunk(task);
            }
            else if (taskType == ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY)
            {
                task.getRenderChunk().resortTransparency(task);
            }

            task.getLock().lock();

            try
            {
                if (task.getStatus() != ChunkRenderTaskSchematic.Status.COMPILING)
                {
                    if (task.isFinished() == false)
                    {
                        LOGGER.warn("Chunk render task was {} when I expected it to be compiling; aborting task", (Object) task.getStatus());
                    }

                    this.freeRenderAllocators(task);
                    this.freeRenderBuilder(task);
                    this.freeRenderBuiltCache(task);
                    return;
                }

                task.setStatus(ChunkRenderTaskSchematic.Status.UPLOADING);
            }
            finally
            {
                task.getLock().unlock();
            }

            final ChunkRenderDataSchematic chunkRenderData = (ChunkRenderDataSchematic) task.getChunkRenderData();
            ArrayList<ListenableFuture<Object>> futuresList = Lists.newArrayList();
            BufferAllocatorCache allocators = task.getAllocatorCache();
            BufferBuilderCache buffers = task.getBufferCache();
            BuiltBufferCache builtBuffers = task.getBuiltBufferCache();
            ChunkRendererSchematicVbo renderChunk = (ChunkRendererSchematicVbo) task.getRenderChunk();

            if (taskType == ChunkRenderTaskSchematic.Type.REBUILD_CHUNK)
            {
                //if (GuiBase.isCtrlDown()) System.out.printf("pre uploadChunk()\n");
                for (RenderLayer layer : ChunkRenderLayers.LAYERS)
                {
                    if (chunkRenderData.isBlockLayerEmpty(layer) == false)
                    {
                        //if (GuiBase.isCtrlDown()) System.out.printf("REBUILD_CHUNK pre uploadChunkBlocks()\n");
                        //System.out.printf("REBUILD_CHUNK pre uploadChunkBlocks(%s)\n", layer.toString());
                        //BufferAllocator allocator = allocators.getBufferByLayer(layer);
                        //BufferBuilder buffer = buffers.getBufferByLayer(layer);
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkBlocks(layer, allocators, buffers, builtBuffers, renderChunk, chunkRenderData, task.getDistanceSq()));
                    }
                }

                for (OverlayRenderType type : ChunkRenderLayers.TYPES)
                {
                    if (chunkRenderData.isOverlayTypeEmpty(type) == false)
                    {
                        //if (GuiBase.isCtrlDown()) System.out.printf("REBUILD_CHUNK pre uploadChunkOverlay()\n");
                        //BufferAllocator allocator = allocators.getBufferByOverlay(type);
                        //BufferBuilder buffer = buffers.getBufferByOverlay(type);
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkOverlay(type, allocators, buffers, builtBuffers, renderChunk, chunkRenderData, task.getDistanceSq()));
                    }
                }
            }
            else if (taskType == ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY)
            {
                RenderLayer layer = RenderLayer.getTranslucent();

                if (chunkRenderData.isBlockLayerEmpty(layer) == false)
                {
                    //System.out.printf("RESORT_TRANSPARENCY pre uploadChunkBlocks(%s)\n", layer.toString());
                    //BufferAllocator allocator = allocators.getBufferByLayer(layer);
                    //BufferBuilder buffer = buffers.getBufferByLayer(layer);
                    futuresList.add(this.chunkRenderDispatcher.uploadChunkBlocks(RenderLayer.getTranslucent(), allocators, buffers, builtBuffers, renderChunk, chunkRenderData, task.getDistanceSq()));
                }

                if (chunkRenderData.isOverlayTypeEmpty(OverlayRenderType.QUAD) == false)
                {
                    //if (GuiBase.isCtrlDown()) System.out.printf("RESORT_TRANSPARENCY pre uploadChunkOverlay()\n");
                    //BufferAllocator allocator = allocators.getBufferByOverlay(OverlayRenderType.QUAD);
                    //BufferBuilder buffer = buffers.getBufferByOverlay(OverlayRenderType.QUAD);
                    futuresList.add(this.chunkRenderDispatcher.uploadChunkOverlay(OverlayRenderType.QUAD, allocators, buffers, builtBuffers, renderChunk, chunkRenderData, task.getDistanceSq()));
                }
            }

            final ListenableFuture<List<Object>> listenablefuture = Futures.allAsList(futuresList);

            task.addFinishRunnable(new Runnable()
            {
                @Override
                public void run()
                {
                    listenablefuture.cancel(false);
                }
            });

            Futures.addCallback(listenablefuture, new FutureCallback<List<Object>>()
            {
                @Override
                public void onSuccess(@Nullable List<Object> list)
                {
                    ChunkRenderWorkerLitematica.this.freeRenderAllocators(task);
                    ChunkRenderWorkerLitematica.this.freeRenderBuilder(task);
                    ChunkRenderWorkerLitematica.this.freeRenderBuiltCache(task);

                    task.getLock().lock();

                    label49:
                    {
                        try
                        {
                            if (task.getStatus() == ChunkRenderTaskSchematic.Status.UPLOADING)
                            {
                                task.setStatus(ChunkRenderTaskSchematic.Status.DONE);
                                break label49;
                            }

                            if (task.isFinished() == false)
                            {
                                ChunkRenderWorkerLitematica.LOGGER.warn("Chunk render task was {} when I expected it to be uploading; aborting task", (Object)task.getStatus());
                            }
                        }
                        finally
                        {
                            task.getLock().unlock();
                        }

                        return;
                    }

                    task.getRenderChunk().setChunkRenderData(chunkRenderData);
                }

                @Override
                public void onFailure(Throwable throwable)
                {
                    ChunkRenderWorkerLitematica.this.freeRenderAllocators(task);
                    ChunkRenderWorkerLitematica.this.freeRenderBuilder(task);
                    ChunkRenderWorkerLitematica.this.freeRenderBuiltCache(task);

                    if ((throwable instanceof CancellationException) == false && (throwable instanceof InterruptedException) == false)
                    {
                        MinecraftClient.getInstance().setCrashReportSupplier(CrashReport.create(throwable, "Rendering Litematica chunk"));
                    }
                }
            }, MoreExecutors.directExecutor());
        }
    }

    private BufferAllocatorCache getRegionRenderAllocatorCache() throws InterruptedException
    {
        return this.allocatorCache != null ? this.allocatorCache : this.chunkRenderDispatcher.allocateRenderAllocators();
    }

    private BufferBuilderCache getRegionRenderCacheBuilder() throws InterruptedException
    {
        return this.bufferCache != null ? this.bufferCache : this.chunkRenderDispatcher.allocateRenderBuilder();
    }

    private BuiltBufferCache getRegionRenderBuiltBuffers() throws InterruptedException
    {
        return this.builtBufferCache != null ? this.builtBufferCache : this.chunkRenderDispatcher.allocateRenderBuiltCache();
    }

    private void freeRenderAllocators(ChunkRenderTaskSchematic generator)
    {
        BufferAllocatorCache bufferAllocatorCache = generator.getAllocatorCache();
        bufferAllocatorCache.clear();

        if (this.bufferCache == null)
        {
            this.chunkRenderDispatcher.freeRenderAllocators(bufferAllocatorCache);
        }
    }

    private void freeRenderBuilder(ChunkRenderTaskSchematic generator)
    {
        BufferBuilderCache builderCache = generator.getBufferCache();
        builderCache.clear();

        if (this.bufferCache == null)
        {
            this.chunkRenderDispatcher.freeRenderBuilder(builderCache);
        }
    }

    private void freeRenderBuiltCache(ChunkRenderTaskSchematic generator)
    {
        BuiltBufferCache builtBufferCache = generator.getBuiltBufferCache();
        builtBufferCache.clear();

        if (this.builtBufferCache == null)
        {
            this.chunkRenderDispatcher.freeRenderBuiltCache(builtBufferCache);
        }
    }

    public void notifyToStop()
    {
        this.shouldRun = false;
    }
}
