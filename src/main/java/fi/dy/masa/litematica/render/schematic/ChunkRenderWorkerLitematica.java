package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.litematica.Litematica;

public class ChunkRenderWorkerLitematica implements Runnable
{
    private static final Logger LOGGER = Litematica.LOGGER;

    private final ChunkRenderDispatcherLitematica chunkRenderDispatcher;
    final private BufferAllocatorCache allocatorCache;
    private boolean shouldRun;
    private ProfilerFiller profiler;

    public ChunkRenderWorkerLitematica(ChunkRenderDispatcherLitematica chunkRenderDispatcherIn, ProfilerFiller profiler)
    {
        this(chunkRenderDispatcherIn, null, profiler);
    }

    public ChunkRenderWorkerLitematica(ChunkRenderDispatcherLitematica chunkRenderDispatcherIn, @Nullable BufferAllocatorCache allocatorCache, ProfilerFiller profiler)
    {
        this.shouldRun = true;
        this.chunkRenderDispatcher = chunkRenderDispatcherIn;
        this.allocatorCache = allocatorCache;
        this.profiler = profiler;

        //LOGGER.error("[LW] init() [Cache: {}]", allocatorCache != null);
    }

    @Override
    public void run()
    {
        //LOGGER.warn("[LW] run()");

        if (this.profiler == null)
        {
            this.profiler = Profiler.get();
        }

        while (this.shouldRun)
        {
            try
            {
                this.processTask(this.chunkRenderDispatcher.getNextChunkUpdate(), this.profiler);
            }
            catch (InterruptedException e)
            {
                LOGGER.debug("Stopping chunk worker due to interrupt");
                return;
            }
            catch (Throwable throwable)
            {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Batching chunks");
                Minecraft.getInstance().delayCrashRaw(Minecraft.getInstance().fillReport(crashreport));
                return;
            }
        }
    }

    protected void processTask(final ChunkRenderTaskSchematic task, ProfilerFiller profiler) throws InterruptedException
    {
        profiler.push("process_task");
        task.getLock().lock();

        //LOGGER.warn("[LW] processTask() task [{}] / [{}]", task.getType().name(), task.getStatus().name());
        try
        {
            if (task.getStatus() != ChunkRenderTaskSchematic.Status.PENDING)
            {
                if (!task.isFinished())
                {
                    LOGGER.warn("Chunk render task was {} when I expected it to be pending; ignoring task", (Object) task.getStatus());
                }

                profiler.pop();
                return;
            }

            task.setStatus(ChunkRenderTaskSchematic.Status.COMPILING);
        }
        finally
        {
            task.getLock().unlock();
        }

        Entity entity = Minecraft.getInstance().getCameraEntity();

        if (entity == null)
        {
            task.finish();
        }
        else
        {
            if (!task.setRegionRenderCacheBuilder(this.getRegionRenderAllocatorCache()))
            {
                profiler.pop();
                throw new InterruptedException("No free Allocator Cache found");
            }

            ChunkRenderTaskSchematic.Type taskType = task.getType();

            profiler.popPush("run_task_now_" + taskType.name());
            if (taskType == ChunkRenderTaskSchematic.Type.REBUILD_CHUNK)
            {
                //LOGGER.warn("[LW] (REBUILD_CHUNK) --> [VBO]");
                task.getRenderChunk().rebuildChunk(task, profiler);
            }
//            else if (taskType == ChunkRenderTaskSchematic.Type.UPLOAD_CHUNK)
//            {
//                LOGGER.warn("[LW] (UPLOAD_CHUNK) --> [VBO]");
//                task.getRenderChunk().uploadChunk(task, profiler);
//            }
            else if (taskType == ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY)
            {
                //LOGGER.warn("[LW] (RESORT_TRANSPARENCY) --> [VBO]");
                task.getRenderChunk().resortTransparency(task, profiler);
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
                    profiler.pop();
                    return;
                }

                task.setStatus(ChunkRenderTaskSchematic.Status.UPLOADING);
            }
            finally
            {
                task.getLock().unlock();
            }

            profiler.popPush("run_task_schedule_"+ taskType.name());
            final ChunkRenderDataSchematic chunkRenderData = task.getChunkRenderData();
            ArrayList<ListenableFuture<Object>> futuresList = Lists.newArrayList();
            ChunkRendererSchematicVbo renderChunk = task.getRenderChunk();
            BufferAllocatorCache allocators = task.getAllocatorCache();

            if (taskType == ChunkRenderTaskSchematic.Type.REBUILD_CHUNK)
            {
                //LOGGER.warn("[LW] (REBUILD_CHUNK) --> Run Uploads");

                //if (GuiBase.isCtrlDown()) System.out.printf("pre uploadChunk()\n");
                for (ChunkSectionLayer layer : ChunkRenderLayers.BLOCK_RENDER_LAYERS)
                {
                    if (chunkRenderData.isBlockLayerEmpty(layer) == false)
                    {
                        //if (GuiBase.isCtrlDown()) System.out.printf("REBUILD_CHUNK pre uploadChunkBlocks()\n");
                        //LOGGER.warn("[LW] REBUILD_CHUNK pre uploadChunkBlocks({})", layer.label());
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkBlocks(layer, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), false, profiler));
                    }
                }

                for (OverlayRenderType type : ChunkRenderLayers.TYPES)
                {
                    if (chunkRenderData.isOverlayTypeEmpty(type) == false)
                    {
                        //if (GuiBase.isCtrlDown()) System.out.printf("REBUILD_CHUNK pre uploadChunkOverlay()\n");
                        //LOGGER.warn("[LW] REBUILD_CHUNK pre uploadChunkOverlay({})", type.name());
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkOverlay(type, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), false, profiler));
                    }
                }
            }
            else if (taskType == ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY)
            {
                //LOGGER.warn("[LW] (RESORT_TRANSPARENCY) --> Schedule Uploads");
                ChunkSectionLayer layer = ChunkSectionLayer.TRANSLUCENT;

                if (chunkRenderData.isBlockLayerEmpty(layer) == false)
                {
                    //System.out.printf("RESORT_TRANSPARENCY pre uploadChunkBlocks(%s)\n", layer.toString());
                    //LOGGER.warn("[LW] RESORT_TRANSPARENCY pre uploadChunkBlocks({})", layer.label());
                    futuresList.add(this.chunkRenderDispatcher.uploadChunkBlocks(layer, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), true, profiler));
                }

                if (chunkRenderData.isOverlayTypeEmpty(OverlayRenderType.QUAD) == false)
                {
                    //if (GuiBase.isCtrlDown()) System.out.printf("RESORT_TRANSPARENCY pre uploadChunkOverlay()\n");
                    //LOGGER.warn("[LW] RESORT_TRANSPARENCY pre uploadChunkOverlay({})", OverlayRenderType.QUAD.name());
                    futuresList.add(this.chunkRenderDispatcher.uploadChunkOverlay(OverlayRenderType.QUAD, allocators, renderChunk, chunkRenderData, task.getDistanceSq(), true, profiler));
                }
            }

            profiler.popPush("run_task_later_" + taskType.name());

            //LOGGER.warn("[LW] (TASK COMBINE) --> futuresList size [{}]", futuresList.size());

            final ListenableFuture<List<Object>> listenablefuture = Futures.allAsList(futuresList);

            task.addFinishRunnable(new Runnable()
            {
                @Override
                public void run()
                {
                    listenablefuture.cancel(false);
                }
            });

            Futures.addCallback(listenablefuture, new FutureCallback<>()
            {
                public void onSuccess(@Nullable List<Object> list)
                {
//                    ChunkRenderWorkerLitematica.this.clearRenderAllocators(task);
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
                                LOGGER.warn("Chunk render task was {} when I expected it to be uploading; aborting task", (Object) task.getStatus());
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
                public void onFailure(@NotNull Throwable throwable)
                {
                    ChunkRenderWorkerLitematica.this.resetRenderAllocators(task);

                    if ((throwable instanceof CancellationException) == false && (throwable instanceof InterruptedException) == false)
                    {
                        Minecraft.getInstance().delayCrashRaw(CrashReport.forThrowable(throwable, "Rendering Litematica chunk"));
                    }
                }
            }, MoreExecutors.directExecutor());
        }

        profiler.pop();
    }

    @Nullable
    private BufferAllocatorCache getRegionRenderAllocatorCache() throws InterruptedException
    {
        return this.allocatorCache != null ? this.allocatorCache : this.chunkRenderDispatcher.allocateRenderAllocators();
    }

    // todo -- check
    private void clearRenderAllocators(ChunkRenderTaskSchematic generator)
    {
        BufferAllocatorCache bufferAllocatorCache = generator.getAllocatorCache();

        if (bufferAllocatorCache != null && !bufferAllocatorCache.isClear())
        {
            bufferAllocatorCache.clearAll();
        }

        if (this.allocatorCache == null)
        {
            this.chunkRenderDispatcher.freeRenderAllocators(bufferAllocatorCache);
        }
    }

    private void resetRenderAllocators(ChunkRenderTaskSchematic generator)
    {
        BufferAllocatorCache bufferAllocatorCache = generator.getAllocatorCache();

        if (bufferAllocatorCache != null && !bufferAllocatorCache.isClear())
        {
            bufferAllocatorCache.resetAll();
        }

        if (this.allocatorCache == null)
        {
            this.chunkRenderDispatcher.freeRenderAllocators(bufferAllocatorCache);
        }
    }

    public void notifyToStop()
    {
        //LOGGER.warn("[LW] stop()");
        this.shouldRun = false;
    }
}
