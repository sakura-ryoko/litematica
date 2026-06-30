package fi.dy.masa.litematica.render.schematic;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.render.IWorldSchematicRenderer;
import fi.dy.masa.litematica.world.WorldSchematic;

public class ChunkRenderGpuDispatcher
{
	protected final ConcurrentHashMap<Long, ChunkRenderGpuUploader> uploaders;
	protected final WorldSchematic world;
	protected final IWorldSchematicRenderer renderer;

	protected ChunkRenderGpuDispatcher(WorldSchematic world, IWorldSchematicRenderer renderer)
	{
		this.uploaders = new ConcurrentHashMap<>(1024, 0.9f, 2);
		this.world = world;
		this.renderer = renderer;
	}

	protected int size()
	{
		return this.uploaders.size();
	}

	protected ChunkRenderGpuUploader addOrGetUploader(final int cx, final int cz)
	{
		final long index = ChunkPos.asLong(cx, cz);

		try
		{
			if (!this.uploaders.containsKey(index))
			{
				ChunkRenderGpuUploader uploader = new ChunkRenderGpuUploader(this.world, this.renderer);

				uploader.setPosition(cx << 4, this.world.getMinY(), cz << 4);

				this.uploaders.put(index, uploader);
			}

			return this.uploaders.get(index);
		}
		catch (Exception e)
		{
			if (Reference.DEBUG_MODE)
			{
				Litematica.debugLog("addOrGetUploader: Exception obtaining a Chunk Uploader; {}", e.getLocalizedMessage());
			}
		}

		return null;
	}

	protected void removeUploader(final int cx, final int cz)
	{
		final long index = ChunkPos.asLong(cx, cz);

		try
		{
			ChunkRenderGpuUploader oldUploader = this.uploaders.remove(index);

			if (oldUploader != null)
			{
				oldUploader.clear();
			}
		}
		catch (Exception e)
		{
			if (Reference.DEBUG_MODE)
			{
				Litematica.debugLog("removeUploader: Exception removing a Chunk Uploader; {}", e.getLocalizedMessage());
			}
		}
	}

	protected void destroy()
	{
		for (ChunkRenderGpuUploader uploader : this.uploaders.values())
		{
			if (uploader != null)
			{
				uploader.clear();
			}
		}

		this.uploaders.clear();
	}
}
