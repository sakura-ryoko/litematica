package fi.dy.masa.litematica.util;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public interface IFluidRendererInvoker
{
	void litematica$setOffsetY(float offset);

	void litematica$tesselate(final BlockAndTintGetter level, final BlockPos pos, final FluidRenderer.Output output, final BlockState blockState, final FluidState fluidState);
}
