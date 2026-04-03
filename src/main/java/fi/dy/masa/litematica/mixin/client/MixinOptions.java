package fi.dy.masa.litematica.mixin.client;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.litematica.render.LitematicaRenderer;

@Mixin(Options.class)
public abstract class MixinOptions
{
	@Inject(method = "save", at = @At("TAIL"))
	private void litematica_onOptionsSave(CallbackInfo ci)
	{
		// Sodium calls Options.save() directly
		if (IrisCompat.hasSodium())
		{
			LitematicaRenderer.getInstance().onResourcePackReload();
		}
	}
}
