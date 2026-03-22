package fi.dy.masa.litematica.render.schematic.ao;

import net.minecraft.core.Direction;
import net.minecraft.util.Util;

// AdjacencyInfo
public enum AONeighborInfoModern
{
	DOWN(new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}, 0.5F, true,
	     new AOSizeModern[]{AOSizeModern.FLIP_WEST, AOSizeModern.SOUTH, AOSizeModern.FLIP_WEST, AOSizeModern.FLIP_SOUTH, AOSizeModern.WEST, AOSizeModern.FLIP_SOUTH, AOSizeModern.WEST, AOSizeModern.SOUTH},
	     new AOSizeModern[]{AOSizeModern.FLIP_WEST, AOSizeModern.NORTH, AOSizeModern.FLIP_WEST, AOSizeModern.FLIP_NORTH, AOSizeModern.WEST, AOSizeModern.FLIP_NORTH, AOSizeModern.WEST, AOSizeModern.NORTH},
	     new AOSizeModern[]{AOSizeModern.FLIP_EAST, AOSizeModern.NORTH, AOSizeModern.FLIP_EAST, AOSizeModern.FLIP_NORTH, AOSizeModern.EAST, AOSizeModern.FLIP_NORTH, AOSizeModern.EAST, AOSizeModern.NORTH},
	     new AOSizeModern[]{AOSizeModern.FLIP_EAST, AOSizeModern.SOUTH, AOSizeModern.FLIP_EAST, AOSizeModern.FLIP_SOUTH, AOSizeModern.EAST, AOSizeModern.FLIP_SOUTH, AOSizeModern.EAST, AOSizeModern.SOUTH}),
	UP(new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}, 1.0F, true,
	   new AOSizeModern[]{AOSizeModern.EAST, AOSizeModern.SOUTH, AOSizeModern.EAST, AOSizeModern.FLIP_SOUTH, AOSizeModern.FLIP_EAST, AOSizeModern.FLIP_SOUTH, AOSizeModern.FLIP_EAST, AOSizeModern.SOUTH},
	   new AOSizeModern[]{AOSizeModern.EAST, AOSizeModern.NORTH, AOSizeModern.EAST, AOSizeModern.FLIP_NORTH, AOSizeModern.FLIP_EAST, AOSizeModern.FLIP_NORTH, AOSizeModern.FLIP_EAST, AOSizeModern.NORTH},
	   new AOSizeModern[]{AOSizeModern.WEST, AOSizeModern.NORTH, AOSizeModern.WEST, AOSizeModern.FLIP_NORTH, AOSizeModern.FLIP_WEST, AOSizeModern.FLIP_NORTH, AOSizeModern.FLIP_WEST, AOSizeModern.NORTH},
	   new AOSizeModern[]{AOSizeModern.WEST, AOSizeModern.SOUTH, AOSizeModern.WEST, AOSizeModern.FLIP_SOUTH, AOSizeModern.FLIP_WEST, AOSizeModern.FLIP_SOUTH, AOSizeModern.FLIP_WEST, AOSizeModern.SOUTH}),
	NORTH(new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}, 0.8F, true,
	      new AOSizeModern[]{AOSizeModern.UP, AOSizeModern.FLIP_WEST, AOSizeModern.UP, AOSizeModern.WEST, AOSizeModern.FLIP_UP, AOSizeModern.WEST, AOSizeModern.FLIP_UP, AOSizeModern.FLIP_WEST},
	      new AOSizeModern[]{AOSizeModern.UP, AOSizeModern.FLIP_EAST, AOSizeModern.UP, AOSizeModern.EAST, AOSizeModern.FLIP_UP, AOSizeModern.EAST, AOSizeModern.FLIP_UP, AOSizeModern.FLIP_EAST},
	      new AOSizeModern[]{AOSizeModern.DOWN, AOSizeModern.FLIP_EAST, AOSizeModern.DOWN, AOSizeModern.EAST, AOSizeModern.FLIP_DOWN, AOSizeModern.EAST, AOSizeModern.FLIP_DOWN, AOSizeModern.FLIP_EAST},
	      new AOSizeModern[]{AOSizeModern.DOWN, AOSizeModern.FLIP_WEST, AOSizeModern.DOWN, AOSizeModern.WEST, AOSizeModern.FLIP_DOWN, AOSizeModern.WEST, AOSizeModern.FLIP_DOWN, AOSizeModern.FLIP_WEST}),
	SOUTH(new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP}, 0.8F, true,
	      new AOSizeModern[]{AOSizeModern.UP, AOSizeModern.FLIP_WEST, AOSizeModern.FLIP_UP, AOSizeModern.FLIP_WEST, AOSizeModern.FLIP_UP, AOSizeModern.WEST, AOSizeModern.UP, AOSizeModern.WEST},
	      new AOSizeModern[]{AOSizeModern.DOWN, AOSizeModern.FLIP_WEST, AOSizeModern.FLIP_DOWN, AOSizeModern.FLIP_WEST, AOSizeModern.FLIP_DOWN, AOSizeModern.WEST, AOSizeModern.DOWN, AOSizeModern.WEST},
	      new AOSizeModern[]{AOSizeModern.DOWN, AOSizeModern.FLIP_EAST, AOSizeModern.FLIP_DOWN, AOSizeModern.FLIP_EAST, AOSizeModern.FLIP_DOWN, AOSizeModern.EAST, AOSizeModern.DOWN, AOSizeModern.EAST},
	      new AOSizeModern[]{AOSizeModern.UP, AOSizeModern.FLIP_EAST, AOSizeModern.FLIP_UP, AOSizeModern.FLIP_EAST, AOSizeModern.FLIP_UP, AOSizeModern.EAST, AOSizeModern.UP, AOSizeModern.EAST}),
	WEST(new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH}, 0.6F, true,
	     new AOSizeModern[]{AOSizeModern.UP, AOSizeModern.SOUTH, AOSizeModern.UP, AOSizeModern.FLIP_SOUTH, AOSizeModern.FLIP_UP, AOSizeModern.FLIP_SOUTH, AOSizeModern.FLIP_UP, AOSizeModern.SOUTH},
	     new AOSizeModern[]{AOSizeModern.UP, AOSizeModern.NORTH, AOSizeModern.UP, AOSizeModern.FLIP_NORTH, AOSizeModern.FLIP_UP, AOSizeModern.FLIP_NORTH, AOSizeModern.FLIP_UP, AOSizeModern.NORTH},
	     new AOSizeModern[]{AOSizeModern.DOWN, AOSizeModern.NORTH, AOSizeModern.DOWN, AOSizeModern.FLIP_NORTH, AOSizeModern.FLIP_DOWN, AOSizeModern.FLIP_NORTH, AOSizeModern.FLIP_DOWN, AOSizeModern.NORTH},
	     new AOSizeModern[]{AOSizeModern.DOWN, AOSizeModern.SOUTH, AOSizeModern.DOWN, AOSizeModern.FLIP_SOUTH, AOSizeModern.FLIP_DOWN, AOSizeModern.FLIP_SOUTH, AOSizeModern.FLIP_DOWN, AOSizeModern.SOUTH}),
	EAST(new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}, 0.6F, true,
	     new AOSizeModern[]{AOSizeModern.FLIP_DOWN, AOSizeModern.SOUTH, AOSizeModern.FLIP_DOWN, AOSizeModern.FLIP_SOUTH, AOSizeModern.DOWN, AOSizeModern.FLIP_SOUTH, AOSizeModern.DOWN, AOSizeModern.SOUTH},
	     new AOSizeModern[]{AOSizeModern.FLIP_DOWN, AOSizeModern.NORTH, AOSizeModern.FLIP_DOWN, AOSizeModern.FLIP_NORTH, AOSizeModern.DOWN, AOSizeModern.FLIP_NORTH, AOSizeModern.DOWN, AOSizeModern.NORTH},
	     new AOSizeModern[]{AOSizeModern.FLIP_UP, AOSizeModern.NORTH, AOSizeModern.FLIP_UP, AOSizeModern.FLIP_NORTH, AOSizeModern.UP, AOSizeModern.FLIP_NORTH, AOSizeModern.UP, AOSizeModern.NORTH},
	     new AOSizeModern[]{AOSizeModern.FLIP_UP, AOSizeModern.SOUTH, AOSizeModern.FLIP_UP, AOSizeModern.FLIP_SOUTH, AOSizeModern.UP, AOSizeModern.FLIP_SOUTH, AOSizeModern.UP, AOSizeModern.SOUTH});

	final Direction[] corners;
	final boolean doNonCubicWeight;
	final AOSizeModern[] vert0Weights;
	final AOSizeModern[] vert1Weights;
	final AOSizeModern[] vert2Weights;
	final AOSizeModern[] vert3Weights;
	private static final AONeighborInfoModern[] VALUES = Util.make(new AONeighborInfoModern[6], (values) ->
	{
		values[Direction.DOWN.get3DDataValue()] = DOWN;
		values[Direction.UP.get3DDataValue()] = UP;
		values[Direction.NORTH.get3DDataValue()] = NORTH;
		values[Direction.SOUTH.get3DDataValue()] = SOUTH;
		values[Direction.WEST.get3DDataValue()] = WEST;
		values[Direction.EAST.get3DDataValue()] = EAST;
	});

	AONeighborInfoModern(final Direction[] corners, final float f, final boolean nonCubicWeight, final AOSizeModern[] neighbor1, final AOSizeModern[] neighbor2, final AOSizeModern[] neighbor3, final AOSizeModern[] neighbor4)
	{
		this.corners = corners;
		this.doNonCubicWeight = nonCubicWeight;
		this.vert0Weights = neighbor1;
		this.vert1Weights = neighbor2;
		this.vert2Weights = neighbor3;
		this.vert3Weights = neighbor4;
	}

	public static AONeighborInfoModern getNeighbourInfo(Direction direction)
	{
		return VALUES[direction.get3DDataValue()];
	}
}
