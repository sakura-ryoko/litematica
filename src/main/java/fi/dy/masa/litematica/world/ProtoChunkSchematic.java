package fi.dy.masa.litematica.world;

import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ImposterProtoChunk;

import fi.dy.masa.litematica.util.WorldPlacingUtils;

/**
 * Used when building chunks from {@link WorldPlacingUtils}
 * Also temporarily holds references to entity spawning positions
 */
public class ProtoChunkSchematic extends ImposterProtoChunk
{
	private final CopyOnWriteArrayList<Pair<WorldPlacingUtils.EntityPosAndRot, CompoundTag>> entities = new  CopyOnWriteArrayList<>();

	public ProtoChunkSchematic(@Nonnull ChunkSchematic chunk)
	{
		super(chunk,true);
	}

	@Override
	public @Nonnull ChunkSchematic getWrapped()
	{
		return (ChunkSchematic) super.getWrapped();
	}

	@Override
	public @Nonnull BlockState getBlockState(@Nonnull BlockPos pos)
	{
		return this.getWrapped().getBlockState(pos);
	}

	@Override
	public BlockState setBlockState(@Nonnull BlockPos pos, @Nonnull BlockState newState, @Block.UpdateFlags int flags)
	{
		return this.getWrapped().setBlockState(pos, newState, flags);
	}

	@Override
	public void setBlockEntity(@Nonnull BlockEntity te)
	{
		this.getWrapped().setBlockEntity(te);
	}

	@Nullable
	public BlockEntity createBlockEntity(BlockPos pos)
	{
		return this.getWrapped().createBlockEntity(pos);
	}

	public ChunkSchematicState getState()
	{
		return this.getWrapped().getState();
	}

	public void setState(ChunkSchematicState state)
	{
		this.getWrapped().setState(state);
	}

	public synchronized void addEntityPairForLater(Pair<WorldPlacingUtils.EntityPosAndRot, CompoundTag> entity)
	{
		this.entities.add(entity);
	}

	public synchronized void spawnAllEntitiesNow(@Nonnull Level world)
	{
		this.entities.forEach(
				entityPair ->
						WorldPlacingUtils.spawnEntityToWorldNow(world, entityPair));

		this.entities.clear();
	}

	public int getEntityCount()
	{
		return this.entities.size();
	}

	public synchronized void clear()
	{
		this.entities.clear();
	}
}
