package fi.dy.masa.litematica.render.test.task;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.crash.CrashReport;
import fi.dy.masa.litematica.render.test.SchematicOverlayType;
import fi.dy.masa.litematica.render.test.buffer.SchematicBlockAllocatorStorage;
import fi.dy.masa.litematica.render.test.builder.SchematicChunkBuilder;
import fi.dy.masa.litematica.render.test.data.SchematicBuiltChunk;
import fi.dy.masa.litematica.render.test.data.SchematicChunkRenderData;

public class SchematicSorterTask extends SchematicTask
{
    private final SchematicChunkRenderData chunkData;
    
    public SchematicSorterTask(SchematicBuiltChunk builtChunk, double dist, boolean priority, SchematicChunkRenderData chunkData)
    {
        super(builtChunk, dist, priority);
        this.chunkData = chunkData;
    }

    @Override
    public String getName()
    {
        return "rend_schem_chk_sort";
    }
    
    @Override
    public CompletableFuture<TaskResult> execute(SchematicChunkBuilder builder, SchematicBlockAllocatorStorage buffers)
    {
        if (this.cancelled.get())
        {
            return CompletableFuture.completedFuture(TaskResult.CANCELLED);
        }
        if (!this.builtChunk.shouldBuild())
        {
            this.cancelled.set(true);

            return CompletableFuture.completedFuture(TaskResult.CANCELLED);
        }
        if (this.cancelled.get())
        {
            return CompletableFuture.completedFuture(TaskResult.CANCELLED);
        }

        BuiltBuffer.SortState sortState = this.chunkData.sortingData;

        if (sortState == null)
        {
            return CompletableFuture.completedFuture(TaskResult.CANCELLED);
        }
        if (this.chunkData.isEmpty(RenderLayer.getTranslucent()) && this.chunkData.isEmpty(SchematicOverlayType.QUAD))
        {
            return CompletableFuture.completedFuture(TaskResult.CANCELLED);
        }

        BufferAllocator cachedBuffer;
        VertexSorter vertexSorter = this.builtChunk.getSorter();
        CompletionStage completableFuture;

        if (!this.chunkData.isEmpty(RenderLayer.getTranslucent()))
        {
            cachedBuffer = buffers.getBufferByLayer(RenderLayer.getTranslucent());
            BufferAllocator.CloseableBuffer closeableBuffer = sortState.sortAndStore(cachedBuffer, vertexSorter);

            if (closeableBuffer == null)
            {
                return CompletableFuture.completedFuture(TaskResult.CANCELLED);
            }
            if (this.cancelled.get())
            {
                closeableBuffer.close();
                return CompletableFuture.completedFuture(TaskResult.CANCELLED);
            }

            completableFuture = builder.uploadSortingData(closeableBuffer,
                    this.builtChunk.getBufferByLayer(RenderLayer.getTranslucent())).thenApply(v -> TaskResult.CANCELLED);
        }
        else
        {
            // TODO might need to remove this part
            cachedBuffer = buffers.getBufferByOverlay(SchematicOverlayType.QUAD);
            BufferAllocator.CloseableBuffer closeableBuffer = sortState.sortAndStore(cachedBuffer, vertexSorter);

            if (closeableBuffer == null)
            {
                return CompletableFuture.completedFuture(TaskResult.CANCELLED);
            }
            if (this.cancelled.get())
            {
                closeableBuffer.close();
                return CompletableFuture.completedFuture(TaskResult.CANCELLED);
            }

            completableFuture = builder.uploadSortingData(closeableBuffer,
                    this.builtChunk.getBufferByOverlay(SchematicOverlayType.QUAD)).thenApply(v -> TaskResult.CANCELLED);
        }

        return ((CompletableFuture) completableFuture).handle((result, throwable) ->
        {
            if (throwable != null && !(throwable instanceof CancellationException) && !(throwable instanceof InterruptedException))
            {
                MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create((Throwable) throwable, "Schematic Rendering section"));
            }
            return this.cancelled.get() ? TaskResult.CANCELLED : TaskResult.SUCCESSFUL;
        });
    }

    @Override
    public void cancel()
    {
        this.cancelled.set(true);
    }
}
