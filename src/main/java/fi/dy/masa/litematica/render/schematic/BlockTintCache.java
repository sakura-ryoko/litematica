package fi.dy.masa.litematica.render.schematic;

import java.util.List;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BlockTintCache
{
	private final List<BlockTintSource> tintSources;
	private final IntList tintValues;
	private BlockColors blockColors;
	private int lastTintIndex;
	private int lastTintValue;
	private boolean initialized;

	protected BlockTintCache()
	{
		this.tintSources = new ObjectArrayList<>();
		this.tintValues = new IntArrayList();
		this.lastTintIndex = -1;
		this.lastTintValue = -1;
		this.initialized = false;
	}

	protected void onReloadResources()
	{
		this.blockColors = BlockModelCacheSchematic.INSTANCE.blockColors();
	}

	private BlockColors blockColors()
	{
		if (this.blockColors == null)
		{
			this.blockColors = BlockModelCacheSchematic.INSTANCE.blockColors();
		}

		return this.blockColors;
	}

	protected int get(final BlockAndTintGetter world, final BlockState state, final BlockPos pos, final int tintIndex)
	{
		if (this.lastTintIndex == tintIndex)
		{
			return this.lastTintValue;
		}
		else
		{
			int tint = this.calculate(world, state, pos, tintIndex);

			this.lastTintIndex = tintIndex;
			this.lastTintValue = tint;

			return tint;
		}
	}

	private void configure(final BlockState state)
	{
		List<BlockTintSource> sources = this.blockColors().getTintSources(state);
		int count = sources.size();

		if (count > 0)
		{
			this.tintSources.addAll(sources);

			for (int i = 0; i < count; i++)
			{
				this.tintValues.add(-1);
			}
		}
	}

	private int calculate(final BlockAndTintGetter world, final BlockState state, final BlockPos pos, final int tintIndex)
	{
		if (!this.initialized)
		{
			this.configure(state);
			this.initialized = true;
		}

		if (tintIndex >= this.tintSources.size())
		{
			return -1;
		}
		else
		{
			BlockTintSource source = this.tintSources.set(tintIndex, null);

			if (source != null)
			{
				int value = source.colorInWorld(state, world, pos);
				this.tintValues.set(tintIndex, value);
				return value;
			}
			else
			{
				return this.tintValues.getInt(tintIndex);
			}
		}
	}

	protected void resetTintCache()
	{
		this.lastTintIndex = -1;

		if (this.initialized)
		{
			this.tintSources.clear();
			this.tintValues.clear();
			this.lastTintValue = -1;
			this.initialized = false;
		}
	}
}
