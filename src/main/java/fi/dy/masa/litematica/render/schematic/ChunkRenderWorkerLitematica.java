package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class ChunkRenderWorkerLitematica
{
    private static final Logger LOGGER = Litematica.LOGGER;

    private final ChunkRenderDispatcherLitematica chunkRenderDispatcher;

    public ChunkRenderWorkerLitematica(ChunkRenderDispatcherLitematica chunkRenderDispatcherIn)
    {
        this.chunkRenderDispatcher = chunkRenderDispatcherIn;
        //LOGGER.error("[LW] init() [Cache: {}]", allocatorCache != null);
    }

    protected void processTask(final ChunkRenderTaskSchematic task) throws InterruptedException
    {
//        profiler.push("process_task");
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

//                profiler.pop();
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
            ChunkRenderTaskSchematic.Type taskType = task.getType();
            ChunkRenderDispatcherBuffers pack = this.chunkRenderDispatcher.allocateBufferPack();
            ChunkPos cp = task.getChunkPos();
            ChunkRenderGpuUploader uploader = LitematicaRenderer.getInstance()
                                                                .getWorldRenderer()
                                                                .getChunkRendererGpuDispatcher()
                                                                .addOrGetUploader(cp.x, cp.z);

//            profiler.popPush("run_task_now_" + taskType.name());
            if (taskType == ChunkRenderTaskSchematic.Type.REBUILD_CHUNK)
            {
                //LOGGER.warn("[LW] (REBUILD_CHUNK) --> [VBO]");
                if (!uploader.isEmpty())
                {
                    uploader.clear();
                }

                task.getRenderChunk().rebuildChunk(task, pack);
            }
            else if (taskType == ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY)
            {
                //LOGGER.warn("[LW] (RESORT_TRANSPARENCY) --> [VBO]");
                task.getRenderChunk().resortTransparency(task, pack);
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

//                    profiler.pop();
                    return;
                }

                task.setStatus(ChunkRenderTaskSchematic.Status.UPLOADING);
            }
            finally
            {
                task.getLock().unlock();
            }

//            profiler.popPush("run_task_schedule_"+ taskType.name());
            ArrayList<ListenableFuture<Object>> futuresList = Lists.newArrayList();
            final ChunkRenderDataSchematic compiledChunk = task.getChunkRenderData();

            if (taskType == ChunkRenderTaskSchematic.Type.REBUILD_CHUNK)
            {
                //LOGGER.warn("[LW] (REBUILD_CHUNK) --> Run Uploads");

                //if (GuiBase.isCtrlDown()) System.out.printf("pre uploadChunk()\n");
                for (ChunkSectionLayer layer : ChunkRenderLayers.BLOCK_RENDER_LAYERS)
                {
                    if (compiledChunk.isBlockLayerEmpty(layer) == false)
                    {
                        //if (GuiBase.isCtrlDown()) System.out.printf("REBUILD_CHUNK pre uploadChunkBlocks()\n");
                        //LOGGER.warn("[LW] REBUILD_CHUNK pre uploadChunkBlocks({})", layer.label());
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkBlocks(layer, compiledChunk, pack, uploader, task.getDistanceSq(), false));
                    }
                }

                for (OverlayRenderType type : ChunkRenderLayers.TYPES)
                {
                    if (compiledChunk.isOverlayTypeEmpty(type) == false)
                    {
                        //if (GuiBase.isCtrlDown()) System.out.printf("REBUILD_CHUNK pre uploadChunkOverlay()\n");
                        //LOGGER.warn("[LW] REBUILD_CHUNK pre uploadChunkOverlay({})", type.name());
                        futuresList.add(this.chunkRenderDispatcher.uploadChunkOverlay(type, compiledChunk, pack, uploader, task.getDistanceSq(), false));
                    }
                }
            }
            else if (taskType == ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY)
            {
                //LOGGER.warn("[LW] (RESORT_TRANSPARENCY) --> Schedule Uploads");
                ChunkSectionLayer layer = ChunkSectionLayer.TRANSLUCENT;

                if (compiledChunk.isBlockLayerEmpty(layer) == false)
                {
                    //System.out.printf("RESORT_TRANSPARENCY pre uploadChunkBlocks(%s)\n", layer.toString());
                    //LOGGER.warn("[LW] RESORT_TRANSPARENCY pre uploadChunkBlocks({})", layer.label());
                    futuresList.add(this.chunkRenderDispatcher.uploadChunkBlocks(layer, compiledChunk, pack, uploader, task.getDistanceSq(), true));
                }

                if (compiledChunk.isOverlayTypeEmpty(OverlayRenderType.QUAD) == false)
                {
                    //if (GuiBase.isCtrlDown()) System.out.printf("RESORT_TRANSPARENCY pre uploadChunkOverlay()\n");
                    //LOGGER.warn("[LW] RESORT_TRANSPARENCY pre uploadChunkOverlay({})", OverlayRenderType.QUAD.name());
                    futuresList.add(this.chunkRenderDispatcher.uploadChunkOverlay(OverlayRenderType.QUAD, compiledChunk, pack, uploader, task.getDistanceSq(), true));
                }
            }

//            profiler.popPush("run_task_later_" + taskType.name());
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
                    ChunkRenderWorkerLitematica.this.chunkRenderDispatcher.freeBufferPack(pack);
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

                    task.getRenderChunk().updateChunkRenderData(compiledChunk);
                }

                @Override
                public void onFailure(@NotNull Throwable throwable)
                {
                    ChunkRenderWorkerLitematica.this.chunkRenderDispatcher.freeBufferPack(pack);

                    if ((throwable instanceof CancellationException) == false && (throwable instanceof InterruptedException) == false)
                    {
                        Minecraft.getInstance().delayCrashRaw(CrashReport.forThrowable(throwable, "Rendering Litematica chunk"));
                    }
                }
            }, MoreExecutors.directExecutor());
        }

//        profiler.pop();
    }
}
