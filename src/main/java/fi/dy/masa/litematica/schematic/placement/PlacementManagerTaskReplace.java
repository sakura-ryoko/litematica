package fi.dy.masa.litematica.schematic.placement;

import java.util.function.Supplier;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.WorldSchematic;

public class PlacementManagerTaskReplace extends PlacementManagerTask
{
	private final Runnable task;
	private final ChunkSchematic chunk;

	protected PlacementManagerTaskReplace(Supplier<WorldSchematic> worldSupplier, int chunkX, int chunkZ,
	                                      @Nonnull ChunkSchematic newChunk)
	{
		super(worldSupplier, chunkX, chunkZ);
		this.chunk = newChunk;
		this.ensureChunkMatchesPos();
		this.task = this.buildTask();
	}

	private void ensureChunkMatchesPos()
	{
		if (!this.pos().equals(this.chunk.getPos()))
		{
			String error = String.format("SchematicPlacementManagerTaskReplace: Chunk position doesn't match; '%s' != '%s'",
			                             this.pos().toString(), this.chunk.getPos().toString());

			Litematica.LOGGER.error(error);
			throw  new IllegalStateException(error);
		}
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

			if (manager.canHandleChunk(level, this.cx(), this.cz()))
			{
				worldSchematic.getChunkProvider().replaceChunk(this.cx(), this.cz(), this.chunk);
				manager.setVisibleSubChunksNeedsUpdate();
			}
		};
	}
}
