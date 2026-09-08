package fi.dy.masa.litematica.mixin.render;

import org.jetbrains.annotations.ApiStatus;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.UniformType;
import net.minecraft.client.renderer.BindGroupLayouts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.render.LitematicaPipelines;

@ApiStatus.Internal
@Mixin(value = BindGroupLayouts.class, priority = 600)
public class MixinBindGroupLayouts
{
	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void registerBindGroups(CallbackInfo ci)
	{
		LitematicaPipelines.LEGACY_TERRAIN_GROUP = BindGroupLayout.builder().withUniform("LegacyTerrainFix", UniformType.UNIFORM_BUFFER).build();
	}
}
