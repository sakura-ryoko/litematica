package fi.dy.masa.litematica.render.test.task;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.ChunkSectionPos;
import fi.dy.masa.litematica.render.test.buffer.SchematicBlockAllocatorStorage;
import fi.dy.masa.litematica.render.test.builder.SchematicChunkBuilder;
import fi.dy.masa.litematica.render.test.data.SchematicBuiltChunk;
import fi.dy.masa.litematica.render.test.data.SchematicChunkRenderData;
import fi.dy.masa.litematica.render.test.data.SchematicRendererRegion;
import fi.dy.masa.litematica.render.test.data.SchematicSectionRenderData;

public class SchematicRebuildTask extends SchematicTask
{
    @Nullable
    protected SchematicRendererRegion schematicRegion;

    public SchematicRebuildTask(SchematicBuiltChunk builtChunk, double dist, boolean priority, SchematicRendererRegion region)
    {
        super(builtChunk, dist, priority);
        this.schematicRegion = region;
    }

    @Override
    public String getName()
    {
        return "rend_schem_chk_rebuild";
    }

    @Override
    public CompletableFuture<SchematicTask.TaskResult> execute(SchematicChunkBuilder builder, SchematicBlockAllocatorStorage buffers)
    {
        if (this.cancelled.get())
        {
            return CompletableFuture.completedFuture(TaskResult.CANCELLED);
        }
        if (!this.builtChunk.shouldBuild())
        {
            this.cancel();
            return CompletableFuture.completedFuture(TaskResult.CANCELLED);
        }
        if (this.cancelled.get())
        {
            return CompletableFuture.completedFuture(TaskResult.CANCELLED);
        }

        SchematicRendererRegion chunkRendererRegion = this.schematicRegion;
        this.schematicRegion = null;

        if (chunkRendererRegion == null)
        {
            this.builtChunk.setCompiled(SchematicChunkRenderData.EMPTY_2);
            return CompletableFuture.completedFuture(TaskResult.SUCCESSFUL);
        }

        ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(this.builtChunk.getChunkOrigin());
        SchematicSectionRenderData renderData = builder.getSectionBuilder().build(chunkSectionPos, chunkRendererRegion, this.builtChunk.getSorter(), buffers);
        this.builtChunk.setNoCullingBlockEntities(renderData.noCullingBlockEntities);

        if (this.cancelled.get())
        {
            renderData.close();
            return CompletableFuture.completedFuture(TaskResult.CANCELLED);
        }
        SchematicChunkRenderData chunkData = new SchematicChunkRenderData();

        chunkData.occlusionData = renderData.occlusionData;
        chunkData.blockEntityList.addAll(renderData.blockEntityList);
        chunkData.sortingData = renderData.sortingData;

        ArrayList<CompletableFuture<Void>> list = new ArrayList<>(renderData.usedLayers.size() + renderData.usedOverlays.size());

        renderData.usedLayers.forEach((renderLayer, layerBuffer) ->
        {
            list.add(builder.uploadMeshData(layerBuffer, this.builtChunk.getBufferByLayer(renderLayer)));
            chunkData.usedLayers.add(renderLayer);
        });
        renderData.usedOverlays.forEach((renderOverlay, layerBuffer) ->
        {
            list.add(builder.uploadMeshData(layerBuffer, this.builtChunk.getBufferByOverlay(renderOverlay)));
            chunkData.usedOverlays.add(renderOverlay);
        });

        return Util.combine(list).handle((results, throwable) ->
        {
            if (throwable != null && !(throwable instanceof CancellationException) && !(throwable instanceof InterruptedException))
            {
                MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create(throwable, "Schematic Rendering section"));
            }
            if (this.cancelled.get())
            {
                return TaskResult.CANCELLED;
            }
            this.builtChunk.setCompiled(chunkData);
            return TaskResult.SUCCESSFUL;
        });
    }

    @Override
    public void cancel()
    {
        this.schematicRegion = null;

        if (this.cancelled.compareAndSet(false, true))
        {
            this.builtChunk.scheduleRebuild(false);
        }
    }
}