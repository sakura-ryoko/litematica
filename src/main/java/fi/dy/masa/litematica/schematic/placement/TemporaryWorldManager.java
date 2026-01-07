package fi.dy.masa.litematica.schematic.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.litematica.Litematica;

public class TemporaryWorldManager implements AutoCloseable
{
	public static final TemporaryWorldManager INSTANCE = new TemporaryWorldManager();

	private final ConcurrentHashMap<String, TemporaryWorldHolder> tempWorlds;

	private TemporaryWorldManager()
	{
		this.tempWorlds = new ConcurrentHashMap<>();
	}

	protected synchronized TemporaryWorldHolder getTemporaryWorld(String worldName)
	{
		worldName = this.ensureSafeWorldName(worldName);

		if (!this.tempWorlds.containsKey(worldName))
		{
			this.tempWorlds.put(worldName, new TemporaryWorldHolder());
			Litematica.debugLog("TemporaryWorldManager: Created New temporary world: '{}' (No origin / size provided)", worldName);
		}

		return this.tempWorlds.get(worldName);
	}

	public synchronized TemporaryWorldHolder getTemporaryWorld(String worldName, BlockPos origin, Vec3i size)
	{
		worldName = this.ensureSafeWorldName(worldName);

		if (!this.tempWorlds.containsKey(worldName))
		{
			this.tempWorlds.put(worldName, new TemporaryWorldHolder(origin, size));
			Litematica.debugLog("TemporaryWorldManager: Created New temporary world: '{}', at '{}' with a size of: '{}'", worldName, origin.toShortString(), size.toShortString());
		}

		return this.tempWorlds.get(worldName);
	}

	public synchronized boolean hasTemporaryWorld(String worldName)
	{
		return this.tempWorlds.containsKey(this.ensureSafeWorldName(worldName));
	}

	public synchronized void removeTemporaryWorld(String worldName)
	{
		worldName = this.ensureSafeWorldName(worldName);

		try (TemporaryWorldHolder removed = this.tempWorlds.remove(worldName))
		{
			if (removed != null)
			{
				Litematica.debugLog("TemporaryWorldManager: Removed temporary world: '{}'", worldName);
			}
		}
		catch (Exception ignored) { }
	}

	public synchronized void setOriginAndSize(String worldName, BlockPos origin, Vec3i size)
	{
		worldName = this.ensureSafeWorldName(worldName);

		if (!this.tempWorlds.containsKey(worldName))
		{
			this.tempWorlds.get(worldName).clear();
			this.tempWorlds.put(worldName, new TemporaryWorldHolder(origin, size));
			Litematica.debugLog("TemporaryWorldManager: Replaced temporary world: '{}', at '{}' with a size of: '{}'", worldName, origin.toShortString(), size.toShortString());
		}
	}

	public synchronized List<ChunkPos> getChunks(String worldName)
	{
		worldName = this.ensureSafeWorldName(worldName);
		List<ChunkPos> chunks = new ArrayList<>();

		if (this.tempWorlds.containsKey(worldName))
		{
			TemporaryWorldHolder world = this.tempWorlds.get(worldName);

			if (world.isEmpty())
			{
				Litematica.LOGGER.error("TemporaryWorldManager#getChunks(): Temporary world: '{}' is empty!  Cannot replace a chunk that was not calculated!", worldName);
			}
			else
			{
				return world.chunkList().stream()
				            .map(pair -> new ChunkPos(pair.getLeft(), pair.getRight()))
				            .toList();
			}
		}
		else
		{
			Litematica.LOGGER.error("TemporaryWorldManager#getChunks(): Temporary world: '{}'; was not found!", worldName);
		}

		return chunks;
	}

//	public synchronized boolean replaceChunkAt(String worldName, int cx, int cz, @NonNull ChunkSchematic newChunk)
//	{
//		worldName = this.ensureSafeWorldName(worldName);
//
//		if (this.tempWorlds.containsKey(worldName))
//		{
//			TemporaryWorldHolder world = this.tempWorlds.get(worldName);
//
//			if (world.isEmpty())
//			{
//				Litematica.LOGGER.error("TemporaryWorldManager#replaceChunkAt(): Temporary world: '{}' is empty!  Cannot replace a chunk that was not calculated!", worldName);
//				return false;
//			}
//
//			if (!world.chunkList().contains(Pair.of(cx, cz)))
//			{
//				Litematica.LOGGER.error("TemporaryWorldManager#replaceChunkAt(): Temporary world: '{}' is empty!  Cannot replace chunk [cx: {}, cz: {}] outside of the requested Area Size", worldName, cx, cz);
//				return false;
//			}
//
//			// TODO --> create a task
//			if (world.chunkManager() != null && Objects.requireNonNull(world.chunkManager()).replaceChunk(cx, cz, newChunk))
//			{
//				Litematica.debugLog("TemporaryWorldManager#replaceChunkAt(): Temporary world: '{}' --> Replaced Chunk at [cx: {}, cz: {}] successfully!", worldName, cx, cz);
//				return true;
//			}
//			else
//			{
//				return false;
//			}
//		}
//
//		Litematica.LOGGER.error("TemporaryWorldManager#replaceChunkAt(): Temporary world: '{}'; was not found!", worldName);
//		return false;
//	}

	private String ensureSafeWorldName(String worldName) throws IllegalStateException
	{
		if (worldName == null || worldName.isEmpty())
		{
			throw new IllegalStateException("Temporary World Name is empty!");
		}

		if (worldName.length() > 256)
		{
			throw new IllegalStateException("Temporary World Name is too long!");
		}

		return FileNameUtils.generateSafeFileName(worldName);
	}

	public void reset()
	{
		this.clear();
	}

	public synchronized void clear()
	{
		if (!this.tempWorlds.isEmpty())
		{
			this.tempWorlds.forEach(
					(s, world) ->
					{
						try
						{
							world.close();
						}
						catch (Exception ignored)
						{
						}
					}
			);

			this.tempWorlds.clear();
		}
	}

	@Override
	public void close() throws Exception
	{
		this.clear();
	}
}
