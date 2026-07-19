package fi.dy.masa.litematica.schematic.conversion;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

import fi.dy.masa.malilib.util.data.tag.CompoundData;

public interface IBlockReaderWithData extends BlockGetter
{
    @Nullable
    CompoundData getBlockEntityData(BlockPos pos);

    // TODO 1.17: These shouldn't matter here... right?
    @Override
    default int getHeight()
    {
        return 384;
    }

    @Override
    default int getMinY()
    {
        return -64;
    }
}
