package fi.dy.masa.litematica.materials.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;

import fi.dy.masa.malilib.data.CachedTagUtils;
import fi.dy.masa.malilib.util.game.RecipeBookUtils;
import fi.dy.masa.litematica.data.CachedTagManager;

/**
 * The idea with this cache is to flatten the JSON structure into a more human-digestible format,
 * and deduplicate multiple Crafting steps into one for each type of item; and then total them up.
 */
public class MaterialListJsonCache
{
//    private static final AnsiLogger LOGGER = new AnsiLogger(MaterialListJsonCache.class, true, true);
    private final List<Entry> entriesFlat;
    private final List<Entry> entriesCombined;
    private final String GATHER_KEY = "GATHER";

    public MaterialListJsonCache()
    {
        this.entriesFlat = new ArrayList<>();
        this.entriesCombined = new ArrayList<>();
    }

    /**
     * Adds a new entry, Flat without combining.
     * Preserve all data.
     * @param input ()
     */
    public void putFlatEntry(Entry input)
    {
        List<Result> results = input.results();
        List<Step> steps = input.steps();

        if (!this.entriesFlat.isEmpty() && !results.isEmpty())
        {
            // Only compare the first Result.  Should only be one under the Flat list anyway.
            Result result = results.getFirst();

            for (int i = 0; i < this.entriesFlat.size(); i++)
            {
                Entry entry = this.entriesFlat.get(i);
                List<Result> entryResults = entry.results();

                if (!entryResults.isEmpty())
                {
                    Result resultEntry = entryResults.getFirst();
                    List<Step> entrySteps = entry.steps();

                    if (resultEntry.equals(result) &&
                        this.compareSteps(steps, entrySteps))
                    {
                        // Don't duplicate steps if they already exist.
                        this.entriesFlat.add(new Entry(input.rawItem(), input.total(), List.of(), results));
                        return;
                    }
                }
            }
        }

        // Simply add it otherwise.
        this.entriesFlat.add(input);
    }

    /**
     * Adds a new entry, but combine them if the Items match, and add to the total required.
     * Remove the Steps data from this output.
     * @param input ()
     */
    public void putCombinedEntry(Entry input, boolean addResultTotals)
    {
        if (!this.entriesCombined.isEmpty())
        {
            RegistryEntry<Item> item = input.rawItem();
            final int total = input.total();
            List<Result> results = input.results();

            // Check for existing item matches
            for (int i = 0; i < this.entriesCombined.size(); i++)
            {
                Entry entry = this.entriesCombined.get(i);

                if (entry.rawItem().equals(item))
                {
                    // Combine steps
                    this.entriesCombined.set(i, new Entry(item, (entry.total() + total), List.of(), this.combineResults(results, entry.results(), addResultTotals)));
                    return;
                }
            }
        }

        // Remove the Steps Display
        this.entriesCombined.add(new Entry(input.rawItem(), input.total(), List.of(), input.results()));
    }

    /**
     * Repack Base Material stacks into their Block + Remainder Entry output;
     * Such as Ingots -> Blocks + Ingot Remainder.
     * @param currentItem ()
     * @return ()
     */
    public List<Entry> combineUnpackedItems(Entry currentItem)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        RegistryEntry<Item> baseItem = currentItem.rawItem();
        if (baseItem == null || mc.world == null) return List.of(currentItem);

        if (CachedTagUtils.matchItemTag(CachedTagManager.UNPACKED_BLOCK_ITEMS_KEY, baseItem))
        {
            final int total = currentItem.total();
            Triple<RegistryEntry<Item>, Float, Integer> pair = MaterialListJsonOverrides.INSTANCE.matchPackingOverride(baseItem, total);
            RegistryKey<Item> ironNugget = Registries.ITEM.getEntry(Items.IRON_NUGGET).getKey().orElseThrow();
            RegistryKey<Item> goldNugget = Registries.ITEM.getEntry(Items.GOLD_NUGGET).getKey().orElseThrow();
            List<Entry> list = new ArrayList<>();

            // Repack Nuggets into Ingots first
            if (baseItem.matchesId(ironNugget.getValue()) || baseItem.matchesId(goldNugget.getValue()))
            {
                final int floor = MathHelper.floor(pair.getMiddle());
                final int multiplier = pair.getRight();
                final float remainCalc = multiplier * (pair.getMiddle() - floor);
                final int remainder = Math.round(remainCalc);

//                LOGGER.error("combineUnpackedItems(): (Nuggets/{}) total: [{}] // pair [{}] --> floor [{}], mul [{}], remainCalc [{}], remainder [{}]", baseItem.getIdAsString(), total, pair.getLeft().getIdAsString(), floor, multiplier, remainCalc, remainder);

                // Add remainder
                if (remainder > 0)
                {
                    list.addLast(new Entry(baseItem, remainder, currentItem.steps(), currentItem.results()));
                }

                // Advance to next
                baseItem = pair.getLeft();
                pair = MaterialListJsonOverrides.INSTANCE.matchPackingOverride(baseItem, (floor));
            }

            if (!pair.getLeft().matchesKey(baseItem.getKey().orElseThrow()) && pair.getMiddle() > 0.0F)
            {
                // Check if it's div by 9, then check if div by 4; or else mul by 1
                final int floor = MathHelper.floor(pair.getMiddle());
                final int multiplier = pair.getRight();
                final float remainCalc = multiplier * (pair.getMiddle() - floor);
                final int remainder = Math.round(remainCalc);

//                LOGGER.error("combineUnpackedItems(): (Other Packed/{}) total: [{}] // pair [{}] --> floor [{}], mul [{}], remainCalc [{}], remainder [{}]", baseItem.getIdAsString(), total, pair.getLeft().getIdAsString(), floor, multiplier, remainCalc, remainder);

                // Add remainder
                if (remainder > 0)
                {
                    list.addFirst(new Entry(baseItem, remainder, list.isEmpty() ? currentItem.steps() : List.of(), currentItem.results()));
                }

                // Add floored count
                if (floor > 0)
                {
                    list.addFirst(new Entry(pair.getLeft(), floor, list.isEmpty() ? currentItem.steps() : List.of(), currentItem.results()));
                }
            }

            if (list.isEmpty())
            {
                return List.of(currentItem);
            }

            return list;
        }

        return List.of(currentItem);
    }

    /**
     * Combines two-Step lists when the Item matches; while deduplicating by simply adding the total required.
     * @param left ()
     * @param right ()
     * @return ()
     */
    public List<Step> combineSteps(List<Step> left, List<Step> right)
    {
        List<Step> list = new ArrayList<>();
        List<Integer> ignores = new ArrayList<>();

//        LOGGER.info("combineSteps: left [{}], right [{}]", left.size(), right.size());

        if (left.isEmpty() && !right.isEmpty())
        {
            return right;
        }

        for (Step entry : left)
        {
            RegistryEntry<Item> item = entry.stepItem();
            final int count = entry.count();
            boolean matched = false;

//            LOGGER.info("combineSteps: left[{}]: [{}]", i, item.getIdAsString());
            for (int j = 0; j < right.size(); j++)
            {
                Step otherEntry = right.get(j);

//                LOGGER.info("left[{}]: [{}] // right[{}]: [{}]", i, item.getIdAsString(), j, otherEntry.stepItem().getIdAsString());
                if (item.equals(otherEntry.stepItem()))
                {
                    Step newEntry = new Step(item, (count + otherEntry.count()), entry.type(), entry.category(), entry.networkId());
//                    newEntry.debug();
                    ignores.add(j);
                    list.add(newEntry);
                    matched = true;
                }
            }

            if (!matched)
            {
                list.add(entry);
            }
        }

        for (int i = 0; i < right.size(); i++)
        {
            Step entry = right.get(i);
//            LOGGER.info("ignores: // right[{}]: [{}] (Contains: {})", i, entry.stepItem().getIdAsString(), ignores.contains(i));

            if (!ignores.contains(i))
            {
                list.add(entry);
            }
        }

//        LOGGER.info("combineSteps: combined [{}]", list.size());
        return list;
    }

    /**
     * Combines two-Step lists when the Item matches; while deduplicating by simply adding the total required.
     * @param left ()
     * @param right ()
     * @return ()
     */
    public List<Result> combineResults(List<Result> left, List<Result> right, boolean addResultTotals)
    {
        List<Result> list = new ArrayList<>();
        List<Integer> ignores = new ArrayList<>();

//        LOGGER.info("combineResults: left [{}], right [{}]", left.size(), right.size());

        if (left.isEmpty() && !right.isEmpty())
        {
            return right;
        }

        for (Result entry : left)
        {
            RegistryEntry<Item> item = entry.resultItem();
            final int count = entry.total();
            boolean matched = false;

//            LOGGER.info("combineResults: left[{}]: [{}]", i, item.getIdAsString());
            for (int j = 0; j < right.size(); j++)
            {
                Result otherEntry = right.get(j);

//                LOGGER.info("left[{}]: [{}] // right[{}]: [{}]", i, item.getIdAsString(), j, otherEntry.stepItem().getIdAsString());
                if (item.equals(otherEntry.resultItem()))
                {
                    Result newEntry = new Result(item, addResultTotals ? (count + otherEntry.total()) : count);
//                    newEntry.debug();
                    ignores.add(j);
                    list.add(newEntry);
                    matched = true;
                }
            }

            if (!matched)
            {
                list.add(entry);
            }
        }

        for (int i = 0; i < right.size(); i++)
        {
            Result entry = right.get(i);
//            LOGGER.info("ignores: // right[{}]: [{}] (Contains: {})", i, entry.resultItem().getIdAsString(), ignores.contains(i));

            if (!ignores.contains(i))
            {
                list.add(entry);
            }
        }

//        LOGGER.info("combineResults: combined [{}]", list.size());
        return list;
    }

    public boolean isEmptyFlat() { return this.entriesFlat.isEmpty(); }

    public boolean isEmptyCombined() { return this.entriesCombined.isEmpty(); }

    public int sizeFlat() { return this.entriesFlat.size(); }

    public int sizeCombined() { return this.entriesCombined.size(); }

    public List<Entry> getEntriesFlat() { return this.entriesFlat; }

    public List<Entry> getEntriesCombined() { return this.entriesCombined; }

    public Iterable<Entry> iteratorFlat() { return Iterables.concat(this.entriesFlat); }

    public Iterable<Entry> iteratorCombined() { return Iterables.concat(this.entriesCombined); }

    public Stream<Entry> streamFlat() { return this.entriesFlat.stream(); }

    public Stream<Entry> streamCombined() { return this.entriesCombined.stream(); }

    public void clearFlat()
    {
        this.entriesFlat.clear();
    }

    public void clearCombined()
    {
        this.entriesCombined.clear();
    }

    public void clearAll()
    {
        this.clearFlat();
        this.clearCombined();
    }

    public Pair<Step, List<Step>> buildStepsBase(MaterialListJsonBase base, List<Step> lastSteps, Result result)
    {
        RegistryEntry<Item> resultItem = base.getInput();
        final int total = base.getCount();

//        if (result != null)
//        {
//            result.debug();
//        }

        List<MaterialListJsonBase> requirements = new ArrayList<>();
        Step furnaceStep;
        Step stonecutterStep;
        Step recipeStep;
        Step finalStep = null;

//        Step baseStep = new Step(resultItem, total, RecipeBookUtils.Type.UNKNOWN, "RESULT", -1);
//        LOGGER.debug("buildStepsEntryEach: (Basic) from resultItem [{}]", resultItem.getIdAsString());
//        baseStep.debug();
//        lastSteps.addFirst(baseStep);

//        LOGGER.debug("buildStepsBase: resultItem: [{}], count [{}], lastSteps [{}]", resultItem.getIdAsString(), total, lastSteps.size());

        if (base.getMaterialsFurnace() != null)
        {
            Pair<Step, List<MaterialListJsonBase>> pair = this.buildStepsEntryEach(resultItem, base.getMaterialsFurnace(), RecipeBookUtils.Type.FURNACE);

            if (!pair.getRight().isEmpty())
            {
                requirements.addAll(pair.getRight());
            }

            furnaceStep = pair.getLeft();
            lastSteps.addFirst(furnaceStep);
        }
        if (base.getMaterialsStonecutter() != null)
        {
            Pair<Step, List<MaterialListJsonBase>> pair = this.buildStepsEntryEach(resultItem, base.getMaterialsStonecutter(), RecipeBookUtils.Type.STONECUTTER);

            if (!pair.getRight().isEmpty())
            {
                requirements.addAll(pair.getRight());
            }

            stonecutterStep = pair.getLeft();
            lastSteps.addFirst(stonecutterStep);
        }
        if (base.getMaterialsCrafting() != null)
        {
            Pair<Step, List<MaterialListJsonBase>> pair = this.buildStepsEntryEach(resultItem, base.getMaterialsCrafting(), RecipeBookUtils.Type.SHAPELESS);

            if (!pair.getRight().isEmpty())
            {
                requirements.addAll(pair.getRight());
            }

            recipeStep = pair.getLeft();
            lastSteps.addFirst(recipeStep);
        }
        if (base.getMaterialsRemaining() != null)
        {
            // Final Step
            Pair<Step, List<MaterialListJsonBase>> pair = this.buildStepsEntryEach(resultItem, base.getMaterialsRemaining(), RecipeBookUtils.Type.UNKNOWN);

            if (!pair.getRight().isEmpty())
            {
                requirements.addAll(pair.getRight());
            }

            finalStep = pair.getLeft();
            lastSteps.addFirst(finalStep);
        }

        if (!requirements.isEmpty())
        {
            List<Step> list = new ArrayList<>();

            for (MaterialListJsonBase baseEach : requirements)
            {
//                LOGGER.debug("buildStepsBase: buildStepsBaseEach (Requirements) PRE steps size [{}]", lastSteps.size());
                Pair<Step, List<Step>> pair = this.buildStepsBaseEach(baseEach, lastSteps, result);

                if (pair.getRight() != null && !pair.getRight().isEmpty())
                {
                    list = this.combineSteps(list, pair.getRight());
                }

//                LOGGER.debug("buildStepsBase: buildStepsBaseEach (Requirements) POST steps size [{}], list size [{}], results size [{}]", lastSteps.size(), list.size());

                if (pair.getLeft() != null)
                {
                    // Final Step
                    list.add(pair.getLeft());
                    lastSteps = this.combineSteps(list, lastSteps);
                    Entry entryOut = new Entry(resultItem, total, lastSteps, result != null ? List.of(result) : List.of());
//                    LOGGER.debug("buildStepsBase: Entry (Requirements) -->");
//                    entryOut.debug();
                    this.putFlatEntry(entryOut);
                    this.putCombinedEntry(entryOut, true);
                    return Pair.of(null, List.of());
                }
            }
        }
        else if (finalStep != null)
        {
            Entry entryOut = new Entry(resultItem, total, lastSteps, result != null ? List.of(result) : List.of());
//            LOGGER.debug("buildStepsBase: Entry (No-Requirements) -->");
//            entryOut.debug();
            this.putFlatEntry(entryOut);
            this.putCombinedEntry(entryOut, true);
            return Pair.of(null, List.of());
        }

//        LOGGER.debug("buildStepsBase: No-Entry (Default) steps size [{}] -->", lastSteps.size());
        return Pair.of(null, List.of());
    }

    public Pair<Step, List<Step>> buildStepsBaseEach(MaterialListJsonBase base, List<Step> lastSteps, Result result)
    {
        return this.buildStepsBase(base, lastSteps, result);
    }

    public Pair<Step, List<MaterialListJsonBase>> buildStepsEntryEach(RegistryEntry<Item> resultItem, MaterialListJsonEntry materials, RecipeBookUtils.Type typeIn)
    {
        RegistryEntry<Item> stepItem = materials.getInputItem();
        final int stepCount = materials.getTotal();

//        LOGGER.debug("buildStepsEntryEach: resultItem: [{}], typeIn [{}]", resultItem.getIdAsString(), typeIn.name());

        if (materials.hasOutput())
        {
            NetworkRecipeId stepNetworkId = materials.getPrimaryId();
//            HashMap<NetworkRecipeId, List<Ingredient>> stepIngs = materials.getRecipeRequirements();
            HashMap<NetworkRecipeId, RecipeBookCategory> stepCats = materials.getRecipeCategory();
            HashMap<NetworkRecipeId, RecipeBookUtils.Type> stepTypes = materials.getRecipeTypes();

//            List<MaterialListJsonBase> requirements = materials.getRequirements();
//            List<Ingredient> ings = stepIngs.get(stepNetworkId);
            RecipeBookCategory category = stepCats.get(stepNetworkId);
            RecipeBookUtils.Type type = stepTypes.get(stepNetworkId);

            Step stepOut = new Step(stepItem, stepCount, type, RecipeBookUtils.getRecipeCategoryId(category), stepNetworkId.index());
//            LOGGER.debug("buildStepsEntryEach: (Output) from resultItem [{}]", resultItem.getIdAsString());
//            stepOut.debug();

            return Pair.of(stepOut, materials.getRequirements());
        }

        Step stepOut = new Step(stepItem, stepCount, typeIn, GATHER_KEY, -1);
//        LOGGER.debug("buildStepsEntryEach: (Basic) from resultItem [{}]", resultItem.getIdAsString());
//        stepOut.debug();

        return Pair.of(stepOut, List.of());
    }

    public void simplifyFlatEntrySteps()
    {
        List<Step> otherSteps = new ArrayList<>();

        for (int i = 0; i < this.entriesFlat.size(); i++)
        {
            Entry entry = this.entriesFlat.get(i);
            List<Step> entrySteps = entry.steps();
//            int entryCount = entry.total();
//            LOGGER.debug("simplifyFlatEntrySteps(): Entry[{}/{}]: steps [{}], otherSteps [{}]", i, entry.rawItem.getIdAsString(), entry.steps().size(), otherSteps.size());

            if (i == 0)
            {
//                LOGGER.debug("simplifyFlatEntrySteps(): Entry[{}/{}]: --> UPDATE OTHER STEPS", i, entry.rawItem.getIdAsString());
                otherSteps.addAll(entrySteps);
            }

//            boolean updated = false;
//            boolean found = false;
//            List<Step> updatedSteps = new ArrayList<>();
//            int updatedCount = entryCount;
//            RegistryEntry<Item> prevStep = null;

//            for (Step step : entrySteps)
//            {
//                if (step.stepItem().equals(entry.rawItem()) && Objects.equals(step.category(), GATHER_KEY))
//                {
//                    if (step.count() >= entryCount)
//                    {
//                        if (step.count() > entryCount)
//                        {
//                            updatedCount = step.count();
//                            updated = true;
//                        }
//
//                        if (!found)
//                        {
//                            updatedSteps.add(step);
//                            found = true;
//                        }
//                        else
//                        {
//                            // Ignore it
//                            LOGGER.debug("simplifyFlatEntrySteps(): Entry[{}/{}]: --> IGNORE STEP [Already matched/found]", i, entry.rawItem.getIdAsString());
//                            updated = true;
//                        }
//                    }
//                    else
//                    {
//                        // Ignore it
//                        LOGGER.debug("simplifyFlatEntrySteps(): Entry[{}/{}]: --> IGNORE STEP [{} < {}]", i, entry.rawItem.getIdAsString(), step.count(), entryCount);
//                        updated = true;
//                    }
//                }
//                else
//                {
//                    if (prevStep != null && prevStep.equals(step.stepItem()))
//                    {
//                        LOGGER.debug("simplifyFlatEntrySteps(): Entry[{}/{}]: --> IGNORE STEP [Equals Previous Item]", i, entry.rawItem.getIdAsString());
//                        updated = true;
//                    }
//                    else
//                    {
//                        updatedSteps.add(step);
//                    }
//                }
//
//                prevStep = step.stepItem();
//            }
//
//            if (updated)
//            {
//                LOGGER.debug("simplifyFlatEntrySteps(): Entry[{}/{}]: --> SIMPLIFY STEPS [{} -> {}]", i, entry.rawItem.getIdAsString(), entrySteps.size(), updatedSteps.size());
//                Entry newEntry = new Entry(entry.rawItem(), updatedCount, updatedSteps, entry.results());
//                this.entriesFlat.set(i, newEntry);
//                entrySteps.clear();
//                entrySteps.addAll(updatedSteps);
//
//                if (i == 0)
//                {
//                    otherSteps.clear();
//                    otherSteps.addAll(updatedSteps);
//                }
//            }

            if (i == 0)
            {
                continue;
            }

            if (!otherSteps.isEmpty() && this.compareSteps(entrySteps, otherSteps))
            {
//                LOGGER.debug("simplifyFlatEntrySteps(): Entry[{}/{}]: --> REPLACE STEPS", i, entry.rawItem.getIdAsString());
                this.entriesFlat.set(i, new Entry(entry.rawItem(), entry.total(), List.of(), entry.results()));
            }
            else
            {
//                LOGGER.debug("simplifyFlatEntrySteps(): Entry[{}/{}]: --> NEXT", i, entry.rawItem.getIdAsString());
                otherSteps.clear();
                otherSteps.addAll(entrySteps);
            }
        }

        // Pass Number 2
        this.simplyFlatEntryResults();
    }

    private void simplyFlatEntryResults()
    {
        // Compare Steps by results
        for (int i = 0; i < this.entriesFlat.size(); i++)
        {
            Entry entry = this.getEntriesFlat().get(i);
            List<Result> entryResults = entry.results();
            List<Step> entrySteps = entry.steps();

            if (!entryResults.isEmpty() && !entrySteps.isEmpty())
            {
                Result entryResult = entryResults.getFirst();

                for (int j = 0; j < this.entriesFlat.size(); j++)
                {
                    if (i == j) continue;

                    Entry otherEntry = this.entriesFlat.get(j);
                    List<Result> otherResults = otherEntry.results();
                    List<Step> otherSteps = otherEntry.steps();

                    if (!otherResults.isEmpty() && !otherSteps.isEmpty())
                    {
                        Result otherResult = otherResults.getFirst();

                        if (otherResult.equals(entryResult) &&
                            this.compareSteps(otherSteps, entrySteps))
                        {
                            // Clear Steps if equal
//                            LOGGER.debug("simplyFlatEntryResults(): Entry[{}/{}]: --> Clear Matching Steps [{} -> 0]", j, otherEntry.rawItem.getIdAsString(), otherSteps.size());
                            this.entriesFlat.set(j, new Entry(otherEntry.rawItem(), otherEntry.total(), List.of(), otherResults));
                        }
                    }
                }
            }
        }
    }

    private boolean compareSteps(List<Step> left, List<Step> right)
    {
        if (left.size() != right.size())
        {
            return false;
        }

        int lCount = 0;

        for (Step entry : left)
        {
            if (this.containsStep(entry, right))
            {
                lCount++;
            }
        }

        int rCount = 0;

        for (Step entry : right)
        {
            if (this.containsStep(entry, left))
            {
                rCount++;
            }
        }

        return lCount == rCount;
    }

    // Binary compare each element.
    private boolean containsStep(Step left, List<Step> right)
    {
        for (Step step : right)
        {
            if (left.equals(step))
            {
                return true;
            }
        }

        return false;
    }

    public void repackCombinedEntries()
    {
        List<Entry> list = new ArrayList<>();

        for (Entry entry : this.entriesCombined)
        {
            list.addAll(this.combineUnpackedItems(entry));
        }

        this.clearCombined();

        for (Entry entry : list)
        {
            this.putCombinedEntry(entry, false);
        }
    }

    public record Step(RegistryEntry<Item> stepItem, Integer count, RecipeBookUtils.Type type, String category, Integer networkId)
    {
        public static final Codec<Step> CODEC = RecordCodecBuilder.create(
                inst -> inst.group(
                        Item.ENTRY_CODEC.fieldOf("StepItem").forGetter(get -> get.stepItem),
                        PrimitiveCodec.INT.fieldOf("StepCount").forGetter(get -> get.count),
                        RecipeBookUtils.Type.CODEC.fieldOf("RecipeType").forGetter(get -> get.type),
                        PrimitiveCodec.STRING.fieldOf("RecipeCategory").forGetter(get -> get.category),
                        PrimitiveCodec.INT.fieldOf("RecipeId").forGetter(get -> get.networkId)
                ).apply(inst, Step::new)
        );

        public boolean equals(Step otherStep)
        {
            return Objects.equals(this.stepItem(), otherStep.stepItem()) &&
                   Objects.equals(this.count(), otherStep.count()) &&
                   Objects.equals(this.type, otherStep.type()) &&
                   Objects.equals(this.category(), otherStep.category()) &&
                   Objects.equals(this.networkId(), otherStep.networkId());
        }

//        public void debug()
//        {
//            LOGGER.debug("Step(): item: [{}], count: [{}], type: [{}], category: [{}], id: [{}]",
//                         this.stepItem().getIdAsString(), this.count(),
//                         this.type().name(), this.category(), this.networkId());
//        }
    }

    public record Result(RegistryEntry<Item> resultItem, Integer total)
    {
        public static final Codec<Result> CODEC = RecordCodecBuilder.create(
                inst -> inst.group(
                        Item.ENTRY_CODEC.fieldOf("ResultItem").forGetter(get -> get.resultItem),
                        PrimitiveCodec.INT.fieldOf("ResultTotal").forGetter(get -> get.total)
                ).apply(inst, Result::new)
        );

        public boolean equals(Result otherResult)
        {
            return Objects.equals(this.resultItem(), otherResult.resultItem()) &&
                   Objects.equals(this.total(), otherResult.total());
        }

//        public void debug()
//        {
//            LOGGER.debug("Result(): item: [{}], total: [{}]",
//                         this.resultItem().getIdAsString(), this.total());
//        }
    }

    public record Entry(RegistryEntry<Item> rawItem, Integer total, List<Step> steps, List<Result> results)
    {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(
                inst -> inst.group(
                        Item.ENTRY_CODEC.fieldOf("RawItem").forGetter(get -> get.rawItem),
                        PrimitiveCodec.INT.fieldOf("TotalEstimate").forGetter(get -> get.total),
                        Codec.list(Step.CODEC).fieldOf("Steps").forGetter(get -> get.steps),
                        Codec.list(Result.CODEC).fieldOf("Results").forGetter(get -> get.results)
                ).apply(inst, Entry::new)
        );

//        public void debug()
//        {
//            LOGGER.debug("Entry(): item: [{}], total: [{}], STEPS/RESULTS -->",
//                         this.rawItem().getIdAsString(), this.total());
//
//            for (Step step : this.steps())
//            {
//                step.debug();
//            }
//            for (Result result : this.results())
//            {
//                result.debug();
//            }
//        }
    }

    public JsonElement toFlatJson(RegistryOps<?> ops)
    {
        JsonArray arr = new JsonArray();

        if (!this.isEmptyFlat())
        {
            this.entriesFlat.forEach(
                    (entry) ->
                            arr.add((JsonElement) Entry.CODEC.encodeStart(ops, entry).getPartialOrThrow())
            );
        }

        return arr;
    }

    public JsonElement toCombinedJson(RegistryOps<?> ops)
    {
        JsonArray arr = new JsonArray();

        if (!this.isEmptyCombined())
        {
            this.entriesCombined.forEach(
                    (entry) ->
                            arr.add((JsonElement) Entry.CODEC.encodeStart(ops, entry).getPartialOrThrow())
            );
        }

        return arr;
    }
}
