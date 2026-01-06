package fi.dy.masa.litematica.schematic.placement;

import java.util.function.Supplier;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.WorldSchematic;

public class PlacementManagerTaskNeedsUpdate extends PlacementManagerTask
{
	private final Runnable task;

	protected PlacementManagerTaskNeedsUpdate(Supplier<WorldSchematic> worldSupplier)
	{
		super(worldSupplier, 0, 0);
		this.task = this.buildTask();
	}

	@Override
	public void run()
	{
		this.task.run();
	}

	@Override
	protected Runnable buildTask()
	{
		return () ->
		{
			DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
//			LitematicaRenderer.getInstance().getWorldRenderer().markNeedsUpdate();
		};
	}
}
