package fi.dy.masa.litematica.schematic.placement;

import java.util.function.Supplier;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.WorldSchematic;

public class PlacementManagerTaskUnload extends PlacementManagerTask
{
	private final Runnable task;

	protected PlacementManagerTaskUnload(Supplier<WorldSchematic> worldSupplier, int chunkX, int chunkZ)
	{
		super(worldSupplier, chunkX, chunkZ);
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
			WorldSchematic worldSchematic = this.worldSupplier().get();
			SchematicPlacementManager manager =  DataManager.getSchematicPlacementManager();

			if (worldSchematic.getChunkSource().hasChunk(this.cx(), this.cz()))
			{
				PlacementManagerDaemonHandler.INSTANCE.removeAllTasksFor(this.cx(), this.cz());
				worldSchematic.unloadEntitiesByChunk(this.cx(), this.cz());
				worldSchematic.getChunkSource().unloadChunk(this.cx(), this.cz());
				worldSchematic.scheduleChunkRenders(this.cx(), this.cz(), true);
				manager.setVisibleSubChunksNeedsUpdate();
			}
		};
	}
}
