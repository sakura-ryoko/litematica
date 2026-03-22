package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockTintCache
{
	private final List<BlockTintSource> sources;
	private final IntList tints;
	private int tintIndex;
	private int lastTint;
	private boolean initialized;

	protected BlockTintCache()
	{
		this.sources = new ArrayList<>();
		this.tints = new IntArrayList();
		this.tintIndex = -1;
		this.initialized = false;
	}

	protected int get(final BlockAndTintGetter world, final BlockState state, final BlockPos pos, final int tintIndex)
	{
		if (this.tintIndex == tintIndex)
		{
			return this.lastTint;
		}
		else
		{
			int tint = this.calculate(world, state, pos, tintIndex);

			this.tintIndex = tintIndex;
			this.lastTint = tint;

			return tint;
		}
	}

	private int calculate(final BlockAndTintGetter world, final BlockState state, final BlockPos pos, final int tintIndex)
	{
		if (!this.initialized)
		{
			this.configure(state);
			this.initialized = true;
		}

		if (tintIndex >= this.sources.size())
		{
			return -1;
		}
		else
		{
			BlockTintSource source = this.sources.set(tintIndex, null);

			if (source != null)
			{
				int value = source.colorInWorld(state, world, pos);
				this.tints.set(tintIndex, value);
				return value;
			}
			else
			{
				return this.tints.getInt(tintIndex);
			}
		}
	}

	private void configure(final BlockState state)
	{
		List<BlockTintSource> sources = BlockModelCacheSchematic.INSTANCE.blockColors().getTintSources(state);
		int count = sources.size();

		if (count > 0)
		{
			this.sources.addAll(sources);

			for (int i = 0; i < count; i++)
			{
				this.tints.add(-1);
			}
		}
	}
}
