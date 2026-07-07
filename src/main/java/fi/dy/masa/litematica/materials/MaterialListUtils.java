package fi.dy.masa.litematica.materials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3i;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.ItemType;

public class MaterialListUtils
{
    public static List<MaterialListEntry> createMaterialListFor(LitematicaSchematic schematic)
    {
        return createMaterialListFor(schematic, schematic.getAreas().keySet());
    }

    /**
     * Creates a material list directly from item types and quantities,
     * bypassing the block-based conversion system. This allows tracking
     * of any item type including non-placeable items like tools and food.
     *
     * @param items Map of ItemType to quantity
     * @param player Player entity for inventory tracking (can be null)
     * @return List of MaterialListEntry objects
     */
    public static List<MaterialListEntry> createMaterialListFromItems(
            java.util.Map<ItemType, Integer> items,
            PlayerEntity player)
    {
        List<MaterialListEntry> list = new ArrayList<>();

        if (items.isEmpty())
        {
            return list;
        }

        Object2IntOpenHashMap<ItemType> playerInvItems = null;

        if (player != null)
        {
            playerInvItems = getInventoryItemCounts(player.getInventory());
        }

        for (java.util.Map.Entry<ItemType, Integer> entry : items.entrySet())
        {
            ItemType type = entry.getKey();
            int count = entry.getValue();
            int countAvailable = playerInvItems != null ? playerInvItems.getInt(type) : 0;

            // For custom item lists, total = missing (no placement state to compare against)
            list.add(new MaterialListEntry(
                type.getStack().copy(),
                count,           // countTotal
                count,           // countMissing (all items are "missing" since nothing is placed)
                0,               // countMismatched (not applicable for custom lists)
                countAvailable   // countAvailable (from player inventory)
            ));
        }

        return list;
    }

    public static List<MaterialListEntry> createMaterialListFor(LitematicaSchematic schematic, Collection<String> subRegions)
    {
        Object2IntOpenHashMap<BlockState> countsTotal = new Object2IntOpenHashMap<>();

        for (String regionName : subRegions)
        {
            LitematicaBlockStateContainer container = schematic.getSubRegionContainer(regionName);

            if (container != null)
            {
                Vec3i size = container.getSize();
                final int sizeX = size.getX();
                final int sizeY = size.getY();
                final int sizeZ = size.getZ();

                for (int y = 0; y < sizeY; ++y)
                {
                    for (int z = 0; z < sizeZ; ++z)
                    {
                        for (int x = 0; x < sizeX; ++x)
                        {
                            BlockState state = container.get(x, y, z);
                            countsTotal.addTo(state, 1);
                        }
                    }
                }
            }
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        return getMaterialList(countsTotal, countsTotal.clone(), new Object2IntOpenHashMap<>(), mc.player);
    }

    public static List<MaterialListEntry> getMaterialList(
            Object2IntOpenHashMap<BlockState> countsTotal,
            Object2IntOpenHashMap<BlockState> countsMissing,
            Object2IntOpenHashMap<BlockState> countsMismatch,
            PlayerEntity player)
    {
        List<MaterialListEntry> list = new ArrayList<>();

        if (!countsTotal.isEmpty())
        {
            MaterialCache cache = MaterialCache.getInstance();
            Object2IntOpenHashMap<ItemType> itemTypesTotal = new Object2IntOpenHashMap<>();
            Object2IntOpenHashMap<ItemType> itemTypesMissing = new Object2IntOpenHashMap<>();
            Object2IntOpenHashMap<ItemType> itemTypesMismatch = new Object2IntOpenHashMap<>();

            convertStatesToStacks(countsTotal, itemTypesTotal, cache);
            convertStatesToStacks(countsMissing, itemTypesMissing, cache);
            convertStatesToStacks(countsMismatch, itemTypesMismatch, cache);

            if (player != null)
            {
                Object2IntOpenHashMap<ItemType> playerInvItems = getInventoryItemCounts(player.getInventory());

                for (ItemType type : itemTypesTotal.keySet())
                {
                    list.add(new MaterialListEntry(type.getStack().copy(),
                                                   itemTypesTotal.getInt(type),
                                                   itemTypesMissing.getInt(type),
                                                   itemTypesMismatch.getInt(type),
                                                   playerInvItems.getInt(type)));
                }
            }
            else
            {
                for (ItemType type : itemTypesTotal.keySet())
                {
                    list.add(new MaterialListEntry(type.getStack().copy(),
                                                   itemTypesTotal.getInt(type),
                                                   itemTypesMissing.getInt(type),
                                                   itemTypesMismatch.getInt(type),
                                                   0));
                }
            }
        }

        return list;
    }

    private static void convertStatesToStacks(
            Object2IntOpenHashMap<BlockState> blockStatesIn,
            Object2IntOpenHashMap<ItemType> itemTypesOut,
            MaterialCache cache)
    {
        for (BlockState state : blockStatesIn.keySet())
        {
            int count = blockStatesIn.getInt(state);
            BlockState stateToConvert = isWaterloggedBlock(state) ? getBaseBlockState(state) : state;

            // Add water bucket for waterlogged blocks
            if (isWaterloggedBlock(state))
            {
                itemTypesOut.addTo(new ItemType(new ItemStack(net.minecraft.item.Items.WATER_BUCKET), false, false), count);
            }

            // Convert block to items
            if (cache.requiresMultipleItems(stateToConvert))
            {
                for (ItemStack stack : cache.getItems(stateToConvert))
                {
                    if (!stack.isEmpty())
                    {
                        itemTypesOut.addTo(new ItemType(stack, true, false), count * stack.getCount());
                    }
                }
            }
            else
            {
                ItemStack stack = cache.getRequiredBuildItemForState(stateToConvert);
                if (!stack.isEmpty())
                {
                    itemTypesOut.addTo(new ItemType(stack, true, false), count * stack.getCount());
                }
            }
        }
    }

    public static void updateAvailableCounts(List<MaterialListEntry> list, PlayerEntity player)
    {
        if (player == null) return;
        Object2IntOpenHashMap<ItemType> playerInvItems = getInventoryItemCounts(player.getInventory());

        for (MaterialListEntry entry : list)
        {
            ItemType type = new ItemType(entry.getStack(), true, false);
            int countAvailable = playerInvItems.getInt(type);
            entry.setCountAvailable(countAvailable);
        }
    }

    public static Object2IntOpenHashMap<ItemType> getInventoryItemCounts(Inventory inv)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        final int slots = inv.size();

        for (int slot = 0; slot < slots; ++slot)
        {
            ItemStack stack = inv.getStack(slot);

            if (stack.isEmpty() == false)
            {
                Item item = stack.getItem();

                if (item instanceof BlockItem &&
                    ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock &&
                    InventoryUtils.shulkerBoxHasItems(stack))
                {
                    Object2IntOpenHashMap<ItemType> boxCounts = getStoredItemCounts(stack);

                    for (ItemType boxType : boxCounts.keySet())
                    {
                        map.addTo(boxType, boxCounts.getInt(boxType));
                    }

                    boxCounts.clear();
                }
                else if (item instanceof BundleItem && InventoryUtils.bundleHasItems(stack))
                {
                    Object2IntOpenHashMap<ItemType> bundleCounts = getBundleItemCounts(stack);

                    for (ItemType bundleType : bundleCounts.keySet())
                    {
                        map.addTo(bundleType, bundleCounts.getInt(bundleType));
                    }

                    bundleCounts.clear();
                }
                else
                {
                    map.addTo(new ItemType(stack, true, false), stack.getCount());
                }
            }
        }

        return map;
    }

    public static Object2IntOpenHashMap<ItemType> getStoredItemCounts(ItemStack stackShulkerBox)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        DefaultedList<ItemStack> items = InventoryUtils.getStoredItems(stackShulkerBox);

        for (ItemStack boxStack : items)
        {
            if (boxStack.isEmpty() == false)
            {
                // Copy Nested Bundles
                if (boxStack.getItem() instanceof BundleItem && InventoryUtils.bundleHasItems(boxStack))
                {
                    Object2IntOpenHashMap<ItemType> bundleMap = getBundleItemCounts(boxStack);

                    if (!bundleMap.isEmpty())
                    {
                        bundleMap.forEach(map::addTo);
                    }
                }

                map.addTo(new ItemType(boxStack, false, false), boxStack.getCount());
            }
        }

        return map;
    }

    public static Object2IntOpenHashMap<ItemType> getBundleItemCounts(ItemStack stackBundle)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        DefaultedList<ItemStack> items = InventoryUtils.getBundleItems(stackBundle);

        for (ItemStack bundleStack : items)
        {
            if (bundleStack.isEmpty() == false)
            {
                // Copy Nested Bundles
                if (bundleStack.getItem() instanceof BundleItem && InventoryUtils.bundleHasItems(bundleStack))
                {
                    Object2IntOpenHashMap<ItemType> bundleMap = getBundleItemCounts(bundleStack);

                    if (!bundleMap.isEmpty())
                    {
                        bundleMap.forEach(map::addTo);
                    }
                }

                map.addTo(new ItemType(bundleStack, false, false), bundleStack.getCount());
            }
        }

        return map;
    }

    private static boolean isWaterloggedBlock(BlockState state)
    {
        return state.contains(net.minecraft.state.property.Properties.WATERLOGGED) &&
               state.get(net.minecraft.state.property.Properties.WATERLOGGED);
    }

    private static BlockState getBaseBlockState(BlockState state)
    {
        if (state.contains(net.minecraft.state.property.Properties.WATERLOGGED))
        {
            return state.with(net.minecraft.state.property.Properties.WATERLOGGED, false);
        }
        return state;
    }
}
