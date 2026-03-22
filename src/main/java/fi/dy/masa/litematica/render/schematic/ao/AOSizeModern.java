package fi.dy.masa.litematica.render.schematic.ao;

// SizeInfo
public enum AOSizeModern
{
	DOWN(0),
	UP(1),
	NORTH(2),
	SOUTH(3),
	WEST(4),
	EAST(5),
	FLIP_DOWN(6),
	FLIP_UP(7),
	FLIP_NORTH(8),
	FLIP_SOUTH(9),
	FLIP_WEST(10),
	FLIP_EAST(11);

	public static final int SIZE = values().length;
	final int index;

	AOSizeModern(final int index)
	{
//		this.shape = face.get3DDataValue() + (flip ? Direction.values().length : 0);
		this.index = index;
	}
}
