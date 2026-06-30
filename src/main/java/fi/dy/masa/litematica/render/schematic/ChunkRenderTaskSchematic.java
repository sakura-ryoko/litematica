package fi.dy.masa.litematica.render.schematic;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public class ChunkRenderTaskSchematic implements Comparable<ChunkRenderTaskSchematic>
{
    private final AtomicReference<ChunkRendererSchematicVbo> chunkRenderer;
    private final AtomicReference<ChunkRenderDataSchematic> chunkRenderData;
    private final ChunkRenderTaskSchematic.Type type;
    private final List<Runnable> listFinishRunnables;
    private final ReentrantLock lock;
    private final Supplier<Vec3> cameraPosSupplier;
    private final double distanceSq;
    private ChunkRenderTaskSchematic.Status status;
    private boolean finished;

    public ChunkRenderTaskSchematic(@Nonnull ChunkRendererSchematicVbo renderChunkIn, ChunkRenderTaskSchematic.Type typeIn, Supplier<Vec3> cameraPosSupplier, double distanceSqIn)
    {
        this.type = typeIn;
		this.listFinishRunnables = Lists.newArrayList();
	    this.lock = new ReentrantLock();
        this.cameraPosSupplier = cameraPosSupplier;
        this.distanceSq = distanceSqIn;
	    this.status = ChunkRenderTaskSchematic.Status.PENDING;
        this.chunkRenderer = new AtomicReference<>(renderChunkIn);
        this.chunkRenderData = new AtomicReference<>(new ChunkRenderDataSchematic());
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
        return this.chunkRenderer.get();
    }

    protected ChunkRenderDataSchematic getChunkRenderData()
    {
        if (this.chunkRenderData.get() == null || this.chunkRenderData.get().isEmpty())
        {
            this.updateChunkRenderData(this.getRenderChunk().getChunkRenderData());
        }

        return this.chunkRenderData.get();
    }

    protected ChunkPos getChunkPos()
    {
        return this.getRenderChunk().getChunkPos();
    }

    protected void updateChunkRenderData(ChunkRenderDataSchematic data)
    {
//        LOGGER.warn("[Task] updateChunkRenderData() type: [{}]", this.type.name());
        this.lock.lock();

        try
        {
//            if (this.chunkRenderData != null)
//            {
//                this.chunkRenderData.clearAll();
//            }
//
//            this.chunkRenderData = data;

            if (!data.isEmpty())
            {
                ChunkRenderDataSchematic oldData = this.chunkRenderData.getAndSet(data);

                if (oldData != null)
                {
//                    LOGGER.error("[Task] updateChunkRenderData() type: [{}] // oldData DUMP -->", this.type.name());
//                    oldData.dumpRenderDataDebug();
                    oldData.clearAll();
                }
            }
//            else
//            {
//                LOGGER.error("[Task] updateChunkRenderData() newData EMPTY!");
//            }

//            LOGGER.error("[Task] updateChunkRenderData() type: [{}] // newData DUMP -->", this.type.name());
//            data.dumpRenderDataDebug();
        }
        finally
        {
            this.lock.unlock();
        }
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
                this.chunkRenderer.get().setNeedsUpdate(false);
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
