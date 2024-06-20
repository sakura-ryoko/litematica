package fi.dy.masa.litematica.render.schematic;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.google.common.primitives.Doubles;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.litematica.Litematica;

public class ChunkRenderTaskSchematic implements Comparable<ChunkRenderTaskSchematic>
{
    private final ChunkRendererSchematicVbo chunkRenderer;
    private final ChunkRenderTaskSchematic.Type type;
    private final ConcurrentLinkedQueue<Runnable> finishRunnables = new ConcurrentLinkedQueue<>();
    private final Supplier<Vec3d> cameraPosSupplier;
    private final double distanceSq;
    private BufferAllocatorCache allocatorCache;
    private ChunkRenderDataSchematic chunkRenderData;
    private final AtomicReference<ChunkRenderTaskSchematic.Status> status = new AtomicReference<>(Status.PENDING);

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
        return this.status.get();
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

        this.allocatorCache = allocatorCache;
        return true;
    }

    protected Status casStatus(Status expected, Status nStatus) {
        return status.compareAndExchange(expected, nStatus);
    }

    protected void finish()
    {
        Status current = status.get();
        if(current==Status.DONE)
            return;
        if(status.compareAndSet(current,Status.DONE)) {
            Runnable runnable;
            while((runnable = finishRunnables.poll())!= null) {
                runnable.run();
            }
        }
    }

    protected void addFinishRunnable(Runnable runnable)
    {
        if(status.get() == Status.DONE) {
            runnable.run();
            return;
        }
        finishRunnables.add(runnable);
        if(status.get() == Status.DONE) {
            runnable = finishRunnables.poll();
            if(runnable!=null)
                runnable.run();
        }
    }


    protected ChunkRenderTaskSchematic.Type getType()
    {
        return this.type;
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
