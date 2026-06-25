package fi.dy.masa.litematica.materials;

import java.util.*;

import fi.dy.masa.litematica.Litematica;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.ItemType;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic.EntityInfo;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.util.InclusionType;

/**
 * Naming convention used in this class:
 * - "create*ItemCounts"  : tally up item counts (Object2IntOpenHashMap<ItemType>) from schematic/world data
 * - "createMaterialList*": build the final List<MaterialListEntry> for a whole schematic/placement
 * - "buildEntriesFrom*"  : turn already-tallied item/block counts into a List<MaterialListEntry>
 */
public class MaterialListUtils
{
    /**
     * Builds the material list for a schematic that hasn't been placed yet, i.e. there is no
     * world to compare against, so everything is reported as missing.
     */
    public static List<MaterialListEntry> createMaterialListForSchematic(LitematicaSchematic schematic,
                                                                           Collection<String> subRegions,
                                                                           InclusionType entitiesInclusionType,
                                                                           InclusionType containersInclusionType)
    {
        Player player = Minecraft.getInstance().player;

        if (entitiesInclusionType == InclusionType.ONLY)
        {
            Object2IntOpenHashMap<ItemType> entitiesTotal = createEntityItemCounts(schematic, subRegions);
            return buildEntriesFromItemCounts(entitiesTotal, entitiesTotal.clone(), new Object2IntOpenHashMap<>(), player);
        }

        if (containersInclusionType == InclusionType.ONLY)
        {
            Object2IntOpenHashMap<ItemType> containersTotal = createContainerItemCounts(schematic, subRegions);
            return buildEntriesFromItemCounts(containersTotal, containersTotal.clone(), new Object2IntOpenHashMap<>(), player);
        }

        Object2IntOpenHashMap<ItemType> total = createBlockItemCounts(schematic, subRegions);

        if (entitiesInclusionType != InclusionType.NONE)
        {
            createEntityItemCounts(schematic, subRegions).forEach(total::addTo);
        }

        if (containersInclusionType != InclusionType.NONE)
        {
            createContainerItemCounts(schematic, subRegions).forEach(total::addTo);
        }

        return buildEntriesFromItemCounts(total, total.clone(), new Object2IntOpenHashMap<>(), player);
    }

    public static List<MaterialListEntry> createBlocksList(LitematicaSchematic schematic)
    {
        return createMaterialListForSchematic(schematic, schematic.getAreas().keySet(), InclusionType.NONE, InclusionType.NONE);
    }

    public static Object2IntOpenHashMap<ItemType> createBlockItemCounts(LitematicaSchematic schematic, Collection<String> subRegions)
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
        MaterialCache cache = MaterialCache.getInstance();

        return convertBlockStatesToItemCounts(countsTotal, cache);
    }

    public static Object2IntOpenHashMap<ItemType> createEntityItemCounts(LitematicaSchematic schematic, Collection<String> subRegions)
    {
        Object2IntOpenHashMap<ItemType> entitiesTotal = new Object2IntOpenHashMap<>();

        for (String regionName : subRegions) {
           List<EntityInfo> entitiesList = schematic.getEntityListForRegion(regionName);
           if (entitiesList != null) {
               for (EntityInfo entityInfo : entitiesList) {
                   String id = entityInfo.nbt.getStringOr("id", "");
                   if (!id.isEmpty()) {
                       Identifier identifier = Identifier.tryParse(id);
                       Item item = BuiltInRegistries.ITEM.getValue(identifier);
                       ItemType itemType = new ItemType(new ItemStack(item), false);
                       entitiesTotal.addTo(itemType, 1);
                   }
               }
           }
        }

        return entitiesTotal;
    }

    public static Object2IntOpenHashMap<ItemType> createContainerItemCounts(LitematicaSchematic schematic, Collection<String> subRegions)
    {
        Object2IntOpenHashMap<ItemType> containersTotal = new Object2IntOpenHashMap<>();
        for (String regionName : subRegions) {
            Collection <CompoundTag> containersList = schematic.getBlockEntityMapForRegion(regionName).values();
            List<EntityInfo> entitiesList = schematic.getEntityListForRegion(regionName);
            ListTag listTag = new ListTag();
            for (CompoundTag containerTag : containersList) {
                listTag.addAll(containerTag.getListOrEmpty("Items"));
            }
            for (EntityInfo entityInfo : entitiesList) {
                if (entityInfo.nbt.contains("Items")) {
                    listTag.addAll(entityInfo.nbt.getListOrEmpty("Items"));
                }
            }
            for (Tag tag : listTag) {
                if (tag instanceof CompoundTag itemTag) {
                    accumulateContainerItem(itemTag, containersTotal);
                }
            }
        }

        return containersTotal;
    }

    /**
     * Adds the counts for a single item stack's NBT, and -- since shulker boxes can't be nested --
     * unpacks one extra level of "minecraft:container" contents if the item itself is a shulker box.
     * Bundles are intentionally not unpacked here.
     */
    private static void accumulateContainerItem(CompoundTag itemTag, Object2IntOpenHashMap<ItemType> containersTotal)
    {
        addItemTagCount(itemTag, containersTotal);

        CompoundTag components = itemTag.getCompoundOrEmpty("components");
        ListTag shulkerItems = components.getListOrEmpty("minecraft:container");

        for (Tag slotTag : shulkerItems) {
            if (slotTag instanceof CompoundTag slotCompound && slotCompound.contains("item")) {
                addItemTagCount(slotCompound.getCompoundOrEmpty("item"), containersTotal);
            }
        }
    }

    private static void addItemTagCount(CompoundTag itemTag, Object2IntOpenHashMap<ItemType> total)
    {
        Identifier identifier = Identifier.tryParse(itemTag.getStringOr("id", ""));
        Item item = BuiltInRegistries.ITEM.getValue(identifier);
        int count = itemTag.getIntOr("count", 0);
        ItemType itemType = new ItemType(new ItemStack(item), false);
        total.addTo(itemType, count);
    }

    /**
     * Turns already-tallied item counts into the final entry list, looking up availability from the player's inventory.
     */
    public static List<MaterialListEntry> buildEntriesFromItemCounts(
            Object2IntOpenHashMap<ItemType> itemTypesTotal,
            Object2IntOpenHashMap<ItemType> itemTypesMissing,
            Object2IntOpenHashMap<ItemType> itemTypesMismatch,
            Player player)
    {
        List<MaterialListEntry> list = new ArrayList<>();

        if (!itemTypesTotal.isEmpty())
        {
            Object2IntOpenHashMap<ItemType> playerInvItems = player != null ? getInventoryItemCounts(player.getInventory()) : new Object2IntOpenHashMap<>();

            for (ItemType type : itemTypesTotal.keySet())
            {
                list.add(new MaterialListEntry(type.getStack().copy(),
                                               itemTypesTotal.getInt(type),
                                               itemTypesMissing.getInt(type),
                                               itemTypesMismatch.getInt(type),
                                               playerInvItems.getInt(type)));
            }
        }
        return list;
    }

    /**
     * Builds the material list for a placement (or other live-world block count), with no entities/containers.
     */
    public static List<MaterialListEntry> buildEntriesFromBlockCounts(
            Object2IntOpenHashMap<BlockState> countsTotal,
            Object2IntOpenHashMap<BlockState> countsMissing,
            Object2IntOpenHashMap<BlockState> countsMismatch,
            Player player)
    {
        if (countsTotal.isEmpty()) {
            return new ArrayList<>();
        }

        MaterialCache cache = MaterialCache.getInstance();
        Object2IntOpenHashMap<ItemType> itemTypesTotal = convertBlockStatesToItemCounts(countsTotal, cache);
        Object2IntOpenHashMap<ItemType> itemTypesMissing = convertBlockStatesToItemCounts(countsMissing, cache);
        Object2IntOpenHashMap<ItemType> itemTypesMismatch = convertBlockStatesToItemCounts(countsMismatch, cache);

        return buildEntriesFromItemCounts(itemTypesTotal, itemTypesMissing, itemTypesMismatch, player);
    }

    /**
     * Builds the material list for a placement, combining live-world block counts with entity/container
     * item counts gathered separately (e.g. filtered to what's visible in the current render layer).
     * Entities/containers are counted from the schematic's ghost/overlay world, so there is no real-world
     * match to check availability against: they are always reported as missing.
     */
    public static List<MaterialListEntry> buildEntriesForPlacement(
            Object2IntOpenHashMap<BlockState> countsTotal,
            Object2IntOpenHashMap<BlockState> countsMissing,
            Object2IntOpenHashMap<BlockState> countsMismatch,
            Object2IntOpenHashMap<ItemType> entitiesTotal,
            InclusionType entitiesInclusionType,
            Object2IntOpenHashMap<ItemType> containersTotal,
            InclusionType containersInclusionType,
            Player player)
    {
        if (entitiesInclusionType == InclusionType.ONLY)
        {
            return buildEntriesFromItemCounts(entitiesTotal, entitiesTotal.clone(), new Object2IntOpenHashMap<>(), player);
        }

        if (containersInclusionType == InclusionType.ONLY)
        {
            return buildEntriesFromItemCounts(containersTotal, containersTotal.clone(), new Object2IntOpenHashMap<>(), player);
        }

        Object2IntOpenHashMap<ItemType> itemTypesTotal;
        Object2IntOpenHashMap<ItemType> itemTypesMissing;
        Object2IntOpenHashMap<ItemType> itemTypesMismatch;

        if (countsTotal.isEmpty())
        {
            itemTypesTotal = new Object2IntOpenHashMap<>();
            itemTypesMissing = new Object2IntOpenHashMap<>();
            itemTypesMismatch = new Object2IntOpenHashMap<>();
        }
        else
        {
            MaterialCache cache = MaterialCache.getInstance();
            itemTypesTotal = convertBlockStatesToItemCounts(countsTotal, cache);
            itemTypesMissing = convertBlockStatesToItemCounts(countsMissing, cache);
            itemTypesMismatch = convertBlockStatesToItemCounts(countsMismatch, cache);
        }

        mergeAsMissing(itemTypesTotal, itemTypesMissing, entitiesTotal, entitiesInclusionType);
        mergeAsMissing(itemTypesTotal, itemTypesMissing, containersTotal, containersInclusionType);

        return buildEntriesFromItemCounts(itemTypesTotal, itemTypesMissing, itemTypesMismatch, player);
    }

    /**
     * Builds the material list for the area analyzer: a plain inventory of what's currently present
     * in the selected area, with no "missing" concept (there is nothing to compare the area against).
     */
    public static List<MaterialListEntry> buildEntriesForAreaAnalyzer(
            Object2IntOpenHashMap<BlockState> countsTotal,
            Object2IntOpenHashMap<ItemType> entitiesTotal,
            InclusionType entitiesInclusionType,
            Object2IntOpenHashMap<ItemType> containersTotal,
            InclusionType containersInclusionType,
            Player player)
    {
        if (entitiesInclusionType == InclusionType.ONLY)
        {
            return buildEntriesFromItemCounts(entitiesTotal, new Object2IntOpenHashMap<>(), new Object2IntOpenHashMap<>(), player);
        }

        if (containersInclusionType == InclusionType.ONLY)
        {
            return buildEntriesFromItemCounts(containersTotal, new Object2IntOpenHashMap<>(), new Object2IntOpenHashMap<>(), player);
        }

        Object2IntOpenHashMap<ItemType> itemTypesTotal = countsTotal.isEmpty()
                ? new Object2IntOpenHashMap<>()
                : convertBlockStatesToItemCounts(countsTotal, MaterialCache.getInstance());

        if (entitiesInclusionType != InclusionType.NONE)
        {
            entitiesTotal.forEach(itemTypesTotal::addTo);
        }

        if (containersInclusionType != InclusionType.NONE)
        {
            containersTotal.forEach(itemTypesTotal::addTo);
        }

        Object2IntOpenHashMap<ItemType> empty = new Object2IntOpenHashMap<>();
        return buildEntriesFromItemCounts(itemTypesTotal, empty, empty, player);
    }

    private static void mergeAsMissing(Object2IntOpenHashMap<ItemType> total,
                                        Object2IntOpenHashMap<ItemType> missing,
                                        Object2IntOpenHashMap<ItemType> extra,
                                        InclusionType inclusionType)
    {
        if (inclusionType == InclusionType.NONE)
        {
            return;
        }

        for (ItemType itemType : extra.keySet())
        {
            int count = extra.getInt(itemType);
            total.addTo(itemType, count);
            missing.addTo(itemType, count);
        }
    }

    private static Object2IntOpenHashMap<ItemType> convertBlockStatesToItemCounts(
            Object2IntOpenHashMap<BlockState> blockStatesIn,
            MaterialCache cache)
    {
        Object2IntOpenHashMap<ItemType> itemTypesOut = new Object2IntOpenHashMap<>();
        for (BlockState state : blockStatesIn.keySet())
        {
            int count = blockStatesIn.getInt(state);
            BlockState stateToConvert = isWaterloggedBlock(state) ? getBaseBlockState(state) : state;

            // Add water bucket for waterlogged blocks
            if (isWaterloggedBlock(state))
            {
                itemTypesOut.addTo(new ItemType(new ItemStack(Items.WATER_BUCKET), false, false), count);
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

        return itemTypesOut;
    }

    public static void updateAvailableCounts(List<MaterialListEntry> list, Player player)
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

    public static Object2IntOpenHashMap<ItemType> getInventoryItemCounts(Container inv)
    {
        Object2IntOpenHashMap<ItemType> map = new Object2IntOpenHashMap<>();
        final int slots = inv.getContainerSize();

        for (int slot = 0; slot < slots; ++slot)
        {
            ItemStack stack = inv.getItem(slot);

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
        NonNullList<ItemStack> items = InventoryUtils.getStoredItems(stackShulkerBox);

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
        NonNullList<ItemStack> items = InventoryUtils.getBundleItems(stackBundle);

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
        return state.hasProperty(BlockStateProperties.WATERLOGGED) &&
               state.getValue(BlockStateProperties.WATERLOGGED);
    }

    private static BlockState getBaseBlockState(BlockState state)
    {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED))
        {
            return state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        return state;
    }
}
