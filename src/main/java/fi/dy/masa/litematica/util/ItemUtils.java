package fi.dy.masa.litematica.util;

import java.util.*;

import fi.dy.masa.litematica.Litematica;
import net.minecraft.block.*;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.SlabType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PlayerHeadItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
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
        if (!ITEMS_FOR_STATES.containsKey(state))
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

    public static void storeTEInStack(ItemStack stack, BlockEntity te, DynamicRegistryManager registryManager)
    {
        NbtCompound tag = te.createNbtWithId(registryManager);

        // This is for the "Hold CTRL" and pick block functionality from the Schematic World

        if ((stack.getItem() instanceof BlockItem &&
            ((BlockItem) stack.getItem()).getBlock() instanceof AbstractSkullBlock)
                || (stack.getItem() instanceof PlayerHeadItem))
        {
            //Litematica.logger.info("storeTEInStack(): is skull item");

            if (tag.contains("profile"))
            {
                NbtCompound skullNbt = tag.getCompound("profile");
                ProfileComponent skullProfile = ComponentUtils.getSkullProfileFromProfile(skullNbt);

                if (skullProfile != null)
                {
                    Litematica.debugLog("storeTEInStack(): applying skull profile component from NBT");

                    stack.set(DataComponentTypes.PROFILE, skullProfile);
                }
                else
                {
                    Litematica.logger.warn("storeTEInStack(): failed to fetch user profile from NBT data (null output)");
                }
            }
            else
            {
                Litematica.debugLog("storeTEInStack(): failed to fetch user profile from NBT data (profile not found)");
            }
        }
        if ((stack.getItem() instanceof BlockItem &&
                ((BlockItem) stack.getItem()).getBlock() instanceof BeehiveBlock))
        {
            //Litematica.logger.info("storeTEInStack(): is beehive item");

            if (tag.contains("bees"))
            {
                NbtList beeNbtList = tag.getList("bees", 10);
                List<BeehiveBlockEntity.BeeData> beeList = ComponentUtils.getBeesDataFromNbt(beeNbtList);

                if (beeList.isEmpty())
                {
                    Litematica.debugLog("storeTEInStack(): beeList is empty");
                }
                else
                {
                    Litematica.debugLog("storeTEInStack(): applying bees component from NBT");

                    stack.set(DataComponentTypes.BEES, beeList);
                }
            }
            else
            {
                Litematica.debugLog("storeTEInStack(): failed to fetch beeList from NBT data (bees not found)");
            }
        }

        // TODO So this is where the now "infamous" Purple (+NBT) lore comes from?
        //  To re-add this, you would need to build the LoreComponent
        /*
        Text newNbtLore = Text.of("(+NBT)");
        List<Text> newLoreList = new ArrayList<>();

        newLoreList.add(newNbtLore);

        LoreComponent lore = new LoreComponent(newLoreList);
        stack.set(DataComponentTypes.LORE, lore);
        */
        // TODO

        /*
        tagList.add(NbtString.of("(+NBT)"));

        stack.setSubNbt("BlockEntityTag", tag);
         */

        //Litematica.logger.info("storeTEInStack(): new components: {}", stack.getComponents());
    }

    public static String getStackString(ItemStack stack)
    {
        if (!stack.isEmpty())
        {
            Identifier rl = Registries.ITEM.getId(stack.getItem());

            return String.format("[%s - display: %s - NBT: %s] (%s)",
                                 rl != null ? rl.toString() : "null", stack.getName().getString(),
                                 stack.getComponents() != null ? stack.getComponents().toString() : "<no NBT>", stack);
        }

        return "<empty>";
    }
}
