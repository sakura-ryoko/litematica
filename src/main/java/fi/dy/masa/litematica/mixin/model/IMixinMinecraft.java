package fi.dy.masa.litematica.mixin.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface IMixinMinecraft
{
	@Accessor("itemModelResolver")
	ItemModelResolver litematica_getItemModelResolver();
}
