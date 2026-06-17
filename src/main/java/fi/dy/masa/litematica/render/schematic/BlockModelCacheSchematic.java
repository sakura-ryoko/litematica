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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import fi.dy.masa.malilib.util.position.PositionUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.mixin.model.*;
import fi.dy.masa.litematica.mixin.render.IMixinGameRenderer;

public class BlockModelCacheSchematic
{
	public static final BlockModelCacheSchematic INSTANCE = new BlockModelCacheSchematic();
	private static final SingleThreadedRandomSource RAND = new SingleThreadedRandomSource(0L);
	private static final Matrix4fc MATRIX = new Matrix4f();
	private final ConcurrentHashMap<BlockState, BlockStateModel> blockStateModelCache;
	private final ConcurrentHashMap<BlockState, BlockModel> blockModelCache;
	private final ConcurrentHashMap<Fluid, FluidModel> fluidModelCache;
	private final Minecraft mc;
	private ModelManager modelManager;
	private BlockModelResolver blockModelResolver;
	private ItemModelResolver itemModelResolver;
	private BlockStateModelSet blockStateModelSet;
	private BlockModelSet blockModelSet;
	private BlockColors blockColors;
	private FluidStateModelSet fluidStateModelSet;
	private EntityModelSet entityModelSet;
	private SpriteGetter spriteGetter;
	private PlayerSkinRenderCache skinCache;

//	private BlockModelRendererSchematic blockModelRenderer;
	private BlockEntityRenderDispatcher blockEntityRenderDispatcher;
	private EntityRenderDispatcher entityRenderDispatcher;
//	private FluidModelRendererSchematic fluidRenderer;
	private FogRenderer fogRenderer;

	private BlockModelCacheSchematic()
	{
		this.blockStateModelCache = new ConcurrentHashMap<>(256, 0.9f, 1);
		this.blockModelCache = new ConcurrentHashMap<>(256, 0.9f, 1);
		this.fluidModelCache = new ConcurrentHashMap<>(32, 0.9f, 1);
		this.mc = Minecraft.getInstance();
	}

	protected RandomSource rand()
	{
		return RAND;
	}

	protected ModelManager modelManager()
	{
		return this.modelManager;
	}

	protected BlockModelResolver blockModelResolver()
	{
		return this.blockModelResolver;
	}

	protected ItemModelResolver itemModelResolver()
	{
		return this.itemModelResolver;
	}

	protected BlockStateModelSet blockStateModelSet()
	{
		return this.blockStateModelSet;
	}

	protected BlockModelSet blockModelSet()
	{
		return this.blockModelSet;
	}

	protected BlockColors blockColors()
	{
		if (this.blockColors == null)
		{
			this.blockColors = BlockColors.createDefault();
		}

		return this.blockColors;
	}

	protected FluidStateModelSet fluidStateModelSet()
	{
		return this.fluidStateModelSet;
	}

	protected EntityModelSet entityModelSet()
	{
		return this.entityModelSet;
	}

	protected SpriteGetter spriteGetter()
	{
		return this.spriteGetter;
	}

	protected PlayerSkinRenderCache skinCache()
	{
		return this.skinCache;
	}

//	protected BlockModelRendererSchematic blockModelRenderer()
//	{
//		if (this.blockModelRenderer == null)
//		{
//			this.blockModelRenderer = new BlockModelRendererSchematic();
//		}
//
//		return this.blockModelRenderer;
//	}

	protected BlockEntityRenderDispatcher blockEntityRenderer()
	{
		if (this.blockEntityRenderDispatcher == null)
		{
			this.blockEntityRenderDispatcher = this.mc.getBlockEntityRenderDispatcher();
		}

		return this.blockEntityRenderDispatcher;
	}

	protected EntityRenderDispatcher entityRenderer()
	{
		if (this.entityRenderDispatcher == null)
		{
			this.entityRenderDispatcher = this.mc.getEntityRenderDispatcher();
		}

		return this.entityRenderDispatcher;
	}

//	protected FluidModelRendererSchematic fluidRenderer()
//	{
//		if (this.fluidRenderer == null)
//		{
//			this.fluidRenderer = new FluidModelRendererSchematic(this.fluidStateModelSet);
//		}
//
//		return this.fluidRenderer;
//	}

	protected FogRenderer fogRenderer()
	{
		if (this.fogRenderer == null)
		{
			this.fogRenderer = ((IMixinGameRenderer) this.mc.gameRenderer).litematica_getFogRenderer();
		}

		return this.fogRenderer;
	}

	private void refresh()
	{
		Litematica.LOGGER.info("BlockModelCacheSchematic: refreshing model cache");
		this.modelManager = this.mc.getModelManager();
		this.blockModelResolver = ((IMixinEntityRenderDispatcher) this.mc.getEntityRenderDispatcher()).litematica_getBlockModelResolver();
		this.itemModelResolver = ((IMixinMinecraft) this.mc).litematica_getItemModelResolver();
		this.blockStateModelSet = this.modelManager.getBlockStateModelSet();
		this.blockModelSet = this.modelManager.getBlockModelSet();
		this.blockColors = BlockColors.createDefault();
		this.fluidStateModelSet = this.modelManager.getFluidStateModelSet();
		this.entityModelSet = this.modelManager.entityModels().get();
		this.spriteGetter = ((IMixinModelManager) this.modelManager).litematica_getAtlasManager();
		this.skinCache = ((IMixinModelManager) this.modelManager).litematica_getPlayerSkinRenderCache();
		this.rebuildCache();
	}

	private void rebuildCache()
	{
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

	private void refreshRenderers()
	{
//		this.blockModelRenderer = new BlockModelRendererSchematic();
		this.entityRenderDispatcher = this.mc.getEntityRenderDispatcher();
		this.blockEntityRenderDispatcher = this.mc.getBlockEntityRenderDispatcher();
//		this.fluidRenderer = new FluidModelRendererSchematic(this.fluidStateModelSet);
		this.fogRenderer = ((IMixinGameRenderer) this.mc.gameRenderer).litematica_getFogRenderer();
	}

	protected void onLoadRenderers()
	{
		if (this.modelManager == null)
		{
			this.refresh();
		}

		this.refreshRenderers();
	}

	public void onReloadResources()
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

	public int fluidSize()
	{
		return this.fluidModelCache.size();
	}

	@Nullable
	public BlockStateModel fetchBlockStateModel(BlockState state)
	{
		BlockStateModel model = this.blockStateModelCache.computeIfAbsent(state, k -> this.blockStateModelSet.get(k));

		if (model != null && this.checkBlockStateModel(model))
		{
			return model;
		}

		if (!state.hasBlockEntity())
		{
			if (Reference.DEBUG_MODE)
			{
				Litematica.LOGGER.warn("fetchBlockStateModel: Block State Model not found for state [{}]", state.toString());
			}

			return ((IMixinBlockStateModelSet) this.blockStateModelSet).litematica_getMissingModel();
		}

		return null;
	}

	public boolean checkBlockStateModel(BlockStateModel model)
	{
		List<BlockStateModelPart> parts = this.getBlockStateModelParts(model);
		if (parts.isEmpty()) { return false; }
		int totalSize = 0;

		for (BlockStateModelPart part : parts)
		{
			for (Direction face : PositionUtils.ALL_DIRECTIONS)
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

	@Nullable
	public BlockModel fetchBlockModel(BlockState state)
	{
		BlockModel model = this.blockModelCache.computeIfAbsent(state, k -> this.blockModelSet.get(k));

		if (model != null)
		{
			return model;
		}

		BlockStateModel stateModel = this.fetchBlockStateModel(state);

		if (stateModel != null)
		{
			return new BlockStateModelWrapper(stateModel, this.blockColors().getTintSources(state), MATRIX);
		}

		if (Reference.DEBUG_MODE)
		{
			Litematica.LOGGER.warn("fetchBlockModel: Block Model not found for state [{}]", state.toString());
		}

		return null;
	}

	public void updateBlockRenderState(final BlockModelRenderState renderState, final BlockState state, final BlockDisplayContext context)
	{
		renderState.clear();
		BlockModel model = this.fetchBlockModel(state);

		if (model != null)
		{
			model.update(renderState, state, context, 42L);
		}
	}

	public void updateItemFrameRenderState(final BlockModelRenderState renderState, final boolean glowing, boolean map)
	{
		this.updateBlockRenderState(renderState, BlockStateDefinitions.getItemFrameFakeState(glowing, map), ItemFrameRenderer.BLOCK_DISPLAY_CONTEXT);
	}

	@Nullable
	public FluidModel fetchFluidModel(FluidState state)
	{
		final Fluid fluid = state.getType();
		FluidModel model = this.fluidModelCache.computeIfAbsent(fluid, k -> this.fluidStateModelSet.get(state));

		if (model != null)
		{
			return model;
		}

		if (Reference.DEBUG_MODE)
		{
			Litematica.LOGGER.warn("fetchFluidModel: Fluid Model not found for state [{}]", state.toString());
		}

		return ((IMixinFluidStateModelSet) this.fluidStateModelSet).litematica_getMissingModel();
	}
}
