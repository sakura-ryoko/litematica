package fi.dy.masa.litematica.render.schematic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Identifier;
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
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.WorldSchematic;

public class WorldRendererSchematic
{
    private final MinecraftClient mc;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockRenderManager blockRenderManager;
    private final BlockModelRendererSchematic blockModelRenderer;
    private final Set<BlockEntity> blockEntities = new HashSet<>();
    private final List<ChunkRendererSchematicVbo> renderInfos = new ArrayList<>(1024);
    private final BufferBuilderStorage bufferBuilders;
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
        this.entityRenderDispatcher = mc.getEntityRenderDispatcher();
        this.bufferBuilders = mc.getBufferBuilders();
        this.renderChunkFactory = ChunkRendererSchematicVbo::new;
        this.blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
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
            this.blockEntities.clear();
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

//        if (this.defaultTex != null)
//        {
//            this.defaultTex.close();
//            this.defaultTex = null;
//        }
//
//        this.defaultTexId = -1;
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

    public int renderBlockLayer(RenderLayer renderLayer, Camera camera, Profiler profiler, RenderPipeline pipeline)
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

        renderLayer.startDrawing();

        //RenderUtils.disableDiffuseLighting();
        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        if (renderLayer.isTranslucent())
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

        boolean reverse = renderLayer.isTranslucent();
        int startIndex = reverse ? this.renderInfos.size() - 1 : 0;
        int stopIndex = reverse ? -1 : this.renderInfos.size();
        int increment = reverse ? -1 : 1;
        int count = 0;

        Fog orgFog = RenderSystem.getShaderFog();
        ArrayList<RenderPass.RenderObject> arrayList = new ArrayList<>();
        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(renderLayer.getDrawMode());
        int indexCount = 0;

        boolean renderAsTranslucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
        int color = -1;

        if (renderAsTranslucent)
        {
            float alpha = (float) Configs.Visuals.GHOST_BLOCK_ALPHA.getDoubleValue();
            color = RenderUtils.color(1.0f, 1.0f, 1.0f, alpha);
        }

        profiler.swap("layer_uniforms");
        // As per IMS
        //initShader(shader, matrices, projMatrix);
        //shader.initializeUniforms(VertexFormat.DrawMode.QUADS, matrices, projMatrix, MinecraftClient.getInstance().getWindow());
        //shader.initializeUniforms(renderLayer.getDrawMode(), matrices, projMatrix, MinecraftClient.getInstance().getWindow());
        //RenderSystem.setupShaderLights(shader);
        RenderSystem.setShaderFog(Fog.DUMMY);

        //GlUniform chunkOffsetUniform = shader.modelOffset;
        boolean startedDrawing = false;

        profiler.swap("layer_iteration");
        this.profiler = profiler;
        for (int i = startIndex; i != stopIndex; i += increment)
        {
            ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);

            if (!renderer.getChunkRenderData().isBlockLayerEmpty(renderLayer))
            {
                BlockPos chunkOrigin = renderer.getOrigin();
                ChunkRenderObjectBuffers buffers = renderer.getBlockBuffersByLayer(renderLayer);

//                if (buffers == null || buffers.isClosed() || !renderer.getChunkRenderData().getBuiltBufferCache().hasBuiltBufferByLayer(renderLayer))
//                {
//                    Litematica.LOGGER.error("Layer [{}], ChunkOrigin [{}], NO BUFFERS!", ChunkRenderLayers.getFriendlyName(renderLayer), chunkOrigin.toShortString());
//                    continue;
//                }

                if (buffers == null)
                {
                    Litematica.LOGGER.error("Layer [{}], ChunkOrigin [{}], NO BUFFERS (NULL)", ChunkRenderLayers.getFriendlyName(renderLayer), chunkOrigin.toShortString());
                    continue;
                }

                if (buffers.isClosed())
                {
                    Litematica.LOGGER.error("Layer [{}], ChunkOrigin [{}], NO BUFFERS (CLOSED)", ChunkRenderLayers.getFriendlyName(renderLayer), chunkOrigin.toShortString());
                    continue;
                }

                if (!renderer.getChunkRenderData().getBuiltBufferCache().hasBuiltBufferByLayer(renderLayer))
                {
                    Litematica.LOGGER.error("Layer [{}], ChunkOrigin [{}], NO BUFFERS (NO DATA) ", ChunkRenderLayers.getFriendlyName(renderLayer), chunkOrigin.toShortString());
                    continue;
                }

                GpuBuffer gpuBuffer;
                VertexFormat.IndexType indexType;

                if (buffers.getIndexBuffer() == null)
                {
                    if (buffers.getIndexCount() > indexCount)
                    {
                        indexCount = buffers.getIndexCount();
                    }

                    gpuBuffer = null;
                    indexType = null;
                }
                else
                {
                    gpuBuffer = buffers.getIndexBuffer();
                    indexType = buffers.getIndexType();
                }

                /*
                if (chunkOffsetUniform != null)
                {
                    chunkOffsetUniform.set((float)(chunkOrigin.getX() - x), (float)(chunkOrigin.getY() - y), (float)(chunkOrigin.getZ() - z));
                    chunkOffsetUniform.upload();
                }
                 */

                arrayList.add(new RenderPass.
                        RenderObject(0, buffers.getVertexBuffer(), gpuBuffer, indexType, 0, buffers.getIndexCount(),
                                     uniform -> uniform.upload("ModelOffset", (float) (chunkOrigin.getX() - x), (float) (chunkOrigin.getY() - y), (float) (chunkOrigin.getZ() - z))));

                startedDrawing = true;
                ++count;
            }
        }

        profiler.swap("layer_draw");

        if (startedDrawing)
        {
            GpuBuffer indexBuffer = indexCount == 0 ? null : shapeIndexBuffer.getIndexBuffer(indexCount);
            VertexFormat.IndexType indexTypeDraw = indexCount == 0 ? null : shapeIndexBuffer.getIndexType();

            try (RenderPass pass = RenderSystem.getDevice()
                                               .createCommandEncoder()
                                               .createRenderPass(renderLayer.getTarget().getColorAttachment(), OptionalInt.empty(),
                                                                 renderLayer.getTarget().getDepthAttachment(), OptionalDouble.empty()))
            {
                pass.setPipeline(pipeline);

                for (int k = 0; k < 12; k++)
                {
                    GpuTexture texture = RenderSystem.getShaderTexture(k);

                    if (texture != null)
                    {
                        pass.bindSampler("Sampler" + k, texture);
                    }
                }

                pass.drawMultipleIndexed(arrayList, indexBuffer, indexTypeDraw);
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

        //shader.unbind();

        if (startedDrawing)
        {
            // todo
//            renderLayer.getVertexFormat().clearState();
        }

//        VertexBuffer.unbind();

        renderLayer.endDrawing();
        RenderSystem.setShaderFog(orgFog);

        profiler.pop();     // layer+ X
        //profiler.pop();

        return count;
    }

    public void renderBlockOverlays(@Nullable Framebuffer otherFb, Camera camera, float lineWidth, Profiler profiler)
    {
        this.profiler = profiler;
        this.renderBlockOverlay(OverlayRenderType.OUTLINE, otherFb, camera, lineWidth, profiler);
        this.renderBlockOverlay(OverlayRenderType.QUAD, otherFb, camera, lineWidth, profiler);
    }

//    public void renderBlockOverlayQuads(@Nullable Framebuffer otherFb, Matrix4f posMatrix, Matrix4f projMatrix, Frustum frustum, Camera camera, Fog fog, BufferBuilderStorage buffers, float lineWidth, Profiler profiler)
//    {
//        this.profiler = profiler;
//        this.renderBlockOverlay(OverlayRenderType.QUAD, otherFb, camera, lineWidth, profiler);
//    }
//
//    public void renderBlockOverlayOutlines(@Nullable Framebuffer otherFb, Matrix4f posMatrix, Matrix4f projMatrix, Frustum frustum, Camera camera, Fog fog, BufferBuilderStorage buffers, float lineWidth, Profiler profiler)
//    {
//        this.profiler = profiler;
//        this.renderBlockOverlay(OverlayRenderType.OUTLINE, otherFb, camera, lineWidth, profiler);
//    }

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

    protected void renderBlockOverlay(OverlayRenderType type, @Nullable Framebuffer otherFb, Camera camera, float lineWidth, Profiler profiler)
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

        float[] offset = new float[]{0.3f, 3.0f, 0.6f};

        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
        MatrixStack matrices = new MatrixStack();

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

//                    if (buffers == null || buffers.isClosed() || !renderer.getChunkRenderData().getBuiltBufferCache().hasBuiltBufferByType(type))
//                    {
//                        Litematica.LOGGER.error("Overlay [{}], ChunkOrigin [{}], NO BUFFERS", type.name(), chunkOrigin.toShortString());
//                        continue;
//                    }

                    if (buffers == null)
                    {
                        Litematica.LOGGER.error("Overlay [{}], ChunkOrigin [{}], NO BUFFERS (NULL)", type.name(), chunkOrigin.toShortString());
                        continue;
                    }

                    if (buffers.isClosed())
                    {
                        Litematica.LOGGER.error("Overlay [{}], ChunkOrigin [{}], NO BUFFERS (CLOSED)", type.name(), chunkOrigin.toShortString());
                        continue;
                    }

                    if (!renderer.getChunkRenderData().getBuiltBufferCache().hasBuiltBufferByType(type))
                    {
                        Litematica.LOGGER.error("Overlay [{}], ChunkOrigin [{}], NO BUFFERS (NO DATA)", type.name(), chunkOrigin.toShortString());
                        continue;
                    }

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

                    this.drawInternal(otherFb, pipeline, buffers, -1, offset, lineWidth, false, true, (type == OverlayRenderType.OUTLINE));

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

//    protected void bindTexture(Identifier id, int textureId, int width, int height) throws RuntimeException
//    {
//        if (textureId < 0 || textureId > 12)
//        {
//            throw new RuntimeException("Invalid textureId of: "+textureId+" for texture: "+id.toString());
//        }
//
//        try
//        {
//            // Verify that we potentially have the correct texture by checking various values
//            while (!this.isTextureValid(width, height))
//            {
//                this.defaultTex = (ResourceTexture) fi.dy.masa.malilib.render.RenderUtils.tex().getTexture(id);
//
//                if (this.isTextureValid(width, height))
//                {
//                    if (this.defaultTex != null)
//                    {
//                        this.defaultTexId = textureId;
//                        this.defaultTex.setFilter(TriState.DEFAULT, false);
//                        RenderSystem.setShaderTexture(textureId, this.defaultTex.getGlTexture());
//                        return;
//                    }
//
//                    break;
//                }
//            }
//        }
//        catch (Exception err)
//        {
//            throw new RuntimeException("Exception reading Texture ["+id.toString()+"]: "+err.getMessage());
//        }
//
//        // General failure & cleanup
//        if (this.defaultTex != null)
//        {
//            // Simple texture rebind since we already have a valid texture
//            this.defaultTexId = textureId;
//            RenderSystem.setShaderTexture(textureId, this.defaultTex.getGlTexture());
//            return;
//        }
//
//        Litematica.LOGGER.error("bindTexture: Error uploading texture [{}]", id.toString());
//
//        if (this.defaultTex != null)
//        {
//            this.defaultTex.close();
//        }
//
//        this.defaultTex = null;
//    }
//
//    private boolean isTextureValid(int width, int height)
//    {
//        if (this.defaultTex == null)
//        {
//            return false;
//        }
//
//        try (TextureContents content = this.defaultTex.loadContents(fi.dy.masa.malilib.render.RenderUtils.mc().getResourceManager()))
//        {
//            NativeImage image = content.image();
//
//            if (image == null || image.getWidth() != width || image.getHeight() != height)
//            {
//                this.defaultTex.close();
//                this.defaultTex = null;
//                return false;
//            }
//        }
//        catch (Exception e)
//        {
//            this.defaultTex.close();
//            this.defaultTex = null;
//            return false;
//        }
//
//        if (((IMixinAbstractTexture) this.defaultTex).malilib_getGlTexture() == null ||
//            this.defaultTex.getGlTexture().isClosed())
//        {
//            this.defaultTex.close();
//            this.defaultTex = null;
//            return false;
//        }
//
//        return true;
//    }

    private void dumpTexture(@Nonnull GpuTexture gpuTexture, Identifier id)
    {
        int mip = gpuTexture.getMipLevels();
        int width = gpuTexture.getWidth(mip);
        int height = gpuTexture.getHeight(mip);

        GpuBuffer gpuBuffer = RenderSystem.getDevice()
                                          .createBuffer(() -> "Debug Texture", BufferType.PIXEL_PACK, BufferUsage.STATIC_READ,
                                                        width * height * gpuTexture.getFormat().pixelSize()
                                          );

        try (GpuBuffer.ReadView readView = RenderSystem.getDevice().createCommandEncoder().readBuffer(gpuBuffer))
        {
            NativeImage nativeImage = new NativeImage(width, height, false);

            for (int k = 0; k < height; k++)
            {
                for (int l = 0; l < width; l++)
                {
                    int m = readView.data().getInt((l + k * width) * gpuTexture.getFormat().pixelSize());
                    nativeImage.setColor(l, height - k - 1, m | 0xFF000000);
                }
            }

            Path dir = FileUtils.getConfigDirectoryAsPath().resolve(Reference.MOD_ID).resolve("textures");
            //  (TextureContents content = ((ReloadableTexture) texture).loadContents(RenderUtils.mc().getResourceManager()))

            try
            {
                if (!Files.isDirectory(dir))
                {
                    Files.createDirectory(dir);
                }

                nativeImage.writeTo(dir.resolve(FileNameUtils.generateSimpleSafeFileName(id.toString() + ".png")));
            }
            catch (Exception err)
            {
                Litematica.LOGGER.error("dumpTexture: Error saving debug texture for [{}]", id.toString());
            }

            nativeImage.close();
        }
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

    private void drawInternal(@Nullable Framebuffer otherFb, RenderPipeline pipeline,
                              ChunkRenderObjectBuffers buffers,
                              int color, float[] offset, float lineWidth,
                              boolean useColor, boolean useOffset, boolean setLineWidth) throws RuntimeException
    {
        if (RenderSystem.isOnRenderThread())
        {
            if (useColor)
            {
                float[] rgba = {ColorHelper.getRedFloat(color), ColorHelper.getGreenFloat(color), ColorHelper.getBlueFloat(color), ColorHelper.getAlphaFloat(color)};
                RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
            }

            if (useOffset)
            {
                RenderSystem.setModelOffset(-offset[0], offset[1], -offset[2]);
            }

            Framebuffer mainFb = RenderUtils.fb();
            GpuTexture texture1;
            GpuTexture texture2;

            if (otherFb != null)
            {
                texture1 = otherFb.getColorAttachment();
                texture2 = otherFb.useDepthAttachment ? otherFb.getDepthAttachment() : null;
            }
            else
            {
                texture1 = mainFb.getColorAttachment();
                texture2 = mainFb.useDepthAttachment ? mainFb.getDepthAttachment() : null;
            }

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

            // Attach Frame buffers
            try (RenderPass pass = RenderSystem.getDevice()
                                               .createCommandEncoder()
                                               .createRenderPass(texture1, OptionalInt.empty(),
                                                                 texture2, OptionalDouble.empty()))
            {
//                Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] renderPass --> setPipeline() [{}]", buffers.getName(), pipeline.getLocation().toString());
                pass.setPipeline(pipeline);

//                if (this.defaultTexId > -1 && this.defaultTexId < 12 && this.defaultTex != null)
//                {
////                    Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] renderPass --> bindSampler() [{}] // DEFAULT", buffers.getName(), this.defaultTexId);
//                    pass.bindSampler("Sampler"+this.defaultTexId, this.defaultTex.getGlTexture());
//                }

//                for (int i = 0; i < 12; i++)
//                {
//                    if (i == this.defaultTexId && this.defaultTex != null)
//                    {
//                        continue;
//                    }
//
//                    GpuTexture drawableTexture = RenderSystem.getShaderTexture(i);
//
//                    if (drawableTexture != null)
//                    {
//                        Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] renderPass --> bindSampler() [{}] // OTHER", buffers.getName(), i);
//                        pass.bindSampler("Sampler"+i, drawableTexture);
//                    }
//                }

                if (setLineWidth)
                {
                    float width = lineWidth > 0.0f ? lineWidth : RenderSystem.getShaderLineWidth();
//                    Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] renderPass --> setUniform() // lineWidth [{}]", buffers.getName(), width);
                    pass.setUniform("LineWidth", width);
                }

//                Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] renderPass --> setVertexBuffer() [0]", buffers.getName());
                pass.setVertexBuffer(0, buffers.getVertexBuffer());
//                Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] renderPass --> setIndexBuffer() [{}]", buffers.getName(), indexType.name());
                pass.setIndexBuffer(indexBuffer, indexType);
//                Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] renderPass --> drawIndexed() [0, {}]", buffers.getName(), buffers.getIndexCount());
                pass.drawIndexed(0, buffers.getIndexCount());
            }

//            Litematica.LOGGER.warn("WorldRendererSchematic#drawInternal() [{}] --> END", buffers.getName());

            if (useOffset)
            {
                RenderSystem.resetModelOffset();
            }

            if (useColor)
            {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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
        return this.getModelForState(state).getParts(rand);
    }

    public void renderEntities(Camera camera, Frustum frustum, Matrix4f posMatrix, float partialTicks, Profiler profiler)
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

            MinecraftClient.getInstance().getBlockEntityRenderDispatcher().configure(this.world, camera, this.mc.crosshairTarget);
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

            MatrixStack matrixStack = new MatrixStack();
            matrixStack.push();
            matrixStack.multiplyPositionMatrix(posMatrix);
            matrixStack.pop();

            VertexConsumerProvider.Immediate entityVertexConsumers = this.bufferBuilders.getEntityVertexConsumers();
            LayerRange layerRange = DataManager.getRenderLayerRange();

            profiler.swap("regular_iterate");
            this.profiler = profiler;
            for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
            {
                BlockPos pos = chunkRenderer.getOrigin();
                ChunkSchematic chunk = this.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                List<Entity> list = chunk.getEntityList();

                if (list.isEmpty() == false)
                {
                    for (Entity entityTmp : list)
                    {
                        if (layerRange.isPositionWithinRange((int) entityTmp.getX(), (int) entityTmp.getY(), (int) entityTmp.getZ()) == false)
                        {
                            continue;
                        }

                        boolean shouldRender = this.entityRenderDispatcher.shouldRender(entityTmp, frustum, cameraX, cameraY, cameraZ);

                        if (shouldRender)
                        {
                            double x = entityTmp.getX() - cameraX;
                            double y = entityTmp.getY() - cameraY;
                            double z = entityTmp.getZ() - cameraZ;

                            matrixStack.push();

                            // TODO --> this render() call does not seem to have a push() and pop(),
                            //  and does not accept Matrix4f/Matrix4fStack as a parameter
                            this.entityRenderDispatcher.render(entityTmp, x, y, z, partialTicks, matrixStack, entityVertexConsumers, this.entityRenderDispatcher.getLight(entityTmp, partialTicks));
                            ++this.countEntitiesRendered;

                            matrixStack.pop();
                        }
                    }
                }
            }

            profiler.swap("block_entities");
            this.profiler = profiler;
            BlockEntityRenderDispatcher renderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();

            profiler.swap("block_entities_iterate");
            for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos)
            {
                ChunkRenderDataSchematic data = chunkRenderer.getChunkRenderData();
                List<BlockEntity> tiles = data.getBlockEntities();

                if (tiles.isEmpty() == false)
                {
                    BlockPos chunkOrigin = chunkRenderer.getOrigin();
                    ChunkSchematic chunk = this.world.getChunkProvider().getChunk(chunkOrigin.getX() >> 4, chunkOrigin.getZ() >> 4);

                    if (chunk != null && data.getTimeBuilt() >= chunk.getTimeCreated())
                    {
                        for (BlockEntity te : tiles)
                        {
                            try
                            {
                                BlockPos pos = te.getPos();
                                matrixStack.push();
                                matrixStack.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);

                                // TODO --> this render() call does not seem to have a push() and pop(),
                                //  and does not accept Matrix4f/Matrix4fStack as a parameter
                                renderer.render(te, partialTicks, matrixStack, entityVertexConsumers);

                                matrixStack.pop();
                            }
                            catch (Exception ignore)
                            {
                            }
                        }
                    }
                }
            }

            profiler.swap("block_entities_render");
            synchronized (this.blockEntities)
            {
                for (BlockEntity te : this.blockEntities)
                {
                    try
                    {
                        BlockPos pos = te.getPos();
                        matrixStack.push();
                        matrixStack.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);

                        // TODO --> this render() call does not seem to have a push() and pop(),
                        //  and does not accept Matrix4f/Matrix4fStack as a parameter
                        renderer.render(te, partialTicks, matrixStack, entityVertexConsumers);

                        matrixStack.pop();
                    }
                    catch (Exception ignore)
                    {
                    }
                }
            }

            profiler.pop();
        }
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
