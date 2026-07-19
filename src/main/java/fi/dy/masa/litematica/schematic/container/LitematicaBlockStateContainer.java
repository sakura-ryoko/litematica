package fi.dy.masa.litematica.schematic.container;

import java.util.Arrays;
import java.util.stream.LongStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;

public class LitematicaBlockStateContainer implements ILitematicaBlockStatePaletteResizer
{
    private static final Codec<ILitematicaBlockStatePalette> PALETTE_CODEC = Codec.either(LitematicaBlockStatePaletteHashMap.CODEC, LitematicaBlockStatePaletteLinear.CODEC)
                                                                                     .xmap(
                                                                                             either -> either.map(type -> type, type -> type),
                                                                                             type ->
                                                                                                     type instanceof LitematicaBlockStatePaletteHashMap hashMap ? Either.left(hashMap) :
                                                                                                     type instanceof LitematicaBlockStatePaletteLinear linear ? Either.right(linear) : null
                                                                                     );
    public static final Codec<LitematicaBlockStateContainer> CODEC = RecordCodecBuilder.create(
            inst -> inst.group(
                    PrimitiveCodec.INT.fieldOf("Bits").forGetter(get -> get.bits),
                    LitematicaBitArray.CODEC.fieldOf("Storage").forGetter(get -> get.storage),
                    PALETTE_CODEC.fieldOf("Palette").forGetter(get -> get.palette),
                    PrimitiveCodec.INT.fieldOf("SizeX").forGetter(get -> get.sizeX),
                    PrimitiveCodec.INT.fieldOf("SizeY").forGetter(get -> get.sizeY),
                    PrimitiveCodec.INT.fieldOf("SizeZ").forGetter(get -> get.sizeZ),
                    PrimitiveCodec.INT.fieldOf("SizeLayer").forGetter(get -> get.sizeLayer),
                    Vec3i.CODEC.fieldOf("Size").forGetter(get -> get.size),
                    PrimitiveCodec.LONG.fieldOf("TotalVolume").forGetter(get -> get.totalVolume),
                    PrimitiveCodec.LONG_STREAM.optionalFieldOf("BlockCounts", LongStream.empty()).forGetter(get -> Arrays.stream(get.blockCounts))
            ).apply(inst, LitematicaBlockStateContainer::new)
    );
    public static final StreamCodec<ByteBuf, LitematicaBlockStateContainer> PACKET_CODEC = new StreamCodec<>()
    {
        @Override
        public void encode(@Nonnull ByteBuf buf, LitematicaBlockStateContainer value)
        {
            LitematicaBitArray.PACKET_CODEC.encode(buf, value.storage);
            ByteBufCodecs.INT.encode(buf, value.bits);
            if (value.palette instanceof LitematicaBlockStatePaletteHashMap hash)
            {
                LitematicaBlockStatePaletteHashMap.PACKET_CODEC.encode(buf, hash);
            }
            else if (value.palette instanceof LitematicaBlockStatePaletteLinear linear)
            {
                LitematicaBlockStatePaletteLinear.PACKET_CODEC.encode(buf, linear);
            }
            else throw new RuntimeException();

            ByteBufCodecs.INT.encode(buf, value.sizeX);
            ByteBufCodecs.INT.encode(buf, value.sizeY);
            ByteBufCodecs.INT.encode(buf, value.sizeZ);
            ByteBufCodecs.INT.encode(buf, value.sizeLayer);
            Vec3i.STREAM_CODEC.encode(buf, value.size);
            ByteBufCodecs.LONG.encode(buf, value.totalVolume);
            ByteBufCodecs.LONG_ARRAY.encode(buf, value.blockCounts);
        }

        @Override
        public @Nonnull LitematicaBlockStateContainer decode(@Nonnull ByteBuf buf)
        {
            LitematicaBitArray storage = LitematicaBitArray.PACKET_CODEC.decode(buf);
            int bits = ByteBufCodecs.INT.decode(buf);
            ILitematicaBlockStatePalette palette;

            if (bits <= 4)
            {
                palette = LitematicaBlockStatePaletteLinear.PACKET_CODEC.decode(buf);
            }
            else
            {
                palette = LitematicaBlockStatePaletteHashMap.PACKET_CODEC.decode(buf);
            }

            return new LitematicaBlockStateContainer(
                    bits, storage, palette,
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    Vec3i.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.LONG.decode(buf),
                    Arrays.stream(ByteBufCodecs.LONG_ARRAY.decode(buf))
            );
        }
    };

    public static final BlockState AIR_BLOCK_STATE = Blocks.AIR.defaultBlockState();
    protected LitematicaBitArray storage;
    protected ILitematicaBlockStatePalette palette;
    protected final Vec3i size;
    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;
    protected final int sizeLayer;
    protected final long totalVolume;
    protected int bits;
    /** Note: This is currently only used for the temporary Sponge schematic support */
    protected long[] blockCounts = new long[0];

    public LitematicaBlockStateContainer(int sizeX, int sizeY, int sizeZ)
    {
        this(sizeX, sizeY, sizeZ, 2, null);
    }

    public LitematicaBlockStateContainer(Vec3i size, int bits, @Nullable long[] backingLongArray)
    {
        this(size.getX(), size.getY(), size.getZ(), bits, backingLongArray);
    }

    public LitematicaBlockStateContainer(int sizeX, int sizeY, int sizeZ, int bits, @Nullable long[] backingLongArray)
    {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.sizeLayer = sizeX * sizeZ;
        this.totalVolume = (long) this.sizeX * (long) this.sizeY * (long) this.sizeZ;
        this.size = new Vec3i(this.sizeX, this.sizeY, this.sizeZ);

        this.setBits(bits, backingLongArray);
    }

    private LitematicaBlockStateContainer(Integer bits, LitematicaBitArray bitArray, ILitematicaBlockStatePalette palette, Integer x, Integer y, Integer z, Integer layer, Vec3i size, Long volume, LongStream counts)
    {
        this.bits = bits;
        this.storage = bitArray;
        this.palette = palette;
        this.sizeX = x;
        this.sizeY = y;
        this.sizeZ = z;
        this.sizeLayer = layer;
        this.size = size;
        this.totalVolume = volume;

        if (!counts.equals(LongStream.empty()))
        {
            this.blockCounts = counts.toArray();
        }
    }

    public Vec3i getSize()
    {
        return this.size;
    }

    public LitematicaBitArray getArray()
    {
        return this.storage;
    }

    public long[] getBlockCounts()
    {
        return this.blockCounts;
    }

    public BlockState get(int x, int y, int z)
    {
        BlockState state = this.palette.getBlockState(this.storage.getAt(this.getIndex(x, y, z)));
        return state == null ? AIR_BLOCK_STATE : state;
    }

    public void set(int x, int y, int z, BlockState state)
    {
        int id = this.palette.idFor(state);
        this.storage.setAt(this.getIndex(x, y, z), id);
    }

    protected void set(int index, BlockState state)
    {
        int id = this.palette.idFor(state);
        this.storage.setAt(index, id);
    }

    protected int getIndex(int x, int y, int z)
    {
        return (y * this.sizeLayer) + z * this.sizeX + x;
    }

    protected void setBits(int bitsIn, @Nullable long[] backingLongArray)
    {
        if (bitsIn != this.bits)
        {
            this.bits = bitsIn;

            if (this.bits <= 4)
            {
                this.bits = Math.max(2, this.bits);
                this.palette = new LitematicaBlockStatePaletteLinear(this.bits, this);
            }
            else
            {
                this.palette = new LitematicaBlockStatePaletteHashMap(this.bits, this);
            }

            this.palette.idFor(AIR_BLOCK_STATE);

            if (backingLongArray != null)
            {
                this.storage = new LitematicaBitArray(this.bits, this.totalVolume, backingLongArray);
            }
            else
            {
                this.storage = new LitematicaBitArray(this.bits, this.totalVolume);
            }
        }
    }

    @Override
    public int onResize(int bits, BlockState state)
    {
        LitematicaBitArray oldStorage = this.storage;
        ILitematicaBlockStatePalette oldPalette = this.palette;
        final long storageLength = oldStorage.size();

        this.setBits(bits, null);

        LitematicaBitArray newStorage = this.storage;

        for (long index = 0; index < storageLength; ++index)
        {
            newStorage.setAt(index, oldStorage.getAt(index));
        }

//        this.palette.readFromNBT(oldPalette.writeToNBT());
        this.palette.readFromData(oldPalette.writeToData());

        return this.palette.idFor(state);
    }

    public long[] getBackingLongArray()
    {
        return this.storage.getBackingLongArray();
    }

    public ILitematicaBlockStatePalette getPalette()
    {
        return this.palette;
    }

    @Deprecated(forRemoval = true)
    public static LitematicaBlockStateContainer createFrom(ListTag palette, long[] blockStates, BlockPos size)
    {
        return createFrom(DataConverterNbt.fromVanillaList(palette), blockStates, size);
    }

    public static LitematicaBlockStateContainer createFrom(ListData palette, long[] blockStates, BlockPos size)
    {
        int bits = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(palette.size() - 1));
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(size.getX(), size.getY(), size.getZ(), bits, blockStates);
//        container.palette.readFromNBT(palette);
        container.palette.readFromData(palette);
        return container;
    }

    @Nullable
    public static LitematicaBlockStateContainer createContainer(int paletteSize, byte[] blockData, Vec3i size)
    {
        int bits = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));
        SpongeBlockstateConverterResults results = convertVarIntByteArrayToPackedLongArray(size, bits, blockData);
        LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(size, bits, results.backingArray);
        //container.palette = createPalette(bits, container);
        container.blockCounts = results.blockCounts;
        return container;
    }

    public static SpongeBlockstateConverterResults convertVarIntByteArrayToPackedLongArray(Vec3i size, int bits, byte[] blockStates)
    {
        int volume = size.getX() * size.getY() * size.getZ();
        LitematicaBitArray bitArray = new LitematicaBitArray(bits, volume);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(blockStates));
        long[] blockCounts = new long[1 << bits];

        for (int i = 0; i < volume; ++i)
        {
            int id = buf.readVarInt();
            bitArray.setAt(i, id);
            ++blockCounts[id];
        }

        return new SpongeBlockstateConverterResults(bitArray.getBackingLongArray(), blockCounts);
    }

    public static class SpongeBlockstateConverterResults
    {
        public final long[] backingArray;
        public final long[] blockCounts;

        protected SpongeBlockstateConverterResults(long[] backingArray, long[] blockCounts)
        {
            this.backingArray = backingArray;
            this.blockCounts = blockCounts;
        }
    }
}
