package fi.dy.masa.litematica.render.schematic;

import java.lang.Math;
import java.util.*;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.Logger;
import org.joml.*;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
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

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.render.uniform.ChunkFixUniform;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.malilib.util.position.LayerRange;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.compat.iris.IrisRenderingFix;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.entity.IMixinEntity;
import fi.dy.masa.litematica.render.IWorldSchematicRenderer;
import fi.dy.masa.litematica.util.invoker.IAvatarInvoker;
import fi.dy.masa.litematica.util.invoker.IEntityHitboxDebugRendererInvoker;
import fi.dy.masa.litematica.util.invoker.IEntityInvoker;
import fi.dy.masa.litematica.util.invoker.IEntityRendererInvoker;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.ChunkSchematicState;
import fi.dy.masa.litematica.world.WorldSchematic;

public class WorldRendererSchematic implements IWorldSchematicRenderer
{
    private static final Logger LOGGER = Litematica.LOGGER;
    private final Minecraft mc;
    private final List<ChunkRendererSchematicVbo> renderInfos;
//    private final Set<BlockEntity> blockEntities;
    private SchematicRenderState schematicRenderState;
    private Set<ChunkRendererSchematicVbo> chunksToUpdate;
    private WorldSchematic world;
    private ChunkRenderDispatcherSchematic chunkRendererDispatcher;
    private ChunkRenderGpuDispatcher chunkRendererGpuDispatcher;
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

    private final HashMap<Vec3, UUID> renderedEntities;
    private int renderDistanceChunks;
    private int renderEntitiesStartupCounter;
    private int countEntitiesTotal;
    private int countEntitiesRendered;
    private int countEntitiesHidden;

    private double lastTranslucentSortX;
    private double lastTranslucentSortY;
    private double lastTranslucentSortZ;
    private boolean needsUpdate;
    private boolean shouldDraw;

    public WorldRendererSchematic(Minecraft mc)
    {
        this.mc = mc;
        this.renderChunkFactory = ChunkRendererSchematicVbo::new;
//	    this.blockEntities = new HashSet<>();
	    this.renderInfos = new ArrayList<>(1024);
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
        int rcGpuTotal = this.chunkRendererGpuDispatcher != null ? this.chunkRendererGpuDispatcher.size() : 0;
        int rcRendered = this.chunkRendererDispatcher != null ? this.getRenderedChunks() : 0;
        return String.format(
                "C: %02d/%02d gU: %02d, %sD: %02d, L: %02d, %s", rcRendered, rcTotal, rcGpuTotal,
                this.mc.smartCull ? "(s) " : "",
                this.renderDistanceChunks, 0,
                this.renderDispatcher == null ? "null" : this.renderDispatcher.getDebugInfo());
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
    @Nullable
    public ChunkRenderGpuDispatcher getChunkRendererGpuDispatcher()
    {
        return this.chunkRendererGpuDispatcher;
    }

    @Override
    public BlockEntityRenderDispatcher getBlockEntityRenderer()
    {
        return BlockModelCacheSchematic.INSTANCE.blockEntityRenderer();
    }

//    @Override
//    public FluidModelRendererSchematic getFluidRenderer()
//    {
//        return BlockModelCacheSchematic.INSTANCE.fluidRenderer();
//    }

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
            this.chunksToUpdate.forEach(ChunkRendererSchematicVbo::deleteGlResources);
            this.chunksToUpdate.clear();
            this.renderInfos.forEach(ChunkRendererSchematicVbo::deleteGlResources);
            this.renderInfos.clear();

            if (this.chunkRendererDispatcher != null)
            {
                this.chunkRendererDispatcher.delete();
                this.chunkRendererDispatcher = null;
            }

            if (this.chunkRendererGpuDispatcher != null)
            {
                this.chunkRendererGpuDispatcher.destroy();
                this.chunkRendererGpuDispatcher = null;
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

            if (this.renderDispatcher == null)
            {
                this.renderDispatcher = new ChunkRenderDispatcherLitematica();
            }

            this.needsUpdate = true;
            this.renderDistanceChunks = this.mc.options.renderDistance().get() + 2;

            if (this.chunkRendererDispatcher != null)
            {
                this.chunkRendererDispatcher.delete();
            }

            if (this.chunkRendererGpuDispatcher != null)
            {
                this.chunkRendererGpuDispatcher.destroy();
                this.chunkRendererGpuDispatcher = null;
            }

            this.stopChunkUpdates(profiler);
			this.clearWorldRenderStates();

//            synchronized (this.blockEntities)
//            {
//                this.blockEntities.clear();
//            }

            BlockModelCacheSchematic.INSTANCE.onLoadRenderers();

            this.chunkRendererDispatcher = new ChunkRenderDispatcherSchematic(this.world, this.renderDistanceChunks, this, this.renderChunkFactory);
            this.chunkRendererGpuDispatcher = new ChunkRenderGpuDispatcher(this.world, this);
            this.renderEntitiesStartupCounter = 2;

            profiler.pop();
        }
    }

    protected void stopChunkUpdates(ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] stopChunkUpdates()");
        if (!this.chunksToUpdate.isEmpty())
        {
            this.chunksToUpdate.forEach(ChunkRendererSchematicVbo::deleteGlResources);
        }

        this.chunksToUpdate.clear();
        this.renderDispatcher.stopChunkUpdates();
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
            this.chunkRendererDispatcher.removeOutOfRangeRenderers(this.chunkRendererGpuDispatcher);
        }

        Vec3 cameraPos = camera.position();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        this.renderDispatcher.setCameraPosition(cameraPos);

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

                    this.renderDispatcher.updateChunkNow(chunkRendererTmp);
                    chunkRendererTmp.clearNeedsUpdate();

                    profiler.pop();
                }
            }
        }

        this.chunksToUpdate.addAll(set);

        if (Reference.DEBUG_MODE && !this.chunksToUpdate.isEmpty())
        {
            Litematica.LOGGER.warn("[WorldRenderer] setupTerrain / chunksToUpdate: {}", this.chunksToUpdate.size());
        }

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
        this.needsUpdate |= this.renderDispatcher.runChunkUploads(finishTimeNano);

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
                    flag = this.renderDispatcher.updateChunkNow(renderChunk);
                }
                else
                {
                    flag = this.renderDispatcher.updateChunkLater(renderChunk);
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

            if (Reference.DEBUG_MODE && index > 0)
            {
                LOGGER.info("[WorldRenderer] updateChunks(): {} Chunks updated.", index);
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
            this.needsUpdate |= this.renderDispatcher.runChunkUploads(finishTimeNano);
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
            final ChunkPos chunkPos = renderer.getChunkPos();
            final ChunkRenderGpuUploader gpuUploader = this.chunkRendererGpuDispatcher.addOrGetUploader(chunkPos.x(), chunkPos.z());
//            long now = System.currentTimeMillis();
//            int uboIndex = -1;

            for (ChunkSectionLayer layer : ChunkSectionLayer.values())
            {
                profiler.popPush("layer_"+ layer.label());

                if (!data.isBlockLayerEmpty(layer))
                {
                    ChunkRenderGpuBuffers buffers = gpuUploader.buffersOrNull(layer);

                    if (buffers == null || buffers.isClosed() || !chunkMeshData.hasMeshData(layer))
                    {
//                        LOGGER.error("Layer [{}], ChunkOrigin [{}], NO BUFFERS!", layer.name(), chunkOrigin.toShortString());
                        continue;
                    }

                    GpuBuffer indexBuffer;
                    IndexType indexType;

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

                    int pos = transformValues.size();

                    VertexFormat vf = layer.pipeline().getVertexFormatBinding(0);

                    transformValues.add(new DynamicUniforms.Transform(
                            matrix4fc,
                            colorMod,
                            new Vector3f((float) (chunkOrigin.getX() - cameraX), (float) (chunkOrigin.getY() - cameraY), (float) (chunkOrigin.getZ() - cameraZ)),
                            texMatrix
                    ));

                    renderMap.get(layer).add(
                            new RenderPass.Draw<>(
                                     0,
                                     buffers.getVertexBuffer(),
                                     indexBuffer, indexType,
                                     0,
                                     buffers.getIndexCount(),
                                     0,
                                     (slices, uploader) ->
                                             uploader.upload("DynamicTransforms", ((GpuBufferSlice[]) slices)[pos])
                             ));

                    startedDrawing = true;
                    ++count;
                }
            }
        }

        if (startedDrawing)
        {
            profiler.popPush("fill_uniforms");
            this.getSchematicRenderState().chunkFixUniform.updateBuffer(atlasWidth, atlasHeight, 1.0f);
            GpuBufferSlice[] transformSlices = RenderSystem.getDynamicUniforms()
                                                           .writeTransforms(
                                                                   transformValues.toArray(new DynamicUniforms.Transform[0])
                                                           );

            profiler.popPush("fill_batch_draw");
            this.getSchematicRenderState().batchDraw = new ChunkRenderBatchDraw(blockAtlas, renderMap,
                                                      renderCollidingBlocks, renderAsTranslucent, indexCount,
                                                      transformSlices,
                                                      this.getSchematicRenderState().chunkFixUniform.getCurrentBufferSlice()
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

            sampler = this.getGpuSampler();

            if (IrisCompat.isShaderActive() && !IrisRenderingFix.INSTANCE.wasWarned)
            {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.warn.shaders_on");
                IrisRenderingFix.INSTANCE.wasWarned = true;
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

//    @Override
//    public LegacyTerrainFixUniform getLegacyTerrainFixUniform()
//    {
//        return this.getSchematicRenderState().legacyTerrainFix;
//    }
//
//    @Override
//    public void clearLegacyTerrainFixUniform()
//    {
//        this.getSchematicRenderState().clearLegacyTerrainFixUniform();
//    }

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
                                                         1, OptionalDouble.empty()
                                          );
        }

        return this.gpuSampler;
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
                    this.renderDispatcher.updateTransparencyLater(chunkRenderer);
                }
            }
        }
    }

    @Override
    public void renderBlockOverlays(Camera camera, float lineWidth, ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] renderBlockOverlays()");
        this.profiler = profiler;
        this.renderBlockOverlay(OverlayRenderType.OUTLINE, camera, lineWidth, profiler);
        this.renderBlockOverlay(OverlayRenderType.QUAD, camera, lineWidth, profiler);
    }

    protected void renderBlockOverlay(OverlayRenderType type, Camera camera, float lineWidth, ProfilerFiller profiler)
    {
//        LOGGER.warn("[WorldRenderer] renderBlockOverlay() [{}]", type.name());
        profiler.push("overlay_" + type.name());
        this.profiler = profiler;

        Vec3 cameraPos = camera.position();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();
        RenderPipeline pipeline = renderThrough ? type.renderThrough() : type.pipeline();

        float[] offset = new float[]{0.3f, 0.0f, 0.6f};

        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();

        profiler.popPush("overlay_iterate");
        this.profiler = profiler;

        for (int i = this.renderInfos.size() - 1; i >= 0; --i)
        {
            ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);

            if (!renderer.getChunkRenderData().isEmpty() && renderer.hasOverlay())
            {
                final ChunkRenderDataSchematic compiledChunk = renderer.getChunkRenderData();
                final ChunkMeshDataSchematic chunkMeshData = compiledChunk.getMeshDataCache();
                final BlockPos chunkOrigin = renderer.getOrigin();
                final ChunkPos cp = renderer.getChunkPos();
                ChunkRenderGpuUploader gpuUploader = this.chunkRendererGpuDispatcher.addOrGetUploader(cp.x(), cp.z());

                if (!compiledChunk.isOverlayTypeEmpty(type))
                {
                    ChunkRenderGpuBuffers buffers = gpuUploader.buffersOrNull(type);

                    if (buffers == null || buffers.isClosed() || !chunkMeshData.hasMeshData(type))
                    {
//                        LOGGER.error("Overlay [{}], ChunkOrigin [{}], NO BUFFERS", type.name(), chunkOrigin.toShortString());
                        continue;
                    }

                    matrix4fStack.pushMatrix();
                    matrix4fStack.translate((float) (chunkOrigin.getX() - x), (float) (chunkOrigin.getY() - y), (float) (chunkOrigin.getZ() - z));
                    this.drawOverlayInternal(type, pipeline, buffers, -1, offset, false, false);
                    matrix4fStack.popMatrix();
                }
            }
        }

        profiler.pop();
    }

    @Override
    public boolean renderBlock(BlockModelRendererSchematic renderer, BlockAndTintGetter world, BlockState state, BlockPos pos, Vec3 offset, IBlockOutputSchematic output)
    {
        try
        {
            this.getProfiler().push("render_block");
            boolean result;
            BlockStateModel model = this.getModelForState(state);

            if (model != null)
            {
                result = renderer.tessellateBlock(world, state, pos, offset, model, state.getSeed(pos), output);
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
    public boolean renderFluid(FluidModelRendererSchematic renderer, BlockAndTintGetter world, BlockState blockState, FluidState fluidState, BlockPos pos, FluidRenderer.Output output, final float offsetY)
    {
        try
        {
            this.getProfiler().push("render_fluid");
            FluidModel model = BlockModelCacheSchematic.INSTANCE.fetchFluidModel(fluidState);   // Pre-Fetch

            if (model != null)
            {
                renderer.setYOffset(offsetY);
                renderer.tesselate(world, pos, output, blockState, fluidState);

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

    // Probably not the most efficient way; but it works.
    private void drawOverlayInternal(OverlayRenderType type,
                                     RenderPipeline pipeline,
                                     ChunkRenderGpuBuffers buffers,
                                     int color, float[] offset,
                                     boolean useColor, boolean useOffset) throws RuntimeException
    {
        if (RenderSystem.isOnRenderThread())
        {
            Vector4f colorMod = new Vector4f(1f, 1f, 1f, 1f);
            Vector3f modelOffset = new Vector3f();
            Matrix4f texMatrix = new Matrix4f();

            if (useOffset)
            {
                modelOffset.set(offset);
            }

            if (useColor)
            {
                float[] rgba = {ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color)};
                colorMod.set(rgba);
            }

            RenderTarget mainFb = RenderUtils.mainTarget();
            GpuTextureView texture1 = mainFb.getColorTextureView();
            GpuTextureView texture2 = mainFb.useDepth ? mainFb.getDepthTextureView() : null;
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(type.topology());
            GpuBuffer indexBuffer;
            IndexType indexType;

            if (buffers.getIndexBuffer() == null)
            {
                if (buffers.getIndexCount() > 0)
                {
                    indexBuffer = shapeIndexBuffer.getBuffer(buffers.getIndexCount());
                    indexType = shapeIndexBuffer.type();
                }
                else
                {
                    LOGGER.error("WorldRendererSchematic#drawInternal() [{}] --> setup IndexBuffer --> NO INDEX COUNT!", buffers.getName());
                    return;
                }
            }
            else
            {
                indexBuffer = buffers.getIndexBuffer();
                indexType = buffers.getIndexType();
            }

            GpuBufferSlice gpuSlice = RenderSystem.getDynamicUniforms()
                                                  .writeTransform(
                                                          RenderSystem.getModelViewMatrixCopy(),
                                                          colorMod,
                                                          modelOffset,
                                                          texMatrix);

            // Attach Frame buffers
            try (RenderPass pass = RenderSystem.getDevice()
                                               .createCommandEncoder()
                                               .createRenderPass(() -> "litematica:drawInternal/schematic_overlay",
                                                                 texture1, Optional.empty(),
                                                                 texture2, OptionalDouble.empty()))
            {
                pass.setPipeline(pipeline);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", gpuSlice);
                pass.setVertexBuffer(0, buffers.getVertexBuffer().slice());
                pass.setIndexBuffer(indexBuffer, indexType);
                pass.drawIndexed(buffers.getIndexCount(), 1, 0, 0, 0);
            }
        }
    }

    @Override
    @Nullable
    public BlockStateModel getModelForState(BlockState state)
    {
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
//                        LOGGER.warn("[WorldRenderer] prepareEntities/iterate: Chunk: {}, Skipping POS {} / UUID [{}]", chunkPos.toString(), entityTmp.position(), entityTmp.getStringUUID());
                        continue;
                    }

	                float tickProgress = tickCounter.getGameTimeDeltaPartialTick(false);
                    boolean shouldRender;

                    if (entityTmp instanceof Avatar avatar)
                    {
                        if (avatar.getType().equals(EntityTypes.MANNEQUIN))
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
                                LOGGER.error("Exception rendering Mannequin [{}]; {}", avatar.getClass().getName(), ex.getLocalizedMessage());
                            }
                        }
                        else if (avatar.getType().equals(EntityTypes.PLAYER))
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
                                LOGGER.error("Exception rendering Player [{}]; {}", avatar.getClass().getName(), ex.getLocalizedMessage());
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
//        this.getBlockRenderer().reload();
	}
}
