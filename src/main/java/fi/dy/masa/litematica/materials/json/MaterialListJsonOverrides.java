package fi.dy.masa.litematica.materials.json;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;

import fi.dy.masa.malilib.data.CachedTagUtils;
import fi.dy.masa.litematica.data.CachedTagManager;

public class MaterialListJsonOverrides
{
    public static final MaterialListJsonOverrides INSTANCE = new MaterialListJsonOverrides();
    private final Set<ResultOverride> overrides = new HashSet<>();
    private final Set<ResultOverride> packingOverrides = new HashSet<>();

    protected MaterialListJsonOverrides()
    {
        this.initOverrides();
        this.initPackingOverrides();
    }

    private RegistryEntry<Item> add(Item item)
    {
        return Registries.ITEM.getEntry(item);
    }

    private void initOverrides()
    {
        // Copper Block
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_COPPER),         this.add(Items.COPPER_BLOCK), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_COPPER),       this.add(Items.COPPER_BLOCK), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_COPPER),        this.add(Items.COPPER_BLOCK), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_COPPER),   this.add(Items.WAXED_COPPER_BLOCK), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_COPPER), this.add(Items.WAXED_COPPER_BLOCK), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_COPPER),  this.add(Items.WAXED_COPPER_BLOCK), Fraction.ONE));
        // Copper Grate
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_COPPER_GRATE),         this.add(Items.COPPER_GRATE), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_COPPER_GRATE),       this.add(Items.COPPER_GRATE), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_COPPER_GRATE),        this.add(Items.COPPER_GRATE), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_COPPER_GRATE),   this.add(Items.WAXED_COPPER_GRATE), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_COPPER_GRATE), this.add(Items.WAXED_COPPER_GRATE), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_COPPER_GRATE),  this.add(Items.WAXED_COPPER_GRATE), Fraction.ONE));
        // Cut Copper
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_CUT_COPPER),         this.add(Items.CUT_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_CUT_COPPER),       this.add(Items.CUT_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_CUT_COPPER),        this.add(Items.CUT_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_CUT_COPPER),   this.add(Items.WAXED_CUT_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_CUT_COPPER), this.add(Items.WAXED_CUT_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_CUT_COPPER),  this.add(Items.WAXED_CUT_COPPER), Fraction.ONE));
        // Chiseled Copper
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_CHISELED_COPPER),         this.add(Items.CHISELED_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_CHISELED_COPPER),       this.add(Items.CHISELED_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_CHISELED_COPPER),        this.add(Items.CHISELED_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_CHISELED_COPPER),   this.add(Items.WAXED_CHISELED_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_CHISELED_COPPER), this.add(Items.WAXED_CHISELED_COPPER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_CHISELED_COPPER),  this.add(Items.WAXED_CHISELED_COPPER), Fraction.ONE));
        // Copper Bulb
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_COPPER_BULB),         this.add(Items.COPPER_BULB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_COPPER_BULB),       this.add(Items.COPPER_BULB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_COPPER_BULB),        this.add(Items.COPPER_BULB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_COPPER_BULB),   this.add(Items.WAXED_COPPER_BULB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_COPPER_BULB), this.add(Items.WAXED_COPPER_BULB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_COPPER_BULB),  this.add(Items.WAXED_COPPER_BULB), Fraction.ONE));
        // Copper Slab
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_CUT_COPPER_SLAB),         this.add(Items.CUT_COPPER_SLAB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_CUT_COPPER_SLAB),       this.add(Items.CUT_COPPER_SLAB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_CUT_COPPER_SLAB),        this.add(Items.CUT_COPPER_SLAB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_CUT_COPPER_SLAB),   this.add(Items.WAXED_CUT_COPPER_SLAB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_CUT_COPPER_SLAB), this.add(Items.WAXED_CUT_COPPER_SLAB), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_CUT_COPPER_SLAB),  this.add(Items.WAXED_CUT_COPPER_SLAB), Fraction.ONE));
        // Copper Stairs
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_CUT_COPPER_STAIRS),         this.add(Items.CUT_COPPER_STAIRS), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_CUT_COPPER_STAIRS),       this.add(Items.CUT_COPPER_STAIRS), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_CUT_COPPER_STAIRS),        this.add(Items.CUT_COPPER_STAIRS), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_CUT_COPPER_STAIRS),   this.add(Items.WAXED_CUT_COPPER_STAIRS), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_CUT_COPPER_STAIRS), this.add(Items.WAXED_CUT_COPPER_STAIRS), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS),  this.add(Items.WAXED_CUT_COPPER_STAIRS), Fraction.ONE));
        // Copper Door
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_COPPER_DOOR),         this.add(Items.COPPER_DOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_COPPER_DOOR),       this.add(Items.COPPER_DOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_COPPER_DOOR),        this.add(Items.COPPER_DOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_COPPER_DOOR),   this.add(Items.WAXED_COPPER_DOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_COPPER_DOOR), this.add(Items.WAXED_COPPER_DOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_COPPER_DOOR),  this.add(Items.WAXED_COPPER_DOOR), Fraction.ONE));
        // Copper Trap Door
        this.overrides.add(new ResultOverride(this.add(Items.EXPOSED_COPPER_TRAPDOOR),         this.add(Items.COPPER_TRAPDOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WEATHERED_COPPER_TRAPDOOR),       this.add(Items.COPPER_TRAPDOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.OXIDIZED_COPPER_TRAPDOOR),        this.add(Items.COPPER_TRAPDOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_EXPOSED_COPPER_TRAPDOOR),   this.add(Items.WAXED_COPPER_TRAPDOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_WEATHERED_COPPER_TRAPDOOR), this.add(Items.WAXED_COPPER_TRAPDOOR), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WAXED_OXIDIZED_COPPER_TRAPDOOR),  this.add(Items.WAXED_COPPER_TRAPDOOR), Fraction.ONE));
        // Stripped Woods
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_ACACIA_LOG),   this.add(Items.ACACIA_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_BAMBOO_BLOCK), this.add(Items.BAMBOO), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_BIRCH_LOG),    this.add(Items.BIRCH_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_CHERRY_LOG),   this.add(Items.CHERRY_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_CRIMSON_STEM), this.add(Items.CRIMSON_STEM), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_DARK_OAK_LOG), this.add(Items.DARK_OAK_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_JUNGLE_LOG),   this.add(Items.JUNGLE_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_MANGROVE_LOG), this.add(Items.MANGROVE_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_OAK_LOG),      this.add(Items.OAK_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_PALE_OAK_LOG), this.add(Items.PALE_OAK_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_SPRUCE_LOG),   this.add(Items.SPRUCE_LOG), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.STRIPPED_WARPED_STEM),  this.add(Items.WARPED_STEM), Fraction.ONE));
        // Concrete
        this.overrides.add(new ResultOverride(this.add(Items.BLACK_CONCRETE),       this.add(Items.BLACK_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.BLUE_CONCRETE),        this.add(Items.BLUE_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.BROWN_CONCRETE),       this.add(Items.BROWN_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.CYAN_CONCRETE),        this.add(Items.CYAN_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.GRAY_CONCRETE),        this.add(Items.GRAY_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.GREEN_CONCRETE),       this.add(Items.GREEN_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.LIGHT_BLUE_CONCRETE),  this.add(Items.LIGHT_BLUE_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.LIGHT_GRAY_CONCRETE),  this.add(Items.LIGHT_GRAY_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.LIME_CONCRETE),        this.add(Items.LIME_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.MAGENTA_CONCRETE),     this.add(Items.MAGENTA_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.ORANGE_CONCRETE),      this.add(Items.ORANGE_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.PINK_CONCRETE),        this.add(Items.PINK_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.PURPLE_CONCRETE),      this.add(Items.PURPLE_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.RED_CONCRETE),         this.add(Items.RED_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.YELLOW_CONCRETE),      this.add(Items.YELLOW_CONCRETE_POWDER), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.WHITE_CONCRETE),       this.add(Items.WHITE_CONCRETE_POWDER), Fraction.ONE));
        // Anvils
        this.overrides.add(new ResultOverride(this.add(Items.CHIPPED_ANVIL), this.add(Items.ANVIL), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.DAMAGED_ANVIL), this.add(Items.ANVIL), Fraction.ONE));
    }

    private void initPackingOverrides()
    {
        Fraction by9 = Fraction.getFraction(1, 9);
        // x4 = x1
        this.packingOverrides.add(new ResultOverride(this.add(Items.CLAY_BALL), this.add(Items.CLAY), Fraction.ONE_QUARTER));
        this.packingOverrides.add(new ResultOverride(this.add(Items.HONEY_BOTTLE), this.add(Items.HONEY_BLOCK), Fraction.ONE_QUARTER));

        // x9 = x1
        this.packingOverrides.add(new ResultOverride(this.add(Items.BONE_MEAL), this.add(Items.BONE_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.COAL), this.add(Items.COAL_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.COPPER_INGOT), this.add(Items.COPPER_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.DIAMOND), this.add(Items.DIAMOND_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.EMERALD), this.add(Items.EMERALD_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.GOLD_INGOT), this.add(Items.GOLD_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.GOLD_NUGGET), this.add(Items.GOLD_INGOT), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.IRON_INGOT), this.add(Items.IRON_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.IRON_NUGGET), this.add(Items.IRON_INGOT), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.LAPIS_LAZULI), this.add(Items.LAPIS_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.MELON_SLICE), this.add(Items.MELON), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.NETHERITE_INGOT), this.add(Items.NETHERITE_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.REDSTONE), this.add(Items.REDSTONE_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.RESIN_BRICK), this.add(Items.RESIN_BRICKS), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.RESIN_CLUMP), this.add(Items.RESIN_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.SLIME_BALL), this.add(Items.SLIME_BLOCK), by9));
        this.packingOverrides.add(new ResultOverride(this.add(Items.WHEAT), this.add(Items.HAY_BLOCK), by9));
    }

    protected Pair<RegistryEntry<Item>, Integer> matchOverride(RegistryEntry<Item> result, Integer total)
    {
        for (ResultOverride map : this.overrides)
        {
            if (map.match(result))
            {
                return Pair.of(map.result(), map.mulInt(total));
            }
        }

        return Pair.of(result, total);
    }

    protected Triple<RegistryEntry<Item>, Float, Integer> matchPackingOverride(RegistryEntry<Item> result, Integer total)
    {
        for (ResultOverride map : this.packingOverrides)
        {
            if (map.match(result))
            {
                return Triple.of(map.result(), map.mulFloat(total), map.divisor());
            }
        }

        return Triple.of(result, total.floatValue(), 1);
    }

    // Overrides for re-dying recipe's
    protected RegistryEntry<Item> overridePrimaryMaterial(RegistryEntry<Item> firstItem)
    {
        if (firstItem.isIn(ItemTags.WOOL))
        {
            return Registries.ITEM.getEntry(Items.WHITE_WOOL);
        }
        else if (firstItem.isIn(ItemTags.WOOL_CARPETS))
        {
            return Registries.ITEM.getEntry(Items.WHITE_CARPET);
        }
        else if (firstItem.isIn(ItemTags.BEDS))
        {
            return Registries.ITEM.getEntry(Items.WHITE_BED);
        }
        else if (firstItem.isIn(ItemTags.CANDLES))
        {
            return Registries.ITEM.getEntry(Items.CANDLE);
        }
        else if (firstItem.isIn(ItemTags.SHULKER_BOXES))
        {
            return Registries.ITEM.getEntry(Items.SHULKER_BOX);
        }
        else if (firstItem.isIn(ItemTags.BANNERS))
        {
            return Registries.ITEM.getEntry(Items.WHITE_BANNER);
        }
        else if (firstItem.isIn(ItemTags.TERRACOTTA))
        {
            return Registries.ITEM.getEntry(Items.TERRACOTTA);
        }
        else if (firstItem.isIn(ItemTags.BUNDLES))
        {
            return Registries.ITEM.getEntry(Items.BUNDLE);
        }
        else if (firstItem.isIn(ItemTags.HARNESSES))
        {
            return Registries.ITEM.getEntry(Items.WHITE_HARNESS);
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_ITEMS_KEY, firstItem))
        {
            return Registries.ITEM.getEntry(Items.GLASS);
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_PANE_ITEMS_KEY, firstItem))
        {
            return Registries.ITEM.getEntry(Items.GLASS_PANE);
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.CONCRETE_POWDER_ITEMS_KEY, firstItem))
        {
            return Registries.ITEM.getEntry(Items.WHITE_CONCRETE_POWDER);
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.CONCRETE_ITEMS_KEY, firstItem))
        {
            return Registries.ITEM.getEntry(Items.WHITE_CONCRETE);
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.GLAZED_TERRACOTTA_ITEMS_KEY, firstItem))
        {
            return Registries.ITEM.getEntry(Items.WHITE_GLAZED_TERRACOTTA);
        }

        return this.matchOverride(firstItem, 1).getLeft();
    }

    // Overrides for particular cases, such as redying of beds instead of choosing the Wool recipe.
    protected boolean overrideShouldSkipRecipe(RegistryEntry<Item> input, List<Ingredient> ingredients)
    {
        for (Ingredient ing : ingredients)
        {
            if (input.isIn(ItemTags.BEDS))
            {
                if (ing.test(Items.WHITE_BED.getDefaultStack()) ||
                    ing.test(Items.BLACK_BED.getDefaultStack()))
                {
                    return true;
                }
            }
            else if (input.isIn(ItemTags.WOOL))
            {
                if (ing.test(Items.WHITE_WOOL.getDefaultStack()) ||
                    ing.test(Items.BLACK_WOOL.getDefaultStack()))
                {
                    return true;
                }
            }
            else if (input.isIn(ItemTags.WOOL_CARPETS))
            {
                if (ing.test(Items.WHITE_CARPET.getDefaultStack()) ||
                    ing.test(Items.BLACK_CARPET.getDefaultStack()))
                {
                    return true;
                }
            }
            else if (input.isIn(ItemTags.CANDLES))
            {
                if (ing.test(Items.WHITE_CANDLE.getDefaultStack()) ||
                    ing.test(Items.BLACK_CANDLE.getDefaultStack()))
                {
                    return true;
                }
            }
            else if (input.isIn(ItemTags.SHULKER_BOXES))
            {
                if (ing.test(Items.WHITE_SHULKER_BOX.getDefaultStack()) ||
                    ing.test(Items.BLACK_SHULKER_BOX.getDefaultStack()))
                {
                    return true;
                }
            }
            else if (input.isIn(ItemTags.BANNERS))
            {
                if (ing.test(Items.WHITE_BANNER.getDefaultStack()) ||
                    ing.test(Items.BLACK_BANNER.getDefaultStack()))
                {
                    return true;
                }
            }
            else if (input.isIn(ItemTags.TERRACOTTA))
            {
                if (ing.test(Items.WHITE_TERRACOTTA.getDefaultStack()) ||
                    ing.test(Items.BLACK_TERRACOTTA.getDefaultStack()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_ITEMS_KEY, input))
            {
                if (ing.test(Items.WHITE_STAINED_GLASS.getDefaultStack()) ||
                    ing.test(Items.BLACK_STAINED_GLASS.getDefaultStack()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_PANE_ITEMS_KEY, input))
            {
                if (ing.test(Items.WHITE_STAINED_GLASS_PANE.getDefaultStack()) ||
                    ing.test(Items.BLACK_STAINED_GLASS_PANE.getDefaultStack()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.CONCRETE_ITEMS_KEY, input))
            {
                if (ing.test(Items.WHITE_CONCRETE.getDefaultStack()) ||
                    ing.test(Items.BLACK_CONCRETE.getDefaultStack()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.CONCRETE_POWDER_ITEMS_KEY, input))
            {
                if (ing.test(Items.WHITE_CONCRETE_POWDER.getDefaultStack()) ||
                    ing.test(Items.BLACK_CONCRETE_POWDER.getDefaultStack()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.GLAZED_TERRACOTTA_ITEMS_KEY, input))
            {
                if (ing.test(Items.WHITE_GLAZED_TERRACOTTA.getDefaultStack()) ||
                    ing.test(Items.BLACK_GLAZED_TERRACOTTA.getDefaultStack()))
                {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean shouldKeepItemOrBlock(RegistryEntry<Item> input)
    {
//        if (CachedTagUtils.matchItemTag(CachedTagManager.PACKED_BLOCK_ITEMS_KEY, input))
//        {
//            return true;
//        }
//        else
        // Only count Ingots / Nuggets as base material
        return CachedTagUtils.matchItemTag(CachedTagManager.UNPACKED_BLOCK_ITEMS_KEY, input);
    }

    public record ResultOverride(RegistryEntry<Item> input, RegistryEntry<Item> result, Fraction multiplier)
    {
        public boolean match(RegistryEntry<Item> otherItem) { return this.input().matchesKey(otherItem.getKey().orElseThrow()); }

        public Integer mulInt(Integer totalIn) { return totalIn * this.multiplier().intValue(); }

        public Float mulFloat(Integer totalIn) { return totalIn * this.multiplier().floatValue(); }

        public Integer divisor() { return this.multiplier().getDenominator(); }
    }
}
