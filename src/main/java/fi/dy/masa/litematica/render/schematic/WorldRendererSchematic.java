package fi.dy.masa.litematica.render.schematic;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.render.RenderContext;
import org.apache.logging.log4j.Logger;
import org.joml.*;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.render.uniform.ChunkFixUniform;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.entity.IMixinEntity;
import fi.dy.masa.litematica.render.IWorldSchematicRenderer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.*;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.ChunkSchematicState;
import fi.dy.masa.litematica.world.WorldSchematic;

public class WorldRendererSchematic implements IWorldSchematicRenderer
{
    private static final String STATUS_OVERLAY_TIMING_PROPERTY = "litematica.debug.schematicOverlayTiming";
    private static final String STATUS_OVERLAY_FORCE_FALLBACK_PROPERTY = "litematica.debug.schematicOverlay.forceFallback";
    private static final long STATUS_OVERLAY_TIMING_LOG_INTERVAL_MS = 5000L;
    private static final Logger LOGGER = Litematica.LOGGER;
    private final Minecraft mc;
    private final List<ChunkRendererSchematicVbo> renderInfos;
//    private final Set<BlockEntity> blockEntities;
    private SchematicRenderState schematicRenderState;
    private Set<ChunkRendererSchematicVbo> chunksToUpdate;
    private WorldSchematic world;
    private ChunkRenderDispatcherSchematic chunkRendererDispatcher;
    private GpuBufferSlice vanillaFogBuffer;
    private GpuSampler gpuSampler;
    private ProfilerFiller profiler;
    private double lastCameraChunkUpdateX;
    private double lastCameraChunkUpdateY;
    private double lastCameraChunkUpdateZ;
    private double lastCameraX;
    private double lastCameraY;
    private double lastCameraZ;
    private float lastCameraPitch;
    private float lastCameraYaw;
    private ChunkRenderDispatcherLitematica renderDispatcher;
    private final IChunkRendererFactory renderChunkFactory;
    private final IdentityHashMap<ChunkRendererSchematicVbo, EnumMap<OverlayRenderType, CachedStatusOverlayMesh>> statusOverlayCache;
    //private ShaderGroup entityOutlineShader;
    //private boolean entityOutlinesRendered;

    private final HashMap<Vec3, UUID> renderedEntities;
    private int renderDistanceChunks;
    private int renderEntitiesStartupCounter;
    private int countEntitiesTotal;
    private int countEntitiesRendered;
    private int countEntitiesHidden;
    private long statusOverlayTimingLastLogTime;
    private long statusOverlayTimingFallbackNanos;
    private long statusOverlayTimingFallbackDraws;
    private long statusOverlayTimingCachedNanos;
    private long statusOverlayTimingCachedBuffers;
    private long statusOverlayTimingCachedDraws;
    private long statusOverlayTimingCacheBuildNanos;
    private long statusOverlayTimingCacheBuilds;
    private long statusOverlayTimingCacheBuildVertices;
    private long statusOverlayTimingChunks;
    private long statusOverlayTimingVerticesCopied;
    private boolean statusOverlayCacheWarningLogged;

    private double lastTranslucentSortX;
    private double lastTranslucentSortY;
    private double lastTranslucentSortZ;
    private boolean needsUpdate;
    private boolean shouldDraw;

    private static final String RENDER_MODE_INVALIDATION_PROPERTY = "litematica.debug.renderModeInvalidation";

    private int lastLayerRenderSignature = Integer.MIN_VALUE;

    public WorldRendererSchematic(Minecraft mc)
    {
        this.mc = mc;
        this.renderChunkFactory = ChunkRendererSchematicVbo::new;
//	    this.blockEntities = new HashSet<>();
	    this.renderInfos = new ArrayList<>(1024);
        this.statusOverlayCache = new IdentityHashMap<>();
        this.renderedEntities = new HashMap<>();
		this.schematicRenderState = this.getSchematicRenderState();
	    this.chunksToUpdate = new LinkedHashSet<>();
        this.profiler = null;
        this.vanillaFogBuffer = null;
        this.gpuSampler = null;
        this.shouldDraw = false;
	    this.lastCameraChunkUpdateX = Double.MIN_VALUE;
	    this.lastCameraChunkUpdateY = Double.MIN_VALUE;
	    this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
	    this.lastCameraX = Double.MIN_VALUE;
	    this.lastCameraY = Double.MIN_VALUE;
	    this.lastCameraZ = Double.MIN_VALUE;
	    this.lastCameraPitch = Float.MIN_VALUE;
	    this.lastCameraYaw = Float.MIN_VALUE;
	    this.renderDistanceChunks = -1;
	    this.renderEntitiesStartupCounter = 2;
	    this.needsUpdate = true;
    }

    @Override
    public void markNeedsUpdate()
    {
        this.needsUpdate = true;
    }

    @Override
    public boolean hasWorld()
    {
        return this.world != null;
    }

    @Override
    public String getDebugInfoRenders()
    {
        int rcTotal = this.chunkRendererDispatcher != null ? this.chunkRendererDispatcher.getRendererCount() : 0;
        int rcRendered = this.chunkRendererDispatcher != null ? this.getRenderedChunks() : 0;
        return String.format("C: %02d/%02d %sD: %02d, L: %02d, %s", rcRendered, rcTotal, this.mc.smartCull ? "(s) " : "", this.renderDistanceChunks, 0, this.renderDispatcher == null ? "null" : this.renderDispatcher.getDebugInfo());
    }

    @Override
    public String getDebugInfoEntities()
    {
		return String.format("E: %02d/%02d, B: %02d", this.countEntitiesRendered, this.countEntitiesTotal, this.countEntitiesHidden);
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
            final ChunkRenderDataSchematic data = chunkRenderer.chunkRenderData;

            if (!data.isEmpty())
            {
                ++count;
            }
        }

        return count;
    }

    @Override
    public ProfilerFiller getProfiler()
    {
        if (this.profiler == null)
        {
            this.profiler = Profiler.get();
            this.profiler.startTick();
        }

        return this.profiler;
    }

    @Override
    public BlockModelRendererSchematic getBlockRenderer()
    {
        return BlockModelCacheSchematic.INSTANCE.blockModelRenderer();
    }

    @Override
    public BlockEntityRenderDispatcher getBlockEntityRenderer()
    {
        return BlockModelCacheSchematic.INSTANCE.blockEntityRenderer();
    }

    @Override
    public FluidRenderer getFluidRenderer()
    {
        return BlockModelCacheSchematic.INSTANCE.fluidRenderer();
    }

    @Override
    public EntityRenderDispatcher getEntityRenderer()
    {
        return BlockModelCacheSchematic.INSTANCE.entityRenderer();
    }

    @Override
    public FogRenderer getFogRenderer()
    {
        return BlockModelCacheSchematic.INSTANCE.fogRenderer();
    }

    @Override
    public SchematicRenderState getSchematicRenderState()
    {
        if (this.schematicRenderState == null)
        {
            this.schematicRenderState = new SchematicRenderState();
        }

        return this.schematicRenderState;
    }

    @Override
	public <T extends Comparable<T>> BlockState getFallbackState(BlockState origState)
	{
		Collection<Property<?>> props = origState.getProperties();
		Block block = origState.getBlock();

		if (FallbackBlocks.BLOCK_TO_ID.containsKey(block))
		{
			Identifier id = FallbackBlocks.BLOCK_TO_ID.get(block);

			LOGGER.warn("getFallbackState: Invalid Block State/Block Model for block [{}]; but we found a matching Litematica fallback block state that you can use.  Perhaps you have the Fusion mod installed?", origState.getBlock().getName().getString());
			BlockState newState = FallbackBlocks.ID_TO_STATE_MANAGER.get(id).any();

			for (Property<?> entry : props)
			{
				@SuppressWarnings("unchecked")
				Property<T> p = (Property<T>) entry;

				if (newState.hasProperty(p))
				{
					T value = origState.getValue(p);

					if (!newState.getValue(p).equals(value))
					{
						newState = newState.setValue(p, value);
					}
				}
			}

			Litematica.debugLog("Fallback Block State -- OLD: [{}] --> NEW: [{}]", origState.toString(), newState.toString());
			return newState;
		}

		return origState;
	}

    protected GpuBufferSlice getEmptyFogBuffer()
    {
        return this.getFogRenderer().getBuffer(FogRenderer.FogMode.NONE);
    }

    private static boolean isEntityType(Entity entity, String path)
    {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && id.equals(Identifier.withDefaultNamespace(path));
    }

    @Override
    public void setWorldAndLoadRenderers(@Nullable WorldSchematic worldSchematic)
    {
//        LOGGER.error("[WorldRenderer] setWorldAndLoadRenderers()");
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
            this.closeStatusOverlayCache();
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

			this.clearWorldRenderStates();
            this.getSchematicRenderState().clearChunkFixUniform();

            if (this.vanillaFogBuffer != null)
            {
                this.vanillaFogBuffer = null;
            }

            this.closeGpuSampler();

//            synchronized (this.blockEntities)
//            {
//                this.blockEntities.clear();
//            }
        }
    }

    @Override
    public void loadRenderers(@Nullable ProfilerFiller profiler)
    {
        if (this.hasWorld())
        {
//            LOGGER.warn("[WorldRenderer] loadRenderers()");
            if (profiler == null)
            {
                profiler = Profiler.get();
            }

            this.profiler = profiler;

            profiler.push("load_renderers");
            this.closeStatusOverlayCache();

            if (this.renderDispatcher == null)
            {
                this.renderDispatcher = new ChunkRenderDispatcherLitematica(profiler);
            }

            this.needsUpdate = true;
            this.renderDistanceChunks = this.mc.options.renderDistance().get() + 2;

            if (this.chunkRendererDispatcher != null)
            {
                this.chunkRendererDispatcher.delete();
            }

            this.stopChunkUpdates(profiler);
			this.clearWorldRenderStates();

//            synchronized (this.blockEntities)
//            {
//                this.blockEntities.clear();
//            }

            BlockModelCacheSchematic.INSTANCE.onLoadRenderers();

            this.chunkRendererDispatcher = new ChunkRenderDispatcherSchematic(this.world, this.renderDistanceChunks, this, this.renderChunkFactory);
            this.renderEntitiesStartupCounter = 2;

            profiler.pop();
        }
    }

    protected void stopChunkUpdates(ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] stopChunkUpdates()");
        this.closeStatusOverlayCache();

        if (!this.chunksToUpdate.isEmpty())
        {
            this.chunksToUpdate.forEach(ChunkRendererSchematicVbo::deleteGlResources);
        }

        this.chunksToUpdate.clear();
        this.renderDispatcher.stopChunkUpdates(profiler);
        this.profiler = null;
		this.clearWorldRenderStates();
        this.vanillaFogBuffer = null;
    }

    @Override
    public void setupTerrain(Camera camera, Frustum frustum, int frameCount, boolean playerSpectator, ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] setupTerrain()");
        this.profiler = profiler;
        profiler.push("setup_terrain");

        if (this.chunkRendererDispatcher == null ||
            this.mc.options.renderDistance().get() + 2 != this.renderDistanceChunks)
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

        profiler.popPush("setup_camera");

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

        Vec3 cameraPos = camera.position();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        this.renderDispatcher.setCameraPosition(cameraPos);
        this.checkLayerRenderStateChanged();

        profiler.popPush("culling");
        BlockPos viewPos = BlockPos.containing(cameraX, cameraY + (double) entity.getEyeHeight(), cameraZ);
        final int centerChunkX = (viewPos.getX() >> 4);
        final int centerChunkZ = (viewPos.getZ() >> 4);
        final int renderDistance = this.mc.options.renderDistance().get() + 2;
        ChunkPos viewChunk = ChunkPos.containing(viewPos);

        this.needsUpdate = this.needsUpdate || !this.chunksToUpdate.isEmpty() ||
                entityX != this.lastCameraX ||
                entityY != this.lastCameraY ||
                entityZ != this.lastCameraZ ||
                entity.getXRot() != this.lastCameraPitch ||
                entity.getYRot() != this.lastCameraYaw;
        this.lastCameraX = cameraX;
        this.lastCameraY = cameraY;
        this.lastCameraZ = cameraZ;
        this.lastCameraPitch = camera.xRot();
        this.lastCameraYaw = camera.yRot();

        profiler.popPush("update");
//        List<ChunkPos> updatePositions = new ArrayList<>();

        if (this.needsUpdate)
        {
            //profiler.push("fetch");

            this.needsUpdate = false;
            this.renderInfos.clear();

            profiler.push("update_sort");
            List<ChunkPos> positions = DataManager.getSchematicPlacementManager().getAndUpdateVisibleChunks(viewChunk);
            int count = 0;
            //positions.sort(new SubChunkPos.DistanceComparator(viewSubChunk));

            //Queue<SubChunkPos> queuePositions = new PriorityQueue<>(new SubChunkPos.DistanceComparator(viewSubChunk));
            //queuePositions.addAll(set);

            //if (GuiBase.isCtrlDown()) System.out.printf("sorted positions: %d\n", positions.size());
//            Litematica.LOGGER.warn("setupTerrain(): positions: {}", positions.size());

            profiler.popPush("update_iteration");

            //while (queuePositions.isEmpty() == false)
            for (ChunkPos chunkPos : positions)
            {
                //SubChunkPos subChunk = queuePositions.poll();
                int cx = chunkPos.x();
                int cz = chunkPos.z();
//                LOGGER.warn("[WorldRenderer] setupTerrain() position[{}], chunkPos: {} // isLoaded: [{}]", count, chunkPos.toString(), this.world.getChunkSource().hasChunk(chunkPos.x(), chunkPos.z()));
                // Only render sub-chunks that are within the client's render distance, and that
                // have been already properly loaded on the client
                if (Math.abs(cx - centerChunkX) <= renderDistance &&
                    Math.abs(cz - centerChunkZ) <= renderDistance &&
                    this.world.getChunkSource().hasChunk(cx, cz))
                {
                    ChunkRendererSchematicVbo chunkRenderer = this.chunkRendererDispatcher.getChunkRenderer(cx, cz);

                    if (chunkRenderer != null && frustum.isVisible(chunkRenderer.getBoundingBox()))
                    {
                        //if (GuiBase.isCtrlDown()) System.out.printf("add @ %s\n", subChunk);
                        if (chunkRenderer.needsUpdate() && chunkPos.equals(viewChunk))
                        {
                            chunkRenderer.setNeedsUpdate(true);
                        }
//                        else if (chunkPos.distanceSquared(viewChunk) <= (renderDistance / 5))
//                        {
//                            // Mark anything within 1/5 of your render distance as needing an update, but not immediately
//                            chunkRenderer.setNeedsUpdate(false);
//                        }

                        this.renderInfos.add(chunkRenderer);
                    }
                }

//                updatePositions.add(chunkPos);
                count++;
            }

            profiler.pop(); // fetch (update_sort)
        }

        profiler.popPush("rebuild_near");
        Set<ChunkRendererSchematicVbo> set = this.chunksToUpdate;
        this.chunksToUpdate = new LinkedHashSet<>();

        for (ChunkRendererSchematicVbo chunkRendererTmp : this.renderInfos)
        {
            if (chunkRendererTmp.needsUpdate() || set.contains(chunkRendererTmp))
            {
                set.remove(chunkRendererTmp);
                this.needsUpdate = true;
                BlockPos pos = chunkRendererTmp.getOrigin().offset(8, 8, 8);
                boolean isNear = pos.distSqr(viewPos) < 1024.0D;

                if (!chunkRendererTmp.needsImmediateUpdate() && !isNear)
                {
//                    LOGGER.warn("[WorldRenderer] setupTerrain --> Update Later @ cp: {}", chunkRendererTmp.getChunkPos().toString());
                    this.chunksToUpdate.add(chunkRendererTmp);
                }
                else
                {
                    //if (GuiBase.isCtrlDown()) System.out.printf("====== update now\n");
//                    LOGGER.warn("[WorldRenderer] setupTerrain --> Update Now @ cp: {}", chunkRendererTmp.getChunkPos().toString());
                    profiler.push("update_now");
                    this.profiler = profiler;

                    this.renderDispatcher.updateChunkNow(chunkRendererTmp, profiler);
                    chunkRendererTmp.clearNeedsUpdate();

                    profiler.pop();
                }
            }
        }

        // Preserve pending chunks that were not in the current visible set.
        this.chunksToUpdate.addAll(set);

		this.clearWorldRenderStates();

        //profiler.pop();
        profiler.pop();     // setup_terrain
    }

    @Override
    public void updateChunks(long finishTimeNano, ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] updateChunks()");
        this.profiler = profiler;
        profiler.push("run_chunk_uploads");
        this.needsUpdate |= this.renderDispatcher.runChunkUploads(finishTimeNano, profiler);

        if (this.profiler == null)
        {
            this.profiler = profiler;
        }

        profiler.popPush("check_updates");

        if (!this.chunksToUpdate.isEmpty())
        {
            Iterator<ChunkRendererSchematicVbo> iterator = this.chunksToUpdate.iterator();
            int index = 0;

            while (iterator.hasNext())
            {
                ChunkRendererSchematicVbo renderChunk = iterator.next();
                boolean flag;
                boolean immediate = renderChunk.needsImmediateUpdate();

                if (immediate)
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

    @Override
    public void capturePreMainValues(CameraRenderState camera, GpuBufferSlice fogBuffer, ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] capturePreMainValues()");
        this.vanillaFogBuffer = fogBuffer;
        this.profiler = profiler;
    }

    @Override
    public void uploadRemainingBuffers(long finishTimeNano, DeltaTracker deltaTracker,
                                       double cameraX, double cameraY, double cameraZ,
                                       ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] uploadRemainingBuffers()");
        this.profiler = profiler;
        if (RenderSystem.isOnRenderThread())
        {
            profiler.push("upload_remaining_buffers");
            this.needsUpdate |= this.renderDispatcher.runChunkUploads(finishTimeNano, profiler);
            profiler.pop();
        }
    }

    @Override
    public int prepareBlockLayers(Matrix4fc matrix4fc,
                                   double cameraX, double cameraY, double cameraZ,
                                   ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] prepareBlockLayers()");
        this.profiler = profiler;
//        RenderSystem.assertOnRenderThread();
        profiler.push("layer_multi_phase");

	    List<DynamicUniforms.Transform> transformValues = new ArrayList<>();
//        EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> renderMap = new EnumMap<>(ChunkSectionLayer.class);
        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> renderMap = new EnumMap<>(ChunkSectionLayer.class);

        for (ChunkSectionLayer layer : ChunkSectionLayer.values())
        {
            renderMap.put(layer, new ArrayList<>());
        }

        profiler.popPush("layer_setup");

        int startIndex = 0;
        int stopIndex = this.renderInfos.size();
        int increment = 1;
        int indexCount = 0;
        int count = 0;
        boolean renderAsTranslucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
        boolean renderCollidingBlocks = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();
        @SuppressWarnings("deprecation")
	    GpuTextureView blockAtlas = this.mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
		int atlasWidth = blockAtlas.getWidth(0);        // todo 4096
	    int atlasHeight = blockAtlas.getHeight(0);      // todo 2048
        Vector4f colorMod = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
	    Matrix4f texMatrix = new Matrix4f();

//        if (IrisCompat.isShaderActive())
//        {
//            Litematica.LOGGER.error("[WorldRenderer] prepareBlockLayers() -- atlasWidth: [{}], atlasHeight: [{}]", atlasWidth, atlasHeight);
//        }

        if (renderAsTranslucent)
        {
            colorMod = new Vector4f(1.0F, 1.0F, 1.0F, (float) Configs.Visuals.GHOST_BLOCK_ALPHA.getDoubleValue());
        }

        boolean startedDrawing = false;

        profiler.popPush("layer_iteration");
        this.profiler = profiler;

        for (int i = startIndex; i != stopIndex; i += increment)
        {
            final ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);
            final ChunkRenderDataSchematic data = renderer.getChunkRenderData();
            final ChunkMeshDataSchematic chunkMeshData = data.getMeshDataCache();
            final BlockPos chunkOrigin = renderer.getOrigin();
//            long now = System.currentTimeMillis();
//            int uboIndex = -1;

            for (ChunkSectionLayer layer : ChunkSectionLayer.values())
            {
                profiler.popPush("layer_"+ layer.label());

                if (!data.isBlockLayerEmpty(layer))
                {
                    // New
//                    ChunkMeshDataSchematic.DrawState drawState = chunkMeshData.getDrawState(layer);
//                    ChunkRenderBufferSlice slice = renderer.getUberSlice(chunkMeshData, layer);

                    // Old
                    ChunkRenderBuffers buffers = renderer.getBuffersOrNull(layer);

                    if (buffers == null || buffers.isClosed() || !chunkMeshData.hasMeshData(layer))
                    {
//                        LOGGER.error("Layer [{}], ChunkOrigin [{}], NO BUFFERS!", layer.name(), chunkOrigin.toShortString());
                        continue;
                    }

                    GpuBuffer indexBuffer;
                    VertexFormat.IndexType indexType;

                    if (buffers.getIndexBuffer() == null)
                    {
                        if (buffers.getIndexCount() > indexCount)
                        {
                            indexCount = buffers.getIndexCount();
                        }

                        indexBuffer = null;
                        indexType = null;
                    }
                    else
                    {
                        indexBuffer = buffers.getIndexBuffer();
                        indexType = buffers.getIndexType();
                    }

                    // New
//                    if (slice != null && drawState != null &&
//                        (!drawState.hasIndexBuffer() || slice.indexBuffer() != null))
//                    {
//                        if (uboIndex == -1)
//                        {
//                            uboIndex = transformValues.size();
//                            transformValues.add(new DynamicUniforms.Transform(
//                                    matrix4fc,
//                                    colorMod,
//                                    new Vector3f((float) (chunkOrigin.getX() - cameraX), (float) (chunkOrigin.getY() - cameraY), (float) (chunkOrigin.getZ() - cameraZ)),
//                                    texMatrix
//                            ));
//                        }
//                    }
//
//                    if (slice == null || drawState == null)
//                    {
//                        continue;
//                    }
//
//                    // Old
                    int pos = transformValues.size();

//                    int hash = 173;
                    VertexFormat vf = layer.pipeline().getVertexFormat();
//                    GpuBuffer vbo = slice.vertexBuffer();
//
//                    if (layer != ChunkSectionLayer.TRANSLUCENT)
//                    {
//                        hash = 31 * hash + vbo.hashCode();
//                    }
//
//                    int index = 0;
//                    GpuBuffer ibo;
//                    VertexFormat.IndexType indexType;
//
//                    if (!drawState.hasIndexBuffer())
//                    {
//                        if (drawState.indexCount() > indexCount)
//                        {
//                            indexCount = drawState.indexCount();
//                        }
//
//                        ibo = null;
//                        indexType = null;
//                    }
//                    else
//                    {
//                        ibo = slice.indexBuffer();
//                        indexType = drawState.indexType();
//
//                        if (layer != ChunkSectionLayer.TRANSLUCENT)
//                        {
//                            hash = 31 * hash + ibo.hashCode();
//                            hash = 31 * hash + indexType.hashCode();
//                        }
//
//                        index = (int) (slice.indexBufferOffset() / indexType.bytes);
//                    }
//
//                    int finalIdx = uboIndex;
//                    int vertex = (int) (slice.vertexBufferOffset() / vf.getVertexSize());
//
                    transformValues.add(new DynamicUniforms.Transform(
                            matrix4fc,
                            colorMod,
                            new Vector3f((float) (chunkOrigin.getX() - cameraX), (float) (chunkOrigin.getY() - cameraY), (float) (chunkOrigin.getZ() - cameraZ)),
                            texMatrix
                    ));

                    // New
//                    List<RenderPass.Draw<GpuBufferSlice[]>> drawSlices = renderMap.get(layer)
//                            .computeIfAbsent(hash,
//                                             (Int2ObjectFunction<? extends List<RenderPass.Draw<GpuBufferSlice[]>>>)(var0 -> new ArrayList<>())
//                            );

                    renderMap.get(layer).add(
                            new RenderPass.Draw<>(
                                     0,
    // OLD
                                     buffers.getVertexBuffer(),
                                     indexBuffer, indexType,
                                     0,
                                     buffers.getIndexCount(),
                                     0,
                                     (slices, uploader) ->
                                             uploader.upload("DynamicTransforms", ((GpuBufferSlice[]) slices)[pos])
//                                     vbo, ibo,
//                                     indexType, index,
//                                     drawState.indexCount(), vertex,
//                                     (ubos, uploader) -> uploader.upload("DynamicTransforms", ubos[finalIdx])
                             ));

                    startedDrawing = true;
                    ++count;
                }
            }
        }

        if (startedDrawing)
        {
            profiler.popPush("fill_uniforms");
            this.getSchematicRenderState().chunkFixUniform.fillBuffer(atlasWidth, atlasHeight, 1.0f);
            GpuBufferSlice[] transformSlices = RenderSystem.getDynamicUniforms()
                                                           .writeTransforms(
                                                                   transformValues.toArray(new DynamicUniforms.Transform[0])
                                                           );

            profiler.popPush("fill_batch_draw");
            this.getSchematicRenderState().batchDraw = new ChunkRenderBatchDraw(blockAtlas, renderMap,
                                                      renderCollidingBlocks, renderAsTranslucent, indexCount,
                                                      transformSlices,
                                                      this.getSchematicRenderState().chunkFixUniform.getCurrentBuffer()
            );
            this.shouldDraw = true;
        }

        profiler.pop();     // layer+ X

        return count;
    }

    @Override
    public void drawBlockLayerGroup(ChunkSectionLayerGroup group, @Nullable GpuSampler sampler)
    {

//        LOGGER.warn("[WorldRenderer] drawBlockLayerGroup() [{}]", group.label());
        if (this.getSchematicRenderState().hasBatchDraw() && this.shouldDraw)
        {
            this.profiler.push(Reference.MOD_ID + "_batch_draw_" + group.label());

            // Disable fog in the Schematic World
            RenderSystem.setShaderFog(this.getEmptyFogBuffer());

            if (sampler != null && this.gpuSampler == null)
            {
                this.setGpuSampler(sampler);
            }

            if (sampler == null)
            {
                sampler = this.getGpuSampler();
            }

            this.getSchematicRenderState().getBatchDraw().draw(group, sampler, this.profiler);
            RenderSystem.setShaderFog(this.vanillaFogBuffer);

            this.profiler.pop();
        }
    }

//    private void dumpSampler(@Nullable GpuSampler sampler)
//    {
//        System.out.print ("GpuSampler Dump -->\n");
//        if (sampler == null)
//        {
//            System.out.print ("  NULL!!!\n");
//            System.out.print ("GpuSampler END\n");
//            return;
//        }
//
//        System.out.printf("  AddressModeU: [%s]\n", sampler.getAddressModeU().name());
//        System.out.printf("  AddressModeV: [%s]\n", sampler.getAddressModeV().name());
//        System.out.printf("  FilterMode-Min: [%s]\n", sampler.getMinFilter().name());
//        System.out.printf("  FilterMode-Mag: [%s]\n", sampler.getMagFilter().name());
//        System.out.printf("  MaxAnisotropy: [%d]\n", sampler.getMaxAnisotropy());
//        System.out.printf("  MaxLod: [%f]\n", sampler.getMaxLod().orElse(-1.0F));
//        System.out.print ("GpuSampler END\n");
//    }

    @Override
    public ChunkFixUniform getChunkFixUniform()
    {
        return this.getSchematicRenderState().chunkFixUniform;
    }

    @Override
    public void clearChunkFixUniform()
    {
        this.getSchematicRenderState().clearChunkFixUniform();
    }

    @Override
	public void clearWorldRenderStates()
	{
		this.getSchematicRenderState().clear();
	}

    @Override
    @Nullable
    public GpuSampler getGpuSampler()
    {
        if (this.gpuSampler == null && RenderSystem.isOnRenderThread())
        {
            this.gpuSampler = RenderSystem.getDevice()
                                          .createSampler(AddressMode.CLAMP_TO_EDGE,
                                                         AddressMode.CLAMP_TO_EDGE,
                                                         FilterMode.LINEAR,
                                                         FilterMode.LINEAR,
                                                         4, OptionalDouble.empty()
                                          );
        }

        return this.gpuSampler;
    }

    @Override
    public void setGpuSampler(@Nonnull GpuSampler gpuSampler)
    {
        this.closeGpuSampler();
        this.gpuSampler = gpuSampler;
    }

    @Override
    public void closeGpuSampler()
    {
        if (this.gpuSampler != null)
        {
            this.gpuSampler.close();
        }

        this.gpuSampler = null;
    }

    @Override
    public void renderEntityDebugHitboxes(IEntityHitboxDebugRendererInvoker invoker, double cameraX, double cameraY, double cameraZ, DebugValueAccess debugValueAccess, Frustum frustum, float ticks)
    {
        if (this.hasWorld())
        {
            for (Entity e : this.world.getEntities().getAll())
            {
                if (!e.isInvisible() &&
                    frustum.isVisible(e.getBoundingBox()) &&
                    (e != this.mc.getCameraEntity() || this.mc.options.getCameraType() != CameraType.FIRST_PERSON))
                {
                    float entityTicks = this.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
	                invoker.litematica$addEntityHitbox(e, entityTicks, false);

                    if (SharedConstants.DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES)
                    {
                        // Shrug; because the schem world is basically the "server-side" also.
                        invoker.litematica$addEntityHitbox(e, entityTicks, true);
                    }
                }
            }
        }
    }

    @Override
	public void updateCameraState(Camera camera, float tickProgress, CameraRenderState cameraState)
	{
        this.getSchematicRenderState().cameraState.initialized = cameraState.initialized;
        this.getSchematicRenderState().cameraState.blockPos = cameraState.blockPos;
        this.getSchematicRenderState().cameraState.pos = cameraState.pos;
        this.getSchematicRenderState().cameraState.xRot = cameraState.xRot;
        this.getSchematicRenderState().cameraState.yRot = cameraState.yRot;
        this.getSchematicRenderState().cameraState.orientation = cameraState.orientation;
        this.getSchematicRenderState().cameraState.isPanoramicMode = cameraState.isPanoramicMode;
        this.getSchematicRenderState().cameraState.cullFrustum = cameraState.cullFrustum;
        this.getSchematicRenderState().cameraState.fogType = cameraState.fogType;
        this.getSchematicRenderState().cameraState.fogData = cameraState.fogData;
        this.getSchematicRenderState().cameraState.hudFov = cameraState.hudFov;
        this.getSchematicRenderState().cameraState.depthFar = cameraState.depthFar;
        this.getSchematicRenderState().cameraState.projectionMatrix = cameraState.projectionMatrix;
        this.getSchematicRenderState().cameraState.viewRotationMatrix = cameraState.viewRotationMatrix;
        this.getSchematicRenderState().cameraState.entityRenderState = cameraState.entityRenderState;
	}

    @Override
    public void scheduleTranslucentSorting(Vec3 cameraPos, ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] scheduleTranslucentSorting()");
        double x = cameraPos.x();
        double y = cameraPos.y();
        double z = cameraPos.z();

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
                if ((chunkRenderer.getChunkRenderData().isBlockLayerStarted(ChunkSectionLayer.TRANSLUCENT) ||
                    (!chunkRenderer.getChunkRenderData().isEmpty() && chunkRenderer.hasOverlay())) && h++ < 15)
                {
                    this.renderDispatcher.updateTransparencyLater(chunkRenderer, profiler);
                }
            }
        }
    }

    @Override
    public void renderBlockOverlays(RenderTarget fb, Camera camera, ProfilerFiller profiler)
    {
        this.profiler = profiler;
        boolean timingEnabled = isStatusOverlayTimingEnabled();
        boolean forceFallback = isStatusOverlayForceFallbackEnabled();

        // Draw filled status faces first so the final colored outlines remain visible.
        this.renderBlockOverlay(fb, OverlayRenderType.QUAD, camera, profiler, timingEnabled, forceFallback);
        this.renderBlockOverlay(fb, OverlayRenderType.OUTLINE, camera, profiler, timingEnabled, forceFallback);
        this.logStatusOverlayTimingIfNeeded(timingEnabled);
    }

    protected void renderBlockOverlay(RenderTarget fb, OverlayRenderType type, Camera camera, ProfilerFiller profiler,
                                      boolean timingEnabled, boolean forceFallback)
    {
        profiler.push("overlay_" + type.name());
        this.profiler = profiler;
        Vec3 cameraPos = camera.position();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();
        RenderPipeline pipeline = renderThrough ? type.getRenderThrough() : type.getPipeline();

        float[] offset = new float[3];

        profiler.popPush("overlay_iterate");
        this.profiler = profiler;

        for (int i = this.renderInfos.size() - 1; i >= 0; --i)
        {
            ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);

            if (!renderer.getChunkRenderData().isEmpty() && renderer.hasOverlay())
            {
                final ChunkRenderDataSchematic compiledChunk = renderer.getChunkRenderData();
                final ChunkMeshDataSchematic chunkMeshData = compiledChunk.getMeshDataCache();

                if (!compiledChunk.isOverlayTypeEmpty(type))
                {
                    BlockPos chunkOrigin = renderer.getOrigin();

                    if (timingEnabled)
                    {
                        ++this.statusOverlayTimingChunks;
                    }

                    if (!chunkMeshData.hasMeshData(type))
                    {
                        continue;
                    }

                    offset[0] = (float) (chunkOrigin.getX() - x);
                    offset[1] = (float) (chunkOrigin.getY() - y);
                    offset[2] = (float) (chunkOrigin.getZ() - z);

                    if (forceFallback == false && this.drawCachedStatusOverlay(fb, renderer, type, chunkMeshData, pipeline, offset, timingEnabled))
                    {
                        continue;
                    }

                    this.drawOverlayMeshDataCameraRelative(fb, type, chunkMeshData, pipeline, offset, timingEnabled);

                }
            }
        }

        profiler.pop();
    }

    private void drawOverlayMeshDataCameraRelative(RenderTarget fb, OverlayRenderType type, ChunkMeshDataSchematic chunkMeshData,
                                                   RenderPipeline pipeline, float[] offset, boolean timingEnabled)
    {
        MeshData sourceMeshData = chunkMeshData.getMeshDataOrNull(type);

        if (sourceMeshData == null)
        {
            return;
        }

        long startTime = timingEnabled ? System.nanoTime() : 0L;
        int copiedVertices = 0;
        int draws = 0;

        try (RenderContext ctx = new RenderContext(() -> "litematica:schematic_status_overlay/" + type.name().toLowerCase(Locale.ROOT), pipeline))
        {
            BufferBuilder builder = ctx.getBuilder();
            copiedVertices = this.copyOverlayMeshDataWithOffset(type, sourceMeshData, offset, builder);

            try (MeshData meshData = builder.build())
            {
                if (meshData != null)
                {
                    ctx.draw(fb, meshData, false, false, false);
                    draws = 1;
                }
            }
        }
        catch (Exception err)
        {
            LOGGER.warn("Schematic status overlay draw failed for {}: {}", type.name(), err.getMessage());
        }
        finally
        {
            if (timingEnabled)
            {
                this.recordStatusOverlayTiming(System.nanoTime() - startTime, copiedVertices, draws);
            }
        }
    }

    private boolean drawCachedStatusOverlay(RenderTarget fb, ChunkRendererSchematicVbo renderer, OverlayRenderType type,
                                            ChunkMeshDataSchematic chunkMeshData, RenderPipeline pipeline,
                                            float[] offset, boolean timingEnabled)
    {
        long startTime = timingEnabled ? System.nanoTime() : 0L;

        try
        {
            CachedStatusOverlayMesh cached = this.getOrCreateCachedStatusOverlay(renderer, type, chunkMeshData, pipeline, timingEnabled);

            if (cached == null || cached.isValid() == false)
            {
                return false;
            }

            cached.draw(fb, offset);

            if (timingEnabled)
            {
                this.statusOverlayTimingCachedNanos += System.nanoTime() - startTime;
                ++this.statusOverlayTimingCachedBuffers;
                ++this.statusOverlayTimingCachedDraws;
            }

            return true;
        }
        catch (Exception err)
        {
            if (this.statusOverlayCacheWarningLogged == false)
            {
                this.statusOverlayCacheWarningLogged = true;
                LOGGER.warn("Cached schematic status overlay draw failed; falling back to per-frame copy path: {}", err.getMessage());
            }

            return false;
        }
    }

    @Nullable
    private CachedStatusOverlayMesh getOrCreateCachedStatusOverlay(ChunkRendererSchematicVbo renderer, OverlayRenderType type,
                                                                   ChunkMeshDataSchematic chunkMeshData, RenderPipeline pipeline,
                                                                   boolean timingEnabled)
    {
        MeshData sourceMeshData = chunkMeshData.getMeshDataOrNull(type);

        if (sourceMeshData == null || sourceMeshData.drawState().vertexCount() <= 0)
        {
            this.removeCachedStatusOverlay(renderer, type);
            return null;
        }

        BlockPos chunkOrigin = renderer.getOrigin();
        int configSignature = this.getStatusOverlayConfigSignature();
        EnumMap<OverlayRenderType, CachedStatusOverlayMesh> rendererCache =
                this.statusOverlayCache.computeIfAbsent(renderer, ignored -> new EnumMap<>(OverlayRenderType.class));
        CachedStatusOverlayMesh cached = rendererCache.get(type);

        if (cached != null && cached.matches(sourceMeshData, pipeline, chunkOrigin, configSignature))
        {
            return cached;
        }

        if (cached != null)
        {
            cached.closeQuietly();
            rendererCache.remove(type);
        }

        long startTime = timingEnabled ? System.nanoTime() : 0L;
        CachedStatusOverlayMesh newCache = this.buildCachedStatusOverlay(type, sourceMeshData, pipeline, chunkOrigin, configSignature);

        if (newCache != null)
        {
            rendererCache.put(type, newCache);

            if (timingEnabled)
            {
                this.statusOverlayTimingCacheBuildNanos += System.nanoTime() - startTime;
                ++this.statusOverlayTimingCacheBuilds;
                this.statusOverlayTimingCacheBuildVertices += newCache.vertexCount();
            }
        }

        return newCache;
    }

    @Nullable
    private CachedStatusOverlayMesh buildCachedStatusOverlay(OverlayRenderType type, MeshData sourceMeshData,
                                                             RenderPipeline pipeline, BlockPos chunkOrigin,
                                                             int configSignature)
    {
        RenderContext ctx = new RenderContext(() -> "litematica:schematic_status_overlay_cached/" + type.name().toLowerCase(Locale.ROOT), pipeline);

        try
        {
            BufferBuilder builder = ctx.getBuilder();
            int copiedVertices = this.copyOverlayMeshDataWithOffset(type, sourceMeshData, new float[] { 0.0F, 0.0F, 0.0F }, builder);

            if (copiedVertices <= 0)
            {
                ctx.close();
                return null;
            }

            MeshData meshData = builder.build();

            if (meshData == null)
            {
                ctx.close();
                return null;
            }

            try (meshData)
            {
                CachedStatusOverlayMesh cached =
                        CachedStatusOverlayMesh.upload(sourceMeshData, pipeline, chunkOrigin.immutable(), type, copiedVertices, configSignature, meshData);
                ctx.close();
                return cached;
            }
        }
        catch (Exception err)
        {
            try
            {
                ctx.close();
            }
            catch (Exception ignored)
            {
            }

            throw new RuntimeException("Failed to build cached status overlay for " + type.name() + ": " + err.getMessage(), err);
        }
    }

    private void removeCachedStatusOverlay(ChunkRendererSchematicVbo renderer, OverlayRenderType type)
    {
        EnumMap<OverlayRenderType, CachedStatusOverlayMesh> rendererCache = this.statusOverlayCache.get(renderer);

        if (rendererCache == null)
        {
            return;
        }

        CachedStatusOverlayMesh cached = rendererCache.remove(type);

        if (cached != null)
        {
            cached.closeQuietly();
        }

        if (rendererCache.isEmpty())
        {
            this.statusOverlayCache.remove(renderer);
        }
    }

    private void closeStatusOverlayCache()
    {
        for (EnumMap<OverlayRenderType, CachedStatusOverlayMesh> rendererCache : this.statusOverlayCache.values())
        {
            for (CachedStatusOverlayMesh cached : rendererCache.values())
            {
                cached.closeQuietly();
            }
        }

        this.statusOverlayCache.clear();
    }

    private int copyOverlayMeshDataWithOffset(OverlayRenderType type, MeshData sourceMeshData, float[] offset, BufferBuilder builder)
    {
        ByteBuffer vertices = sourceMeshData.vertexBuffer().duplicate().order(ByteOrder.nativeOrder());
        int vertexSize = type.getVertexFormat().getVertexSize();
        int vertexCount = sourceMeshData.drawState().vertexCount();
        int copiedVertices = 0;

        for (int i = 0; i < vertexCount; ++i)
        {
            int base = vertices.position() + i * vertexSize;

            if (base + 16 > vertices.limit())
            {
                break;
            }

            float vx = vertices.getFloat(base) + offset[0];
            float vy = vertices.getFloat(base + 4) + offset[1];
            float vz = vertices.getFloat(base + 8) + offset[2];
            int color = ARGB.toABGR(vertices.getInt(base + 12));

            if (type == OverlayRenderType.OUTLINE)
            {
                float lineWidth = vertexSize >= 20 && base + 20 <= vertices.limit() ? vertices.getFloat(base + 16) : 1.0F;
                builder.addVertex(vx, vy, vz).setColor(color).setLineWidth(lineWidth);
            }
            else
            {
                builder.addVertex(vx, vy, vz).setColor(color);
            }

            ++copiedVertices;
        }

        return copiedVertices;
    }

    private static boolean isStatusOverlayTimingEnabled()
    {
        return Boolean.getBoolean(STATUS_OVERLAY_TIMING_PROPERTY);
    }

    private static boolean isStatusOverlayForceFallbackEnabled()
    {
        return Boolean.getBoolean(STATUS_OVERLAY_FORCE_FALLBACK_PROPERTY);
    }

    private int getStatusOverlayConfigSignature()
    {
        LayerRange range = DataManager.getRenderLayerRange();

        return Objects.hash(
                Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES.getBooleanValue(),
                Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES.getBooleanValue(),
                Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue(),
                Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY_CULLING.getBooleanValue(),
                Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue(),
                Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue(),
                Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue(),
                Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue(),
                Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.getDoubleValue(),
                Configs.Colors.SCHEMATIC_OVERLAY_COLOR_MISSING.getIntegerValue(),
                Configs.Colors.SCHEMATIC_OVERLAY_COLOR_EXTRA.getIntegerValue(),
                Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK.getIntegerValue(),
                Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_STATE.getIntegerValue(),
                Configs.Colors.SCHEMATIC_OVERLAY_COLOR_DIFF_BLOCK.getIntegerValue(),
                range.getLayerMode(),
                range.getAxis(),
                range.getLayerSingle(),
                range.getLayerAbove(),
                range.getLayerBelow(),
                range.getLayerRangeMin(),
                range.getLayerRangeMax(),
                range.getLayerMin(),
                range.getLayerMax()
        );

    }

    private void recordStatusOverlayTiming(long elapsedNanos, int vertices, int draws)
    {
        this.statusOverlayTimingFallbackNanos += elapsedNanos;
        this.statusOverlayTimingVerticesCopied += vertices;
        this.statusOverlayTimingFallbackDraws += draws;
    }

    private void logStatusOverlayTimingIfNeeded(boolean timingEnabled)
    {
        if (!timingEnabled)
        {
            return;
        }

        long now = System.currentTimeMillis();

        if (this.statusOverlayTimingLastLogTime == 0L)
        {
            this.statusOverlayTimingLastLogTime = now;
        }

        if (now - this.statusOverlayTimingLastLogTime < STATUS_OVERLAY_TIMING_LOG_INTERVAL_MS)
        {
            return;
        }

        LOGGER.info("[StatusOverlayTiming] cachedDrawMs={} cacheBuildMs={} fallbackCopyDrawMs={} overlayChunks={} verticesCopied={} cacheBuildVertices={} cachedBuffers={} cachedDrawCalls={} fallbackDrawCalls={} cacheBuilds={}",
                    String.format(Locale.ROOT, "%.3f", this.statusOverlayTimingCachedNanos / 1_000_000.0D),
                    String.format(Locale.ROOT, "%.3f", this.statusOverlayTimingCacheBuildNanos / 1_000_000.0D),
                    String.format(Locale.ROOT, "%.3f", this.statusOverlayTimingFallbackNanos / 1_000_000.0D),
                    this.statusOverlayTimingChunks,
                    this.statusOverlayTimingVerticesCopied,
                    this.statusOverlayTimingCacheBuildVertices,
                    this.statusOverlayTimingCachedBuffers,
                    this.statusOverlayTimingCachedDraws,
                    this.statusOverlayTimingFallbackDraws,
                    this.statusOverlayTimingCacheBuilds);

        this.statusOverlayTimingLastLogTime = now;
        this.statusOverlayTimingFallbackNanos = 0L;
        this.statusOverlayTimingFallbackDraws = 0L;
        this.statusOverlayTimingCachedNanos = 0L;
        this.statusOverlayTimingCachedBuffers = 0L;
        this.statusOverlayTimingCachedDraws = 0L;
        this.statusOverlayTimingCacheBuildNanos = 0L;
        this.statusOverlayTimingCacheBuilds = 0L;
        this.statusOverlayTimingCacheBuildVertices = 0L;
        this.statusOverlayTimingChunks = 0L;
        this.statusOverlayTimingVerticesCopied = 0L;
    }

    private static class CachedStatusOverlayMesh implements AutoCloseable
    {
        private final GpuBuffer vertexBuffer;
        private final RenderSystem.AutoStorageIndexBuffer sequentialIndexBuffer;
        private final MeshData sourceMeshData;
        private final RenderPipeline pipeline;
        private final BlockPos chunkOrigin;
        private final OverlayRenderType type;
        private final int indexCount;
        private final int vertexCount;
        private final int configSignature;
        private boolean valid;

        private CachedStatusOverlayMesh(GpuBuffer vertexBuffer, RenderSystem.AutoStorageIndexBuffer sequentialIndexBuffer,
                                        MeshData sourceMeshData, RenderPipeline pipeline,
                                        BlockPos chunkOrigin, OverlayRenderType type, int indexCount, int vertexCount,
                                        int configSignature)
        {
            this.vertexBuffer = vertexBuffer;
            this.sequentialIndexBuffer = sequentialIndexBuffer;
            this.sourceMeshData = sourceMeshData;
            this.pipeline = pipeline;
            this.chunkOrigin = chunkOrigin;
            this.type = type;
            this.indexCount = indexCount;
            this.vertexCount = vertexCount;
            this.configSignature = configSignature;
            this.valid = true;
        }

        private static CachedStatusOverlayMesh upload(MeshData sourceMeshData, RenderPipeline pipeline,
                                                      BlockPos chunkOrigin, OverlayRenderType type,
                                                      int vertexCount, int configSignature, MeshData meshData)
        {
            GpuDevice device = RenderSystem.getDevice();
            int expectedSize = meshData.vertexBuffer().remaining();
            GpuBuffer vertexBuffer = device.createBuffer(() -> "litematica:schematic_status_overlay_cached/" + type.name().toLowerCase(Locale.ROOT) + " VertexBuffer", 40, expectedSize);
            device.createCommandEncoder().writeToBuffer(vertexBuffer.slice(), meshData.vertexBuffer());
            RenderSystem.AutoStorageIndexBuffer sequentialIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());

            return new CachedStatusOverlayMesh(vertexBuffer, sequentialIndexBuffer, sourceMeshData, pipeline, chunkOrigin, type,
                                               meshData.drawState().indexCount(), vertexCount, configSignature);
        }

        private boolean matches(MeshData sourceMeshData, RenderPipeline pipeline, BlockPos chunkOrigin, int configSignature)
        {
            return this.valid &&
                   this.sourceMeshData == sourceMeshData &&
                   this.pipeline == pipeline &&
                   this.chunkOrigin.equals(chunkOrigin) &&
                   this.configSignature == configSignature;
        }

        private boolean isValid()
        {
            return this.valid && this.vertexBuffer.isClosed() == false && this.indexCount > 0;
        }

        private int vertexCount()
        {
            return this.vertexCount;
        }

        private void draw(RenderTarget fb, float[] offset)
        {
            GpuDevice device = RenderSystem.getDevice();
            GpuTextureView colorTexture = fb.getColorTextureView();
            GpuTextureView depthTexture = fb.useDepth ? fb.getDepthTextureView() : null;
            Matrix4f modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrixCopy()).translate(offset[0], offset[1], offset[2]);
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                                                    .writeTransform(modelViewMatrix,
                                                                    new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                                                                    new Vector3f(0.0F, 0.0F, 0.0F),
                                                                    new Matrix4f());
            GpuBuffer indexBuffer = this.sequentialIndexBuffer.getBuffer(this.indexCount);

            try (RenderPass pass = device.createCommandEncoder()
                                         .createRenderPass(() -> "litematica:schematic_status_overlay_cached/" + this.type.name().toLowerCase(Locale.ROOT),
                                                           colorTexture, OptionalInt.empty(),
                                                           depthTexture, OptionalDouble.empty()))
            {
                pass.setPipeline(this.pipeline);

                ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();

                if (scissorState.enabled())
                {
                    pass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
                }

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setIndexBuffer(indexBuffer, this.sequentialIndexBuffer.type());
                pass.setVertexBuffer(0, this.vertexBuffer);
                pass.drawIndexed(0, 0, this.indexCount, 1);
            }
        }

        private void closeQuietly()
        {
            try
            {
                this.close();
            }
            catch (Exception err)
            {
                LOGGER.warn("Failed to close cached status overlay mesh for {}: {}", this.type.name(), err.getMessage());
            }
        }

        @Override
        public void close() throws Exception
        {
            this.valid = false;
            this.vertexBuffer.close();
        }
    }

    @Override
    public boolean renderBlock(BlockAndTintGetter world, BlockState state, BlockPos pos, Vec3 offset, IBlockOutputSchematic output)
    {
        try
        {
            this.getProfiler().push("render_block");
            boolean result;
            BlockStateModel model = this.getModelForState(state);

            if (model != null)
            {
                result = this.getBlockRenderer().tessellateBlock(world, state, pos, offset, model, state.getSeed(pos), output);
//                System.out.printf("renderBlock(): result [%s] (stateIn: %s)\n", result, state.toString());
            }
            else
            {
                result = false;
//                System.out.printf("renderBlock(): result [false] (stateIn: %s)\n", state.toString());
            }

            this.getProfiler().pop();
            return result;
        }
        catch (Throwable e)
        {
            LOGGER.error("renderBlock(): Exception rendering block at pos {} [{}]; {}", pos.toShortString(), state.toString(), e.getLocalizedMessage());
        }

        return false;
    }

    @Override
    public boolean renderFluid(BlockAndTintGetter world, BlockState blockState, FluidState fluidState, BlockPos pos, FluidRenderer.Output output, final float offsetY)
    {
        try
        {
            this.getProfiler().push("render_fluid");
            FluidModel model = BlockModelCacheSchematic.INSTANCE.fetchFluidModel(fluidState);   // Pre-Fetch

            if (model != null)
            {
                if (offsetY != 0.0f)
                {
                    IFluidRendererInvoker invoker = (IFluidRendererInvoker) this.getFluidRenderer();
                    invoker.litematica$setOffsetY(offsetY);
                    invoker.litematica$tesselate(world, pos, output, blockState, fluidState);
                }
                else
                {
                    this.getFluidRenderer().tesselate(world, pos, output, blockState, fluidState);
                }

                return true;
            }

            this.getProfiler().pop();
        }
        catch (Throwable e)
        {
            LOGGER.error("renderFluid(): Exception rendering fluid at pos {} [{}]; {}", pos.toShortString(), fluidState.toString(), e.getLocalizedMessage());
        }

        return false;
    }

    @Override
    @Nullable
    public BlockStateModel getModelForState(BlockState state)
    {
        if (state.isAir())
        {
            return null;
        }

        return BlockModelCacheSchematic.INSTANCE.fetchBlockStateModel(state);
    }

    @Override
    public List<BlockStateModelPart> getModelParts(BlockPos pos, BlockState state, RandomSource rand)
    {
        List<BlockStateModelPart> parts = new ArrayList<>();
        BlockStateModel model = this.getModelForState(state);

        if (model != null)
        {
            model.collectParts(rand, parts);

            if (parts.isEmpty())
            {
                // Try Fallback Blocks first.
                model = this.getModelForState(this.getFallbackState(state));

                if (model != null)
                {
                    model.collectParts(rand, parts);
                }
            }

            if (parts.isEmpty())
            {
                model = this.getModelForState(state.getBlock().defaultBlockState());

                if (model != null)
                {
                    model.collectParts(rand, parts);
                }

                LOGGER.warn("getModelParts: Invalid Block Model for block at [{}] with state [{}]; Attempting to reset to default.", pos.toShortString(), state.toString());
            }
        }

        return parts;
    }

    @Override
    public void prepareEntities(Camera camera, Frustum frustum, LevelRenderState renderStates, DeltaTracker tickCounter, ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] prepareEntities()");
        this.profiler = profiler;

        if (this.renderEntitiesStartupCounter > 0)
        {
            --this.renderEntitiesStartupCounter;
        }
        else
        {
            profiler.push("entities_prepare");

            double cameraX = camera.position().x;
            double cameraY = camera.position().y;
            double cameraZ = camera.position().z;

            this.getEntityRenderer().prepare(camera, this.mc.crosshairPickEntity);
            this.countEntitiesTotal = 0;
            this.countEntitiesRendered = 0;
            this.countEntitiesHidden = 0;
            this.countEntitiesTotal = this.world.getRegularEntityCount();
            this.renderedEntities.clear();

            LayerRange layerRange = DataManager.getRenderLayerRange();

            profiler.popPush("entities_iterate");
            this.getSchematicRenderState().entityStates.clear();

            for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
            {
                BlockPos pos = chunkRenderer.getOrigin();
                ChunkPos chunkPos = chunkRenderer.getChunkPos();
//                ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
                ChunkSchematic chunk = this.world.getChunkSource().getChunkIfExists(chunkPos.x(), chunkPos.z());

                if (chunk == null || chunk.isEmpty() ||
                    !DataManager.getSchematicPlacementManager().checkIfChunkShouldRender(chunkPos.x(), chunkPos.z()))
                {
                    continue;
                }

//                List<Entity> list = chunk.getEntityList();
                AABB bb = chunkRenderer.getBoundingBox();
//                List<Entity> list = this.world.getEntities((Entity) null, bb, fi.dy.masa.litematica.util.EntityUtils.NOT_PLAYER);
                ImmutableList<Entity> list = this.world.getEntitiesByChunk(chunkPos.x(), chunkPos.z(), fi.dy.masa.litematica.util.EntityUtils.NOT_PLAYER);

//                LOGGER.error("[WorldRenderer] prepareEntities: Chunk: {}, EntityList [{}] // BB: [{}]", chunkPos.toString(), list.size(), bb.toString());
//                LOGGER.warn("[WorldRenderer] prepareEntities: Chunk: [{}], TestList: [{}]", pos.toShortString(), list.size());

                for (Entity entityTmp : list)
                {
//                    LOGGER.error("[WorldRenderer] prepareEntities/iterate: Chunk: {}, Entity [{}/{}], CHK-Pos: [X: {}, Y: {}, Z: {}]",
//                                            chunkPos.toString(),
//                                            entityTmp.getName().getString(), entityTmp.getStringUUID(),
//                                            entityTmp.getX(),
//                                            entityTmp.getY(),
//                                            entityTmp.getZ()
//                    );

                    if ((this.renderedEntities.containsKey(entityTmp.position()) && this.renderedEntities.get(entityTmp.position()).equals(entityTmp.getUUID())) ||
                        !layerRange.isPositionWithinRange(MathUtils.floor(entityTmp.getX()), MathUtils.floor(entityTmp.getY()), MathUtils.floor(entityTmp.getZ())))
                    {
//                        LOGGER.warn("[WorldRenderer] prepareEntities/iterate: Chunk: {}, Skipping POS / UUID [{}]", chunkPos.toString(), entityTmp.position(), entityTmp.getStringUUID());
                        continue;
                    }

	                float tickProgress = tickCounter.getGameTimeDeltaPartialTick(false);
                    boolean shouldRender;

                    if (entityTmp instanceof Avatar avatar)
                    {
                        if (isEntityType(avatar, "mannequin"))
                        {
                            try
                            {
                                ClientMannequin cm = (ClientMannequin) avatar;
                                ((IAvatarInvoker) cm).litematica$tryUpdateSkin();
                                EntityRenderState state = ((IEntityRendererInvoker) this.getEntityRenderer()).litematica_getRenderStateNullSafe(cm, tickProgress);

                                if (state != null)
                                {
                                    shouldRender = ((IEntityRendererInvoker) this.getEntityRenderer()).litematica_shouldRender(cm, frustum, cameraX, cameraY, cameraZ);

                                    if (shouldRender)
                                    {
                                        this.getSchematicRenderState().entityStates.add(state);
                                        this.renderedEntities.put(cm.position(), cm.getUUID());
                                        ++this.countEntitiesRendered;
                                    }
                                }
                            }
                            catch (Exception ex)
                            {
                                Litematica.LOGGER.error("Exception rendering Mannequin [{}]; {}", avatar.getClass().getName(), ex.getLocalizedMessage());
                            }
                        }
                        else if (isEntityType(avatar, "player"))
                        {
                            try
                            {
                                AbstractClientPlayer acp = (AbstractClientPlayer) avatar;
                                EntityRenderState state = ((IEntityRendererInvoker) this.getEntityRenderer()).litematica_getRenderStateNullSafe(acp, tickProgress);

                                if (state != null)
                                {
                                    shouldRender = ((IEntityRendererInvoker) this.getEntityRenderer()).litematica_shouldRender(acp, frustum, cameraX, cameraY, cameraZ);

                                    if (shouldRender)
                                    {
                                        this.getSchematicRenderState().entityStates.add(state);
                                        this.renderedEntities.put(acp.position(), acp.getUUID());
                                        ++this.countEntitiesRendered;
                                    }
                                }
                            }
                            catch (Exception ex)
                            {
                                Litematica.LOGGER.error("Exception rendering Player [{}]; {}", avatar.getClass().getName(), ex.getLocalizedMessage());
                            }
                        }

                        // Guess we can't (Or shouldn't) render Players
                        continue;
                    }
                    else if (entityTmp instanceof EnderDragon || entityTmp instanceof EnderDragonPart)
                    {
                        shouldRender = true;
                        // Still half broken.
                    }
                    else
                    {
                        shouldRender = ((IEntityRendererInvoker) this.getEntityRenderer()).litematica_shouldRender(entityTmp, frustum, cameraX, cameraY, cameraZ);
                    }

                    if (shouldRender)
                    {
//                        LOGGER.warn("[WorldRenderer] prepareEntities/shouldRender: Chunk: [{}], EntityPos [{}] // Adj. Pos: X [{}], Y [{}], Z [{}]",
//                                    pos.toShortString(), entityTmp.position().toString(),
//                                    entityTmp.getX(), entityTmp.getY(), entityTmp.getZ());

                        // Check for Salmon / Cod 'inWater' fix
                        // Because the entities might be following the ClientWorld State
                        if (entityTmp instanceof Salmon || entityTmp instanceof Cod ||
                            entityTmp instanceof Tadpole || entityTmp instanceof AbstractHorse ||
                            entityTmp instanceof TropicalFish || entityTmp instanceof AgeableWaterCreature)
                        {
                            BlockState state = this.world.getBlockState(entityTmp.blockPosition());
                            Fluid fluid = state.getFluidState() != null ? state.getFluidState().getType() : Fluids.EMPTY;

                            if ((fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) &&
                                !((IMixinEntity) entityTmp).litematica_isTouchingWater())
                            {
                                ((IEntityInvoker) entityTmp).litematica$toggleTouchingWater(true);
                            }
                        }

						EntityRenderState state = this.getEntityRenderer().extractEntity(entityTmp, tickProgress);
						this.getSchematicRenderState().entityStates.add(state);

                        this.renderedEntities.put(entityTmp.position(), entityTmp.getUUID());
                        ++this.countEntitiesRendered;
                    }
//                    else
//                    {
//                        LOGGER.warn("Skipping Entity at pos X: [{}], Y: [{}], Z: [{}] (Should Render = False)", entityTmp.getX(), entityTmp.getY(), entityTmp.getZ());
//                    }
                }
            }

            profiler.pop();
        }
    }

    @Override
	public void renderEntities(Camera camera, Frustum frustum, PoseStack matrices, LevelRenderState renderStates, SubmitNodeCollector queue, ProfilerFiller profiler)
	{
//        LOGGER.warn("[WorldRenderer] renderEntities()");
        if (this.getSchematicRenderState().entityStates.isEmpty())
        {
            return;
        }

		Vec3 pos = camera.position();
		double cameraX = pos.x();
		double cameraY = pos.y();
		double cameraZ = pos.z();

		profiler.push("render_entities");

		for (EntityRenderState state : this.getSchematicRenderState().entityStates)
		{
            if (state != null)      // This should never be NULL
            {
                this.getEntityRenderer().submit(state, this.getSchematicRenderState().cameraState, state.x - cameraX, state.y - cameraY, state.z - cameraZ, matrices, queue);
            }
		}

		profiler.pop();
	}

    @Override
	public void prepareBlockEntities(Camera camera, Frustum frustum, LevelRenderState renderStates, PoseStack matrices, float tickProgress, ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] prepareBlockEntities()");
        this.profiler = profiler;
        profiler.push("block_entities_prepare");

        double cameraX = camera.position().x;
        double cameraY = camera.position().y;
        double cameraZ = camera.position().z;

        this.getBlockEntityRenderer().prepare(camera.position());
        LayerRange layerRange = DataManager.getRenderLayerRange();
        this.profiler = profiler;
        this.getSchematicRenderState().blockEntityStates.clear();

        profiler.popPush("block_entities_iteration");
        for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
        {
            final ChunkPos chunkPos = chunkRenderer.getChunkPos();
            ChunkSchematic chunk = this.world.getChunkSource().getChunkForLighting(chunkPos.x(), chunkPos.z());

            if (chunk == null || chunk.isEmpty() || !DataManager.getSchematicPlacementManager().checkIfChunkShouldRender(chunkPos.x(), chunkPos.z()))
            {
                continue;
            }

            final ChunkRenderDataSchematic data = chunkRenderer.getChunkRenderData();
//            LOGGER.error("[WorldRenderer] prepareBlockEntities(): Chunk: [{}/{}] // data Built: [{}], chunk Built: [{}]", chunkPos.toString(), chunk.getState(), data.getTimeBuilt(), chunk.getTimeCreated());

            if (chunk.getState().atLeast(ChunkSchematicState.LOADED) && data.getTimeBuilt() >= chunk.getTimeCreated())
            {
                final ChunkMeshDataSchematic chunkMeshData = data.getMeshDataCache();
                List<BlockEntity> tiles = chunkMeshData.getBlockEntities();
                List<BlockEntity> noCullTiles = chunkMeshData.getNoCullBlockEntities();

//                LOGGER.warn("[WorldRenderer] prepareBlockEntities(): Chunk: {} // tiles: [{}], noCullTiles: [{}]", chunkPos.toString(), tiles.size(), noCullTiles.size());

                if (!tiles.isEmpty())
                {
                    for (BlockEntity te : tiles)
                    {
                        BlockPos pos = te.getBlockPos();

                        if (!layerRange.isPositionWithinRange(pos.getX(), pos.getY(), pos.getZ()))
                        {
                            continue;
                        }

//                        LOGGER.warn("[WorldRenderer] prepareBlockEntities(): type: [{}], pos: [{}], level: [{}]", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(te.getType()).toString(), te.getBlockPos(), te.getLevel().dimension().identifier().toString());

                        try
                        {
                            matrices.pushPose();
                            matrices.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
                            BlockEntityRenderState state = this.getBlockEntityRenderer().tryExtractRenderState(te, tickProgress, null, false);
                            this.getSchematicRenderState().blockEntityStates.add(state);
                            // Ignore crumbling, because there is no point in the Schem World.
                            matrices.popPose();
                        }
                        catch (Exception err)
                        {
                            LOGGER.error("[Pass 1] Error rendering blockEntities; Exception: {}", err.getLocalizedMessage());
                        }
                    }
                }

                if (!noCullTiles.isEmpty())
                {
                    for (BlockEntity te : noCullTiles)
                    {
                        BlockPos pos = te.getBlockPos();

                        if (!layerRange.isPositionWithinRange(pos.getX(), pos.getY(), pos.getZ()))
                        {
                            continue;
                        }

//                        LOGGER.warn("[WorldRenderer] prepareBlockEntities(): [NO-CULL] type: [{}], pos: [{}], level: [{}]", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(te.getType()).toString(), te.getBlockPos(), te.getLevel().dimension().identifier().toString());

                        try
                        {
                            matrices.pushPose();
                            matrices.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
                            BlockEntityRenderState state = this.getBlockEntityRenderer().tryExtractRenderState(te, tickProgress, null, true);
                            this.getSchematicRenderState().blockEntityStates.add(state);
                            // Ignore crumbling, because there is no point in the Schem World.
                            matrices.popPose();
                        }
                        catch (Exception err)
                        {
                            LOGGER.error("[Pass 2] Error rendering blockEntities; Exception: {}", err.getLocalizedMessage());
                        }
                    }
                }
            }
        }

//        profiler.popPush("render_be_no_cull");
//        synchronized (this.blockEntities)
//        {
//            for (BlockEntity te : this.blockEntities)
//            {
//                BlockPos pos = te.getBlockPos();
//
//                if (!layerRange.isPositionWithinRange(pos.getX(), pos.getY(), pos.getZ()))
//                {
//                    continue;
//                }
//
////                LOGGER.warn("[WorldRenderer] prepareBlockEntities(): [NO-CULL] type: [{}], pos: [{}], level: [{}]", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(te.getType()).toString(), te.getBlockPos(), te.getLevel().dimension().identifier().toString());
//
//                try
//                {
//                    matrices.pushPose();
//                    matrices.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
//					BlockEntityRenderState state = this.getBlockEntityRenderer().tryExtractRenderState(te, tickProgress, null);
//					this.schematicRenderState.tileEntityStates.add(state);
//                    matrices.popPose();
//                }
//                catch (Exception err)
//                {
//                    LOGGER.error("[Pass 2] Error rendering blockEntities; Exception: {}", err.getLocalizedMessage());
//                }
//            }
//        }

        profiler.pop();
    }

    @Override
	public void renderBlockEntities(Camera camera, Frustum frustum, PoseStack matrices, LevelRenderState renderStates, SubmitNodeCollector queue, ProfilerFiller profiler)
	{
//        LOGGER.warn("[WorldRenderer] renderBlockEntities()");
        if (this.getSchematicRenderState().blockEntityStates.isEmpty())
        {
            return;
        }

		Vec3 cameraPos = camera.position();
		double cameraX = cameraPos.x();
		double cameraY = cameraPos.y();
		double cameraZ = cameraPos.z();

		profiler.push("render_block_entities");

		for (BlockEntityRenderState state : this.getSchematicRenderState().blockEntityStates)
		{
            if (state != null)      // This should never be NULL
            {
                BlockPos pos = state.blockPos;
                matrices.pushPose();
                matrices.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
                this.getBlockEntityRenderer().submit(state, matrices, queue, this.getSchematicRenderState().cameraState);
                matrices.popPose();
            }
		}

		profiler.pop();
	}

//    @Override
//    public void updateBlockEntities(Collection<BlockEntity> toRemove, Collection<BlockEntity> toAdd)
//    {
//        LOGGER.warn("[WorldRenderer] updateBlockEntities()");
////        int last = this.blockEntities.size();
//
//        synchronized (this.blockEntities)
//        {
//            this.blockEntities.removeAll(toRemove);
//            this.blockEntities.addAll(toAdd);
//        }
//    }

    // `immediate` is only to be used with 'setBlockDirty()`
    @Override
    public void scheduleChunkRenders(int chunkX, int chunkZ, boolean immediate)
    {
//         LOGGER.warn("[WorldRenderer] scheduleChunkRenders()");
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            this.chunkRendererDispatcher.scheduleChunkRender(chunkX, chunkZ, immediate);
        }
    }

    @Override
    public ChunkSchematicState getChunkSchematicState(int chunkX, int chunkZ)
    {
        if (this.hasWorld())
        {
            return this.world.getChunkSource().getChunkState(chunkX, chunkZ);
        }

        return ChunkSchematicState.NO_WORLD_EXCEPTION;
    }

    @Override
    public void setChunkSchematicState(int chunkX, int chunkZ, ChunkSchematicState state)
    {
        if (this.hasWorld())
        {
            this.world.getChunkSource().setChunkState(chunkX, chunkZ, state);
        }
    }

    @Override
    public void reloadBlockRenderManager()
	{
        BlockModelCacheSchematic.INSTANCE.onReloadResources();
        this.getBlockRenderer().reload();
	}

    private int getLayerRenderSignature()
    {
        LayerRange range = DataManager.getRenderLayerRange();

        return Objects.hash(
                range.getLayerMode(),
                range.getAxis(),
                range.getLayerSingle(),
                range.getLayerAbove(),
                range.getLayerBelow(),
                range.getLayerRangeMin(),
                range.getLayerRangeMax(),
                range.getLayerMin(),
                range.getLayerMax()
        );
    }

    private void invalidateAllSchematicRenderChunksForLayerChange(int oldSignature, int newSignature)
    {
        int dirtyCount = 0;

        this.closeStatusOverlayCache();

        for (ChunkRendererSchematicVbo renderer : this.renderInfos)
        {
            if (renderer != null)
            {
                renderer.setNeedsUpdate(true);
                this.chunksToUpdate.add(renderer);
                ++dirtyCount;
            }
        }

        this.needsUpdate = true;
        this.clearWorldRenderStates();

        if (Boolean.getBoolean(RENDER_MODE_INVALIDATION_PROPERTY))
        {
            LayerRange range = DataManager.getRenderLayerRange();

            LOGGER.warn(
                    "[Litematica] Layer render state changed ({} -> {}), mode={}, axis={}, min={}, max={}, single={}, marked {} schematic chunks dirty",
                    oldSignature,
                    newSignature,
                    range.getLayerMode(),
                    range.getAxis(),
                    range.getLayerMin(),
                    range.getLayerMax(),
                    range.getLayerSingle(),
                    dirtyCount
            );
        }
    }

    private void checkLayerRenderStateChanged()
    {
        int currentSignature = this.getLayerRenderSignature();

        if (this.lastLayerRenderSignature == Integer.MIN_VALUE)
        {
            this.lastLayerRenderSignature = currentSignature;
            return;
        }

        if (currentSignature != this.lastLayerRenderSignature)
        {
            int oldSignature = this.lastLayerRenderSignature;
            this.lastLayerRenderSignature = currentSignature;
            this.invalidateAllSchematicRenderChunksForLayerChange(oldSignature, currentSignature);
        }
    }
}
