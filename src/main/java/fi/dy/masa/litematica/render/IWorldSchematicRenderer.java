package fi.dy.masa.litematica.render;

import java.util.Collection;
import java.util.List;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Brightness;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.render.uniform.ChunkFixUniform;
import fi.dy.masa.litematica.world.ChunkSchematicState;
import fi.dy.masa.litematica.world.WorldSchematic;

/**
 * The purpose of adding this as an Interface is so that we can
 * test a "replacement" Schematic World Renderer system in the future;
 * more easily using this common interface via {@link LitematicaRenderer}
 * without needing to change almost any other code.
 * <br>
 * The goal with any future Renderer is to off load *all* Meshing
 * tasks into a separate thread; while maintaining the drawing, and
 * resorting tasks in the Game's Rendering thread; along with anything
 * else that is calling {@link com.mojang.blaze3d.systems.RenderSystem};
 * such as building the {@link com.mojang.blaze3d.buffers.GpuBuffer} objects.
 */
public interface IWorldSchematicRenderer
{
	void markNeedsUpdate();

	boolean hasWorld();

	String getDebugInfoRenders();

	String getDebugInfoEntities();

	ProfilerFiller getProfiler();

	EntityRenderDispatcher getEntityRenderer();

	BlockEntityRenderDispatcher getBlockEntityRenderer();

	void setWorldAndLoadRenderers(@Nullable WorldSchematic world);

	void loadRenderers(@Nullable ProfilerFiller profiler);

	void reloadBlockRenderManager(BlockRenderDispatcher dispatcher);

	ChunkFixUniform getChunkFixUniform();

	void updateCameraState(Camera camera, float tickProgress);

	void setupTerrain(Camera camera, Frustum frustum, int frameCount, boolean playerSpectator, ProfilerFiller profiler);

	void updateChunks(long finishTimeNano, ProfilerFiller profiler);

	void capturePreMainValues(Camera camera, GpuBufferSlice fogBuffer, ProfilerFiller profiler);

	int prepareBlockLayers(Matrix4fc matrix4fc, double cameraX, double cameraY, double cameraZ, ProfilerFiller profiler);

	<T extends Comparable<T>> BlockState getFallbackState(BlockState origState);

	boolean hasQuadsForModel(List<BlockModelPart> modelParts, BlockState state, @Nullable Direction side);

	boolean hasQuadsForModelPart(BlockModelPart modelPart, BlockState state, @Nullable Direction side);

	BlockStateModel getModelForState(BlockState state);

	List<BlockModelPart> getModelParts(BlockPos pos, BlockState state, RandomSource rand);

	boolean renderBlock(BlockAndTintGetter world, BlockState state, BlockPos pos, PoseStack matrices, BufferBuilder bufferBuilderIn);

	void renderFluid(BlockAndTintGetter world, BlockState blockState, FluidState fluidState, BlockPos pos, BufferBuilder bufferBuilderIn);

	void drawBlockLayerGroup(ChunkSectionLayerGroup group, @Nullable GpuSampler sampler);

	void scheduleTranslucentSorting(Vec3 cameraPos, ProfilerFiller profiler);

	void prepareEntities(Camera camera, Frustum frustum, LevelRenderState renderStates, DeltaTracker tickCounter, ProfilerFiller profiler);

	void renderEntities(Camera camera, Frustum frustum, PoseStack matrices, LevelRenderState renderStates, SubmitNodeCollector queue, ProfilerFiller profiler);

	void prepareBlockEntities(Camera camera, Frustum frustum, LevelRenderState renderStates, PoseStack matrices, float tickProgress, ProfilerFiller profiler);

	void renderBlockEntities(Camera camera, Frustum frustum, PoseStack matrices, LevelRenderState renderStates, SubmitNodeCollector queue, ProfilerFiller profiler);

	void updateBlockEntities(Collection<BlockEntity> toRemove, Collection<BlockEntity> toAdd);

	void renderBlockOverlays(Camera camera, float lineWidth, ProfilerFiller profiler);

	void scheduleChunkRenders(int chunkX, int chunkZ, boolean immediate);

	ChunkSchematicState getChunkSchematicState(int chunkX, int chunkZ);

	void setChunkSchematicState(int chunkX, int chunkZ, ChunkSchematicState state);

	void clearBlockBatchDraw();

	void clearChunkFixUniform();

	void clearWorldRenderStates();

	static int getLightmap(BlockAndTintGetter world, BlockPos pos)
	{
		return getLightmap(LightGetter.DEFAULT, world, world.getBlockState(pos), pos);
	}

	static int getLightmap(LightGetter getter, BlockAndTintGetter world, BlockState state, BlockPos pos)
	{
		if (state.emissiveRendering(world, pos))
		{
			return 15728880;
		}

		int light = getter.packedLight(world, pos);
		int blockLight = LightTexture.block(light);
		int luminance = state.getLightEmission();

		if (blockLight < luminance)
		{
			return LightTexture.pack(luminance, LightTexture.sky(light));
		}

		return light;
	}

	@FunctionalInterface
	interface LightGetter
	{
		LightGetter DEFAULT = (world, pos) ->
				Brightness.pack(world.getBrightness(LightLayer.BLOCK, pos), world.getBrightness(LightLayer.SKY, pos));

		int packedLight(BlockAndTintGetter world, BlockPos pos);
	}
}
