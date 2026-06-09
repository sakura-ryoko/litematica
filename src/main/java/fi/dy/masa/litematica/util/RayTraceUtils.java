package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.malilib.util.position.LayerRange;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

public class RayTraceUtils
{
    private static final net.minecraft.world.phys.AABB FULL_BLOCK_BOUNDS = new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1);

    private static RayTraceWrapper closestBox;
    private static RayTraceWrapper closestCorner;
    private static RayTraceWrapper closestOrigin;
    private static double closestBoxDistance;
    private static double closestCornerDistance;
    private static double closestOriginDistance;
    private static HitType originType;

    @Nullable
    public static BlockPos getTargetedPosition(Level world, Entity player, double maxDistance, boolean sneakToOffset)
    {
        HitResult trace = getRayTraceFromEntity(world, player, false, maxDistance);

        if (trace.getType() != HitResult.Type.BLOCK)
        {
            return null;
        }

        BlockHitResult traceBlock = (BlockHitResult) trace;
        BlockPos pos = traceBlock.getBlockPos();

        // Sneaking puts the position adjacent the targeted block face, not sneaking puts it inside the targeted block
        if (sneakToOffset == player.isShiftKeyDown())
        {
            pos = pos.relative(traceBlock.getDirection());
        }

        return pos;
    }

    @Nonnull
    public static RayTraceWrapper getWrappedRayTraceFromEntity(Level world, Entity entity, double range)
    {
        Vec3 eyesPos = entity.getEyePosition(1f);
        Vec3 rangedLookRot = entity.getViewVector(1f).scale(range);
        Vec3 lookEndPos = eyesPos.add(rangedLookRot);

        HitResult result = getRayTraceFromEntity(world, entity, false, range);
        double closestVanilla = result.getType() != HitResult.Type.MISS ? result.getLocation().distanceTo(eyesPos) : -1D;

        AreaSelection area = DataManager.getSelectionManager().getCurrentSelection();
        RayTraceWrapper wrapper = null;

        clearTraceVars();

        if (DataManager.getToolMode().getUsesSchematic() == false && area != null)
        {
            for (Box box : area.getAllSubRegionBoxes())
            {
                boolean hitCorner = false;
                hitCorner |= traceToSelectionBoxCorner(box, Corner.CORNER_1, eyesPos, lookEndPos);
                hitCorner |= traceToSelectionBoxCorner(box, Corner.CORNER_2, eyesPos, lookEndPos);

                if (hitCorner == false)
                {
                    traceToSelectionBoxBody(box, eyesPos, lookEndPos);
                }
            }

            BlockPos origin = area.getExplicitOrigin();

            if (origin != null)
            {
                traceToOrigin(origin, eyesPos, lookEndPos, HitType.SELECTION_ORIGIN, null);
            }
        }

        if (DataManager.getToolMode().getUsesSchematic())
        {
            for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements())
            {
                if (placement.isEnabled())
                {
                    traceToPlacementBox(placement, eyesPos, lookEndPos);
                    traceToOrigin(placement.getOrigin(), eyesPos, lookEndPos, HitType.PLACEMENT_ORIGIN, placement);
                }
            }
        }

        double closestDistance = closestVanilla;

        if (closestBoxDistance >= 0 && (closestVanilla < 0 || closestBoxDistance <= closestVanilla))
        {
            closestDistance = closestBoxDistance;
            wrapper = closestBox;
        }

        // Corners are preferred over box body hits, thus this being after the box check
        if (closestCornerDistance >= 0 && (closestVanilla < 0 || closestCornerDistance <= closestVanilla))
        {
            closestDistance = closestCornerDistance;
            wrapper = closestCorner;
        }

        // Origins are preferred over everything else
        if (closestOriginDistance >= 0 && (closestVanilla < 0 || closestOriginDistance <= closestVanilla))
        {
            closestDistance = closestOriginDistance;

            if (originType == HitType.PLACEMENT_ORIGIN)
            {
                wrapper = closestOrigin;
            }
            else
            {
                wrapper = new RayTraceWrapper(RayTraceWrapper.HitType.SELECTION_ORIGIN);
            }
        }

        clearTraceVars();

        if (wrapper == null || closestDistance < 0)
        {
            wrapper = new RayTraceWrapper();
        }

        return wrapper;
    }

    private static void clearTraceVars()
    {
        closestBox = null;
        closestCorner = null;
        closestOrigin = null;
        closestBoxDistance = -1D;
        closestCornerDistance = -1D;
        closestOriginDistance = -1D;
    }

    private static boolean traceToSelectionBoxCorner(Box box, Corner corner, Vec3 start, Vec3 end)
    {
        BlockPos pos = (corner == Corner.CORNER_1) ? box.getPos1() : (corner == Corner.CORNER_2) ? box.getPos2() : null;

        if (pos != null)
        {
            net.minecraft.world.phys.AABB bb = PositionUtils.createAABBForPosition(pos);
            Optional<Vec3> optional = bb.clip(start, end);

            if (optional.isPresent())
            {
                double dist = optional.get().distanceTo(start);

                if (closestCornerDistance < 0 || dist < closestCornerDistance)
                {
                    closestCornerDistance = dist;
                    closestCorner = new RayTraceWrapper(box, corner, optional.get());
                }

                return true;
            }
        }

        return false;
    }

    private static boolean traceToSelectionBoxBody(Box box, Vec3 start, Vec3 end)
    {
        if (box.getPos1() != null && box.getPos2() != null)
        {
            net.minecraft.world.phys.AABB bb = PositionUtils.createEnclosingAABB(box.getPos1(), box.getPos2());
            Optional<Vec3> optional = bb.clip(start, end);

            if (optional.isPresent())
            {
                double dist = optional.get().distanceTo(start);

                if (closestBoxDistance < 0 || dist < closestBoxDistance)
                {
                    closestBoxDistance = dist;
                    closestBox = new RayTraceWrapper(box, Corner.NONE, optional.get());
                }

                return true;
            }
        }

        return false;
    }

    private static boolean traceToPlacementBox(SchematicPlacement placement, Vec3 start, Vec3 end)
    {
        ImmutableMap<String, Box> boxes = placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        boolean hitSomething = false;

        for (Map.Entry<String, Box> entry : boxes.entrySet())
        {
            String boxName = entry.getKey();
            Box box = entry.getValue();

            if (box.getPos1() != null && box.getPos2() != null)
            {
                net.minecraft.world.phys.AABB bb = PositionUtils.createEnclosingAABB(box.getPos1(), box.getPos2());
                Optional<Vec3> optional = bb.clip(start, end);

                if (optional.isPresent())
                {
                    double dist = optional.get().distanceTo(start);

                    if (closestBoxDistance < 0 || dist < closestBoxDistance)
                    {
                        closestBoxDistance = dist;
                        closestBox = new RayTraceWrapper(placement, optional.get(), boxName);
                        hitSomething = true;
                    }
                }
            }
        }

        return hitSomething;
    }

    private static boolean traceToOrigin(BlockPos pos, Vec3 start, Vec3 end, HitType type, @Nullable SchematicPlacement placement)
    {
        if (pos != null)
        {
            net.minecraft.world.phys.AABB bb = PositionUtils.createAABBForPosition(pos);
            Optional<Vec3> optional = bb.clip(start, end);

            if (optional.isPresent())
            {
                double dist = optional.get().distanceTo(start);

                if (closestOriginDistance < 0 || dist < closestOriginDistance)
                {
                    closestOriginDistance = dist;
                    originType = type;

                    if (type == HitType.PLACEMENT_ORIGIN)
                    {
                        closestOrigin = new RayTraceWrapper(placement, optional.get(), null);
                    }

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Ray traces to the closest position on the given list
     * @param posList ()
     * @param entity ()
     * @param range ()
     * @return ()
     */
    @Nullable
    public static BlockHitResult traceToPositions(List<BlockPos> posList, Entity entity, double range)
    {
        if (posList.isEmpty())
        {
            return null;
        }

        Vec3 eyesPos = entity.getEyePosition(1f);
        Vec3 rangedLookRot = entity.getViewVector(1f).scale(range);
        Vec3 lookEndPos = eyesPos.add(rangedLookRot);

        double closest = -1D;
        BlockHitResult trace = null;

        for (BlockPos pos : posList)
        {
            if (pos != null)
            {
                BlockHitResult hit = net.minecraft.world.phys.AABB.clip(ImmutableList.of(FULL_BLOCK_BOUNDS), eyesPos, lookEndPos, pos);

                if (hit != null)
                {
                    double dist = hit.getLocation().distanceTo(eyesPos);

                    if (closest < 0 || dist < closest)
                    {
                        trace = new BlockHitResult(hit.getLocation(), hit.getDirection(), pos, false);
                        closest = dist;
                    }
                }
            }
        }

        return trace;
    }

    @Nullable
    public static BlockHitResult traceToSchematicWorld(Entity entity, double range,
                                                       boolean respectRenderRange, boolean targetFluids)
    {
        boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();

        if (respectRenderRange &&
            (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() == false ||
             Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() == invert))
        {
            return null;
        }

        Level world = SchematicWorldHandler.getSchematicWorld();

        if (world == null)
        {
            return null;
        }

        Vec3 eyesPos = entity.getEyePosition(1f);
        Vec3 rangedLookRot = entity.getViewVector(1f).scale(range);
        Vec3 lookEndPos = eyesPos.add(rangedLookRot);
        ClipContext.Fluid fluidMode = targetFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;

        return rayTraceBlocks(world, eyesPos, lookEndPos, fluidMode, false, true, respectRenderRange, 200);
    }

    @Nullable
    public static RayTraceWrapper getGenericTrace(Level worldClient, Entity entity, double range)
    {
        return getGenericTrace(worldClient, entity, range, true, true, false);
    }

    @Nullable
    public static RayTraceWrapper getGenericTraceNoFluids(Level worldClient, Entity entity, double range)
    {
        return getGenericTrace(worldClient, entity, range, true, false, false);
    }

    @Nullable
    public static RayTraceWrapper getGenericTrace(Level worldClient, Entity entity,
                                                  double range, boolean respectRenderRange,
                                                  boolean targetFluids, boolean includeVerifier)
    {
        HitResult traceClient = getRayTraceFromEntity(worldClient, entity, targetFluids, range);
        HitResult traceSchematic = traceToSchematicWorld(entity, range, respectRenderRange, targetFluids);
        double distClosest = -1D;
        HitType type = HitType.MISS;
        Vec3 eyesPos = entity.getEyePosition(1f);
        HitResult trace = null;

        if (traceSchematic != null && traceSchematic.getType() == HitResult.Type.BLOCK)
        {
            double dist = eyesPos.distanceToSqr(traceSchematic.getLocation());

            if (distClosest < 0 || dist < distClosest)
            {
                trace = traceSchematic;
                distClosest = eyesPos.distanceToSqr(traceSchematic.getLocation());
                type = HitType.SCHEMATIC_BLOCK;
            }
        }

        if (traceClient != null && traceClient.getType() == HitResult.Type.BLOCK)
        {
            double dist = eyesPos.distanceToSqr(traceClient.getLocation());

            if (distClosest < 0 || dist < distClosest)
            {
                trace = traceClient;
                type = HitType.VANILLA_BLOCK;
            }
        }

        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (includeVerifier && placement != null && placement.hasVerifier())
        {
            SchematicVerifier verifier = placement.getSchematicVerifier();
            List<BlockPos> posList = verifier.getSelectedMismatchBlockPositionsForRender();
            BlockHitResult traceMismatch = traceToPositions(posList, entity, range);

            // Mismatch overlay has priority over other hits
            if (traceMismatch != null)
            {
                trace = traceMismatch;
                type = HitType.MISMATCH_OVERLAY;
            }
        }

        if (type != HitType.MISS)
        {
            return new RayTraceWrapper(type, (BlockHitResult) trace);
        }

        return null;
    }

    @Nullable
    public static RayTraceWrapper getSchematicWorldTraceWrapperIfClosest(Level worldClient, Entity entity, double range)
    {
        RayTraceWrapper trace = getGenericTrace(worldClient, entity, range);

        if (trace != null && trace.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            return trace;
        }

        return null;
    }

    @Nullable
    public static BlockPos getSchematicWorldTraceIfClosest(Level worldClient, Entity entity, double range)
    {
        RayTraceWrapper trace = getSchematicWorldTraceWrapperIfClosest(worldClient, entity, range);
        return trace != null && trace.getHitType() == HitType.SCHEMATIC_BLOCK ? trace.getBlockHitResult().getBlockPos() : null;
    }

    @Nullable
    public static BlockPos getSchematicWorldTraceIfClosestNoFluids(Level worldClient, Entity entity, double range)
    {
        RayTraceWrapper trace = getSchematicWorldTraceWrapperIfClosestNoFluids(worldClient, entity, range);
        return trace != null && trace.getHitType() == HitType.SCHEMATIC_BLOCK ? trace.getBlockHitResult().getBlockPos() : null;
    }

    @Nullable
    public static RayTraceWrapper getSchematicWorldTraceWrapperIfClosestNoFluids(Level worldClient, Entity entity, double range)
    {
        RayTraceWrapper trace = getGenericTraceNoFluids(worldClient, entity, range);

        if (trace != null && trace.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            return trace;
        }

        return null;
    }

    @Nullable
    public static BlockPos getFurthestSchematicWorldBlockBeforeVanilla(Level worldClient,
                                                                       Entity entity,
                                                                       double maxRange,
                                                                       boolean requireVanillaBlockBehind)
    {
        Vec3 eyesPos = entity.getEyePosition(1f);
        Vec3 rangedLookRot = entity.getViewVector(1f).scale(maxRange);
        Vec3 lookEndPos = eyesPos.add(rangedLookRot);

        @Nullable BlockPos closestVanillaPos = null;
        @Nullable Direction side = null;
        double closestVanilla = -1.0;

        HitResult traceVanilla = getRayTraceFromEntity(worldClient, entity, false, maxRange);

        if (traceVanilla.getType() == HitResult.Type.BLOCK)
        {
            closestVanilla = traceVanilla.getLocation().distanceToSqr(eyesPos);
            BlockHitResult vanillaHitResult = (BlockHitResult) traceVanilla;
            side = vanillaHitResult.getDirection();
            closestVanillaPos = vanillaHitResult.getBlockPos();
        }
        else if (requireVanillaBlockBehind)
        {
            return null;
        }

        Level worldSchematic = SchematicWorldHandler.getSchematicWorld();
        List<BlockHitResult> list = rayTraceBlocksToList(worldSchematic, eyesPos, lookEndPos, ClipContext.Fluid.NONE, false, false, true, 200);
        BlockHitResult furthestTrace = null;
        double furthestDist = -1.0;

        if (list.isEmpty() == false)
        {
            for (BlockHitResult trace : list)
            {
                double dist = trace.getLocation().distanceToSqr(eyesPos);

                if ((furthestDist < 0 || dist > furthestDist) &&
                    (dist < closestVanilla || closestVanilla < 0) &&
                    trace.getBlockPos().equals(closestVanillaPos) == false)
                {
                    furthestDist = dist;
                    furthestTrace = trace;
                }

                if (closestVanilla >= 0 && dist > closestVanilla)
                {
                    break;
                }
            }
        }

        // Didn't trace to any schematic blocks, but hit a vanilla block.
        // Check if there is a schematic block adjacent to the vanilla block
        // (which means that it has a non-full-cube collision box, since
        // it wasn't hit by the trace), and no block in the client world.
        // Note that this method is only used for the "pickBlockLast" type
        // of pick blocking, not for the "first" variant, where this would
        // probably be annoying if you want to pick block the client world block.
        if (furthestTrace == null && side != null && closestVanillaPos != null)
        {
            BlockPos pos = closestVanillaPos.relative(side);
            LayerRange layerRange = DataManager.getRenderLayerRange();

            if (layerRange.isPositionWithinRange(pos) &&
                worldSchematic.getBlockState(pos).isAir() == false &&
                worldClient.getBlockState(pos).isAir())
            {
                return pos;
            }
        }

        return furthestTrace != null ? furthestTrace.getBlockPos() : null;
    }

    @Nullable
    public static RayTraceWrapper getFurthestSchematicWorldTraceBeforeVanilla(Level worldClient,
                                                                              Entity entity,
                                                                              double maxRange)
    {
        Vec3 eyesPos = entity.getEyePosition(1f);
        Vec3 rangedLookRot = entity.getViewVector(1f).scale(maxRange);
        Vec3 lookEndPos = eyesPos.add(rangedLookRot);

        @Nullable BlockPos closestVanillaPos = null;
        double closestVanilla = -1.0;

        HitResult traceVanilla = getRayTraceFromEntity(worldClient, entity, false, maxRange);

        if (traceVanilla.getType() == HitResult.Type.BLOCK)
        {
            closestVanilla = traceVanilla.getLocation().distanceToSqr(eyesPos);
            BlockHitResult vanillaHitResult = (BlockHitResult) traceVanilla;
            closestVanillaPos = vanillaHitResult.getBlockPos();
        }

        Level worldSchematic = SchematicWorldHandler.getSchematicWorld();
        List<BlockHitResult> list = rayTraceBlocksToList(worldSchematic, eyesPos, lookEndPos, ClipContext.Fluid.NONE, false, false, true, 200);
        BlockHitResult furthestTrace = null;
        double furthestDist = -1.0;

        if (list.isEmpty() == false)
        {
            for (BlockHitResult trace : list)
            {
                double dist = trace.getLocation().distanceToSqr(eyesPos);

                if ((furthestDist < 0 || dist > furthestDist) &&
                    (dist < closestVanilla || closestVanilla < 0) &&
                    trace.getBlockPos().equals(closestVanillaPos) == false)
                {
                    furthestDist = dist;
                    furthestTrace = trace;
                }

                if (closestVanilla >= 0 && dist > closestVanilla)
                {
                    break;
                }
            }
        }

        return furthestTrace != null ? new RayTraceWrapper(HitType.SCHEMATIC_BLOCK, furthestTrace) : null;
    }

    @Nonnull
    public static HitResult getRayTraceFromEntity(Level worldIn, Entity entityIn, boolean useLiquids)
    {
        double reach = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.entityInteractionRange() + 0.5d : 5.0d;
        return getRayTraceFromEntity(worldIn, entityIn, useLiquids, reach);
    }

    @Nonnull
    public static HitResult getRayTraceFromEntity(Level world, Entity entity, boolean useLiquids, double range)
    {
        Vec3 eyesPos = entity.getEyePosition(1f);
        Vec3 rangedLookRot = entity.getViewVector(1f).scale(range);
        Vec3 lookEndPos = eyesPos.add(rangedLookRot);
        ClipContext.Fluid fluidMode = useLiquids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;

        HitResult result = rayTraceBlocks(world, eyesPos, lookEndPos, fluidMode, false, false, false, 1000);

        net.minecraft.world.phys.AABB bb = entity.getBoundingBox().inflate(rangedLookRot.x, rangedLookRot.y, rangedLookRot.z).inflate(1d, 1d, 1d);
        List<Entity> list = world.getEntities(entity, bb);

        double closest = result != null && result.getType() == HitResult.Type.BLOCK ? eyesPos.distanceTo(result.getLocation()) : Double.MAX_VALUE;
        Entity targetEntity = null;
        Optional<Vec3> optional = Optional.empty();

        for (int i = 0; i < list.size(); i++)
        {
            Entity entityTmp = list.get(i);
            Optional<Vec3> optionalTmp = entityTmp.getBoundingBox().clip(eyesPos, lookEndPos);

            if (optionalTmp.isPresent())
            {
                double distance = eyesPos.distanceTo(optionalTmp.get());

                if (distance <= closest)
                {
                    targetEntity = entityTmp;
                    optional = optionalTmp;
                    closest = distance;
                }
            }
        }

        if (targetEntity != null)
        {
            result = new EntityHitResult(targetEntity, optional.get());
        }

        if (result == null || eyesPos.distanceTo(result.getLocation()) > range)
        {
            result = BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO);
        }

        return result;
    }

    /**
     * Mostly copy pasted from World#rayTraceBlocks() except for the added maxSteps argument and the layer range check
     */
    @Nullable
    public static BlockHitResult rayTraceBlocks(Level world, Vec3 start, Vec3 end,
            ClipContext.Fluid fluidMode, boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange, int maxSteps)
    {
        if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z) ||
            Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z))
        {
            return null;
        }

        LayerRange range = DataManager.getRenderLayerRange();
        RayTraceCalcsData data = new RayTraceCalcsData(start, end, range, fluidMode);

        BlockState blockState = world.getBlockState(data.blockPos);
        FluidState fluidState = world.getFluidState(data.blockPos);

        BlockHitResult trace = traceFirstStep(data, world, blockState, fluidState, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, respectLayerRange);

        if (trace != null)
        {
            return trace;
        }

        while (--maxSteps >= 0)
        {
            if (rayTraceCalcs(data, returnLastUncollidableBlock, respectLayerRange))
            {
                return data.trace;
            }

            blockState = world.getBlockState(data.blockPos);
            fluidState = world.getFluidState(data.blockPos);

            if (traceLoopSteps(data, world, blockState, fluidState, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, respectLayerRange))
            {
                return data.trace;
            }
        }

        return returnLastUncollidableBlock ? data.trace : null;
    }

    @Nullable
    private static BlockHitResult traceFirstStep(RayTraceCalcsData data,
            Level world, BlockState blockState, FluidState fluidState,
            boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange)
    {
        if ((respectLayerRange == false || data.range.isPositionWithinRange(data.x, data.y, data.z)) &&
            (ignoreBlockWithoutBoundingBox == false || blockState.getCollisionShape(world, data.blockPos).isEmpty() == false))
        {
            VoxelShape blockShape = blockState.getShape(world, data.blockPos, CollisionContext.of(Minecraft.getInstance().player));
            boolean blockCollidable = ! blockShape.isEmpty();
            boolean fluidCollidable = data.fluidMode.canPick(fluidState);

            if (blockCollidable || fluidCollidable)
            {
                BlockHitResult trace = null;

                if (blockCollidable)
                {
                    trace = blockShape.clip(data.start, data.end, data.blockPos);
                }

                if (trace == null && fluidCollidable)
                {
                    trace = fluidState.getShape(world, data.blockPos).clip(data.start, data.end, data.blockPos);
                }

                if (trace != null)
                {
                    return trace;
                }
            }
        }

        return null;
    }

    private static boolean traceLoopSteps(RayTraceCalcsData data,
            Level world, BlockState blockState, FluidState fluidState,
            boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange)
    {
        if ((respectLayerRange == false || data.range.isPositionWithinRange(data.x, data.y, data.z)) &&
                (ignoreBlockWithoutBoundingBox == false || blockState.getBlock() == Blocks.NETHER_PORTAL ||
                blockState.getBlock() == Blocks.END_PORTAL || blockState.getBlock() == Blocks.END_GATEWAY ||
             blockState.getCollisionShape(world, data.blockPos).isEmpty() == false))
        {
            VoxelShape blockShape = blockState.getShape(world, data.blockPos, CollisionContext.of(Minecraft.getInstance().player));
            boolean blockCollidable = ! blockShape.isEmpty();
            boolean fluidCollidable = data.fluidMode.canPick(fluidState);

            if (blockCollidable == false && fluidCollidable == false)
            {
                Vec3 pos = new Vec3(data.currentX, data.currentY, data.currentZ);
                data.trace = BlockHitResult.miss(pos, data.facing, data.blockPos);
            }
            else
            {
                BlockHitResult traceTmp = null;

                if (blockCollidable)
                {
                    traceTmp = blockShape.clip(data.start, data.end, data.blockPos);
                }

                if (traceTmp == null && fluidCollidable)
                {
                    traceTmp = fluidState.getShape(world, data.blockPos).clip(data.start, data.end, data.blockPos);
                }

                if (traceTmp != null)
                {
                    data.trace = traceTmp;
                    return true;
                }
            }
        }

        return false;
    }

    public static List<BlockHitResult> rayTraceBlocksToList(Level world, Vec3 start, Vec3 end,
            ClipContext.Fluid fluidMode, boolean ignoreBlockWithoutBoundingBox,
            boolean returnLastUncollidableBlock, boolean respectLayerRange, int maxSteps)
    {
        if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z) ||
            Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z))
        {
            return ImmutableList.of();
        }

        LayerRange range = DataManager.getRenderLayerRange();
        RayTraceCalcsData data = new RayTraceCalcsData(start, end, range, fluidMode);

        BlockState blockState = world.getBlockState(data.blockPos);
        FluidState fluidState = world.getFluidState(data.blockPos);

        BlockHitResult trace = traceFirstStep(data, world, blockState, fluidState, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, respectLayerRange);
        List<BlockHitResult> hits = new ArrayList<>();

        if (trace != null)
        {
            hits.add(trace);
        }

        while (--maxSteps >= 0)
        {
            if (rayTraceCalcs(data, returnLastUncollidableBlock, respectLayerRange))
            {
                if (data.trace != null)
                {
                    hits.add(data.trace);
                }

                return hits;
            }

            blockState = world.getBlockState(data.blockPos);
            fluidState = world.getFluidState(data.blockPos);

            if (traceLoopSteps(data, world, blockState, fluidState, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock, respectLayerRange))
            {
                hits.add(data.trace);
            }
        }

        return hits;
    }

    private static boolean rayTraceCalcs(RayTraceCalcsData data, boolean returnLastNonCollidableBlock, boolean respectLayerRange)
    {
        boolean xDiffers = true;
        boolean yDiffers = true;
        boolean zDiffers = true;
        double nextX = 999.0D;
        double nextY = 999.0D;
        double nextZ = 999.0D;

        if (Double.isNaN(data.currentX) || Double.isNaN(data.currentY) || Double.isNaN(data.currentZ))
        {
            data.trace = null;
            return true;
        }

        if (data.x == data.xEnd && data.y == data.yEnd && data.z == data.zEnd)
        {
            if (returnLastNonCollidableBlock == false)
            {
                data.trace = null;
            }

            return true;
        }

        if (data.xEnd > data.x)
        {
            nextX = (double) data.x + 1.0D;
        }
        else if (data.xEnd < data.x)
        {
            nextX = (double) data.x + 0.0D;
        }
        else
        {
            xDiffers = false;
        }

        if (data.yEnd > data.y)
        {
            nextY = (double) data.y + 1.0D;
        }
        else if (data.yEnd < data.y)
        {
            nextY = (double) data.y + 0.0D;
        }
        else
        {
            yDiffers = false;
        }

        if (data.zEnd > data.z)
        {
            nextZ = (double) data.z + 1.0D;
        }
        else if (data.zEnd < data.z)
        {
            nextZ = (double) data.z + 0.0D;
        }
        else
        {
            zDiffers = false;
        }

        double relStepX = 999.0D;
        double relStepY = 999.0D;
        double relStepZ = 999.0D;
        double distToEndX = data.end.x - data.currentX;
        double distToEndY = data.end.y - data.currentY;
        double distToEndZ = data.end.z - data.currentZ;

        if (xDiffers)
        {
            relStepX = (nextX - data.currentX) / distToEndX;
        }

        if (yDiffers)
        {
            relStepY = (nextY - data.currentY) / distToEndY;
        }

        if (zDiffers)
        {
            relStepZ = (nextZ - data.currentZ) / distToEndZ;
        }

        if (relStepX == -0.0D)
        {
            relStepX = -1.0E-4D;
        }

        if (relStepY == -0.0D)
        {
            relStepY = -1.0E-4D;
        }

        if (relStepZ == -0.0D)
        {
            relStepZ = -1.0E-4D;
        }

        if (relStepX < relStepY && relStepX < relStepZ)
        {
            data.facing = data.xEnd > data.x ? Direction.WEST : Direction.EAST;
            data.currentX = nextX;
            data.currentY += distToEndY * relStepX;
            data.currentZ += distToEndZ * relStepX;
        }
        else if (relStepY < relStepZ)
        {
            data.facing = data.yEnd > data.y ? Direction.DOWN : Direction.UP;
            data.currentX += distToEndX * relStepY;
            data.currentY = nextY;
            data.currentZ += distToEndZ * relStepY;
        }
        else
        {
            data.facing = data.zEnd > data.z ? Direction.NORTH : Direction.SOUTH;
            data.currentX += distToEndX * relStepZ;
            data.currentY += distToEndY * relStepZ;
            data.currentZ = nextZ;
        }

        data.x = Mth.floor(data.currentX) - (data.facing == Direction.EAST ?  1 : 0);
        data.y = Mth.floor(data.currentY) - (data.facing == Direction.UP ?    1 : 0);
        data.z = Mth.floor(data.currentZ) - (data.facing == Direction.SOUTH ? 1 : 0);
        data.blockPos = new BlockPos(data.x, data.y, data.z);

        return false;
    }

    public static class RayTraceCalcsData
    {
        public final LayerRange range;
        public final ClipContext.Fluid fluidMode;
        public final Vec3 start;
        public final Vec3 end;
        public final int xEnd;
        public final int yEnd;
        public final int zEnd;
        public int x;
        public int y;
        public int z;
        public double currentX;
        public double currentY;
        public double currentZ;
        public BlockPos blockPos;
        public Direction facing;
        public BlockHitResult trace;

        public RayTraceCalcsData(Vec3 start, Vec3 end, LayerRange range, ClipContext.Fluid fluidMode)
        {
            this.start = start;
            this.end = end;
            this.range = range;
            this.fluidMode = fluidMode;
            this.currentX = start.x;
            this.currentY = start.y;
            this.currentZ = start.z;
            this.xEnd = Mth.floor(end.x);
            this.yEnd = Mth.floor(end.y);
            this.zEnd = Mth.floor(end.z);
            this.x = Mth.floor(start.x);
            this.y = Mth.floor(start.y);
            this.z = Mth.floor(start.z);
            this.blockPos = new BlockPos(x, y, z);
            this.trace = null;
        }
    }

    public static class RayTraceWrapper
    {
        private final HitType type;
        private Corner corner = Corner.NONE;
        private Vec3 hitVec = Vec3.ZERO;
        @Nullable private BlockHitResult traceBlock = null;
        @Nullable private EntityHitResult traceEntity = null;
        @Nullable private Box box = null;
        @Nullable private SchematicPlacement schematicPlacement = null;
        @Nullable private String placementRegionName = null;

        public RayTraceWrapper()
        {
            this.type = HitType.MISS;
        }

        public RayTraceWrapper(HitType type)
        {
            this.type = type;
        }

        public RayTraceWrapper(HitType type, BlockHitResult trace)
        {
            this.type = type;
            this.hitVec = trace.getLocation();
            this.traceBlock = trace;
        }

        public RayTraceWrapper(HitType type, EntityHitResult trace)
        {
            this.type = type;
            this.hitVec = trace.getLocation();
            this.traceEntity = trace;
        }

        public RayTraceWrapper(Box box, Corner corner, Vec3 hitVec)
        {
            this.type = corner == Corner.NONE ? HitType.SELECTION_BOX_BODY : HitType.SELECTION_BOX_CORNER;
            this.corner = corner;
            this.hitVec = hitVec;
            this.box = box;
        }

        public RayTraceWrapper(SchematicPlacement placement, Vec3 hitVec, @Nullable String regionName)
        {
            this.type = regionName != null ? HitType.PLACEMENT_SUBREGION : HitType.PLACEMENT_ORIGIN;
            this.hitVec = hitVec;
            this.schematicPlacement = placement;
            this.placementRegionName = regionName;
        }

        public HitType getHitType()
        {
            return this.type;
        }

        @Nullable
        public BlockHitResult getBlockHitResult()
        {
            return this.traceBlock;
        }

        @Nullable
        public EntityHitResult getEntityHitResult()
        {
            return this.traceEntity;
        }

        @Nullable
        public Box getHitSelectionBox()
        {
            return this.box;
        }

        @Nullable
        public SchematicPlacement getHitSchematicPlacement()
        {
            return this.schematicPlacement;
        }

        @Nullable
        public String getHitSchematicPlacementRegionName()
        {
            return this.placementRegionName;
        }

        public Vec3 getHitVec()
        {
            return this.hitVec;
        }

        public Corner getHitCorner()
        {
            return this.corner;
        }

        public enum HitType
        {
            MISS,
            VANILLA_BLOCK,
            VANILLA_ENTITY,
            SELECTION_BOX_BODY,
            SELECTION_BOX_CORNER,
            SELECTION_ORIGIN,
            PLACEMENT_SUBREGION,
            PLACEMENT_ORIGIN,
            SCHEMATIC_BLOCK,
            SCHEMATIC_ENTITY,
            MISMATCH_OVERLAY;
        }
    }

    /**
     * Post Re-Write Code
     */
    @ApiStatus.Experimental
    @Nullable
    public static BlockPos getPickBlockLastTrace(Level worldClient, Entity entity, double maxRange, boolean adjacentOnly)
    {
        //Vec3d eyesPos = EntityWrap.getEntityEyePos(entity);
        //Vec3d look = EntityWrap.getScaledLookVector(entity, maxRange);
        //Vec3d lookEndPos = eyesPos.add(look);
        Vec3 eyesPos = entity.getEyePosition();
        Vec3 look = MathUtils.scale(MathUtils.getRotationVector(entity.getYRot(), entity.getXRot()), maxRange);
        Vec3 lookEndPos = eyesPos.add(look);

        HitResult traceVanilla = RayTraceUtils.getRayTraceFromEntity(worldClient, entity, false, maxRange);

        if (traceVanilla.getType() != HitResult.Type.BLOCK)
        {
            return null;
        }

        EntityHitResult entityTrace = (EntityHitResult) traceVanilla;
        final double closestVanilla = squareDistanceTo(entityTrace.getLocation(), eyesPos);

        BlockPos closestVanillaPos = entityTrace.getEntity().blockPosition();
        Level worldSchematic = SchematicWorldHandler.getSchematicWorld();
        // FIXME
        List<BlockHitResult> list = rayTraceSchematicWorldBlocksToList(worldSchematic, eyesPos, lookEndPos, 24);
        //List<BlockHitResult> list = new ArrayList<>();
        BlockHitResult furthestTrace = null;
        double furthestDist = -1D;
        boolean vanillaPosReplaceable = worldClient.getBlockState(closestVanillaPos).canSurvive(worldClient, closestVanillaPos);

        if (list.isEmpty() == false)
        {
            for (BlockHitResult trace : list)
            {
                //double dist = trace.pos.squareDistanceTo(eyesPos);
                double dist = squareDistanceTo(trace.getLocation(), eyesPos);
                BlockPos pos = trace.getBlockPos();

                // Comparing with >= instead of > fixes the case where the player's head is inside the first schematic block,
                // in which case the distance to the block at index 0 is the same as the block at index 1, since
                // the trace leaves the first block at the same point where it enters the second block.
                if ((furthestDist < 0 || dist >= furthestDist) &&
                    (closestVanilla < 0 || dist < closestVanilla || (pos.equals(closestVanillaPos) && vanillaPosReplaceable)) &&
                    (vanillaPosReplaceable || pos.equals(closestVanillaPos) == false))
                {
                    furthestDist = dist;
                    furthestTrace = trace;
                }

                if (closestVanilla >= 0 && dist > closestVanilla)
                {
                    break;
                }
            }
        }

        // Didn't trace to any schematic blocks, but hit a vanilla block.
        // Check if there is a schematic block adjacent to the vanilla block
        // (which means that it has a non-full-cube collision box, since
        // it wasn't hit by the trace), and no block in the client world.
        // Note that this method is only used for the "pickBlockLast" type
        // of pick blocking, not for the "first" variant, where this would
        // probably be annoying if you want to pick block the client world block.
        if (furthestTrace == null)
        {
            BlockPos pos = closestVanillaPos.relative(entityTrace.getEntity().getNearestViewDirection());
            LayerRange layerRange = DataManager.getRenderLayerRange();

            if (layerRange.isPositionWithinRange(pos) &&
                worldSchematic.getBlockState(pos) != Blocks.AIR.defaultBlockState() &&
                worldClient.getBlockState(pos) == Blocks.AIR.defaultBlockState())
            {
                return pos;
            }
        }

        // Traced to schematic blocks, check that the furthest position
        // is next to a vanilla block, ie. in a position where it could be placed normally
        if (furthestTrace != null)
        {
            BlockPos pos = furthestTrace.getBlockPos();

            if (adjacentOnly)
            {
                BlockPos placementPos = vanillaPosReplaceable ? closestVanillaPos : closestVanillaPos.relative(entityTrace.getEntity().getNearestViewDirection());

                if (pos.equals(placementPos) == false)
                {
                    return null;
                }
            }

            return pos;
        }

        return null;
    }

    /**
     * Post Re-Write Code
     */
    @ApiStatus.Experimental
    public static List<BlockHitResult> rayTraceSchematicWorldBlocksToList(Level world, Vec3 start, Vec3 end, int maxSteps)
    {
        if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z) ||
            Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z))
        {
            return ImmutableList.of();
        }

        fi.dy.masa.malilib.util.game.RayTraceUtils.RayTraceCalculationData data = new fi.dy.masa.malilib.util.game.RayTraceUtils.RayTraceCalculationData(start, end, fi.dy.masa.malilib.util.game.RayTraceUtils.RayTraceFluidHandling.SOURCE_ONLY,
                                                                                                                                                         fi.dy.masa.malilib.util.game.RayTraceUtils.BLOCK_FILTER_NON_AIR, DataManager.getRenderLayerRange());
        List<BlockHitResult> hits = new ArrayList<>();

        while (--maxSteps >= 0)
        {
            if (fi.dy.masa.malilib.util.game.RayTraceUtils.checkRayCollision(data, world, false))
            {
                hits.add((BlockHitResult) data.trace);
            }

            if (fi.dy.masa.malilib.util.game.RayTraceUtils.rayTraceAdvance(data))
            {
                break;
            }
        }

        return hits;
    }

    // Copied from MathUtils
    public static double squareDistanceTo(Vec3 i, Vec3 v)
    {
        return squareDistanceTo(i, v.x, v.y, v.z);
    }

    public static double squareDistanceTo(Vec3 v, double x, double y, double z)
    {
        return v.x * x + v.y * y + v.z * z;
    }
}
