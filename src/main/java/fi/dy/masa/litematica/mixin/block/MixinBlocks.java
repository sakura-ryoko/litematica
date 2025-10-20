package fi.dy.masa.litematica.mixin.block;

import net.minecraft.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.schematic.blocks.FallbackBlocks;

@Mixin(Blocks.class)
public class MixinBlocks
{
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void litematica_registerBlocks(CallbackInfo ci)
	{
		FallbackBlocks.register();
	}
}
