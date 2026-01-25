package fi.dy.masa.litematica.render.schematic.ao;

import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.litematica.render.IWorldSchematicRenderer;

public class AOBrightness
{
	private boolean enabled;
	private final Long2IntLinkedOpenHashMap intCache = Util.make(() ->
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
	private final Long2FloatLinkedOpenHashMap floatCache = Util.make(() ->
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
		long l = pos.asLong();
		int i = this.intCache.get(l);

		if (i != Integer.MAX_VALUE)
		{
			return i;
		}
		else
		{
			int j = IWorldSchematicRenderer.LightGetter.DEFAULT.packedLight(world, pos);

			if (this.intCache.size() == 100)
			{
				this.intCache.removeFirstInt();
			}

			this.intCache.put(l, j);

			return j;
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
		this.intCache.clear();
		this.floatCache.clear();
	}

	public int getInt(BlockState state, BlockAndTintGetter world, BlockPos pos)
	{
//		long l = pos.asLong();
//		int i;
//
//		if (this.enabled)
//		{
//			i = this.intCache.get(l);
//
//			if (i != Integer.MAX_VALUE)
//			{
//				return i;
//			}
//		}
//
//		i = WorldRendererSchematic.getLightmap(world, pos);
//
//		if (this.enabled)
//		{
//			if (this.intCache.size() == 100)
//			{
//				this.intCache.removeFirstInt();
//			}
//
//			this.intCache.put(l, i);
//		}
//
//		return i;

		return IWorldSchematicRenderer.getLightmap(this.enabled
		                                          ? this.lightGetter
		                                          : IWorldSchematicRenderer.LightGetter.DEFAULT, world, state, pos);
	}

	public float getFloat(BlockState state, BlockAndTintGetter blockView, BlockPos pos)
	{
		long l = pos.asLong();

		if (this.enabled)
		{
			float f = this.floatCache.get(l);

			if (!Float.isNaN(f))
			{
				return f;
			}
		}

		float f = state.getShadeBrightness(blockView, pos);

		if (this.enabled)
		{
			if (this.floatCache.size() == 100)
			{
				this.floatCache.removeFirstFloat();
			}

			this.floatCache.put(l, f);
		}

		return f;
	}
}
