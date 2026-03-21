package fi.dy.masa.litematica.schematic.placement;

import java.util.function.Supplier;

import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.malilib.interfaces.DefaultThreadTaskBase;
import fi.dy.masa.litematica.world.WorldSchematic;

public abstract class PlacementManagerTask extends DefaultThreadTaskBase
{
	private final Supplier<WorldSchematic> worldSupplier;
	private final int chunkX;
	private final int chunkZ;
	private final ChunkPos chunkPos;
	private final Long chunkLong;

	protected PlacementManagerTask(Supplier<WorldSchematic> worldSupplier, int chunkX, int chunkZ)
	{
		super();
		this.worldSupplier = worldSupplier;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.chunkPos = new ChunkPos(chunkX, chunkZ);
		this.chunkLong = this.chunkPos.pack();
	}

	protected PlacementManagerTask(Supplier<WorldSchematic> worldSupplier, ChunkPos chunkPos)
	{
		super();
		this.worldSupplier = worldSupplier;
		this.chunkX = chunkPos.x();
		this.chunkZ = chunkPos.z();
		this.chunkPos = chunkPos;
		this.chunkLong = chunkPos.pack();
	}

	protected PlacementManagerTask(Supplier<WorldSchematic> worldSupplier, Long longPos)
	{
		super();
		this.worldSupplier = worldSupplier;
		this.chunkX = ChunkPos.getX(longPos);
		this.chunkZ = ChunkPos.getZ(longPos);
		this.chunkPos = new ChunkPos(this.chunkX, chunkZ);
		this.chunkLong = longPos;
	}

	protected Supplier<WorldSchematic> worldSupplier()
	{
		return this.worldSupplier;
	}

	protected int cx()
	{
		return this.chunkX;
	}

	protected int cz()
	{
		return this.chunkZ;
	}

	protected ChunkPos pos()
	{
		return this.chunkPos;
	}

	protected Long asLong()
	{
		return this.chunkLong;
	}

	protected abstract Runnable buildTask();
}
