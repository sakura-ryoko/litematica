package fi.dy.masa.litematica.world;

/**
 * Rudimentary Semaphore system for thread safety
 */
public enum ChunkSchematicState
{
	NO_WORLD_EXCEPTION      (0),
	NEW                     (1),
	EMPTY                   (2),
	UNLOADED                (3),
	PROTO                   (4),
	LOADED                  (5),
	FILLED                  (6),
	RENDERED                (7),
	;

	private final int index;

	ChunkSchematicState(int index)
	{
		this.index = index;
	}

	public int getIndex()
	{
		return this.index;
	}

	public boolean atLeast(ChunkSchematicState state)
	{
		return this.index >= state.index;
	}
}
