package fi.dy.masa.litematica.render.schematic;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.Logger;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.IWorldSchematicRenderer;
import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.util.IgnoreBlockRegistry;
import fi.dy.masa.litematica.util.OverlayType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.world.ChunkSchematicState;
import fi.dy.masa.litematica.world.WorldSchematic;

public class ChunkRendererSchematicVbo implements AutoCloseable
{
    private static final Logger LOGGER = Litematica.LOGGER;
    protected static int schematicRenderChunksUpdated;

    protected volatile WorldSchematic world;
    protected final IWorldSchematicRenderer worldRenderer;
    private final RandomSource rand;
    protected final ReentrantLock chunkRenderLock;
    protected final ReentrantLock chunkRenderDataLock;
    protected final Set<BlockEntity> setBlockEntities;
    protected ProfilerFiller profiler;
    protected final BlockPos.MutableBlockPos position;
    protected final BlockPos.MutableBlockPos chunkRelativePos;
    protected ChunkPos chunkPosition;

    protected final List<IntBoundingBox> boxes;
    protected final EnumSet<OverlayRenderType> existingOverlays;

    private AABB boundingBox;
    protected Color4f overlayColor;
    protected boolean hasOverlay;
    private boolean ignoreClientWorldFluids;
    private IgnoreBlockRegistry ignoreBlockRegistry;

    protected ChunkCacheSchematic schematicWorldView;
    protected ChunkCacheSchematic clientWorldView;

    protected ChunkRenderTaskSchematic compileTask;
    protected ChunkRenderDataSchematic chunkRenderData;

    private boolean needsUpdate;
    private boolean needsImmediateUpdate;

    protected ChunkRendererSchematicVbo(WorldSchematic world, IWorldSchematicRenderer worldRenderer)
    {
        this.world = world;
        this.worldRenderer = worldRenderer;
		this.rand = RandomSource.create();
        this.chunkRenderData = ChunkRenderDataSchematic.EMPTY;
        this.chunkRenderLock = new ReentrantLock();
		this.setBlockEntities = new HashSet<>();
        this.chunkRenderDataLock = new ReentrantLock();
        this.position = new BlockPos.MutableBlockPos();
        this.chunkRelativePos = new BlockPos.MutableBlockPos();
		this.boxes = new ArrayList<>();
		this.existingOverlays = EnumSet.noneOf(OverlayRenderType.class);
		this.hasOverlay = false;
    }

    public boolean hasOverlay()
    {
        return this.hasOverlay;
    }

	public boolean isEmpty()
	{
		return this.boxes.isEmpty() && this.getPartsCount() == 0;
	}

    protected ProfilerFiller getProfiler()
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

    protected ChunkRenderDataSchematic getChunkRenderData()
    {
        return this.chunkRenderData;
    }

    protected void updateChunkRenderData(ChunkRenderDataSchematic data)
    {
//        LOGGER.warn("[VBO] updateChunkRenderData()");
        this.chunkRenderDataLock.lock();

        try
        {
            if (this.chunkRenderData != null && !this.chunkRenderData.isEmpty())
            {
                final int comparator = ChunkRenderDataSchematic.COMPARATOR.compare(this.chunkRenderData, data);
                ChunkMeshDataSchematic oldMeshDataCache = this.chunkRenderData.getMeshDataCache();
//                LOGGER.error("[VBO] updateChunkRenderData() compare: [{}] // oldData DUMP -->", comparator);
//                this.chunkRenderData.dumpRenderDataDebug();

                if (comparator > 0)
                {
//                    LOGGER.error("[VBO] updateChunkRenderData() oldData CLEAR");
                    this.chunkRenderData.clearAll();
                    this.chunkRenderData = data;
                }
//                else
//                {
//                    // Don't update
//                    LOGGER.error("[VBO] updateChunkRenderData() oldData SAVE");
//                }

                // Check if Mesh needs saving
                if (!oldMeshDataCache.isEmpty())
                {
                    ChunkMeshDataSchematic oldData = this.chunkRenderData.updateMeshDataCache(oldMeshDataCache);

                    if (oldData != null)
                    {
                        oldData.clearAll();
                    }
                }
            }
            else
            {
//                LOGGER.error("[VBO] updateChunkRenderData() oldData EMPTY/NULL --> newData");
                this.chunkRenderData = data;
            }
        }
        finally
        {
            this.chunkRenderDataLock.unlock();
        }

//        LOGGER.error("[VBO] updateChunkRenderData() // newData DUMP -->");
//        this.chunkRenderData.dumpRenderDataDebug();
    }

    public BlockPos getOrigin()
    {
        return this.position;
    }

    protected ChunkPos getChunkPos()
    {
        if (this.chunkPosition == null)
        {
            this.chunkPosition = new ChunkPos(this.position.immutable());
        }

        return this.chunkPosition;
    }

    public AABB getBoundingBox()
    {
        if (this.boundingBox == null)
        {
            int x = this.position.getX();
            int y = this.position.getY();
            int z = this.position.getZ();
            this.boundingBox = new AABB(x, y, z, x + 16, y + this.world.getHeight(), z + 16);
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
            this.chunkPosition = null;
            this.position.set(x, y, z);
        }
    }

    protected void setChunkPosition(int chunkX, int chunkZ)
    {
        this.chunkPosition = new ChunkPos(chunkX, chunkZ);
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
        //this.world = null;
    }

    protected void resortTransparency(ChunkRenderTaskSchematic task, ChunkRenderDispatcherBuffers pack)
    {
        this.getProfiler().push("resort_task");
        ChunkRenderDataSchematic data = task.getChunkRenderData();
        Vec3 cameraPos = task.getCameraPosSupplier().get();
        ChunkSectionLayer layerTranslucent = ChunkSectionLayer.TRANSLUCENT;
        ChunkMeshDataSchematic chunkMeshData = data.getMeshDataCache();

        float x = (float) cameraPos.x - this.position.getX();
        float y = (float) cameraPos.y - this.position.getY();
        float z = (float) cameraPos.z - this.position.getZ();

        boolean resortBlocks = Configs.Visuals.RENDER_ENABLE_TRANSLUCENT_RESORTING.getBooleanValue();

        if (!data.isBlockLayerEmpty(layerTranslucent) && resortBlocks)
        {
            this.getProfiler().popPush("resort_blocks");
            //RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_TRANSLUCENT);

            if (chunkMeshData.hasMeshData(layerTranslucent))
            {
                try
                {
                    this.resortRenderBlocks(layerTranslucent, x, y, z, data, chunkMeshData, pack);

                    ChunkMeshDataSchematic oldData = data.updateMeshDataCache(chunkMeshData);

                    if (oldData != null)
                    {
                        oldData.clearAll();
                    }
                }
                catch (Exception e)
                {
                    LOGGER.error("resortTransparency() [VBO] caught exception for layer [{}] // {}", layerTranslucent.label(), e.toString());
                }
            }
        }

        //if (GuiBase.isCtrlDown()) System.out.printf("resortTransparency\n");
        //if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue())
        boolean resortOverlay = Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue() && false;

        if (resortOverlay)
        {
            this.getProfiler().popPush("resort_overlay");
            OverlayRenderType type = OverlayRenderType.QUAD;

            if (!data.isOverlayTypeEmpty(type))
            {
                if (chunkMeshData.hasMeshData(type))
                {
                    try
                    {
                        this.resortRenderOverlay(type, x, y, z, data, chunkMeshData, pack);

                        ChunkMeshDataSchematic oldData = data.updateMeshDataCache(chunkMeshData);

                        if (oldData != null)
                        {
                            oldData.clearAll();
                        }
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

    protected void rebuildChunk(ChunkRenderTaskSchematic task, ChunkRenderDispatcherBuffers pack)
    {
        this.getProfiler().push("rebuild_chunk");
        ChunkRenderDataSchematic data = new ChunkRenderDataSchematic();
        task.getLock().lock();

        try
        {
            if (task.getStatus() != ChunkRenderTaskSchematic.Status.COMPILING)
            {
                return;
            }

			if (task.getChunkRenderData() != null)
			{
				task.getChunkRenderData().clearAll();
			}

            task.updateChunkRenderData(data);
        }
        finally
        {
            task.getLock().unlock();
        }

        pack.builderCache().clearAll();

        //LOGGER.warn("[VBO] rebuildChunk() pos [{}]", this.position.toShortString());

//        Set<BlockEntity> tileEntities = new HashSet<>();
        BlockPos posChunk = this.position;
        LayerRange range = DataManager.getRenderLayerRange();
        ChunkMeshDataSchematic chunkMeshData = new ChunkMeshDataSchematic();

        if (!pack.allocatorCache().isClear())
        {
            // Using 'reset' will not warn us about 'unused buffers'
            pack.allocatorCache().resetAll();
        }

        chunkMeshData.clearTileCache();

        this.existingOverlays.clear();
        this.hasOverlay = false;
        this.getProfiler().popPush("rebuild_chunk_start");
        
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

                Vec3 cameraPos = task.getCameraPosSupplier().get();
                float x = (float) cameraPos.x - this.position.getX();
                float y = (float) cameraPos.y - this.position.getY();
                float z = (float) cameraPos.z - this.position.getZ();
                Set<ChunkSectionLayer> usedBlockLayers = new HashSet<>();
                Set<RenderType> usedRenderLayers = new HashSet<>();
                PoseStack matrixStack = new PoseStack();
                // TODO --> Do we need to change this to a Matrix4f in the future?
                int bottomY = this.position.getY();
                VisGraph visGraph = new VisGraph();

                this.getProfiler().popPush("rebuild_chunk_boxes");
                for (IntBoundingBox box : this.boxes)
                {
                    box = range.getClampedRenderBoundingBox(box);

                    // The rendered layer(s) don't intersect this sub-volume
                    if (box == null)
                    {
                        continue;
                    }

                    BlockPos posFrom = new BlockPos(box.minX(), box.minY(), box.minZ());
                    BlockPos posTo   = new BlockPos(box.maxX(), box.maxY(), box.maxZ());

					BlockModelRendererSchematic.enableCache();

                    for (BlockPos posMutable : BlockPos.MutableBlockPos.betweenClosed(posFrom, posTo))
                    {
                        // Fluid models and the overlay use the VertexConsumer#vertex(x, y, z) method.
                        // Fluid rendering and the overlay do not use the MatrixStack.
                        // Block models use the VertexConsumer#quad() method, and they use the MatrixStack.
                        matrixStack.pushPose();
                        matrixStack.translate(posMutable.getX() & 0xF, posMutable.getY() - bottomY, posMutable.getZ() & 0xF);

//                        this.renderBlocksAndOverlay(posMutable, data, allocators, tileEntities, usedLayers, matrixStack);
                        this.renderBlocksAndOverlay(posMutable, data, chunkMeshData, pack, usedBlockLayers, usedRenderLayers, matrixStack, visGraph);

                        matrixStack.popPose();
                    }

	                BlockModelRendererSchematic.disableCache();
                }

                this.getProfiler().popPush("rebuild_chunk_layers");
                for (ChunkSectionLayer layerTmp : ChunkRenderLayers.BLOCK_RENDER_LAYERS)
                {
                    if (usedBlockLayers.contains(layerTmp))
                    {
                        data.setBlockLayerUsed(layerTmp);
                    }

                    if (data.isBlockLayerStarted(layerTmp))
                    {
                        try
                        {
                            data.setBlockLayerUsed(layerTmp);
                            this.postRenderBlocks(layerTmp, x, y, z, data, chunkMeshData, pack);
                        }
                        catch (Exception e)
                        {
                            LOGGER.error("rebuildChunk() [VBO] failed to postRenderBlocks() for layer [{}] --> {}", layerTmp.label(), e.toString());
                        }
                    }
                }

                if (this.hasOverlay)
                {
                    this.getProfiler().popPush("rebuild_chunk_overlays");
                    //if (GuiBase.isCtrlDown()) System.out.printf("postRenderOverlays\n");
                    for (OverlayRenderType type : this.existingOverlays)
                    {
                        if (data.isOverlayTypeStarted(type))
                        {
                            try
                            {
                                data.setOverlayTypeUsed(type);
                                this.postRenderOverlay(type, x, y, z, data, chunkMeshData, pack);
                            }
                            catch (Exception e)
                            {
                                LOGGER.error("rebuildChunk() [VBO] failed to postRenderOverlay() for overlay type [{}] --> {}", type.name(), e.toString());
                            }
                        }
                    }
                }

                chunkMeshData.updateVisibility(visGraph);
                chunkMeshData.compileLayerDrawStates(usedBlockLayers);
                chunkMeshData.compileOverlayDrawStates(this.existingOverlays);
            }
        }

        this.getProfiler().pop();
        this.profiler = null;
        this.chunkRenderLock.lock();

        try
        {
//            List<BlockEntity> noCull = data.getNoCullBlockEntities();
//            Set<BlockEntity> set = Sets.newHashSet(noCull);
//            Set<BlockEntity> set2;
//
////            LOGGER.warn("[VBO] combine BE - noCull [{}], set [{}], setBE [{}]", noCull.size(), set.size(), this.setBlockEntities.size());
//
//            synchronized (this.setBlockEntities)
//            {
//                set2 = Sets.newHashSet(this.setBlockEntities);
//                set.removeAll(this.setBlockEntities);
//                noCull.forEach(set2::remove);
//                this.setBlockEntities.clear();
//                this.setBlockEntities.addAll(noCull);
//            }
//
////            LOGGER.warn("[VBO] combine BE - set2 [{}], set [{}], setBE [{}]", set2.size(), set.size(), this.setBlockEntities.size());
//            this.worldRenderer.updateBlockEntities(set2, set);

            chunkMeshData.setTimeBuilt(this.world.getGameTime());
            data.setTimeBuilt(this.world.getGameTime());

            if (!chunkMeshData.isEmpty())
            {
                ChunkMeshDataSchematic oldData = data.updateMeshDataCache(chunkMeshData);

                if (oldData != null)
                {
                    oldData.clearAll();
                }
            }
        }
        finally
        {
            this.updateChunkRenderData(data);
            this.chunkRenderLock.unlock();
        }

        if (this.worldRenderer.getChunkSchematicState(this.chunkPosition.x, this.chunkPosition.z).atLeast(ChunkSchematicState.RENDERED))
        {
            this.worldRenderer.setChunkSchematicState(this.chunkPosition.x, this.chunkPosition.z, ChunkSchematicState.RENDERED);
        }
    }

    protected void renderBlocksAndOverlay(BlockPos pos,
                                          @Nonnull ChunkRenderDataSchematic data,
                                          @Nonnull ChunkMeshDataSchematic chunkMeshData,
                                          ChunkRenderDispatcherBuffers pack,
                                          Set<ChunkSectionLayer> usedBlockLayers,
                                          Set<RenderType> usedRenderLayers,
                                          PoseStack matrixStack, VisGraph visGraph)
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

//        LOGGER.warn("[VBO] renderBlocksAndOverlay() pos [{}], stateSchematic: [{}]", pos.toShortString(), stateSchematic.toString());
        this.getProfiler().push("render_build");
        this.overlayColor = null;

        // Schematic has a block, client has air
        if (clientHasAir || (stateSchematic != stateClient && Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue()))
        {
            final boolean hasTE = stateSchematic.hasBlockEntity();

            if (stateSchematic.isSolidRender())
            {
                visGraph.setOpaque(pos);
            }

            if (hasTE)
            {
//                LOGGER.warn("[VBO] Chunk: {} // addBlockEntity - state [{}]", this.chunkPosition.toString(), stateSchematic.toString());
                this.addBlockEntity(stateSchematic, pos, chunkMeshData);
            }

            boolean translucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();
//	        boolean translucent = false;
            // TODO change when the fluids become separate
            FluidState fluidState = stateSchematic.getFluidState();

            if (!fluidState.isEmpty() && Configs.Visuals.ENABLE_SCHEMATIC_FLUIDS.getBooleanValue())
            {
                this.getProfiler().popPush("render_build_fluids");
                ChunkSectionLayer layer = ItemBlockRenderTypes.getRenderLayer(fluidState);
                int offsetY = ((pos.getY() >> 4) << 4) - this.position.getY();
                BufferBuilder bufferSchematic = this.preRenderBlocks(pack, layer);

                if (!data.isBlockLayerStarted(layer))
                {
                    data.setBlockLayerStarted(layer);
                }

                ((IBufferBuilderPatch) bufferSchematic).litematica$setOffsetY(offsetY);

                this.worldRenderer.renderFluid(this.schematicWorldView, stateSchematic, fluidState, pos, bufferSchematic);
                usedBlockLayers.add(layer);
                ((IBufferBuilderPatch) bufferSchematic).litematica$setOffsetY(0.0F);
            }

            if (stateSchematic.getRenderShape() == RenderShape.MODEL)
            {
                this.getProfiler().popPush("render_build_blocks");
                ChunkSectionLayer layer = translucent ? ChunkSectionLayer.TRANSLUCENT : ItemBlockRenderTypes.getChunkRenderType(stateSchematic);
                BufferBuilder bufferSchematic = this.preRenderBlocks(pack, layer);

                if (!data.isBlockLayerStarted(layer))
                {
                    data.setBlockLayerStarted(layer);
                }

                if (this.worldRenderer.renderBlock(this.schematicWorldView, stateSchematic, pos, matrixStack, bufferSchematic))
                {
                    usedBlockLayers.add(layer);
                }

                if (clientHasAir)
                {
                    missing = true;
                }
            }
        }

        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue())
        {
            this.getProfiler().popPush("render_build_overlays");
            OverlayType type = this.getOverlayType(stateSchematic, stateClient);

            this.overlayColor = getOverlayColor(type);

            if (this.overlayColor != null)
            {
                if (this.shouldCullOverlayPos(pos, stateSchematic, stateClient))
                {
                    this.getProfiler().pop();
                    return;
                }

                this.renderOverlay(type, pos, stateSchematic, missing, data, chunkMeshData, pack);
            }
        }

        this.getProfiler().pop();
    }

    private boolean shouldCullOverlayPos(BlockPos posIn, BlockState stateSchematic, BlockState stateClient)
    {
        if (stateSchematic.getFluidState().isEmpty() == false &&
            Configs.Visuals.ENABLE_SCHEMATIC_FLUIDS.getBooleanValue() == false)
        {
            return true;
        }

        if (Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue() &&
            Configs.Visuals.RENDER_TRANSLUCENT_INNER_SIDES.getBooleanValue())
        {
            return false;
        }

        // This helps cull the Overlay Rendering more thuroughly
        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY_CULLING.getBooleanValue() && stateClient.is(BlockTags.AIR))
        {
            // If Client World is AIR, then only check the Schematic
            int count = 0;

            for (Direction side : fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS)
            {
                if (DataManager.getRenderLayerRange().isPositionAtRenderEdgeOnSide(posIn, side) ||
//                    Block.shouldDrawSide(stateSchematic, this.schematicWorldView, posIn, side, posIn.offset(side)))
                    Block.shouldRenderFace(stateSchematic, this.schematicWorldView.getBlockState(posIn.relative(side)), side))
                {
                    count++;
                }
            }

            if (count == 0)
            {
                return true;
            }
        }

        return false;
    }

    protected void renderOverlay(OverlayType type, BlockPos pos, BlockState stateSchematic, boolean missing,
                                 @Nonnull ChunkRenderDataSchematic data,
                                 @Nonnull ChunkMeshDataSchematic chunkMeshData,
                                 ChunkRenderDispatcherBuffers pack)
    {
        this.getProfiler().push("render_overlay");
        boolean useDefault = false;
        BlockPos.MutableBlockPos relPos = this.getChunkRelativePosition(pos);
        OverlayRenderType overlayType;

//        LOGGER.error("[VBO] renderOverlay: type: [{}] (bool: {}), relPos: [{}] // stateSchematic: [{}]", type.name(), missing, relPos.toShortString(), stateSchematic.toString());

        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES.getBooleanValue())
        {
            this.getProfiler().push("overlay_sides");
            overlayType = OverlayRenderType.QUAD;
            BufferBuilder bufferOverlayQuads = this.preRenderOverlay(pack, overlayType);

            if (!data.isOverlayTypeStarted(overlayType))
            {
                data.setOverlayTypeStarted(overlayType);
            }

            if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue())
            {
                this.getProfiler().popPush("cull_inner_sides");
                BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();
                List<BlockModelPart> modelParts = this.worldRenderer.getModelParts(relPos, stateSchematic, this.rand);

                if (RenderUtils.hasQuads(modelParts))
                {
                    VoxelShape shape = stateSchematic.getCollisionShape(this.schematicWorldView, pos);

                    for (int i = 0; i < 6; i++)
                    {
                        Direction side = fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS[i];
                        posMutable.set(pos.getX() + side.getStepX(), pos.getY() + side.getStepY(), pos.getZ() + side.getStepZ());
                        BlockState adjStateSchematic = this.schematicWorldView.getBlockState(posMutable);
                        BlockState adjStateClient = this.clientWorldView.getBlockState(posMutable);
                        OverlayType typeAdj = getOverlayType(adjStateSchematic, adjStateClient);
                        boolean fullSquareSide = Block.isFaceFull(shape, side);

//                        LOGGER.warn("renderOverlay: Quad; side [{}], fullSquareSide: [{}]", side.asString(), fullSquareSide);

                        // Only render the model-based outlines or sides for missing blocks
                        if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
                        {
                            this.getProfiler().popPush("cull_render_model_sides");

                            if (type.getRenderPriority() > typeAdj.getRenderPriority() ||
                                !fullSquareSide)
                            {
                                this.getProfiler().popPush("cull_render_model");

                                for (BlockModelPart part : modelParts)
                                {
//                                final int light = WorldRenderer.getLightmapCoordinates(this.schematicWorldView, relPos);
//                                    LOGGER.warn("renderOverlay: Batched Block Model Side Quads [{}] -->", side.asString());
                                    RenderUtils.drawBlockModelQuadOverlayBatched(part, stateSchematic, relPos, side, this.overlayColor, 0, bufferOverlayQuads);
                                }
                            }
                            else { useDefault = true; }
                        }
                        else if (type.getRenderPriority() > typeAdj.getRenderPriority())
                        {
                            this.getProfiler().popPush("cull_render_default");
//                            LOGGER.warn("renderOverlay: Batched Block Side Quads [{}] -->", side.asString());
                            RenderUtils.drawBlockBoxSideBatchedQuads(relPos, side, this.overlayColor, 0, bufferOverlayQuads);
                        }
                        else { useDefault = true; }
                    }
                }
                else { useDefault = true; }
            }
            else
            {
                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
                {
                    this.getProfiler().popPush("render_model_sides");
                    List<BlockModelPart> modelParts = this.worldRenderer.getModelParts(relPos, stateSchematic, this.rand);

                    if (RenderUtils.hasQuads(modelParts))
                    {
//                    this.getProfiler().swap("render_model");
//                        LOGGER.warn("renderOverlay: Batched Block Model Quads -->");
                        RenderUtils.drawBlockModelQuadOverlayBatched(modelParts, stateSchematic, relPos, this.overlayColor, 0, bufferOverlayQuads);
                    }
                    else { useDefault = true; }
                }
                else
                {
                    this.getProfiler().popPush("render_batched");
//                    LOGGER.warn("renderOverlay: Batched Default Quads A -->");
//                    fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxSidesBatchedQuads(relPos, this.overlayColor, 0, bufferOverlayQuads);
                    RenderUtils.drawBlockBoxBatchedQuads(relPos, this.overlayColor, 0, bufferOverlayQuads);
                }
            }

            if (useDefault)
            {
                try
                {
                    this.getProfiler().popPush("render_batched_default");
//                    LOGGER.warn("renderOverlay: Batched Default Quads B -->");
                    RenderUtils.drawBlockBoxBatchedQuads(relPos, this.overlayColor, 0, bufferOverlayQuads);
                }
                catch (Exception ignored) { }
            }

            this.getProfiler().pop();
        }

        if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES.getBooleanValue())
        {
            this.getProfiler().push("overlay_outlines");
            useDefault = false;
            overlayType = OverlayRenderType.OUTLINE;
            BufferBuilder bufferOverlayOutlines = this.preRenderOverlay(pack, overlayType);

	        if (!data.isOverlayTypeStarted(overlayType))
            {
                data.setOverlayTypeStarted(overlayType);
            }

            Color4f overlayColor = new Color4f(this.overlayColor.r, this.overlayColor.g, this.overlayColor.b, 1f);
	        float lineWidth = 1.0f;

            this.getProfiler().popPush("cull_inner_sides");
            if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue())
            {
                OverlayType[][][] adjTypes = new OverlayType[3][3][3];
                BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

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
                    if (stateSchematic.canOcclude())
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

                        this.getProfiler().popPush("render_model_batched");
                        List<BlockModelPart> modelParts = this.worldRenderer.getModelParts(relPos, stateSchematic, this.rand);

                        if (RenderUtils.hasQuads(modelParts))
                        {
                            //RenderUtils.renderModelQuadOutlines(bakedModel, stateSchematic, relPos, side, overlayColor, 0, bufferOverlayOutlines);
                            RenderUtils.drawDebugBlockModelOutlinesBatched(modelParts, stateSchematic, relPos, overlayColor, 0, lineWidth, bufferOverlayOutlines);
                        }
                        else { useDefault = true; }
                    }
                }
                else
                {
                    this.getProfiler().popPush("render_reduced_edges");
                    this.renderOverlayReducedEdges(pos, adjTypes, type, lineWidth, bufferOverlayOutlines);
                    //RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(pos, relPos, overlayColor, 0, bufferOverlayOutlines);
                }
            }
            else
            {
                this.getProfiler().popPush("render_fallback");
                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue())
                {
                    this.getProfiler().popPush("render_model_batched");
                    List<BlockModelPart> modelParts = this.worldRenderer.getModelParts(relPos, stateSchematic, this.rand);

                    if (RenderUtils.hasQuads(modelParts))
                    {
                        RenderUtils.drawDebugBlockModelOutlinesBatched(modelParts, stateSchematic, relPos, overlayColor, 0, lineWidth, bufferOverlayOutlines);
                    }
                    else { useDefault = true; }
                }
                else { useDefault = true; }
            }

            if (useDefault)
            {
                try
                {
                    this.getProfiler().popPush("render_batched_box");
//                    LOGGER.warn("renderOverlay: Batched Default Box Outlines -->");
//                    fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(relPos, overlayColor, 0, bufferOverlayOutlines, matrices.peek());
                    RenderUtils.drawBlockBoundingBoxOutlinesBatchedDebugLines(relPos, overlayColor, 0, lineWidth, bufferOverlayOutlines);
                }

                catch (Exception ignored) { }
            }

            this.getProfiler().pop();
        }

        this.getProfiler().pop();
    }

    protected BlockPos.MutableBlockPos getChunkRelativePosition(BlockPos pos)
    {
        return this.chunkRelativePos.set(pos.getX() & 0xF, pos.getY() - this.position.getY(), pos.getZ() & 0xF);
    }

    protected void renderOverlayReducedEdges(BlockPos pos, OverlayType[][][] adjTypes, OverlayType typeSelf, float lineWidth, BufferBuilder bufferOverlayOutlines)
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

                this.getProfiler().popPush("edges_plop");
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
                            this.getProfiler().popPush("render_batched");
                            RenderUtils.drawBlockBoxEdgeBatchedDebugLines(this.getChunkRelativePosition(pos), axis, corner, this.overlayColor, lineWidth, bufferOverlayOutlines);
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
                if (clientHasAir)
                {
                    return OverlayType.NONE;
                }
                else if (this.ignoreClientWorldFluids && stateClient.liquid())
                {
                    return OverlayType.NONE;
                }
                else if (this.ignoreBlockRegistry.hasBlock(stateClient.getBlock()))
                {
                    return OverlayType.NONE;
                }
                else
                {
                    return OverlayType.EXTRA;
                }
            }
            else
            {
                if (clientHasAir || (this.ignoreClientWorldFluids && stateClient.liquid()))
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

    private <T extends BlockEntity> void addBlockEntity(BlockState state, BlockPos pos, ChunkMeshDataSchematic chunkMeshData)
    {
        BlockEntity te = this.schematicWorldView.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);

        if (te == null)
        {
            te = ((EntityBlock) state.getBlock()).newBlockEntity(pos, state);

            if (te != null)
            {
                this.schematicWorldView.world.getChunkAt(pos).addAndRegisterBlockEntity(te);
                this.schematicWorldView.addBlockEntity(pos, te);
            }
        }

        if (te != null)
        {
            BlockEntityRenderer<BlockEntity, BlockEntityRenderState> tesr = this.worldRenderer.getBlockEntityRenderer().getRenderer(te);

            if (tesr != null)
            {
                chunkMeshData.addBlockEntity(te);

                // noCullingTE
                if (tesr.shouldRenderOffScreen())
                {
                    chunkMeshData.addNoCullBlockEntity(te);
                }
            }
        }
//        else
//        {
//            LOGGER.error("[VBO] addBlockEntity: Block Entity not found at {}", pos.toShortString());
//        }
    }

    private BufferBuilder preRenderBlocks(ChunkRenderDispatcherBuffers pack, ChunkSectionLayer layer)
    {
        return pack.getBuilder(layer);
    }

    private BufferBuilder preRenderOverlay(ChunkRenderDispatcherBuffers pack, OverlayRenderType type)
    {
        this.existingOverlays.add(type);
        this.hasOverlay = true;
        return pack.getBuilder(type);
    }

    private void postRenderBlocks(ChunkSectionLayer layer, float x, float y, float z,
                                  @Nonnull ChunkRenderDataSchematic chunkRenderData,
                                  @Nonnull ChunkMeshDataSchematic chunkMeshData,
                                  ChunkRenderDispatcherBuffers pack)
            throws RuntimeException
    {
        //LOGGER.warn("[VBO] postRenderBlocks(): layer: [{}]", layer.label());

        if (!chunkRenderData.isBlockLayerEmpty(layer))
        {
            MeshData meshData;

            if (chunkMeshData.hasMeshData(layer))
            {
                MeshData oldMesh = chunkMeshData.getMeshDataOrNull(layer);

                if (oldMesh != null)
                {
                    oldMesh.close();
                }
            }

            if (pack.builderCache().hasBuilder(layer))
            {
                BufferBuilder builder = pack.getBuilder(layer);
                meshData = builder.build();

                if (meshData == null)
                {
                    //LOGGER.error("[VBO] postRenderBlocks(): layer: [{}] -- Mesh is null!", layer.label());
                    chunkRenderData.setBlockLayerUnused(layer);
                    return;
                }
                else
                {
                    chunkMeshData.saveMeshData(layer, meshData);
                }
            }
            else
            {
                //LOGGER.error("[VBO] postRenderBlocks(): layer: [{}] -- Invalid Builder", layer.label());
                chunkRenderData.setBlockLayerUnused(layer);
                return;
            }

            if (layer == ChunkSectionLayer.TRANSLUCENT && Configs.Visuals.RENDER_ENABLE_TRANSLUCENT_RESORTING.getBooleanValue())
            {
                try
                {
                    //LOGGER.warn("[VBO] postRenderBlocks(): layer: [{}] --> RESORT", layer.label());
                    this.resortRenderBlocks(layer, x, y, z, chunkRenderData, chunkMeshData, pack);
                }
                catch (Exception e)
                {
                    //LOGGER.error("[VBO] postRenderBlocks(): layer: [{}] -- resortRenderBlocks() Exception: {}", layer.label(), e.getLocalizedMessage());
                    throw new RuntimeException(e.toString());
                }
            }
        }
        else
        {
            //LOGGER.error("[VBO] postRenderBlocks(): layer: [{}] -- Layer not started!", layer.label());
        }

        //LOGGER.warn("[VBO] postRenderBlocks(): layer: [{}] --> END", layer.label());
    }

    private void postRenderOverlay(OverlayRenderType type, float x, float y, float z,
                                   @Nonnull ChunkRenderDataSchematic chunkRenderData,
                                   @Nonnull ChunkMeshDataSchematic chunkMeshData,
                                   ChunkRenderDispatcherBuffers pack)
            throws RuntimeException
    {
        //LOGGER.warn("[VBO] postRenderOverlay(): overlay type: [{}]", type.name());

        if (!chunkRenderData.isOverlayTypeEmpty(type))
        {
            MeshData meshData;

            if (chunkMeshData.hasMeshData(type))
            {
                MeshData oldMesh = chunkMeshData.getMeshDataOrNull(type);

                if (oldMesh != null)
                {
                    oldMesh.close();
                }
            }

            if (pack.builderCache().hasBuilder(type))
            {
                BufferBuilder builder = pack.getBuilder(type);
                meshData = builder.build();

                if (meshData == null)
                {
                    chunkRenderData.setOverlayTypeUnused(type);
                    return;
                }
                else
                {
                    chunkMeshData.saveMeshData(type, meshData);
                }
            }
            else
            {
                chunkRenderData.setOverlayTypeUnused(type);
                return;
            }

            boolean resortOverlays = false;

            if (type.isTranslucent() && resortOverlays)
            {
                try
                {
                    this.resortRenderOverlay(type, x, y, z, chunkRenderData, chunkMeshData, pack);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e.toString());
                }
            }
        }
    }

    private void resortRenderBlocks(ChunkSectionLayer layer, float x, float y, float z,
                                    @Nonnull ChunkRenderDataSchematic chunkRenderData,
                                    @Nonnull ChunkMeshDataSchematic chunkMeshData,
                                    ChunkRenderDispatcherBuffers pack)
            throws InterruptedException
    {
        //LOGGER.warn("[VBO] resortRenderBlocks() layer [{}]", ChunkRenderLayers.getFriendlyName(layer));

        if (!chunkRenderData.isBlockLayerEmpty(layer))
        {
            ByteBufferBuilder allocator = pack.allocatorCache().getAllocator(layer);
            MeshData meshData;

            if (allocator == null)
            {
                chunkRenderData.setBlockLayerUnused(layer);
                return;
            }
            if (!chunkMeshData.hasMeshData(layer))
            {
                chunkRenderData.setBlockLayerUnused(layer);
                return;
            }

            meshData = chunkMeshData.getMeshDataOrNull(layer);

            if (meshData == null)
            {
                chunkRenderData.setBlockLayerUnused(layer);
                return;
            }

            if (layer == ChunkSectionLayer.TRANSLUCENT && Configs.Visuals.RENDER_ENABLE_TRANSLUCENT_RESORTING.getBooleanValue())
            {
                MeshData.SortState sortingData;
                VertexSorting sorter = VertexSorting.byDistance(x, y, z);

                if (!chunkMeshData.hasTransparentSortingDataForBlockLayer(layer))
                {
                    sortingData = meshData.sortQuads(allocator, sorter);

                    if (sortingData == null)
                    {
                        throw new InterruptedException("Sort State failure");
                    }

                    chunkMeshData.setTransparentSortingDataForBlockLayer(layer, sortingData);
                }
                else
                {
                    sortingData = chunkMeshData.getTransparentSortingDataForBlockLayer(layer);
                }

                if (sortingData == null)
                {
                    throw new InterruptedException("Sorting Data failure");
                }
            }
        }
    }

    private void resortRenderOverlay(OverlayRenderType type, float x, float y, float z,
                                     @Nonnull ChunkRenderDataSchematic chunkRenderData,
                                     @Nonnull ChunkMeshDataSchematic chunkMeshData,
                                     ChunkRenderDispatcherBuffers pack)
            throws InterruptedException
    {
        //LOGGER.warn("[VBO] resortRenderOverlay() overlay type [{}]", type.name());

        if (!chunkRenderData.isOverlayTypeEmpty(type))
        {
            ByteBufferBuilder allocator = pack.allocatorCache().getAllocator(type);
            MeshData meshData;

            if (allocator == null)
            {
                chunkRenderData.setOverlayTypeUnused(type);
                return;
            }
            if (!chunkMeshData.hasMeshData(type))
            {
                chunkRenderData.setOverlayTypeUnused(type);
                return;
            }

            meshData = chunkMeshData.getMeshDataOrNull(type);

            if (meshData == null)
            {
                chunkRenderData.setOverlayTypeUnused(type);
                return;
            }

            boolean resortOverlays = false;

            if (type.isTranslucent() && resortOverlays)
            {
                MeshData.SortState sortingData;
                VertexSorting sorter = VertexSorting.byDistance(x, y, z);

                if (!chunkMeshData.hasTransparentSortingDataForOverlay(type))
                {
                    sortingData = meshData.sortQuads(allocator, sorter);

                    if (sortingData == null)
                    {
                        throw new InterruptedException("Sort State failure");
                    }

                    chunkMeshData.setTransparentSortingDataForOverlay(type, sortingData);
                }
                else
                {
                    sortingData = chunkMeshData.getTransparentSortingDataForOverlay(type);
                }

                if (sortingData == null)
                {
                    throw new InterruptedException("Sorting Data failure");
                }
            }
        }
    }

    protected ChunkRenderTaskSchematic makeCompileTaskChunkSchematic(Supplier<Vec3> cameraPosSupplier)
    {
//        LOGGER.warn("[VBO] makeCompileTaskChunkSchematic()");
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
    protected ChunkRenderTaskSchematic makeCompileTaskTransparencySchematic(Supplier<Vec3> cameraPosSupplier)
    {
//        LOGGER.warn("[VBO] makeCompileTaskTransparencySchematic()");
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
                this.compileTask.updateChunkRenderData(this.chunkRenderData);

                return this.compileTask;
            }
        }
        finally
        {
            this.chunkRenderLock.unlock();
        }

        return null;
    }

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
        this.chunkRenderLock.lock();

        try
        {
            this.finishCompileTask();
        }
        finally
        {
            this.chunkRenderDataLock.lock();

            try
            {
                if (this.chunkRenderData != null && !this.chunkRenderData.equals(ChunkRenderDataSchematic.EMPTY))
                {
                    this.chunkRenderData.clearAll();
                }
            }
            finally
            {
                this.chunkRenderData = ChunkRenderDataSchematic.EMPTY;
                this.chunkRenderDataLock.unlock();
            }

            this.existingOverlays.clear();
            this.hasOverlay = false;

            this.chunkRenderLock.unlock();
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

    protected int getPartsCount()
    {
        int chunkX = this.position.getX() / 16;
        int chunkZ = this.position.getZ() / 16;

        return DataManager.getSchematicPlacementManager().getPlacementPartsInChunkCount(chunkX, chunkZ);
    }

    private void rebuildWorldView()
    {
        synchronized (this.boxes)
        {
            this.ignoreClientWorldFluids = Configs.Visuals.IGNORE_EXISTING_FLUIDS.getBooleanValue();
            this.ignoreBlockRegistry = new IgnoreBlockRegistry();
            ClientLevel worldClient = Minecraft.getInstance().level;
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
