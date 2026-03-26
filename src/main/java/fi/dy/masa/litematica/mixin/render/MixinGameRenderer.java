package fi.dy.masa.litematica.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.LitematicaRenderer;

@Mixin(GameRenderer.class)
public class MixinGameRenderer
{
	@Shadow @Final private Camera mainCamera;

	@Inject(method = "extractCamera", at = @At("TAIL"))
	private void litematica_updateCameraState(DeltaTracker deltaTracker, float worldPartialTicks, float cameraEntityPartialTicks, CallbackInfo ci,
	                                          @Local(name = "cameraState") CameraRenderState cameraState)
	{
		LitematicaRenderer.getInstance().updateCameraState(this.mainCamera, cameraEntityPartialTicks, cameraState);
	}
}
