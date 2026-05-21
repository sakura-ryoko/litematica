package fi.dy.masa.litematica.mixin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FaceAttachedHorizontalDirectionalBlock.class)
public interface IMixinWallMountedBlock
{
    @Invoker("canSurvive")
    boolean litematica_invokeCanPlaceAt(BlockState state, LevelReader level, BlockPos pos);
}
