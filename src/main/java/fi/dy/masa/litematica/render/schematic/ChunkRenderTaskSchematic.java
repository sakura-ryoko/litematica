package fi.dy.masa.litematica.render.schematic;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.litematica.Litematica;

public class ChunkRenderTaskSchematic implements Comparable<ChunkRenderTaskSchematic>
{
    private final ChunkRendererSchematicVbo chunkRenderer;
    private final ChunkRenderTaskSchematic.Type type;
    // Threaded
    //private final ConcurrentLinkedQueue<Runnable> finishRunnables = new ConcurrentLinkedQueue<>();
    private final List<Runnable> listFinishRunnables = Lists.<Runnable>newArrayList();
    private final ReentrantLock lock = new ReentrantLock();
    //
    private final Supplier<Vec3d> cameraPosSupplier;
    private final double distanceSq;
    private BufferAllocatorCache allocatorCache;
    private ChunkRenderDataSchematic chunkRenderData;
    // Threaded
    //private final AtomicReference<ChunkRenderTaskSchematic.Status> status = new AtomicReference<>(Status.PENDING);
    private ChunkRenderTaskSchematic.Status status = ChunkRenderTaskSchematic.Status.PENDING;
    private boolean finished;
    //

    public ChunkRenderTaskSchematic(ChunkRendererSchematicVbo renderChunkIn, ChunkRenderTaskSchematic.Type typeIn, Supplier<Vec3d> cameraPosSupplier, double distanceSqIn)
    {
        this.chunkRenderer = renderChunkIn;
        this.type = typeIn;
        this.cameraPosSupplier = cameraPosSupplier;
        this.distanceSq = distanceSqIn;
    }

    public Supplier<Vec3d> getCameraPosSupplier()
    {
        return this.cameraPosSupplier;
    }

    public ChunkRenderTaskSchematic.Status getStatus()
    {
        //Threaded Code
        //return this.status.get();
        return this.status;
    }

    protected ChunkRendererSchematicVbo getRenderChunk()
    {
        return this.chunkRenderer;
    }

    protected ChunkRenderDataSchematic getChunkRenderData()
    {
        return this.chunkRenderData;
    }

    protected void setChunkRenderData(ChunkRenderDataSchematic chunkRenderData)
    {
        if (this.chunkRenderData != null)
        {
            this.chunkRenderData.clearAll();
        }

        this.chunkRenderData = chunkRenderData;
    }

    public BufferAllocatorCache getAllocatorCache()
    {
        return this.allocatorCache;
    }

    public boolean setRegionRenderCacheBuilder(BufferAllocatorCache allocatorCache)
    {
        if (allocatorCache == null)
        {
            Litematica.logger.error("setRegionRenderCacheBuilder() [Task] allocatorCache is null");
            return false;
        }
        if (this.allocatorCache != null)
        {
            this.allocatorCache.closeAll();
        }

        this.allocatorCache = allocatorCache;
        return true;
    }

    /* Threaded Code
    protected Status casStatus(Status expected, Status nStatus)
    {
        return status.compareAndExchange(expected, nStatus);
    }

    protected void finish()
    {
        Status current = status.get();

        if (current == Status.DONE)
        {
            return;
        }
        if (status.compareAndSet(current,Status.DONE))
        {
            Runnable runnable;
            while((runnable = finishRunnables.poll())!= null)
            {
                runnable.run();
            }
        }
    }

    protected void addFinishRunnable(Runnable runnable)
    {
        if (status.get() == Status.DONE)
        {
            runnable.run();
            return;
        }
        finishRunnables.add(runnable);
        if (status.get() == Status.DONE)
        {
            runnable = finishRunnables.poll();
            if (runnable != null)
            {
                runnable.run();
            }
        }
    }
     */

    protected void setStatus(ChunkRenderTaskSchematic.Status statusIn)
    {
        this.lock.lock();

        try
        {
            this.status = statusIn;
        }
        finally
        {
            this.lock.unlock();
        }
    }

    protected void finish()
    {
        this.lock.lock();

        try
        {
            if (this.type == ChunkRenderTaskSchematic.Type.REBUILD_CHUNK && this.status != ChunkRenderTaskSchematic.Status.DONE)
            {
                this.chunkRenderer.setNeedsUpdate(false);
            }

            this.finished = true;
            this.status = ChunkRenderTaskSchematic.Status.DONE;

            for (Runnable runnable : this.listFinishRunnables)
            {
                runnable.run();
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }

    protected void addFinishRunnable(Runnable runnable)
    {
        this.lock.lock();

        try
        {
            this.listFinishRunnables.add(runnable);

            if (this.finished)
            {
                runnable.run();
            }
        }
        finally
        {
            this.lock.unlock();
        }
    }

    public ReentrantLock getLock()
    {
        return this.lock;
    }

    protected ChunkRenderTaskSchematic.Type getType()
    {
        return this.type;
    }

    protected boolean isFinished()
    {
        return this.finished;
    }

    public int compareTo(ChunkRenderTaskSchematic other)
    {
        return Doubles.compare(this.distanceSq, other.distanceSq);
    }

    public double getDistanceSq()
    {
        return this.distanceSq;
    }

    public enum Status
    {
        PENDING,
        COMPILING,
        UPLOADING,
        DONE
    }

    public enum Type
    {
        REBUILD_CHUNK,
        RESORT_TRANSPARENCY
    }
}
