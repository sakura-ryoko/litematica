package fi.dy.masa.litematica.schematic.container;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.serialization.Codec;

import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;

public interface ILitematicaBlockStatePalette
{
    @ApiStatus.Experimental
    Codec<? extends ILitematicaBlockStatePalette> codec();

    void setResizer(ILitematicaBlockStatePaletteResizer resizer);

    /**
     * Gets the palette id for the given block state and adds
     * the state to the palette if it doesn't exist there yet.
     */
    int idFor(BlockState state);

    /**
     * Gets the block state by the palette id.
     */
    @Nullable
    BlockState getBlockState(int indexKey);

    int getPaletteSize();

    /**
     * @deprecated use readFromData()
     */
    @Deprecated(forRemoval = true)
    default void readFromNBT(ListTag tagList)
    {
        this.readFromData(DataConverterNbt.fromVanillaList(tagList));
    }

    /**
     * @deprecated use writeToData()
     */
    @Deprecated(forRemoval = true)
    default ListTag writeToNBT()
    {
        return DataConverterNbt.toVanillaList(this.writeToData());
    }

    void readFromData(ListData tagList);

    ListData writeToData();

    /**
     * Sets the current mapping of the palette.
     * This is meant for reading the palette from file.
     * @param list ()
     * @return true if the mapping was set successfully, false if it failed
     */
    boolean setMapping(List<BlockState> list);

    List<BlockState> fromMapping();
}
