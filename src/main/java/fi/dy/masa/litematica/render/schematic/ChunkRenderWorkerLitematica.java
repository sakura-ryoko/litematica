package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.Entity;
import net.minecraft.util.crash.CrashReport;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;
import org.jetbrains.annotations.NotNull;

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

    protected void processTask(final ChunkRenderTaskSchematic task) throws InterruptedException
    {
        ChunkRenderTaskSchematic.Status oldStatus;
        oldStatus = task.casStatus(ChunkRenderTaskSchematic.Status.PENDING, ChunkRenderTaskSchematic.Status.COMPILING);
        if(oldStatus != ChunkRenderTaskSchematic.Status.PENDING)
            return;
        Entity entity = MinecraftClient.getInstance().getCameraEntity();
        if (entity == null) {
            task.finish();
            return;
        }

        if (!task.setRegionRenderCacheBuilder(this.getRegionRenderAllocatorCache()))
        {
            throw new InterruptedException("No free Allocator Cache found");
        }

        ChunkRenderTaskSchematic.Type taskType = task.getType();
        switch (task.getType()) {
            case REBUILD_CHUNK -> task.getRenderChunk().rebuildChunk(task);
            case RESORT_TRANSPARENCY -> task.getRenderChunk().resortTransparency(task);
        }

        oldStatus = task.casStatus(ChunkRenderTaskSchematic.Status.COMPILING, ChunkRenderTaskSchematic.Status.UPLOADING);
        if(oldStatus != ChunkRenderTaskSchematic.Status.COMPILING)
        {
            resetRenderAllocators(task);
            return;
        }

        final ChunkRenderDataSchematic chunkRenderData = task.getChunkRenderData();
        ArrayList<ListenableFuture<Object>> futuresList = Lists.newArrayList();
        ChunkRendererSchematicVbo renderChunk = task.getRenderChunk();
        BufferAllocatorCache allocators = task.getAllocatorCache();

        switch (taskType) {
            case REBUILD_CHUNK -> {
                for(RenderLayer layer: ChunkRenderLayers.LAYERS)
                    if (!chunkRenderData.isBlockLayerEmpty(layer))
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkBlocks(layer, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), false));
                for (OverlayRenderType type : ChunkRenderLayers.TYPES)
                    if (!chunkRenderData.isOverlayTypeEmpty(type))
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkOverlay(type, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), false));
            }
            case RESORT_TRANSPARENCY -> {
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
