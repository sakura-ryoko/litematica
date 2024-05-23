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
public class OmegaBufferBuilder implements VertexConsumer
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

    // New BufferBuilder stuff
    private static final long field_52068 = -1L;
    private static final long field_52069 = -1L;
    private static final boolean field_52070;
    private final class_9799 field_52071;
    private long field_52072 = -1L;
    private int vertexCount;
    private final VertexFormat format;
    private final VertexFormat.DrawMode field_52073;
    private final boolean canSkipElementChecks;
    private final boolean hasOverlay;
    private final int field_52074;
    private final int field_52075;
    private final int[] field_52076;
    private int field_52077;
    private boolean building = true;

    public OmegaBufferBuilder(class_9799 arg, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat,
                              int capacity)
    {
        if (!vertexFormat.method_60836(VertexFormatElement.field_52107))
        {
            throw new IllegalArgumentException("Cannot build mesh with no position element");
        }
        else
        {
            this.field_52071 = arg;
            this.field_52073 = drawMode;
            this.format = vertexFormat;
            this.field_52074 = vertexFormat.getVertexSizeByte();
            this.field_52075 = vertexFormat.method_60839() & ~VertexFormatElement.field_52107.method_60843();
            this.field_52076 = vertexFormat.method_60838();
            boolean bl = vertexFormat == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
            boolean bl2 = vertexFormat == VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;
            this.canSkipElementChecks = bl || bl2;
            this.hasOverlay = bl;
        }
    }

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
        this.grow(this.format.getVertexSizeByte());
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
        if (this.format == format)
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

    // Newer Code
    @Nullable
    public class_9801 method_60794()
    {
        this.method_60802();
        this.method_60806();
        class_9801 lv = this.method_60804();
        this.building = false;
        this.field_52072 = -1L;
        return lv;
    }

    public class_9801 method_60800()
    {
        class_9801 lv = this.method_60794();
        if (lv == null)
        {
            throw new IllegalStateException("BufferBuilder was empty");
        }
        else
        {
            return lv;
        }
    }

    private void method_60802()
    {
        if (!this.building)
        {
            throw new IllegalStateException("Not building!");
        }
    }

    @Nullable
    private class_9801 method_60804()
    {
        if (this.vertexCount == 0)
        {
            return null;
        }
        else
        {
            class_9799.class_9800 lv = this.field_52071.method_60807();
            if (lv == null)
            {
                return null;
            }
            else
            {
                int i = this.field_52073.getIndexCount(this.vertexCount);
                VertexFormat.IndexType indexType = VertexFormat.IndexType.smallestFor(this.vertexCount);
                return new class_9801(lv, new class_9801.DrawParameters(this.format, this.vertexCount, i, this.field_52073, indexType));
            }
        }
    }

    private long method_60805()
    {
        this.method_60802();
        this.method_60806();
        ++this.vertexCount;
        long l = this.field_52071.method_60808(this.field_52074);
        this.field_52072 = l;
        return l;
    }

    private long method_60798(VertexFormatElement vertexFormatElement)
    {
        int i = this.field_52077;
        int j = i & ~vertexFormatElement.method_60843();
        if (j == i)
        {
            return -1L;
        }
        else
        {
            this.field_52077 = j;
            long l = this.field_52072;
            if (l == -1L)
            {
                throw new IllegalArgumentException("Not currently building vertex");
            }
            else
            {
                return l + (long)this.field_52076[vertexFormatElement.id()];
            }
        }
    }

    private void method_60806()
    {
        if (this.vertexCount != 0)
        {
            if (this.field_52077 != 0)
            {
                Stream var10000 = VertexFormatElement.method_60848(this.field_52077);
                VertexFormat var10001 = this.format;
                Objects.requireNonNull(var10001);
                String string = (String)var10000.map(var10001::method_60837).collect(Collectors.joining(", "));
                throw new IllegalStateException("Missing elements in vertex: " + string);
            }
            else
            {
                if (this.field_52073 == VertexFormat.DrawMode.LINES || this.field_52073 == VertexFormat.DrawMode.LINE_STRIP)
                {
                    long l = this.field_52071.method_60808(this.field_52074);
                    MemoryUtil.memCopy(l - (long)this.field_52074, l, (long)this.field_52074);
                    ++this.vertexCount;
                }

            }
        }
    }

    private static void method_60797(long l, int i)
    {
        int j = ColorHelper.Abgr.method_60675(i);
        MemoryUtil.memPutInt(l, field_52070 ? j : Integer.reverseBytes(j));
    }

    private static void method_60801(long l, int i)
    {
        if (field_52070)
        {
            MemoryUtil.memPutInt(l, i);
        }
        else
        {
            MemoryUtil.memPutShort(l, (short)(i & '\uffff'));
            MemoryUtil.memPutShort(l + 2L, (short)(i >> 16 & '\uffff'));
        }

    }

    public VertexConsumer vertex(float f, float g, float h)
    {
        //return super.vertex(x, (float) (y + this.offsetY), z);

        long l = this.method_60805() + (long)this.field_52076[VertexFormatElement.field_52107.id()];
        this.field_52077 = this.field_52075;
        MemoryUtil.memPutFloat(l, f);
        MemoryUtil.memPutFloat(l + 4L, g);
        MemoryUtil.memPutFloat(l + 8L, h);
        return this;
    }

    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        long l = this.method_60798(VertexFormatElement.field_52108);
        if (l != -1L) {
            MemoryUtil.memPutByte(l, (byte)red);
            MemoryUtil.memPutByte(l + 1L, (byte)green);
            MemoryUtil.memPutByte(l + 2L, (byte)blue);
            MemoryUtil.memPutByte(l + 3L, (byte)alpha);
        }

        return this;
    }

    public VertexConsumer color(int argb)
    {
        long l = this.method_60798(VertexFormatElement.field_52108);
        if (l != -1L)
        {
            method_60797(l, argb);
        }

        return this;
    }

    public VertexConsumer texture(float u, float v)
    {
        long l = this.method_60798(VertexFormatElement.field_52109);
        if (l != -1L)
        {
            MemoryUtil.memPutFloat(l, u);
            MemoryUtil.memPutFloat(l + 4L, v);
        }

        return this;
    }

    public VertexConsumer method_60796(int i, int j)
    {
        return this.method_60799((short)i, (short)j, VertexFormatElement.field_52111);
    }

    public VertexConsumer overlay(int uv)
    {
        long l = this.method_60798(VertexFormatElement.field_52111);
        if (l != -1L)
        {
            method_60801(l, uv);
        }

        return this;
    }

    public VertexConsumer light(int u, int v)
    {
        return this.method_60799((short)u, (short)v, VertexFormatElement.field_52112);
    }

    public VertexConsumer method_60803(int i)
    {
        long l = this.method_60798(VertexFormatElement.field_52112);
        if (l != -1L)
        {
            method_60801(l, i);
        }

        return this;
    }

    private VertexConsumer method_60799(short s, short t, VertexFormatElement vertexFormatElement)
    {
        long l = this.method_60798(vertexFormatElement);
        if (l != -1L)
        {
            MemoryUtil.memPutShort(l, s);
            MemoryUtil.memPutShort(l + 2L, t);
        }

        return this;
    }

    public VertexConsumer normal(float x, float y, float z)
    {
        long l = this.method_60798(VertexFormatElement.field_52113);
        if (l != -1L)
        {
            MemoryUtil.memPutByte(l, method_60795(x));
            MemoryUtil.memPutByte(l + 1L, method_60795(y));
            MemoryUtil.memPutByte(l + 2L, method_60795(z));
        }

        return this;
    }

    private static byte method_60795(float f) {
        return (byte)((int)(MathHelper.clamp(f, -1.0F, 1.0F) * 127.0F) & 255);
    }

    public void vertex(float x, float y, float z, int i, float green, float blue, int j, int k, float v, float f, float g)
    {
        if (this.canSkipElementChecks)
        {
            long l = this.method_60805();
            MemoryUtil.memPutFloat(l + 0L, x);
            MemoryUtil.memPutFloat(l + 4L, y);
            MemoryUtil.memPutFloat(l + 8L, z);
            method_60797(l + 12L, i);
            MemoryUtil.memPutFloat(l + 16L, green);
            MemoryUtil.memPutFloat(l + 20L, blue);
            long m;
            if (this.hasOverlay)
            {
                method_60801(l + 24L, j);
                m = l + 28L;
            }
            else
            {
                m = l + 24L;
            }

            method_60801(m + 0L, k);
            MemoryUtil.memPutByte(m + 4L, method_60795(v));
            MemoryUtil.memPutByte(m + 5L, method_60795(f));
            MemoryUtil.memPutByte(m + 6L, method_60795(g));
        }
        else
        {
            VertexConsumer.super.vertex(x, y, z, i, green, blue, j, k, v, f, g);
        }
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
