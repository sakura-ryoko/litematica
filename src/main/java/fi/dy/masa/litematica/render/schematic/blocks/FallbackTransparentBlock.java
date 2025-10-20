package fi.dy.masa.litematica.render.schematic.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class FallbackTransparentBlock extends Block
{
	public static final MapCodec<FallbackTransparentBlock> CODEC = createCodec(FallbackTransparentBlock::new);

	public FallbackTransparentBlock(Settings settings)
	{
		super(settings);
	}

	@Override
	protected MapCodec<? extends FallbackTransparentBlock> getCodec()
	{
		return CODEC;
	}

	@Override
	protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		return VoxelShapes.empty();
	}

	@Override
	protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos)
	{
		return 1.0F;
	}

	@Override
	protected boolean isTransparent(BlockState state)
	{
		return true;
	}
}
