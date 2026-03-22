package fi.dy.masa.litematica.mixin.model;

import java.util.Map;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelSet;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockModelSet.class)
public interface IMixinBlockModelSet
{
	@Accessor("blockModelByStateCache")
	Map<BlockState, BlockModel> litematica_getBlockModelCache();

	@Accessor("fallback")
	BlockStateModelSet litematica_getFallback();

	@Accessor("blockColors")
	BlockColors litematica_getBlockColors();
}
