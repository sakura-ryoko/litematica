package fi.dy.masa.litematica.render.schematic;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import net.minecraft.world.phys.Vec3;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import fi.dy.masa.litematica.Litematica;

public class ChunkRenderTaskSchematic implements Comparable<ChunkRenderTaskSchematic>
{
    private final ChunkRendererSchematicVbo chunkRenderer;
    private final ChunkRenderTaskSchematic.Type type;
    private final List<Runnable> listFinishRunnables;
    private final ReentrantLock lock;
    private final Supplier<Vec3> cameraPosSupplier;
    private final double distanceSq;
    private BufferAllocatorCache allocatorCache;
    private ChunkRenderDataSchematic chunkRenderData;
    private ChunkRenderTaskSchematic.Status status;
    private boolean finished;

    public ChunkRenderTaskSchematic(ChunkRendererSchematicVbo renderChunkIn, ChunkRenderTaskSchematic.Type typeIn, Supplier<Vec3> cameraPosSupplier, double distanceSqIn)
    {
        this.chunkRenderer = renderChunkIn;
        this.type = typeIn;
		this.listFinishRunnables = Lists.newArrayList();
	    this.lock = new ReentrantLock();
        this.cameraPosSupplier = cameraPosSupplier;
        this.distanceSq = distanceSqIn;
	    this.status = ChunkRenderTaskSchematic.Status.PENDING;
    }

    public Supplier<Vec3> getCameraPosSupplier()
    {
        return this.cameraPosSupplier;
    }

    public ChunkRenderTaskSchematic.Status getStatus()
    {
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
            Litematica.LOGGER.error("setRegionRenderCacheBuilder() [Task] allocatorCache is null");
            return false;
        }
        if (this.allocatorCache != null && !this.allocatorCache.isClear())
        {
            this.allocatorCache.closeAll();
        }

        this.allocatorCache = allocatorCache;
        return true;
    }

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
//        UPLOAD_CHUNK,
        RESORT_TRANSPARENCY
    }
}
