package fi.dy.masa.litematica.mixin.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockBehaviour.class)
public interface IMixinAbstractBlock
{
    @Invoker("getCloneItemStack")
    ItemStack litematica_getPickStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData);
}
