package fi.dy.masa.litematica.mixin.screen;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface IMixinAbstractContainerScreen
{
    @Accessor("leftPos")
    int litematica_getX();

    @Accessor("topPos")
    int litematica_getY();
}
