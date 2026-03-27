package fi.dy.masa.litematica.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;

import fi.dy.masa.litematica.util.IFluidRendererInvoker;

@Mixin(FluidRenderer.class)
public abstract class MixinFluidRenderer implements IFluidRendererInvoker
{
	@Shadow public abstract void tesselate(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState);
	@Unique private float offsetY = 0.0f;

	@Override
	public void litematica$setOffsetY(float offset)
	{
		this.offsetY = offset;
	}

	@Override
	public void litematica$tesselate(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState)
	{
		this.tesselate(level, pos, output, blockState, fluidState);
	}

//	@WrapOperation(method = "tesselate",
//	               at = @At(value = "INVOKE",
//	                 target = "Lnet/minecraft/client/renderer/block/FluidRenderer$Output;getBuilder(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
//	private VertexConsumer litematica_onLiquidFixPre(FluidRenderer.Output instance, ChunkSectionLayer chunkSectionLayer, Operation<VertexConsumer> original)
//	{
//		VertexConsumer consumer = original.call(instance, chunkSectionLayer);
//
//		if (this.offsetY == Float.MIN_VALUE)
//		{
//			return consumer;
//		}
//
//		if (this.offsetY != 0.0F)
//		{
//			System.out.printf("litematica_onLiquidFixPre(): offset %04f\n", this.offsetY);
//			BufferBuilderPatch builder = (BufferBuilderPatch) consumer;
//			builder.setYOffset(this.offsetY);
//			return builder;
//		}
//
//		return consumer;
//	}
//
//	@Inject(method = "tesselate", at = @At("TAIL"))
//	private void litematica_onLiquidFixPost(BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState, CallbackInfo ci,
//	                                        @Local(name = "builder") VertexConsumer builder)
//	{
//		if (this.offsetY == Float.MIN_VALUE) { return; }
//		BufferBuilderPatch patch = (BufferBuilderPatch) builder;
//		patch.setYOffset(0.0f);
//		this.offsetY = Float.MIN_VALUE;
//	}

	@WrapOperation(method = "vertex(Lcom/mojang/blaze3d/vertex/VertexConsumer;FFFIFFI)V",
	               at = @At(value = "INVOKE",
	                        target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V"))
	private void litematica_modifyVertexArgs(VertexConsumer instance,
	                                         float x, float y, float z, int color, float u, float v,
	                                         int overlayCoords, int lightCoords,
	                                         float nx, float ny, float nz, Operation<Void> original)
	{
		original.call(instance, x, (y + this.offsetY), z, color, u, v, overlayCoords, lightCoords, nx, ny, nz);
	}
}
