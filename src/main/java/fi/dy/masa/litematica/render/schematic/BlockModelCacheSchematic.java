package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModelWrapper;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import fi.dy.masa.litematica.mixin.model.IMixinBlockModelSet;
import fi.dy.masa.litematica.mixin.model.IMixinBlockStateModelSet;
import fi.dy.masa.litematica.mixin.model.IMixinFluidStateModelSet;
import fi.dy.masa.litematica.mixin.model.IMixinModelManager;

public class BlockModelCacheSchematic
{
	public static final BlockModelCacheSchematic INSTANCE = new BlockModelCacheSchematic();
	private static final SingleThreadedRandomSource RAND = new SingleThreadedRandomSource(0L);
	private static final Matrix4fc MATRIX = new Matrix4f();
	private final ConcurrentHashMap<BlockState, BlockStateModel> blockStateModelCache;
	private final ConcurrentHashMap<BlockState, BlockModel> blockModelCache;
	private final ConcurrentHashMap<Fluid, FluidModel> fluidModelCache;
	private ModelManager modelManager;
	private BlockStateModelSet blockStateModelSet;
	private FluidStateModelSet fluidStateModelSet;
	private BlockModelSet blockModelSet;
	private BlockColors blockColors;

	private BlockModelCacheSchematic()
	{
		this.blockStateModelCache = new ConcurrentHashMap<>(256, 0.9f, 1);
		this.blockModelCache = new ConcurrentHashMap<>(256, 0.9f, 1);
		this.fluidModelCache = new ConcurrentHashMap<>(32, 0.9f, 1);
	}

	protected void register()
	{
		// Init / Registered
		if (this.modelManager == null)
		{
			this.refresh();
		}
	}

	protected RandomSource rand()
	{
		return RAND;
	}

	protected AtlasManager atlas()
	{
		return ((IMixinModelManager) this.modelManager).litematica_getAtlasManager();
	}

	protected PlayerSkinRenderCache skinCache()
	{
		return ((IMixinModelManager) this.modelManager).litematica_getPlayerSkinRenderCache();
	}

	protected BlockColors blockColors()
	{
		return this.blockColors;
	}

	protected ModelManager modelManager()
	{
		return this.modelManager;
	}

	protected FluidStateModelSet fluidStateModelSet()
	{
		return this.fluidStateModelSet;
	}

	protected EntityModelSet entityModelSet()
	{
		return this.modelManager.entityModels().get();
	}

	private void refresh()
	{
		Minecraft mc = Minecraft.getInstance();
//		this.blockModelResolver = ((IMixinMinecraft) this.mc).malilib_getBlockModelResolver();
		this.modelManager = mc.getModelManager();
		this.blockStateModelSet = this.modelManager.getBlockStateModelSet();
		this.blockModelSet = this.modelManager.getBlockModelSet();
		this.fluidStateModelSet = this.modelManager.getFluidStateModelSet();
		this.blockColors = ((IMixinBlockModelSet) this.blockModelSet).litematica_getBlockColors();

		synchronized (this.blockStateModelCache)
		{
			this.blockStateModelCache.clear();
			this.blockStateModelCache.putAll(((IMixinBlockStateModelSet) this.blockStateModelSet).litematica_getModelMap());
		}

		synchronized (this.blockModelCache)
		{
			this.blockModelCache.clear();
			this.blockModelCache.putAll(((IMixinBlockModelSet) this.blockModelSet).litematica_getBlockModelCache());
		}

		synchronized (this.fluidModelCache)
		{
			this.fluidModelCache.clear();
			this.fluidModelCache.putAll(((IMixinFluidStateModelSet) this.fluidStateModelSet).litematica_getModelByFluid());
		}
	}

	protected void onReloadResources()
	{
		this.refresh();
	}

	public int stateModelSize()
	{
		return this.blockStateModelCache.size();
	}

	public int modelSize()
	{
		return this.blockModelCache.size();
	}

	public BlockStateModel fetchBlockStateModel(BlockState state)
	{
		BlockStateModel model;

		if (this.blockStateModelCache.containsKey(state))
		{
			synchronized (this.blockStateModelCache)
			{
				model = this.blockStateModelCache.get(state);
			}
		}
		else
		{
			model = this.blockStateModelSet.get(state);

			synchronized (this.blockStateModelCache)
			{
				this.blockStateModelCache.put(state, model);
			}
		}

		if (model != null && this.checkBlockStateModel(model))
		{
			return model;
		}

		return ((IMixinBlockStateModelSet) this.blockStateModelSet).litematica_getMissingModel();
	}

	public boolean checkBlockStateModel(BlockStateModel model)
	{
		List<BlockStateModelPart> parts = this.getBlockStateModelParts(model);
		if (parts.isEmpty()) { return false; }
		int totalSize = 0;

		for (BlockStateModelPart part : parts)
		{
			for (Direction face : Direction.values())
			{
				totalSize += this.getBlockStateModelPartFace(part, face).size();
			}

			totalSize += this.getBlockStateModelPartFace(part, null).size();
		}

		return totalSize > 0;
	}

	public List<BlockStateModelPart> getBlockStateModelParts(BlockStateModel model)
	{
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RAND, parts);
		return parts;
	}

	public List<BakedQuad> getBlockStateModelPartFace(BlockStateModelPart part, @Nullable Direction face)
	{
		return part.getQuads(face);
	}

	public BlockModel fetchBlockModel(BlockState state)
	{
		BlockModel model;

		if (this.blockModelCache.containsKey(state))
		{
			synchronized (this.blockModelCache)
			{
				model = this.blockModelCache.get(state);
			}
		}
		else
		{
			model = this.blockModelSet.get(state);

			synchronized (this.blockModelCache)
			{
				this.blockModelCache.put(state, model);
			}
		}

		if (model != null)
		{
			return model;
		}

		return new BlockStateModelWrapper(this.fetchBlockStateModel(state), this.blockColors.getTintSources(state), MATRIX);
	}

	public void updateBlockRenderState(final BlockModelRenderState renderState, final BlockState state, final BlockDisplayContext context)
	{
		renderState.clear();
		this.fetchBlockModel(state).update(renderState, state, context, 42L);
	}

	public void updateItemFrameRenderState(final BlockModelRenderState renderState, final boolean glowing, boolean map)
	{
		this.updateBlockRenderState(renderState, BlockStateDefinitions.getItemFrameFakeState(glowing, map), ItemFrameRenderer.BLOCK_DISPLAY_CONTEXT);
	}

	public FluidModel fetchFluidModel(FluidState state)
	{
		FluidModel model;
		final Fluid fluid = state.getType();

		if (this.fluidModelCache.containsKey(fluid))
		{
			synchronized (this.fluidModelCache)
			{
				model = this.fluidModelCache.get(fluid);
			}
		}
		else
		{
			model = this.fluidStateModelSet.get(state);

			synchronized (this.fluidModelCache)
			{
				this.fluidModelCache.put(fluid, model);
			}
		}

		if (model != null)
		{
			return model;
		}

		return ((IMixinFluidStateModelSet) this.fluidStateModelSet).litematica_getMissingModel();
	}
}
