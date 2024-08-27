package fi.dy.masa.litematica.util.post_rewrite.malilib;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PlacementUtils
{
    /**
     * The checkMaterial flag is provided to deal with the vanilla inconsistency of checking the replaceability
     * of the block versus that of the material. ItemBlock offsets the position based on the replaceability
     * of the block, and then later checks if the block can be placed in the new offset position
     * based on the replaceability of the material instead. If <b>checkMaterial</b> is true, then the
     * replaceability of the material can override the non-replaceability of the block for the return value.
     */
    public static boolean isReplaceable(World world, BlockPos pos, boolean checkMaterial)
    {
        BlockState state = world.getBlockState(pos);

        return state.canPlaceAt(world, pos) ||
                (checkMaterial && state.isReplaceable());
    }
}
