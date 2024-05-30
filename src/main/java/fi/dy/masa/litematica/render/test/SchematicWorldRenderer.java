package fi.dy.masa.litematica.render.test;

import javax.annotation.Nonnull;
import java.util.*;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.test.buffer.SchematicBuilderStorage;
import fi.dy.masa.litematica.render.test.buffer.SchematicBuiltChunkStorage;
import fi.dy.masa.litematica.render.test.builder.SchematicChunkBuilder;
import fi.dy.masa.litematica.render.test.builder.SchematicRegionBuilder;
import fi.dy.masa.litematica.render.test.data.SchematicBuiltChunk;
import fi.dy.masa.litematica.render.test.data.SchematicChunkRenderData;
import fi.dy.masa.litematica.render.test.dispatch.SchematicBlockRenderManager;
import fi.dy.masa.litematica.world.WorldSchematic;

public class SchematicWorldRenderer
{
    public static int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    private MinecraftClient mc;
    private WorldSchematic world;
    private SchematicBlockRenderManager blockRenderManager;
    private BlockRenderManager vanillaBlockRenderManager;
    private EntityRenderDispatcher entityRenderDispatch;
    private BlockEntityRenderDispatcher blockEntityRenderDispatch;
    private BufferBuilderStorage vanillaBufferBuilders;
    private SchematicBuilderStorage bufferBuilders;
    @Nullable
    private SchematicChunkBuilder chunkBuilder;
    @Nullable
    private SchematicBuiltChunkStorage chunkStorage;
    private Set<BlockEntity> noCullingBlockEntities = Sets.newHashSet();
    private final ObjectArrayList<SchematicBuiltChunk> builtChunks = new ObjectArrayList<>();
    private boolean fancyGraphics;
    private int viewDistance;

    private double lastCameraChunkUpdateX = Double.MIN_VALUE;
    private double lastCameraChunkUpdateY = Double.MIN_VALUE;
    private double lastCameraChunkUpdateZ = Double.MIN_VALUE;
    private double lastCameraX = Double.MIN_VALUE;
    private double lastCameraY = Double.MIN_VALUE;
    private double lastCameraZ = Double.MIN_VALUE;
    private float lastCameraPitch = Float.MIN_VALUE;
    private float lastCameraYaw = Float.MIN_VALUE;

    private int renderDistanceChunks = -1;
    private int renderEntitiesStartupCounter = 2;
    private int countEntitiesTotal;
    private int countEntitiesRendered;
    private int countEntitiesHidden;

    private double lastTranslucentSortX;
    private double lastTranslucentSortY;
    private double lastTranslucentSortZ;
    private boolean displayListEntitiesDirty = true;

    public SchematicWorldRenderer(@Nonnull MinecraftClient mc, @Nullable BlockRenderManager vanillaBlockRenderManager, @Nullable EntityRenderDispatcher entityDispatch, @Nullable BlockEntityRenderDispatcher blockEntityDispatch, @Nullable SchematicBuilderStorage bufferBuilders, @Nonnull BufferBuilderStorage vanillaBufferBuilders)
    {
        this.mc = mc;
        this.vanillaBlockRenderManager = vanillaBlockRenderManager != null ? vanillaBlockRenderManager : mc.getBlockRenderManager();
        this.entityRenderDispatch = entityDispatch != null ? entityDispatch : mc.getEntityRenderDispatcher();
        this.blockEntityRenderDispatch = blockEntityDispatch != null ? blockEntityDispatch : mc.getBlockEntityRenderDispatcher();
        THREAD_COUNT = Runtime.getRuntime().availableProcessors();      // Apparently, this value can change
        this.bufferBuilders = bufferBuilders != null ? bufferBuilders : new SchematicBuilderStorage(THREAD_COUNT);
        this.vanillaBufferBuilders = vanillaBufferBuilders;
        this.chunkBuilder = null;
        this.fancyGraphics = MinecraftClient.isFancyGraphicsOrBetter();
    }

    public void init()
    {
        //this.loadRenderers();
    }

    public String getChunksDebugString()
    {
        int i = this.chunkStorage.chunks.length;
        int j = this.getCompletedChunkCount();

        // FIXME later
        return String.format(Locale.ROOT, "[Schematic World] C: %d/%d %sD: %d, %s", j, i, this.mc.chunkCullingEnabled ? "(s) " : "", this.viewDistance, this.chunkBuilder == null ? "null" : this.chunkBuilder.getDebugString());
    }

    public double getRenderedChunks()
    {
        return this.chunkStorage.chunks.length;
    }

    public int getCompletedChunkCount()
    {
        int i = 0;

        for (SchematicBuiltChunk builtChunk : this.builtChunks)
        {
            if (builtChunk.getData().isEmpty())
            {
                continue;
            }

            i++;
        }

        return i;
    }

    /**
     * Fallback function
     */
    private void loadRenderers()
    {
        this.world.getProfiler().push("litematica_load_renderers");

        if (this.mc == null)
        {
            this.mc = MinecraftClient.getInstance();
        }

        this.reloadChunkBuilder(this.mc.world, this.mc.getBufferBuilders(), this.mc.getBlockRenderManager(), this.mc.getBlockEntityRenderDispatcher());

        this.world.getProfiler().pop();
    }

    /**
     * Should be called via the Mixin
     */
    public void reloadChunkBuilder(ClientWorld world, @Nonnull BufferBuilderStorage buffers, @Nullable BlockRenderManager blockRenderer, @Nullable BlockEntityRenderDispatcher blockEntityRender)
    {
        if (world == null)
        {
            return;
        }
        this.world.getProfiler().push("litematica_reload_chunk_builder");

        this.displayListEntitiesDirty = true;
        this.renderDistanceChunks = this.mc.options.getViewDistance().getValue();

        if (this.bufferBuilders == null)
        {
            this.world.getProfiler().push("allocate_builder_storage");

            THREAD_COUNT = Runtime.getRuntime().availableProcessors();      // Apparently, this value can change
            this.bufferBuilders = new SchematicBuilderStorage(THREAD_COUNT);

            this.world.getProfiler().pop();
        }
        if (this.vanillaBufferBuilders != buffers)
        {
            this.vanillaBufferBuilders = buffers;
        }
        if (this.vanillaBlockRenderManager == null)
        {
            this.vanillaBlockRenderManager = blockRenderer != null ? blockRenderer : this.mc.getBlockRenderManager();
        }
        if (this.blockRenderManager == null)
        {
            this.blockRenderManager = new SchematicBlockRenderManager(this.mc.getBlockColors(), this.vanillaBlockRenderManager);
        }
        if (this.blockEntityRenderDispatch == null)
        {
            this.blockEntityRenderDispatch = blockEntityRender != null ? blockEntityRender : this.mc.getBlockEntityRenderDispatcher();
        }
        if (this.chunkBuilder == null)
        {
            this.world.getProfiler().push("create_chunk_builder");

            this.chunkBuilder = new SchematicChunkBuilder(this.world, this, Util.getMainWorkerExecutor(), this.blockRenderManager, blockEntityRender, this.bufferBuilders.getBlockBufferBuilders(), this.bufferBuilders.getBlockBufferBuildersPool());

            this.world.getProfiler().pop();
        }
        else
        {
            this.chunkBuilder.setSchematicWorld(this.world);
        }

        this.world.getProfiler().pop();
    }

    public void setWorld(@Nullable WorldSchematic world)
    {
        this.lastCameraChunkUpdateX = Double.MIN_VALUE;
        this.lastCameraChunkUpdateY = Double.MIN_VALUE;
        this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
        //this.entityRenderDispatch.setWorld(world);
        this.world = world;

        // reloadChunkBuilder() loads the renderer, and should already have been done, but let's make sure
        if (this.chunkBuilder == null && world != null)
        {
            this.loadRenderers();
        }
        else
        {
            if (this.chunkStorage != null)
            {
                this.clearChunks();
                this.chunkStorage = null;
            }

            if (this.chunkBuilder != null)
            {
                this.chunkBuilder.stop();
                this.chunkBuilder = null;
            }
            this.builtChunks.clear();
        }
    }

    public void setFancyGraphics()
    {
        this.fancyGraphics = MinecraftClient.isFancyGraphicsOrBetter();
    }

    public void setViewDistance(int viewDistance)
    {
        this.viewDistance = viewDistance;
    }

    public int getViewDistance()
    {
        return this.viewDistance;
    }

    public void clearChunks()
    {
        if (this.chunkStorage != null)
        {
            this.chunkStorage.clear();
        }
    }

    public void resetChunkBuilder()
    {
        if (this.chunkBuilder != null)
        {
            this.chunkBuilder.reset();
        }
    }

    public @Nullable SchematicChunkBuilder getChunkBuilder()
    {
        return this.chunkBuilder;
    }

    public void clearCulling()
    {
        Set<BlockEntity> set = this.noCullingBlockEntities;

        synchronized (set)
        {
            this.noCullingBlockEntities.clear();
        }
    }

    public void createBuiltChunkStorage()
    {
        this.chunkStorage = new SchematicBuiltChunkStorage(this.chunkBuilder, this.world, this.viewDistance, this);
    }

    public void clearBuiltChunks()
    {
        this.builtChunks.clear();
    }

    public void updateCameraEntity(Entity entity)
    {
        if (entity != null)
        {
            this.chunkStorage.updateCameraPosition(entity.getX(), entity.getZ());
        }
    }

    public void updateNoCullingBlockEntities(HashSet<BlockEntity> set2, HashSet<BlockEntity> set)
    {
        // NO-OP
    }

    public void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator)
    {
        this.world.getProfiler().push("setup_terrain");

        if ((this.mc.options.getViewDistance().getValue() != this.renderDistanceChunks) ||
                (this.chunkBuilder == null || this.chunkStorage == null))
        {
            this.loadRenderers();
        }

        Entity entity = EntityUtils.getCameraEntity();

        if (entity == null)
        {
            entity = this.mc.player;
        }

        //camera.update(this.world, entity, this.mc.options.perspective > 0, this.mc.options.perspective == 2, this.mc.getTickDelta());

        this.world.getProfiler().push("camera");

        double entityX = entity.getX();
        double entityY = entity.getY();
        double entityZ = entity.getZ();
        int sectionX = ChunkSectionPos.getSectionCoord(entityX);
        int sectionY = ChunkSectionPos.getSectionCoord(entityY);
        int sectionZ = ChunkSectionPos.getSectionCoord(entityZ);
        double diffX = entityX - this.lastCameraChunkUpdateX;
        double diffY = entityY - this.lastCameraChunkUpdateY;
        double diffZ = entityZ - this.lastCameraChunkUpdateZ;

        if (diffX * diffX + diffY * diffY + diffZ * diffZ > 256.0)
        {
            this.lastCameraChunkUpdateX = entityX;
            this.lastCameraChunkUpdateY = entityY;
            this.lastCameraChunkUpdateZ = entityZ;
            //this.chunkRendererDispatcher.removeOutOfRangeRenderers();
        }

        if (this.lastCameraChunkUpdateX != sectionX || this.lastCameraChunkUpdateY != sectionY || this.lastCameraChunkUpdateZ != sectionZ)
        {
            this.lastCameraChunkUpdateX = sectionX;
            this.lastCameraChunkUpdateY = sectionY;
            this.lastCameraChunkUpdateZ = sectionZ;
            this.chunkStorage.updateCameraPosition(entityX, entityZ);
        }

        this.world.getProfiler().swap("renderlist_camera");

        Vec3d cameraVec3d = camera.getPos();
        double cameraX = cameraVec3d.x;
        double cameraY = cameraVec3d.y;
        double cameraZ = cameraVec3d.z;

        //this.renderDispatcher.setCameraPosition(cameraPos);
        this.chunkBuilder.setCameraVec3d(cameraVec3d);

        this.world.getProfiler().swap("culling");
        BlockPos viewPos = BlockPos.ofFloored(cameraX, cameraY + (double) entity.getStandingEyeHeight(), cameraZ);
        final int centerChunkX = (viewPos.getX() >> 4);
        final int centerChunkZ = (viewPos.getZ() >> 4);
        final int renderDistance = this.mc.options.getViewDistance().getValue();
        ChunkPos viewChunk = new ChunkPos(viewPos);

        this.displayListEntitiesDirty = this.displayListEntitiesDirty || this.isRenderingReady(viewPos) ||
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

        this.world.getProfiler().push("update");

        if (this.displayListEntitiesDirty)
        {
            this.world.getProfiler().push("fetch");

            this.displayListEntitiesDirty = false;
            //this.renderInfos.clear();

            this.world.getProfiler().swap("sort");
            List<ChunkPos> positions = DataManager.getSchematicPlacementManager().getAndUpdateVisibleChunks(viewChunk);

            this.world.getProfiler().swap("iteration");
            for (ChunkPos chunkPos : positions)
            {
                int cx = chunkPos.x;
                int cz = chunkPos.z;
                // Only render sub-chunks that are within the client's render distance, and that
                // have been already properly loaded on the client
                if (Math.abs(cx - centerChunkX) <= renderDistance &&
                        Math.abs(cz - centerChunkZ) <= renderDistance &&
                        this.world.getChunkProvider().isChunkLoaded(cx, cz))
                {
                    // TODO
                }
            }
        }

        this.world.getProfiler().pop();
        this.world.getProfiler().pop();
    }

    public boolean isTerrainRenderComplete()
    {
        return this.chunkBuilder.isEmpty();
    }

    public boolean isRenderingReady(BlockPos pos)
    {
        SchematicBuiltChunk chunk = this.chunkStorage.getRenderedChunk(pos);

        return chunk != null && chunk.chunkData.get() != SchematicChunkRenderData.EMPTY_1;
    }

    public void renderLayer(RenderLayer layer, double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix)
    {
        // TODO
    }

    public void renderOverlays(double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix)
    {
        for (SchematicOverlayType type : SchematicOverlayType.values())
        {
            this.renderOverlay(type, x, y, z, matrix4f, positionMatrix);
        }
    }
    public void renderOverlay(SchematicOverlayType type, double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix)
    {
        // TODO
    }

    public void renderEntities(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2)
    {
        // TODO
    }

    public void renderBlockEntities(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2)
    {
        // TODO
    }

    public void updateChunks(Camera camera, ChunkBuilderMode mode)
    {
        this.mc.getProfiler().push("schematic_populate_sections_to_compile");

        SchematicRegionBuilder regionBuilder = new SchematicRegionBuilder();
        BlockPos cameraPos = camera.getBlockPos();
        ArrayList<SchematicBuiltChunk> list = new ArrayList<>();

        for (SchematicBuiltChunk chunk : this.builtChunks)
        {
            if (!chunk.isDirty())
            {
                continue;
            }
            boolean dirty = false;

            if (mode == ChunkBuilderMode.NEARBY)
            {
                BlockPos dist = chunk.getChunkOrigin().add(8, 8, 8);
                dirty = dist.getSquaredDistance(cameraPos) < 768.0 || chunk.isDirtyImportant();
            }
            else if (mode == ChunkBuilderMode.PLAYER_AFFECTED)
            {
                dirty = chunk.isDirtyImportant();
            }
            if (dirty)
            {
                this.mc.getProfiler().push("schematic_build_near_sync");
                this.chunkBuilder.rebuildTask(chunk, regionBuilder);
                chunk.markClean();
                continue;
            }
            list.add(chunk);
        }
        this.mc.getProfiler().swap("schematic_upload");
        this.chunkBuilder.uploadTask();
        this.mc.getProfiler().swap("schematic_schedule_async_compile");
        for (SchematicBuiltChunk chunk : list)
        {
            chunk.scheduleRebuild(this.chunkBuilder, regionBuilder);
            chunk.markClean();
        }
        this.mc.getProfiler().pop();
    }
}
