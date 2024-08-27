package fi.dy.masa.litematica.mixin.post_rewrite;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBinding.class)
public interface IMixinKeyBinding
{
    @Accessor("boundKey")
    InputUtil.Key litematica_getBoundKey();
}
