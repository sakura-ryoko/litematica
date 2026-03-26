package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.malilib.util.position.Vec3f;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.schematic.ao.AOLightmap;
import fi.dy.masa.litematica.render.schematic.ao.AOProcessor;

public class BlockModelRendererSchematic
{
	private final List<BlockStateModelPart> parts;
	private final BlockPos.MutableBlockPos mutableBlockPos;
	private final QuadInstance quadInst;
	private final BlockTintCache tintCache;
	private final AOLightmap lightmap;
	private AOProcessor processor;
	private boolean useAmbientOcclusion;

    public BlockModelRendererSchematic()
    {
		this.lightmap = new AOLightmap();
		this.parts = new ArrayList<>();
		this.mutableBlockPos = new BlockPos.MutableBlockPos();
		this.quadInst = new QuadInstance();
		this.tintCache = new BlockTintCache();
		this.useAmbientOcclusion = false;
	    this.processor = AOProcessor.get(this.lightmap);
		BlockModelCacheSchematic.INSTANCE.register();
    }

	public static boolean shouldRenderModelSide(BlockAndTintGetter worldIn, BlockState stateIn, BlockPos posIn,
	                                            Direction side, BlockPos mutable)
	{
		return (DataManager.getRenderLayerRange().isPositionAtRenderEdgeOnSide(posIn, side) ||
				Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() &&
				(Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.getBooleanValue())) ||
				Block.shouldRenderFace(stateIn, worldIn.getBlockState(mutable), side);
	}

	public void reload()
	{
		this.reloadLightmap();
		BlockModelCacheSchematic.INSTANCE.onReloadResources();
	}

	public void reloadLightmap()
	{
		this.lightmap.disableCache();
		this.processor = AOProcessor.get(this.lightmap);
	}

	public void enableCache()
	{
		this.lightmap.enableCache();
	}

	public void disableCache()
	{
		this.lightmap.disableCache();
	}

	public void setUseAmbientOcclusion(boolean use)
	{
		this.useAmbientOcclusion = use;
	}

	public boolean tessellateBlock(final BlockAndTintGetter worldIn,
	                               final BlockState stateIn, final BlockPos posIn, final Vec3 pos,
	                               final BlockStateModel modelIn, final long seed,
	                               final IBlockOutputSchematic output)
	{
		BlockModelCacheSchematic.INSTANCE.rand().setSeed(seed);
		modelIn.collectParts(BlockModelCacheSchematic.INSTANCE.rand(), this.parts);

		if (!this.parts.isEmpty())
		{
			boolean ao = this.useAmbientOcclusion && stateIn.getLightEmission() == 0 && this.parts.getFirst().useAmbientOcclusion();

			final Vec3 offset = stateIn.getOffset(posIn);
			final Vec3f v3 = new Vec3f(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);

			try
			{
				if (ao)
				{
//                System.out.printf("renderModelSmooth(): pos [%s] / state [%s] / parts? [%d]\n", posIn.toShortString(), stateIn, modelParts.size());
					return this.tessellateModelSmooth(worldIn, this.parts, stateIn, posIn, v3, output);
				}
				else
				{
//                System.out.printf("renderModelFlat(): pos [%s] / state [%s] / parts? [%d]\n", posIn.toShortString(), stateIn, modelParts.size());
					return this.tessellateModelFlat(worldIn, this.parts, stateIn, posIn, v3, output);
				}
			}
			catch (Throwable throwable)
			{
				//Litematica.logger.error("renderModel: Crash caught: [{}]", !throwable.getMessage().isEmpty() ? throwable.getMessage() : "<EMPTY>");
				CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block model");
				CrashReportCategory crashreportcategory = crashreport.addCategory("Block model being tesselated");
				CrashReportCategory.populateBlockDetails(crashreportcategory, worldIn, posIn, stateIn);
				crashreportcategory.setDetail("Using AO", ao);
				throw new ReportedException(crashreport);
			}
			finally
			{
				this.parts.clear();
				this.lightmap.disableCache();
			}
		}

		return false;
	}

    public boolean tessellateModelFlat(final BlockAndTintGetter worldIn, final List<BlockStateModelPart> modelParts,
                                       final BlockState stateIn, final BlockPos posIn, final Vec3f v3,
                                       final IBlockOutputSchematic out)
    {
	    boolean renderedSomething = false;
	    int isValid = 0;
	    int shouldRenderFace = 0;

        for (BlockStateModelPart part : modelParts)
        {
            for (Direction side : PositionUtils.ALL_DIRECTIONS)
            {
				int mask = 1 << side.ordinal();
				boolean valid = (isValid & mask) == 1;
	            boolean shouldRender = (shouldRenderFace & mask) == 1;

				if (!valid || shouldRender)
				{
					List<BakedQuad> quads = part.getQuads(side);

					if (!quads.isEmpty())
					{
						BlockPos relPos = this.mutableBlockPos.setWithOffset(posIn, side);

						if (!valid)
						{
							shouldRender = shouldRenderModelSide(worldIn, stateIn, posIn, side, relPos);
							isValid |= mask;

							if (shouldRender)
							{
								isValid |= mask;
							}
						}

						if (shouldRender)
						{
							int light = this.lightmap.brightnessCache.getLight(stateIn, worldIn, relPos);
							this.tessellateQuadsFlat(worldIn, stateIn, posIn, quads, light, v3, out);
							renderedSomething = true;
						}
					}
				}
            }

            List<BakedQuad> quads = part.getQuads(null);

            if (!quads.isEmpty())
            {
                this.tessellateQuadsFlat(worldIn, stateIn, posIn, quads, -1, v3, out);
                renderedSomething = true;
            }
        }

        return renderedSomething;
    }


	public boolean tessellateModelSmooth(final BlockAndTintGetter worldIn, final List<BlockStateModelPart> modelParts,
	                                     final BlockState stateIn, final BlockPos posIn, final Vec3f v3,
	                                     final IBlockOutputSchematic out)
	{
		boolean renderedSomething = false;
		int isValid = 0;
		int shouldRenderFace = 0;

		for (BlockStateModelPart part : modelParts)
		{
			for (Direction side : PositionUtils.ALL_DIRECTIONS)
			{
				int mask = 1 << side.ordinal();
				boolean valid = (isValid & mask) == 1;
				boolean shouldRender = (shouldRenderFace & mask) == 1;

				if (!valid || shouldRender)
				{
					List<BakedQuad> quads = part.getQuads(side);

					if (!quads.isEmpty())
					{
						BlockPos relPos = this.mutableBlockPos.setWithOffset(posIn, side);

						if (!valid)
						{
							shouldRender = shouldRenderModelSide(worldIn, stateIn, posIn, side, relPos);
							isValid |= mask;

							if (shouldRender)
							{
								shouldRenderFace |= mask;
							}
						}

						if (shouldRender)
						{
							this.tessellateQuadsSmooth(worldIn, stateIn, posIn, quads, v3, out);
							renderedSomething = true;
						}
					}
				}
			}

			List<BakedQuad> quads = part.getQuads(null);

			if (!quads.isEmpty())
			{
				this.tessellateQuadsSmooth(worldIn, stateIn, posIn, quads, v3, out);
				renderedSomething = true;
			}
		}

		return renderedSomething;
	}

	private void tessellateQuadsFlat(final BlockAndTintGetter world,
	                                 final BlockState state, final BlockPos pos,
	                                 final List<BakedQuad> quads,
	                                 final int light, final Vec3f v3,
	                                 final IBlockOutputSchematic out)
	{
		for (BakedQuad quad : quads)
		{
			this.processor.prepareFlat(world, state, pos, light, quad, this.quadInst);
			this.tessellateQuad(world, state, pos, quad, v3, out);
		}
	}

	private void tessellateQuadsSmooth(final BlockAndTintGetter world,
	                                   final BlockState state, final BlockPos pos,
	                                   final List<BakedQuad> quads,
	                                   final Vec3f v3,
	                                   final IBlockOutputSchematic out)
	{
		for (BakedQuad bakedQuad : quads)
		{
			this.processor.prepareSmooth(world, state, pos, bakedQuad, this.quadInst);
			this.tessellateQuad(world, state, pos, bakedQuad, v3, out);
		}
	}

	private void tessellateQuad(final BlockAndTintGetter world,
	                            final BlockState state, final BlockPos pos, final BakedQuad bakedQuad,
	                            final Vec3f v3, final IBlockOutputSchematic out)
    {
		int tint = bakedQuad.materialInfo().tintIndex();

	    if (tint != -1)
	    {
			this.quadInst.multiplyColor(this.tintCache.get(world, state, pos, tint));
	    }

	    out.put(v3.x, v3.y, v3.z, bakedQuad, this.quadInst);
    }
}
