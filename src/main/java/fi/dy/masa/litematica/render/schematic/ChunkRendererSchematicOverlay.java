package fi.dy.masa.litematica.render.schematic;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.util.OverlayType;
import fi.dy.masa.litematica.util.PositionUtils;

public class ChunkRendererSchematicOverlay implements AutoCloseable
{
    private final WorldRendererSchematic worldRenderer;
    private final ChunkCacheSchematic schematicWorldView;
    private final ChunkCacheSchematic clientWorldView;
    private Profiler profiler;

    protected final EnumSet<OverlayRenderType> existingOverlays = EnumSet.noneOf(OverlayRenderType.class);
    protected boolean hasOverlay = false;
    protected Color4f overlayColor;
    private boolean ignoreClientWorldFluids;

    protected ChunkRendererSchematicOverlay(WorldRendererSchematic worldRenderer,
                                            ChunkCacheSchematic schematicWorldView,
                                            ChunkCacheSchematic clientWorldView)
    {
        this.worldRenderer = worldRenderer;
        this.schematicWorldView = schematicWorldView;
        this.clientWorldView = clientWorldView;
        this.rebuild();
    }

    public boolean hasOverlay()
    {
        return this.hasOverlay;
    }

    public EnumSet<OverlayRenderType> getOverlayTypes()
    {
        return this.existingOverlays;
    }

    protected void rebuild()
    {
        this.clear();
        this.overlayColor = null;
        this.ignoreClientWorldFluids = Configs.Visuals.IGNORE_EXISTING_FLUIDS.getBooleanValue();
        this.profiler = Profilers.get();
    }

    protected void clear()
    {
        this.existingOverlays.clear();
        this.hasOverlay = false;
    }

    private BufferBuilder preRenderOverlay(OverlayRenderType type,
                                           @Nonnull BufferAllocatorCache allocators,
                                           @Nonnull BufferBuilderCache builderCache)
    {
        this.existingOverlays.add(type);
        this.hasOverlay = true;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        return builderCache.getBufferByOverlay(type, allocators);
    }

    protected void renderOverlays(BlockPos pos,
                                  BlockState stateSchematic,
                                  BlockState stateClient, boolean missing,
                                  BlockPos.Mutable relPos,
                                  @Nonnull ChunkRenderDataSchematic data,
                                  @Nonnull BufferAllocatorCache allocators,
                                  @Nonnull BufferBuilderCache builderCache)
    {
        OverlayType type = this.getOverlayType(stateSchematic, stateClient);
        this.overlayColor = getOverlayColor(type);
        OverlayRenderType overlayType;

        if (this.profiler == null)
        {
            this.profiler = Profilers.get();
        }
        /*
        Litematica.LOGGER.warn("renderOverlays(): sch: [{}], cli: [{}]", stateSchematic.toString(), stateClient.toString());
        Litematica.LOGGER.error(" // type: [{}], color: [{}]", type.name(), this.overlayColor != null ? this.overlayColor.toString() : "<>");
         */

        if (this.overlayColor != null)
        {
            if (stateSchematic.getFluidState().isEmpty() == false &&
                Configs.Visuals.ENABLE_SCHEMATIC_FLUIDS.getBooleanValue() == false)
            {
                return;
            }

            this.profiler.push(Reference.MOD_ID+"_render_overlays");
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_SIDES.getBooleanValue())
            {
                overlayType = OverlayRenderType.QUAD;
                BufferBuilder bufferOverlayQuads = builderCache.getBufferByOverlay(overlayType, allocators);

                if (data.isOverlayTypeStarted(overlayType) == false || bufferOverlayQuads == null)
                {
                    data.setOverlayTypeStarted(overlayType);
                    bufferOverlayQuads = this.preRenderOverlay(overlayType, allocators, builderCache);
                }

                this.renderOverlaySides(type, pos, relPos, stateSchematic, missing, bufferOverlayQuads);
            }

            if (Configs.Visuals.SCHEMATIC_OVERLAY_ENABLE_OUTLINES.getBooleanValue())
            {
                overlayType = OverlayRenderType.OUTLINE;
                BufferBuilder bufferOverlayOutlines = builderCache.getBufferByOverlay(overlayType, allocators);

                if (data.isOverlayTypeStarted(overlayType) == false || bufferOverlayOutlines == null)
                {
                    data.setOverlayTypeStarted(overlayType);
                    bufferOverlayOutlines = this.preRenderOverlay(overlayType, allocators, builderCache);
                }

                this.renderOverlayOutlines(type, pos, relPos, stateSchematic, missing, bufferOverlayOutlines);
            }
            this.profiler.pop();
        }
    }

    protected void renderOverlaySides(OverlayType type,
                                      BlockPos pos, BlockPos.Mutable relPos,
                                      BlockState stateSchematic,
                                      boolean missing,
                                      @Nonnull BufferBuilder bufferOverlayQuads)
    {
        boolean useDefault = false;

        this.profiler.push("overlay_sides");
        if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue())
        {
            BlockPos.Mutable posMutable = new BlockPos.Mutable();

            this.profiler.swap("overlay_sides_cull_inner_sides");
            for (int i = 0; i < 6; ++i)
            {
                Direction side = fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS[i];
                posMutable.set(pos.getX() + side.getOffsetX(), pos.getY() + side.getOffsetY(), pos.getZ() + side.getOffsetZ());
                BlockState adjStateSchematic = this.schematicWorldView.getBlockState(posMutable);
                BlockState adjStateClient    = this.clientWorldView.getBlockState(posMutable);
                OverlayType typeAdj = this.getOverlayType(adjStateSchematic, adjStateClient);

                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
                {
                    this.profiler.swap("overlay_sides_cull_render_model_sides");
                    BakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);

                    if (this.worldRenderer.hasQuadsForModel(bakedModel, stateSchematic, side))
                    {
                        if (type.getRenderPriority() > typeAdj.getRenderPriority() ||
                            !Block.isFaceFullSquare(stateSchematic.getCollisionShape(this.schematicWorldView, pos), side))
                        {
                            this.profiler.swap("overlay_sides_cull_render_model");
                            RenderUtils.drawBlockModelQuadOverlayBatched(bakedModel, stateSchematic, relPos, side, this.overlayColor, 0, bufferOverlayQuads);
                        }
                    }
                    else { useDefault = true; }
                }
                else { useDefault = true; }

                if (useDefault && type.getRenderPriority() > typeAdj.getRenderPriority())
                {
                    this.profiler.swap("overlay_sides_cull_render_default");
                    RenderUtils.drawBlockBoxSideBatchedQuads(relPos, side, this.overlayColor, 0, bufferOverlayQuads);
                }
            }
        }
        else
        {
            // Only render the model-based outlines or sides for missing blocks
            if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
            {
                this.profiler.swap("overlay_sides_render_model_sides");
                BakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);

                if (this.worldRenderer.hasQuadsForModel(bakedModel, stateSchematic, null))
                {
                    this.profiler.swap("overlay_sides_render_model");
                    RenderUtils.drawBlockModelQuadOverlayBatched(bakedModel, stateSchematic, relPos, this.overlayColor, 0, bufferOverlayQuads);
                }
                else { useDefault = true; }
            }
            else { useDefault = true; }

            if (useDefault)
            {
                this.profiler.swap("overlay_sides_render_batched");
                fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxSidesBatchedQuads(relPos, this.overlayColor, 0, bufferOverlayQuads);
            }
        }

        this.profiler.pop();
    }

    private void dumpBakedModelQuads(BakedModel model, BlockState state, Random rand)
    {
        Litematica.LOGGER.warn("dumpBakedModelQuads(): state: [{}]", state.toString());

        for (int i = 0; i < Direction.values().length; i++)
        {
            Direction face = Direction.byId(i);
            List<BakedQuad> list = model.getQuads(state, face, rand);
            int j = 0;

            Litematica.LOGGER.warn(" FACING[{}]: [{}] // Quad Size: [{}]", i, face.getName(), list.size());

            for (BakedQuad quad : list)
            {
                Litematica.LOGGER.warn("   QUAD[{}/{}]: face: [{}], atlas-sprite: [{}]", i, j, quad.getFace().getName(), quad.getSprite().toString());
                j++;
            }
        }
    }

    protected void renderOverlayOutlines(OverlayType type,
                                         BlockPos pos, BlockPos.Mutable relPos,
                                         BlockState stateSchematic,
                                         boolean missing,
                                         @Nonnull BufferBuilder bufferOverlayOutlines)
    {
        Color4f overlayColor = new Color4f(this.overlayColor.r, this.overlayColor.g, this.overlayColor.b, 1f);
        boolean useDefault = false;

        this.profiler.push("overlay_outlines");
        if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue())
        {
            /*
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
             */
            BlockPos.Mutable posMutable = new BlockPos.Mutable();

            this.profiler.swap("overlay_outlines_cull");
            for (int i = 0; i < 6; ++i)
            {
                Direction side = fi.dy.masa.malilib.util.position.PositionUtils.ALL_DIRECTIONS[i];
                posMutable.set(pos.getX() + side.getOffsetX(), pos.getY() + side.getOffsetY(), pos.getZ() + side.getOffsetZ());
                BlockState adjStateSchematic = this.schematicWorldView.getBlockState(posMutable);
                BlockState adjStateClient = this.clientWorldView.getBlockState(posMutable);
                OverlayType typeAdj = this.getOverlayType(adjStateSchematic, adjStateClient);

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
                        this.profiler.swap("overlay_outlines_cull_model");
                        if (type.getRenderPriority() > typeAdj.getRenderPriority() ||
                            !Block.isFaceFullSquare(stateSchematic.getCollisionShape(this.schematicWorldView, pos), side))
                        {
                            BakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);

                            if (this.worldRenderer.hasQuadsForModel(bakedModel, stateSchematic, side))
                            {
                                this.profiler.swap("overlay_outlines_cull_render_model");
                                //RenderUtils.drawBlockModelOutlinesBatched(bakedModel, stateSchematic, relPos, overlayColor, 0, bufferOverlayOutlines);
                                RenderUtils.renderModelQuadOutlines(bakedModel, stateSchematic, relPos, side, overlayColor, 0, bufferOverlayOutlines);
                            }
                            else { useDefault = true; }
                        }
                        else { useDefault = true; }
                    }
                }

                // FIXME --> this is quite broken / laggy
                if (useDefault)
                {
                    this.profiler.swap("overlay_outlines_cull_render_batched");
                    //this.renderOverlayReducedEdges(pos, relPos, adjTypes, type, bufferOverlayOutlines);
                    RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(pos, relPos, overlayColor, 0, bufferOverlayOutlines);
                }
            }
        }
        else
        {
            this.profiler.swap("overlay_outlines_render_batched");
            // Only render the model-based outlines or sides for missing blocks
            if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue())
            {
                BakedModel bakedModel = this.worldRenderer.getModelForState(stateSchematic);
                if (this.worldRenderer.hasQuadsForModel(bakedModel, stateSchematic, null))
                {
                    RenderUtils.drawBlockModelOutlinesBatched(bakedModel, stateSchematic, relPos, overlayColor, 0, bufferOverlayOutlines);
                }
                else { useDefault = true; }
            }
            else { useDefault = true; }
        }

        if (useDefault)
        {
            try
            {
                this.profiler.swap("overlay_outlines_render_batched_bounding_box");
                fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(relPos, overlayColor, 0, bufferOverlayOutlines);
            }
            catch (Exception ignored) { }
        }
        this.profiler.pop();
    }

    protected void renderOverlayReducedEdges(BlockPos pos, BlockPos.Mutable relPos, OverlayType[][][] adjTypes, OverlayType typeSelf, BufferBuilder bufferOverlayOutlines)
    {
        OverlayType[] neighborTypes = new OverlayType[4];
        Vec3i[] neighborPositions = new Vec3i[4];
        int lines = 0;

        this.profiler.push("overlay_reduced_edges");
        for (Direction.Axis axis : PositionUtils.AXES_ALL)
        {
            for (int corner = 0; corner < 4; ++corner)
            {
                Vec3i[] offsets = PositionUtils.getEdgeNeighborOffsets(axis, corner);
                int index = -1;
                boolean hasCurrent = false;

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

                this.profiler.swap("overlay_reduced_edges_plop");
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
                            this.profiler.swap("overlay_reduced_edges_batched");
                            RenderUtils.drawBlockBoxEdgeBatchedLines(relPos, axis, corner, overlayColor, bufferOverlayOutlines);
                        }
                        catch (IllegalStateException ignored) { }
                        lines++;
                    }
                }
            }
        }

        this.profiler.pop();
        //System.out.printf("typeSelf: %s, pos: %s, lines: %d\n", typeSelf, pos, lines);
    }

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
                return (clientHasAir || (this.ignoreClientWorldFluids && stateClient.isLiquid()))
                        ? OverlayType.NONE : OverlayType.EXTRA;
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
            case WRONG_STATE:
                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_STATE.getBooleanValue())
                {
                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_STATE.getColor();
                }
                break;
            case WRONG_BLOCK:
                if (Configs.Visuals.SCHEMATIC_OVERLAY_TYPE_WRONG_BLOCK.getBooleanValue())
                {
                    overlayColor = Configs.Colors.SCHEMATIC_OVERLAY_COLOR_WRONG_BLOCK.getColor();
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

    @Override
    public void close() throws Exception
    {
        this.rebuild();
    }
}
