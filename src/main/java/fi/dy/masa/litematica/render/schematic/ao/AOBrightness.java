package fi.dy.masa.litematica.render.schematic.ao;

import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.litematica.render.IWorldSchematicRenderer;

// Cache
public class AOBrightness
{
	private boolean enabled;
	private final Long2IntLinkedOpenHashMap colors = Util.make(() ->
	                                                             {
		                                                             Long2IntLinkedOpenHashMap long2IntLinkedOpenHashMap = new Long2IntLinkedOpenHashMap(100, 0.25F)
		                                                             {
			                                                             protected void rehash(int newN)
			                                                             {
			                                                             }
		                                                             };
		                                                             long2IntLinkedOpenHashMap.defaultReturnValue(Integer.MAX_VALUE);
		                                                             return long2IntLinkedOpenHashMap;
	                                                             });
	private final Long2FloatLinkedOpenHashMap brightness = Util.make(() ->
	                                                                 {
		                                                                 Long2FloatLinkedOpenHashMap long2FloatLinkedOpenHashMap = new Long2FloatLinkedOpenHashMap(100, 0.25F)
		                                                                 {
			                                                                 protected void rehash(int newN)
			                                                                 {
			                                                                 }
		                                                                 };
		                                                                 long2FloatLinkedOpenHashMap.defaultReturnValue(Float.NaN);
		                                                                 return long2FloatLinkedOpenHashMap;
	                                                                 });
	private final IWorldSchematicRenderer.LightGetter lightGetter = (world, pos) ->
	{
		long key = pos.asLong();
		int color = this.colors.get(key);

		if (color != Integer.MAX_VALUE)
		{
			return color;
		}
		else
		{
			int val = IWorldSchematicRenderer.LightGetter.DEFAULT.packedLight(world, pos);

			if (this.colors.size() == 100)
			{
				this.colors.removeFirstInt();
			}

			this.colors.put(key, val);

			return val;
		}
	};

	public AOBrightness() { }

	public boolean isEnabled()
	{
		return this.enabled;
	}

	public void enable()
	{
		this.enabled = true;
	}

	public void disable()
	{
		this.enabled = false;
		this.colors.clear();
		this.brightness.clear();
	}

	public int getLight(BlockState state, BlockAndTintGetter world, BlockPos pos)
	{
		return IWorldSchematicRenderer.getLightmap(this.enabled
		                                          ? this.lightGetter
		                                          : IWorldSchematicRenderer.LightGetter.DEFAULT, world, state, pos);
	}

	public float getShade(BlockState state, BlockAndTintGetter blockView, BlockPos pos)
	{
		long key = pos.asLong();

		if (this.enabled)
		{
			float bright = this.brightness.get(key);

			if (!Float.isNaN(bright))
			{
				return bright;
			}
		}

		float val = state.getShadeBrightness(blockView, pos);

		if (this.enabled)
		{
			if (this.brightness.size() == 100)
			{
				this.brightness.removeFirstFloat();
			}

			this.brightness.put(key, val);
		}

		return val;
	}
}
