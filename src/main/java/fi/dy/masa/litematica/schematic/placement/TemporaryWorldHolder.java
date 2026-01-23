package fi.dy.masa.litematica.schematic.placement;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.world.ChunkManagerSchematic;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class TemporaryWorldHolder implements AutoCloseable
{
	private WorldSchematic world;
	private BlockPos origin;
	private Vec3i size;
	private List<Pair<Integer, Integer>> chunks;

	protected TemporaryWorldHolder()
	{
		this.world = null;
		this.origin = BlockPos.ZERO;
		this.size = BlockPos.ZERO;
		this.chunks = new ArrayList<>();
	}

	protected TemporaryWorldHolder(BlockPos origin, Vec3i size)
	{
		this();
		this.createWorld();
		this.calculateChunks(origin, size);
	}

	protected void createWorld()
	{
		this.world = SchematicWorldHandler.createSchematicWorld(null);
	}

	protected void calculateChunks(BlockPos origin, Vec3i size) throws IllegalArgumentException
	{
		this.ensureWorld();
		this.chunks = new ArrayList<>();
		this.origin = origin;
		this.size = size;

		BlockPos posEnd = origin.offset(PositionUtils.getRelativeEndPositionFromAreaSize(size));
		BlockPos posMin = PositionUtils.getMinCorner(origin, posEnd);
		BlockPos posMax = PositionUtils.getMaxCorner(origin, posEnd);
		final int cxMin = posMin.getX() >> 4;
		final int czMin = posMin.getZ() >> 4;
		final int cxMax = posMax.getX() >> 4;
		final int czMax = posMax.getZ() >> 4;

		for (int cz = czMin; cz <= czMax; ++cz)
		{
			for (int cx = cxMin; cx <= cxMax; ++cx)
			{
				this.chunkManager().loadChunk(cx, cz);         // TODO FIXME
				this.chunks.add(Pair.of(cx, cz));
			}
		}
	}

	public boolean hasWorld()
	{
		return this.world != null;
	}

	public boolean isEmpty()
	{
		return this.chunks.isEmpty();
	}

	public @Nullable WorldSchematic world()
	{
		return this.world;
	}

	public BlockPos origin()
	{
		return this.origin;
	}

	public Vec3i size()
	{
		return this.size;
	}

	public List<Pair<Integer, Integer>> chunkList()
	{
		return this.chunks;
	}

	protected @Nullable ChunkManagerSchematic chunkManager() throws IllegalStateException
	{
		this.ensureWorld();
		return this.world.getChunkSource();
	}

	private void ensureWorld() throws IllegalStateException
	{
		if (this.world == null)
		{
			throw new IllegalStateException("TemporaryWorldHolder: No World!");
		}
	}

	protected void clear()
	{
		this.chunks.clear();
		this.origin = BlockPos.ZERO;
		this.size = BlockPos.ZERO;

		if (this.world != null)
		{
			this.world.clearEntities();

			try
			{
				this.world.close();
			}
			catch (Exception ignored)
			{
			}

			this.world = null;
		}
	}

	@Override
	public void close() throws Exception
	{
		this.clear();
	}
}
