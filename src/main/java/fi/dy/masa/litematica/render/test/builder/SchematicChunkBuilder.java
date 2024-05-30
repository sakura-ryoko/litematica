package fi.dy.masa.litematica.render.test.builder;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import com.google.common.collect.Queues;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.thread.TaskExecutor;
import fi.dy.masa.litematica.render.test.SchematicWorldRenderer;
import fi.dy.masa.litematica.render.test.buffer.SchematicBlockAllocatorStorage;
import fi.dy.masa.litematica.render.test.buffer.SchematicBlockBuilderPool;
import fi.dy.masa.litematica.render.test.data.SchematicBuiltChunk;
import fi.dy.masa.litematica.render.test.dispatch.SchematicBlockRenderManager;
import fi.dy.masa.litematica.render.test.task.SchematicTask;
import fi.dy.masa.litematica.world.WorldSchematic;

@Environment(EnvType.CLIENT)
public class SchematicChunkBuilder
{
    private final PriorityBlockingQueue<SchematicTask> prioritizedTaskQueue = Queues.newPriorityBlockingQueue();
    private final Queue<SchematicTask> taskQueue = Queues.newLinkedBlockingDeque();
    private int processablePrioritizedTaskCount = 2;
    private volatile int queuedTaskCount;
    private final Queue<Runnable> uploadQueue = Queues.newConcurrentLinkedQueue();
    private final TaskExecutor<Runnable> tasks;
    private final Executor executor;
    final SchematicBlockAllocatorStorage buffers;
    private final SchematicBlockBuilderPool buffersPool;
    WorldSchematic schematicWorld;
    public SchematicWorldRenderer schematicRenderer;
    private Vec3d cameraPos = Vec3d.ZERO;
    private volatile boolean stopped;
    private final SchematicSectionBuilder sectionBuilder;

    public SchematicChunkBuilder(WorldSchematic world, SchematicWorldRenderer worldRenderer, Executor executor,
                                     SchematicBlockRenderManager blockRenderManager, BlockEntityRenderDispatcher blockEntityRenderDispatcher,
                                     SchematicBlockAllocatorStorage buffers, SchematicBlockBuilderPool buffersPool)
    {
        this.schematicWorld = world;
        this.schematicRenderer = worldRenderer;
        this.buffers = buffers;
        this.buffersPool = buffersPool;
        this.executor = executor;
        this.tasks = TaskExecutor.create(executor, "Schematic Section Renderer");
        this.tasks.send(this::scheduleTasks);
        this.sectionBuilder = new SchematicSectionBuilder(blockRenderManager, blockEntityRenderDispatcher);
    }

    public void setSchematicWorld(WorldSchematic newWorld)
    {
        this.schematicWorld = newWorld;
    }

    public WorldSchematic getSchematicWorld()
    {
        return this.schematicWorld;
    }

    public SchematicSectionBuilder getSectionBuilder()
    {
        return this.sectionBuilder;
    }

    private void scheduleTasks()
    {
        if (this.stopped || this.buffersPool.hasNoneAvailable())
        {
            return;
        }

        SchematicTask task = this.pollTask();

        if (task == null)
        {
            return;
        }

        SchematicBlockAllocatorStorage blockBufferBuilderStorage = Objects.requireNonNull(this.buffersPool.newStorage());
        this.queuedTaskCount = this.prioritizedTaskQueue.size() + this.taskQueue.size();

        ((CompletableFuture) CompletableFuture.supplyAsync(Util.debugSupplier(task.getName(), () -> task.execute(this, blockBufferBuilderStorage)), this.executor).thenCompose(future -> future)).whenComplete((result, throwable) ->
        {
            if (throwable != null)
            {
                MinecraftClient.getInstance().setCrashReportSupplierAndAddDetails(CrashReport.create((Throwable) throwable, "Schematic Batching sections"));
                return;
            }
            this.tasks.send(() ->
            {
                if (result == SchematicTask.TaskResult.SUCCESSFUL)
                {
                    blockBufferBuilderStorage.clear();
                }
                else
                {
                    blockBufferBuilderStorage.reset();
                }

                this.buffersPool.release(blockBufferBuilderStorage);
                this.scheduleTasks();
            });
        });
    }

    @Nullable
    private SchematicTask pollTask()
    {
        SchematicTask newTask;

        if (this.processablePrioritizedTaskCount <= 0 && (newTask = this.taskQueue.poll()) != null)
        {
            this.processablePrioritizedTaskCount = 2;
            return newTask;
        }

        newTask = this.prioritizedTaskQueue.poll();

        if (newTask != null)
        {
            --this.processablePrioritizedTaskCount;
            return newTask;
        }

        this.processablePrioritizedTaskCount = 2;
        return this.taskQueue.poll();
    }

    public String getDebugString()
    {
        return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.queuedTaskCount, this.uploadQueue.size(), this.buffersPool.getAvailableCount());
    }

    public int getToBatchCount()
    {
        return this.queuedTaskCount;
    }

    public int getChunksToUpload()
    {
        return this.uploadQueue.size();
    }

    public int getFreeBufferCount()
    {
        return this.buffersPool.getAvailableCount();
    }

    public void setCameraVec3d(Vec3d pos)
    {
        this.cameraPos = pos;
    }

    public Vec3d getCameraVec3d()
    {
        return this.cameraPos;
    }

    public void uploadTask()
    {
        Runnable runnable;

        while ((runnable = this.uploadQueue.poll()) != null)
        {
            runnable.run();
        }
    }

    public void rebuildTask(SchematicBuiltChunk chunk, SchematicRegionBuilder builder)
    {
        chunk.rebuildTask(builder, this.buffers);
    }

    public void sendTask(SchematicTask task)
    {
        if (this.stopped)
        {
            return;
        }

        this.tasks.send(() ->
        {
            if (this.stopped)
            {
                return;
            }
            if (task.priorityTask)
            {
                this.prioritizedTaskQueue.offer(task);
            }
            else
            {
                this.taskQueue.offer(task);
            }
            this.queuedTaskCount = this.prioritizedTaskQueue.size() + this.taskQueue.size();
            this.scheduleTasks();
        });
    }

    public CompletableFuture<Void> uploadMeshData(BuiltBuffer builtBuffer, VertexBuffer vertexBuffer)
    {
        if (this.stopped)
        {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() ->
        {
            if (vertexBuffer.isClosed())
            {
                builtBuffer.close();
                return;
            }
            vertexBuffer.bind();
            vertexBuffer.upload(builtBuffer);
            VertexBuffer.unbind();
        }, this.uploadQueue::add);
    }

    public CompletableFuture<Void> uploadSortingData(BufferAllocator.CloseableBuffer resultBuffer, VertexBuffer vertexBuffer)
    {
        if (this.stopped)
        {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() ->
        {
            if (vertexBuffer.isClosed())
            {
                resultBuffer.close();
                return;
            }
            vertexBuffer.bind();
            vertexBuffer.uploadIndexBuffer(resultBuffer);
            VertexBuffer.unbind();
        }, this.uploadQueue::add);
    }

    public void reset()
    {
        this.clear();
    }

    private void clear()
    {
        SchematicTask task;

        while (!this.prioritizedTaskQueue.isEmpty())
        {
            task = this.prioritizedTaskQueue.poll();
            if (task == null) continue;
            task.cancel();
        }

        while (!this.taskQueue.isEmpty())
        {
            task = this.taskQueue.poll();
            if (task == null) continue;
            task.cancel();
        }

        this.queuedTaskCount = 0;
    }

    public boolean isEmpty()
    {
        return this.queuedTaskCount == 0 && this.uploadQueue.isEmpty();
    }

    public void stop()
    {
        this.stopped = true;
        this.clear();
        this.uploadTask();
    }
}
