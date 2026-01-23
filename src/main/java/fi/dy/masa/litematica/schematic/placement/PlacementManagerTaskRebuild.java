package fi.dy.masa.litematica.schematic.placement;

import java.util.Collection;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.WorldPlacingUtils;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.ChunkSchematicState;
import fi.dy.masa.litematica.world.ProtoChunkSchematic;
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
				PlacementManagerDaemonHandler.INSTANCE.clearAllTasks();
				return;
			}

			if (manager.getAllSchematicsTouchingChunk(this.pos()).isEmpty())
			{
//				manager.removePendingRebuildFor(this.pos());
				PlacementManagerDaemonHandler.INSTANCE.removeAllTasksFor(this.cx(), this.cz());
				manager.markChunkForUnload(this.cx(), this.cz());
				return;
			}

//			boolean shouldLoad = false;

			if (manager.canHandleChunk(level, this.cx(), this.cz()))
			{
				if (worldSchematic.getChunkSource().hasChunk(this.cx(), this.cz()))
				{
					worldSchematic.unloadEntitiesByChunk(this.cx(), this.cz());
					worldSchematic.getChunkSource().unloadChunk(this.cx(), this.cz());
					manager.setVisibleSubChunksNeedsUpdate();
				}

				worldSchematic.getChunkSource().loadChunk(this.cx(), this.cz());
				manager.setVisibleSubChunksNeedsUpdate();
//				shouldLoad = true;
			}

//			if (shouldLoad)
			if (worldSchematic.getChunkSource().hasChunk(this.cx(), this.cz()))
			{
				ProtoChunkSchematic protoChunk = new ProtoChunkSchematic(new ChunkSchematic(worldSchematic, this.pos()));
				Collection<SchematicPlacement> placements = manager.getAllSchematicsTouchingChunk(this.pos());

				protoChunk.setState(ChunkSchematicState.PROTO);

				if (!placements.isEmpty())
				{
					for (SchematicPlacement placement : placements)
					{
						if (placement.isEnabled())
						{
							WorldPlacingUtils.placeToProtoChunk(protoChunk, this.pos(), placement);
						}
					}

					// Load Real Chunk and spawn the entities
					worldSchematic.unloadEntitiesByChunk(this.cx(), this.cz());
					worldSchematic.getChunkSource().replaceChunk(this.cx(), this.cz(), protoChunk.getWrapped());
					protoChunk.spawnAllEntitiesNow(worldSchematic);
				}

				protoChunk.clear();

//				else
//				{
//					worldSchematic.getChunkSource().loadChunk(this.cx(), this.cz());
//				}

//				if (worldSchematic.getChunkSource().hasChunk(this.cx(), this.cz()))
//				{
//				      manager.removePendingRebuildFor(this.pos());
					worldSchematic.scheduleChunkRenders(this.cx(), this.cz());
//					manager.setVisibleSubChunksNeedsUpdate();

					PlacementManagerDaemonHandler.INSTANCE.removeUnloadTasksFor(this.cx(), this.cz());
					PlacementManagerDaemonHandler.INSTANCE.removeRebuildTasksFor(this.cx(), this.cz());
				}
//			}
		};
	}
}
