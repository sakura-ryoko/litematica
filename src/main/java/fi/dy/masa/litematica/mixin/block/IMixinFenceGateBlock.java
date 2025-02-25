package fi.dy.masa.litematica.mixin.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;

@Mixin(FenceGateBlock.class)
public interface IMixinFenceGateBlock
{
    @Invoker("isWall")
    boolean litematica_invokeIsWall(BlockState state);
}
