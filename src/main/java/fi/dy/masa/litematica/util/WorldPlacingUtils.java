package fi.dy.masa.litematica.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Vec3i;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.util.DataTypeUtils;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import fi.dy.masa.malilib.util.nbt.NbtView;
import fi.dy.masa.malilib.util.position.IntBoundingBox;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic.EntityInfo;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.world.ChunkSchematicState;
import fi.dy.masa.litematica.world.ProtoChunkSchematic;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

public class WorldPlacingUtils
{
    public static ProtoChunkSchematic placeToProtoChunk(@Nonnull ProtoChunkSchematic chunk,
                                                        ChunkPos chunkPos,
                                                        SchematicPlacement schematicPlacement)
    {
        ProtoChunkSchematic filledChunk = null;
        LitematicaSchematic schematic = schematicPlacement.getSchematic();
        Set<String> regionsTouchingChunk = schematicPlacement.getRegionsTouchingChunk(chunkPos.x(), chunkPos.z());
        BlockPos origin = schematicPlacement.getOrigin();
        boolean allSuccess = true;

        try
        {
            for (String regionName : regionsTouchingChunk)
            {
                LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);

                if (container == null)
                {
                    allSuccess = false;
                    continue;
                }

                SubRegionPlacement placement = schematicPlacement.getRelativeSubRegionPlacement(regionName);

                if (placement != null && placement.isEnabled())
                {
                    Map<BlockPos, CompoundData> blockEntityMap = schematic.getBlockEntityMapForRegion(regionName);

                    filledChunk = placeBlocksToProtoChunk(chunk, chunkPos, regionName, container, blockEntityMap, origin, schematicPlacement, placement);

                    if (filledChunk == null)
                    {
                        allSuccess = false;
                        Litematica.LOGGER.warn("Invalid/missing schematic data in schematic '{}' for sub-region '{}'", schematic.getMetadata().getName(), regionName);
                    }

                    List<EntityInfo> entityList = schematic.getEntityListForRegion(regionName);

                    if (filledChunk != null &&
                        !schematicPlacement.ignoreEntities() &&
                        !placement.ignoreEntities() &&
                        entityList != null)
                    {
                        filledChunk = prepareEntitiesInProtoChunk(filledChunk, chunkPos, entityList, origin, schematicPlacement, placement);
                    }
                }
            }
        }
        catch (Exception ignored) { }

        if (allSuccess)
        {
            return filledChunk;
        }
        return null;
    }

    public static ProtoChunkSchematic placeBlocksToProtoChunk(@Nonnull ProtoChunkSchematic chunk,
                                                              ChunkPos chunkPos, String regionName,
                                                              LitematicaBlockStateContainer container,
                                                              Map<BlockPos, CompoundData> blockEntityMap,
                                                              BlockPos origin,
                                                              SchematicPlacement schematicPlacement,
                                                              SubRegionPlacement placement)
    {
        ReplaceBehavior replace = (ReplaceBehavior) Configs.Generic.PLACEMENT_REPLACE_BEHAVIOR.getOptionListValue();
        IntBoundingBox bounds = schematicPlacement.getBoxWithinChunkForRegion(regionName, chunkPos.x(), chunkPos.z());
        Vec3i regionSize = schematicPlacement.getSchematic().getAreaSizeAsVec3i(regionName);

        if (bounds == null || container == null || blockEntityMap == null || regionSize == null)
        {
            return null;
        }

        BlockPos regionPos = placement.getPos();

        // These are the untransformed relative positions
        BlockPos posEndRel = (new BlockPos(PositionUtils.getRelativeEndPositionFromAreaSize(regionSize))).offset(regionPos);
        BlockPos posMinRel = PositionUtils.getMinCorner(regionPos, posEndRel);

        // The transformed sub-region origin position
        BlockPos regionPosTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // The relative offset of the affected region's corners, to the sub-region's origin corner
        BlockPos boxMinRel = new BlockPos(bounds.minX() - origin.getX() - regionPosTransformed.getX(), 0, bounds.minZ() - origin.getZ() - regionPosTransformed.getZ());
        BlockPos boxMaxRel = new BlockPos(bounds.maxX() - origin.getX() - regionPosTransformed.getX(), 0, bounds.maxZ() - origin.getZ() - regionPosTransformed.getZ());

        // Reverse transform that relative offset, to get the untransformed orientation's offsets
        boxMinRel = PositionUtils.getReverseTransformedBlockPos(boxMinRel, placement.getMirror(), placement.getRotation());
        boxMaxRel = PositionUtils.getReverseTransformedBlockPos(boxMaxRel, placement.getMirror(), placement.getRotation());

        boxMinRel = PositionUtils.getReverseTransformedBlockPos(boxMinRel, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        boxMaxRel = PositionUtils.getReverseTransformedBlockPos(boxMaxRel, schematicPlacement.getMirror(), schematicPlacement.getRotation());

        // Get the offset relative to the sub-region's minimum corner, instead of the origin corner (which can be at any corner)
        boxMinRel = boxMinRel.subtract(posMinRel.subtract(regionPos));
        boxMaxRel = boxMaxRel.subtract(posMinRel.subtract(regionPos));

        BlockPos posMin = PositionUtils.getMinCorner(boxMinRel, boxMaxRel);
        BlockPos posMax = PositionUtils.getMaxCorner(boxMinRel, boxMaxRel);

        // Origin and sub-region origin added together for performance
        BlockPos totalRegionPosTransformed = regionPosTransformed.offset(origin);

        final int startX = posMin.getX();
        final int startZ = posMin.getZ();
        final int endX = posMax.getX();
        final int endZ = posMax.getZ();

        final int startY = 0;
        final int endY = Math.abs(regionSize.getY()) - 1;
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        //System.out.printf("sx: %d, sy: %d, sz: %d => ex: %d, ey: %d, ez: %d\n", startX, startY, startZ, endX, endY, endZ);

        if (startX < 0 || startZ < 0 || endX >= container.getSize().getX() || endZ >= container.getSize().getZ())
        {
            System.out.printf("DEBUG ============= OUT OF BOUNDS - region: %s, sx: %d, sz: %d, ex: %d, ez: %d - size x: %d z: %d =============\n",
                              regionName, startX, startZ, endX, endZ, container.getSize().getX(), container.getSize().getZ());
            return null;
        }

        final Rotation rotationCombined = schematicPlacement.getRotation().getRotated(placement.getRotation());
        final Mirror mirrorMain = schematicPlacement.getMirror();
        final BlockState barrier = Blocks.BARRIER.defaultBlockState();
        Mirror mirrorSub = placement.getMirror();
        final boolean ignoreInventories = Configs.Generic.PASTE_IGNORE_INVENTORY.getBooleanValue();

        if (mirrorSub != Mirror.NONE &&
            (schematicPlacement.getRotation() == Rotation.CLOCKWISE_90 ||
            schematicPlacement.getRotation() == Rotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
        }

        final int posMinRelMinusRegX = posMinRel.getX() - regionPos.getX();
        final int posMinRelMinusRegY = posMinRel.getY() - regionPos.getY();
        final int posMinRelMinusRegZ = posMinRel.getZ() - regionPos.getZ();

//        dumpBlockEntityMap(blockEntityMap);

        for (int y = startY; y <= endY; ++y)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                for (int x = startX; x <= endX; ++x)
                {
                    BlockState state = container.get(x, y, z);

                    if (state.getBlock() == Blocks.STRUCTURE_VOID)
                    {
                        continue;
                    }

                    posMutable.set(x, y, z);
                    CompoundData teNBT = blockEntityMap.get(posMutable);
                    BlockPos origPos = posMutable.immutable();

                    posMutable.set(posMinRelMinusRegX + x,
                                   posMinRelMinusRegY + y,
                                   posMinRelMinusRegZ + z);

                    BlockPos pos = PositionUtils.getTransformedPlacementPosition(posMutable, schematicPlacement, placement);
                    pos = pos.offset(totalRegionPosTransformed);

//                    BlockState stateOld = world.getBlockState(pos);
                    BlockState stateOld = chunk.getBlockState(pos);

                    if ((replace == ReplaceBehavior.NONE && stateOld.isAir() == false) ||
                        (replace == ReplaceBehavior.WITH_NON_AIR && state.isAir() == true))
                    {
                        continue;
                    }

                    // Fix inventory of adjacent chest sides when mirrored
                    if (state.hasBlockEntity() && state.is(Blocks.CHEST) &&
                        !ignoreInventories && mirrorMain != Mirror.NONE &&
                        !(state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) &&
                        Configs.Generic.FIX_CHEST_MIRROR.getBooleanValue())
                    {
                        Direction facing = state.getValue(ChestBlock.FACING);
                        Direction.Axis axis = facing.getAxis();
                        ChestType type = state.getValue(ChestBlock.TYPE).getOpposite();

                        if (mirrorMain != Mirror.NONE && axis != Direction.Axis.Y)
                        {
                            Direction facingAdj = type == ChestType.LEFT ? facing.getCounterClockWise(Direction.Axis.Y) : facing.getClockWise(Direction.Axis.Y);
                            BlockPos posAdj = origPos.relative(facingAdj);

                            if (blockEntityMap.containsKey(posAdj))
                            {
                                teNBT = blockEntityMap.getOrDefault(posAdj, teNBT).copy();
                            }
                        }
                    }

                    if (mirrorMain != Mirror.NONE) { state = state.mirror(mirrorMain); }
                    if (mirrorSub != Mirror.NONE)  { state = state.mirror(mirrorSub); }
                    if (rotationCombined != Rotation.NONE) { state = state.rotate(rotationCombined); }

//                    BlockEntity te = world.getBlockEntity(pos);
                    BlockEntity te = chunk.getBlockEntity(pos);

                    if (te != null)
                    {
                        if (te instanceof Container)
                        {
                            ((Container) te).clearContent();
                        }

                        chunk.setBlockState(pos, barrier, 0x14);
                    }

                    chunk.setBlockState(pos, state, 0x12);

                    // world.setBlock(pos, state, 0x12)
                    if (teNBT != null)
                    {
//                        te = world.getBlockEntity(pos);
                        te = chunk.createBlockEntity(pos);

                        if (te != null)
                        {
                            teNBT = teNBT.copy();
                            teNBT.putInt("x", pos.getX());
                            teNBT.putInt("y", pos.getY());
                            teNBT.putInt("z", pos.getZ());

                            if (ignoreInventories)
                            {
                                teNBT.remove("Items");
                            }

                            try
                            {
                                // world.registryAccess()
                                NbtView view = NbtView.getReader(teNBT, SchematicWorldHandler.INSTANCE.getRegistryManager());
                                te.loadWithComponents(view.getReader());

                                if (ignoreInventories && te instanceof Container)
                                {
                                    ((Container) te).clearContent();
                                }
                            }
                            catch (Exception e)
                            {
                                Litematica.LOGGER.warn("Failed to load BlockEntity data for {} @ {}", state, pos);
                            }

                            chunk.setBlockEntity(te);
                        }
                    }
                }
            }
        }

        if (!chunk.getState().atLeast(ChunkSchematicState.FILLED))
        {
            chunk.setState(ChunkSchematicState.FILLED);
        }

        return chunk;
    }

    public static ProtoChunkSchematic prepareEntitiesInProtoChunk(@Nonnull ProtoChunkSchematic chunk,
                                                                  ChunkPos chunkPos,
                                                                  List<EntityInfo> entityList,
                                                                  BlockPos origin,
                                                                  SchematicPlacement schematicPlacement,
                                                                  SubRegionPlacement placement)
    {
        BlockPos regionPos = placement.getPos();

        if (entityList == null)
        {
            return chunk;
        }

        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        final int offX = regionPosRelTransformed.getX() + origin.getX();
        final int offY = regionPosRelTransformed.getY() + origin.getY();
        final int offZ = regionPosRelTransformed.getZ() + origin.getZ();
        final double minX = (chunkPos.x() << 4);
        final double minZ = (chunkPos.z() << 4);
        final double maxX = (chunkPos.x() << 4) + 16;
        final double maxZ = (chunkPos.z() << 4) + 16;

        final Rotation rotationCombined = schematicPlacement.getRotation().getRotated(placement.getRotation());
        final Mirror mirrorMain = schematicPlacement.getMirror();
        Mirror mirrorSub = placement.getMirror();

        if (mirrorSub != Mirror.NONE &&
            (schematicPlacement.getRotation() == Rotation.CLOCKWISE_90 ||
            schematicPlacement.getRotation() == Rotation.COUNTERCLOCKWISE_90))
        {
            mirrorSub = mirrorSub == Mirror.FRONT_BACK ? Mirror.LEFT_RIGHT : Mirror.FRONT_BACK;
        }

        for (EntityInfo info : entityList)
        {
            Vec3 pos = info.posVec();
            pos = PositionUtils.getTransformedPosition(pos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
            pos = PositionUtils.getTransformedPosition(pos, placement.getMirror(), placement.getRotation());
            double x = pos.x + offX;
            double y = pos.y + offY;
            double z = pos.z + offZ;
            float[] origRot = new float[2];

            if (x >= minX && x < maxX && z >= minZ && z < maxZ)
            {
                CompoundData tag = info.nbt().copy();
                String id = tag.getStringOrDefault("id", "");

//                Vec3 p = NbtUtils.readEntityPositionFromTag(tag);
                Vec3 p = DataTypeUtils.readVec3dFromListTag(tag);
                Vec3 pn = new Vec3(x, y, z);

                // Entity Pos Fix
                if (p == null || (!p.equals(pn)))
                {
                    p = pn;
//                    NbtUtils.putVec3dCodec(tag, p, "Pos");
                    DataTypeUtils.putVec3dCodec(tag, pn, NbtKeys.POS);
                }

                // Avoid warning about invalid hanging position.
                // Note that this position isn't technically correct, but it only needs to be within 16 blocks
                // of the entity position to avoid the warning.
                if (id.equals("minecraft:glow_item_frame") ||
                    id.equals("minecraft:item_frame") ||
                    id.equals("minecraft:leash_knot") ||
                    id.equals("minecraft:painting"))
                {
                    tag.putInt("TileX", (int) p.x);
                    tag.putInt("TileY", (int) p.y);
                    tag.putInt("TileZ", (int) p.z);

                    // Block-Attached Pos (1.21.5+) Fix
                    BlockPos ps = tag.getCodec(NbtKeys.ATTACHED_BLOCK_POS, BlockPos.CODEC).orElse(null);
                    BlockPos nps = new BlockPos((int) x, (int) y, (int) z);

                    if (ps == null || (!ps.equals(nps)))
                    {
                        tag.putCodec(NbtKeys.ATTACHED_BLOCK_POS, BlockPos.CODEC, nps);
                    }
                }

                ListData rotation = tag.getList("Rotation");
                origRot[0] = rotation.getFloatAt(0);
                origRot[1] = rotation.getFloatAt(1);

                // TODO -- this can "only" fix leashes saved with newer builds!
                // Leash-Knot fix (We can't fix the UUID part, unless the other Mob has the *exact same* UUID in the Schematic World)
                BlockPos lp = tag.getCodec(NbtKeys.LEASH, BlockPos.CODEC).orElse(null);

                if (lp != null && !lp.equals(BlockPos.ZERO))
                {
                    final int adjX = lp.getX() + offX;
                    final int adjY = lp.getY() + offY;
                    final int adjZ = lp.getZ() + offZ;

                    BlockPos nlp = new BlockPos(adjX, adjY, adjZ);
                    tag.putCodec(NbtKeys.LEASH, BlockPos.CODEC, nlp);
                }

                // Home Pos fix
                BlockPos hp = tag.getCodec(NbtKeys.HOME_POS, BlockPos.CODEC).orElse(null);

                if (hp != null && !hp.equals(BlockPos.ZERO))
                {
                    final int hr = tag.getIntOrDefault(NbtKeys.HOME_RADIUS, -1);
                    final int adjX = hp.getX() + offX;
                    final int adjY = hp.getY() + offY;
                    final int adjZ = hp.getZ() + offZ;

                    if (hr > 0)
                    {
                        BlockPos nhp = new BlockPos(adjX, adjY, adjZ);
                        tag.putCodec(NbtKeys.HOME_POS, BlockPos.CODEC, nhp);
                    }
                }

                chunk.addEntityPairForLater(Pair.of(new EntityPosAndRot(x, y, z, rotationCombined, mirrorMain, mirrorSub, origRot), tag));
            }
        }

        return chunk;
    }

    public static void spawnEntityToWorldNow(@Nonnull Level world, Pair<EntityPosAndRot, CompoundData> entityPair)
    {
        double x = entityPair.getLeft().x;
        double y = entityPair.getLeft().y;
        double z = entityPair.getLeft().z;
        Rotation rotationCombined = entityPair.getLeft().rot();
        Mirror mirrorMain = entityPair.getLeft().mirrorMain();
        Mirror mirrorSub = entityPair.getLeft().mirrorSub();
        float[] origRot = entityPair.getLeft().origRot();

        Entity entity = EntityUtils.createEntityAndPassengersFromData(entityPair.getRight(), world);

        if (entity != null)
        {
            rotateEntity(entity, x, y, z, rotationCombined, mirrorMain, mirrorSub);
            //System.out.printf("post: %.1f - rot: %s, mm: %s, ms: %s\n", rotationYaw, rotationCombined, mirrorMain, mirrorSub);

            // Update the sleeping position to the current position
            if (entity instanceof LivingEntity living && living.isSleeping())
            {
                living.setSleepingPos(BlockPos.containing(x, y, z));
            }

            // Hack fix to fix the painting position offsets.
            // The vanilla code will end up moving the position by one in two of the orientations,
            // because it sets the hanging position to the given position (floored)
            // and then it offsets the position from the hanging position
            // by 0.5 or 1.0 blocks depending on the painting size.
            if (entity instanceof Painting paintingEntity)
            {
                Direction right = paintingEntity.getDirection().getCounterClockWise();

                if ((paintingEntity.getVariant().value().width() % 2) == 0 &&
                    right.getAxisDirection() == AxisDirection.POSITIVE)
                {
                    x -= 1.0 * right.getStepX();
                    z -= 1.0 * right.getStepZ();
                }

                if ((paintingEntity.getVariant().value().height() % 2) == 0)
                {
                    y -= 1.0;
                }

                entity.setPos(x, y, z);
            }
            if (entity instanceof ItemFrame frameEntity)
            {
                if (frameEntity.getYRot() != origRot[0] && (frameEntity.getXRot() == 90.0F || frameEntity.getXRot() == -90.0F))
                {
                    // Fix Yaw only if Pitch is +/- 90.0F (Floor, Ceiling mounted)
                    frameEntity.setYRot(origRot[0]);
                }
            }

            EntityUtils.spawnEntityAndPassengersInWorld(entity, world);

            if (entity instanceof Display || entity instanceof Leashable)
            {
                entity.tick(); // Required to set the full data for rendering
            }

            // TODO -- doesn't work
//            // Try to re-Attach leashes
//            if (entity instanceof Leashable l && world instanceof WorldSchematic ws)
//            {
//                Leashable.LeashData leashData = l.getLeashData();
//
//                if (leashData != null && leashData.delayedLeashInfo != null)
//                {
//                    Optional<UUID> uuid = leashData.delayedLeashInfo.left();
//                    Optional<BlockPos> pos = leashData.delayedLeashInfo.right();
//
//                    if (uuid.isPresent())
//                    {
//                        Entity holder = ws.getEntity(uuid.get());
//
//                        if (holder != null)
//                        {
//                            l.setLeashedTo(holder, true);
//                        }
//                    }
//                    else
//                    {
//                        pos.ifPresent(blockPos ->
//                                              l.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(ws, blockPos), true)
//                        );
//                    }
//                }
//            }
        }
    }

    public static void rotateEntity(Entity entity, double x, double y, double z,
                                    Rotation rotationCombined, Mirror mirrorMain, Mirror mirrorSub)
    {
        float rotationYaw = entity.getYRot();

        if (mirrorMain != Mirror.NONE)         { rotationYaw = entity.mirror(mirrorMain); }
        if (mirrorSub != Mirror.NONE)          { rotationYaw = entity.mirror(mirrorSub); }
        if (rotationCombined != Rotation.NONE) { rotationYaw += entity.getYRot() - entity.rotate(rotationCombined); }

        entity.snapTo(x, y, z, rotationYaw, entity.getXRot());
        EntityUtils.setEntityRotations(entity, rotationYaw, entity.getXRot());
    }

    public record EntityPosAndRot(double x, double y, double z, Rotation rot, Mirror mirrorMain, Mirror mirrorSub, float[] origRot) {  }
}
