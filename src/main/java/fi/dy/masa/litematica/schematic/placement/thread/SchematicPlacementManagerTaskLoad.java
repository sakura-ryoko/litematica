package fi.dy.masa.litematica.schematic.placement.thread;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.world.WorldSchematic;

public class SchematicPlacementManagerTaskLoad extends SchematicPlacementManagerTask
{
	private final Runnable task;

	protected SchematicPlacementManagerTaskLoad(Supplier<WorldSchematic> worldSupplier, int chunkX, int chunkZ)
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
				// TODO -- Clear Rebuild queue
				return;
			}

			if (manager.getAllSchematicsTouchingChunk(this.pos()).isEmpty())
			{
				manager.removePendingRebuildFor(this.pos());
				return;
			}

			if (DataManager.getSchematicPlacementManager().canHandleChunk(level, this.cx(), this.cz()))
			{
				if (worldSchematic.getChunkProvider().hasChunk(this.cx(), this.cz()))
				{
					worldSchematic.getChunkProvider().unloadChunk(this.cx(), this.cz());
					DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
				}

				worldSchematic.getChunkProvider().loadChunk(this.cx(), this.cz());
			}
		};
	}
}
