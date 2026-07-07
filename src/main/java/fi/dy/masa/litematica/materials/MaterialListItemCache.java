package fi.dy.masa.litematica.materials;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import fi.dy.masa.malilib.util.ItemType;

/**
 * Maintains a cache of recently accessed items from opened containers.
 * Items are ordered by recency - most recently seen items are at the front.
 * This allows material lists to prioritize showing items that are currently accessible.
 */
public class MaterialListItemCache
{
    private static final MaterialListItemCache INSTANCE = new MaterialListItemCache();

    // LinkedHashSet maintains insertion order while preventing duplicates
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

        // Collect all unique items from this container
        List<ItemType> foundItems = new ArrayList<>();

        for (Slot slot : slots)
        {
            if (slot.hasStack())
            {
                ItemStack stack = slot.getStack();
                // Use ItemType to compare items without NBT
                ItemType itemType = new ItemType(stack, false, false);

                // Only add if not already in our found list for this container
                if (!foundItems.contains(itemType))
                {
                    foundItems.add(itemType);
                }
            }
        }

        // Update cache: remove then re-add to move items to the front
        for (ItemType itemType : foundItems)
        {
            this.cachedItems.remove(itemType);
            this.cachedItems.add(itemType);
        }
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

        // Convert LinkedHashSet to list to get index (more efficient than iterating)
        List<ItemType> items = new ArrayList<>(this.cachedItems);

        // Reverse index so most recent (last in set) = lowest priority number
        for (int i = items.size() - 1; i >= 0; i--)
        {
            if (items.get(i).equals(itemType))
            {
                return items.size() - 1 - i; // 0 = most recent
            }
        }

        return Integer.MAX_VALUE; // Not in cache
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
