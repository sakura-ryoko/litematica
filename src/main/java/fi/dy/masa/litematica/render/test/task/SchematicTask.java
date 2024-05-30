package fi.dy.masa.litematica.render.test.task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.common.primitives.Doubles;
import fi.dy.masa.litematica.render.test.buffer.SchematicBlockAllocatorStorage;
import fi.dy.masa.litematica.render.test.builder.SchematicChunkBuilder;
import fi.dy.masa.litematica.render.test.data.SchematicBuiltChunk;

public abstract class SchematicTask implements Comparable<SchematicTask>
{
    protected SchematicBuiltChunk builtChunk;
    protected final double dist;
    protected final AtomicBoolean cancelled = new AtomicBoolean(false);
    public final boolean priorityTask;

    public SchematicTask(SchematicBuiltChunk builtChunk, double dist, boolean priority)
    {
        this.builtChunk = builtChunk;
        this.dist = dist;
        this.priorityTask = priority;
    }

    public abstract CompletableFuture<TaskResult> execute(SchematicChunkBuilder builder, SchematicBlockAllocatorStorage buffer);

    public abstract void cancel();

    public abstract String getName();

    @Override
    public int compareTo(SchematicTask task)
    {
        return Doubles.compare(this.dist, task.dist);
    }

    public enum TaskResult
    {
        SUCCESSFUL,
        CANCELLED;
    }
}
