package fi.dy.masa.litematica.mixin.block;

import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractBlock.class)
public interface IMixinAbstractBlock
{
    // todo 1.21.4+
//    @Invoker("getPickStack")
//    ItemStack litematica_getPickStack(WorldView worldView, BlockPos blockPos, BlockState blockState, boolean bl);
}
