package fi.dy.masa.litematica.util.post_rewrite;

import javax.annotation.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class EntityUtils2
{
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
}
