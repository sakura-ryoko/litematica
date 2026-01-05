package fi.dy.masa.litematica.util;

import javax.annotation.Nullable;
import org.jetbrains.annotations.ApiStatus;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.game.BlockUtils;
import fi.dy.masa.malilib.util.game.PlacementUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

/**
 * Post Re-Write Code
 */
@ApiStatus.Experimental
public class PickBlockUtils
{
	// FIXME DO NOT USE
    @Nullable
    public static InteractionHand doPickBlockForStack(ItemStack stack)
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
        {
            return null;
        }
//        boolean ignoreNbt = Configs.Generic.PICK_BLOCK_IGNORE_NBT.getBooleanValue();
        boolean ignoreNbt = false;
        InteractionHand hand = EntityUtils.getUsedHandForItem(player, stack, ignoreNbt);

        if (stack.isEmpty() == false && hand == null)
        {
            //switchItemToHand(stack, ignoreNbt);
            //hand = EntityWrap.getUsedHandForItem(player, stack, ignoreNbt);

//            fi.dy.masa.malilib.util.InventoryUtils.swapItemToMainHand(stack, mc);
//            hand = Hand.MAIN_HAND;
			return null;
        }

        if (hand != null)
        {
            InventoryUtils.preRestockHand(player, hand, 6, true);
        }

        return hand;
    }

    // FIXME DO NOT USE
	@Nullable
    public static InteractionHand pickBlockLast()
    {
        Minecraft mc = Minecraft.getInstance();
        Level world = mc.level;
        BlockPos pos = Registry.BLOCK_PLACEMENT_POSITION_HANDLER.getCurrentPlacementPosition();

        if (mc.player == null)
        {
            return null;
        }

        // No overrides by other mods
        if (pos == null)
        {
            double reach = mc.player.blockInteractionRange();
            Entity entity = mc.getCameraEntity();

			if (entity != null)
			{
				pos = RayTraceUtils.getPickBlockLastTrace(world, entity, reach, true);
			}
        }

        if (pos != null && world != null && PlacementUtils.isReplaceable(world, pos, true))
        {
            return doPickBlockForPosition(pos);
        }

        return null;
    }

	// FIXME DO NOT USE
    @Nullable
    private static InteractionHand doPickBlockForPosition(BlockPos pos)
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
        {
            return null;
        }

        Level world = SchematicWorldHandler.getSchematicWorld();
        Level clientWorld = mc.level;
        if (world == null || clientWorld == null)
        {
            return null;
        }
        BlockState state = world.getBlockState(pos);
//        ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);
        ItemStack stack = state.getBlock().asItem().getDefaultInstance();
//        boolean ignoreNbt = Configs.Generic.PICK_BLOCK_IGNORE_NBT.getBooleanValue();
        boolean ignoreNbt = false;

        if (stack.isEmpty() == false)
        {
            InteractionHand hand = EntityUtils.getUsedHandForItem(player, stack, ignoreNbt);

            if (hand == null)
            {
                if (player.isCreative() && GuiBase.isCtrlDown())
                {
                    BlockEntity te = world.getBlockEntity(pos);

                    // The creative mode pick block with NBT only works correctly
                    // if the server world doesn't have a TileEntity in that position.
                    // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                    if (te != null && mc.level.isEmptyBlock(pos))
                    {
                        stack = stack.copy();
                        //ItemUtils.storeBlockEntityInStack(stack, te);
                        //te.setStackNbt(stack, clientWorld.getRegistryManager());
                        BlockUtils.setStackNbt(stack, te, clientWorld.registryAccess());
                    }
                }

                return doPickBlockForStack(stack);
            }

            return hand;
        }

        return null;
    }
}
