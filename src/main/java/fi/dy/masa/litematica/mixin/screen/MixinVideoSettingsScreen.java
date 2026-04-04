package fi.dy.masa.litematica.mixin.screen;

import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.litematica.render.schematic.BlockModelCacheSchematic;

@Mixin(VideoSettingsScreen.class)
public abstract class MixinVideoSettingsScreen
{
	@Inject(method = "removed", at = @At("TAIL"))
	private void litematica_onVideoSettingsClose(CallbackInfo ci)
	{
		if (!IrisCompat.hasSodium())
		{
			BlockModelCacheSchematic.INSTANCE.onReloadResources();
		}
	}
}
