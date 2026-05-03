package fi.dy.masa.litematica.data;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.malilib.data.CachedItemTags;
import fi.dy.masa.malilib.data.CachedTagKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Caches Block/Item Tags as if they are real Vanilla Block/Item tags.
 */
public class CachedTagManager
{
	public static final CachedTagKey GLASS_ITEMS_KEY                = new CachedTagKey(Reference.MOD_ID, "glass_items");
	public static final CachedTagKey GLASS_PANE_ITEMS_KEY           = new CachedTagKey(Reference.MOD_ID, "glass_pane_items");
	public static final CachedTagKey CONCRETE_POWDER_ITEMS_KEY      = new CachedTagKey(Reference.MOD_ID, "concrete_powder_items");
	public static final CachedTagKey CONCRETE_ITEMS_KEY             = new CachedTagKey(Reference.MOD_ID, "concrete_items");
	public static final CachedTagKey GLAZED_TERRACOTTA_ITEMS_KEY    = new CachedTagKey(Reference.MOD_ID, "glazed_terracotta_items");
	public static final CachedTagKey PACKED_BLOCK_ITEMS_KEY         = new CachedTagKey(Reference.MOD_ID, "packed_block_items");
    public static final CachedTagKey UNPACKED_BLOCK_ITEMS_KEY       = new CachedTagKey(Reference.MOD_ID, "unpacked_block_items");

    public List<CachedTagKey> getKeys()
    {
        List<CachedTagKey> list = new ArrayList<>();

        list.add(GLASS_ITEMS_KEY);
        list.add(GLASS_PANE_ITEMS_KEY);
        list.add(CONCRETE_POWDER_ITEMS_KEY);
        list.add(CONCRETE_ITEMS_KEY);
        list.add(GLAZED_TERRACOTTA_ITEMS_KEY);
        list.add(PACKED_BLOCK_ITEMS_KEY);
        list.add(UNPACKED_BLOCK_ITEMS_KEY);

        return list;
    }

    public static void startCache()
    {
        clearCache();

		CachedItemTags.getInstance().build(GLASS_ITEMS_KEY, buildGlassItemCache());
		CachedItemTags.getInstance().build(GLASS_PANE_ITEMS_KEY, buildGlassPanesItemCache());
		CachedItemTags.getInstance().build(CONCRETE_POWDER_ITEMS_KEY, buildConcretePowderItemCache());
		CachedItemTags.getInstance().build(CONCRETE_ITEMS_KEY, buildConcreteItemCache());
		CachedItemTags.getInstance().build(GLAZED_TERRACOTTA_ITEMS_KEY, buildGlazedTerracottaItemCache());
        CachedItemTags.getInstance().build(PACKED_BLOCK_ITEMS_KEY, buildPackedBlockItemCache());
        CachedItemTags.getInstance().build(UNPACKED_BLOCK_ITEMS_KEY, buildUnpackedBlockItemCache());
    }

	private static List<String> buildGlassItemCache()
	{
		List<String> list = new ArrayList<>();

		list.add("minecraft:glass");

		for (DyeColor color : DyeColor.VALUES)
		{
			list.add("minecraft:" + color.getName() + "_stained_glass");
		}

		list.add("minecraft:tinted_glass");

		return list;
	}

	private static List<String> buildGlassPanesItemCache()
	{
		List<String> list = new ArrayList<>();

		list.add("minecraft:glass_pane");

		for (DyeColor color : DyeColor.VALUES)
		{
			list.add("minecraft:" + color.getName() + "_stained_glass_pane");
		}

		return list;
	}

	private static List<String> buildConcretePowderItemCache()
	{
		List<String> list = new ArrayList<>();
		list.add("#minecraft:concrete_powders");

		return list;
	}

	private static List<String> buildConcreteItemCache()
	{
		List<String> list = new ArrayList<>();
		list.add("#minecraft:concrete");

		return list;
	}

	private static List<String> buildGlazedTerracottaItemCache()
	{
		List<String> list = new ArrayList<>();
		list.add("#minecraft:glazed_terracotta");

		return list;
	}
    private static List<String> buildPackedBlockItemCache()
    {
        List<String> list = new ArrayList<>();

        list.add(itemId(Items.BONE_BLOCK));
        list.add(itemId(Items.CLAY));
        list.add(itemId(Items.COAL_BLOCK));
        list.add(itemId(Items.COPPER_BLOCK.weathering().unaffected()));
        list.add(itemId(Items.DIAMOND_BLOCK));
        list.add(itemId(Items.EMERALD_BLOCK));
        list.add(itemId(Items.GOLD_BLOCK));
        list.add(itemId(Items.HAY_BLOCK));
        list.add(itemId(Items.HONEY_BLOCK));
        list.add(itemId(Items.IRON_BLOCK));
        list.add(itemId(Items.LAPIS_BLOCK));
        list.add(itemId(Items.MELON));
        list.add(itemId(Items.NETHERITE_BLOCK));
        list.add(itemId(Items.RAW_COPPER_BLOCK));
        list.add(itemId(Items.RAW_GOLD_BLOCK));
        list.add(itemId(Items.RAW_IRON_BLOCK));
        list.add(itemId(Items.REDSTONE_BLOCK));
        list.add(itemId(Items.RESIN_BLOCK));
        list.add(itemId(Items.RESIN_BRICKS));
        list.add(itemId(Items.SLIME_BLOCK));

        return list;
    }

    private static List<String> buildUnpackedBlockItemCache()
    {
        List<String> list = new ArrayList<>();

        list.add(itemId(Items.BONE));
        list.add(itemId(Items.CLAY_BALL));
        list.add(itemId(Items.COAL));
        list.add(itemId(Items.COPPER_INGOT));
        list.add(itemId(Items.DIAMOND));
        list.add(itemId(Items.EMERALD));
        list.add(itemId(Items.GLOWSTONE_DUST));
        list.add(itemId(Items.GOLD_INGOT));
        list.add(itemId(Items.GOLD_NUGGET));
        list.add(itemId(Items.HONEY_BOTTLE));
        list.add(itemId(Items.ICE));
        list.add(itemId(Items.IRON_INGOT));
        list.add(itemId(Items.IRON_NUGGET));
        list.add(itemId(Items.LAPIS_LAZULI));
        list.add(itemId(Items.MELON_SLICE));
        list.add(itemId(Items.NETHERITE_INGOT));
        list.add(itemId(Items.NETHER_WART));
        list.add(itemId(Items.PACKED_ICE));
        list.add(itemId(Items.REDSTONE));
        list.add(itemId(Items.RESIN_BRICK));
        list.add(itemId(Items.RESIN_CLUMP));
        list.add(itemId(Items.SLIME_BALL));
        list.add(itemId(Items.WHEAT));

        return list;
    }

    private static void clearCache()
    {
		CachedItemTags.getInstance().clearEntry(GLASS_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(GLASS_PANE_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(CONCRETE_POWDER_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(CONCRETE_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(GLAZED_TERRACOTTA_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(PACKED_BLOCK_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(UNPACKED_BLOCK_ITEMS_KEY);
    }

	private static String itemId(Item item)
	{
		return BuiltInRegistries.ITEM.getKey(item).toString();
	}
}
