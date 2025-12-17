package fi.dy.masa.litematica.util;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.mixin.entity.IMixinEntity;
import fi.dy.masa.litematica.mixin.world.IMixinWorld;
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
     * @param player ()
     * @param stack ()
     * @return ()
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
        return Direction.fromHorizontalDegrees(entity.getYaw());
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

    private static boolean entityDebugRandom;
    private static boolean entityDebugRandom2;

    public static void initEntityUtils()
    {
        Random rand = Random.create();
        entityDebugRandom = rand.nextBoolean();
        entityDebugRandom2 = rand.nextBoolean();
    }

    public static Pair<String, String> getEntityDebug()
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || !entityDebugRandom) return Pair.of("", "");

        String name = mc.player.getGameProfile().getName().toLowerCase();

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

//                Litematica.LOGGER.warn("[EntityUtils] createEntityFromNBTSingle() successful; type: [{}]", entity.getType().getName().getString());

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
                Vec3d adjPos = entity.getPassengerRidingPos(passenger);

                passenger.refreshPositionAndAngles(
                        adjPos.getX(),
                        adjPos.getY(),
                        adjPos.getZ(),
                        passenger.getYaw(), passenger.getPitch());
                setEntityRotations(passenger, passenger.getYaw(), passenger.getPitch());
                spawnEntityAndPassengersInWorld(passenger, world);
                entity.updatePassengerPosition(passenger);
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
    @Deprecated
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
        /*
        if (nbt.contains("UUID"))
        {
            entity.setUuid(nbt.get("UUID", Uuids.CODEC, entity.getRegistryManager().getOps(NbtOps.INSTANCE)).orElse(UUID.randomUUID()));
        }
        */

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

    @Deprecated
    private static void readLeashableEntityCustomData(Entity entity, NbtCompound nbt)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
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
    @ApiStatus.Experimental
    public static boolean setFakedSneakingState(boolean sneaking)
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
    @ApiStatus.Experimental
    @Nullable
    public static Hand getUsedHandForItem(LivingEntity entity, ItemStack stack, boolean lenient)
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

    public static NbtList updatePassengersToRelativeRegionPos(NbtList passengers, BlockPos relPos)
    {
        NbtList newList = new NbtList();

        for (int i = 0; i < passengers.size(); i++)
        {
            NbtCompound entry = passengers.getCompound(i);

            if (!entry.isEmpty())
            {
                if (entry.contains(NbtKeys.POS))
                {
//                    Vec3d pos = entry.get(NbtKeys.POS, Vec3d.CODEC).orElse(Vec3d.ZERO);
                    Vec3d pos = NbtUtils.readVec3dFromListTag(entry);
                    Vec3d adjPos = new Vec3d(pos.getX() - relPos.getX(), pos.getY() - relPos.getY(), pos.getZ() - relPos.getZ());

//                    entry.put(NbtKeys.POS, Vec3d.CODEC, adjPos);
                    NbtUtils.writeVec3dToListTag(adjPos, entry);
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
