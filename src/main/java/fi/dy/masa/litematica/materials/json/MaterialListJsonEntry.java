package fi.dy.masa.litematica.materials.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.tuple.Pair;
import fi.dy.masa.malilib.mixin.recipe.IMixinIngredient;
import fi.dy.masa.malilib.util.game.RecipeBookUtils;

public class MaterialListJsonEntry
{
//    private static final AnsiLogger LOGGER = new AnsiLogger(MaterialListJsonEntry.class, true, true);
    private final List<MaterialListJsonBase> requirements;
    private final Holder<Item> inputItem;
    private final int total;
    private final Type type;
    private boolean hasOutput = false;
    private RecipeDisplayId primaryId;
    private HashMap<RecipeDisplayId, List<Ingredient>> recipeRequirements;
    private HashMap<RecipeDisplayId, RecipeBookCategory> recipeCategory;
    private HashMap<RecipeDisplayId, RecipeBookUtils.Type> recipeTypes;

    private MaterialListJsonEntry(Holder<Item> inputItem, int total, Type type)
    {
        this.requirements = new ArrayList<>();
        this.inputItem = inputItem;
        this.total = total;
        this.type = type;
    }

    public static @Nullable MaterialListJsonEntry build(Holder<Item> input, final int total, List<RecipeBookUtils.Type> types, @Nullable Holder<Item> prevItem, boolean craftingOnly)
    {
        Minecraft mc = Minecraft.getInstance();
        if (input == null || mc.level == null) return null;

        Pair<Holder<Item>, Integer> itemOverride = MaterialListJsonOverrides.INSTANCE.matchOverride(input, total);

        if (types.isEmpty())
        {
            // No types to match (Remaining logic)
            return new MaterialListJsonEntry(itemOverride.getLeft(), itemOverride.getRight(), Type.EMPTY);
        }

        ItemStack shadow = new ItemStack(itemOverride.getLeft());
        List<Pair<RecipeDisplayId, RecipeDisplayEntry>> lookup = RecipeBookUtils.getDisplayEntryFromRecipeBook(shadow, types);
        ContextMap map = RecipeBookUtils.getMap(mc);

        if (lookup.isEmpty() || MaterialListJsonOverrides.INSTANCE.shouldKeepItemOrBlock(itemOverride.getLeft()))
        {
            // No recipes found / Packed Item/Block (Such as Iron Blocks)
            return new MaterialListJsonEntry(itemOverride.getLeft(), itemOverride.getRight(), Type.LAST);
        }

        final int lookupCount = lookup.size();
        final Type outType = lookupCount > 1 ? Type.MULTI : Type.ONE;

//        LOGGER.warn("MaterialListJsonEntry#build(): Found [{}] recipe(s) for item [{}]", lookupCount, itemOverride.getLeft().getIdAsString());

        MaterialListJsonEntry result = new MaterialListJsonEntry(itemOverride.getLeft(), itemOverride.getRight(), outType);
        result.recipeRequirements = new HashMap<>();
        result.recipeCategory = new HashMap<>();
        result.recipeTypes = new HashMap<>();
        result.hasOutput = true;

        // Only report the first entry (It's just redundant work, but we flagged it as MULTI)
        Pair<RecipeDisplayId, RecipeDisplayEntry> pair = lookup.getFirst();
        RecipeDisplayId id = pair.getLeft();
        RecipeDisplayEntry entry = pair.getRight();
        List<ItemStack> resultStacks = entry.resultItems(map);
        ItemStack resultStack = resultStacks.getFirst();
        int resultCount = resultStack.getCount();
        RecipeBookCategory category = entry.category();
        RecipeBookUtils.Type type = RecipeBookUtils.Type.fromRecipeDisplay(entry.display());

        // Select Stonecutter / Crafting type based on ALT input
        if (lookup.size() > 1)
        {
            if (craftingOnly && type == RecipeBookUtils.Type.STONECUTTER)
            {
                Pair<RecipeDisplayId, RecipeDisplayEntry> altPair = lookup.get(1);
                RecipeDisplayEntry altEntry = altPair.getRight();
                RecipeBookUtils.Type altType = RecipeBookUtils.Type.fromRecipeDisplay(altEntry.display());

                if (altType == RecipeBookUtils.Type.SHAPED || altType == RecipeBookUtils.Type.SHAPELESS)
                {
//                    LOGGER.warn("MaterialListJsonEntry#build(): Found alternate Crafting type recipe(s) for item [{}] over Stonecutter type", itemOverride.getLeft().getIdAsString());
                    id = altPair.getLeft();
                    entry = altEntry;
                    resultStacks = entry.resultItems(map);
                    resultStack = resultStacks.getFirst();
                    resultCount = resultStack.getCount();
                    category = entry.category();
                    type = altType;
                }
            }
            else if (!craftingOnly &&
                    (type == RecipeBookUtils.Type.SHAPED || type == RecipeBookUtils.Type.SHAPELESS))
            {
                Pair<RecipeDisplayId, RecipeDisplayEntry> altPair = lookup.get(1);
                RecipeDisplayEntry altEntry = altPair.getRight();
                RecipeBookUtils.Type altType = RecipeBookUtils.Type.fromRecipeDisplay(altEntry.display());

                if (altType == RecipeBookUtils.Type.STONECUTTER)
                {
//                    LOGGER.warn("MaterialListJsonEntry#build(): Found alternate Stonecutter type recipe(s) for item [{}] over Crafting type", itemOverride.getLeft().getIdAsString());
                    id = altPair.getLeft();
                    entry = altEntry;
                    resultStacks = entry.resultItems(map);
                    resultStack = resultStacks.getFirst();
                    resultCount = resultStack.getCount();
                    category = entry.category();
                    type = altType;
                }
            }
        }

        // Stacks was already verified
        if (entry.craftingRequirements().isPresent())
        {
            List<Ingredient> ingredients = entry.craftingRequirements().get();

            // Override for repetitive recipe types; such as Re-Coloring of beds.
            if (lookupCount > 1 && MaterialListJsonOverrides.INSTANCE.overrideShouldSkipRecipe(itemOverride.getLeft(), ingredients))
            {
                pair = lookup.get(1);
                id = pair.getLeft();
                entry = pair.getRight();

                if (entry.craftingRequirements().isPresent())
                {
//                    Litematica.LOGGER.warn("MaterialListJsonEntry#build(): skipping recipe for [{}]", resultStack.toString());
                    resultStacks = entry.resultItems(map);
                    resultStack = resultStacks.getFirst();
                    resultCount = resultStack.getCount();
                    category = entry.category();
                    type = RecipeBookUtils.Type.fromRecipeDisplay(entry.display());
                    ingredients = entry.craftingRequirements().get();
                }
                else
                {
                    pair = lookup.getFirst();
                    id = pair.getLeft();
                    entry = pair.getRight();
                }
            }

            result.recipeRequirements.put(id, ingredients);
            result.recipeCategory.put(id, category);
            result.recipeTypes.put(id, type);
            result.primaryId = id;

            HashMap<Holder<Item>, Integer> ded = new HashMap<>();

            for (Ingredient ing : ingredients)
            {
                SlotDisplay display = ing.display();
                List<ItemStack> displayStacks = display.resolveForStacks(map);
                ItemStack displayStack = displayStacks.getFirst();
                Holder<Item> itemEntry = displayStack.typeHolder();
                HolderSet<Item> ingEntries = ((IMixinIngredient) (Object) ing).malilib_getEntries();

                if (ingEntries.size() > 1)
                {
                    itemEntry = MaterialListJsonOverrides.INSTANCE.overridePrimaryMaterial(ingEntries.get(0));
                    display = new SlotDisplay.ItemSlotDisplay(itemEntry);
                    displayStacks = display.resolveForStacks(map);
                    displayStack = displayStacks.getFirst();
//                    LOGGER.warn("build(): ingredient [{}] reduced to a single item from [{}] entries", itemEntry.getIdAsString(), ingEntries.size());
                }

                if (prevItem != null && prevItem == itemEntry)
                {
                    // Stop loops.
//                    LOGGER.warn("build(): ingredient matches previous item [{}] ... Skipping", prevItem.getIdAsString());
                    continue;
                }

//                LOGGER.warn("build(): ResultStack: [{}] // Result Count: [{}] // total: [{}]", resultStack.toString(), resultCount, itemOverride.getRight());
                int adjustedTotal = itemOverride.getRight();

                if (resultCount > 1)
                {
//                    final float adjusted = ((float) itemOverride.getRight() / resultCount);
                    final Fraction adjusted = Fraction.getFraction(itemOverride.getRight(), resultCount);
                    final int floor = Mth.floor(adjusted.floatValue());
                    final float remainderCalc = resultCount * (adjusted.floatValue() - floor);
                    final int remainderCount = Math.round(remainderCalc);
                    adjustedTotal = Math.max(floor + (remainderCount > 0 ? 1 : 0), (remainderCount > 0 ? 1 : 0));

//                    LOGGER.warn("build(): orgTotal: [{}], resultCount: [{}] --> adjusted: [{}], floor: [{}] // remainderCalc: [{}], remainderCount: [{}] // AdjustedTotal: [{}]", itemOverride.getRight(), resultCount, adjusted, floor, remainderCalc, remainderCount, adjustedTotal);
                }

                if (ded.containsKey(itemEntry))
                {
                    final int count = (ded.get(itemEntry) + adjustedTotal);
//                    LOGGER.warn("build(): ded combine entry [{}] // adjTotal: [{}] --> count [{}]", itemEntry.getIdAsString(), adjustedTotal, count);
                    ded.put(itemEntry, count);
                }
                else
                {
//                    LOGGER.warn("build(): ded single entry [{}] // adjTotal: [{}]", itemEntry.getIdAsString(), adjustedTotal);
                    ded.put(itemEntry, adjustedTotal);
                }
            }

//            LOGGER.error("MaterialListJsonEntry#build(): Found [{}] sub-materials(s) for item [{}]", ded.size(), itemOverride.getLeft().getIdAsString());

            // Ignore IDEA warnings here.
            ded.forEach(
                    (key, count) ->
                            result.requirements.add(new MaterialListJsonBase(key, count, itemOverride.getLeft(), craftingOnly))
            );
        }

        return result;
    }

//    private static void dumpDisplayStacks(List<ItemStack> list)
//    {
//        LOGGER.info("dumpDisplayStacks() size [{}]", list.size());
//
//        for (int i = 0; i < list.size(); i++)
//        {
//            ItemStack entry = list.get(i);
//            LOGGER.info("[{}] stack [{}]", i, entry.toString());
//        }
//    }

    public Type getType() { return this.type; }

    public Holder<Item> getInputItem() { return this.inputItem; }

    public List<MaterialListJsonBase> getRequirements() { return this.requirements; }

    public RecipeDisplayId getPrimaryId() { return this.primaryId; }

    public HashMap<RecipeDisplayId, List<Ingredient>> getRecipeRequirements()
    {
        return this.recipeRequirements;
    }

    public HashMap<RecipeDisplayId, RecipeBookCategory> getRecipeCategory()
    {
        return this.recipeCategory;
    }

    public HashMap<RecipeDisplayId, RecipeBookUtils.Type> getRecipeTypes()
    {
        return this.recipeTypes;
    }

    public boolean hasOutput()
    {
        return this.hasOutput;
    }

    public int getTotal()
    {
        return this.total;
    }

    public enum Type
    {
        LAST,
        EMPTY,
        ONE,
        MULTI
    }

    public JsonElement toJson(RegistryOps<?> ops)
    {
        JsonObject obj = new JsonObject();

        obj.add("Item", new JsonPrimitive(this.getInputItem().getRegisteredName()));
        obj.add("Count", new JsonPrimitive(this.getTotal()));
        obj.add("Type", new JsonPrimitive(this.type.name()));

        if (this.hasOutput())
        {
            obj.add("PrimaryId", new JsonPrimitive(this.getPrimaryId().index()));
            obj.add("Recipes", this.lookupResultsToJson(ops));
        }

        return obj;
    }

    private JsonArray lookupResultsToJson(RegistryOps<?> ops)
    {
        JsonArray arr = new JsonArray();

        for (RecipeDisplayId id : this.recipeRequirements.keySet())
        {
            List<Ingredient> requires = this.recipeRequirements.get(id);
            RecipeBookCategory category = this.recipeCategory.get(id);
            RecipeBookUtils.Type type = this.recipeTypes.get(id);
            JsonObject obj = new JsonObject();

            obj.add("NetworkId", new JsonPrimitive(id.index()));
            obj.add("Category", new JsonPrimitive(RecipeBookUtils.getRecipeCategoryId(category)));
            obj.add("Type", new JsonPrimitive(type.name()));

            JsonArray itemArr = new JsonArray();
            for (Ingredient ing : requires)
            {
                itemArr.add((JsonElement) Ingredient.CODEC.encodeStart(ops, ing).getPartialOrThrow());
            }
            obj.add("Ingredients", itemArr);

            JsonArray outputArr = new JsonArray();
            for (MaterialListJsonBase jsonEntry : this.requirements)
            {
                outputArr.add(jsonEntry.toJson(ops));
            }
            obj.add("Requirements", outputArr);

            arr.add(obj);
        }

        return arr;
    }
}
