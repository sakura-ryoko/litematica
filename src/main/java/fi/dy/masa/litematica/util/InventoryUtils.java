package fi.dy.masa.litematica.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.annotation.Experimental;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.world.WorldSchematic;

public class InventoryUtils
{
    private static final List<Integer> PICK_BLOCKABLE_SLOTS = new ArrayList<>();
    private static int nextPickSlotIndex;
    private static Pair<BlockPos, InventoryOverlay.Context> lastBlockEntityContext = null;

    public static void setPickBlockableSlots(String configStr)
    {
        PICK_BLOCKABLE_SLOTS.clear();
        String[] parts = configStr.split(",");

        for (String str : parts)
        {
            try
            {
                int slotNum = Integer.parseInt(str) - 1;

                if (PlayerInventory.isValidHotbarIndex(slotNum) &&
                    PICK_BLOCKABLE_SLOTS.contains(slotNum) == false)
                {
                    PICK_BLOCKABLE_SLOTS.add(slotNum);
                }
            }
            catch (NumberFormatException ignore) {}
        }
    }

    public static void setPickedItemToHand(ItemStack stack, MinecraftClient mc)
    {
        int slotNum = mc.player.getInventory().getSlotWithStack(stack);
        setPickedItemToHand(slotNum, stack, mc);
    }

    public static void setPickedItemToHand(int sourceSlot, ItemStack stack, MinecraftClient mc)
    {
        PlayerEntity player = mc.player;
        PlayerInventory inventory = player.getInventory();

        if (PlayerInventory.isValidHotbarIndex(sourceSlot))
        {
            inventory.selectedSlot = sourceSlot;
        }
        else
        {
            if (PICK_BLOCKABLE_SLOTS.size() == 0)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_valid_slots_configured");
                return;
            }

            int hotbarSlot = sourceSlot;

            if (sourceSlot == -1 || PlayerInventory.isValidHotbarIndex(sourceSlot) == false)
            {
                hotbarSlot = getEmptyPickBlockableHotbarSlot(inventory);
            }

            if (hotbarSlot == -1)
            {
                hotbarSlot = getPickBlockTargetSlot(player);
            }

            if (hotbarSlot != -1)
            {
                inventory.selectedSlot = hotbarSlot;

                if (EntityUtils.isCreativeMode(player))
                {
                    inventory.main.set(hotbarSlot, stack.copy());
                }
                else
                {
                    fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack.copy(), mc);
                }

                WorldUtils.setEasyPlaceLastPickBlockTime();
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.warn.pickblock.no_suitable_slot_found");
            }
        }
    }

    public static void schematicWorldPickBlock(ItemStack stack, BlockPos pos,
                                               World schematicWorld, MinecraftClient mc)
    {
        if (mc.player == null || mc.interactionManager == null || mc.world == null)
        {
            return;
        }
        if (stack.isEmpty() == false)
        {
            PlayerInventory inv = mc.player.getInventory();
            stack = stack.copy();

            if (EntityUtils.isCreativeMode(mc.player))
            {
                BlockEntity te = schematicWorld.getBlockEntity(pos);

                // The creative mode pick block with NBT only works correctly
                // if the server world doesn't have a TileEntity in that position.
                // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                if (GuiBase.isCtrlDown() && te != null && mc.world.isAir(pos))
                {
                    //te.setStackNbt(stack, schematicWorld.getRegistryManager());
                    fi.dy.masa.malilib.util.game.BlockUtils.setStackNbt(stack, te, schematicWorld.getRegistryManager());
                    //stack.set(DataComponentTypes.LORE, new LoreComponent(ImmutableList.of(Text.of("(+NBT)"))));
                }

                setPickedItemToHand(stack, mc);
                mc.interactionManager.clickCreativeStack(mc.player.getStackInHand(Hand.MAIN_HAND), 36 + inv.selectedSlot);

                //return true;
            }
            else
            {
                int slot = inv.getSlotWithStack(stack);
                boolean shouldPick = inv.selectedSlot != slot;

                if (shouldPick && slot != -1)
                {
                    setPickedItemToHand(stack, mc);
                }
                else if (slot == -1 && Configs.Generic.PICK_BLOCK_SHULKERS.getBooleanValue())
                {
                    slot = findSlotWithBoxWithItem(mc.player.playerScreenHandler, stack, false);

                    if (slot != -1)
                    {
                        ItemStack boxStack = mc.player.playerScreenHandler.slots.get(slot).getStack();
                        setPickedItemToHand(boxStack, mc);
                    }
                }

                //return shouldPick == false || canPick;
            }
        }
    }

    private static boolean canPickToSlot(PlayerInventory inventory, int slotNum)
    {
        if (PICK_BLOCKABLE_SLOTS.contains(slotNum) == false)
        {
            return false;
        }

        ItemStack stack = inventory.getStack(slotNum);

        if (stack.isEmpty())
        {
            return true;
        }

        return (Configs.Generic.PICK_BLOCK_AVOID_DAMAGEABLE.getBooleanValue() == false ||
                stack.isDamageable() == false) &&
               (Configs.Generic.PICK_BLOCK_AVOID_TOOLS.getBooleanValue() == false ||
                (stack.getItem() instanceof MiningToolItem) == false);
    }

    private static int getPickBlockTargetSlot(PlayerEntity player)
    {
        if (PICK_BLOCKABLE_SLOTS.isEmpty())
        {
            return -1;
        }

        int slotNum = player.getInventory().selectedSlot;

        if (canPickToSlot(player.getInventory(), slotNum))
        {
            return slotNum;
        }

        if (nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
        {
            nextPickSlotIndex = 0;
        }

        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i)
        {
            slotNum = PICK_BLOCKABLE_SLOTS.get(nextPickSlotIndex);

            if (++nextPickSlotIndex >= PICK_BLOCKABLE_SLOTS.size())
            {
                nextPickSlotIndex = 0;
            }

            if (canPickToSlot(player.getInventory(), slotNum))
            {
                return slotNum;
            }
        }

        return -1;
    }

    private static int getEmptyPickBlockableHotbarSlot(PlayerInventory inventory)
    {
        for (int i = 0; i < PICK_BLOCKABLE_SLOTS.size(); ++i)
        {
            int slotNum = PICK_BLOCKABLE_SLOTS.get(i);

            if (PlayerInventory.isValidHotbarIndex(slotNum))
            {
                ItemStack stack = inventory.getStack(slotNum);

                if (stack.isEmpty())
                {
                    return slotNum;
                }
            }
        }

        return -1;
    }

    public static boolean doesShulkerBoxContainItem(ItemStack stack, ItemStack referenceItem)
    {
        DefaultedList<ItemStack> items = fi.dy.masa.malilib.util.InventoryUtils.getStoredItems(stack);

        return doesListContainItem(items, referenceItem);
    }

    public static boolean doesBundleContainItem(ItemStack stack, ItemStack referenceItem)
    {
        DefaultedList<ItemStack> items = fi.dy.masa.malilib.util.InventoryUtils.getBundleItems(stack);

        return doesListContainItem(items, referenceItem);
    }

    private static boolean doesListContainItem(DefaultedList<ItemStack> items, ItemStack referenceItem)
    {
        if (items.size() > 0)
        {
            for (ItemStack item : items)
            {
                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreNbt(item, referenceItem))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public static int findSlotWithBoxWithItem(ScreenHandler container, ItemStack stackReference, boolean reverse)
    {
        final int startSlot = reverse ? container.slots.size() - 1 : 0;
        final int endSlot = reverse ? -1 : container.slots.size();
        final int increment = reverse ? -1 : 1;
        final boolean isPlayerInv = container instanceof PlayerScreenHandler;

        for (int slotNum = startSlot; slotNum != endSlot; slotNum += increment)
        {
            Slot slot = container.slots.get(slotNum);

            if ((isPlayerInv == false || fi.dy.masa.malilib.util.InventoryUtils.isRegularInventorySlot(slot.id, false)) &&
                doesShulkerBoxContainItem(slot.getStack(), stackReference))
            {
                return slot.id;
            }
        }

        return -1;
    }

    /**
     * Get a valid Inventory Object by any means necessary.
     *
     * @param world (Input ClientWorld)
     * @param pos (Pos of the Tile Entity)
     * @return (The result InventoryOverlay.Context | NULL if not obtainable)
     */
    public static @Nullable InventoryOverlay.Context getTargetInventory(World world, BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        Block blockTmp = state.getBlock();
        NbtCompound nbt = new NbtCompound();
        BlockEntity be = null;

        if (blockTmp instanceof BlockEntityProvider)
        {
            if (world instanceof ServerWorld || world instanceof WorldSchematic)
            {
                be = world.getWorldChunk(pos).getBlockEntity(pos);

                if (be != null)
                {
                    nbt = be.createNbtWithIdentifyingData(world.getRegistryManager());
                }
            }
            else
            {
                Pair<BlockEntity, NbtCompound> pair = EntitiesDataStorage.getInstance().requestBlockEntity(world, pos);

                if (pair != null)
                {
                    nbt = pair.getRight();
                }
            }

            //Litematica.logger.warn("getTarget():2: pos [{}], be [{}], nbt [{}]", pos.toShortString(), be != null, nbt != null);

            InventoryOverlay.Context ctx = getTargetInventoryFromBlock(world, pos, be, nbt);

            if (world instanceof WorldSchematic)
            {
                return ctx;
            }

            if (lastBlockEntityContext != null && !lastBlockEntityContext.getLeft().equals(pos))
            {
                lastBlockEntityContext = null;
            }

            if (ctx != null && ctx.inv() != null)
            {
                lastBlockEntityContext = Pair.of(pos, ctx);
                return ctx;
            }
            else if (lastBlockEntityContext != null && lastBlockEntityContext.getLeft().equals(pos))
            {
                return lastBlockEntityContext.getRight();
            }
        }

        return null;
    }

    private static @Nullable InventoryOverlay.Context getTargetInventoryFromBlock(World world, BlockPos pos, @Nullable BlockEntity be, NbtCompound nbt)
    {
        Inventory inv;

        if (be != null)
        {
            if (nbt.isEmpty())
            {
                nbt = be.createNbtWithIdentifyingData(world.getRegistryManager());
            }
            inv = fi.dy.masa.malilib.util.InventoryUtils.getInventory(world, pos);
        }
        else
        {
            if (nbt.isEmpty())
            {
                Pair<BlockEntity, NbtCompound> pair = EntitiesDataStorage.getInstance().requestBlockEntity(world, pos);

                if (pair != null)
                {
                    nbt = pair.getRight();
                }
            }

            inv = EntitiesDataStorage.getInstance().getBlockInventory(world, pos, false);
        }

        if (nbt != null && !nbt.isEmpty())
        {
            Inventory inv2 = fi.dy.masa.malilib.util.InventoryUtils.getNbtInventory(nbt, inv != null ? inv.size() : -1, world.getRegistryManager());

            if (inv == null)
            {
                inv = inv2;
            }
        }

        //Litematica.logger.warn("getTarget(): [SchematicWorld? {}] pos [{}], inv [{}], be [{}], nbt [{}]", world instanceof WorldSchematic ? "YES" : "NO", pos.toShortString(), inv != null, be != null, nbt != null ? nbt.getString("id") : new NbtCompound());

        if (inv == null || nbt == null)
        {
            return null;
        }

        return new InventoryOverlay.Context(InventoryOverlay.getBestInventoryType(inv, nbt), inv, be != null ? be : world.getBlockEntity(pos), null, nbt, new Refresher());
    }

    // This really isn't used for this use case; but this is just here for Compat
    public static class Refresher implements InventoryOverlay.Refresher
    {

        @Override
        public InventoryOverlay.Context onContextRefresh(InventoryOverlay.Context data, World world)
        {
            // Refresh data
            if (data.be() != null)
            {
                getTargetInventory(world, data.be().getPos());
                data = getTargetInventoryFromBlock(data.be().getWorld(), data.be().getPos(), data.be(), data.nbt());
            }
            /*
            else if (data.entity() != null)
            {
                EntitiesDataStorage.getInstance().requestEntity(world, data.entity().getId());
                data = getTargetInventoryFromEntity(data.entity(), data.nbt());
            }
             */

            return data;
        }
    }

    /**
     * Converts an NbtCompound representation of an ItemStack into a '/give' compatible string.
     * This is the format used by the ItemStringReader(), including Data Components.
     *
     * @param nbt (Nbt Input, must be valid ItemStack.encode() format)
     * @return (The String Result | NULL if the NBT is invalid)
     */
    @Nullable
    public static String convertItemNbtToString(NbtCompound nbt)
    {
        StringBuilder result = new StringBuilder();

        if (nbt.isEmpty())
        {
            return null;
        }

        if (nbt.contains("id"))
        {
            result.append(nbt.getString("id"));
        }
        else
        {
            return null;
        }
        if (nbt.contains("components"))
        {
            NbtCompound components = nbt.getCompound("components");
            int count = 0;

            result.append("[");

            for (String key : components.getKeys())
            {
                if (count > 0)
                {
                    result.append(", ");
                }

                result.append(key);
                result.append("=");
                result.append(components.get(key));
                count++;
            }

            result.append("]");
        }
        if (nbt.contains("count"))
        {
            int count = nbt.getInt("count");

            if (count > 1)
            {
                result.append(" ");
                result.append(count);
            }
        }

        return result.toString();
    }

    /**
     * Post Re-Write Code
     * -
     * Re-stocks more items to the stack in the player's current hotbar slot.
     * @param threshold the number of items at or below which the re-stocking will happen
     * @param allowHotbar whether to allow taking items from other hotbar slots
     */
    @Experimental
    public static void PRW_preRestockHand(PlayerEntity player,
                                          Hand hand,
                                          int threshold,
                                          boolean allowHotbar)
    {
        PlayerInventory container = player.getInventory();
        final ItemStack handStack = player.getStackInHand(hand);
        final int count = handStack.getCount();
        final int max = handStack.getMaxCount();

        if (handStack.isEmpty() == false &&
            PRW_getCursorStack().isEmpty() &&
            (count <= threshold && count < max))
        {
            int endSlot = allowHotbar ? 44 : 35;
            int currentMainHandSlot = PRW_getSelectedHotbarSlot() + 36;
            int currentSlot = hand == Hand.MAIN_HAND ? currentMainHandSlot : 45;

            for (int slotNum = 9; slotNum <= endSlot; ++slotNum)
            {
                if (slotNum == currentMainHandSlot)
                {
                    continue;
                }

                MinecraftClient mc = MinecraftClient.getInstance();
                ScreenHandler handler = player.playerScreenHandler;

                Slot slot = handler.slots.get(slotNum);
                ItemStack stackSlot = container.getStack(slotNum);

                if (fi.dy.masa.malilib.util.InventoryUtils.areStacksEqualIgnoreDurability(stackSlot, handStack))
                {
                    // If all the items from the found slot can fit into the current
                    // stack in hand, then left click, otherwise right click to split the stack
                    int button = stackSlot.getCount() + count <= max ? 0 : 1;

                    //clickSlot(container, slot, button, ClickType.PICKUP);
                    //clickSlot(container, currentSlot, 0, ClickType.PICKUP);

                    mc.interactionManager.clickSlot(handler.syncId, slot.id, button, SlotActionType.PICKUP, player);
                    mc.interactionManager.clickSlot(handler.syncId, currentSlot, 0, SlotActionType.PICKUP, player);

                    break;
                }
            }
        }
    }

    @Experimental
    public static ItemStack PRW_getCursorStack()
    {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null)
        {
            return ItemStack.EMPTY;
        }
        PlayerInventory inv = player.getInventory();
        return inv != null ? inv.getMainHandStack() : ItemStack.EMPTY;
    }

    @Experimental
    public static int PRW_getSelectedHotbarSlot()
    {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null)
        {
            return 0;
        }
        PlayerInventory inv = player.getInventory();
        return inv != null ? inv.selectedSlot : 0;
    }
}
