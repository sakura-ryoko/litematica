package fi.dy.masa.litematica.render.schematic;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Sets;

import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.world.WorldSchematic;

public class ChunkRendererSchematicVbo implements AutoCloseable
{
    protected static int schematicRenderChunksUpdated;

    protected volatile WorldSchematic world;
    protected final WorldRendererSchematic worldRenderer;
    protected ChunkRendererSchematicOverlay overlayRenderer;
    // UNTHREADED CODE
    protected final ReentrantLock chunkRenderLock;
    protected final ReentrantLock chunkRenderDataLock;
    protected final Set<BlockEntity> setBlockEntities = new HashSet<>();
    //
    //protected final AtomicReference<Set<BlockEntity>> setBlockEntities = new AtomicReference<>(new HashSet<>());
    protected final BlockPos.Mutable position;
    protected final BlockPos.Mutable chunkRelativePos;

    protected final Map<RenderLayer, VertexBuffer> vertexBufferBlocks;
    protected final Map<OverlayRenderType, VertexBuffer> vertexBufferOverlay;
    protected final List<IntBoundingBox> boxes = new ArrayList<>();

    private net.minecraft.util.math.Box boundingBox;
    protected ChunkCacheSchematic schematicWorldView;
    protected ChunkCacheSchematic clientWorldView;

    private final BufferBuilderCache builderCache;

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
        this.vertexBufferBlocks = new IdentityHashMap<>();
        this.vertexBufferOverlay = new IdentityHashMap<>();
        this.position = new BlockPos.Mutable();
        this.chunkRelativePos = new BlockPos.Mutable();
        this.builderCache = new BufferBuilderCache();
    }

    protected VertexBuffer getBlocksVertexBufferByLayer(RenderLayer layer)
    {
        return this.vertexBufferBlocks.computeIfAbsent(layer, l -> new VertexBuffer(GlUsage.STATIC_WRITE));
    }

    protected VertexBuffer getOverlayVertexBuffer(OverlayRenderType type)
    {
        //if (GuiBase.isCtrlDown()) System.out.printf("getOverlayVertexBuffer: type: %s, buf: %s\n", type, this.vertexBufferOverlay[type.ordinal()]);
        return this.vertexBufferOverlay.computeIfAbsent(type, l -> new VertexBuffer(GlUsage.STATIC_WRITE));
    }

    protected ChunkRenderDataSchematic getChunkRenderData()
    {
        // Threaded code
        //return this.chunkRenderData.get();
        return this.chunkRenderData;
    }

    protected boolean hasOverlay()
    {
        if (this.overlayRenderer != null)
        {
            return this.overlayRenderer.hasOverlay();
        }

        return false;
    }

    protected BufferBuilderCache getBuilderCache()
    {
        return this.builderCache;
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
        this.vertexBufferBlocks.values().forEach(VertexBuffer::close);
        this.vertexBufferOverlay.values().forEach(VertexBuffer::close);
        this.vertexBufferBlocks.clear();
        this.vertexBufferOverlay.clear();
    }

    protected void resortTransparency(ChunkRenderTaskSchematic task)
    {
        ChunkRenderDataSchematic data = task.getChunkRenderData();
        Vec3d cameraPos = task.getCameraPosSupplier().get();
        RenderLayer layerTranslucent = RenderLayer.getTranslucent();
        BufferAllocatorCache allocators = task.getAllocatorCache();

        float x = (float) cameraPos.x - this.position.getX();
        float y = (float) cameraPos.y - this.position.getY();
        float z = (float) cameraPos.z - this.position.getZ();

        if (data.isBlockLayerEmpty(layerTranslucent) == false)
        {
            //RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_TRANSLUCENT);
            if (data.getBuiltBufferCache().hasBuiltBufferByLayer(layerTranslucent))
            {
                try
                {
                    this.resortRenderBlocks(layerTranslucent, x, y, z, data, allocators);
                }
                catch (Exception e)
                {
                    Litematica.LOGGER.error("resortTransparency() [VBO] caught exception for layer [{}] // {}", ChunkRenderLayers.getFriendlyName(layerTranslucent), e.toString());
                }
            }
        }

        //if (GuiBase.isCtrlDown()) System.out.printf("resortTransparency\n");
        //if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue())

        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_RESORTING.getBooleanValue())
        {
            OverlayRenderType type = OverlayRenderType.QUAD;

            if (data.isOverlayTypeEmpty(type) == false)
            {
                if (data.getBuiltBufferCache().hasBuiltBufferByType(type))
                {
                    try
                    {
                        this.resortRenderOverlay(type, x, y, z, data, allocators);
                    }
                    catch (Exception e)
                    {
                        Litematica.LOGGER.error("resortTransparency() [VBO] caught exception for overlay type [{}] // {}", type.getDrawMode().name(), e.toString());
                    }
                }
            }
        }
    }

    protected void rebuildChunk(ChunkRenderTaskSchematic task)
    {
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

        Set<BlockEntity> tileEntities = new HashSet<>();
        BlockPos posChunk = this.position;
        LayerRange range = DataManager.getRenderLayerRange();
        BufferAllocatorCache allocators = task.getAllocatorCache();

        if (this.overlayRenderer != null)
        {
            this.overlayRenderer.rebuild();
        }

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

                    this.overlayRenderer = new ChunkRendererSchematicOverlay(this.worldRenderer, this.schematicWorldView, this.clientWorldView);

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
                            Litematica.LOGGER.error("rebuildChunk() [VBO] failed to postRenderBlocks() for layer [{}] --> {}", ChunkRenderLayers.getFriendlyName(layerTmp), e.toString());
                        }
                    }
                }

                if (this.overlayRenderer != null && this.overlayRenderer.hasOverlay())
                {
                    //if (GuiBase.isCtrlDown()) System.out.printf("postRenderOverlays\n");
                    for (OverlayRenderType type : this.overlayRenderer.getOverlayTypes())
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
                                Litematica.LOGGER.error("rebuildChunk() [VBO] failed to postRenderOverlay() for overlay type [{}] --> {}", type.getDrawMode().name(), e.toString());
                            }
                        }
                    }
                }
            }
        }

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

            if (fluidState.isEmpty() == false &&
                Configs.Visuals.ENABLE_SCHEMATIC_FLUIDS.getBooleanValue())
            {
                RenderLayer layer = RenderLayers.getFluidLayer(fluidState);
                int offsetY = ((pos.getY() >> 4) << 4) - this.position.getY();
                BufferBuilder bufferSchematic = this.builderCache.getBufferByLayer(layer, allocators);

                if (data.isBlockLayerStarted(layer) == false || bufferSchematic == null)
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
                RenderLayer layer = translucent ? RenderLayer.getTranslucent() : RenderLayers.getBlockLayer(stateSchematic);
                BufferBuilder bufferSchematic = this.builderCache.getBufferByLayer(layer, allocators);

                if (data.isBlockLayerStarted(layer) == false || bufferSchematic == null)
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
            if (this.overlayRenderer == null)
            {
                this.overlayRenderer = new ChunkRendererSchematicOverlay(this.worldRenderer, this.schematicWorldView, this.clientWorldView);
            }

            this.overlayRenderer.renderOverlays(pos, stateSchematic, stateClient, missing, this.getChunkRelativePosition(pos), data, allocators, this.builderCache);
        }
    }

    protected BlockPos.Mutable getChunkRelativePosition(BlockPos pos)
    {
        return this.chunkRelativePos.set(pos.getX() & 0xF, pos.getY() - this.position.getY(), pos.getZ() & 0xF);
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

    protected void uploadBuiltBuffer(@Nonnull BuiltBuffer builtBuffer, @Nonnull VertexBuffer vertexBuffer)
    {
        if (vertexBuffer.isClosed())
        {
            //Litematica.logger.error("uploadBuiltBuffer() [VBO] - Error, vertexBuffer is closed/Null");
            builtBuffer.close();
            return;
        }

        vertexBuffer.bind();
        vertexBuffer.upload(builtBuffer);
        VertexBuffer.unbind();
    }

    private void postRenderBlocks(RenderLayer layer, float x, float y, float z, @Nonnull ChunkRenderDataSchematic chunkRenderData, @Nonnull BufferAllocatorCache allocators)
            throws RuntimeException
    {
        if (!chunkRenderData.isBlockLayerEmpty(layer))
        {
            BuiltBuffer built;

            if (chunkRenderData.getBuiltBufferCache().hasBuiltBufferByLayer(layer))
            {
                chunkRenderData.getBuiltBufferCache().getBuiltBufferByLayer(layer).close();
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

    private void postRenderOverlay(OverlayRenderType type, float x, float y, float z,
                                   @Nonnull ChunkRenderDataSchematic chunkRenderData, @Nonnull BufferAllocatorCache allocators)
            throws RuntimeException
    {
        //RenderSystem.applyModelViewMatrix();
        if (chunkRenderData.isOverlayTypeEmpty(type) == false)
        {
            BuiltBuffer built;

            if (chunkRenderData.getBuiltBufferCache().hasBuiltBufferByType(type))
            {
                chunkRenderData.getBuiltBufferCache().getBuiltBufferByType(type).close();
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

    protected void uploadSortingState(@Nonnull BufferAllocator.CloseableBuffer result, @Nonnull VertexBuffer vertexBuffer)
    {
        if (vertexBuffer.isClosed())
        {
            result.close();
            return;
        }

        vertexBuffer.bind();
        vertexBuffer.uploadIndexBuffer(result);
        VertexBuffer.unbind();
    }

    private void resortRenderBlocks(RenderLayer layer, float x, float y, float z, @Nonnull ChunkRenderDataSchematic chunkRenderData, @Nonnull BufferAllocatorCache allocators)
            throws InterruptedException
    {
        if (chunkRenderData.isBlockLayerEmpty(layer) == false)
        {
            BufferAllocator allocator = allocators.getBufferByLayer(layer);
            BuiltBuffer built;

            if (allocator == null)
            {
                chunkRenderData.setBlockLayerUnused(layer);
                return;
            }
            if (chunkRenderData.getBuiltBufferCache().hasBuiltBufferByLayer(layer) == false)
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

            if (layer == RenderLayer.getTranslucent())
            {
                BuiltBuffer.SortState sortingData;
                VertexSorter sorter = VertexSorter.byDistance(x, y, z);

                if (chunkRenderData.hasTransparentSortingData() == false)
                {
                    sortingData = built.sortQuads(allocator, sorter);

                    if (sortingData == null)
                    {
                        throw new InterruptedException("Sort State failure");
                    }

                    chunkRenderData.setTransparentSortingData(sortingData);
                }
                else
                {
                    sortingData = chunkRenderData.getTransparentSortingData();
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
        //RenderSystem.applyModelViewMatrix();
        if (chunkRenderData.isOverlayTypeEmpty(type) == false)
        {
            BufferAllocator allocator = allocators.getBufferByOverlay(type);
            BuiltBuffer built;

            if (allocator == null)
            {
                chunkRenderData.setOverlayTypeUnused(type);
                return;
            }
            if (chunkRenderData.getBuiltBufferCache().hasBuiltBufferByType(type) == false)
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

                if (chunkRenderData.hasTransparentSortingDataForOverlay(type) == false)
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

            if (this.chunkRenderData != null && !this.chunkRenderData.equals(ChunkRenderDataSchematic.EMPTY))
            {
                this.chunkRenderData.clearAll();
            }
            this.builderCache.clearAll();
            this.chunkRenderData = ChunkRenderDataSchematic.EMPTY;

            if (this.overlayRenderer != null)
            {
                this.overlayRenderer.clear();
            }
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
