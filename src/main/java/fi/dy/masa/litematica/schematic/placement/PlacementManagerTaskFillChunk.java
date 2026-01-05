package fi.dy.masa.litematica.schematic.placement;

import java.util.Collection;
import java.util.function.Supplier;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.PasteLayerBehavior;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.util.SchematicPlacingUtils;
import fi.dy.masa.litematica.world.WorldSchematic;

public class PlacementManagerTaskFillChunk extends PlacementManagerTask
{
	private final Runnable task;

	protected PlacementManagerTaskFillChunk(Supplier<WorldSchematic> worldSupplier, int chunkX, int chunkZ)
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

			if (worldSchematic.getChunkProvider().hasChunk(this.cx(), this.cz()))
			{
				SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
				Collection<SchematicPlacement> placements = manager.getAllSchematicsTouchingChunk(this.pos());

				if (!placements.isEmpty())
				{
					ReplaceBehavior behavior = (ReplaceBehavior) Configs.Generic.PLACEMENT_REPLACE_BEHAVIOR.getOptionListValue();
					PasteLayerBehavior layers = (PasteLayerBehavior) Configs.Generic.PASTE_LAYER_BEHAVIOR.getOptionListValue();

					for (SchematicPlacement placement : placements)
					{
						if (placement.isEnabled())
						{
							SchematicPlacingUtils.placeToWorldWithinChunk(worldSchematic, this.pos(), placement, behavior, layers, false);
						}
					}

					worldSchematic.scheduleChunkRenders(this.cx(), this.cz());
				}

				manager.removePendingRebuildFor(this.pos());
			}
		};
	}
}
