package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;

@Mixin(SignBlockEntity.class)
public interface IMixinSignBlockEntity
{
    @Accessor("frontText")
    SignText litematica$getFrontText();

    @Accessor("backText")
    SignText litematica$getBackText();
}
