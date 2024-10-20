package fi.dy.masa.litematica.mixin;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Post Re-Write code
 */
@Mixin(KeyBinding.class)
public interface IMixinKeyBinding
{
    @Accessor("boundKey")
    InputUtil.Key litematica_getBoundKey();
}
