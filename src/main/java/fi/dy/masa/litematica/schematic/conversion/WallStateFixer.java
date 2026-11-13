package fi.dy.masa.litematica.schematic.conversion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// Walls did not store adjacent sides in blockstate prior to the flattening, and WallSides were
// changed to none/low/tall (v2503)
public class WallStateFixer implements SchematicConversionFixers.IStateFixer
{
    public static final WallStateFixer INSTANCE = new WallStateFixer();

    private static final VoxelShape SHAPE_PILLAR = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape SHAPE_NORTH = Block.box(7.0D, 0.0D, 0.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(7.0D, 0.0D, 7.0D, 9.0D, 16.0D, 16.0D);
    private static final VoxelShape SHAPE_WEST = Block.box(0.0D, 0.0D, 7.0D, 9.0D, 16.0D, 9.0D);
    private static final VoxelShape SHAPE_EAST = Block.box(7.0D, 0.0D, 7.0D, 16.0D, 16.0D, 9.0D);

    @Override
    public BlockState fixState(IBlockReaderWithData reader, BlockState state, BlockPos pos)
    {
        BlockGetter world = reader;
        FluidState fluidState = state.getFluidState(); // FIXME
        BlockPos posNorth = pos.north();
        BlockPos posEast = pos.east();
        BlockPos posSouth = pos.south();
        BlockPos posWest = pos.west();
        BlockPos posUp = pos.above();
        BlockState stateNorth = world.getBlockState(posNorth);
        BlockState stateEast = world.getBlockState(posEast);
        BlockState stateSouth = world.getBlockState(posSouth);
        BlockState stateWest = world.getBlockState(posWest);
        BlockState stateUp = world.getBlockState(posUp);

        boolean connectNorth = this.shouldConnectTo(stateNorth, stateNorth.isFaceSturdy(world, posNorth, Direction.SOUTH), Direction.SOUTH);
        boolean connectEast  = this.shouldConnectTo(stateEast,  stateEast .isFaceSturdy(world, posEast, Direction.WEST), Direction.WEST);
        boolean connectSouth = this.shouldConnectTo(stateSouth, stateSouth.isFaceSturdy(world, posSouth, Direction.NORTH), Direction.NORTH);
        boolean connectWest  = this.shouldConnectTo(stateWest,  stateWest .isFaceSturdy(world, posWest, Direction.EAST), Direction.EAST);
        BlockState baseState = state.getBlock().defaultBlockState().setValue(WallBlock.WATERLOGGED, fluidState.getType() == Fluids.WATER);

        return this.getWallStateWithConnections(world, baseState, posUp, stateUp, connectNorth, connectEast, connectSouth, connectWest);
    }

    private BlockState getWallStateWithConnections(BlockGetter worldView,
                                                   BlockState baseState,
                                                   BlockPos pos,
                                                   BlockState stateUp,
                                                   boolean canConnectNorth,
                                                   boolean canConnectEast,
                                                   boolean canConnectSouth,
                                                   boolean canConnectWest)
    {
        VoxelShape shapeAbove = stateUp.getCollisionShape(worldView, pos).getFaceShape(Direction.DOWN);
        BlockState stateWithSides = this.getWallSideConnections(baseState, canConnectNorth, canConnectEast, canConnectSouth, canConnectWest, shapeAbove);

        return stateWithSides.setValue(WallBlock.UP, this.shouldConnectUp(stateWithSides, stateUp, shapeAbove));
    }

    private BlockState getWallSideConnections(BlockState blockState,
                                                     boolean canConnectNorth,
                                                     boolean canConnectEast,
                                                     boolean canConnectSouth,
                                                     boolean canConnectWest,
                                                     VoxelShape shapeAbove)
    {
        return blockState
                       .setValue(WallBlock.NORTH, this.getConnectionShape(canConnectNorth, shapeAbove, SHAPE_NORTH))
                       .setValue(WallBlock.EAST,  this.getConnectionShape(canConnectEast, shapeAbove, SHAPE_EAST))
                       .setValue(WallBlock.SOUTH, this.getConnectionShape(canConnectSouth, shapeAbove, SHAPE_SOUTH))
                       .setValue(WallBlock.WEST,  this.getConnectionShape(canConnectWest, shapeAbove, SHAPE_WEST));
    }

    private boolean shouldConnectTo(BlockState state, boolean faceFullSquare, Direction side)
    {
        Block block = state.getBlock();

        return state.is(BlockTags.WALLS) ||
               Block.isExceptionForConnection(state) == false && faceFullSquare ||
               block instanceof IronBarsBlock ||
               block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(state, side);
    }

    private boolean shouldConnectUp(BlockState blockState, BlockState stateUp, VoxelShape shapeAbove)
    {
        boolean isUpConnectedWallAbove = stateUp.getBlock() instanceof WallBlock && stateUp.getValue(WallBlock.UP);

        if (isUpConnectedWallAbove)
        {
            return true;
        }
        else
        {
            WallSide shapeNorth = blockState.getValue(WallBlock.NORTH);
            WallSide shapeSouth = blockState.getValue(WallBlock.SOUTH);
            WallSide shapeEast  = blockState.getValue(WallBlock.EAST);
            WallSide shapeWest  = blockState.getValue(WallBlock.WEST);
            boolean unconnectedNorth = shapeNorth == WallSide.NONE;
            boolean unconnectedSouth = shapeSouth == WallSide.NONE;
            boolean unconnectedEast  = shapeEast == WallSide.NONE;
            boolean unconnectedWest  = shapeWest == WallSide.NONE;
            boolean isPillarOrWallEnd = unconnectedNorth && unconnectedSouth && unconnectedWest && unconnectedEast ||
                                        unconnectedNorth != unconnectedSouth || unconnectedWest != unconnectedEast;

            if (isPillarOrWallEnd)
            {
                return true;
            }
            else
            {
                boolean inTallLine = shapeNorth == WallSide.TALL && shapeSouth == WallSide.TALL ||
                                     shapeEast == WallSide.TALL && shapeWest == WallSide.TALL;

                if (inTallLine)
                {
                    return false;
                }
                else
                {
                    return stateUp.is(BlockTags.WALL_POST_OVERRIDE) || this.shapesDoNotIntersect(shapeAbove, SHAPE_PILLAR);
                }
            }
        }
    }

    private WallSide getConnectionShape(boolean canConnect, VoxelShape shapeAbove, VoxelShape shapeSideClearance)
    {
        if (canConnect)
        {
            return this.shapesDoNotIntersect(shapeAbove, shapeSideClearance) ? WallSide.TALL : WallSide.LOW;
        }
        else
        {
            return WallSide.NONE;
        }
    }

    private boolean shapesDoNotIntersect(VoxelShape voxelShape, VoxelShape voxelShape2)
    {
        return Shapes.joinIsNotEmpty(voxelShape2, voxelShape, BooleanOp.ONLY_FIRST) == false;
    }
}
