package fi.dy.masa.litematica.mixin.block;

import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GlazedTerracottaBlock.class)
public abstract class MixinGlazedTerracottaBlock extends HorizontalDirectionalBlock
{
	protected MixinGlazedTerracottaBlock(Properties properties)
	{
		super(properties);
	}

//	@Override
//	protected @NonNull BlockState mirror(final @NonNull BlockState state, final @NonNull Mirror mirror)
//	{
//		// return state.rotate(mirror.getRotation(state.getValue(FACING)));
//		BlockState newState = super.mirror(state, mirror);
//
//		if (Configs.Generic.FIX_GLAZED_TERRACOTTA_MIRROR.getBooleanValue())
//		{
//			Direction oldFacing = state.getValue(FACING);
//			Direction newFacing = newState.getValue(FACING);
//
//			if (mirror == Mirror.LEFT_RIGHT && oldFacing.getAxis() == Direction.Axis.X)
//			{
//				newState = newState.setValue(FACING, newFacing.getOpposite());
//			}
//			else if (mirror == Mirror.FRONT_BACK && oldFacing.getAxis() == Direction.Axis.Z)
//			{
//				newState = newState.setValue(FACING, newFacing.getOpposite());
//			}
//		}
//
//		return newState;
//	}
}
