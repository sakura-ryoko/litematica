package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.util.OverlayType;
import fi.dy.masa.litematica.util.PositionUtils;

public class OverlayModelRendererSchematic
{
    protected boolean renderQuads(BakedModel bakedModel, OverlayType type,
                                  BlockRenderView schematicWorldIn, BlockRenderView clientWorldIn,
                                  BlockState stateSchematic, BlockPos posIn, BlockPos relPos,
                                  Color4f color, BufferBuilder bufferBuilderIn,
                                  boolean missing, boolean ignoreFluids)
    {
        boolean rendered = false;

        if (Configs.Visuals.OVERLAY_REDUCED_INNER_SIDES.getBooleanValue())
        {
            BlockPos.Mutable posMutable = new BlockPos.Mutable();

            for (Direction side : fi.dy.masa.malilib.util.PositionUtils.ALL_DIRECTIONS)
            {
                posMutable.set(posIn.getX() + side.getOffsetX(), posIn.getY() + side.getOffsetY(), posIn.getZ() + side.getOffsetZ());
                BlockState adjStateSchematic = schematicWorldIn.getBlockState(posMutable);
                BlockState adjStateClient    = clientWorldIn.getBlockState(posMutable);
                OverlayType typeAdj = getOverlayType(adjStateSchematic, adjStateClient, ignoreFluids);

                // Only render the model-based outlines or sides for missing blocks
                if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
                {
                    if (type.getRenderPriority() > typeAdj.getRenderPriority() ||
                        !Block.isFaceFullSquare(stateSchematic.getCollisionShape(schematicWorldIn, posIn), side))
                        //this.shouldDrawSides(stateSchematic, schematicWorldIn, posIn, side, posMutable))
                    {
                        RenderUtils.drawBlockModelQuadOverlayBatched(bakedModel, stateSchematic, relPos, side, color, 0, bufferBuilderIn);
                        rendered = true;
                    }
                }
                else
                {
                    if (type.getRenderPriority() > typeAdj.getRenderPriority())
                    {
                        RenderUtils.drawBlockBoxSideBatchedQuads(relPos, side, color, 0, bufferBuilderIn);
                        rendered = true;
                    }
                }
            }
        }
        else
        {
            // Only render the model-based outlines or sides for missing blocks
            if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_SIDES.getBooleanValue())
            {
                RenderUtils.drawBlockModelQuadOverlayBatched(bakedModel, stateSchematic, relPos, color, 0, bufferBuilderIn);
                rendered = true;
            }
            else
            {
                try
                {
                    fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxSidesBatchedQuads(relPos, color, 0, bufferBuilderIn);
                    rendered = true;
                }
                catch (Exception ignored) { }
            }
        }

        return rendered;
    }

    protected boolean renderOutlines(BakedModel bakedModel, OverlayType type,
                                     BlockRenderView schematicWorldIn, BlockRenderView clientWorldIn,
                                     BlockState stateSchematic, BlockPos posIn, BlockPos relPos,
                                     Color4f color, BufferBuilder bufferBuilderIn,
                                     boolean missing, boolean ignoreFluids)
    {
        Color4f overlayColor = new Color4f(color.r, color.g, color.b, 1f);
        boolean rendered = false;

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
                            posMutable.set(posIn.getX() + x - 1, posIn.getY() + y - 1, posIn.getZ() + z - 1);
                            BlockState adjStateSchematic = schematicWorldIn.getBlockState(posMutable);
                            BlockState adjStateClient    = clientWorldIn.getBlockState(posMutable);
                            adjTypes[x][y][z] = getOverlayType(adjStateSchematic, adjStateClient, ignoreFluids);
                        }
                        else
                        {
                            adjTypes[x][y][z] = type;
                        }
                    }
                }
            }

            // Only render the model-based outlines or sides for missing blocks
            if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue())
            {
                // FIXME: how to implement this correctly here... >_>
                if (stateSchematic.isOpaque())
                {
                    this.renderOverlayReducedEdges(posIn, adjTypes, type, relPos, overlayColor, bufferBuilderIn);
                }
                else
                {
                    RenderUtils.drawBlockModelOutlinesBatched(bakedModel, stateSchematic, relPos, overlayColor, 0, bufferBuilderIn);
                }
                rendered = true;
            }
            else
            {
                this.renderOverlayReducedEdges(posIn, adjTypes, type, relPos, overlayColor, bufferBuilderIn);
                rendered = true;
            }
        }
        else
        {
            // Only render the model-based outlines or sides for missing blocks
            if (missing && Configs.Visuals.SCHEMATIC_OVERLAY_MODEL_OUTLINE.getBooleanValue())
            {
                RenderUtils.drawBlockModelOutlinesBatched(bakedModel, stateSchematic, relPos, overlayColor, 0, bufferBuilderIn);
                rendered = true;
            }
            else
            {
                try
                {
                    fi.dy.masa.malilib.render.RenderUtils.drawBlockBoundingBoxOutlinesBatchedLines(relPos, overlayColor, 0, bufferBuilderIn);
                    rendered = true;
                }
                catch (Exception ignored) { }
            }
        }

        return rendered;
    }

    private void renderOverlayReducedEdges(BlockPos pos, OverlayType[][][] adjTypes, OverlayType typeSelf,
                                           BlockPos relPos, Color4f overlayColor, BufferBuilder bufferOverlayOutlines)
    {
        OverlayType[] neighborTypes = new OverlayType[4];
        Vec3i[] neighborPositions = new Vec3i[4];
        int lines = 0;

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
                            RenderUtils.drawBlockBoxEdgeBatchedLines(relPos, axis, corner, overlayColor, bufferOverlayOutlines);
                        }
                        catch (IllegalStateException err)
                        {
                            return;
                        }
                        lines++;
                    }
                }
            }
        }
        //System.out.printf("typeSelf: %s, pos: %s, lines: %d\n", typeSelf, pos, lines);
    }

    protected static OverlayType getOverlayType(BlockState stateSchematic, BlockState stateClient, boolean ignoreClientWorldFluids)
    {
        if (stateSchematic == stateClient)
        {
            return OverlayType.NONE;
        }
        else
        {
            boolean clientHasAir = stateClient.isAir();
            boolean schematicHasAir = stateSchematic.isAir();

            // TODO --> Maybe someday Mojang will add something to replace isLiquid(), and isSolid(), someday?
            if (schematicHasAir)
            {
                return (clientHasAir || (ignoreClientWorldFluids && stateClient.isLiquid())) ? OverlayType.NONE : OverlayType.EXTRA;
            }
            else
            {
                if (clientHasAir || (ignoreClientWorldFluids && stateClient.isLiquid()))
                {
                    return OverlayType.MISSING;
                }
                // Wrong block
                else if (stateSchematic.getBlock() != stateClient.getBlock())
                {
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
            default:
        }

        return overlayColor;
    }

    private static final ThreadLocal<Object2ByteLinkedOpenHashMap<Block.NeighborGroup>> CULL_MAP = ThreadLocal.withInitial(() ->
    {
        Object2ByteLinkedOpenHashMap<Block.NeighborGroup> object2ByteLinkedOpenHashMap = new Object2ByteLinkedOpenHashMap<>(2048, 0.25f)
        {
            @Override
            protected void rehash(int newN) { }
        };
        object2ByteLinkedOpenHashMap.defaultReturnValue((byte) 127);
        return object2ByteLinkedOpenHashMap;
    });

    private boolean shouldDrawSides(BlockState stateIn, BlockView schematicWorld, BlockPos posIn, Direction side, BlockPos relPos)
    {
        BlockState relState = schematicWorld.getBlockState(relPos);

        if (stateIn.isSideInvisible(relState, side))
        {
            return false;
        }
        if (relState.isOpaque())
        {
            Block.NeighborGroup neighborGroup = new Block.NeighborGroup(stateIn, relState, side);
            Object2ByteLinkedOpenHashMap<Block.NeighborGroup> object2ByteLinkedOpenHashMap = CULL_MAP.get();
            byte b = object2ByteLinkedOpenHashMap.getAndMoveToFirst(neighborGroup);

            if (b != 127)
            {
                return b != 0;
            }

            VoxelShape voxel = stateIn.getCullingFace(schematicWorld, posIn, side);
            if (voxel.isEmpty())
            {
                return true;
            }

            VoxelShape voxelRel = relState.getCullingFace(schematicWorld, relPos, side.getOpposite());
            boolean block = VoxelShapes.matchesAnywhere(voxel, voxelRel, BooleanBiFunction.ONLY_FIRST);

            if (object2ByteLinkedOpenHashMap.size() == 2048)
            {
                object2ByteLinkedOpenHashMap.removeLastByte();
            }
            object2ByteLinkedOpenHashMap.putAndMoveToFirst(neighborGroup, (byte)(block ? 1 : 0));

            return block;
        }

        return true;
    }
}
