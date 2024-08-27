package fi.dy.masa.litematica.util.post_rewrite;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class InventoryUtils2
{
    /**
     * Re-stocks more items to the stack in the player's current hotbar slot.
     * @param threshold the number of items at or below which the re-stocking will happen
     * @param allowHotbar whether to allow taking items from other hotbar slots
     */
    public static void preRestockHand(PlayerEntity player,
                                      Hand hand,
                                      int threshold,
                                      boolean allowHotbar)
    {
        PlayerInventory container = player.getInventory();
        final ItemStack handStack = player.getStackInHand(hand);
        final int count = handStack.getCount();
        final int max = handStack.getMaxCount();

        if (handStack.isEmpty() == false &&
                getCursorStack().isEmpty() &&
                (count <= threshold && count < max))
        {
            int endSlot = allowHotbar ? 44 : 35;
            int currentMainHandSlot = getSelectedHotbarSlot() + 36;
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

    public static ItemStack getCursorStack()
    {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null)
        {
            return ItemStack.EMPTY;
        }
        PlayerInventory inv = player.getInventory();
        return inv != null ? inv.getMainHandStack() : ItemStack.EMPTY;
    }

    public static int getSelectedHotbarSlot()
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
