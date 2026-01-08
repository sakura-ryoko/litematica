package fi.dy.masa.litematica.schematic.placement;

import java.util.function.Supplier;
import javax.annotation.Nonnull;

import fi.dy.masa.litematica.world.WorldSchematic;

public class PlacementManagerTaskOther extends PlacementManagerTask
{
	private final Runnable task;

	protected PlacementManagerTaskOther(Supplier<WorldSchematic> worldSupplier, int chunkX, int chunkZ,
	                                    @Nonnull Runnable task)
	{
		super(worldSupplier, chunkX, chunkZ);
		this.task = task;
	}

	@Override
	public void run()
	{
		this.task.run();
	}

	@Override
	protected Runnable buildTask()
	{
		return this.task;
	}
}
