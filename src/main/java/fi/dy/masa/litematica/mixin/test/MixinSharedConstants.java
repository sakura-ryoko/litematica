package fi.dy.masa.litematica.mixin.test;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.malilib.MaLiLibReference;
import fi.dy.masa.litematica.Reference;

@ApiStatus.Internal
@VisibleForTesting
@Mixin(SharedConstants.class)
public class MixinSharedConstants
{
	@Shadow public static boolean IS_RUNNING_IN_IDE;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void litematica_sharedConstants(CallbackInfo ci)
	{
		IS_RUNNING_IN_IDE = Reference.LOCAL_DEBUG || MaLiLibReference.RUNNING_IN_IDE;
	}
}
