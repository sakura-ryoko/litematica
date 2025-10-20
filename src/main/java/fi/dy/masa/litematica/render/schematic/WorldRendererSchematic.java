package fi.dy.masa.litematica.render.schematic;

import java.lang.Math;
import java.util.*;
import javax.annotation.Nullable;
import org.joml.*;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.DynamicUniforms;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.*;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.property.Property;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.BlockRenderView;

import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.entity.IMixinEntity;
import fi.dy.masa.litematica.mixin.render.IMixinGameRenderer;
import fi.dy.masa.litematica.render.schematic.blocks.FallbackBlocks;
import fi.dy.masa.litematica.util.IEntityInvoker;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.WorldSchematic;

public class WorldRendererSchematic
{
    private final MinecraftClient mc;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final BlockRenderManager blockRenderManager;
    private final BlockModelRendererSchematic blockModelRenderer;
    private final Set<BlockEntity> blockEntities = new HashSet<>();
    private final List<ChunkRendererSchematicVbo> renderInfos = new ArrayList<>(1024);
//    private final BufferBuilderStorage bufferBuilders;
	private final HashMap<Block, Block> fallbackBlocks;
    private Set<ChunkRendererSchematicVbo> chunksToUpdate = new LinkedHashSet<>();
    private WorldSchematic world;
    private ChunkRenderDispatcherSchematic chunkRendererDispatcher;
    private FogRenderer fogRenderer;
    private ChunkRenderBatchDraw batchDraw;
    private GpuBufferSlice vanillaFogBuffer;
    private Profiler profiler;
    private double lastCameraChunkUpdateX = Double.MIN_VALUE;
    private double lastCameraChunkUpdateY = Double.MIN_VALUE;
    private double lastCameraChunkUpdateZ = Double.MIN_VALUE;
    private double lastCameraX = Double.MIN_VALUE;
    private double lastCameraY = Double.MIN_VALUE;
    private double lastCameraZ = Double.MIN_VALUE;
    private float lastCameraPitch = Float.MIN_VALUE;
    private float lastCameraYaw = Float.MIN_VALUE;
    private ChunkRenderDispatcherLitematica renderDispatcher;
    private final IChunkRendererFactory renderChunkFactory;
    //private ShaderGroup entityOutlineShader;
    //private boolean entityOutlinesRendered;

    private int renderDistanceChunks = -1;
    private int renderEntitiesStartupCounter = 2;
    private int countEntitiesTotal;
    private int countEntitiesRendered;
    private int countEntitiesHidden;

    private double lastTranslucentSortX;
    private double lastTranslucentSortY;
    private double lastTranslucentSortZ;
    private boolean displayListEntitiesDirty = true;
    private boolean shouldDraw;

    public WorldRendererSchematic(MinecraftClient mc)
    {
        this.mc = mc;
//        this.bufferBuilders = mc.getBufferBuilders();
        this.renderChunkFactory = ChunkRendererSchematicVbo::new;
        this.blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        this.entityRenderDispatcher = mc.getEntityRenderDispatcher();
        this.blockEntityRenderDispatcher = mc.getBlockEntityRenderDispatcher();
        this.blockModelRenderer = new BlockModelRendererSchematic(mc.getBlockColors());
        this.blockModelRenderer.setBakedManager(mc.getBakedModelManager());
        this.fogRenderer = ((IMixinGameRenderer) mc.gameRenderer).litematica_getFogRenderer();
		this.fallbackBlocks = new HashMap<>();
        this.profiler = null;
        this.vanillaFogBuffer = null;
        this.batchDraw = null;
        this.shouldDraw = false;
		this.buildFallbackBlocks();
    }

    public void markNeedsUpdate()
    {
        this.displayListEntitiesDirty = true;
    }

    public boolean hasWorld()
    {
        return this.world != null;
    }

    public String getDebugInfoRenders()
    {
        int rcTotal = this.chunkRendererDispatcher != null ? this.chunkRendererDispatcher.getRendererCount() : 0;
        int rcRendered = this.chunkRendererDispatcher != null ? this.getRenderedChunks() : 0;
        return String.format("C: %d/%d %sD: %d, L: %d, %s", rcRendered, rcTotal, this.mc.chunkCullingEnabled ? "(s) " : "", this.renderDistanceChunks, 0, this.renderDispatcher == null ? "null" : this.renderDispatcher.getDebugInfo());
    }

    public String getDebugInfoEntities()
    {
        return "E: " + this.countEntitiesRendered + "/" + this.countEntitiesTotal + ", B: " + this.countEntitiesHidden;
    }

    protected ChunkRenderDispatcherLitematica getRenderDispatcher()
    {
        return this.renderDispatcher;
    }

    protected int getRenderedChunks()
    {
        int count = 0;

        for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
        {
            // Threaded Code
            //ChunkRenderDataSchematic data = chunkRenderer.chunkRenderData.get();
            ChunkRenderDataSchematic data = chunkRenderer.chunkRenderData;

            if (data != ChunkRenderDataSchematic.EMPTY && !data.isBlockLayerEmpty())
            {
                ++count;
            }
        }

        return count;
    }

    protected Profiler getProfiler()
    {
        if (this.profiler == null)
        {
            this.profiler = Profilers.get();
            this.profiler.startTick();
        }

        return this.profiler;
    }

    protected EntityRenderDispatcher getEntityRenderer()
    {
        return this.entityRenderDispatcher;
    }

    protected BlockEntityRenderDispatcher getBlockEntityRenderer()
    {
        return this.blockEntityRenderDispatcher;
    }

	private void buildFallbackBlocks()
	{
		this.fallbackBlocks.put(Blocks.BLACK_STAINED_GLASS, FallbackBlocks.BLACK_GLASS);
		this.fallbackBlocks.put(Blocks.BLUE_STAINED_GLASS, FallbackBlocks.BLUE_GLASS);
		this.fallbackBlocks.put(Blocks.BROWN_STAINED_GLASS, FallbackBlocks.BROWN_GLASS);
		this.fallbackBlocks.put(Blocks.CYAN_STAINED_GLASS, FallbackBlocks.CYAN_GLASS);
		this.fallbackBlocks.put(Blocks.GLASS, FallbackBlocks.GLASS);
		this.fallbackBlocks.put(Blocks.GRAY_STAINED_GLASS, FallbackBlocks.GRAY_GLASS);
		this.fallbackBlocks.put(Blocks.GREEN_STAINED_GLASS, FallbackBlocks.GREEN_GLASS);
		this.fallbackBlocks.put(Blocks.LIME_STAINED_GLASS, FallbackBlocks.LIME_GLASS);
		this.fallbackBlocks.put(Blocks.LIGHT_BLUE_STAINED_GLASS, FallbackBlocks.LT_BLUE_GLASS);
		this.fallbackBlocks.put(Blocks.LIGHT_GRAY_STAINED_GLASS, FallbackBlocks.LT_GRAY_GLASS);
		this.fallbackBlocks.put(Blocks.MAGENTA_STAINED_GLASS, FallbackBlocks.MAGENTA_GLASS);
		this.fallbackBlocks.put(Blocks.ORANGE_STAINED_GLASS, FallbackBlocks.ORANGE_GLASS);
		this.fallbackBlocks.put(Blocks.PINK_STAINED_GLASS, FallbackBlocks.PINK_GLASS);
		this.fallbackBlocks.put(Blocks.PURPLE_STAINED_GLASS, FallbackBlocks.PURPLE_GLASS);
		this.fallbackBlocks.put(Blocks.RED_STAINED_GLASS, FallbackBlocks.RED_GLASS);
		this.fallbackBlocks.put(Blocks.TINTED_GLASS, FallbackBlocks.TINTED_GLASS);
		this.fallbackBlocks.put(Blocks.WHITE_STAINED_GLASS, FallbackBlocks.WHITE_GLASS);
		this.fallbackBlocks.put(Blocks.YELLOW_STAINED_GLASS, FallbackBlocks.YELLOW_GLASS);

		this.fallbackBlocks.put(Blocks.BLACK_STAINED_GLASS_PANE, FallbackBlocks.BLACK_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.BLUE_STAINED_GLASS_PANE, FallbackBlocks.BLUE_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.BROWN_STAINED_GLASS_PANE, FallbackBlocks.BROWN_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.CYAN_STAINED_GLASS_PANE, FallbackBlocks.CYAN_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.GLASS_PANE, FallbackBlocks.GLASS_PANE);
		this.fallbackBlocks.put(Blocks.GRAY_STAINED_GLASS_PANE, FallbackBlocks.GRAY_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.GREEN_STAINED_GLASS_PANE, FallbackBlocks.GREEN_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.LIME_STAINED_GLASS_PANE, FallbackBlocks.LIME_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, FallbackBlocks.LT_BLUE_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, FallbackBlocks.LT_GRAY_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.MAGENTA_STAINED_GLASS_PANE, FallbackBlocks.MAGENTA_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.ORANGE_STAINED_GLASS_PANE, FallbackBlocks.ORANGE_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.PINK_STAINED_GLASS_PANE, FallbackBlocks.PINK_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.PURPLE_STAINED_GLASS_PANE, FallbackBlocks.PURPLE_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.RED_STAINED_GLASS_PANE, FallbackBlocks.RED_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.WHITE_STAINED_GLASS_PANE, FallbackBlocks.WHITE_GLASS_PANE);
		this.fallbackBlocks.put(Blocks.YELLOW_STAINED_GLASS_PANE, FallbackBlocks.YELLOW_GLASS_PANE);
	}

	private <T extends Comparable<T>> BlockState getFallbackState(BlockState origState)
	{
		Collection<Property<?>> props = origState.getProperties();

		if (this.fallbackBlocks.containsKey(origState.getBlock()))
		{
//			Litematica.LOGGER.warn("getFallbackState: Invalid Block State/Block Model for block [{}]; but we found a matching Litematica fallback block state that you can use.  Perhaps you have the Fusion mod installed?", origState.getBlock().getName().getString());
			BlockState newState = this.fallbackBlocks.get(origState.getBlock()).getDefaultState();

			for (Property<?> entry : props)
			{
				@SuppressWarnings("unchecked")
				Property<T> p = (Property<T>) entry;

				if (newState.contains(p))
				{
					T value = origState.get(p);

					if (!newState.get(p).equals(value))
					{
						newState = newState.with(p, value);
					}
				}
			}

			Litematica.debugLog("Fallback Block State -- OLD: %s --> NEW: %s", origState.toString(), newState.toString());
			return newState;
		}

		return origState;
	}

    protected GpuBufferSlice getEmptyFogBuffer()
    {
        if (this.fogRenderer == null)
        {
            this.fogRenderer = ((IMixinGameRenderer) this.mc.gameRenderer).litematica_getFogRenderer();
        }

        return this.fogRenderer.getFogBuffer(FogRenderer.FogType.NONE);
    }

    public void setWorldAndLoadRenderers(@Nullable WorldSchematic worldSchematic)
    {
        this.lastCameraChunkUpdateX = Double.MIN_VALUE;
        this.lastCameraChunkUpdateY = Double.MIN_VALUE;
        this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
        //this.renderManager.setWorld(worldClientIn);
        this.world = worldSchematic;

        if (worldSchematic != null)
        {
            this.loadRenderers(this.profiler);
        }
        else
        {
            this.chunksToUpdate.forEach(ChunkRendererSchematicVbo::deleteGlResources);
            this.chunksToUpdate.clear();
            this.renderInfos.forEach(ChunkRendererSchematicVbo::deleteGlResources);
            this.renderInfos.clear();

            if (this.chunkRendererDispatcher != null)
            {
                this.chunkRendererDispatcher.delete();
                this.chunkRendererDispatcher = null;
            }

            if (this.renderDispatcher != null)
            {
                this.renderDispatcher.stopWorkerThreads();
            }

            this.renderDispatcher = null;
            this.profiler = null;

            this.clearBlockBatchDraw();

            if (this.vanillaFogBuffer != null)
            {
                this.vanillaFogBuffer = null;
            }

            synchronized (this.blockEntities)
            {
                this.blockEntities.clear();
            }
        }
    }

    public void loadRenderers(@Nullable Profiler profiler)
    {
        if (this.hasWorld())
        {
            if (profiler == null)
            {
                profiler = Profilers.get();
            }

            this.profiler = profiler;

            profiler.push("load_renderers");

            if (this.renderDispatcher == null)
            {
                this.renderDispatcher = new ChunkRenderDispatcherLitematica(profiler);
            }

            this.displayListEntitiesDirty = true;
            this.renderDistanceChunks = this.mc.options.getViewDistance().getValue() + 2;

            if (this.chunkRendererDispatcher != null)
            {
                this.chunkRendererDispatcher.delete();
            }

            this.stopChunkUpdates(profiler);
            this.clearBlockBatchDraw();

            synchronized (this.blockEntities)
            {
                this.blockEntities.clear();
            }

            this.chunkRendererDispatcher = new ChunkRenderDispatcherSchematic(this.world, this.renderDistanceChunks, this, this.renderChunkFactory);
            this.renderEntitiesStartupCounter = 2;

            profiler.pop();
        }
    }

    protected void stopChunkUpdates(Profiler profiler)
    {
        if (!this.chunksToUpdate.isEmpty())
        {
            this.chunksToUpdate.forEach(ChunkRendererSchematicVbo::deleteGlResources);
        }

        this.chunksToUpdate.clear();
        this.renderDispatcher.stopChunkUpdates(profiler);
        this.profiler = null;
        this.clearBlockBatchDraw();
        this.vanillaFogBuffer = null;
    }

    public void setupTerrain(Camera camera, Frustum frustum, int frameCount, boolean playerSpectator, Profiler profiler)
    {
        this.profiler = profiler;
        profiler.push("setup_terrain");

        if (this.chunkRendererDispatcher == null ||
            this.mc.options.getViewDistance().getValue() + 2 != this.renderDistanceChunks)
        {
            this.loadRenderers(profiler);
        }

        Entity entity = EntityUtils.getCameraEntity();

        if (this.mc.player == null) return;
        if (entity == null)
        {
            entity = this.mc.player;
        }

        //camera.update(this.world, entity, this.mc.options.perspective > 0, this.mc.options.perspective == 2, this.mc.getTickDelta());

        profiler.swap("camera");

        double entityX = entity.getX();
        double entityY = entity.getY();
        double entityZ = entity.getZ();
        double diffX = entityX - this.lastCameraChunkUpdateX;
        double diffY = entityY - this.lastCameraChunkUpdateY;
        double diffZ = entityZ - this.lastCameraChunkUpdateZ;

        if (diffX * diffX + diffY * diffY + diffZ * diffZ > 256.0)
        {
            this.lastCameraChunkUpdateX = entityX;
            this.lastCameraChunkUpdateY = entityY;
            this.lastCameraChunkUpdateZ = entityZ;
            this.chunkRendererDispatcher.removeOutOfRangeRenderers();
        }

        profiler.swap("renderlist_camera");

        Vec3d cameraPos = camera.getPos();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        this.renderDispatcher.setCameraPosition(cameraPos);

        profiler.swap("culling");
        BlockPos viewPos = BlockPos.ofFloored(cameraX, cameraY + (double) entity.getStandingEyeHeight(), cameraZ);
        final int centerChunkX = (viewPos.getX() >> 4);
        final int centerChunkZ = (viewPos.getZ() >> 4);
        final int renderDistance = this.mc.options.getViewDistance().getValue() + 2;
        ChunkPos viewChunk = new ChunkPos(viewPos);

        this.displayListEntitiesDirty = this.displayListEntitiesDirty || !this.chunksToUpdate.isEmpty() ||
                entityX != this.lastCameraX ||
                entityY != this.lastCameraY ||
                entityZ != this.lastCameraZ ||
                entity.getPitch() != this.lastCameraPitch ||
                entity.getYaw() != this.lastCameraYaw;
        this.lastCameraX = cameraX;
        this.lastCameraY = cameraY;
        this.lastCameraZ = cameraZ;
        this.lastCameraPitch = camera.getPitch();
        this.lastCameraYaw = camera.getYaw();

        profiler.swap("update");

        if (this.displayListEntitiesDirty)
        {
            //profiler.push("fetch");

            this.displayListEntitiesDirty = false;
            this.renderInfos.clear();

            profiler.push("update_sort");
            List<ChunkPos> positions = DataManager.getSchematicPlacementManager().getAndUpdateVisibleChunks(viewChunk);
            //positions.sort(new SubChunkPos.DistanceComparator(viewSubChunk));

            //Queue<SubChunkPos> queuePositions = new PriorityQueue<>(new SubChunkPos.DistanceComparator(viewSubChunk));
            //queuePositions.addAll(set);

            //if (GuiBase.isCtrlDown()) System.out.printf("sorted positions: %d\n", positions.size());

            profiler.swap("update_iteration");

            //while (queuePositions.isEmpty() == false)
            for (ChunkPos chunkPos : positions)
            {
                //SubChunkPos subChunk = queuePositions.poll();
                int cx = chunkPos.x;
                int cz = chunkPos.z;
                //Litematica.logger.warn("setupTerrain() [WorldRenderer] positions[{}] chunkPos: {} // isLoaded: {}", i, chunkPos.toString(), this.world.getChunkProvider().isChunkLoaded(cx, cz));
                // Only render sub-chunks that are within the client's render distance, and that
                // have been already properly loaded on the client
                if (Math.abs(cx - centerChunkX) <= renderDistance &&
                    Math.abs(cz - centerChunkZ) <= renderDistance &&
                    this.world.getChunkProvider().isChunkLoaded(cx, cz))
                {
                    ChunkRendererSchematicVbo chunkRenderer = this.chunkRendererDispatcher.getChunkRenderer(cx, cz);

                    if (chunkRenderer != null && frustum.isVisible(chunkRenderer.getBoundingBox()))
                    {
                        //if (GuiBase.isCtrlDown()) System.out.printf("add @ %s\n", subChunk);
                        if (chunkRenderer.needsUpdate() && chunkPos.equals(viewChunk))
                        {
                            chunkRenderer.setNeedsUpdate(true);
                        }

                        this.renderInfos.add(chunkRenderer);
                    }
                }
            }

            profiler.pop(); // fetch (update_sort)
        }

        profiler.swap("rebuild_near");
        Set<ChunkRendererSchematicVbo> set = this.chunksToUpdate;
        this.chunksToUpdate = new LinkedHashSet<>();

        for (ChunkRendererSchematicVbo chunkRendererTmp : this.renderInfos)
        {
            if (chunkRendererTmp.needsUpdate() || set.contains(chunkRendererTmp))
            {
                this.displayListEntitiesDirty = true;
                BlockPos pos = chunkRendererTmp.getOrigin().add(8, 8, 8);
                boolean isNear = pos.getSquaredDistance(viewPos) < 1024.0D;

                if (!chunkRendererTmp.needsImmediateUpdate() && !isNear)
                {
                    this.chunksToUpdate.add(chunkRendererTmp);
                }
                else
                {
                    //if (GuiBase.isCtrlDown()) System.out.printf("====== update now\n");
                    profiler.push("update_now");
                    this.profiler = profiler;

                    this.renderDispatcher.updateChunkNow(chunkRendererTmp, profiler);
                    chunkRendererTmp.clearNeedsUpdate();

                    profiler.pop();
                }
            }
        }

        this.chunksToUpdate.addAll(set);
        this.clearBlockBatchDraw();

        //profiler.pop();
        profiler.pop();     // setup_terrain
    }

    public void updateChunks(long finishTimeNano, Profiler profiler)
    {
        this.profiler = profiler;
        profiler.push("run_chunk_uploads");
        this.displayListEntitiesDirty |= this.renderDispatcher.runChunkUploads(finishTimeNano, profiler);

        if (this.profiler == null)
        {
            this.profiler = profiler;
        }

        profiler.swap("check_update");

        if (!this.chunksToUpdate.isEmpty())
        {
            Iterator<ChunkRendererSchematicVbo> iterator = this.chunksToUpdate.iterator();
            int index = 0;

            while (iterator.hasNext())
            {
                ChunkRendererSchematicVbo renderChunk = iterator.next();
                boolean flag;

                if (renderChunk.needsImmediateUpdate())
                {
                    flag = this.renderDispatcher.updateChunkNow(renderChunk, profiler);
                }
                else
                {
                    flag = this.renderDispatcher.updateChunkLater(renderChunk, profiler);
                }

                if (!flag)
                {
                    break;
                }

                renderChunk.clearNeedsUpdate();
                iterator.remove();
                long i = finishTimeNano - System.nanoTime();

                if (i < 0L)
                {
                    break;
                }
                index++;
            }
        }

        profiler.pop();
    }

    public void capturePreMainValues(GpuBufferSlice fogBuffer, Profiler profiler)
    {
        this.vanillaFogBuffer = fogBuffer;
        this.profiler = profiler;
    }

    public int prepareBlockLayers(Matrix4fc matrix4fc,
                                   double cameraX, double cameraY, double cameraZ,
                                   Profiler profiler)
    {
        this.profiler = profiler;
        RenderSystem.assertOnRenderThread();

        profiler.push("layer_multi_phase");

//        renderLayer.startDrawing();

        ArrayList<DynamicUniforms.UniformValue> uniformValues = new ArrayList<>();
        EnumMap<BlockRenderLayer, List<RenderPass.RenderObject<GpuBufferSlice[]>>> renderMap = new EnumMap<>(BlockRenderLayer.class);
//        Vec3d cameraPos = camera.getPos();

        for (BlockRenderLayer layer : BlockRenderLayer.values())
        {
            renderMap.put(layer, new ArrayList<>());
        }

        profiler.swap("layer_setup");

//        boolean reverse = renderLayer.isTranslucent();
//        int startIndex = reverse ? this.renderInfos.size() - 1 : 0;
//        int stopIndex = reverse ? -1 : this.renderInfos.size();
//        int increment = reverse ? -1 : 1;
//        int count = 0;

        int startIndex = 0;
        int stopIndex = this.renderInfos.size();
        int increment = 1;
        int indexCount = 0;
        int count = 0;

//        Fog orgFog = RenderSystem.getShaderFog();
//        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());

        boolean renderAsTranslucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
        boolean renderCollidingBlocks = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();
        Matrix4f matrix4f = new Matrix4f();
        Vector4f colorVector;
//        int color = -1;

        if (renderAsTranslucent)
        {
            colorVector = new Vector4f(1.0F, 1.0F, 1.0F, (float) Configs.Visuals.GHOST_BLOCK_ALPHA.getDoubleValue());
        }
        else
        {
            colorVector = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
        }

//        profiler.swap("layer_uniforms");

//        RenderSystem.setShaderFog(this.getEmptyFogBuffer());
        boolean startedDrawing = false;

        profiler.swap("layer_iteration");
        this.profiler = profiler;

        for (int i = startIndex; i != stopIndex; i += increment)
        {
            ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);

            for (BlockRenderLayer layer : BlockRenderLayer.values())
            {
                profiler.swap("layer_"+ layer.getName());

                if (!renderer.getChunkRenderData().isBlockLayerEmpty(layer))
                {
                    BlockPos chunkOrigin = renderer.getOrigin();
                    ChunkRenderObjectBuffers buffers = renderer.getBlockBuffersByBlockLayer(layer);

                    if (buffers == null || buffers.isClosed() || !renderer.getChunkRenderData().getBuiltBufferCache().hasBuiltBufferByBlockLayer(layer))
                    {
//                    Litematica.LOGGER.error("Layer [{}], ChunkOrigin [{}], NO BUFFERS!", ChunkRenderLayers.getFriendlyName(layer), chunkOrigin.toShortString());
                        continue;
                    }

                    GpuBuffer vertexBuffer;
                    VertexFormat.IndexType indexType;

                    if (buffers.getIndexBuffer() == null)
                    {
                        if (buffers.getIndexCount() > indexCount)
                        {
                            indexCount = buffers.getIndexCount();
                        }

                        vertexBuffer = null;
                        indexType = null;
                    }
                    else
                    {
                        vertexBuffer = buffers.getIndexBuffer();
                        indexType = buffers.getIndexType();
                    }

                    int pos = uniformValues.size();
                    uniformValues.add(new DynamicUniforms.UniformValue(
                            matrix4fc, colorVector,
                            new Vector3f((float) (chunkOrigin.getX() - cameraX), (float) (chunkOrigin.getY() - cameraY), (float) (chunkOrigin.getZ() - cameraZ)),
                            matrix4f, 1.0f
                    ));

                    renderMap.get(layer)
                            .add(new RenderPass.RenderObject<>(
                                    0, buffers.getVertexBuffer(),
                                    vertexBuffer, indexType,
                                    0, buffers.getIndexCount(),
                                    (slices, uploader) ->
                                            uploader.upload("DynamicTransforms", ((GpuBufferSlice[]) slices)[pos])
                            ));

                    startedDrawing = true;
                    ++count;

                }
            }
        }

//        profiler.swap("layer_cleanup");

//        if (renderAsTranslucent)
//        {
//            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
//        }

        if (startedDrawing)
        {
            GpuBufferSlice[] bufferSlices = RenderSystem.getDynamicUniforms()
                                                        .writeAll(
                                                                uniformValues.toArray(new DynamicUniforms.UniformValue[0])
                                                        );

//        renderLayer.endDrawing();
//        RenderSystem.setShaderFog(origFogBuffer);

            this.batchDraw = new ChunkRenderBatchDraw(renderMap, renderCollidingBlocks, renderAsTranslucent, indexCount, bufferSlices);
            this.shouldDraw = true;
        }

        profiler.pop();     // layer+ X

        return count;
    }

    public void drawBlockLayerGroup(BlockRenderLayerGroup group)
    {
        if (this.batchDraw != null && this.shouldDraw)
        {
            this.profiler.push(Reference.MOD_ID + "_batch_draw_" + group.getName());

            // Disable fog in the Schematic World
            RenderSystem.setShaderFog(this.getEmptyFogBuffer());
            this.batchDraw.draw(group, this.profiler);
            RenderSystem.setShaderFog(this.vanillaFogBuffer);

            this.profiler.pop();
        }
    }

    public void clearBlockBatchDraw()
    {
        if (this.batchDraw != null)
        {
            this.batchDraw = null;
        }

        this.shouldDraw = false;
    }

    public void scheduleTranslucentSorting(Vec3d cameraPos, Profiler profiler)
    {
        double x = cameraPos.getX();
        double y = cameraPos.getY();
        double z = cameraPos.getZ();

        this.profiler = profiler;
        double diffX = x - this.lastTranslucentSortX;
        double diffY = y - this.lastTranslucentSortY;
        double diffZ = z - this.lastTranslucentSortZ;

        if (diffX * diffX + diffY * diffY + diffZ * diffZ > 1.0D)
        {
            this.lastTranslucentSortX = x;
            this.lastTranslucentSortY = y;
            this.lastTranslucentSortZ = z;
            int h = 0;

            for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
            {
                if ((chunkRenderer.getChunkRenderData().isBlockLayerStarted(BlockRenderLayer.TRANSLUCENT) ||
                    (chunkRenderer.getChunkRenderData() != ChunkRenderDataSchematic.EMPTY && chunkRenderer.hasOverlay())) && h++ < 15)
                {
                    this.renderDispatcher.updateTransparencyLater(chunkRenderer, profiler);
                }
            }
        }
    }

    public void renderBlockOverlays(Camera camera, float lineWidth, Profiler profiler)
    {
        this.profiler = profiler;
        this.renderBlockOverlay(OverlayRenderType.OUTLINE, camera, lineWidth, profiler);
        this.renderBlockOverlay(OverlayRenderType.QUAD, camera, lineWidth, profiler);
    }

    protected void renderBlockOverlay(OverlayRenderType type, Camera camera, float lineWidth, Profiler profiler)
    {
        profiler.push("overlay_" + type.name());
        this.profiler = profiler;

//        RenderUtils.blend(true);
        // ???
//        RenderSystem.defaultBlendFunc();

        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();
//        RenderLayer renderLayer = type.getRenderLayer();
        RenderPipeline pipeline = renderThrough ? type.getRenderThrough() : type.getPipeline();

        float[] offset = new float[]{0.3f, 0.0f, 0.6f};

        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
//        MatrixStack matrices = new MatrixStack();

//        ArrayList<RenderPass.RenderObject> arrayList = new ArrayList<>();
//        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
//        int indexCount = 0;
//        boolean startedDrawing = false;

//        GlUniform chunkOffsetUniform = shader.modelOffset;

        profiler.swap("overlay_iterate");
//        renderLayer.startDrawing();
        this.profiler = profiler;

        for (int i = this.renderInfos.size() - 1; i >= 0; --i)
        {
            ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);

            if (renderer.getChunkRenderData() != ChunkRenderDataSchematic.EMPTY && renderer.hasOverlay())
            {
                ChunkRenderDataSchematic compiledChunk = renderer.getChunkRenderData();

                if (!compiledChunk.isOverlayTypeEmpty(type))
                {
                    ChunkRenderObjectBuffers buffers = renderer.getOverlayBuffersByType(type);
                    BlockPos chunkOrigin = renderer.getOrigin();

                    if (buffers == null || buffers.isClosed() || !renderer.getChunkRenderData().getBuiltBufferCache().hasBuiltBufferByType(type))
                    {
//                        Litematica.LOGGER.error("Overlay [{}], ChunkOrigin [{}], NO BUFFERS", type.name(), chunkOrigin.toShortString());
                        continue;
                    }

//                    if (buffers == null)
//                    {
//                        Litematica.LOGGER.error("Overlay [{}], ChunkOrigin [{}], NO BUFFERS (NULL)", type.name(), chunkOrigin.toShortString());
//                        continue;
//                    }
//
//                    if (buffers.isClosed())
//                    {
//                        Litematica.LOGGER.error("Overlay [{}], ChunkOrigin [{}], NO BUFFERS (CLOSED)", type.name(), chunkOrigin.toShortString());
//                        continue;
//                    }
//
//                    if (!renderer.getChunkRenderData().getBuiltBufferCache().hasBuiltBufferByType(type))
//                    {
//                        Litematica.LOGGER.error("Overlay [{}], ChunkOrigin [{}], NO BUFFERS (NO DATA)", type.name(), chunkOrigin.toShortString());
//                        continue;
//                    }

//                    if (chunkOffsetUniform != null)
//                    {
//                        chunkOffsetUniform.set((float)(chunkOrigin.getX() - x), (float)(chunkOrigin.getY() - y), (float)(chunkOrigin.getZ() - z));
//                        chunkOffsetUniform.upload();
//                    }

//                    RenderSystem.backupProjectionMatrix();
                    matrix4fStack.pushMatrix();
//                    matrices.push();
//                    matrix4fStack.mul(matrices.peek().getPositionMatrix());
                    matrix4fStack.translate((float) (chunkOrigin.getX() - x), (float) (chunkOrigin.getY() - y), (float) (chunkOrigin.getZ() - z));

                    this.drawInternal(pipeline, buffers, -1, offset, lineWidth, false, false, (type == OverlayRenderType.OUTLINE));

//                    arrayList.add(new RenderPass.
//                            RenderObject(0, buffers.getVertexBuffer(), gpuBuffer, indexType, 0, buffers.getIndexCount(),
//                                         uniform -> uniform.upload("ModelOffset", (float) (chunkOrigin.getX() - x), (float) (chunkOrigin.getY() - y), (float) (chunkOrigin.getZ() - z))));

//                    startedDrawing = true;

                    matrix4fStack.popMatrix();
//                    matrices.pop();
//                    RenderSystem.restoreProjectionMatrix();
                }
            }
        }

//        if (chunkOffsetUniform != null)
//        {
//            chunkOffsetUniform.set(0.0F, 0.0F, 0.0F);
//        }

//        renderLayer.endDrawing();
//        RenderUtils.blend(false);
        profiler.pop();
    }

    public boolean renderBlock(BlockRenderView world, BlockState state, BlockPos pos, MatrixStack matrixStack, BufferBuilder bufferBuilderIn)
    {
        this.getProfiler().push("render_block");
        try
        {
            BlockRenderType renderType = state.getRenderType();

            if (renderType == BlockRenderType.INVISIBLE)
            {
                this.getProfiler().pop();
                return false;
            }
            else
            {
                boolean result;
                long seed = state.getRenderingSeed(pos);
                List<BlockModelPart> parts = this.getModelParts(pos, state, Random.create(seed));

                BlockModelRendererSchematic.enableCache();
                result = renderType == BlockRenderType.MODEL &&
                        this.blockModelRenderer.renderModel(world, parts, state, pos, matrixStack, bufferBuilderIn, seed);
                BlockModelRendererSchematic.disableCache();

                //System.out.printf("renderBlock(): result [%s]\n", result);

                // TODO --> For testing the Vanilla Block Model Renderer
                /*
                BlockModelRenderer.enableBrightnessCache();
                this.blockRenderManager.renderBlock(state, pos, world, matrixStack, bufferBuilderIn, true, Random.create(state.getRenderingSeed(pos)));
                result = true;
                BlockModelRenderer.disableBrightnessCache();
                 */

                this.getProfiler().pop();
                return result;
            }
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.create(throwable, "Tesselating block in world");
            CrashReportSection crashreportcategory = crashreport.addElement("Block being tesselated");
            CrashReportSection.addBlockInfo(crashreportcategory, world, pos, state);
            this.getProfiler().pop();
            throw new CrashException(crashreport);
        }
    }

    public void renderFluid(BlockRenderView world, BlockState blockState, FluidState fluidState, BlockPos pos, BufferBuilder bufferBuilderIn)
    {
        this.getProfiler().push("render_fluid");
        // Sometimes this collides with FAPI
        try
        {
            this.blockRenderManager.renderFluid(pos, world, bufferBuilderIn, blockState, fluidState);
        }
        catch (Exception ignored) { }
        this.getProfiler().pop();
    }

    // Probably not the most efficient way; but it works.
    private void drawInternal(RenderPipeline pipeline,
                              ChunkRenderObjectBuffers buffers,
                              int color, float[] offset, float lineWidth,
                              boolean useColor,  boolean useOffset, boolean setLineWidth) throws RuntimeException
    {
        if (RenderSystem.isOnRenderThread())
        {
            Vector4f colorMod = new Vector4f(1f, 1f, 1f, 1f);
            Vector3f modelOffset = new Vector3f();
            Matrix4f texMatrix = new Matrix4f();
            float line = 0.0f;

            if (useOffset)
            {
                RenderSystem.setModelOffset(offset[0], offset[1], offset[2]);
                modelOffset.set(offset);
            }

            if (useColor)
            {
                float[] rgba = {ColorHelper.getRedFloat(color), ColorHelper.getGreenFloat(color), ColorHelper.getBlueFloat(color), ColorHelper.getAlphaFloat(color)};
//                RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
                colorMod.set(rgba);
            }

            if (setLineWidth)
            {
                line = lineWidth;
            }

            Framebuffer mainFb = RenderUtils.fb();
            GpuTextureView texture1 = mainFb.getColorAttachmentView();
            GpuTextureView texture2 = mainFb.useDepthAttachment ? mainFb.getDepthAttachmentView() : null;

//            Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] --> setup IndexBuffer", buffers.getName());
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;

            if (buffers.getIndexBuffer() == null)
            {
                if (buffers.getIndexCount() > 0)
                {
                    indexBuffer = shapeIndexBuffer.getIndexBuffer(buffers.getIndexCount());
                    indexType = shapeIndexBuffer.getIndexType();
                }
                else
                {
                    Litematica.LOGGER.error("WorldRendererSchematic#drawInternal() [{}] --> setup IndexBuffer --> NO INDEX COUNT!", buffers.getName());
                    return;
                }
            }
            else
            {
                indexBuffer = buffers.getIndexBuffer();
                indexType = buffers.getIndexType();
            }

//            Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] --> new renderPass", buffers.getName());

            GpuBufferSlice gpuSlice = RenderSystem.getDynamicUniforms()
                                                  .write(
                                                          RenderSystem.getModelViewMatrix(),
                                                          colorMod,
                                                          modelOffset,
                                                          texMatrix,
                                                          line);

            // Attach Frame buffers
            try (RenderPass pass = RenderSystem.getDevice()
                                               .createCommandEncoder()
                                               .createRenderPass(() -> "litematica:drawInternal/schematic_overlay",
                                                                 texture1, OptionalInt.empty(),
                                                                 texture2, OptionalDouble.empty()))
            {
//                Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] renderPass --> setPipeline() [{}]", buffers.getName(), pipeline.getLocation().toString());
                pass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", gpuSlice);

//                Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] renderPass --> setVertexBuffer() [0]", buffers.getName());
                pass.setVertexBuffer(0, buffers.getVertexBuffer());
//                Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] renderPass --> setIndexBuffer() [{}]", buffers.getName(), indexType.name());

                pass.setIndexBuffer(indexBuffer, indexType);
//                Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] renderPass --> drawIndexed() [0, {}]", buffers.getName(), buffers.getIndexCount());

                pass.drawIndexed(0, 0, buffers.getIndexCount(), 1);
            }

//            Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] --> END", buffers.getName());

            if (useOffset)
            {
                RenderSystem.resetModelOffset();
            }
        }
    }

    public boolean hasQuadsForModel(List<BlockModelPart> modelParts, BlockState state, @Nullable Direction side)
    {
        BlockModelPart part = modelParts.getFirst();

        if (side != null)
        {
            List<BakedQuad> list = part.getQuads(side);

            return !list.isEmpty();
        }

        for (Direction entry : Direction.values())
        {
            List<BakedQuad> list = part.getQuads(side);

            if (!list.isEmpty())
            {
                return true;
            }
        }

        return false;
    }

    public boolean hasQuadsForModelPart(BlockModelPart modelPart, BlockState state, @Nullable Direction side)
    {
        if (side != null)
        {
            List<BakedQuad> list = modelPart.getQuads(side);

            return !list.isEmpty();
        }

        for (Direction entry : Direction.values())
        {
            List<BakedQuad> list = modelPart.getQuads(side);

            if (!list.isEmpty())
            {
                return true;
            }
        }

        return false;
    }

    public BlockStateModel getModelForState(BlockState state)
    {
        return this.blockRenderManager.getModels().getModel(state);
    }

    public List<BlockModelPart> getModelParts(BlockPos pos, BlockState state, Random rand)
    {
        rand.setSeed(state.getRenderingSeed(pos));
        List<BlockModelPart> parts = this.getModelForState(state).getParts(rand);

        if (parts.isEmpty())
        {
			// Try Fallback Blocks first.
	        parts = this.getModelForState(this.getFallbackState(state)).getParts(rand);
        }

		if (parts.isEmpty())
		{
			parts = this.getModelForState(state.getBlock().getDefaultState()).getParts(rand);
			Litematica.LOGGER.warn("getModelParts: Invalid Block Model for block at [{}] with state [{}]; Attempting to reset to default.", pos.toShortString(), state.toString());
		}

        return parts;
    }

    public void renderEntities(Camera camera, Frustum frustum, MatrixStack matrices, VertexConsumerProvider.Immediate immediate, float partialTicks, Profiler profiler)
    {
        this.profiler = profiler;

        if (this.renderEntitiesStartupCounter > 0)
        {
            --this.renderEntitiesStartupCounter;
        }
        else
        {
            profiler.push("entities_prepare");

            double cameraX = camera.getPos().x;
            double cameraY = camera.getPos().y;
            double cameraZ = camera.getPos().z;

            this.entityRenderDispatcher.configure(this.world, camera, this.mc.targetedEntity);

            this.countEntitiesTotal = 0;
            this.countEntitiesRendered = 0;
            this.countEntitiesHidden = 0;

            this.countEntitiesTotal = this.world.getRegularEntityCount();

            profiler.swap("regular");
            //List<Entity> entitiesMultipass = Lists.<Entity>newArrayList();

            // TODO --> Convert Matrix4f back to to MatrixStack?
            //  Causes strange entity behavior (translations not applied)
            //  if this is missing ( Including the push() and pop() ... ?)
            //  Doing this restores the expected behavior of Entity Rendering in the Schematic World

//            MatrixStack matrixStack = new MatrixStack();
//            matrixStack.push();
//            matrixStack.multiplyPositionMatrix(posMatrix);
//            matrixStack.pop();

//            VertexConsumerProvider.Immediate entityVertexConsumers = this.bufferBuilders.getEntityVertexConsumers();
            LayerRange layerRange = DataManager.getRenderLayerRange();

            profiler.swap("regular_iterate");
            this.profiler = profiler;
            for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
            {
                BlockPos pos = chunkRenderer.getOrigin();
                ChunkSchematic chunk = this.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                List<Entity> list = chunk.getEntityList();

//                Litematica.LOGGER.error("[WorldRenderer] Chunk: [{}], EntityList [{}]", pos.toShortString(), list.size());

                for (Entity entityTmp : list)
                {
//                    Litematica.LOGGER.error("[WorldRenderer] Chunk: [{}], Entity [{}/{}], CHK-Pos: [X: {} (vs {}), Y: {} (vs {}), Z: {} (vs {})]",
//                                            pos.toShortString(),
//                                            entityTmp.getName().getString(), entityTmp.getUuidAsString(),
//                                            entityTmp.getX(), (int) entityTmp.getX(),
//                                            entityTmp.getY(), (int) entityTmp.getY(),
//                                            entityTmp.getZ(), (int) entityTmp.getZ()
//                    );

                    if (!layerRange.isPositionWithinRange((int) entityTmp.getX(), (int) entityTmp.getY(), (int) entityTmp.getZ()))
                    {
                        continue;
                    }

                    boolean shouldRender = this.entityRenderDispatcher.shouldRender(entityTmp, frustum, cameraX, cameraY, cameraZ);

                    if (shouldRender)
                    {
                        double lerpX = MathHelper.lerp(partialTicks, entityTmp.lastRenderX, entityTmp.getX());
                        double lerpY = MathHelper.lerp(partialTicks, entityTmp.lastRenderY, entityTmp.getY());
                        double lerpZ = MathHelper.lerp(partialTicks, entityTmp.lastRenderZ, entityTmp.getZ());

                        double x = lerpX - cameraX;
                        double y = lerpY - cameraY;
                        double z = lerpZ - cameraZ;

//                        Litematica.LOGGER.warn("[WorldRenderer] Chunk: [{}], EntityPos [{}] // Adj. Pos: X [{}], Y [{}], Z [{}]", pos.toShortString(), entityTmp.getBlockPos().toShortString(), x, y, z);
                        matrices.push();

                        // Check for Salmon / Cod 'inWater' fix
                        // Because the entities might be following the ClientWorld State
                        if (entityTmp instanceof SalmonEntity || entityTmp instanceof CodEntity ||
                            entityTmp instanceof TadpoleEntity || entityTmp instanceof AbstractHorseEntity ||
                            entityTmp instanceof TropicalFishEntity || entityTmp instanceof WaterAnimalEntity)
                        {
                            BlockState state = this.world.getBlockState(entityTmp.getBlockPos());
                            Fluid fluid = state.getFluidState() != null ? state.getFluidState().getFluid() : Fluids.EMPTY;

                            if ((fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) &&
                                !((IMixinEntity) entityTmp).litematica_isTouchingWater())
                            {
                                ((IEntityInvoker) entityTmp).litematica$toggleTouchingWater(true);
                            }
                        }

                        this.entityRenderDispatcher.render(entityTmp, x, y, z, partialTicks, matrices, immediate, this.entityRenderDispatcher.getLight(entityTmp, partialTicks));
                        ++this.countEntitiesRendered;
                        matrices.pop();
                    }
//                    else
//                    {
//                        Litematica.LOGGER.warn("Skipping Entity at pos X: [{}], Y: [{}], Z: [{}] (Should Render = False)", entityTmp.getX(), entityTmp.getY(), entityTmp.getZ());
//                    }
                }
            }
        }
    }

    public void renderBlockEntities(Camera camera, Frustum frustum, MatrixStack matrices, VertexConsumerProvider.Immediate immediate, VertexConsumerProvider.Immediate immediate2, float partialTicks, Profiler profiler)
    {
        this.profiler = profiler;

        profiler.push("block_entities_prepare");

        double cameraX = camera.getPos().x;
        double cameraY = camera.getPos().y;
        double cameraZ = camera.getPos().z;

        this.blockEntityRenderDispatcher.configure(this.world, camera, this.mc.crosshairTarget);

//        MatrixStack matrixStack = new MatrixStack();
//        matrixStack.push();
//        matrixStack.multiplyPositionMatrix(posMatrix);
//        matrixStack.pop();

//        VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
        LayerRange layerRange = DataManager.getRenderLayerRange();

        profiler.swap("block_entities");
        this.profiler = profiler;

        profiler.swap("render_be");
        for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
        {
            ChunkRenderDataSchematic data = chunkRenderer.getChunkRenderData();
            List<BlockEntity> tiles = data.getBlockEntities();

            if (!tiles.isEmpty())
            {
                BlockPos chunkOrigin = chunkRenderer.getOrigin();
                ChunkSchematic chunk = this.world.getChunkProvider().getChunk(chunkOrigin.getX() >> 4, chunkOrigin.getZ() >> 4);

                if (chunk != null && data.getTimeBuilt() >= chunk.getTimeCreated())
                {
                    for (BlockEntity te : tiles)
                    {
                        BlockPos pos = te.getPos();

                        if (!layerRange.isPositionWithinRange(pos.getX(), pos.getY(), pos.getZ()))
                        {
                            continue;
                        }

                        try
                        {
                            matrices.push();
                            matrices.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
                            this.blockEntityRenderDispatcher.render(te, partialTicks, matrices, immediate2);
                            matrices.pop();
                        }
                        catch (Exception err)
                        {
                            Litematica.LOGGER.error("[Pass 1] Error rendering blockEntities; Exception: {}", err.getLocalizedMessage());
                        }
                    }
                }
            }
        }

        immediate2.drawCurrentLayer();

        profiler.swap("render_be_no_cull");
        synchronized (this.blockEntities)
        {
            for (BlockEntity te : this.blockEntities)
            {
                BlockPos pos = te.getPos();

                if (!layerRange.isPositionWithinRange(pos.getX(), pos.getY(), pos.getZ()))
                {
                    continue;
                }

                try
                {
                    matrices.push();
                    matrices.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
                    this.blockEntityRenderDispatcher.render(te, partialTicks, matrices, immediate);
                    matrices.pop();
                }
                catch (Exception err)
                {
                    Litematica.LOGGER.error("[Pass 2] Error rendering blockEntities; Exception: {}", err.getLocalizedMessage());
                }
            }
        }

        immediate.drawCurrentLayer();
        profiler.pop();
    }

    /*
    private boolean isOutlineActive(Entity entityIn, Entity viewer, Camera camera)
    {
        boolean sleeping = viewer instanceof LivingEntity && ((LivingEntity) viewer).isSleeping();

        if (entityIn == viewer && this.mc.options.perspective == 0 && sleeping == false)
        {
            return false;
        }
        else if (entityIn.isGlowing())
        {
            return true;
        }
        else if (this.mc.player.isSpectator() && this.mc.options.keySpectatorOutlines.isPressed() && entityIn instanceof PlayerEntity)
        {
            return entityIn.ignoreFrustumCheck || camera.isBoundingBoxInFrustum(entityIn.getBoundingBox()) || entityIn.isRidingOrBeingRiddenBy(this.mc.player);
        }
        else
        {
            return false;
        }
    }
    */

    public void updateBlockEntities(Collection<BlockEntity> toRemove, Collection<BlockEntity> toAdd)
    {
//        int last = this.blockEntities.size();

        synchronized (this.blockEntities)
        {
            this.blockEntities.removeAll(toRemove);
            this.blockEntities.addAll(toAdd);
        }
    }

    public void scheduleChunkRenders(int chunkX, int chunkZ)
    {
        this.getProfiler().push("schedule_render");
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            this.chunkRendererDispatcher.scheduleChunkRender(chunkX, chunkZ);
        }
        this.getProfiler().pop();
    }
}
