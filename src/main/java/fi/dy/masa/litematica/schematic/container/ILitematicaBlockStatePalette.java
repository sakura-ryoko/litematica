package fi.dy.masa.litematica.schematic.container;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.util.data.tag.ListData;

public interface ILitematicaBlockStatePalette
{
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
