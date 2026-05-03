package fi.dy.masa.litematica.mixin.model;

import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderDispatcher.class)
public interface IMixinEntityRenderDispatcher
{
	@Accessor("blockModelResolver")
	BlockModelResolver litematica_getBlockModelResolver();
}
