package fi.dy.masa.litematica.schematic.container;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.netty.buffer.ByteBuf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.data.tag.util.DataTypeUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

public class LitematicaBlockStatePaletteLinear implements ILitematicaBlockStatePalette
{
    public static final Codec<LitematicaBlockStatePaletteLinear> CODEC = RecordCodecBuilder.create(
            inst -> inst.group(
                    PrimitiveCodec.INT.fieldOf("Bits").forGetter(get -> get.bits),
                    Codec.list(BlockState.CODEC).fieldOf("StatePalette").forGetter(LitematicaBlockStatePaletteLinear::fromMapping)
            ).apply(inst, LitematicaBlockStatePaletteLinear::new)
    );
    public static final StreamCodec<ByteBuf, LitematicaBlockStatePaletteLinear> PACKET_CODEC = new StreamCodec<>()
    {
        @Override
        public void encode(@Nonnull ByteBuf buf, LitematicaBlockStatePaletteLinear value)
        {
            ByteBufCodecs.INT.encode(buf, value.bits);
            ByteBufCodecs.TRUSTED_TAG.encode(buf, DataConverterNbt.toVanillaList(value.writeToData()));
        }

        @Override
        public @Nonnull LitematicaBlockStatePaletteLinear decode(@Nonnull ByteBuf buf)
        {
            Integer bitsIn = ByteBufCodecs.INT.decode(buf);
            Tag nbt = ByteBufCodecs.TRUSTED_TAG.decode(buf);
            return new LitematicaBlockStatePaletteLinear(bitsIn, DataConverterNbt.fromVanillaList((ListTag) nbt));
        }
    };
    private final BlockState[] states;
    private ILitematicaBlockStatePaletteResizer resizeHandler;
    private final int bits;
    private int currentSize;

    public LitematicaBlockStatePaletteLinear(int bitsIn, ILitematicaBlockStatePaletteResizer resizeHandler)
    {
        this.states = new BlockState[1 << bitsIn];
        this.bits = bitsIn;
        this.resizeHandler = resizeHandler;
    }

    private LitematicaBlockStatePaletteLinear(int bitsIn, List<BlockState> list)
    {
        this.bits = bitsIn;
        this.resizeHandler = null;
        this.states = new BlockState[1 << bitsIn];
        this.setMapping(list);
    }

    private LitematicaBlockStatePaletteLinear(int bitsIn, ListData list)
    {
        this.bits = bitsIn;
        this.resizeHandler = null;
        this.states = new BlockState[1 << bitsIn];
        this.readFromData(list);
    }

    @Override
    public Codec<LitematicaBlockStatePaletteLinear> codec()
    {
        return CODEC;
    }

    @Override
    public void setResizer(ILitematicaBlockStatePaletteResizer resizer)
    {
        this.resizeHandler = resizer;
    }

    @Override
    public int idFor(BlockState state)
    {
        for (int i = 0; i < this.currentSize; ++i)
        {
            if (this.states[i] == state)
            {
                return i;
            }
        }

        final int size = this.currentSize;

        if (size < this.states.length)
        {
            this.states[size] = state;
            ++this.currentSize;
            return size;
        }
        else
        {
            return this.resizeHandler.onResize(this.bits + 1, state);
        }
    }

    @Override
    @Nullable
    public BlockState getBlockState(int indexKey)
    {
        return indexKey >= 0 && indexKey < this.currentSize ? this.states[indexKey] : null;
    }

    @Override
    public int getPaletteSize()
    {
        return this.currentSize;
    }

    private void requestNewId(BlockState state)
    {
        final int size = this.currentSize;

        if (size < this.states.length)
        {
            this.states[size] = state;
            ++this.currentSize;
        }
        else
        {
            int newId = this.resizeHandler.onResize(this.bits + 1, LitematicaBlockStateContainer.AIR_BLOCK_STATE);

            if (newId <= size)
            {
                this.states[size] = state;
                ++this.currentSize;
            }
        }
    }

    @Override
    public void readFromData(ListData tagList)
    {
        //RegistryEntryLookup<Block> lookup = Registries.BLOCK.getReadOnlyWrapper();
//        HolderGetter<Block> lookup = SchematicWorldHandler.INSTANCE.getRegistryManager().lookupOrThrow(Registries.BLOCK);
        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundData tag = tagList.getCompoundAt(i);
//            BlockState state = NbtUtils.readBlockState(lookup, DataConverterNbt.toVanillaCompound(tag));
            BlockState state = DataTypeUtils.readBlockStateFromTag(tag, SchematicWorldHandler.INSTANCE.getRegistryManager());

            if (i > 0 || state != LitematicaBlockStateContainer.AIR_BLOCK_STATE)
            {
                this.requestNewId(state);
            }
        }
    }

    @Override
    public ListData writeToData()
    {
        ListData tagList = new ListData();

        for (int id = 0; id < this.currentSize; ++id)
        {
            BlockState state = this.states[id];

            if (state == null)
            {
                state = LitematicaBlockStateContainer.AIR_BLOCK_STATE;
            }

//            CompoundTag tag = NbtUtils.writeBlockState(state);
            CompoundData tag = DataTypeUtils.writeBlockStateToTag(state);
            tagList.add(tag);
        }

        return tagList;
    }

    @Override
    public boolean setMapping(List<BlockState> list)
    {
        final int size = list.size();

        if (size <= this.states.length)
        {
            for (int id = 0; id < size; ++id)
            {
                this.states[id] = list.get(id);
            }

            this.currentSize = size;

            return true;
        }

        return false;
    }

    @Override
    public List<BlockState> fromMapping()
    {
        List<BlockState> list = new ArrayList<>();

        for (int id = 0; id < this.currentSize; ++id)
        {
            BlockState state = this.states[id];

            if (state == null)
            {
                state = LitematicaBlockStateContainer.AIR_BLOCK_STATE;
            }

            list.add(state);
        }

        return list;
    }
}
