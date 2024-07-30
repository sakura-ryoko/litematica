package fi.dy.masa.litematica.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WallMountedBlock.class)
public interface IMixinWallMountedBlock
{

    @Invoker("canPlaceAt")
    boolean invokeCanPlaceAt(BlockState state, WorldView world, BlockPos pos);
}
