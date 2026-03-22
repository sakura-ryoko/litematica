package fi.dy.masa.litematica.render.schematic.ao;

import net.minecraft.core.Direction;

public enum AOSizeLegacy
{
	DOWN(Direction.DOWN, false),
	UP(Direction.UP, false),
	NORTH(Direction.NORTH, false),
	SOUTH(Direction.SOUTH, false),
	WEST(Direction.WEST, false),
	EAST(Direction.EAST, false),
	FLIP_DOWN(Direction.DOWN, true),
	FLIP_UP(Direction.UP, true),
	FLIP_NORTH(Direction.NORTH, true),
	FLIP_SOUTH(Direction.SOUTH, true),
	FLIP_WEST(Direction.WEST, true),
	FLIP_EAST(Direction.EAST, true);

	final int shape;

	AOSizeLegacy(final Direction face, final boolean flip)
	{
		this.shape = face.get3DDataValue() + (flip ? Direction.values().length : 0);
	}
}
