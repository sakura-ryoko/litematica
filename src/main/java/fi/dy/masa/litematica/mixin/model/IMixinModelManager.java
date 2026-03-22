package fi.dy.masa.litematica.mixin.model;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelManager.class)
public interface IMixinModelManager
{
	@Accessor("atlasManager")
	AtlasManager litematica_getAtlasManager();

	@Accessor("playerSkinRenderCache")
	PlayerSkinRenderCache litematica_getPlayerSkinRenderCache();

	@Accessor("blockColors")
	BlockColors litematica_getBlockColors();
}
