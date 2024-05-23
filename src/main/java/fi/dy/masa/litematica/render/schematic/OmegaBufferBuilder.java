package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.class_9799;
import net.minecraft.class_9801;
import net.minecraft.client.render.*;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import fi.dy.masa.litematica.Litematica;

/**
 * Well, there is always a way to fix when Mojang deletes things <.<
 */
@Environment(value = EnvType.CLIENT)
public class OmegaBufferBuilder
{
    // Hack Fix
    @Nullable public OmegaBufferBuilder.OmegaBuilt lastRenderBuildBuffer;
    public boolean first = true;
    private double offsetY;

    // Old BufferBuilder Stuff
    private ByteBuffer oldBuffer;
    private boolean oldClosed;
    private int oldBuiltBufferCount;
    private int oldBatchOffset;
    private int oldElementOffset;
    private int oldVertexCount;
    @Nullable
    private VertexFormatElement oldCurrentElement;
    private int oldCurrentElementId;
    private VertexFormat oldFormat;
    private VertexFormat.DrawMode oldDrawMode;
    private boolean oldCanSkipElementChecks;
    private boolean oldHasOverlay;
    private boolean oldBuilding;
    @Nullable
    private Vector3f[] oldSortingPrimitiveCenters;
    @Nullable
    private VertexSorter oldSorter;
    private boolean oldHasNoVertexBuffer;

    public OmegaBufferBuilder(int capacity)
    {
        this.oldBuffer = OmegaAlloc.alloc(capacity);
        this.offsetY = 0;
        this.oldClosed = false;
        this.oldBuiltBufferCount = 0;
        this.oldBatchOffset = 0;
        this.oldElementOffset = 0;
        this.oldVertexCount = 0;
        //this.oldCurrentElement;
        //this.oldCurrentElementId;
        //this.oldFormat;
        //this.oldDrawMode;
        this.oldCanSkipElementChecks = false;
        this.oldHasOverlay = false;
        this.oldBuilding = false;
        //this.oldSortingPrimitiveCenters;
        //this.oldSorter;
        this.oldHasNoVertexBuffer = false;

    }

    // Older code
    public void setOffsetY(double offset)
    {
        this.offsetY = offset;
    }

    private void grow()
    {
        this.grow(this.oldFormat.getVertexSizeByte());
    }

    private void grow(int size)
    {
        if (this.oldElementOffset + size <= this.oldBuffer.capacity())
        {
            return;
        }
        int i = this.oldBuffer.capacity();
        int j = Math.min(i, 0x200000);
        int k = i + size;
        int newSize = Math.max(i + j, k);
        Litematica.logger.debug("OmegaBufferBuilder grow buffer: Old size {} bytes, new size {} bytes.", i, newSize);
        ByteBuffer byteBuffer = OmegaAlloc.resize(this.oldBuffer, newSize);
        byteBuffer.rewind();
        this.oldBuffer = byteBuffer;
    }

    public void clear()
    {
        if (this.oldBuiltBufferCount > 0)
        {
            Litematica.logger.warn("Clearing OmegaBufferBuilder with unused batches");
        }
        this.reset();
    }

    public void reset()
    {
        this.oldBuiltBufferCount = 0;
        this.oldBatchOffset = 0;
        this.oldElementOffset = 0;
    }

    public void close()
    {
        if (this.oldBuiltBufferCount > 0)
        {
            throw new IllegalStateException("OmegaBufferBuilder closed with unused batches");
        }
        if (this.oldBuilding)
        {
            throw new IllegalStateException("Cannot close OmegaBufferBuilder while it is building");
        }
        if (this.oldClosed)
        {
            return;
        }
        this.oldClosed = true;
        OmegaAlloc.free(this.oldBuffer);
    }

    public void setSorter(VertexSorter sorter)
    {
        if (this.oldDrawMode != VertexFormat.DrawMode.QUADS)
        {
            return;
        }
        this.oldSorter = sorter;
        if (this.oldSortingPrimitiveCenters == null)
        {
            this.oldSortingPrimitiveCenters = this.buildPrimitiveCenters();
        }
    }

    //@Override
    public void begin(VertexFormat.DrawMode drawMode, VertexFormat format)
    {
        if (this.lastRenderBuildBuffer == null)
        {
            if (this.first == false)
            {
                this.end();
            }
            else
            {
                this.first = false;
            }
        }
        else
        {
            this.lastRenderBuildBuffer = null;
        }

        if (this.oldBuilding)
        {
            throw new IllegalStateException("Already building!");
        }
        this.ensureNotClosed();
        this.oldBuilding = true;
        this.oldDrawMode = drawMode;
        this.setFormat(format);
        this.oldCurrentElement = format.getElements().getFirst();
        this.oldCurrentElementId = 0;
        this.oldBuffer.rewind();
    }

    public OmegaBuilt end()
    {
        return this.lastRenderBuildBuffer;
    }

    private void ensureNotClosed()
    {
        if (this.oldClosed)
        {
            throw new IllegalStateException("This BufferBuilder has been closed");
        }
    }

    private void setFormat(VertexFormat format)
    {
        if (this.oldFormat == format)
        {
            return;
        }
        this.oldFormat = format;
        boolean bl = format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
        boolean bl2 = format == VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;
        this.oldCanSkipElementChecks = bl || bl2;
        this.oldHasOverlay = bl;
    }

    public void beginSortedIndexBuffer(OmegaTransparentSortingData state)
    {
        this.ensureNotClosed();
        this.oldBuffer.rewind();
        this.oldDrawMode = state.drawMode;
        this.oldVertexCount = state.vertexCount;
        this.oldElementOffset = this.oldBatchOffset;
        this.oldSortingPrimitiveCenters = state.primitiveCenters;
        this.oldSorter = state.sorter;
        this.oldHasNoVertexBuffer = true;
    }

    private Vector3f[] buildPrimitiveCenters()
    {
        FloatBuffer floatBuffer = this.oldBuffer.asFloatBuffer();
        int i = this.oldBatchOffset / 4;
        int j = this.oldFormat.getVertexSizeByte() / 4;
        int k = j * this.oldDrawMode.additionalVertexCount;
        int l = this.oldVertexCount / this.oldDrawMode.additionalVertexCount;
        Vector3f[] vector3fs = new Vector3f[l];
        for (int m = 0; m < l; ++m)
        {
            float f = floatBuffer.get(i + m * k + 0);
            float g = floatBuffer.get(i + m * k + 1);
            float h = floatBuffer.get(i + m * k + 2);
            float n = floatBuffer.get(i + m * k + j * 2 + 0);
            float o = floatBuffer.get(i + m * k + j * 2 + 1);
            float p = floatBuffer.get(i + m * k + j * 2 + 2);
            float q = (f + n) / 2.0f;
            float r = (g + o) / 2.0f;
            float s = (h + p) / 2.0f;
            vector3fs[m] = new Vector3f(q, r, s);
        }

        return vector3fs;
    }

    private void writeSortedIndices(VertexFormat.IndexType indexType)
    {
        if (this.oldSortingPrimitiveCenters == null || this.oldSorter == null)
        {
            throw new IllegalStateException("Sorting state uninitialized");
        }
        int[] is = this.oldSorter.sort(this.oldSortingPrimitiveCenters);
        IntConsumer intConsumer = this.getIndexConsumer(this.oldElementOffset, indexType);

        for (int i : is)
        {
            intConsumer.accept(i * this.oldDrawMode.additionalVertexCount + 0);
            intConsumer.accept(i * this.oldDrawMode.additionalVertexCount + 1);
            intConsumer.accept(i * this.oldDrawMode.additionalVertexCount + 2);
            intConsumer.accept(i * this.oldDrawMode.additionalVertexCount + 2);
            intConsumer.accept(i * this.oldDrawMode.additionalVertexCount + 3);
            intConsumer.accept(i * this.oldDrawMode.additionalVertexCount + 0);
        }
    }

    private IntConsumer getIndexConsumer(int elementOffset, VertexFormat.IndexType indexType)
    {
        MutableInt mutableInt = new MutableInt(elementOffset);

        return switch (indexType)
        {
            default -> throw new MatchException(null, null);
            case VertexFormat.IndexType.SHORT -> index -> this.oldBuffer.putShort(mutableInt.getAndAdd(2), (short)index);
            case VertexFormat.IndexType.INT -> index -> this.oldBuffer.putInt(mutableInt.getAndAdd(4), index);
        };
    }

    public boolean isBatchEmpty()
    {
        return this.oldVertexCount == 0;
    }

    public boolean isBuilding()
    {
        return this.oldBuilding;
    }

    private ByteBuffer slice(int begin, int end)
    {
        return MemoryUtil.memSlice(this.oldBuffer, begin, end);
    }

    public OmegaTransparentSortingData getSortingData()
    {
        return new OmegaTransparentSortingData(this.oldDrawMode, this.oldVertexCount, this.oldSortingPrimitiveCenters, this.oldSorter);
    }

    public VertexConsumer vertex(float f, float g, float h)
    {
        //return super.vertex(x, (float) (y + this.offsetY), z);
        return this.oldBuffer::vertex;
    }


    @Environment(value = EnvType.CLIENT)
    public static class OmegaTransparentSortingData
    {
        final VertexFormat.DrawMode drawMode;
        final int vertexCount;
        @Nullable
        final Vector3f[] primitiveCenters;
        @org.jetbrains.annotations.Nullable
        final VertexSorter sorter;

        OmegaTransparentSortingData(VertexFormat.DrawMode drawMode, int vertexCount, @Nullable Vector3f[] primitiveCenters, @Nullable VertexSorter sorter)
        {
            this.drawMode = drawMode;
            this.vertexCount = vertexCount;
            this.primitiveCenters = primitiveCenters;
            this.sorter = sorter;
        }
    }

    @Environment(value = EnvType.CLIENT)
    public class OmegaBuilt
    {
        private final int batchOffset;
        private final OmegaDraw parameters;
        private boolean released;

        OmegaBuilt(int batchOffset, OmegaDraw parameters)
        {
            this.batchOffset = batchOffset;
            this.parameters = parameters;
        }

        public boolean isEmpty() { return this.parameters.vertexCount() == 0; }

        public OmegaDraw getParameters()
        {
            return parameters;
        }

        @Nullable
        public ByteBuffer getVertexBuffer()
        {
            if (this.parameters.indexOnly()) {
                return null;
            }
            int i = this.batchOffset + this.parameters.getVertexBufferStart();
            int j = this.batchOffset + this.parameters.getVertexBufferEnd();
            return OmegaBufferBuilder.this.slice(i, j);
        }
    }

    @Environment(value = EnvType.CLIENT)
    public record OmegaDraw(VertexFormat format, int vertexCount, int indexCount, VertexFormat.DrawMode mode, VertexFormat.IndexType indexType, boolean indexOnly, boolean sequentialIndex)
    {
        public int getVertexBufferSize() {
            return this.vertexCount * this.format.getVertexSizeByte();
        }

        public int getVertexBufferStart() {
            return 0;
        }

        public int getVertexBufferEnd() {
            return this.getVertexBufferSize();
        }

        public int getIndexBufferStart() {
            return this.indexOnly ? 0 : this.getVertexBufferEnd();
        }

        public int getIndexBufferEnd() {
            return this.getIndexBufferStart() + this.getIndexBufferSize();
        }

        private int getIndexBufferSize() {
            return this.sequentialIndex ? 0 : this.indexCount * this.indexType.size;
        }

        public int getBufferSize() {
            return this.getIndexBufferEnd();
        }
    }

    @Environment(value = EnvType.CLIENT)
    private static class OmegaAlloc
    {
        // Do we really need to map our own memory for this? >.>
        private static final MemoryUtil.MemoryAllocator ALLOC = MemoryUtil.getAllocator(false);

        static ByteBuffer alloc(int size)
        {
            long l = ALLOC.malloc(size);
            if (l == 0L) {
                throw new OutOfMemoryError("Failed to allocate " + size + " bytes");
            }
            return MemoryUtil.memByteBuffer(l, size);
        }

        static ByteBuffer resize(ByteBuffer src, int newSize)
        {
            long l = ALLOC.realloc(MemoryUtil.memAddress0(src), newSize);
            if (l == 0L)
            {
                throw new OutOfMemoryError("Failed to resize buffer from " + src.capacity() + " bytes to " + newSize + " bytes");
            }
            return MemoryUtil.memByteBuffer(l, newSize);
        }

        static void free(ByteBuffer var)
        {
            ALLOC.free(MemoryUtil.memAddress0(var));
        }
    }
    static {
        field_52070 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    }
}
