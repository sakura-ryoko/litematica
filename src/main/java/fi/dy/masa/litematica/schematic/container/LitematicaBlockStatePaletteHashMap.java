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
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.data.tag.util.DataTypeUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

public class LitematicaBlockStatePaletteHashMap implements ILitematicaBlockStatePalette
{
    public static final Codec<LitematicaBlockStatePaletteHashMap> CODEC = RecordCodecBuilder.create(
            inst -> inst.group(
                    PrimitiveCodec.INT.fieldOf("Bits").forGetter(get -> get.bits),
                    Codec.list(BlockState.CODEC).fieldOf("StatePalette").forGetter(LitematicaBlockStatePaletteHashMap::fromMapping)
            ).apply(inst, LitematicaBlockStatePaletteHashMap::new)
    );
    public static final StreamCodec<ByteBuf, LitematicaBlockStatePaletteHashMap> PACKET_CODEC = new StreamCodec<>()
    {
        @Override
        public void encode(@Nonnull ByteBuf buf, LitematicaBlockStatePaletteHashMap value)
        {
            ByteBufCodecs.INT.encode(buf, value.bits);
            ByteBufCodecs.TRUSTED_TAG.encode(buf, DataConverterNbt.toVanillaList(value.writeToData()));
        }

        @Override
        public @Nonnull LitematicaBlockStatePaletteHashMap decode(@Nonnull ByteBuf buf)
        {
            Integer bitsIn = ByteBufCodecs.INT.decode(buf);
            Tag nbt = ByteBufCodecs.TRUSTED_TAG.decode(buf);
            return new LitematicaBlockStatePaletteHashMap(bitsIn, DataConverterNbt.fromVanillaList((ListTag) nbt));
        }
    };
    private final CrudeIncrementalIntIdentityHashBiMap<BlockState> statePaletteMap;
    private ILitematicaBlockStatePaletteResizer paletteResizer;
    private final int bits;

    public LitematicaBlockStatePaletteHashMap(int bitsIn, ILitematicaBlockStatePaletteResizer paletteResizer)
    {
        this.bits = bitsIn;
        this.paletteResizer = paletteResizer;
        this.statePaletteMap = CrudeIncrementalIntIdentityHashBiMap.create(1 << bitsIn);
    }

    private LitematicaBlockStatePaletteHashMap(int bitsIn, List<BlockState> list)
    {
        this.bits = bitsIn;
        this.paletteResizer = null;
        this.statePaletteMap = CrudeIncrementalIntIdentityHashBiMap.create(1 << bitsIn);
        this.setMapping(list);
    }

    private LitematicaBlockStatePaletteHashMap(int bitsIn, ListData list)
    {
        this.bits = bitsIn;
        this.paletteResizer = null;
        this.statePaletteMap = CrudeIncrementalIntIdentityHashBiMap.create(1 << bitsIn);
        this.readFromData(list);
    }

    @Override
    public Codec<LitematicaBlockStatePaletteHashMap> codec()
    {
        return CODEC;
    }

    @Override
    public void setResizer(ILitematicaBlockStatePaletteResizer resizer)
    {
        this.paletteResizer = resizer;
    }

    @Override
    public int idFor(BlockState state)
    {
        int i = this.statePaletteMap.getId(state);

        if (i == -1)
        {
            i = this.statePaletteMap.add(state);

            if (i >= (1 << this.bits))
            {
                i = this.paletteResizer.onResize(this.bits + 1, state);
            }
        }

        return i;
    }

    @Override
    @Nullable
    public BlockState getBlockState(int indexKey)
    {
        return this.statePaletteMap.byId(indexKey);
    }

    @Override
    public int getPaletteSize()
    {
        return this.statePaletteMap.size();
    }

    private void requestNewId(BlockState state)
    {
        final int origId = this.statePaletteMap.add(state);

        if (origId >= (1 << this.bits))
        {
            int newId = this.paletteResizer.onResize(this.bits + 1, LitematicaBlockStateContainer.AIR_BLOCK_STATE);

            if (newId <= origId)
            {
                this.statePaletteMap.add(state);
            }
        }
    }

    @Override
    public void readFromData(ListData tagList)
    {
        //RegistryEntryLookup<Block> lookup = Registries.BLOCK.getReadOnlyWrapper();
//        HolderGetter<Block> lookup = SchematicWorldHandler.INSTANCE.getRegistryManager().lookupOrThrow(Registries.BLOCK);
        // Ugly, but it should work, without changing the ILitematicaBlockStatePalette interface.

        final int size = tagList.size();

        for (int i = 0; i < size; ++i)
        {
            CompoundData tag = tagList.getCompoundAt(i);
//            BlockState state = NbtUtils.readBlockState(lookup, tag);
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

        for (int id = 0; id < this.statePaletteMap.size(); ++id)
        {
            BlockState state = this.statePaletteMap.byId(id);

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
    public List<BlockState> fromMapping()
    {
        List<BlockState> list = new ArrayList<>();

        for (int i = 0; i < this.statePaletteMap.size(); i++)
        {
            BlockState state = this.statePaletteMap.byId(i);

            if (state == null)
            {
                state = LitematicaBlockStateContainer.AIR_BLOCK_STATE;
            }

            list.add(state);
        }

        return list;
    }

    @Override
    public boolean setMapping(List<BlockState> list)
    {
        this.statePaletteMap.clear();

        for (BlockState blockState : list)
        {
            this.statePaletteMap.add(blockState);
        }

        return true;
    }
}
