package fi.dy.masa.litematica.schematic.placement;

import java.util.Collection;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.PasteLayerBehavior;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.util.SchematicPlacingUtils;
import fi.dy.masa.litematica.world.WorldSchematic;

public class PlacementManagerTaskRebuild extends PlacementManagerTask
{
	private final Runnable task;

	protected PlacementManagerTaskRebuild(Supplier<WorldSchematic> worldSupplier, int chunkX, int chunkZ)
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
			SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
			WorldSchematic worldSchematic = this.worldSupplier().get();
			ClientLevel level = Minecraft.getInstance().level;

			if (level == null)
			{
				PlacementManagerDaemonHandler.INSTANCE.updateAll();
				return;
			}

			if (manager.getAllSchematicsTouchingChunk(this.pos()).isEmpty())
			{
//				manager.removePendingRebuildFor(this.pos());
				PlacementManagerDaemonHandler.INSTANCE.removeAllTasksFor(this.cx(), this.cz());
				PlacementManagerDaemonHandler.INSTANCE.addTask(new PlacementManagerTaskUnload(this.worldSupplier(), this.cx(), this.cz()));
				return;
			}

			if (manager.canHandleChunk(level, this.cx(), this.cz()))
			{
				if (worldSchematic.getChunkProvider().hasChunk(this.cx(), this.cz()))
				{
					worldSchematic.getChunkProvider().unloadChunk(this.cx(), this.cz());
					worldSchematic.unloadEntitiesByChunk(this.cx(), this.cz());
					manager.setVisibleSubChunksNeedsUpdate();       // todo
				}

				worldSchematic.getChunkProvider().loadChunk(this.cx(), this.cz());
			}

			if (worldSchematic.getChunkProvider().hasChunk(this.cx(), this.cz()))
			{
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
					manager.setVisibleSubChunksNeedsUpdate();
				}

//				manager.removePendingRebuildFor(this.pos());
				PlacementManagerDaemonHandler.INSTANCE.removeUnloadTasksFor(this.cx(), this.cz());
				PlacementManagerDaemonHandler.INSTANCE.removeRebuildTasksFor(this.cx(), this.cz());
			}
		};
	}
}
