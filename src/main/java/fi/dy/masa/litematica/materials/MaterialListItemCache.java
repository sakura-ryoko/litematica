package fi.dy.masa.litematica.materials;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.data.ItemType;
import fi.dy.masa.litematica.config.Configs;

/**
 * Maintains a cache of recently accessed items from opened containers.
 * Items are ordered by recency - most recently seen items are at the front.
 * This allows material lists to prioritize showing items that are currently accessible.
 * -
 * - From Stormatica by CubicMetre
 */
public class MaterialListItemCache
{
    private static final MaterialListItemCache INSTANCE = new MaterialListItemCache();

    private final LinkedHashSet<ItemType> cachedItems = new LinkedHashSet<>();
    private boolean enabled = true;

    private MaterialListItemCache()
    {
    }

    public static MaterialListItemCache getInstance()
    {
        return INSTANCE;
    }

    /**
     * Scans a container's slots and updates the cache with all unique item types found.
     * Items that already exist in the cache are moved to the front (most recent).
     *
     * @param slots The slots from a HandledScreen's ScreenHandler
     */
    public void scanContainer(List<Slot> slots)
    {
        if (!this.enabled)
        {
            return;
        }

        List<ItemType> foundItems = new ArrayList<>();

        for (Slot slot : slots)
        {
            if (slot.hasItem())
            {
                ItemStack stack = slot.getItem();

                if (stack.is(Items.SHULKER_BOX) && Configs.Generic.MATERIAL_LIST_CONTAINER_SCAN_SHULKERS.getBooleanValue())
                {
                    NonNullList<ItemStack> storedItems = this.updateFromShulker(stack);

                    for (ItemStack item : storedItems)
                    {
                        ItemType itemType = new ItemType(item, false, false);

                        if (!foundItems.contains(itemType))
                        {
                            foundItems.add(itemType);
                        }
                    }
                }
                else if (stack.is(ItemTags.BUNDLES) && Configs.Generic.MATERIAL_LIST_CONTAINER_SCAN_BUNDLES.getBooleanValue())
                {
                    NonNullList<ItemStack> storedItems = this.updateFromBundle(stack);

                    for (ItemStack item : storedItems)
                    {
                        ItemType itemType = new ItemType(item, false, false);

                        if (!foundItems.contains(itemType))
                        {
                            foundItems.add(itemType);
                        }
                    }
                }
                else
                {
                    ItemType itemType = new ItemType(stack, false, false);

                    if (!foundItems.contains(itemType))
                    {
                        foundItems.add(itemType);
                    }
                }
            }
        }

        for (ItemType itemType : foundItems)
        {
            this.cachedItems.remove(itemType);
            this.cachedItems.add(itemType);
        }
    }

    private NonNullList<ItemStack> updateFromShulker(ItemStack stack)
    {
        if (stack.is(Items.SHULKER_BOX) && InventoryUtils.shulkerBoxHasItems(stack))
        {
            NonNullList<ItemStack> storedItems = InventoryUtils.getStoredItems(stack);

            if (storedItems != null && !storedItems.isEmpty())
            {
                return storedItems;
            }
        }

        return NonNullList.create();
    }

    private NonNullList<ItemStack> updateFromBundle(ItemStack stack)
    {
        if (stack.is(ItemTags.BUNDLES) && InventoryUtils.bundleHasItems(stack))
        {
            NonNullList<ItemStack> storedItems = InventoryUtils.getBundleItems(stack);

            if (storedItems != null && !storedItems.isEmpty())
            {
                return storedItems;
            }
        }

        return NonNullList.create();
    }

    /**
     * Gets the cache priority for a specific item type.
     * Lower numbers = higher priority (more recently accessed).
     * Returns Integer.MAX_VALUE if item is not in cache.
     *
     * @param stack The item stack to check
     * @return Priority index (0 = highest priority)
     */
    public int getCachePriority(ItemStack stack)
    {
        if (!this.enabled)
        {
            return Integer.MAX_VALUE;
        }

        ItemType itemType = new ItemType(stack, false, false);

        List<ItemType> items = new ArrayList<>(this.cachedItems);

        // Reverse index so most recent (last in set) = lowest priority number
        for (int i = items.size() - 1; i >= 0; i--)
        {
            if (items.get(i).equals(itemType))
            {
                return items.size() - 1 - i; // 0 = most recent
            }
        }

        return Integer.MAX_VALUE;
    }

    /**
     * Clears all cached items
     */
    public void clear()
    {
        this.cachedItems.clear();
    }

    /**
     * Enables or disables the cache system
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;

        if (!enabled)
        {
            this.clear();
        }
    }

    /**
     * Returns whether the cache is currently enabled
     */
    public boolean isEnabled()
    {
        return this.enabled;
    }

    /**
     * Gets the number of unique items currently cached
     */
    public int getCacheSize()
    {
        return this.cachedItems.size();
    }
}
