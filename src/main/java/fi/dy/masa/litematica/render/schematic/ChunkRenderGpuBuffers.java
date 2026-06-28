package fi.dy.masa.litematica.render.schematic;

import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;

public class ChunkRenderGpuBuffers implements AutoCloseable
{
    private final Supplier<String> name;
    protected volatile GpuBuffer vertexBuffer;
    @Nullable
    protected volatile GpuBuffer indexBuffer;
    private volatile int indexCount;
    private volatile VertexFormat.IndexType indexType;

    protected ChunkRenderGpuBuffers(Supplier<String> name,
                                    GpuBuffer vertexBuffer,
                                    @Nullable GpuBuffer indexBuffer,
                                    int indexCount, VertexFormat.IndexType indexType)
    {
        this.name = name;
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.indexCount = indexCount;
        this.indexType = indexType;
    }

    protected String getName()
    {
        return this.name.get();
    }

    protected GpuBuffer getVertexBuffer()
    {
        return this.vertexBuffer;
    }

    @Nullable
    protected GpuBuffer getIndexBuffer()
    {
        return this.indexBuffer;
    }

    protected void setIndexBuffer(@Nullable GpuBuffer indexBuffer)
    {
        this.indexBuffer = indexBuffer;
    }

    protected int getIndexCount()
    {
        return this.indexCount;
    }

    protected VertexFormat.IndexType getIndexType()
    {
        return this.indexType;
    }

    protected void setIndexType(VertexFormat.IndexType indexType)
    {
        this.indexType = indexType;
    }

    protected void setIndexCount(int indexCount)
    {
        this.indexCount = indexCount;
    }

    protected void setVertexBuffer(GpuBuffer vertexBuffer)
    {
        this.vertexBuffer = vertexBuffer;
    }

    public boolean isClosed()
    {
        if (this.vertexBuffer.isClosed()) { return true; }
        GpuBuffer localIndexBuffer = this.indexBuffer;

        return localIndexBuffer != null && localIndexBuffer.isClosed();
    }

    @Override
    public void close() throws Exception
    {
        this.vertexBuffer.close();
        GpuBuffer localIndexBuffer = this.indexBuffer;

        if (localIndexBuffer != null)
        {
            localIndexBuffer.close();
        }
    }
}
