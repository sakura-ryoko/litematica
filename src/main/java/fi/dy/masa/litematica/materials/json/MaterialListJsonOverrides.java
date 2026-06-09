package fi.dy.masa.litematica.materials.json;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.WeatheringCopperCollection;

import fi.dy.masa.malilib.data.CachedTagUtils;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
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

    private Holder<Item> add(Item item)
    {
        return BuiltInRegistries.ITEM.wrapAsHolder(item);
    }

    private void initOverrides()
    {
        // Copper Block
        this.addCopperOverrides(Items.COPPER_BLOCK);
        // Copper Grate
        this.addCopperOverrides(Items.COPPER_GRATE);
        // Cut Copper
        this.addCopperOverrides(Items.CUT_COPPER);
        // Chiseled Copper
        this.addCopperOverrides(Items.CHISELED_COPPER);
        // Copper Bulb
        this.addCopperOverrides(Items.COPPER_BULB);
        // Copper Slab
        this.addCopperOverrides(Items.CUT_COPPER_SLAB);
        // Copper Stairs
        this.addCopperOverrides(Items.CUT_COPPER_STAIRS);
        // Copper Door
        this.addCopperOverrides(Items.COPPER_DOOR);
        // Copper Trap Door
        this.addCopperOverrides(Items.COPPER_TRAPDOOR);
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
        for (DyeColor color : DyeColor.VALUES)
        {
            this.overrides.add(new ResultOverride(this.add(Items.CONCRETE.pick(color)), this.add(Items.CONCRETE_POWDER.pick(color)), Fraction.ONE));
        }
        // Anvils
        this.overrides.add(new ResultOverride(this.add(Items.CHIPPED_ANVIL), this.add(Items.ANVIL), Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(Items.DAMAGED_ANVIL), this.add(Items.ANVIL), Fraction.ONE));
    }

    private void addCopperOverrides(WeatheringCopperCollection<Item> collection)
    {
        Holder<Item> unwaxedBase = this.add(collection.weathering().unaffected());
        this.overrides.add(new ResultOverride(this.add(collection.weathering().exposed()), unwaxedBase, Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(collection.weathering().weathered()), unwaxedBase, Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(collection.weathering().oxidized()), unwaxedBase, Fraction.ONE));

        Holder<Item> waxedBase = this.add(collection.waxed().unaffected());
        this.overrides.add(new ResultOverride(this.add(collection.waxed().exposed()), waxedBase, Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(collection.waxed().weathered()), waxedBase, Fraction.ONE));
        this.overrides.add(new ResultOverride(this.add(collection.waxed().oxidized()), waxedBase, Fraction.ONE));
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
        this.packingOverrides.add(new ResultOverride(this.add(Items.COPPER_INGOT), this.add(Items.COPPER_BLOCK.weathering().unaffected()), by9));
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

    protected Pair<Holder<Item>, Integer> matchOverride(Holder<Item> result, Integer total)
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

    protected Triple<Holder<Item>, Float, Integer> matchPackingOverride(Holder<Item> result, Integer total)
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
    protected Holder<Item> overridePrimaryMaterial(Holder<Item> firstItem)
    {
        if (firstItem.is(ItemTags.WOOL))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.WOOL.white());
        }
        else if (firstItem.is(ItemTags.WOOL_CARPETS))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.CARPET.white());
        }
        else if (firstItem.is(ItemTags.BEDS))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.BED.white());
        }
        else if (firstItem.is(ItemTags.CANDLES))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.CANDLE);
        }
        else if (firstItem.is(ItemTags.SHULKER_BOXES))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.SHULKER_BOX);
        }
        else if (firstItem.is(ItemTags.BANNERS))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.BANNER.white());
        }
        else if (firstItem.is(ItemTags.TERRACOTTA))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.TERRACOTTA);
        }
        else if (firstItem.is(ItemTags.BUNDLES))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.BUNDLE);
        }
        else if (firstItem.is(ItemTags.HARNESSES))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.HARNESS.white());
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_ITEMS_KEY, firstItem))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.GLASS);
        }
        else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_PANE_ITEMS_KEY, firstItem))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.GLASS_PANE);
        }
        else if (firstItem.is(ItemTags.CONCRETE_POWDERS))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.CONCRETE_POWDER.white());
        }
        else if (firstItem.is(ItemTags.CONCRETE))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.CONCRETE.white());
        }
        else if (firstItem.is(ItemTags.GLAZED_TERRACOTTA))
        {
            return BuiltInRegistries.ITEM.wrapAsHolder(Items.GLAZED_TERRACOTTA.white());
        }

        return this.matchOverride(firstItem, 1).getLeft();
    }

    // Overrides for particular cases, such as redying of beds instead of choosing the Wool recipe.
    protected boolean overrideShouldSkipRecipe(Holder<Item> input, List<Ingredient> ingredients)
    {
        for (Ingredient ing : ingredients)
        {
            if (input.is(ItemTags.BEDS))
            {
                if (ing.test(Items.BED.white().getDefaultInstance()) ||
                    ing.test(Items.BED.black().getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.WOOL))
            {
                if (ing.test(Items.WOOL.white().getDefaultInstance()) ||
                    ing.test(Items.WOOL.black().getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.WOOL_CARPETS))
            {
                if (ing.test(Items.CARPET.white().getDefaultInstance()) ||
                    ing.test(Items.CARPET.black().getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.CANDLES))
            {
                if (ing.test(Items.DYED_CANDLE.white().getDefaultInstance()) ||
                    ing.test(Items.DYED_CANDLE.black().getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.SHULKER_BOXES))
            {
                if (ing.test(Items.DYED_SHULKER_BOX.white().getDefaultInstance()) ||
                    ing.test(Items.DYED_SHULKER_BOX.black().getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.BANNERS))
            {
                if (ing.test(Items.BANNER.white().getDefaultInstance()) ||
                    ing.test(Items.BANNER.black().getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.TERRACOTTA))
            {
                if (ing.test(Items.DYED_TERRACOTTA.white().getDefaultInstance()) ||
                    ing.test(Items.DYED_TERRACOTTA.black().getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_ITEMS_KEY, input))
            {
                if (ing.test(Items.STAINED_GLASS.white().getDefaultInstance()) ||
                    ing.test(Items.STAINED_GLASS.black().getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (CachedTagUtils.matchItemTag(CachedTagManager.GLASS_PANE_ITEMS_KEY, input))
            {
                if (ing.test(Items.STAINED_GLASS_PANE.white().getDefaultInstance()) ||
                    ing.test(Items.STAINED_GLASS_PANE.black().getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.CONCRETE))
            {
                if (ing.test(Items.CONCRETE.white().getDefaultInstance()) ||
                    ing.test(Items.CONCRETE.black().getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.CONCRETE_POWDERS))
            {
                if (ing.test(Items.CONCRETE_POWDER.white().getDefaultInstance()) ||
                    ing.test(Items.CONCRETE_POWDER.black().getDefaultInstance()))
                {
                    return true;
                }
            }
            else if (input.is(ItemTags.GLAZED_TERRACOTTA))
            {
                if (ing.test(Items.GLAZED_TERRACOTTA.white().getDefaultInstance()) ||
                    ing.test(Items.GLAZED_TERRACOTTA.black().getDefaultInstance()))
                {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean shouldKeepItemOrBlock(Holder<Item> input)
    {
//        if (CachedTagManager.matchItemTag(CachedTagManager.PACKED_BLOCK_ITEMS_KEY, input))
//        {
//            return true;
//        }
//        else
        // Only count Ingots / Nuggets as base material
        return CachedTagUtils.matchItemTag(CachedTagManager.UNPACKED_BLOCK_ITEMS_KEY, input);
    }

    public record ResultOverride(Holder<Item> input, Holder<Item> result, Fraction multiplier)
    {
        public boolean match(Holder<Item> otherItem) { return this.input().is(otherItem.unwrapKey().orElseThrow()); }

        public Integer mulInt(Integer totalIn) { return totalIn * this.multiplier().intValue(); }

        public Float mulFloat(Integer totalIn) { return totalIn * this.multiplier().floatValue(); }

        public Integer divisor() { return this.multiplier().getDenominator(); }
    }
}
