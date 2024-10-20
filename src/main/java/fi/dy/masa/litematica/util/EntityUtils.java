package fi.dy.masa.litematica.util;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.LeashKnotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.IMixinEntity;
import fi.dy.masa.litematica.mixin.IMixinWorld;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;

public class EntityUtils
{
    public static final Predicate<Entity> NOT_PLAYER = entity -> (entity instanceof PlayerEntity) == false;

    public static boolean isCreativeMode(PlayerEntity player)
    {
        return player.getAbilities().creativeMode;
    }

    public static boolean hasToolItem(LivingEntity entity)
    {
        return hasToolItemInHand(entity, Hand.MAIN_HAND) ||
               hasToolItemInHand(entity, Hand.OFF_HAND);
    }

    public static boolean hasToolItemInHand(LivingEntity entity, Hand hand)
    {
        // Data Component-aware toolItem Code (aka NBT)
        if (DataManager.getInstance().hasToolItemComponents())
        {
            ItemStack toolItem = DataManager.getInstance().getToolItemComponents();
            ItemStack stackHand = entity.getStackInHand(hand);

            if (toolItem != null)
            {
                return InventoryUtils.areStacksAndNbtEqual(toolItem, stackHand);
            }

            return false;
        }

        // Standard toolItem Code
        ItemStack toolItem = DataManager.getToolItem();

        if (toolItem.isEmpty())
        {
            return entity.getMainHandStack().isEmpty();
        }

        ItemStack stackHand = entity.getStackInHand(hand);

        return InventoryUtils.areStacksEqualIgnoreNbt(toolItem, stackHand);
    }

    /**
     * Checks if the requested item is currently in the player's hand such that it would be used for using/placing.
     * This means, that it must either be in the main hand, or the main hand must be empty and the item is in the offhand.
     * @param player
     * @param stack
     * @return
     */
    @Nullable
    public static Hand getUsedHandForItem(PlayerEntity player, ItemStack stack)
    {
        Hand hand = null;

        if (InventoryUtils.areStacksEqualIgnoreNbt(player.getMainHandStack(), stack))
        {
            hand = Hand.MAIN_HAND;
        }
        else if (player.getMainHandStack().isEmpty() &&
                InventoryUtils.areStacksEqualIgnoreNbt(player.getOffHandStack(), stack))
        {
            hand = Hand.OFF_HAND;
        }

        return hand;
    }

    public static boolean areStacksEqualIgnoreDurability(ItemStack stack1, ItemStack stack2)
    {
        return InventoryUtils.areStacksEqualIgnoreDurability(stack1, stack2);
    }

    public static Direction getHorizontalLookingDirection(Entity entity)
    {
        return Direction.fromRotation(entity.getYaw());
    }

    public static Direction getVerticalLookingDirection(Entity entity)
    {
        return entity.getPitch() > 0 ? Direction.DOWN : Direction.UP;
    }

    public static Direction getClosestLookingDirection(Entity entity)
    {
        if (entity.getPitch() > 60.0f)
        {
            return Direction.DOWN;
        }
        else if (-entity.getPitch() > 60.0f)
        {
            return Direction.UP;
        }

        return getHorizontalLookingDirection(entity);
    }

    @Nullable
    public static <T extends Entity> T findEntityByUUID(List<T> list, UUID uuid)
    {
        if (uuid == null)
        {
            return null;
        }

        for (T entity : list)
        {
            if (entity.getUuid().equals(uuid))
            {
                return entity;
            }
        }

        return null;
    }

    @Nullable
    public static String getEntityId(Entity entity)
    {
        EntityType<?> entitytype = entity.getType();
        Identifier resourcelocation = EntityType.getId(entitytype);
        return entitytype.isSaveable() && resourcelocation != null ? resourcelocation.toString() : null;
    }

    @Nullable
    private static Entity createEntityFromNBTSingle(NbtCompound nbt, World world)
    {
        try
        {
            Optional<Entity> optional = EntityType.getEntityFromNbt(nbt, world, SpawnReason.LOAD);

            if (optional.isPresent())
            {
                Entity entity = optional.get();
                entity.setUuid(UUID.randomUUID());
                return entity;
            }
        }
        catch (Exception ignore)
        {
        }

        return null;
    }

    /**
     * Note: This does NOT spawn any of the entities in the world!
     * @param nbt
     * @param world
     * @return
     */
    @Nullable
    public static Entity createEntityAndPassengersFromNBT(NbtCompound nbt, World world)
    {
        Entity entity = createEntityFromNBTSingle(nbt, world);

        if (entity == null)
        {
            return null;
        }
        else
        {
            if (nbt.contains("Passengers", Constants.NBT.TAG_LIST))
            {
                NbtList taglist = nbt.getList("Passengers", Constants.NBT.TAG_COMPOUND);

                for (int i = 0; i < taglist.size(); ++i)
                {
                    Entity passenger = createEntityAndPassengersFromNBT(taglist.getCompound(i), world);

                    if (passenger != null)
                    {
                        passenger.startRiding(entity, true);
                    }
                }
            }

            return entity;
        }
    }

    public static void spawnEntityAndPassengersInWorld(Entity entity, World world)
    {
        if (world.spawnEntity(entity) && entity.hasPassengers())
        {
            for (Entity passenger : entity.getPassengerList())
            {
                passenger.refreshPositionAndAngles(
                        entity.getX(),
                        entity.getY() + entity.getPassengerRidingPos(passenger).getY(),
                        entity.getZ(),
                        passenger.getYaw(), passenger.getPitch());
                setEntityRotations(passenger, passenger.getYaw(), passenger.getPitch());
                spawnEntityAndPassengersInWorld(passenger, world);
            }
        }
    }

    public static void setEntityRotations(Entity entity, float yaw, float pitch)
    {
        entity.setYaw(yaw);
        entity.prevYaw = yaw;

        entity.setPitch(pitch);
        entity.prevPitch = pitch;

        if (entity instanceof LivingEntity livingBase)
        {
            livingBase.headYaw = yaw;
            livingBase.bodyYaw = yaw;
            livingBase.prevHeadYaw = yaw;
            livingBase.prevBodyYaw = yaw;
            //livingBase.renderYawOffset = yaw;
            //livingBase.prevRenderYawOffset = yaw;
        }
    }

    public static List<Entity> getEntitiesWithinSubRegion(World world, BlockPos origin, BlockPos regionPos, BlockPos regionSize,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement)
    {
        // These are the untransformed relative positions
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        BlockPos posEndAbs = PositionUtils.getTransformedPlacementPosition(regionSize.add(-1, -1, -1), schematicPlacement, placement).add(regionPosRelTransformed).add(origin);
        BlockPos regionPosAbs = regionPosRelTransformed.add(origin);
        Box bb = PositionUtils.createEnclosingAABB(regionPosAbs, posEndAbs);

        return world.getOtherEntities(null, bb, EntityUtils.NOT_PLAYER);
    }

    public static boolean shouldPickBlock(PlayerEntity player)
    {
        return Configs.Generic.PICK_BLOCK_ENABLED.getBooleanValue() &&
                (Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue() == false ||
                hasToolItem(player) == false) &&
                Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue();
    }

    // entity.readNbt(nbt);
    public static void loadNbtIntoEntity(Entity entity, NbtCompound nbt)
    {
        entity.fallDistance = nbt.getFloat("FallDistance");
        entity.setFireTicks(nbt.getShort("Fire"));
        if (nbt.contains("Air")) {
            entity.setAir(nbt.getShort("Air"));
        }

        entity.setOnGround(nbt.getBoolean("OnGround"));
        entity.setInvulnerable(nbt.getBoolean("Invulnerable"));
        entity.setPortalCooldown(nbt.getInt("PortalCooldown"));
        if (nbt.containsUuid("UUID")) {
            entity.setUuid(nbt.getUuid("UUID"));
        }

        if (nbt.contains("CustomName", NbtElement.STRING_TYPE)) {
            String string = nbt.getString("CustomName");
            entity.setCustomName(Text.Serialization.fromJson(string, entity.getRegistryManager()));
        }

        entity.setCustomNameVisible(nbt.getBoolean("CustomNameVisible"));
        entity.setSilent(nbt.getBoolean("Silent"));
        entity.setNoGravity(nbt.getBoolean("NoGravity"));
        entity.setGlowing(nbt.getBoolean("Glowing"));
        entity.setFrozenTicks(nbt.getInt("TicksFrozen"));
        if (nbt.contains("Tags", NbtElement.LIST_TYPE)) {
            entity.getCommandTags().clear();
            NbtList nbtList4 = nbt.getList("Tags", NbtElement.STRING_TYPE);
            int max = Math.min(nbtList4.size(), 1024);

            for(int i = 0; i < max; ++i) {
                entity.getCommandTags().add(nbtList4.getString(i));
            }
        }

        if (entity instanceof Leashable)
        {
            readLeashableEntityCustomData(entity, nbt);
        }
        else
        {
            ((IMixinEntity) entity).litematica_readCustomDataFromNbt(nbt);
        }
    }

    private static void readLeashableEntityCustomData(Entity entity, NbtCompound nbt)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        assert entity instanceof Leashable;
        Leashable leashable = (Leashable) entity;
        ((IMixinEntity) entity).litematica_readCustomDataFromNbt(nbt);
        if (leashable.getLeashData() != null && leashable.getLeashData().unresolvedLeashData != null)
        {
            leashable.getLeashData().unresolvedLeashData
                    .ifLeft(uuid ->
                            // We MUST use client-side world here.
                            leashable.attachLeash(((IMixinWorld) mc.world).litematica_getEntityLookup().get(uuid), false))
                    .ifRight(pos ->
                            leashable.attachLeash(LeashKnotEntity.getOrCreate(mc.world, pos), false));
        }
    }

    /**
     * Post Re-Write Code
     */
    public static boolean PRW_setFakedSneakingState(boolean sneaking)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;

        if (player != null && player.isSneaking() != sneaking)
        {
            //CPacketEntityAction.Action action = sneaking ? CPacketEntityAction.Action.START_SNEAKING : CPacketEntityAction.Action.STOP_SNEAKING;
            //player.connection.sendPacket(new CPacketEntityAction(player, action));
            //player.movementInput.sneak = sneaking;

            player.setSneaking(sneaking);

            return true;
        }

        return false;
    }

    /**
     * Checks if the requested item is currently in the entity's hand such that it would be used for using/placing.
     * This means, that it must either be in the main hand, or the main hand must be empty and the item is in the offhand.
     * @param lenient if true, then NBT tags and also damage of damageable items are ignored
     */
    @Nullable
    public static Hand PRW_getUsedHandForItem(LivingEntity entity, ItemStack stack, boolean lenient)
    {
        Hand hand = null;
        //Hand tmpHand = ItemWrap.isEmpty(getMainHandItem(entity)) ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        //ItemStack handStack = getHeldItem(entity, tmpHand);
        Hand tmpHand = entity.getMainHandStack().isEmpty() ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack handStack = entity.getStackInHand(tmpHand);


        if ((lenient && fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreDurability(handStack, stack)) ||
            (lenient == false && fi.dy.masa.malilib.util.InventoryUtils.areStacksEqual(handStack, stack)))
        {
            hand = tmpHand;
        }

        return hand;
    }
}
