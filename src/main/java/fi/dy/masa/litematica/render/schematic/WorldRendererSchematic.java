package fi.dy.masa.litematica.render.schematic;

import java.util.*;
import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.*;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.BlockRenderView;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.entity.IMixinEntity;
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
    private Set<ChunkRendererSchematicVbo> chunksToUpdate = new LinkedHashSet<>();
    private WorldSchematic world;
    private ChunkRenderDispatcherSchematic chunkRendererDispatcher;
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
        this.profiler = null;
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

    protected int getRenderedChunks()
    {
        int count = 0;

        for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
        {
            // Threaded Code
            //ChunkRenderDataSchematic data = chunkRenderer.chunkRenderData.get();
            ChunkRenderDataSchematic data = chunkRenderer.chunkRenderData;

            if (data != ChunkRenderDataSchematic.EMPTY && !data.isEmpty())
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

	private <T extends Comparable<T>> BlockState getFallbackState(BlockState origState)
	{
		Collection<Property<?>> props = origState.getProperties();
		Block block = origState.getBlock();

		if (FallbackBlocks.BLOCK_TO_ID.containsKey(block))
		{
			Identifier id = FallbackBlocks.BLOCK_TO_ID.get(block);
			Litematica.LOGGER.warn("getFallbackState: Invalid Block State/Block Model for block [{}]; but we found a matching Litematica fallback block state that you can use.  Perhaps you have the Fusion mod installed?", origState.getBlock().getName().getString());
			BlockState newState = FallbackBlocks.ID_TO_STATE_MANAGER.get(id).getDefaultState();

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

			Litematica.debugLog("Fallback Block State -- OLD: [{}] --> NEW: [{}]", origState.toString(), newState.toString());
			return newState;
		}

		return origState;
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
        if (this.chunksToUpdate.isEmpty() == false)
        {
            this.chunksToUpdate.forEach(ChunkRendererSchematicVbo::deleteGlResources);
        }

        this.chunksToUpdate.clear();
        this.renderDispatcher.stopChunkUpdates(profiler);
        this.profiler = null;
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

        this.displayListEntitiesDirty = this.displayListEntitiesDirty || this.chunksToUpdate.isEmpty() == false ||
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

                if (chunkRendererTmp.needsImmediateUpdate() == false && isNear == false)
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

        if (this.chunksToUpdate.isEmpty() == false)
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

    public int renderBlockLayer(RenderLayer renderLayer, Matrix4f matrices, Camera camera, Matrix4f projMatrix, Profiler profiler, ShaderProgram shader)
    {
        this.profiler = profiler;
        RenderSystem.assertOnRenderThread();

        if (renderLayer.toString().contains("RenderType"))
        {
            profiler.push("layer_multi_phase");
        }
        else
        {
            profiler.push("layer_" + ChunkRenderLayers.getFriendlyName(renderLayer));
        }

        boolean isTranslucent = renderLayer == RenderLayer.getTranslucent();

        renderLayer.startDrawing();
        //RenderUtils.disableDiffuseLighting();
        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        if (isTranslucent)
        {
            profiler.push("translucent_sort");
            this.profiler = profiler;
            double diffX = x - this.lastTranslucentSortX;
            double diffY = y - this.lastTranslucentSortY;
            double diffZ = z - this.lastTranslucentSortZ;

            if (diffX * diffX + diffY * diffY + diffZ * diffZ > 1.0D)
            {
                //int i = ChunkSectionPos.getSectionCoord(x);
                //int j = ChunkSectionPos.getSectionCoord(y);
                //int k = ChunkSectionPos.getSectionCoord(z);
                //boolean block = i != ChunkSectionPos.getSectionCoord(this.lastTranslucentSortX) || k != ChunkSectionPos.getSectionCoord(this.lastTranslucentSortZ) || j != ChunkSectionPos.getSectionCoord(this.lastTranslucentSortY);
                this.lastTranslucentSortX = x;
                this.lastTranslucentSortY = y;
                this.lastTranslucentSortZ = z;
                int h = 0;

                for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
                {
                    //if ((chunkRenderer.getChunkRenderData().isBlockLayerStarted(renderLayer) || !block  && !chunkRenderer.isAxisAlignedWith(i, j, k) ||
                    if ((chunkRenderer.getChunkRenderData().isBlockLayerStarted(renderLayer) ||
                        (chunkRenderer.getChunkRenderData() != ChunkRenderDataSchematic.EMPTY && chunkRenderer.hasOverlay())) && h++ < 15)
                    {
                        this.renderDispatcher.updateTransparencyLater(chunkRenderer, profiler);
                    }
                }
            }

            profiler.pop();
        }

        //profiler.push("filter_empty");
        profiler.swap("layer_setup");

        boolean reverse = isTranslucent;
        int startIndex = reverse ? this.renderInfos.size() - 1 : 0;
        int stopIndex = reverse ? -1 : this.renderInfos.size();
        int increment = reverse ? -1 : 1;
        int count = 0;

        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
        Fog orgFog = RenderSystem.getShaderFog();
        //ShaderProgram shader = RenderSystem.getShader();
        BufferRenderer.reset();

        boolean renderAsTranslucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();

        if (renderAsTranslucent)
        {
            float alpha = (float) Configs.Visuals.GHOST_BLOCK_ALPHA.getDoubleValue();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        }

        profiler.swap("layer_uniforms");
        // As per IMS
        //initShader(shader, matrices, projMatrix);
        //shader.initializeUniforms(VertexFormat.DrawMode.QUADS, matrices, projMatrix, MinecraftClient.getInstance().getWindow());
        shader.initializeUniforms(renderLayer.getDrawMode(), matrices, projMatrix, MinecraftClient.getInstance().getWindow());
        RenderSystem.setupShaderLights(shader);
        RenderSystem.setShaderFog(Fog.DUMMY);
        shader.bind();

        //GlUniform chunkOffsetUniform = shader.modelOffset;
        boolean startedDrawing = false;

        profiler.swap("layer_iteration");
        this.profiler = profiler;
        for (int i = startIndex; i != stopIndex; i += increment)
        {
            ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);

            if (renderer.getChunkRenderData().isBlockLayerEmpty(renderLayer) == false)
            {
                BlockPos chunkOrigin = renderer.getOrigin();
                VertexBuffer buffer = renderer.getBlocksVertexBufferByLayer(renderLayer);

                if (buffer == null || buffer.isClosed())
                {
                    continue;
                }

                if (renderer.getChunkRenderData().getBuiltBufferCache().hasBuiltBufferByLayer(renderLayer) == false)
                {
                    continue;
                }

                /*
                if (chunkOffsetUniform != null)
                {
                    chunkOffsetUniform.set((float)(chunkOrigin.getX() - x), (float)(chunkOrigin.getY() - y), (float)(chunkOrigin.getZ() - z));
                    chunkOffsetUniform.upload();
                }
                 */

                matrix4fStack.pushMatrix();
                matrix4fStack.translate((float) (chunkOrigin.getX() - x), (float) (chunkOrigin.getY() - y), (float) (chunkOrigin.getZ() - z));
                buffer.bind();
                //buffer.draw(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), shader);
                buffer.draw(matrix4fStack, projMatrix, shader);
                VertexBuffer.unbind();
                matrix4fStack.popMatrix();
                startedDrawing = true;
                ++count;
            }
        }

        profiler.swap("layer_cleanup");
        if (renderAsTranslucent)
        {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        /*
        if (chunkOffsetUniform != null)
        {
            chunkOffsetUniform.set(0.0F, 0.0F, 0.0F);
        }
         */

        shader.unbind();

        if (startedDrawing)
        {
            renderLayer.getVertexFormat().clearState();
        }

        VertexBuffer.unbind();
        renderLayer.endDrawing();
        RenderSystem.setShaderFog(orgFog);

        profiler.pop();     // layer+ X
        //profiler.pop();

        return count;
    }

    public void renderBlockOverlays(Matrix4f viewMatrix, Camera camera, Matrix4f projMatrix, Profiler profiler)
    {
        this.profiler = profiler;
        this.renderBlockOverlay(OverlayRenderType.OUTLINE, viewMatrix, camera, projMatrix, profiler);
        this.renderBlockOverlay(OverlayRenderType.QUAD, viewMatrix, camera, projMatrix, profiler);
    }

    /*
    Disable, as per IMS

    protected static void initShader(ShaderProgram shader, Matrix4f matrix4f, Matrix4f projMatrix)
    {
        //for (int i = 0; i < 12; ++i) shader.addSampler("Sampler" + i, RenderSystem.getShaderTexture(i));

        if (shader.modelViewMat != null) shader.modelViewMat.set(matrix4f);
        if (shader.projectionMat != null) shader.projectionMat.set(projMatrix);
        if (shader.textureMat != null) shader.textureMat.set(RenderSystem.getTextureMatrix());
        if (shader.colorModulator != null) shader.colorModulator.set(RenderSystem.getShaderColor());
        if (shader.glintAlpha != null) shader.glintAlpha.set(RenderSystem.getShaderGlintAlpha());
        Fog fog = RenderSystem.getShaderFog();
        if (shader.fogStart != null) shader.fogStart.set(fog.start());
        if (shader.fogEnd != null) shader.fogEnd.set(fog.end());
        if (shader.fogColor != null) shader.fogColor.setAndFlip(fog.red(), fog.green(), fog.blue(), fog.alpha());
        if (shader.fogShape != null) shader.fogShape.set(fog.shape().getId());
        Window window = MinecraftClient.getInstance().getWindow();
        if (shader.screenSize != null) shader.screenSize.set((float) window.getFramebufferWidth(), (float) window.getFramebufferHeight());
        if (shader.gameTime != null) shader.gameTime.set(RenderSystem.getShaderGameTime());
        if (shader.lineWidth != null) shader.lineWidth.set(RenderSystem.getShaderLineWidth());
    }
     */

    protected void renderBlockOverlay(OverlayRenderType type, Matrix4f viewMatrix, Camera camera, Matrix4f projMatrix, Profiler profiler)
    {
        profiler.push("overlay_" + type.name());
        this.profiler = profiler;
        RenderLayer renderLayer = RenderLayer.getTranslucent();
        renderLayer.startDrawing();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        //profiler.swap("render");

        boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();

        if (renderThrough)
        {
            RenderSystem.disableDepthTest();
        }
        else
        {
            RenderSystem.enableDepthTest();
        }

        ShaderProgram originalShader = RenderSystem.getShader();
        ShaderProgram shader = RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferRenderer.reset();
        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();

        profiler.swap("overlay_iterate");
        this.profiler = profiler;
        for (int i = this.renderInfos.size() - 1; i >= 0; --i)
        {
            ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);

            if (renderer.getChunkRenderData() != ChunkRenderDataSchematic.EMPTY && renderer.hasOverlay())
            {
                ChunkRenderDataSchematic compiledChunk = renderer.getChunkRenderData();

                if (compiledChunk.isOverlayTypeEmpty(type) == false)
                {
                    VertexBuffer buffer = renderer.getOverlayVertexBuffer(type);
                    BlockPos chunkOrigin = renderer.getOrigin();

                    if (buffer == null || buffer.isClosed() || renderer.getChunkRenderData().getBuiltBufferCache().hasBuiltBufferByType(type) == false)
                    {
                        continue;
                    }

                    matrix4fStack.pushMatrix();
                    matrix4fStack.translate((float) (chunkOrigin.getX() - x), (float) (chunkOrigin.getY() - y), (float) (chunkOrigin.getZ() - z));
                    buffer.bind();
                    buffer.draw(matrix4fStack, projMatrix, shader);

                    VertexBuffer.unbind();
                    matrix4fStack.popMatrix();
                }
            }
        }

        renderLayer.endDrawing();

        RenderSystem.setShader(originalShader);
        RenderSystem.disableBlend();

        profiler.pop();
    }

    public boolean renderBlock(BlockRenderView world, BlockState state, BlockPos pos, MatrixStack matrices, BufferBuilder bufferBuilderIn)
    {
        try
        {
            BlockRenderType renderType = state.getRenderType();

            if (renderType == BlockRenderType.INVISIBLE)
            {
                return false;
            }
            else
            {
                this.getProfiler().push("render_block");
                boolean result;
                BlockModelRendererSchematic.enableCache();
                result = renderType == BlockRenderType.MODEL &&
                       this.blockModelRenderer.renderModel(world, this.getModelForState(state), state, pos, matrices, bufferBuilderIn, state.getRenderingSeed(pos));
                BlockModelRendererSchematic.disableCache();

                //System.out.printf("renderBlock(): result [%s]\n", result);

                // TODO --> For testing the Vanilla Block Model Renderer
                /*
                BlockModelRenderer.enableBrightnessCache();
                this.blockRenderManager.renderBlock(state, pos, world, matrices, bufferBuilderIn, true, Random.create(state.getRenderingSeed(pos)));
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

    public BakedModel getModelForState(BlockState state)
    {
        // FIXME (ENTITYBLOCK_ANIMATED - removed)
        /*
        if (state.getRenderType() == BlockRenderType.ENTITYBLOCK_ANIMATED)
        {
            return this.blockRenderManager.getModels().getModelManager().getMissingBlockModel();
        }
         */

        //return this.blockRenderManager.getModel(state);
	    BakedModel model = this.blockRenderManager.getModels().getModel(state);

		// fixme do not use
//		if (this.hasQuadsForModel(model, state, null))
//		{
//			return this.blockRenderManager.getModels().getModel(this.getFallbackState(state));
//		}

		return model;
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

//            MinecraftClient.getInstance().getBlockEntityRenderDispatcher().configure(this.world, camera, this.mc.crosshairTarget);
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

                if (!list.isEmpty())
                {
                    for (Entity entityTmp : list)
                    {
                        if (!layerRange.isPositionWithinRange((int) entityTmp.getX(), (int) entityTmp.getY(), (int) entityTmp.getZ()))
                        {
                            continue;
                        }

                        boolean shouldRender = this.entityRenderDispatcher.shouldRender(entityTmp, frustum, cameraX, cameraY, cameraZ);

                        if (shouldRender)
                        {
//                            double x = entityTmp.getX() - cameraX;
//                            double y = entityTmp.getY() - cameraY;
//                            double z = entityTmp.getZ() - cameraZ;

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

                            // TODO --> this render() call does not seem to have a push() and pop(),
                            //  and does not accept Matrix4f/Matrix4fStack as a parameter
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

            profiler.pop();
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
//        BlockEntityRenderDispatcher renderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();

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
