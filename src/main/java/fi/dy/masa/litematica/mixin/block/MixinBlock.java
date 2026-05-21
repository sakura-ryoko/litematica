package fi.dy.masa.litematica.mixin.block;

import java.util.function.Supplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.util.WorldUtils;

@Mixin(Block.class)
public class MixinBlock
{
    @Inject(method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private static void litematica_preventItemDrops(Level level,
                                                    Supplier<ItemEntity> entityFactory,
                                                    ItemStack itemStack,
                                                    CallbackInfo ci)
    {
        if (WorldUtils.shouldPreventBlockUpdates(level))
        {
            ci.cancel();
        }
    }
}
