package fi.dy.masa.litematica.render.schematic.ao;

import net.minecraft.core.Direction;
import net.minecraft.util.Util;

// AdjacencyInfo
public enum AONeighborInfoLegacy
{
	DOWN(new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}, 0.5F, true,
	     new AOSizeLegacy[]{AOSizeLegacy.FLIP_WEST, AOSizeLegacy.SOUTH, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.WEST, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.WEST, AOSizeLegacy.SOUTH},
	     new AOSizeLegacy[]{AOSizeLegacy.FLIP_WEST, AOSizeLegacy.NORTH, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.WEST, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.WEST, AOSizeLegacy.NORTH},
	     new AOSizeLegacy[]{AOSizeLegacy.FLIP_EAST, AOSizeLegacy.NORTH, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.EAST, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.EAST, AOSizeLegacy.NORTH},
	     new AOSizeLegacy[]{AOSizeLegacy.FLIP_EAST, AOSizeLegacy.SOUTH, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.EAST, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.EAST, AOSizeLegacy.SOUTH}),
	UP(new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}, 1.0F, true,
	   new AOSizeLegacy[]{AOSizeLegacy.EAST, AOSizeLegacy.SOUTH, AOSizeLegacy.EAST, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.SOUTH},
	   new AOSizeLegacy[]{AOSizeLegacy.EAST, AOSizeLegacy.NORTH, AOSizeLegacy.EAST, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.NORTH},
	   new AOSizeLegacy[]{AOSizeLegacy.WEST, AOSizeLegacy.NORTH, AOSizeLegacy.WEST, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.NORTH},
	   new AOSizeLegacy[]{AOSizeLegacy.WEST, AOSizeLegacy.SOUTH, AOSizeLegacy.WEST, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.SOUTH}),
	NORTH(new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}, 0.8F, true,
	      new AOSizeLegacy[]{AOSizeLegacy.UP, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.UP, AOSizeLegacy.WEST, AOSizeLegacy.FLIP_UP, AOSizeLegacy.WEST, AOSizeLegacy.FLIP_UP, AOSizeLegacy.FLIP_WEST},
	      new AOSizeLegacy[]{AOSizeLegacy.UP, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.UP, AOSizeLegacy.EAST, AOSizeLegacy.FLIP_UP, AOSizeLegacy.EAST, AOSizeLegacy.FLIP_UP, AOSizeLegacy.FLIP_EAST},
	      new AOSizeLegacy[]{AOSizeLegacy.DOWN, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.DOWN, AOSizeLegacy.EAST, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.EAST, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.FLIP_EAST},
	      new AOSizeLegacy[]{AOSizeLegacy.DOWN, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.DOWN, AOSizeLegacy.WEST, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.WEST, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.FLIP_WEST}),
	SOUTH(new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP}, 0.8F, true,
	      new AOSizeLegacy[]{AOSizeLegacy.UP, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.FLIP_UP, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.FLIP_UP, AOSizeLegacy.WEST, AOSizeLegacy.UP, AOSizeLegacy.WEST},
	      new AOSizeLegacy[]{AOSizeLegacy.DOWN, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.FLIP_WEST, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.WEST, AOSizeLegacy.DOWN, AOSizeLegacy.WEST},
	      new AOSizeLegacy[]{AOSizeLegacy.DOWN, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.EAST, AOSizeLegacy.DOWN, AOSizeLegacy.EAST},
	      new AOSizeLegacy[]{AOSizeLegacy.UP, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.FLIP_UP, AOSizeLegacy.FLIP_EAST, AOSizeLegacy.FLIP_UP, AOSizeLegacy.EAST, AOSizeLegacy.UP, AOSizeLegacy.EAST}),
	WEST(new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH}, 0.6F, true,
	     new AOSizeLegacy[]{AOSizeLegacy.UP, AOSizeLegacy.SOUTH, AOSizeLegacy.UP, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.FLIP_UP, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.FLIP_UP, AOSizeLegacy.SOUTH},
	     new AOSizeLegacy[]{AOSizeLegacy.UP, AOSizeLegacy.NORTH, AOSizeLegacy.UP, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.FLIP_UP, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.FLIP_UP, AOSizeLegacy.NORTH},
	     new AOSizeLegacy[]{AOSizeLegacy.DOWN, AOSizeLegacy.NORTH, AOSizeLegacy.DOWN, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.NORTH},
	     new AOSizeLegacy[]{AOSizeLegacy.DOWN, AOSizeLegacy.SOUTH, AOSizeLegacy.DOWN, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.SOUTH}),
	EAST(new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}, 0.6F, true,
	     new AOSizeLegacy[]{AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.SOUTH, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.DOWN, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.DOWN, AOSizeLegacy.SOUTH},
	     new AOSizeLegacy[]{AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.NORTH, AOSizeLegacy.FLIP_DOWN, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.DOWN, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.DOWN, AOSizeLegacy.NORTH},
	     new AOSizeLegacy[]{AOSizeLegacy.FLIP_UP, AOSizeLegacy.NORTH, AOSizeLegacy.FLIP_UP, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.UP, AOSizeLegacy.FLIP_NORTH, AOSizeLegacy.UP, AOSizeLegacy.NORTH},
	     new AOSizeLegacy[]{AOSizeLegacy.FLIP_UP, AOSizeLegacy.SOUTH, AOSizeLegacy.FLIP_UP, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.UP, AOSizeLegacy.FLIP_SOUTH, AOSizeLegacy.UP, AOSizeLegacy.SOUTH});

	final Direction[] corners;
	final boolean doNonCubicWeight;
	final AOSizeLegacy[] vert0Weights;
	final AOSizeLegacy[] vert1Weights;
	final AOSizeLegacy[] vert2Weights;
	final AOSizeLegacy[] vert3Weights;
	private static final AONeighborInfoLegacy[] VALUES = Util.make(new AONeighborInfoLegacy[6], (values) ->
	{
		values[Direction.DOWN.get3DDataValue()] = DOWN;
		values[Direction.UP.get3DDataValue()] = UP;
		values[Direction.NORTH.get3DDataValue()] = NORTH;
		values[Direction.SOUTH.get3DDataValue()] = SOUTH;
		values[Direction.WEST.get3DDataValue()] = WEST;
		values[Direction.EAST.get3DDataValue()] = EAST;
	});

	AONeighborInfoLegacy(final Direction[] corners, final float f, final boolean nonCubicWeight, final AOSizeLegacy[] neighbor1, final AOSizeLegacy[] neighbor2, final AOSizeLegacy[] neighbor3, final AOSizeLegacy[] neighbor4)
	{
		this.corners = corners;
		this.doNonCubicWeight = nonCubicWeight;
		this.vert0Weights = neighbor1;
		this.vert1Weights = neighbor2;
		this.vert2Weights = neighbor3;
		this.vert3Weights = neighbor4;
	}

	public static AONeighborInfoLegacy getNeighbourInfo(Direction direction)
	{
		return VALUES[direction.get3DDataValue()];
	}
}
