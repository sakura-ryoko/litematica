package fi.dy.masa.litematica.schematic.placement;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class PlacementManagerTaskFixer extends PlacementManagerTask
{
	private final Runnable task;
	int offset;

	protected PlacementManagerTaskFixer(Supplier<WorldSchematic> worldSupplier, int chunkX, int chunkZ, int offset)
	{
		super(worldSupplier, chunkX, chunkZ);
		this.task = this.buildTask();
		this.offset = offset;
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
			Set<ChunkPos> loaded = new HashSet<>();
			Set<ChunkPos> notLoaded = new HashSet<>();

			final int startX = this.pos().x - this.offset;
			final int startZ = this.pos().z - this.offset;
			final int endX = this.pos().x + this.offset;
			final int endZ = this.pos().z + this.offset;
			ClientLevel clientWorld = Minecraft.getInstance().level;

			if (clientWorld == null) { return; }

			for (int cx = startX; cx < endX; cx++)
			{
				for (int cz = startZ; cz < endZ; cz++)
				{
					final ChunkPos cp = new ChunkPos(cx, cz);

					if (!this.worldSupplier().get().getChunkSource().hasChunk(cx, cz) &&
					    DataManager.getSchematicPlacementManager().canHandleChunk(clientWorld, cx, cz))
					{
						Frustum frustum = Minecraft.getInstance().levelRenderer.getCapturedFrustum();

						// Check Frustum culling
						if (frustum != null)
						{
							BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(cx << 4, clientWorld.getMinY(), cz << 4);
							int x = pos.getX();
							int y = pos.getY();
							int z = pos.getZ();
							AABB bb = new AABB(x, y, z, x + 16, y + clientWorld.getHeight(), z + 16);

							if (frustum.isVisible(bb))
							{
								notLoaded.add(cp);
							}
						}
						else
						{
							notLoaded.add(cp);
						}
					}
					else if (this.worldSupplier().get().getChunkSource().hasChunk(cx, cz))
					{
						loaded.add(cp);
					}
				}
			}

			if (!loaded.isEmpty())
			{
				Litematica.debugLog("SchematicPlacementManager//FIXER: checking [{}] loaded chunks", loaded.size());
				loaded.forEach(c ->
							   {
								   List<SchematicPlacement>
										   placements = DataManager.getSchematicPlacementManager()
							                                       .getAllSchematicsTouchingChunk(c);

								   if (placements.isEmpty())
								   {
									   DataManager.getSchematicPlacementManager().markChunkForUnload(c.x, c.z);
//												  .schedulePendingTaskForNextTick(new PlacementManagerTaskUnload(this.worldSupplier(), c.x, c.z));
								   }
								   else
								   {
									   boolean unload = true;

									   for (SchematicPlacement s : placements)
									   {
										   if (s.isRenderingEnabled())
										   {
											   unload = false;
										   }
									   }

									   if (unload)
									   {
										   DataManager.getSchematicPlacementManager().markChunkForUnload(c.x, c.z);
//													  .schedulePendingTaskForNextTick(new PlacementManagerTaskUnload(this.worldSupplier(), c.x, c.z));
									   }
								   }
							   });
			}

			if (!notLoaded.isEmpty())
			{
				Litematica.debugLog("SchematicPlacementManager//FIXER: checking [{}] unloaded chunks", notLoaded.size());
				notLoaded.forEach(c ->
								  {
									  List<SchematicPlacement> placements = DataManager.getSchematicPlacementManager()
																					   .getAllSchematicsTouchingChunk(c);

									  // Load/Rebuild if Chunk is Near, no matter what for Verifier.
									  if (c.getChessboardDistance(this.pos()) <= 3)
									  {
										  DataManager.getSchematicPlacementManager()
													 .schedulePendingTaskForNextTick(new PlacementManagerTaskRebuild(this.worldSupplier(), c.x, c.z));
									  }
									  else
									  {
										  if (!placements.isEmpty())
										  {
											  boolean rebuild = false;

											  for (SchematicPlacement s : placements)
											  {
												  if (s.isRenderingEnabled())
												  {
													  rebuild = true;
												  }
											  }

											  if (rebuild)
											  {
												  DataManager.getSchematicPlacementManager()
															 .schedulePendingTaskForNextTick(new PlacementManagerTaskRebuild(this.worldSupplier(), c.x, c.z));
											  }
										  }
									  }
								  });
			}
		};
	}
}
