package fi.dy.masa.litematica.mixin.model;

import java.util.Map;

import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockStateModelSet.class)
public interface IMixinBlockStateModelSet
{
	@Accessor("modelByState")
	Map<BlockState, BlockStateModel> litematica_getModelMap();

	@Accessor("missingModel")
	BlockStateModel litematica_getMissingModel();
}
