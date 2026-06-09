package fi.dy.masa.litematica.mixin.hud;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.LitematicaDebugHud;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;

@Mixin(DebugScreenEntryList.class)
public abstract class MixinDebugScreenEntryList
{
	@Inject(method = "rebuildCurrentList", at = @At("TAIL"))
	private void litematica_updateVisibleEntries(CallbackInfo ci)
	{
		LitematicaDebugHud.INSTANCE.checkConfig();
	}
}
