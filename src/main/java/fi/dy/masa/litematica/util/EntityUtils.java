package fi.dy.masa.litematica.util;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import fi.dy.masa.malilib.util.nbt.NbtView;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.entity.IMixinEntity;
import fi.dy.masa.litematica.mixin.world.IMixinWorld;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;

public class EntityUtils
{
    public static final Predicate<Entity> NOT_PLAYER = entity -> !(entity instanceof Player);

    public static boolean isCreativeMode(Player player)
    {
        return player.getAbilities().instabuild;
    }

    public static boolean hasToolItem(LivingEntity entity)
    {
        return hasToolItemInHand(entity, InteractionHand.MAIN_HAND) ||
               hasToolItemInHand(entity, InteractionHand.OFF_HAND);
    }

    public static boolean hasToolItemInHand(LivingEntity entity, InteractionHand hand)
    {
        // Data Component-aware toolItem Code (aka NBT)
        if (DataManager.getInstance().hasToolItemComponents())
        {
            ItemStack toolItem = DataManager.getInstance().getToolItemComponents();
            ItemStack stackHand = entity.getItemInHand(hand);

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
            return entity.getMainHandItem().isEmpty();
        }

        ItemStack stackHand = entity.getItemInHand(hand);

        return InventoryUtils.areStacksEqualIgnoreNbt(toolItem, stackHand);
    }

    /**
     * Checks if the requested item is currently in the player's hand such that it would be used for using/placing.
     * This means, that it must either be in the main hand, or the main hand must be empty and the item is in the offhand.
     * @param player ()
     * @param stack ()
     * @return ()
     */
    @Nullable
    public static InteractionHand getUsedHandForItem(Player player, ItemStack stack)
    {
        InteractionHand hand = null;

        if (InventoryUtils.areStacksEqualIgnoreNbt(player.getMainHandItem(), stack))
        {
            hand = InteractionHand.MAIN_HAND;
        }
        else if (player.getMainHandItem().isEmpty() &&
                InventoryUtils.areStacksEqualIgnoreNbt(player.getOffhandItem(), stack))
        {
            hand = InteractionHand.OFF_HAND;
        }

        return hand;
    }

    public static boolean areStacksEqualIgnoreDurability(ItemStack stack1, ItemStack stack2)
    {
        return InventoryUtils.areStacksEqualIgnoreDurability(stack1, stack2);
    }

    public static Direction getHorizontalLookingDirection(Entity entity)
    {
        return Direction.fromYRot(entity.getYRot());
    }

    public static Direction getVerticalLookingDirection(Entity entity)
    {
        return entity.getXRot() > 0 ? Direction.DOWN : Direction.UP;
    }

    public static Direction getClosestLookingDirection(Entity entity)
    {
        if (entity.getXRot() > 60.0f)
        {
            return Direction.DOWN;
        }
        else if (-entity.getXRot() > 60.0f)
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
            if (entity.getUUID().equals(uuid))
            {
                return entity;
            }
        }

        return null;
    }

    private static boolean entityDebugRandom;
    private static boolean entityDebugRandom2;

    public static void initEntityUtils()
    {
        RandomSource rand = RandomSource.create();
        entityDebugRandom = rand.nextBoolean();
        entityDebugRandom2 = rand.nextBoolean();
    }

    private static boolean isGoat(GameProfile profile)
    {
        return profile.name().equalsIgnoreCase("docm77");
    }

    public static Pair<String, String> getEntityDebug()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null)
        {
            return Pair.of("", "");
        }
        else if (isGoat(mc.player.getGameProfile()))
        {
            // Adjusted as per doc -- he wants it always shown ^_^
            return Pair.of("Goatmatica", "Grind. Optimize. Automate. Thrive.");
        }
        else if (!entityDebugRandom)
        {
            return Pair.of("", "");
        }

        String name = mc.player.getGameProfile().name().toLowerCase();

        switch (name)
        {
            case "sakuraryoko" ->
            {
                return Pair.of("Sakuramatica", "The Sakura Goddess Herself.");
            }
            case "docm77" ->
            {
                return Pair.of("Goatmatica", "Grind. Optimize. Automate. Thrive.");
            }
            case "xisuma", "xisumavoid" ->
            {
                return entityDebugRandom2 ? Pair.of("Xisumatica", "Chief architect & humble leader.") : Pair.of("Xisumatica", "Check out Soulside Eclipse on Spotify.");
            }
            case "renthedog", "rendog" ->
            {
                return entityDebugRandom2 ? Pair.of("Dogmatica", "Gigacorps' most famous employee.") : Pair.of("Renmatica", "Docm77's single ladies' favorite.");
            }
            case "geminitay" ->
            {
                return entityDebugRandom2 ? Pair.of("Slaymatica", "God's favorite Princess.") : Pair.of("Slaymatica", "Hermitcraft's chief remover of heads.");
            }
            case "pearlescentmoon" ->
            {
                return entityDebugRandom2 ? Pair.of("Pearlmatica", "The queen of aussie ping.") : Pair.of("", "");
            }
            case "falsesymmetry" ->
            {
                return entityDebugRandom2 ? Pair.of("Queenmatica", "The Queen of Hearts, Heads, and Body Parts.") : Pair.of("", "");
            }
            case "tango" ->
            {
                return entityDebugRandom2 ? Pair.of("Tangomatica", "The Dungeon Master.") : Pair.of("Tangomatica", "Master of the thingificator.");
            }
            case "etho", "ethoslab" ->
            {
                return entityDebugRandom2 ? Pair.of("Slabmatica", "The Canadian legend.") : Pair.of("", "");
            }
            case "ijevin" ->
            {
                return entityDebugRandom2 ? Pair.of("iJevinatica", "iJevin's favorite mod suite (thank you!)") : Pair.of("", "");
            }
            case "cubfan135" ->
            {
                return entityDebugRandom2 ? Pair.of("Cubmatica", "Ladies and gentlemen; Beautiful, absolutely beautiful.") : Pair.of("", "");
            }
	        case "smajor1995" ->
	        {
		        return entityDebugRandom2 ? Pair.of("Scottmatica", "The most friendly and soothing voice in the game.") : Pair.of("", "");
	        }
	        case "shubbleyt" ->
	        {
		        return entityDebugRandom2 ? Pair.of("Starmatica", "Red Mushroom blocks are soo underrated.") : Pair.of("", "");
	        }
	        case "goodtimewithscar" ->
	        {
		        return entityDebugRandom2 ? Pair.of("Scarmatica", "The Ore Snatcher.") : Pair.of("Scarmatica", "Architect of the whimsy.");
	        }
	        case "joehillssays", "joehillstsd" ->
	        {
		        return entityDebugRandom2 ? Pair.of("Joematica", "One of the True Hermits.") : Pair.of("Hillsmatica", "Howdy y'all from Nashville, TN!");
	        }
            default ->
            {
                return Pair.of("", "");
            }
        }
    }

    @Nullable
    public static String getEntityId(Entity entity)
    {
        EntityType<?> entitytype = entity.getType();
        Identifier id = EntityType.getKey(entitytype);
        return entitytype.canSerialize() && id != null ? id.toString() : null;
    }

    @Nullable
    private static Entity createEntityFromNBTSingle(CompoundTag nbt, Level world)
    {
        try
        {
            NbtView view = NbtView.getReader(nbt, world.registryAccess());
            Optional<Entity> optional = EntityType.create(view.getReader(), world, EntitySpawnReason.LOAD);

            if (optional.isPresent())
            {
                Entity entity = optional.get();

                if (!nbt.contains("UUID"))
                {
                    entity.setUUID(UUID.randomUUID());
                }

                if (nbt.contains("LastEntityID"))
                {
                    entity.setId(nbt.getIntOr("LastEntityID", -1));
                }

                if (Reference.DEBUG_MODE)
                {
                    Litematica.LOGGER.warn("[EntityUtils] createEntityFromNBTSingle() successful; type({}): [{}/{}]",
                                           entity.getId(),
                                           entity.getStringUUID(),
                                           entity.getType().getDescription().getString());
                }

                return entity;
            }
        }
        catch (Exception err)
        {
            Litematica.LOGGER.error("createEntityFromNBTSingle: Exception; {}", err.getLocalizedMessage());
        }

        return null;
    }

    /**
     * Note: This does NOT spawn any of the entities in the world!
     * @param nbt ()
     * @param world ()
     * @return ()
     */
    @Nullable
    public static Entity createEntityAndPassengersFromNBT(CompoundTag nbt, Level world)
    {
        Entity entity = createEntityFromNBTSingle(nbt, world);

        if (entity == null)
        {
            return null;
        }
        else
        {
            if (nbt.contains("Passengers"))
            {
                ListTag taglist = nbt.getListOrEmpty("Passengers");

                for (int i = 0; i < taglist.size(); ++i)
                {
                    Entity passenger = createEntityAndPassengersFromNBT(taglist.getCompoundOrEmpty(i), world);

                    if (passenger != null)
                    {
                        passenger.startRiding(entity, true, false);
                    }
                }
            }

            return entity;
        }
    }

    public static void spawnEntityAndPassengersInWorld(Entity entity, Level world)
    {
        if (world.addFreshEntity(entity) && entity.isVehicle())
        {
            for (Entity passenger : entity.getPassengers())
            {
                Vec3 adjPos = entity.getPassengerRidingPosition(passenger);

                passenger.snapTo(
                        adjPos.x(),
                        adjPos.y(),
                        adjPos.z(),
                        passenger.getYRot(), passenger.getXRot());
                setEntityRotations(passenger, passenger.getYRot(), passenger.getXRot());
                spawnEntityAndPassengersInWorld(passenger, world);
                entity.positionRider(passenger);
            }
        }
    }

    public static void setEntityRotations(Entity entity, float yaw, float pitch)
    {
        entity.setYRot(yaw);
        entity.yRotO = yaw;

        entity.setXRot(pitch);
        entity.xRotO = pitch;

        if (entity instanceof LivingEntity livingBase)
        {
            livingBase.yHeadRot = yaw;
            livingBase.yBodyRot = yaw;
            livingBase.yHeadRotO = yaw;
            livingBase.yBodyRotO = yaw;
            //livingBase.renderYawOffset = yaw;
            //livingBase.prevRenderYawOffset = yaw;
        }
    }

    public static List<Entity> getEntitiesWithinSubRegion(Level world, BlockPos origin, BlockPos regionPos, BlockPos regionSize,
            SchematicPlacement schematicPlacement, SubRegionPlacement placement)
    {
        // These are the untransformed relative positions
        BlockPos regionPosRelTransformed = PositionUtils.getTransformedBlockPos(regionPos, schematicPlacement.getMirror(), schematicPlacement.getRotation());
        BlockPos posEndAbs = PositionUtils.getTransformedPlacementPosition(regionSize.offset(-1, -1, -1), schematicPlacement, placement).offset(regionPosRelTransformed).offset(origin);
        BlockPos regionPosAbs = regionPosRelTransformed.offset(origin);
        AABB bb = PositionUtils.createEnclosingAABB(regionPosAbs, posEndAbs);

        return world.getEntities((Entity) null, bb, EntityUtils.NOT_PLAYER);
    }

    public static boolean shouldPickBlock(Player player)
    {
        return Configs.Generic.PICK_BLOCK_ENABLED.getBooleanValue() &&
                (Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue() == false ||
                hasToolItem(player) == false) &&
                Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue();
    }

    // entity.readNbt(nbt);
    @Deprecated
    public static void loadNbtIntoEntity(Entity entity, CompoundTag nbt)
    {
        entity.fallDistance = nbt.getFloatOr("FallDistance", 0f);
        entity.setRemainingFireTicks(nbt.getShortOr("Fire", (short) 0));
        if (nbt.contains("Air")) {
            entity.setAirSupply(nbt.getShortOr("Air", (short) 0));
        }

        entity.setOnGround(nbt.getBooleanOr("OnGround", true));
        entity.setInvulnerable(nbt.getBooleanOr("Invulnerable", false));
        entity.setPortalCooldown(nbt.getIntOr("PortalCooldown", 0));
        /*
        if (nbt.containsUuid("UUID")) {
            entity.setUuid(nbt.getUuid("UUID"));
        }
         */
        if (nbt.contains("UUID"))
        {
            entity.setUUID(nbt.read("UUID", UUIDUtil.AUTHLIB_CODEC, entity.registryAccess().createSerializationContext(NbtOps.INSTANCE)).orElse(UUID.randomUUID()));
        }

        if (nbt.contains("CustomName"))
        {
            nbt.read("CustomName", ComponentSerialization.CODEC).ifPresent(entity::setCustomName);
        }

        entity.setCustomNameVisible(nbt.getBooleanOr("CustomNameVisible", false));
        entity.setSilent(nbt.getBooleanOr("Silent", false));
        entity.setNoGravity(nbt.getBooleanOr("NoGravity", false));
        entity.setGlowingTag(nbt.getBooleanOr("Glowing", false));
        entity.setTicksFrozen(nbt.getIntOr("TicksFrozen", 0));
        if (nbt.contains("Tags"))
        {
            entity.entityTags().clear();
            ListTag nbtList4 = nbt.getListOrEmpty("Tags");
            int max = Math.min(nbtList4.size(), 1024);

            for(int i = 0; i < max; ++i)
            {
                entity.entityTags().add(nbtList4.getStringOr(i, ""));
            }
        }

        if (entity instanceof Leashable)
        {
            readLeashableEntityCustomData(entity, nbt);
        }
        else
        {
            NbtView view = NbtView.getReader(nbt, entity.registryAccess());
            ((IMixinEntity) entity).litematica_readCustomData(view.getReader());
        }
    }

    @Deprecated
    private static void readLeashableEntityCustomData(Entity entity, CompoundTag nbt)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        assert entity instanceof Leashable;
        Leashable leashable = (Leashable) entity;
        NbtView view = NbtView.getReader(nbt, mc.level.registryAccess());
        ((IMixinEntity) entity).litematica_readCustomData(view.getReader());
        if (leashable.getLeashData() != null && leashable.getLeashData().delayedLeashInfo != null)
        {
            leashable.getLeashData().delayedLeashInfo
                    .ifLeft(uuid ->
                            // We MUST use client-side world here.
                            leashable.setLeashedTo(((IMixinWorld) mc.level).litematica_getEntityLookup().get(uuid), false))
                    .ifRight(pos ->
                            leashable.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(mc.level, pos), false));
        }
    }

    /**
     * Post Re-Write Code
     */
    @ApiStatus.Experimental
    public static boolean setFakedSneakingState(boolean sneaking)
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player != null && player.isShiftKeyDown() != sneaking)
        {
            //CPacketEntityAction.Action action = sneaking ? CPacketEntityAction.Action.START_SNEAKING : CPacketEntityAction.Action.STOP_SNEAKING;
            //player.connection.sendPacket(new CPacketEntityAction(player, action));
            //player.movementInput.sneak = sneaking;

            player.setShiftKeyDown(sneaking);

            return true;
        }

        return false;
    }

    /**
     * Checks if the requested item is currently in the entity's hand such that it would be used for using/placing.
     * This means, that it must either be in the main hand, or the main hand must be empty and the item is in the offhand.
     * @param lenient if true, then NBT tags and also damage of damageable items are ignored
     */
    @ApiStatus.Experimental
    @Nullable
    public static InteractionHand getUsedHandForItem(LivingEntity entity, ItemStack stack, boolean lenient)
    {
        InteractionHand hand = null;
        //Hand tmpHand = ItemWrap.isEmpty(getMainHandItem(entity)) ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
        //ItemStack handStack = getHeldItem(entity, tmpHand);
        InteractionHand tmpHand = entity.getMainHandItem().isEmpty() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack handStack = entity.getItemInHand(tmpHand);


        if ((lenient && InventoryUtils.areStacksEqualIgnoreDurability(handStack, stack)) ||
            (lenient && InventoryUtils.areStacksEqualIgnoreNbt(handStack, stack)) ||
            (lenient == false && InventoryUtils.areStacksEqual(handStack, stack)))
        {
            hand = tmpHand;
        }

        return hand;
    }

    public static ListTag updatePassengersToRelativeRegionPos(ListTag passengers, BlockPos relPos)
    {
        ListTag newList = new ListTag();

        for (int i = 0; i < passengers.size(); i++)
        {
            CompoundTag entry = passengers.getCompoundOrEmpty(i);

            if (!entry.isEmpty())
            {
                if (entry.contains(NbtKeys.POS))
                {
                    Vec3 pos = entry.read(NbtKeys.POS, Vec3.CODEC).orElse(Vec3.ZERO);
                    Vec3 adjPos = new Vec3(pos.x() - relPos.getX(), pos.y() - relPos.getY(), pos.z() - relPos.getZ());

                    entry.store(NbtKeys.POS, Vec3.CODEC, adjPos);
                    newList.add(entry);
                }
                else
                {
                    newList.add(entry);
                }
            }
        }

        return newList;
    }
}
