package fi.dy.masa.litematica.util.post_rewrite;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.post_rewrite.malilib.MathUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

public class RayTraceUtils2
{
    @Nullable
    public static BlockPos getPickBlockLastTrace(World worldClient, Entity entity, double maxRange, boolean adjacentOnly)
    {
        //Vec3d eyesPos = EntityWrap.getEntityEyePos(entity);
        //Vec3d look = EntityWrap.getScaledLookVector(entity, maxRange);
        //Vec3d lookEndPos = eyesPos.add(look);
        Vec3d eyesPos = entity.getEyePos();
        Vec3d look = MathUtils.scale(MathUtils.getRotationVector(entity.getYaw(), entity.getPitch()), maxRange);
        Vec3d lookEndPos = eyesPos.add(look);

        HitResult traceVanilla = RayTraceUtils.getRayTraceFromEntity(worldClient, entity, false, maxRange);

        if (traceVanilla.getType() != HitResult.Type.BLOCK)
        {
            return null;
        }

        EntityHitResult entityTrace = (EntityHitResult) traceVanilla;
        final double closestVanilla = MathUtils.squareDistanceTo(entityTrace.getPos(), eyesPos);

        BlockPos closestVanillaPos = entityTrace.getEntity().getBlockPos();
        World worldSchematic = SchematicWorldHandler.getSchematicWorld();
        //List<BlockHitResult> list = rayTraceSchematicWorldBlocksToList(worldSchematic, eyesPos, lookEndPos, 24);
        List<BlockHitResult> list = new ArrayList<>();
        BlockHitResult furthestTrace = null;
        double furthestDist = -1D;
        boolean vanillaPosReplaceable = worldClient.getBlockState(closestVanillaPos).canPlaceAt(worldClient, closestVanillaPos);

        if (list.isEmpty() == false)
        {
            for (BlockHitResult trace : list)
            {
                //double dist = trace.pos.squareDistanceTo(eyesPos);
                double dist = MathUtils.squareDistanceTo(trace.getPos(), eyesPos);
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
            BlockPos pos = closestVanillaPos.offset(entityTrace.getEntity().getFacing());
            LayerRange layerRange = DataManager.getRenderLayerRange();

            if (layerRange.isPositionWithinRange(pos) &&
                    worldSchematic.getBlockState(pos) != Blocks.AIR.getDefaultState() &&
                    worldClient.getBlockState(pos) == Blocks.AIR.getDefaultState())
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
                BlockPos placementPos = vanillaPosReplaceable ? closestVanillaPos : closestVanillaPos.offset(entityTrace.getEntity().getFacing());

                if (pos.equals(placementPos) == false)
                {
                    return null;
                }
            }

            return pos;
        }

        return null;
    }

    /*
    public static List<BlockHitResult> rayTraceSchematicWorldBlocksToList(World world, Vec3d start, Vec3d end, int maxSteps)
    {
        if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z) ||
            Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z))
        {
            return ImmutableList.of();
        }

        RayTraceCalculationData data = new RayTraceCalculationData(start, end, RayTraceFluidHandling.SOURCE_ONLY,
                RayTraceUtils.BLOCK_FILTER_NON_AIR, DataManager.getRenderLayerRange());
        List<BlockHitResult> hits = new ArrayList<>();

        while (--maxSteps >= 0)
        {
            if (RayTraceUtils.checkRayCollision(data, world, false))
            {
                hits.add(HitResult.of(data.trace));
            }

            if (RayTraceUtils.rayTraceAdvance(data))
            {
                break;
            }
        }

        return hits;
    }
     */
}
