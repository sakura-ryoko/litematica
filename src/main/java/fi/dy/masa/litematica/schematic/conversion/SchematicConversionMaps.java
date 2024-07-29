package fi.dy.masa.litematica.schematic.conversion;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.NBTUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;

public class SchematicConversionMaps
{
    private static final Object2IntOpenHashMap<String> OLD_BLOCK_NAME_TO_SHIFTED_BLOCK_ID = DataFixUtils.make(new Object2IntOpenHashMap<>(), (map) -> { map.defaultReturnValue(-1); });
    private static final Int2ObjectOpenHashMap<String> ID_META_TO_UPDATED_NAME = new Int2ObjectOpenHashMap<>();
    private static final Object2IntOpenHashMap<BlockState> BLOCKSTATE_TO_ID_META = DataFixUtils.make(new Object2IntOpenHashMap<>(), (map) -> { map.defaultReturnValue(-1); });
    private static final Int2ObjectOpenHashMap<BlockState> ID_META_TO_BLOCKSTATE = new Int2ObjectOpenHashMap<>();
    private static final HashMap<String, String> OLD_NAME_TO_NEW_NAME = new HashMap<>();
    private static final HashMap<String, String> NEW_NAME_TO_OLD_NAME = new HashMap<>();
    private static final HashMap<NbtCompound, NbtCompound> OLD_STATE_TO_NEW_STATE = new HashMap<>();
    private static final HashMap<NbtCompound, NbtCompound> NEW_STATE_TO_OLD_STATE = new HashMap<>();
    private static final ArrayList<ConversionData> CACHED_DATA = new ArrayList<>();

    private static boolean initialized;

    public static void addEntry(int idMeta, String newStateString, String... oldStateStrings)
    {
        CACHED_DATA.add(new ConversionData(idMeta, newStateString, oldStateStrings));
    }

    public static void computeMaps()
    {
        if (initialized)
        {
            return;
        }

        clearMaps();
        addOverrides();

        for (ConversionData data : CACHED_DATA)
        {
            try
            {
                if (data.oldStateStrings.length > 0)
                {
                    NbtCompound oldStateTag = getStateTagFromString(data.oldStateStrings[0]);

                    if (oldStateTag != null)
                    {
                        String name = oldStateTag.getString("Name");
                        OLD_BLOCK_NAME_TO_SHIFTED_BLOCK_ID.putIfAbsent(name, data.idMeta & 0xFFF0);
                    }
                }

                NbtCompound newStateTag = getStateTagFromString(data.newStateString);

                if (newStateTag != null)
                {
                    addIdMetaToBlockState(data.idMeta, newStateTag, data.oldStateStrings);
                }
            }
            catch (Exception e)
            {
                Litematica.logger.warn("addEntry(): Exception while adding blockstate conversion map entry for ID '{}' (fixed state: '{}')", data.idMeta, data.newStateString, e);
            }
        }

        initialized = true;
    }

    @Nullable
    public static BlockState get_1_13_2_StateForIdMeta(int idMeta)
    {
        return ID_META_TO_BLOCKSTATE.get(idMeta);
    }

    public static NbtCompound get_1_13_2_StateTagFor_1_12_Tag(NbtCompound oldStateTag)
    {
        NbtCompound tag = OLD_STATE_TO_NEW_STATE.get(oldStateTag);
        return tag != null ? tag : oldStateTag;
    }

    public static NbtCompound get_1_12_StateTagFor_1_13_2_Tag(NbtCompound newStateTag)
    {
        NbtCompound tag = NEW_STATE_TO_OLD_STATE.get(newStateTag);
        return tag != null ? tag : newStateTag;
    }

    public static int getOldNameToShiftedBlockId(String oldBlockname)
    {
        return OLD_BLOCK_NAME_TO_SHIFTED_BLOCK_ID.getInt(oldBlockname);
    }

    private static void addOverrides()
    {
        BlockState air = Blocks.AIR.getDefaultState();
        BLOCKSTATE_TO_ID_META.put(air, 0);
        ID_META_TO_BLOCKSTATE.put(0, air);

        int idOldLog = (17 << 4) | 12;
        int idNewLog = (162 << 4) | 12;
        ID_META_TO_BLOCKSTATE.put(idOldLog | 0, Blocks.OAK_WOOD.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y));
        ID_META_TO_BLOCKSTATE.put(idOldLog | 1, Blocks.SPRUCE_WOOD.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y));
        ID_META_TO_BLOCKSTATE.put(idOldLog | 2, Blocks.BIRCH_WOOD.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y));
        ID_META_TO_BLOCKSTATE.put(idOldLog | 3, Blocks.JUNGLE_WOOD.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y));
        ID_META_TO_BLOCKSTATE.put(idNewLog | 0, Blocks.ACACIA_WOOD.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y));
        ID_META_TO_BLOCKSTATE.put(idNewLog | 1, Blocks.DARK_OAK_WOOD.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Y));

        // These will get converted to the correct type in the state fixers
        ID_META_TO_UPDATED_NAME.put(1648, "minecraft:melon");

        ID_META_TO_UPDATED_NAME.put(2304, "minecraft:skeleton_skull");
        ID_META_TO_UPDATED_NAME.put(2305, "minecraft:skeleton_skull");
        ID_META_TO_UPDATED_NAME.put(2306, "minecraft:skeleton_wall_skull");
        ID_META_TO_UPDATED_NAME.put(2307, "minecraft:skeleton_wall_skull");
        ID_META_TO_UPDATED_NAME.put(2308, "minecraft:skeleton_wall_skull");
        ID_META_TO_UPDATED_NAME.put(2309, "minecraft:skeleton_wall_skull");
        ID_META_TO_UPDATED_NAME.put(2312, "minecraft:skeleton_skull");
        ID_META_TO_UPDATED_NAME.put(2313, "minecraft:skeleton_skull");
        ID_META_TO_UPDATED_NAME.put(2314, "minecraft:skeleton_wall_skull");
        ID_META_TO_UPDATED_NAME.put(2315, "minecraft:skeleton_wall_skull");
        ID_META_TO_UPDATED_NAME.put(2316, "minecraft:skeleton_wall_skull");
        ID_META_TO_UPDATED_NAME.put(2317, "minecraft:skeleton_wall_skull");

        ID_META_TO_UPDATED_NAME.put(3664, "minecraft:shulker_box");
        ID_META_TO_UPDATED_NAME.put(3665, "minecraft:shulker_box");
        ID_META_TO_UPDATED_NAME.put(3666, "minecraft:shulker_box");
        ID_META_TO_UPDATED_NAME.put(3667, "minecraft:shulker_box");
        ID_META_TO_UPDATED_NAME.put(3668, "minecraft:shulker_box");
        ID_META_TO_UPDATED_NAME.put(3669, "minecraft:shulker_box");
    }

    private static void clearMaps()
    {
        OLD_BLOCK_NAME_TO_SHIFTED_BLOCK_ID.clear();
        ID_META_TO_UPDATED_NAME.clear();

        BLOCKSTATE_TO_ID_META.clear();
        ID_META_TO_BLOCKSTATE.clear();

        OLD_NAME_TO_NEW_NAME.clear();
        NEW_NAME_TO_OLD_NAME.clear();

        OLD_STATE_TO_NEW_STATE.clear();
        NEW_STATE_TO_OLD_STATE.clear();
    }

    private static void addIdMetaToBlockState(int idMeta, NbtCompound newStateTag, String... oldStateStrings)
    {
        try
        {
            // The flattening map actually has outdated names for some blocks...
            // Ie. some blocks were renamed after the flattening, so we need to handle those here.
            String newName = newStateTag.getString("Name");
            String overriddenName = ID_META_TO_UPDATED_NAME.get(idMeta);

            if (overriddenName != null)
            {
                newName = overriddenName;
                newStateTag.putString("Name", newName);
            }

            RegistryEntryLookup<Block> lookup = Registries.BLOCK.getReadOnlyWrapper();
            // Store the id + meta => state maps before renaming the block for the state <=> state maps
            BlockState state = NbtHelper.toBlockState(lookup, newStateTag);
            //System.out.printf("id: %5d, state: %s, tag: %s\n", idMeta, state, newStateTag);
            ID_META_TO_BLOCKSTATE.putIfAbsent(idMeta, state);

            // Don't override the id and meta for air, which is what unrecognized blocks will turn into
            BLOCKSTATE_TO_ID_META.putIfAbsent(state, idMeta);

            if (oldStateStrings.length > 0)
            {
                NbtCompound oldStateTag = getStateTagFromString(oldStateStrings[0]);
                String oldName = oldStateTag.getString("Name");

                // Don't run the vanilla block rename for overridden names
                if (overriddenName == null)
                {
                    newName = updateBlockName(newName, Configs.Generic.DATAFIXER_DEFAULT_SCHEMA.getIntegerValue());
                    newStateTag.putString("Name", newName);
                }

                if (oldName.equals(newName) == false)
                {
                    OLD_NAME_TO_NEW_NAME.putIfAbsent(oldName, newName);
                    NEW_NAME_TO_OLD_NAME.putIfAbsent(newName, oldName);
                }

                addOldStateToNewState(newStateTag, oldStateStrings);
            }
        }
        catch (Exception e)
        {
            Litematica.logger.warn("addIdMetaToBlockState(): Exception while adding blockstate conversion map entry for ID '{}'", idMeta, e);
        }
    }

    private static void addOldStateToNewState(NbtCompound newStateTagIn, String... oldStateStrings)
    {
        try
        {
            // A 1:1 mapping from the old state to the new state
            if (oldStateStrings.length == 1)
            {
                NbtCompound oldStateTag = getStateTagFromString(oldStateStrings[0]);

                if (oldStateTag != null)
                {
                    OLD_STATE_TO_NEW_STATE.putIfAbsent(oldStateTag, newStateTagIn);
                    NEW_STATE_TO_OLD_STATE.putIfAbsent(newStateTagIn, oldStateTag);
                }
            }
            // Multiple old states collapsed into one new state.
            // These are basically states where all the properties were not stored in metadata, but
            // some of the property values were calculated in the getActualState() method.
            else if (oldStateStrings.length > 1)
            {
                NbtCompound oldStateTag = getStateTagFromString(oldStateStrings[0]);

                // Same property names and same number of properties - just remap the block name.
                // FIXME Is this going to be correct for everything?
                if (oldStateTag != null && newStateTagIn.getKeys().equals(oldStateTag.getKeys()))
                {
                    String oldBlockName = oldStateTag.getString("Name");
                    String newBlockName = OLD_NAME_TO_NEW_NAME.get(oldBlockName);

                    if (newBlockName != null && newBlockName.equals(oldBlockName) == false)
                    {
                        for (String oldStateString : oldStateStrings)
                        {
                            oldStateTag = getStateTagFromString(oldStateString);

                            if (oldStateTag != null)
                            {
                                NbtCompound newTag = oldStateTag.copy();
                                newTag.putString("Name", newBlockName);

                                OLD_STATE_TO_NEW_STATE.putIfAbsent(oldStateTag, newTag);
                                NEW_STATE_TO_OLD_STATE.putIfAbsent(newTag, oldStateTag);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Litematica.logger.warn("addOldStateToNewState(): Exception while adding new blockstate to old blockstate conversion map entry for '{}'", newStateTagIn, e);
        }
    }

    public static NbtCompound getStateTagFromString(String str)
    {
        try
        {
            return StringNbtReader.parse(str.replace('\'', '"'));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static String updateBlockName(String oldName, int oldVersion)
    {
        NbtString tagStr = NbtString.of(oldName);

        try
        {
            return MinecraftClient.getInstance().getDataFixer().update(TypeReferences.BLOCK_NAME, new Dynamic<>(NbtOps.INSTANCE, tagStr), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue().asString();
        }
        catch (Exception e)
        {
            Litematica.logger.warn("updateBlockName: failed to update Block Name [{}], preserving original state (data may become lost)", oldName);
            return oldName;
        }
    }

    /**
     * These are the Vanilla Data Fixer's for the 1.20.x -> 1.20.5 changes
     */
    public static NbtCompound updateBlockStates(NbtCompound oldBlockState, int oldVersion)
    {
        try
        {
            return (NbtCompound) MinecraftClient.getInstance().getDataFixer().update(TypeReferences.BLOCK_STATE, new Dynamic<>(NbtOps.INSTANCE, oldBlockState), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue();
        }
        catch (Exception e)
        {
            Litematica.logger.warn("updateBlockStates: failed to update Block State [{}], preserving original state (data may become lost)",
                                   oldBlockState.contains("Name") ? oldBlockState.getString("Name") : "?");
            return oldBlockState;
        }
    }

    public static NbtCompound updateBlockEntity(NbtCompound oldBlockEntity, int oldVersion)
    {
        try
        {
            return (NbtCompound) MinecraftClient.getInstance().getDataFixer().update(TypeReferences.BLOCK_ENTITY, new Dynamic<>(NbtOps.INSTANCE, oldBlockEntity), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue();
        }
        catch (Exception e)
        {
            BlockPos pos = NBTUtils.readBlockPos(oldBlockEntity);
            Litematica.logger.warn("updateBlockEntity: failed to update Block Entity [{}] at [{}], preserving original state (data may become lost)",
                                   oldBlockEntity.contains("id") ? oldBlockEntity.getString("id") : "?", pos != null ? pos.toShortString() : "?");
            return oldBlockEntity;
        }
    }

    public static NbtCompound updateEntity(NbtCompound oldEntity, int oldVersion)
    {
        try
        {
            return (NbtCompound) MinecraftClient.getInstance().getDataFixer().update(TypeReferences.ENTITY, new Dynamic<>(NbtOps.INSTANCE, oldEntity), oldVersion, LitematicaSchematic.MINECRAFT_DATA_VERSION).getValue();
        }
        catch (Exception e)
        {
            Litematica.logger.warn("updateEntity: failed to update Entity [{}], preserving original state (data may become lost)",
                                   oldEntity.contains("id") ? oldEntity.getString("id") : "?");
            return oldEntity;
        }
    }

    // Fix missing "id" tags.  This seems to be an issue with 1.19.x litematics.
    public static NbtCompound checkForIdTag(NbtCompound tags)
    {
        if (tags.contains("id"))
        {
            return tags;
        }
        if (tags.contains("Id"))
        {
            tags.putString("id", tags.getString("Id"));
            return tags;
        }

        // We don't have an "id" tag, let's try to fix it
        if (tags.contains("Bees") || tags.contains("bees"))
        {
            tags.putString("id", "minecraft:beehive");
        }
        else if (tags.contains("TransferCooldown") && tags.contains("Items"))
        {
            tags.putString("id", "minecraft:hopper");
        }
        else if (tags.contains("SkullOwner"))
        {
            tags.putString("id", "minecraft:skull");
        }
        else if (tags.contains("Patterns") || tags.contains("patterns"))
        {
            tags.putString("id", "minecraft:banner");
        }
        else if (tags.contains("Sherds") || tags.contains("sherds"))
        {
            tags.putString("id", "minecraft:decorated_pot");
        }
        else if (tags.contains("last_interacted_slot") && tags.contains("Items"))
        {
            tags.putString("id", "minecraft:chiseled_bookshelf");
        }
        else if (tags.contains("CookTime") && tags.contains("Items"))
        {
            tags.putString("id", "minecraft:furnace");
        }
        else if (tags.contains("RecordItem"))
        {
            tags.putString("id", "minecraft:jukebox");
        }
        else if (tags.contains("Book") || tags.contains("book"))
        {
            tags.putString("id", "minecraft:lectern");
        }
        else if (tags.contains("front_text"))
        {
            tags.putString("id", "minecraft:sign");
        }
        else if (tags.contains("BrewTime") || tags.contains("Fuel"))
        {
            tags.putString("id", "minecraft:brewing_stand");
        }
        else if ((tags.contains("LootTable") && tags.contains("LootTableSeed")) || (tags.contains("hit_direction") || tags.contains("item")))
        {
            tags.putString("id", "minecraft:suspicious_sand");
        }
        else if (tags.contains("SpawnData") || tags.contains("SpawnPotentials"))
        {
            tags.putString("id", "minecraft:spawner");
        }
        else if (tags.contains("normal_config"))
        {
            tags.putString("id", "minecraft:trial_spawner");
        }
        else if (tags.contains("shared_data"))
        {
            tags.putString("id", "minecraft:vault");
        }
        else if (tags.contains("pool") && tags.contains("final_state") && tags.contains("placement_priority"))
        {
            tags.putString("id", "minecraft:jigsaw");
        }
        else if (tags.contains("author") && tags.contains("metadata") && tags.contains("showboundingbox"))
        {
            tags.putString("id", "minecraft:structure_block");
        }
        else if (tags.contains("ExactTeleport") && tags.contains("Age"))
        {
            tags.putString("id", "minecraft:end_gateway");
        }
        else if (tags.contains("Items"))
        {
            tags.putString("id", "minecraft:chest");
        }
        else if (tags.contains("last_vibration_frequency") || tags.contains("listener"))
        {
            tags.putString("id", "minecraft:sculk_sensor");
        }
        else if (tags.contains("warning_level") || tags.contains("listener"))
        {
            tags.putString("id", "minecraft:sculk_shrieker");
        }
        else if (tags.contains("OutputSignal"))
        {
            tags.putString("id", "minecraft:comparator");
        }
        else if (tags.contains("facing") || tags.contains("extending"))
        {
            tags.putString("id", "minecraft:piston");
        }
        else if (tags.contains("x") && tags.contains("y") && tags.contains("z"))
        {
            // Might only have x y z pos
            tags.putString("id", "minecraft:piston");
        }

        // Fix any erroneous Items tags with the null "tag" tag.
        if (tags.contains("Items"))
        {
            NbtList items = fixItemsTag(tags.getList("Items", Constants.NBT.TAG_COMPOUND));
            tags.put("Items", items);
        }

        return tags;
    }

    // Fix null 'tag' entries.  This seems to be an issue with 1.19.x litematics.
    private static NbtList fixItemsTag(NbtList items)
    {
        NbtList newList = new NbtList();

        for (int i = 0; i < items.size(); i++)
        {
            NbtCompound itemEntry = items.getCompound(i);
            if (itemEntry.contains("tag"))
            {
                NbtCompound tag = null;
                try
                {
                    tag = itemEntry.getCompound("tag");
                }
                catch (Exception ignored) {}

                // Remove 'tag' if it is set to null
                if (tag == null)
                {
                    itemEntry.remove("tag");
                }
                else
                {
                    // Fix nested entries if they exist
                    if (tag.contains("BlockEntityTag", Constants.NBT.TAG_COMPOUND))
                    {
                        NbtCompound entityEntry = tag.getCompound("BlockEntityTag");

                        if (entityEntry.contains("Items", Constants.NBT.TAG_LIST))
                        {
                            NbtList nestedItems = fixItemsTag(entityEntry.getList("Items", Constants.NBT.TAG_COMPOUND));
                            entityEntry.put("Items", nestedItems);
                        }

                        tag.put("BlockEntityTag", entityEntry);
                    }

                    itemEntry.put("tag", tag);
                }
            }

            newList.add(itemEntry);
        }

        return newList;
    }

    private static class ConversionData
    {
        private final int idMeta;
        private final String newStateString;
        private final String[] oldStateStrings;

        private ConversionData(int idMeta, String newStateString, String[] oldStateStrings)
        {
            this.idMeta = idMeta;
            this.newStateString = newStateString;
            this.oldStateStrings = oldStateStrings;
        }
    }
}
