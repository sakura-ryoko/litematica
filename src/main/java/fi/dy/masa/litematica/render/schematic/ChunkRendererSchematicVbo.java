package fi.dy.masa.litematica.render.schematic;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.Logger;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.chunk.WorldChunk;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.util.OverlayType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.world.WorldSchematic;

public class ChunkRendererSchematicVbo implements AutoCloseable
{
    private static final Logger LOGGER = Litematica.LOGGER;
    protected static int schematicRenderChunksUpdated;

    protected volatile WorldSchematic world;
    protected final WorldRendererSchematic worldRenderer;
    // UNTHREADED CODE
    protected final ReentrantLock chunkRenderLock;
    protected final ReentrantLock chunkRenderDataLock;
    protected final Set<BlockEntity> setBlockEntities = new HashSet<>();
    protected Profiler profiler;
    //
    //protected final AtomicReference<Set<BlockEntity>> setBlockEntities = new AtomicReference<>(new HashSet<>());
    protected final BlockPos.Mutable position;
    protected final BlockPos.Mutable chunkRelativePos;

//    protected final Map<RenderLayer, GpuBuffer> vertexBufferBlocks;
//    protected final Map<OverlayRenderType, GpuBuffer> vertexBufferOverlay;
    protected final List<IntBoundingBox> boxes = new ArrayList<>();
    protected final EnumSet<OverlayRenderType> existingOverlays = EnumSet.noneOf(OverlayRenderType.class);

    private net.minecraft.util.math.Box boundingBox;
    protected Color4f overlayColor;
    protected boolean hasOverlay = false;
    private boolean ignoreClientWorldFluids;

    protected ChunkCacheSchematic schematicWorldView;
    protected ChunkCacheSchematic clientWorldView;

    private final BufferBuilderCache builderCache;
    private final GpuBufferCache gpuBufferCache;

    /*  THREADED CODE
    protected AtomicReference<ChunkRenderTaskSchematic> compileTask = new AtomicReference<>(null);
    protected AtomicReference<ChunkRenderDataSchematic> chunkRenderData = new AtomicReference<>(ChunkRenderDataSchematic.EMPTY);
     */
    protected ChunkRenderTaskSchematic compileTask;
    protected ChunkRenderDataSchematic chunkRenderData;

    private boolean needsUpdate;
    private boolean needsImmediateUpdate;

    protected ChunkRendererSchematicVbo(WorldSchematic world, WorldRendererSchematic worldRenderer)
    {
        this.world = world;
        this.worldRenderer = worldRenderer;
        this.chunkRenderData = ChunkRenderDataSchematic.EMPTY;
        this.chunkRenderLock = new ReentrantLock();
        this.chunkRenderDataLock = new ReentrantLock();
//        this.vertexBufferBlocks = new IdentityHashMap<>();
//        this.vertexBufferOverlay = new IdentityHashMap<>();
        this.position = new BlockPos.Mutable();
        this.chunkRelativePos = new BlockPos.Mutable();
        this.builderCache = new BufferBuilderCache();
        this.gpuBufferCache = new GpuBufferCache();
    }

    public boolean hasOverlay()
    {
        return this.hasOverlay;
    }

    protected Profiler getProfiler()
    {
        if (this.profiler == null)
        {
            this.profiler = this.worldRenderer.getProfiler();
        }

        return this.profiler;
    }

    public EnumSet<OverlayRenderType> getOverlayTypes()
    {
        return this.existingOverlays;
    }

    // fixme
//    protected VertexBuffer getBlocksVertexBufferByLayer(RenderLayer layer)
//    {
//        return this.vertexBufferBlocks.computeIfAbsent(layer, l -> new VertexBuffer(GlUsage.STATIC_WRITE));
//    }
//
//    protected VertexBuffer getOverlayVertexBuffer(OverlayRenderType type)
//    {
//        //if (GuiBase.isCtrlDown()) System.out.printf("getOverlayVertexBuffer: type: %s, buf: %s\n", type, this.vertexBufferOverlay[type.ordinal()]);
//        return this.vertexBufferOverlay.computeIfAbsent(type, l -> new VertexBuffer(GlUsage.STATIC_WRITE));
//    }

    protected @Nullable ChunkRenderObjectBuffers getBlockBuffersByLayer(RenderLayer layer)
    {
        if (this.gpuBufferCache.hasBuffersByLayer(layer))
        {
            return this.gpuBufferCache.getBuffersByLayer(layer);
        }

        return null;
    }

    protected @Nullable ChunkRenderObjectBuffers getOverlayBuffersByType(OverlayRenderType type)
    {
        if (this.gpuBufferCache.hasBuffersByType(type))
        {
            return this.gpuBufferCache.getBuffersByType(type);
        }

        return null;
    }

    protected ChunkRenderDataSchematic getChunkRenderData()
    {
        // Threaded code
        //return this.chunkRenderData.get();
        return this.chunkRenderData;
    }

    protected BufferBuilderCache getBuilderCache()
    {
        return this.builderCache;
    }

    protected GpuBufferCache getGpuBufferCache()
    {
        return this.gpuBufferCache;
    }

    protected void setChunkRenderData(ChunkRenderDataSchematic data)
    {
        this.chunkRenderDataLock.lock();

        try
        {
            this.chunkRenderData = data;
        }
        finally
        {
            this.chunkRenderDataLock.unlock();
        }
        // Threaded Code
        //this.chunkRenderData.set(data);
    }

    public BlockPos getOrigin()
    {
        return this.position;
    }

    public net.minecraft.util.math.Box getBoundingBox()
    {
        if (this.boundingBox == null)
        {
            int x = this.position.getX();
            int y = this.position.getY();
            int z = this.position.getZ();
            this.boundingBox = new net.minecraft.util.math.Box(x, y, z, x + 16, y + this.world.getHeight(), z + 16);
        }

        return this.boundingBox;
    }

    protected void setPosition(int x, int y, int z)
    {
        if (x != this.position.getX() ||
            y != this.position.getY() ||
            z != this.position.getZ())
        {
            this.clear();
            this.boundingBox = null;
            this.position.set(x, y, z);
        }
    }

    protected double getDistanceSq()
    {
        Entity entity = EntityUtils.getCameraEntity();

        if (entity == null) return 0;

        double x = this.position.getX() + 8.0D - entity.getX();
        double z = this.position.getZ() + 8.0D - entity.getZ();

        return x * x + z * z;
    }

    protected void deleteGlResources()
    {
        this.clear();
        this.closeAllVertexBuffers();
        //this.world = null;
    }

    private void closeAllVertexBuffers()
    {
//        this.vertexBufferBlocks.values().forEach(VertexBuffer::close);
//        this.vertexBufferOverlay.values().forEach(VertexBuffer::close);
//        this.vertexBufferBlocks.clear();
//        this.vertexBufferOverlay.clear();

        this.gpuBufferCache.clearAll();
    }

    protected void resortTransparency(ChunkRenderTaskSchematic task, Profiler profiler)
    {
        this.profiler = profiler;
        this.getProfiler().push("resort_task");
        ChunkRenderDataSchematic data = task.getChunkRenderData();
        Vec3d cameraPos = task.getCameraPosSupplier().get();
        RenderLayer layerTranslucent = RenderLayer.getTranslucent();
        BufferAllocatorCache allocators = task.getAllocatorCache();

        float x = (float) cameraPos.x - this.position.getX();
        float y = (float) cameraPos.y - this.position.getY();
        float z = (float) cameraPos.z - this.position.getZ();

        if (!data.isBlockLayerEmpty(layerTranslucent))
        {
            this.getProfiler().swap("resort_blocks");
            //RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_TRANSLUCENT);

            if (data.getBuiltBufferCache().hasBuiltBufferByLayer(layerTranslucent))
            {
                try
                {
                    this.resortRenderBlocks(layerTranslucent, x, y, z, data, allocators);
                }
                catch (Exception e)
                {
                    LOGGER.error("resortTransparency() [VBO] caught exception for layer [{}] // {}", ChunkRenderLayers.getFriendlyName(layerTranslucent), e.toString());
                }
            }
        }

        //if (GuiBase.isCtrlDown()) System.out.printf("resortTransparency\n");
        //if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue())

        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_RESORTING.getBooleanValue())
        {
            this.getProfiler().swap("resort_overlay");
            OverlayRenderType type = OverlayRenderType.QUAD;

            if (!data.isOverlayTypeEmpty(type))
            {
                if (data.getBuiltBufferCache().hasBuiltBufferByType(type))
                {
                    try
                    {
                        this.resortRenderOverlay(type, x, y, z, data, allocators);
                    }
                    catch (Exception e)
                    {
                        LOGGER.error("resortTransparency() [VBO] caught exception for overlay type [{}] // {}", type.getDrawMode().name(), e.toString());
                    }
                }
            }
        }

        this.getProfiler().pop();
        this.profiler = null;
    }

    protected void rebuildChunk(ChunkRenderTaskSchematic task, Profiler profiler)
    {
        this.profiler = profiler;
        this.getProfiler().push("rebuild_chunk");
        ChunkRenderDataSchematic data = new ChunkRenderDataSchematic();
        //task.setChunkRenderData(data);
        task.getLock().lock();

        try
        {
            if (task.getStatus() != ChunkRenderTaskSchematic.Status.COMPILING)
            {
                return;
            }

            task.setChunkRenderData(data);
        }
        finally
        {
            task.getLock().unlock();
        }

        this.builderCache.clearAll();
        this.gpuBufferCache.clearAll();

        LOGGER.warn("[VBO] rebuildChunk() pos [{}]", this.position.toShortString());

        Set<BlockEntity> tileEntities = new HashSet<>();
        BlockPos posChunk = this.position;
        LayerRange range = DataManager.getRenderLayerRange();
        BufferAllocatorCache allocators = task.getAllocatorCache();

        this.existingOverlays.clear();
        this.hasOverlay = false;

        this.getProfiler().swap("rebuild_chunk_start");
        
        synchronized (this.boxes)
        {
            int minX = posChunk.getX();
            int minY = posChunk.getY();
            int minZ = posChunk.getZ();
            int maxX = minX + 15;
            int maxY = minY + this.world.getHeight();
            int maxZ = minZ + 15;

            if (!this.boxes.isEmpty() &&
                (!this.schematicWorldView.isEmpty() || !this.clientWorldView.isEmpty()) &&
                 range.intersectsBox(minX, minY, minZ, maxX, maxY, maxZ))
            {
                ++schematicRenderChunksUpdated;

                Vec3d cameraPos = task.getCameraPosSupplier().get();
                float x = (float) cameraPos.x - this.position.getX();
                float y = (float) cameraPos.y - this.position.getY();
                float z = (float) cameraPos.z - this.position.getZ();
                Set<RenderLayer> usedLayers = new HashSet<>();
                MatrixStack matrixStack = new MatrixStack();
                // TODO --> Do we need to change this to a Matrix4f in the future?
                int bottomY = this.position.getY();

                this.getProfiler().swap("rebuild_chunk_boxes");
                for (IntBoundingBox box : this.boxes)
                {
                    box = range.getClampedRenderBoundingBox(box);

                    // The rendered layer(s) don't intersect this sub-volume
                    if (box == null)
                    {
                        continue;
                    }

                    BlockPos posFrom = new BlockPos(box.minX, box.minY, box.minZ);
                    BlockPos posTo   = new BlockPos(box.maxX, box.maxY, box.maxZ);

                    for (BlockPos posMutable : BlockPos.Mutable.iterate(posFrom, posTo))
                    {
                        // Fluid models and the overlay use the VertexConsumer#vertex(x, y, z) method.
                        // Fluid rendering and the overlay do not use the MatrixStack.
                        // Block models use the VertexConsumer#quad() method, and they use the MatrixStack.
                        matrixStack.push();
                        matrixStack.translate(posMutable.getX() & 0xF, posMutable.getY() - bottomY, posMutable.getZ() & 0xF);

                        this.renderBlocksAndOverlay(posMutable, data, allocators, tileEntities, usedLayers, matrixStack);

                        matrixStack.pop();
                    }
                }

                this.getProfiler().swap("rebuild_chunk_layers");
                for (RenderLayer layerTmp : ChunkRenderLayers.LAYERS)
                {
                    if (usedLayers.contains(layerTmp))
                    {
                        data.setBlockLayerUsed(layerTmp);
                    }

                    if (data.isBlockLayerStarted(layerTmp))
                    {
                        try
                        {
                            data.setBlockLayerUsed(layerTmp);
                            this.postRenderBlocks(layerTmp, x, y, z, data, allocators);
                        }
                        catch (Exception e)
                        {
                            LOGGER.error("rebuildChunk() [VBO] failed to postRenderBlocks() for layer [{}] --> {}", ChunkRenderLayers.getFriendlyName(layerTmp), e.toString());
                        }
                    }
                }

                if (this.hasOverlay)
                {
                    this.getProfiler().swap("rebuild_chunk_overlays");
                    //if (GuiBase.isCtrlDown()) System.out.printf("postRenderOverlays\n");
                    for (OverlayRenderType type : this.existingOverlays)
                    {
                        if (data.isOverlayTypeStarted(type))
                        {
                            try
                            {
                                data.setOverlayTypeUsed(type);
                                this.postRenderOverlay(type, x, y, z, data, allocators);
                            }
                            catch (Exception e)
                            {
                                LOGGER.error("rebuildChunk() [VBO] failed to postRenderOverlay() for overlay type [{}] --> {}", type.getDrawMode().name(), e.toString());
                            }
                        }
                    }
                }
            }
        }

        this.getProfiler().swap("rebuild_chunk_lock");
        this.chunkRenderLock.lock();

        try
        {
            Set<BlockEntity> set = Sets.newHashSet(tileEntities);
            Set<BlockEntity> set1 = Sets.newHashSet(this.setBlockEntities);
            set.removeAll(this.setBlockEntities);
            set1.removeAll(tileEntities);
            this.setBlockEntities.clear();
            this.setBlockEntities.addAll(tileEntities);
            this.worldRenderer.updateBlockEntities(set1, set);
            this.builderCache.clearAll();
        }
        finally
        {
            this.chunkRenderLock.unlock();
        }

        /*
        Set<BlockEntity> removed = this.setBlockEntities.getAndSet(tileEntities);
        Set<BlockEntity> added = Sets.newHashSet(tileEntities);
        added.removeAll(removed);
        removed.removeAll(tileEntities);

        Threaded Code

        synchronized (this.builderCache)
        {
            // probably not necessary to do block Entity update this with builderCache locked but doing so out of caution.
            this.worldRenderer.updateBlockEntities(removed, added);
            this.builderCache.clearAll();
        }
         */

        this.getProfiler().pop();
        this.profiler = null;
        data.setTimeBuilt(this.world.getTime());
    }

    protected void renderBlocksAndOverlay(BlockPos pos, @Nonnull ChunkRenderDataSchematic data, @Nonnull BufferAllocatorCache allocators, Set<BlockEntity> tileEntities,
                                          Set<RenderLayer> usedLayers, MatrixStack matrixStack)
    {
        BlockState stateSchematic = this.schematicWorldView.getBlockState(pos);
        BlockState stateClient    = this.clientWorldView.getBlockState(pos);
        boolean clientHasAir = stateClient.isAir();
        boolean schematicHasAir = stateSchematic.isAir();
        boolean missing = false;

        if (clientHasAir && schematicHasAir)
        {
            return;
        }

        LOGGER.warn("[VBO] renderBlocksAndOverlay() pos [{}]", pos.toShortString());
        this.getProfiler().push("render_build");
        this.overlayColor = null;

        // Schematic has a block, client has air
        if (clientHasAir || (stateSchematic != stateClient && Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue()))
        {
            if (stateSchematic.hasBlockEntity())
            {
                this.addBlockEntity(pos, data, tileEntities);
            }

            boolean translucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
            // TODO change when the fluids become separate
            FluidState fluidState = stateSchematic.getFluidState();

            if (!fluidState.isEmpty() &&
                Configs.Visuals.ENABLE_SCHEMATIC_FLUIDS.getBooleanValue())
            {
                this.getProfiler().swap("render_build_fluids");
                RenderLayer layer = RenderLayers.getFluidLayer(fluidState);
                int offsetY = ((pos.getY() >> 4) << 4) - this.position.getY();
                BufferBuilder bufferSchematic = this.builderCache.getBufferByLayer(layer, allocators);

                if (!data.isBlockLayerStarted(layer) || bufferSchematic == null)
                {
                    data.setBlockLayerStarted(layer);
                    bufferSchematic = this.preRenderBlocks(layer, allocators);
                }

                ((IBufferBuilderPatch) bufferSchematic).litematica$setOffsetY(offsetY);

                this.worldRenderer.renderFluid(this.schematicWorldView, stateSchematic, fluidState, pos, bufferSchematic);
                usedLayers.add(layer);
                ((IBufferBuilderPatch) bufferSchematic).litematica$setOffsetY(0.0F);
            }

            if (stateSchematic.getRenderType() != BlockRenderType.INVISIBLE)
            {
                this.getProfiler().swap("render_build_blocks");
                RenderLayer layer = translucent ? RenderLayer.getTranslucent() : RenderLayers.getBlockLayer(stateSchematic);
                BufferBuilder bufferSchematic = this.builderCache.getBufferByLayer(layer, allocators);

                if (!data.isBlockLayerStarted(layer) || bufferSchematic == null)
                {
                    data.setBlockLayerStarted(layer);
                    bufferSchematic = this.preRenderBlocks(layer, allocators);
                }

                if (this.worldRenderer.renderBlock(this.schematicWorldView, stateSchematic, pos, matrixStack, bufferSchematic))
                {
                    usedLayers.add(layer);
                }

                if (clientHasAir)
                {
                    missing = true;
                }
            }
        }

        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue())
        {
            this.getProfiler().swap("render_build_overlays");
            OverlayType type = this.getOverlayType(stateSchematic, stateClient);

            this.overlayColor = getOverlayColor(type);

            if (this.overlayColor != null)
            {
                if (!stateSchematic.getFluidState().isEmpty() &&
                    !Configs.Visuals.ENABLE_SCHEMATIC_FLUIDS.getBooleanValue())
                {
                    this.getProfiler().pop();
                    return;
                }

                this.renderOverlay(type, pos, stateSchematic, missing, data, allocators);
            }
        }

        this.getProfiler().pop();
    }

    protected void renderOverlay(OverlayType type, BlockPos pos, BlockState stateSchematic, boolean missing, @Nonnull ChunkRenderDataSchematic data, @Nonnull BufferAllocatorCache allocators)
    {
        this.getProfiler().push("render_overlay");
        boolean useDefault = false;

        //RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BlockPos.Mutable relPos = this.getChunkRelativePosition(pos);
        OverlayRenderType overlayType;

        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES.getBooleanValue())
        {
            this.getProfiler().push("overlay_sides");
            overlayType = OverlayRenderType.QUAD;
            BufferBuilder bufferOverlayQuads = this.builderCache.getBufferByOverlay(overlayType, allocators);

            if (!data.isOverlayTypeStarted(overlayType) || bufferOverlayQuads == null)
            {
                data.setOverlayTypeStarted(overlayType);
                bufferOverlayQuads = this.preRenderOverlay(overlayType, allocators);
            }

            if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue())
            {
                this.getProfiler().swap("cull_inner_sides");
                BlockPos.Mutable posMutable = new BlockPos.Mutable();
                List<BlockModelPart> modelParts = this.worldRenderer.getModelParts(relPos, stateSchematic, Random.create());

                for (int i = 0; i < 6; ++i)
                {
                    Direction side = fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS[i];
                    posMutable.set(pos.getX() + side.getOffsetX(), pos.getY() + side.getOffsetY(), pos.getZ() + side.getOffsetZ());
                    BlockState adjStateSchematic = this.schematicWorldView.getBlockState(posMutable);
                    BlockState adjStateClient    = this.clientWorldView.getBlockState(posMutable);
                    OverlayType typeAdj = getOverlayType(adjStateSchematic, adjStateClient);

                    // Only render the model-based outlines or sides for missing blocks
                    if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
                    {
                        this.getProfiler().swap("cull_render_model_sides");

                        if (type.getRenderPriority() > typeAdj.getRenderPriority() ||
                            !Block.isFaceFullSquare(stateSchematic.getCollisionShape(this.schematicWorldView, pos), side))
                        {
                            this.getProfiler().swap("cull_render_model");

                            for (BlockModelPart part : modelParts)
                            {
                                final int light = WorldRenderer.getLightmapCoordinates(this.schematicWorldView, relPos);
                                RenderUtils.drawBlockModelQuadOverlayBatched(part, stateSchematic, relPos, side, this.overlayColor, 0, bufferOverlayQuads);
                            }
                        }
                    }
                    else if (type.getRenderPriority() > typeAdj.getRenderPriority())
                    {
                        this.getProfiler().swap("cull_render_default");
                        RenderUtils.drawBlockBoxSideBatchedQuads(relPos, side, this.overlayColor, 0, bufferOverlayQuads);
                    }
                }
            }
            else
            {
                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
                {
                    this.getProfiler().swap("render_model_sides");
                    List<BlockModelPart> modelParts = this.worldRenderer.getModelParts(relPos, stateSchematic, Random.create());

//                    this.getProfiler().swap("render_model");
                    RenderUtils.drawBlockModelQuadOverlayBatched(modelParts, stateSchematic, relPos, this.overlayColor, 0, bufferOverlayQuads);
                }
                else
                {
                    this.getProfiler().swap("render_batched");
//                    fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxSidesBatchedQuads(relPos, this.overlayColor, 0, bufferOverlayQuads);
                    RenderUtils.drawBlockBoxBatchedQuads(relPos, this.overlayColor, 0, bufferOverlayQuads);
                }
            }

            this.getProfiler().pop();
        }

        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES.getBooleanValue())
        {
            this.getProfiler().push("overlay_outlines");
            useDefault = false;
            overlayType = OverlayRenderType.OUTLINE;
            BufferBuilder bufferOverlayOutlines = this.builderCache.getBufferByOverlay(overlayType, allocators);

            if (!data.isOverlayTypeStarted(overlayType) || bufferOverlayOutlines == null)
            {
                data.setOverlayTypeStarted(overlayType);
                bufferOverlayOutlines = this.preRenderOverlay(overlayType, allocators);
            }

            Color4f overlayColor = new Color4f(this.overlayColor.r, this.overlayColor.g, this.overlayColor.b, 1f);

            this.getProfiler().swap("cull_inner_sides");
            if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue())
            {
                OverlayType[][][] adjTypes = new OverlayType[3][3][3];
                BlockPos.Mutable posMutable = new BlockPos.Mutable();

                for (int y = 0; y <= 2; ++y)
                {
                    for (int z = 0; z <= 2; ++z)
                    {
                        for (int x = 0; x <= 2; ++x)
                        {
                            if (x != 1 || y != 1 || z != 1)
                            {
                                posMutable.set(pos.getX() + x - 1, pos.getY() + y - 1, pos.getZ() + z - 1);
                                BlockState adjStateSchematic = this.schematicWorldView.getBlockState(posMutable);
                                BlockState adjStateClient    = this.clientWorldView.getBlockState(posMutable);
                                adjTypes[x][y][z] = this.getOverlayType(adjStateSchematic, adjStateClient);
                            }
                            else
                            {
                                adjTypes[x][y][z] = type;
                            }
                        }
                    }
                }

                //this.getProfiler().swap("cull");
                /*
                for (int i = 0; i < 6; ++i)
                {
                    Direction side = fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS[i];
                    posMutable.set(pos.getX() + side.getOffsetX(), pos.getY() + side.getOffsetY(), pos.getZ() + side.getOffsetZ());
                    BlockState adjStateSchematic = this.schematicWorldView.getBlockState(posMutable);
                    BlockState adjStateClient = this.clientWorldView.getBlockState(posMutable);
                    OverlayType typeAdj = this.getOverlayType(adjStateSchematic, adjStateClient);
                 */

                // FIXME --> this is quite broken / laggy (Why?)
                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue())
                {
                    // FIXME: how to implement this correctly here... >_>
                    if (stateSchematic.isOpaque())
                    {
                        useDefault = true;
                    }
                    else
                    {
//                        this.getProfiler().swap("model");
                        /*
                        if (type.getRenderPriority() > typeAdj.getRenderPriority())
                        {
                         */

                        this.getProfiler().swap("render_model_batched");
                        List<BlockModelPart> modelParts = this.worldRenderer.getModelParts(relPos, stateSchematic, Random.create());
                        //RenderUtils.renderModelQuadOutlines(bakedModel, stateSchematic, relPos, side, overlayColor, 0, bufferOverlayOutlines);
                        RenderUtils.drawDebugBlockModelOutlinesBatched(modelParts, stateSchematic, relPos, overlayColor, 0, bufferOverlayOutlines);
                    }
                }
                else
                {
                    this.getProfiler().swap("render_reduced_edges");
                    this.renderOverlayReducedEdges(pos, adjTypes, type, bufferOverlayOutlines);
                    //RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(pos, relPos, overlayColor, 0, bufferOverlayOutlines);
                }
            }
            else
            {
                this.getProfiler().swap("render_fallback");
                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue())
                {
                    this.getProfiler().swap("render_model_batched");
                    List<BlockModelPart> modelParts = this.worldRenderer.getModelParts(relPos, stateSchematic, Random.create());
                    RenderUtils.drawDebugBlockModelOutlinesBatched(modelParts, stateSchematic, relPos, overlayColor, 0, bufferOverlayOutlines);
                }
                else { useDefault = true; }
            }

            if (useDefault)
            {
                try
                {
                    this.getProfiler().swap("render_batched_box");
//                    fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(relPos, overlayColor, 0, bufferOverlayOutlines, matrices.peek());
                    RenderUtils.drawBlockBoundingBoxOutlinesBatchedDebugLines(relPos, overlayColor, 0, bufferOverlayOutlines);
                }

                catch (Exception ignored) { }
            }

            this.getProfiler().pop();
        }

        this.getProfiler().pop();
    }

    protected BlockPos.Mutable getChunkRelativePosition(BlockPos pos)
    {
        return this.chunkRelativePos.set(pos.getX() & 0xF, pos.getY() - this.position.getY(), pos.getZ() & 0xF);
    }

    protected void renderOverlayReducedEdges(BlockPos pos, OverlayType[][][] adjTypes, OverlayType typeSelf, BufferBuilder bufferOverlayOutlines)
    {
        OverlayType[] neighborTypes = new OverlayType[4];
        Vec3i[] neighborPositions = new Vec3i[4];
        int lines = 0;

        this.getProfiler().push("overlay_reduced_edges");
        for (Direction.Axis axis : PositionUtils.AXES_ALL)
        {
            for (int corner = 0; corner < 4; ++corner)
            {
                Vec3i[] offsets = PositionUtils.getEdgeNeighborOffsets(axis, corner);
                int index = -1;
                boolean hasCurrent = false;

                if (offsets == null)
                {
                    continue;
                }
                // Find the position(s) around a given edge line that have the shared greatest rendering priority
                for (int i = 0; i < 4; ++i)
                {
                    Vec3i offset = offsets[i];
                    OverlayType type = adjTypes[offset.getX() + 1][offset.getY() + 1][offset.getZ() + 1];

                    // type NONE
                    if (type == OverlayType.NONE)
                    {
                        continue;
                    }

                    // First entry, or sharing at least the current highest found priority
                    if (index == -1 || type.getRenderPriority() >= neighborTypes[index - 1].getRenderPriority())
                    {
                        // Actually a new highest priority, add it as the first entry and rewind the index
                        if (index < 0 || type.getRenderPriority() > neighborTypes[index - 1].getRenderPriority())
                        {
                            index = 0;
                        }
                        // else: Same priority as a previous entry, append this position

                        //System.out.printf("plop 0 axis: %s, corner: %d, i: %d, index: %d, type: %s\n", axis, corner, i, index, type);
                        neighborPositions[index] = new Vec3i(pos.getX() + offset.getX(), pos.getY() + offset.getY(), pos.getZ() + offset.getZ());
                        neighborTypes[index] = type;
                        // The self position is the first (offset = [0, 0, 0]) in the arrays
                        hasCurrent |= (i == 0);
                        ++index;
                    }
                }

                this.getProfiler().swap("edges_plop");
                //System.out.printf("plop 1 index: %d, pos: %s\n", index, pos);
                // Found something to render, and the current block is among the highest priority for this edge
                if (index > 0 && hasCurrent)
                {
                    Vec3i posTmp = new Vec3i(pos.getX(), pos.getY(), pos.getZ());
                    int ind = -1;

                    for (int i = 0; i < index; ++i)
                    {
                        Vec3i tmp = neighborPositions[i];
                        //System.out.printf("posTmp: %s, tmp: %s\n", posTmp, tmp);

                        // Just prioritize the position to render a shared highest priority edge by the coordinates
                        if (tmp.getX() <= posTmp.getX() && tmp.getY() <= posTmp.getY() && tmp.getZ() <= posTmp.getZ())
                        {
                            posTmp = tmp;
                            ind = i;
                        }
                    }

                    // The current position is the one that should render this edge
                    if (posTmp.getX() == pos.getX() && posTmp.getY() == pos.getY() && posTmp.getZ() == pos.getZ())
                    {
                        //System.out.printf("plop 2 index: %d, ind: %d, pos: %s, off: %s\n", index, ind, pos, posTmp);
                        try
                        {
                            this.getProfiler().swap("render_batched");
                            RenderUtils.drawBlockBoxEdgeBatchedDebugLines(this.getChunkRelativePosition(pos), axis, corner, this.overlayColor, bufferOverlayOutlines);
                        }
                        catch (IllegalStateException ignored)
                        {
                            this.getProfiler().pop();
                            return;
                        }

                        lines++;
                    }
                }
            }
        }

        this.getProfiler().pop();
        //System.out.printf("typeSelf: %s, pos: %s, lines: %d\n", typeSelf, pos, lines);
    }

    @SuppressWarnings("deprecation")
    protected OverlayType getOverlayType(BlockState stateSchematic, BlockState stateClient)
    {
        if (stateSchematic == stateClient)
        {
            return OverlayType.NONE;
        }
        else
        {
            boolean clientHasAir = stateClient.isAir();
            boolean schematicHasAir = stateSchematic.isAir();

            // TODO --> Maybe someday Mojang will add something to replace isLiquid(), and isSolid()
            if (schematicHasAir)
            {
                return (clientHasAir || (this.ignoreClientWorldFluids && stateClient.isLiquid())) ? OverlayType.NONE : OverlayType.EXTRA;
            }
            else
            {
                if (clientHasAir || (this.ignoreClientWorldFluids && stateClient.isLiquid()))
                {
                    return OverlayType.MISSING;
                }
                // Wrong block
                else if (stateSchematic.getBlock() != stateClient.getBlock())
                {
                    if (Configs.Generic.ENABLE_DIFFERENT_BLOCKS.getBooleanValue() &&
                        BlockUtils.isInSameGroup(stateSchematic, stateClient))
                    {
                        if (BlockUtils.matchPropertiesOnly(stateSchematic, stateClient))
                        {
                            // Different block of a common BlockTags Group, and same state
                            return OverlayType.DIFF_BLOCK;
                        }
                        else
                        {
                            return OverlayType.WRONG_STATE;
                        }
                    }

                    return OverlayType.WRONG_BLOCK;
                }
                // Wrong state
                else
                {
                    return OverlayType.WRONG_STATE;
                }
            }
        }
    }

    @Nullable
    protected static Color4f getOverlayColor(OverlayType overlayType)
    {
        Color4f overlayColor = null;

        switch (overlayType)
        {
            case MISSING:
                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_MISSING.getBooleanValue())
                {
                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_MISSING.getColor();
                }
                break;
            case EXTRA:
                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_EXTRA.getBooleanValue())
                {
                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_EXTRA.getColor();
                }
                break;
            case WRONG_BLOCK:
                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK.getBooleanValue())
                {
                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK.getColor();
                }
                break;
            case WRONG_STATE:
                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_STATE.getBooleanValue())
                {
                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_STATE.getColor();
                }
                break;
            case DIFF_BLOCK:
                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_DIFF_BLOCK.getBooleanValue())
                {
                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_DIFF_BLOCK.getColor();
                }
                break;
            default:
        }

        return overlayColor;
    }

    private void addBlockEntity(BlockPos pos, ChunkRenderDataSchematic chunkRenderData, Set<BlockEntity> blockEntities)
    {
        BlockEntity te = this.schematicWorldView.getBlockEntity(pos, WorldChunk.CreationType.CHECK);

        if (te != null)
        {
            BlockEntityRenderer<BlockEntity> tesr = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(te);

            if (tesr != null)
            {
                chunkRenderData.addBlockEntity(te);

                if (tesr.rendersOutsideBoundingBox(te))
                {
                    blockEntities.add(te);
                }
            }
        }
    }

    private BufferBuilder preRenderBlocks(RenderLayer layer, @Nonnull BufferAllocatorCache allocators)
    {
        return this.builderCache.getBufferByLayer(layer, allocators);
    }

    private BufferBuilder preRenderOverlay(OverlayRenderType type, @Nonnull BufferAllocatorCache allocators)
    {
        this.existingOverlays.add(type);
        this.hasOverlay = true;

        return this.builderCache.getBufferByOverlay(type, allocators);
    }

    protected void uploadBuffersByLayer(RenderLayer layer, @Nonnull BuiltBuffer meshData)
    {
        LOGGER.warn("[VBO] uploadBuffersByLayer() Layer [{}], IndexCount [{}]", ChunkRenderLayers.getFriendlyName(layer), meshData.getDrawParameters().indexCount());
        ChunkRenderObjectBuffers gpuBuffers = this.gpuBufferCache.getBuffersByLayer(layer);

        if (gpuBuffers != null)
        {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

            if (gpuBuffers.vertexBuffer.size() < meshData.getBuffer().remaining())
            {
//                LOGGER.warn("[VBO] uploadBuffersByLayer() Layer [{}], --> RESIZE / NEW BUFFER", ChunkRenderLayers.getFriendlyName(layer));
                gpuBuffers.vertexBuffer.close();
                gpuBuffers.setVertexBuffer(RenderSystem.getDevice()
                                                       .createBuffer(() -> "VertexBuffer: " + gpuBuffers.getName() + " VBO Section: [" + this.chunkRelativePos.toShortString() + "]", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.getBuffer())
                );
            }
            else if (!gpuBuffers.vertexBuffer.isClosed())
            {
//                LOGGER.warn("[VBO] uploadBuffersByLayer() Layer [{}], --> WRITE BUFFER", ChunkRenderLayers.getFriendlyName(layer));
                encoder.writeToBuffer(gpuBuffers.vertexBuffer, meshData.getBuffer(), 0);
            }

            // Resorting
            if (meshData.getSortedBuffer() != null)
            {
//                LOGGER.warn("[VBO] uploadBuffersByLayer() Layer [{}], RESORTING", ChunkRenderLayers.getFriendlyName(layer));

                if (gpuBuffers.indexBuffer != null && gpuBuffers.indexBuffer.size() >= meshData.getSortedBuffer().remaining())
                {
                    if (!gpuBuffers.indexBuffer.isClosed())
                    {
//                        LOGGER.warn("[VBO] uploadBuffersByLayer() Layer [{}], RESORTING --> WRITE BUFFER", ChunkRenderLayers.getFriendlyName(layer));
                        encoder.writeToBuffer(gpuBuffers.indexBuffer, meshData.getSortedBuffer(), 0);
                    }
                }
                else
                {
                    if (gpuBuffers.indexBuffer != null)
                    {
                        gpuBuffers.indexBuffer.close();
                    }

//                    LOGGER.warn("[VBO] uploadBuffersByLayer() Layer [{}], RESORTING --> CREATE/SET INDEX BUFFER", ChunkRenderLayers.getFriendlyName(layer));
                    gpuBuffers.setIndexBuffer(RenderSystem.getDevice()
                                                          .createBuffer(() -> "SortedBuffer: " + gpuBuffers.getName() + " VBO Section: [" + this.chunkRelativePos.toShortString() + "]", BufferType.INDICES, BufferUsage.STATIC_WRITE, meshData.getSortedBuffer())
                    );
                }
            }
            else if (gpuBuffers.indexBuffer != null)
            {
                gpuBuffers.indexBuffer.close();
//                LOGGER.warn("[VBO] uploadBuffersByLayer() Layer [{}], ELSE --> CLEAR INDEX BUFFER", ChunkRenderLayers.getFriendlyName(layer));
                gpuBuffers.setIndexBuffer(null);
            }

//            LOGGER.warn("[VBO] uploadBuffersByLayer() Layer [{}], INDEX COUNT/TYPE --> SAVE", ChunkRenderLayers.getFriendlyName(layer));
            gpuBuffers.setIndexCount(meshData.getDrawParameters().indexCount());
            gpuBuffers.setIndexType(meshData.getDrawParameters().indexType());
//            this.gpuBufferCache.storeBuffersByLayer(layer, gpuBuffers);
        }
        else
        {
            Supplier<String> name = () -> ChunkRenderLayers.getFriendlyName(layer);
//            LOGGER.warn("[VBO] uploadBuffersByLayer() Layer [{}], NEW VERTEX BUFFER", ChunkRenderLayers.getFriendlyName(layer));
            GpuBuffer vertexBuffer = RenderSystem.getDevice()
                                                 .createBuffer(() -> "VertexBuffer: " + name.get() + " VBO Section: [" + this.chunkRelativePos.toShortString() + "]", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.getBuffer()
                                                 );
            GpuBuffer indexBuffer = meshData.getSortedBuffer() != null ? RenderSystem.getDevice()
                                                                                     .createBuffer(() -> "IndexBuffer: " + name.get() + " VBO Section: [" + this.chunkRelativePos.toShortString() + "]", BufferType.INDICES, BufferUsage.STATIC_WRITE, meshData.getSortedBuffer()
                                                                                     ) : null;

//            LOGGER.warn("[VBO] uploadBuffersByLayer() Layer [{}], NEW VERTEX BUFFER --> SAVE", ChunkRenderLayers.getFriendlyName(layer));
            this.gpuBufferCache.storeBuffersByLayer(layer,
                                                    new ChunkRenderObjectBuffers(name, vertexBuffer, indexBuffer,
                                                                                 meshData.getDrawParameters().indexCount(),
                                                                                 meshData.getDrawParameters().indexType())
            );
        }

        LOGGER.warn("[VBO] uploadBuffersByLayer() Layer [{}], END", ChunkRenderLayers.getFriendlyName(layer));
//        meshData.close();
    }

    protected void uploadBuffersByType(OverlayRenderType type, @Nonnull BuiltBuffer meshData)
    {
        LOGGER.warn("[VBO] uploadBuffersByType() Overlay [{}], IndexCount [{}]", type.name(), meshData.getDrawParameters().indexCount());
        ChunkRenderObjectBuffers gpuBuffers = this.gpuBufferCache.getBuffersByType(type);

        if (gpuBuffers != null)
        {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

            if (gpuBuffers.vertexBuffer.size() < meshData.getBuffer().remaining())
            {
//                LOGGER.warn("[VBO] uploadBuffersByType() Overlay [{}], --> RESIZE / NEW BUFFER", type.name());
                gpuBuffers.vertexBuffer.close();
                gpuBuffers.setVertexBuffer(RenderSystem.getDevice()
                                                       .createBuffer(() -> "VertexBuffer: Overlay/" + gpuBuffers.getName() + " VBO Section: [" + this.chunkRelativePos.toShortString() + "]", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.getBuffer())
                );
            }
            else if (!gpuBuffers.vertexBuffer.isClosed())
            {
//                LOGGER.warn("[VBO] uploadBuffersByType() Overlay [{}], --> WRITE BUFFER", type.name());
                encoder.writeToBuffer(gpuBuffers.vertexBuffer, meshData.getBuffer(), 0);
            }

            // Resorting
            if (meshData.getSortedBuffer() != null)
            {
//                LOGGER.warn("[VBO] uploadBuffersByType() Overlay [{}], RESORTING", type.name());

                if (gpuBuffers.indexBuffer != null && gpuBuffers.indexBuffer.size() >= meshData.getSortedBuffer().remaining())
                {
                    if (!gpuBuffers.indexBuffer.isClosed())
                    {
//                        LOGGER.warn("[VBO] uploadBuffersByType() Overlay [{}], RESORTING --> WRITE BUFFER", type.name());
                        encoder.writeToBuffer(gpuBuffers.indexBuffer, meshData.getSortedBuffer(), 0);
                    }
                }
                else
                {
                    if (gpuBuffers.indexBuffer != null)
                    {
                        gpuBuffers.indexBuffer.close();
                    }

//                    LOGGER.warn("[VBO] uploadBuffersByType() Overlay [{}], RESORTING --> CREATE/SET INDEX BUFFER", type.name());
                    gpuBuffers.setIndexBuffer(RenderSystem.getDevice()
                                                          .createBuffer(() -> "SortedBuffer: Overlay/" + gpuBuffers.getName() + " VBO Section: [" + this.chunkRelativePos.toShortString() + "]", BufferType.INDICES, BufferUsage.STATIC_WRITE, meshData.getSortedBuffer())
                    );
                }
            }
            else if (gpuBuffers.indexBuffer != null)
            {
//                LOGGER.warn("[VBO] uploadBuffersByType() Overlay [{}], ELSE --> CLEAR INDEX BUFFER", type.name());
                gpuBuffers.indexBuffer.close();
                gpuBuffers.setIndexBuffer(null);
            }

//            LOGGER.warn("[VBO] uploadBuffersByType() Overlay [{}], INDEX COUNT/TYPE --> SAVE", type.name());
            gpuBuffers.setIndexCount(meshData.getDrawParameters().indexCount());
            gpuBuffers.setIndexType(meshData.getDrawParameters().indexType());
//            this.gpuBufferCache.storeBuffersByType(type, gpuBuffers);
        }
        else
        {
            Supplier<String> name = type::name;
//            LOGGER.warn("[VBO] uploadBuffersByType() Overlay [{}], NEW VERTEX BUFFER", type.name());
            GpuBuffer vertexBuffer = RenderSystem.getDevice()
                                                 .createBuffer(() -> "VertexBuffer: Overlay/" + name.get() + " VBO Section: [" + this.chunkRelativePos.toShortString() + "]", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshData.getBuffer()
                                                 );
            GpuBuffer indexBuffer = meshData.getSortedBuffer() != null ? RenderSystem.getDevice()
                                                                                     .createBuffer(() -> "IndexBuffer: " + name.get() + " VBO Section: [" + this.chunkRelativePos.toShortString() + "]", BufferType.INDICES, BufferUsage.STATIC_WRITE, meshData.getSortedBuffer()
                                                                                     ) : null;

//            LOGGER.warn("[VBO] uploadBuffersByType() Overlay [{}], NEW VERTEX BUFFER --> SAVE", type.name());
            this.gpuBufferCache.storeBuffersByType(type,
                                                    new ChunkRenderObjectBuffers(name, vertexBuffer, indexBuffer,
                                                                                 meshData.getDrawParameters().indexCount(),
                                                                                 meshData.getDrawParameters().indexType())
            );
        }

        LOGGER.warn("[VBO] uploadBuffersByType() Overlay [{}], END", type.name());
//        meshData.close();
    }

    protected void uploadIndexByLayer(RenderLayer layer, @Nonnull BufferAllocator.CloseableBuffer buffer)
    {
        LOGGER.warn("[VBO] uploadIndexByLayer() Layer [{}] --> BEGIN", ChunkRenderLayers.getFriendlyName(layer));
        if (this.gpuBufferCache.hasBuffersByLayer(layer))
        {
            ChunkRenderObjectBuffers gpuBuffers = this.gpuBufferCache.getBuffersByLayer(layer);

            assert gpuBuffers != null;
            if (gpuBuffers.indexBuffer == null)
            {
//                LOGGER.warn("[VBO] uploadIndexByLayer() Layer [{}] --> SET INDEX BUFFER", ChunkRenderLayers.getFriendlyName(layer));
                gpuBuffers.setIndexBuffer(
                        RenderSystem.getDevice()
                                    .createBuffer(() -> "IndexBuffer: " + gpuBuffers.getName() + " VBO Section: [" + this.chunkRelativePos.toShortString() + "]", BufferType.INDICES, BufferUsage.STATIC_WRITE, buffer.getBuffer())
                );
            }
            else
            {
                if (!gpuBuffers.indexBuffer.isClosed())
                {
//                    LOGGER.warn("[VBO] uploadIndexByLayer() Layer [{}] --> WRITE INDEX BUFFER", ChunkRenderLayers.getFriendlyName(layer));
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffers.indexBuffer, buffer.getBuffer(), 0);
                }
            }
        }

        LOGGER.warn("[VBO] uploadIndexByLayer() Layer [{}] --> END", ChunkRenderLayers.getFriendlyName(layer));
//        buffer.close();
    }

    protected void uploadIndexByType(OverlayRenderType type, @Nonnull BufferAllocator.CloseableBuffer buffer)
    {
        LOGGER.warn("[VBO] uploadIndexByType() Overlay [{}] --> BEGIN", type.name());
        if (this.gpuBufferCache.hasBuffersByType(type))
        {
            ChunkRenderObjectBuffers gpuBuffers = this.gpuBufferCache.getBuffersByType(type);

            assert gpuBuffers != null;
            if (gpuBuffers.indexBuffer == null)
            {
//                LOGGER.warn("[VBO] uploadIndexByType() Overlay [{}] --> SET INDEX BUFFER", type.name());
                gpuBuffers.setIndexBuffer(
                        RenderSystem.getDevice()
                                    .createBuffer(() -> "IndexBuffer: Overlay/" + gpuBuffers.getName() + " VBO Section: [" + this.chunkRelativePos.toShortString() + "]", BufferType.INDICES, BufferUsage.STATIC_WRITE, buffer.getBuffer())
                );
            }
            else
            {
                if (!gpuBuffers.indexBuffer.isClosed())
                {
//                    LOGGER.warn("[VBO] uploadIndexByType() Overlay [{}] --> WRITE INDEX BUFFER", type.name());
                    RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffers.indexBuffer, buffer.getBuffer(), 0);
                }
            }
        }

        LOGGER.warn("[VBO] uploadIndexByType() Overlay [{}] --> END", type.name());
//        buffer.close();
    }

    private void postRenderBlocks(RenderLayer layer, float x, float y, float z, @Nonnull ChunkRenderDataSchematic chunkRenderData, @Nonnull BufferAllocatorCache allocators)
            throws RuntimeException
    {
        LOGGER.warn("[VBO] postRenderBlocks(): layer: [{}]", ChunkRenderLayers.getFriendlyName(layer));

        if (!chunkRenderData.isBlockLayerEmpty(layer))
        {
            BuiltBuffer built;

            if (chunkRenderData.getBuiltBufferCache().hasBuiltBufferByLayer(layer))
            {
                Objects.requireNonNull(chunkRenderData.getBuiltBufferCache().getBuiltBufferByLayer(layer)).close();
            }

            if (this.builderCache.hasBufferByLayer(layer))
            {
                BufferBuilder builder = this.builderCache.getBufferByLayer(layer, allocators);
                built = builder.endNullable();

                if (built == null)
                {
                    chunkRenderData.setBlockLayerUnused(layer);
                    return;
                }
                else
                {
                    chunkRenderData.getBuiltBufferCache().storeBuiltBufferByLayer(layer, built);
                }
            }
            else
            {
                chunkRenderData.setBlockLayerUnused(layer);
                return;
            }

            if (layer == RenderLayer.getTranslucent())
            {
                try
                {
                    this.resortRenderBlocks(layer, x, y, z, chunkRenderData, allocators);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e.toString());
                }
            }
        }
    }

    private void postRenderOverlay(OverlayRenderType type, float x, float y, float z, @Nonnull ChunkRenderDataSchematic chunkRenderData, @Nonnull BufferAllocatorCache allocators)
            throws RuntimeException
    {
        LOGGER.warn("[VBO] postRenderOverlay(): overlay type: [{}]", type.name());

        if (!chunkRenderData.isOverlayTypeEmpty(type))
        {
            BuiltBuffer built;

            if (chunkRenderData.getBuiltBufferCache().hasBuiltBufferByType(type))
            {
                Objects.requireNonNull(chunkRenderData.getBuiltBufferCache().getBuiltBufferByType(type)).close();
            }

            if (this.builderCache.hasBufferByOverlay(type))
            {
                BufferBuilder builder = this.builderCache.getBufferByOverlay(type, allocators);
                built = builder.endNullable();

                if (built == null)
                {
                    chunkRenderData.setOverlayTypeUnused(type);
                    return;
                }
                else
                {
                    chunkRenderData.getBuiltBufferCache().storeBuiltBufferByType(type, built);
                }
            }
            else
            {
                chunkRenderData.setOverlayTypeUnused(type);
                return;
            }

            if (type.isTranslucent() && Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_RESORTING.getBooleanValue())
            {
                try
                {
                    this.resortRenderOverlay(type, x, y, z, chunkRenderData, allocators);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e.toString());
                }
            }
        }
    }

    protected VertexSorter createVertexSorter(float x, float y, float z)
    {
        return VertexSorter.byDistance(x, y, z);
    }

    protected VertexSorter createVertexSorter(Vec3d pos)
    {
        return VertexSorter.byDistance((float) pos.getX(), (float) pos.getY(), (float) pos.getZ());
    }

    protected VertexSorter createVertexSorter(Vec3d pos, BlockPos origin)
    {
        return VertexSorter.byDistance((float)(pos.x - (double)origin.getX()), (float)(pos.y - (double) origin.getY()), (float)(pos.z - (double) origin.getZ()));
    }

    protected VertexSorter createVertexSorter(Camera camera)
    {
        Vec3d vec3d = camera.getPos();

        return this.createVertexSorter(vec3d, this.getOrigin());
    }

//    protected void uploadSortingState(@Nonnull BufferAllocator.CloseableBuffer result, @Nonnull VertexBuffer vertexBuffer)
//    {
//        if (vertexBuffer.isClosed())
//        {
//            result.close();
//            return;
//        }
//
//        vertexBuffer.bind();
//        vertexBuffer.uploadIndexBuffer(result);
//        VertexBuffer.unbind();
//    }

    private void resortRenderBlocks(RenderLayer layer, float x, float y, float z, @Nonnull ChunkRenderDataSchematic chunkRenderData, @Nonnull BufferAllocatorCache allocators)
            throws InterruptedException
    {
        LOGGER.warn("[VBO] resortRenderBlocks() layer [{}]", ChunkRenderLayers.getFriendlyName(layer));

        if (!chunkRenderData.isBlockLayerEmpty(layer))
        {
            BufferAllocator allocator = allocators.getBufferByLayer(layer);
            BuiltBuffer built;

            if (allocator == null)
            {
                chunkRenderData.setBlockLayerUnused(layer);
                return;
            }
            if (!chunkRenderData.getBuiltBufferCache().hasBuiltBufferByLayer(layer))
            {
                chunkRenderData.setBlockLayerUnused(layer);
                return;
            }

            built = chunkRenderData.getBuiltBufferCache().getBuiltBufferByLayer(layer);

            if (built == null)
            {
                chunkRenderData.setBlockLayerUnused(layer);
                return;
            }

            if (layer.isTranslucent())
            {
                BuiltBuffer.SortState sortingData;
                VertexSorter sorter = VertexSorter.byDistance(x, y, z);

                if (!chunkRenderData.hasTransparentSortingDataForLayer(layer))
                {
                    sortingData = built.sortQuads(allocator, sorter);

                    if (sortingData == null)
                    {
                        throw new InterruptedException("Sort State failure");
                    }

                    chunkRenderData.setTransparentSortingDataForLayer(layer, sortingData);
                }
                else
                {
                    sortingData = chunkRenderData.getTransparentSortingDataForLayer(layer);
                }

                if (sortingData == null)
                {
                    throw new InterruptedException("Sorting Data failure");
                }
            }
        }
    }

    private void resortRenderOverlay(OverlayRenderType type, float x, float y, float z, @Nonnull ChunkRenderDataSchematic chunkRenderData, @Nonnull BufferAllocatorCache allocators)
            throws InterruptedException
    {
        LOGGER.warn("[VBO] resortRenderOverlay() overlay type [{}]", type.name());

        if (!chunkRenderData.isOverlayTypeEmpty(type))
        {
            BufferAllocator allocator = allocators.getBufferByOverlay(type);
            BuiltBuffer built;

            if (allocator == null)
            {
                chunkRenderData.setOverlayTypeUnused(type);
                return;
            }
            if (!chunkRenderData.getBuiltBufferCache().hasBuiltBufferByType(type))
            {
                chunkRenderData.setOverlayTypeUnused(type);
                return;
            }

            built = chunkRenderData.getBuiltBufferCache().getBuiltBufferByType(type);

            if (built == null)
            {
                chunkRenderData.setOverlayTypeUnused(type);
                return;
            }

            if (type.isTranslucent() && Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_RESORTING.getBooleanValue())
            {
                BuiltBuffer.SortState sortingData;
                VertexSorter sorter = VertexSorter.byDistance(x, y, z);

                if (!chunkRenderData.hasTransparentSortingDataForOverlay(type))
                {
                    sortingData = built.sortQuads(allocator, sorter);

                    if (sortingData == null)
                    {
                        throw new InterruptedException("Sort State failure");
                    }

                    chunkRenderData.setTransparentSortingDataForOverlay(type, sortingData);
                }
                else
                {
                    sortingData = chunkRenderData.getTransparentSortingDataForOverlay(type);
                }

                if (sortingData == null)
                {
                    throw new InterruptedException("Sorting Data failure");
                }
            }
        }
    }

    protected ChunkRenderTaskSchematic makeCompileTaskChunkSchematic(Supplier<Vec3d> cameraPosSupplier)
    {
        /*  Threaded Code

        ChunkRenderTaskSchematic generator = new ChunkRenderTaskSchematic(this, ChunkRenderTaskSchematic.Type.REBUILD_CHUNK, cameraPosSupplier, this.getDistanceSq());
        this.finishCompileTask(generator);
        this.rebuildWorldView();

        return generator;
         */

        this.chunkRenderLock.lock();
        ChunkRenderTaskSchematic generator;

        try
        {
            //if (GuiBase.isCtrlDown()) System.out.printf("makeCompileTaskChunk()\n");
            this.finishCompileTask();
            this.rebuildWorldView();
            this.compileTask = new ChunkRenderTaskSchematic(this, ChunkRenderTaskSchematic.Type.REBUILD_CHUNK, cameraPosSupplier, this.getDistanceSq());
            generator = this.compileTask;
        }
        finally
        {
            this.chunkRenderLock.unlock();
        }

        return generator;
    }

    @Nullable
    protected ChunkRenderTaskSchematic makeCompileTaskTransparencySchematic(Supplier<Vec3d> cameraPosSupplier)
    {
        /* Threaded Code

        if (compileTask.get().getStatus() == ChunkRenderTaskSchematic.Status.PENDING)
            return null;
        ChunkRenderTaskSchematic newTask = new ChunkRenderTaskSchematic(this, ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY, cameraPosSupplier, this.getDistanceSq());
        newTask.setChunkRenderData(this.chunkRenderData.get());
        finishCompileTask(newTask);
        return newTask;
         */

        this.chunkRenderLock.lock();

        try
        {
            if (this.compileTask == null || this.compileTask.getStatus() != ChunkRenderTaskSchematic.Status.PENDING)
            {
                if (this.compileTask != null && this.compileTask.getStatus() != ChunkRenderTaskSchematic.Status.DONE)
                {
                    this.compileTask.finish();
                }

                this.compileTask = new ChunkRenderTaskSchematic(this, ChunkRenderTaskSchematic.Type.RESORT_TRANSPARENCY, cameraPosSupplier, this.getDistanceSq());
                this.compileTask.setChunkRenderData(this.chunkRenderData);

                return this.compileTask;
            }
        }
        finally
        {
            this.chunkRenderLock.unlock();
        }

        return null;
    }

    /* Threaded Code

    protected void finishCompileTask(@Nullable ChunkRenderTaskSchematic newTask)
    {
        ChunkRenderTaskSchematic oldtask = compileTask.getAndSet(newTask);
        if (oldtask != null)
            oldtask.finish();
    }
     */

    protected void finishCompileTask()
    {
        this.chunkRenderLock.lock();

        try
        {
            if (this.compileTask != null && this.compileTask.getStatus() != ChunkRenderTaskSchematic.Status.DONE)
            {
                this.compileTask.finish();
                this.compileTask = null;
            }
        }
        finally
        {
            this.chunkRenderLock.unlock();
        }
    }

    protected ReentrantLock getLockCompileTask()
    {
        return this.chunkRenderLock;
    }

    protected void clear()
    {
        try
        {
            this.finishCompileTask();
        }
        finally
        {
            /* Threaded Code

            this.chunkRenderData.get().clearAll();
            this.chunkRenderData.set(ChunkRenderDataSchematic.EMPTY);
             */

            LOGGER.warn("[VBO] clear() pos [{}]", this.position.toShortString());

            if (this.chunkRenderData != null && !this.chunkRenderData.equals(ChunkRenderDataSchematic.EMPTY))
            {
                this.chunkRenderData.clearAll();
            }

            this.builderCache.clearAll();
            this.gpuBufferCache.clearAll();
            this.chunkRenderData = ChunkRenderDataSchematic.EMPTY;
            this.existingOverlays.clear();
            this.hasOverlay = false;
        }
    }

    protected void setNeedsUpdate(boolean immediate)
    {
        if (this.needsUpdate)
        {
            immediate |= this.needsImmediateUpdate;
        }

        this.needsUpdate = true;
        this.needsImmediateUpdate = immediate;
    }

    protected void clearNeedsUpdate()
    {
        this.needsUpdate = false;
        this.needsImmediateUpdate = false;
    }

    protected boolean needsUpdate()
    {
        return this.needsUpdate;
    }

    protected boolean needsImmediateUpdate()
    {
        return this.needsUpdate && this.needsImmediateUpdate;
    }

    private void rebuildWorldView()
    {
        synchronized (this.boxes)
        {
            this.ignoreClientWorldFluids = Configs.Visuals.IGNORE_EXISTING_FLUIDS.getBooleanValue();
            ClientWorld worldClient = MinecraftClient.getInstance().world;
            assert worldClient != null;
            this.schematicWorldView = new ChunkCacheSchematic(this.world, worldClient, this.position, 2);
            this.clientWorldView    = new ChunkCacheSchematic(worldClient, worldClient, this.position, 2);
            this.boxes.clear();

            int chunkX = this.position.getX() / 16;
            int chunkZ = this.position.getZ() / 16;

            for (PlacementPart part : DataManager.getSchematicPlacementManager().getPlacementPartsInChunk(chunkX, chunkZ))
            {
                this.boxes.add(part.bb);
            }
        }
    }

    @Override
    public void close() throws Exception
    {
        this.deleteGlResources();
    }
}
