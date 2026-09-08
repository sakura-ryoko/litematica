package fi.dy.masa.litematica.mixin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RedstoneWireBlock.class)
public interface IMixinRedstoneWireBlock
{
    @Invoker("getConnectionState")
    BlockState litematica_GetPlacementState(BlockGetter level, BlockState state, BlockPos pos);
}
