package fi.dy.masa.litematica.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.litematica.render.schematic.BlockModelCacheSchematic;

@Mixin(Options.class)
public abstract class MixinOptions
{
	@Shadow protected Minecraft minecraft;

	@Inject(method = "save", at = @At("TAIL"))
	private void litematica_onOptionsSave(CallbackInfo ci)
	{
		// Sodium calls Options.save() directly
		if (IrisCompat.hasSodium() && this.minecraft.level != null)
		{
			BlockModelCacheSchematic.INSTANCE.onReloadResources();
		}
	}
}
