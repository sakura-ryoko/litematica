package fi.dy.masa.litematica.util.post_rewrite;

import javax.annotation.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

import fi.dy.masa.litematica.config.Configs;

public class PickBlockUtils
{
    @Nullable
    public static Hand doPickBlockForStack(ItemStack stack)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null)
        {
            return null;
        }
        boolean ignoreNbt = Configs.Test.PICK_BLOCK_IGNORE_NBT.getBooleanValue();
        Hand hand = EntityUtils2.getUsedHandForItem(player, stack, ignoreNbt);

        if (stack.isEmpty() == false && hand == null)
        {
            //switchItemToHand(stack, ignoreNbt);
            //hand = EntityWrap.getUsedHandForItem(player, stack, ignoreNbt);

            fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack, mc);
            hand = Hand.MAIN_HAND;
        }

        if (hand != null)
        {
            InventoryUtils2.preRestockHand(player, hand, 6, true);
        }

        return hand;
    }

    /*
    @Nullable
    public static Hand pickBlockLast()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        BlockPos pos = Registry.BLOCK_PLACEMENT_POSITION_HANDLER.getCurrentPlacementPosition();

        if (mc.player == null)
        {
            return null;
        }

        // No overrides by other mods
        if (pos == null)
        {
            double reach = mc.player.getBlockInteractionRange();
            Entity entity = mc.getCameraEntity();
            pos = RayTraceUtils2.getPickBlockLastTrace(world, entity, reach, true);
        }

        if (pos != null && PlacementUtils.isReplaceable(world, pos, true))
        {
            return doPickBlockForPosition(pos);
        }

        return null;
    }

    @Nullable
    private static Hand doPickBlockForPosition(BlockPos pos)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null)
        {
            return null;
        }

        World world = SchematicWorldHandler.getSchematicWorld();
        World clientWorld = mc.world;
        if (world == null || clientWorld == null)
        {
            return null;
        }
        BlockState state = world.getBlockState(pos);
        ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);
        boolean ignoreNbt = Configs.Test.PICK_BLOCK_IGNORE_NBT.getBooleanValue();

        if (stack.isEmpty() == false)
        {
            Hand hand = EntityUtils2.getUsedHandForItem(player, stack, ignoreNbt);

            if (hand == null)
            {
                if (player.isCreative() && GuiBase.isCtrlDown())
                {
                    BlockEntity te = world.getBlockEntity(pos);

                    // The creative mode pick block with NBT only works correctly
                    // if the server world doesn't have a TileEntity in that position.
                    // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                    if (te != null && mc.world.isAir(pos))
                    {
                        stack = stack.copy();
                        //ItemUtils.storeBlockEntityInStack(stack, te);
                        te.setStackNbt(stack, clientWorld.getRegistryManager());
                    }
                }

                return doPickBlockForStack(stack);
            }

            return hand;
        }

        return null;
    }
     */
}
