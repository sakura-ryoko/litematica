package fi.dy.masa.litematica.render.schematic.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

/**
 * Cloned to support
 */
public class FallbackPaneBlock extends HorizontalConnectingBlock
{
	public static final MapCodec<FallbackPaneBlock> CODEC = createCodec(FallbackPaneBlock::new);

	@Override
	public MapCodec<? extends FallbackPaneBlock> getCodec()
	{
		return CODEC;
	}

	protected FallbackPaneBlock(AbstractBlock.Settings settings)
	{
		super(2.0F, 16.0F, 2.0F, 16.0F, 16.0F, settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(WATERLOGGED, false));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx)
	{
		BlockView world = ctx.getWorld();
		BlockPos pos = ctx.getBlockPos();
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		BlockPos posNorth = pos.north();
		BlockPos posSouth = pos.south();
		BlockPos posWest = pos.west();
		BlockPos posEast = pos.east();
		BlockState stateNorth = world.getBlockState(posNorth);
		BlockState stateSouth = world.getBlockState(posSouth);
		BlockState stateWest = world.getBlockState(posWest);
		BlockState stateEast = world.getBlockState(posEast);

		return this.getDefaultState()
		           .with(NORTH, this.connectsTo(stateNorth, stateNorth.isSideSolidFullSquare(world, posNorth, Direction.SOUTH)))
		           .with(SOUTH, this.connectsTo(stateSouth, stateSouth.isSideSolidFullSquare(world, posSouth, Direction.NORTH)))
		           .with(WEST, this.connectsTo(stateWest, stateWest.isSideSolidFullSquare(world, posWest, Direction.EAST)))
		           .with(EAST, this.connectsTo(stateEast, stateEast.isSideSolidFullSquare(world, posEast, Direction.WEST)))
		           .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
	}

	@Override
	protected BlockState getStateForNeighborUpdate(
			  BlockState state, Direction direction,
			  BlockState neighborState, WorldAccess world,
			  BlockPos pos, BlockPos neighborPos)
	{
		if (state.get(WATERLOGGED))
		{
			world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}

		return direction.getAxis().isHorizontal()
		       ? state.with(FACING_PROPERTIES.get(direction), this.connectsTo(neighborState, neighborState.isSideSolidFullSquare(world, neighborPos, direction.getOpposite())))
		       : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
	}

	@Override
	protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
	{
		return VoxelShapes.empty();
	}

	@Override
	protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction)
	{
		if (stateFrom.isOf(this))
		{
			if (!direction.getAxis().isHorizontal())
			{
				return true;
			}

			if ((Boolean) state.get((Property<?>) FACING_PROPERTIES.get(direction)) && (Boolean) stateFrom.get((Property<?>) FACING_PROPERTIES.get(direction.getOpposite())))
			{
				return true;
			}
		}

		return super.isSideInvisible(state, stateFrom, direction);
	}

	public final boolean connectsTo(BlockState state, boolean sideSolidFullSquare)
	{
		return !cannotConnect(state) && sideSolidFullSquare || state.getBlock() instanceof PaneBlock || state.isIn(BlockTags.WALLS);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
	{
		builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
	}
}
