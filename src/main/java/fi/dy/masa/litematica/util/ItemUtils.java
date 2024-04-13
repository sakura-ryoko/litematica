package fi.dy.masa.litematica.util;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemUtils
{
    private static final IdentityHashMap<BlockState, ItemStack> ITEMS_FOR_STATES = new IdentityHashMap<>();

    /*

    *** Irrelevant to use any further; ComponentMap's are set, but are EMPTY so why bother checking them this way?

    public static boolean areTagsEqualIgnoreDamage(ItemStack stackReference, ItemStack stackToCheck)
    {
        ComponentMap tagReference = stackReference.getComponents();
        ComponentMap tagToCheck = stackToCheck.getComponents();

        if (tagReference != null && tagToCheck != null)
        {
            if (tagReference.contains(DataComponentTypes.DAMAGE) || tagToCheck.contains(DataComponentTypes.DAMAGE))
            {
                return false;
            }
                Set<String> keysReference = new HashSet<>(tagReference.getKeys());

                for (String key : keysReference) {
                    if (key.equals("Damage")) {
                        continue;
                    }

                    if (!Objects.equals(tagReference.get(key), tagToCheck.get(key))) {
                        return false;
                    }
                }
                return Objects.equals(stackReference.get(DataComponentTypes.DAMAGE), stackToCheck.get(DataComponentTypes.DAMAGE));
        }

        return (tagReference == null) && (tagToCheck == null);
    }
*/

    public static ItemStack getItemForState(BlockState state)
    {
        ItemStack stack = ITEMS_FOR_STATES.get(state);
        return stack != null ? stack : ItemStack.EMPTY;
    }

    public static void setItemForBlock(World world, BlockPos pos, BlockState state)
    {
        if (ITEMS_FOR_STATES.containsKey(state) == false)
        {
            ITEMS_FOR_STATES.put(state, getItemForBlock(world, pos, state, false));
        }
    }

    public static ItemStack getItemForBlock(World world, BlockPos pos, BlockState state, boolean checkCache)
    {
        if (checkCache)
        {
            ItemStack stack = ITEMS_FOR_STATES.get(state);

            if (stack != null)
            {
                return stack;
            }
        }

        if (state.isAir())
        {
            return ItemStack.EMPTY;
        }

        ItemStack stack = getStateToItemOverride(state);

        if (stack.isEmpty())
        {
            stack = state.getBlock().getPickStack(world, pos, state);
        }

        if (stack.isEmpty())
        {
            stack = ItemStack.EMPTY;
        }
        else
        {
            overrideStackSize(state, stack);
        }

        ITEMS_FOR_STATES.put(state, stack);

        return stack;
    }

    public static ItemStack getStateToItemOverride(BlockState state)
    {
        if (state.getBlock() == Blocks.LAVA)
        {
            return new ItemStack(Items.LAVA_BUCKET);
        }
        else if (state.getBlock() == Blocks.WATER)
        {
            return new ItemStack(Items.WATER_BUCKET);
        }

        return ItemStack.EMPTY;
    }

    private static void overrideStackSize(BlockState state, ItemStack stack)
    {
        if (state.getBlock() instanceof SlabBlock && state.get(SlabBlock.TYPE) == SlabType.DOUBLE)
        {
            stack.setCount(2);
        }
    }

    /**
     * This is used by Holding CTRL and picking a block from the Schematic World
     */
    public static void storeTEInStack(@Nonnull ItemStack stack, @Nonnull BlockEntity te, @Nonnull DynamicRegistryManager registryManager)
    {
        te.setStackNbt(stack, registryManager);
        stack.set(DataComponentTypes.LORE, new LoreComponent(ImmutableList.of(Text.of("(+NBT)"))));
    }

    public static String getStackString(ItemStack stack)
    {
        if (stack.isEmpty() == false)
        {
            Identifier rl = Registries.ITEM.getId(stack.getItem());

            return String.format("[%s - display: %s - NBT: %s] (%s)",
                                 rl != null ? rl.toString() : "null", stack.getName().getString(),
                                 stack.getComponents() != null ? stack.getComponents().toString() : "<no NBT>", stack);
        }

        return "<empty>";
    }
}
