package fi.dy.masa.litematica.mixin.model;

import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.LitematicaRenderer;

@Mixin(PackRepository.class)
public class MixinPackRepository
{
	@Inject(method = "reload", at = @At("TAIL"))
	private void litematica_onBlockRenderManagerReload(CallbackInfo ci)
	{
		LitematicaRenderer.getInstance().onResourcePackReload();
	}
}
