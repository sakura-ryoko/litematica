package fi.dy.masa.litematica.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

import fi.dy.masa.malilib.data.CachedItemTags;
import fi.dy.masa.malilib.data.CachedTagKey;
import fi.dy.masa.litematica.Reference;

/**
 * Caches Block/Item Tags as if they are real Vanilla Block/Item tags.
 */
public class CachedTagManager
{
	public static final CachedTagKey GLASS_ITEMS_KEY                = new CachedTagKey(Reference.MOD_ID, "glass_items");
	public static final CachedTagKey GLASS_PANE_ITEMS_KEY           = new CachedTagKey(Reference.MOD_ID, "glass_pane_items");
	public static final CachedTagKey PACKED_BLOCK_ITEMS_KEY         = new CachedTagKey(Reference.MOD_ID, "packed_block_items");
    public static final CachedTagKey UNPACKED_BLOCK_ITEMS_KEY       = new CachedTagKey(Reference.MOD_ID, "unpacked_block_items");

    public List<CachedTagKey> getKeys()
    {
        List<CachedTagKey> list = new ArrayList<>();

        list.add(GLASS_ITEMS_KEY);
        list.add(GLASS_PANE_ITEMS_KEY);
        list.add(PACKED_BLOCK_ITEMS_KEY);
        list.add(UNPACKED_BLOCK_ITEMS_KEY);

        return list;
    }

    public static void startCache()
    {
        clearCache();

		CachedItemTags.getInstance().build(GLASS_ITEMS_KEY, buildGlassItemCache());
		CachedItemTags.getInstance().build(GLASS_PANE_ITEMS_KEY, buildGlassPanesItemCache());
        CachedItemTags.getInstance().build(PACKED_BLOCK_ITEMS_KEY, buildPackedBlockItemCache());
        CachedItemTags.getInstance().build(UNPACKED_BLOCK_ITEMS_KEY, buildUnpackedBlockItemCache());
    }

	private static List<String> buildGlassItemCache()
	{
		List<String> list = new ArrayList<>();

		list.add(BuiltInRegistries.ITEM.getKey(Items.GLASS).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.black()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.blue()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.brown()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.cyan()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.gray()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.green()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.lightBlue()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.lightGray()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.lime()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.magenta()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.orange()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.pink()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.purple()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.red()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.yellow()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS.white()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.TINTED_GLASS).toString());

		return list;
	}

	private static List<String> buildGlassPanesItemCache()
	{
		List<String> list = new ArrayList<>();

		list.add(BuiltInRegistries.ITEM.getKey(Items.GLASS_PANE).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.black()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.blue()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.brown()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.cyan()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.gray()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.green()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.lightBlue()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.lightGray()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.lime()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.magenta()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.orange()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.pink()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.purple()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.red()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.yellow()).toString());
		list.add(BuiltInRegistries.ITEM.getKey(Items.STAINED_GLASS_PANE.white()).toString());

		return list;
	}

    private static List<String> buildPackedBlockItemCache()
    {
        List<String> list = new ArrayList<>();

        list.add(BuiltInRegistries.ITEM.getKey(Items.BONE_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.CLAY).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.COAL_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.COPPER_BLOCK.weathering().unaffected()).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.EMERALD_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.GOLD_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.HAY_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.HONEY_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.IRON_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.LAPIS_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.MELON).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RAW_COPPER_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RAW_GOLD_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RAW_IRON_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.REDSTONE_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RESIN_BLOCK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RESIN_BRICKS).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.SLIME_BLOCK).toString());

        return list;
    }

    private static List<String> buildUnpackedBlockItemCache()
    {
        List<String> list = new ArrayList<>();

        list.add(BuiltInRegistries.ITEM.getKey(Items.BONE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.CLAY_BALL).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.COAL).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.COPPER_INGOT).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.DIAMOND).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.EMERALD).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.GLOWSTONE_DUST).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.GOLD_INGOT).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.GOLD_NUGGET).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.HONEY_BOTTLE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.ICE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.IRON_NUGGET).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.LAPIS_LAZULI).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.MELON_SLICE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.NETHERITE_INGOT).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.NETHER_WART).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.PACKED_ICE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.REDSTONE).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RESIN_BRICK).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.RESIN_CLUMP).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.SLIME_BALL).toString());
        list.add(BuiltInRegistries.ITEM.getKey(Items.WHEAT).toString());

        return list;
    }

    private static void clearCache()
    {
		CachedItemTags.getInstance().clearEntry(GLASS_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(GLASS_PANE_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(PACKED_BLOCK_ITEMS_KEY);
		CachedItemTags.getInstance().clearEntry(UNPACKED_BLOCK_ITEMS_KEY);
    }
}
