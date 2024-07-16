package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.crash.CrashReport;
import fi.dy.masa.litematica.Litematica;

public class ChunkRenderWorkerLitematica implements Runnable
{
    private static final Logger LOGGER = Litematica.logger;

    private final ChunkRenderDispatcherLitematica chunkRenderDispatcher;
    final private BufferAllocatorCache allocatorCache;
    private boolean shouldRun;

    public ChunkRenderWorkerLitematica(ChunkRenderDispatcherLitematica chunkRenderDispatcherIn)
    {
        this(chunkRenderDispatcherIn, null);
    }

    public ChunkRenderWorkerLitematica(ChunkRenderDispatcherLitematica chunkRenderDispatcherIn, @Nullable BufferAllocatorCache allocatorCache)
    {
        this.shouldRun = true;
        this.chunkRenderDispatcher = chunkRenderDispatcherIn;
        this.allocatorCache = allocatorCache;
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

    /* Threaded Code
    protected void processTask(final ChunkRenderTaskSchematic task) throws InterruptedException
    {
        ChunkRenderTaskSchematic.Status oldStatus;
        oldStatus = task.casStatus(ChunkRenderTaskSchematic.Status.PENDING, ChunkRenderTaskSchematic.Status.COMPILING);

        if (oldStatus != ChunkRenderTaskSchematic.Status.PENDING)
        {
            return;
        }

        Entity entity = MinecraftClient.getInstance().getCameraEntity();

        if (entity == null)
        {
            task.finish();
            return;
        }

        if (!task.setRegionRenderCacheBuilder(this.getRegionRenderAllocatorCache()))
        {
            throw new InterruptedException("No free Allocator Cache found");
        }

        ChunkRenderTaskSchematic.Type taskType = task.getType();
        switch (task.getType())
        {
            case REBUILD_CHUNK -> task.getRenderChunk().rebuildChunk(task);
            case RESORT_TRANSPARENCY -> task.getRenderChunk().resortTransparency(task);
        }

        oldStatus = task.casStatus(ChunkRenderTaskSchematic.Status.COMPILING, ChunkRenderTaskSchematic.Status.UPLOADING);
        if (oldStatus != ChunkRenderTaskSchematic.Status.COMPILING)
        {
            resetRenderAllocators(task);
            return;
        }

        final ChunkRenderDataSchematic chunkRenderData = task.getChunkRenderData();
        ArrayList<ListenableFuture<Object>> futuresList = Lists.newArrayList();
        ChunkRendererSchematicVbo renderChunk = task.getRenderChunk();
        BufferAllocatorCache allocators = task.getAllocatorCache();

        switch (taskType)
        {
            case REBUILD_CHUNK ->
            {
                for(RenderLayer layer: ChunkRenderLayers.LAYERS)
                    if (!chunkRenderData.isBlockLayerEmpty(layer))
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkBlocks(layer, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), false));
                for (OverlayRenderType type : ChunkRenderLayers.TYPES)
                    if (!chunkRenderData.isOverlayTypeEmpty(type))
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkOverlay(type, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), false));
            }
            case RESORT_TRANSPARENCY ->
            {
                RenderLayer layer = RenderLayer.getTranslucent();
                if (!chunkRenderData.isBlockLayerEmpty(layer))
                    futuresList.add(this.chunkRenderDispatcher.uploadChunkBlocks(RenderLayer.getTranslucent(), allocators, renderChunk, chunkRenderData, task.getDistanceSq(), true));
                if (!chunkRenderData.isOverlayTypeEmpty(OverlayRenderType.QUAD))
                    futuresList.add(this.chunkRenderDispatcher.uploadChunkOverlay(OverlayRenderType.QUAD, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), true));
            }
        }

        final ListenableFuture<List<Object>> listenablefuture = Futures.allAsList(futuresList);

        task.addFinishRunnable(() -> listenablefuture.cancel(false));
        Futures.addCallback(listenablefuture, new FutureCallback<List<Object>>()
        {
            @Override
            public void onSuccess(@Nullable List<Object> list)
            {
                ChunkRenderWorkerLitematica.this.clearRenderAllocators(task);
                task.casStatus(ChunkRenderTaskSchematic.Status.UPLOADING,ChunkRenderTaskSchematic.Status.DONE);
                task.getRenderChunk().setChunkRenderData(chunkRenderData);
            }

            @Override
            public void onFailure(@NotNull Throwable throwable)
            {
                ChunkRenderWorkerLitematica.this.resetRenderAllocators(task);
                if (!(throwable instanceof CancellationException) && !(throwable instanceof InterruptedException))
                    MinecraftClient.getInstance().setCrashReportSupplier(CrashReport.create(throwable, "Rendering Litematica chunk"));
            }
        }, MoreExecutors.directExecutor());
    }
     */

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
            if (task.setRegionRenderCacheBuilder(this.getRegionRenderAllocatorCache()) == false)
            {
                throw new InterruptedException("No free Allocator Cache found");
            }

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

                    this.resetRenderAllocators(task);
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
            ChunkRendererSchematicVbo renderChunk = (ChunkRendererSchematicVbo) task.getRenderChunk();
            BufferAllocatorCache allocators = task.getAllocatorCache();

            if (taskType == ChunkRenderTaskSchematic.Type.REBUILD_CHUNK)
            {
                //if (GuiBase.isCtrlDown()) System.out.printf("pre uploadChunk()\n");
                for (RenderLayer layer : ChunkRenderLayers.LAYERS)
                {
                    if (chunkRenderData.isBlockLayerEmpty(layer) == false)
                    {
                        //if (GuiBase.isCtrlDown()) System.out.printf("REBUILD_CHUNK pre uploadChunkBlocks()\n");
                        //System.out.printf("REBUILD_CHUNK pre uploadChunkBlocks(%s)\n", layer.toString());
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkBlocks(layer, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), false));
                    }
                }

                for (OverlayRenderType type : ChunkRenderLayers.TYPES)
                {
                    if (chunkRenderData.isOverlayTypeEmpty(type) == false)
                    {
                        //if (GuiBase.isCtrlDown()) System.out.printf("REBUILD_CHUNK pre uploadChunkOverlay()\n");
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkOverlay(type, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), false));
                    }
                }
            }
            else if (taskType == ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY)
            {
                RenderLayer layer = RenderLayer.getTranslucent();

                if (chunkRenderData.isBlockLayerEmpty(layer) == false)
                {
                    //System.out.printf("RESORT_TRANSPARENCY pre uploadChunkBlocks(%s)\n", layer.toString());
                    futuresList.add(this.chunkRenderDispatcher.uploadChunkBlocks(RenderLayer.getTranslucent(), allocators, renderChunk, chunkRenderData, task.getDistanceSq(), true));
                }
                if (chunkRenderData.isOverlayTypeEmpty(OverlayRenderType.QUAD) == false)
                {
                    //if (GuiBase.isCtrlDown()) System.out.printf("RESORT_TRANSPARENCY pre uploadChunkOverlay()\n");
                    futuresList.add(this.chunkRenderDispatcher.uploadChunkOverlay(OverlayRenderType.QUAD, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), true));
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
                    ChunkRenderWorkerLitematica.this.clearRenderAllocators(task);

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
                    ChunkRenderWorkerLitematica.this.resetRenderAllocators(task);

                    if ((throwable instanceof CancellationException) == false && (throwable instanceof InterruptedException) == false)
                    {
                        MinecraftClient.getInstance().setCrashReportSupplier(CrashReport.create(throwable, "Rendering Litematica chunk"));
                    }
                }
            }, MoreExecutors.directExecutor());
        }
    }

    @Nullable
    private BufferAllocatorCache getRegionRenderAllocatorCache() throws InterruptedException
    {
        return this.allocatorCache != null ? this.allocatorCache : this.chunkRenderDispatcher.allocateRenderAllocators();
    }

    private void clearRenderAllocators(ChunkRenderTaskSchematic generator)
    {
        BufferAllocatorCache bufferAllocatorCache = generator.getAllocatorCache();
        bufferAllocatorCache.clearAll();

        if (this.allocatorCache == null)
        {
            this.chunkRenderDispatcher.freeRenderAllocators(bufferAllocatorCache);
        }
    }

    private void resetRenderAllocators(ChunkRenderTaskSchematic generator)
    {
        BufferAllocatorCache bufferAllocatorCache = generator.getAllocatorCache();
        bufferAllocatorCache.resetAll();

        if (this.allocatorCache == null)
        {
            this.chunkRenderDispatcher.freeRenderAllocators(bufferAllocatorCache);
        }
    }

    public void notifyToStop()
    {
        this.shouldRun = false;
    }
}
