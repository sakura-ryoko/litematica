package fi.dy.masa.litematica.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import fi.dy.masa.litematica.Litematica;
import net.minecraft.block.entity.*;
import net.minecraft.component.type.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;

import javax.annotation.Nullable;
import java.util.*;

/**
 * This file is meant to be a new central place for managing Component <-> NBT data
 * --> The Deprecated functions have been replaced by using the Vanilla DataFixer upon a schematic load.
 * This is useful code to reference, so I didn't delete it, and this still has used functions.
 */
public class ComponentUtils
{
    //public static final ComponentMap EMPTY = ComponentMap.EMPTY;
    //public static final Pattern PATTERN_COMPONENT_BASE = Pattern.compile("^(?<name>(?:[a-z0-9\\._-]+:)[a-z0-9\\._-]+)$");

    /**
     * Execute this prior to "reading in" a BlockEntity's NBT tags, so that Vanilla can handle the rest.
     * --> Deprecated, but this is an exhaustive list of older valid NBT tags.
     */
    /*
    @Deprecated
    private ComponentMap fromBlockEntityNBT(@Nonnull ItemStack stackIn, BlockEntity be, @Nonnull NbtCompound nbt, @Nonnull DynamicRegistryManager registryLookup)
    {
        BlockPos blockPos;
        Identifier blockId;
        BlockEntityType<?> blockType;
        Text customName;

        if (nbt.isEmpty())
        {
            Litematica.logger.error("fromBlockEntityNBT(): nbt given is empty");
            return null;
        }
        // Generic required data
        if (nbt.contains("x") && nbt.contains("y") && nbt.contains("z"))
        {
            blockPos = new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));

            //nbt.remove("x");
            //nbt.remove("y");
            //nbt.remove("z");
        }
        else
        {
            // Invalid NBT data
            Litematica.logger.error("fromBlockEntityNBT() received invalid NBT data, \"x, y, z\" (BlockPos) is missing");
            return null;
        }
        if (nbt.contains("id"))
        {
            String entityTypeString;
            entityTypeString = nbt.getString("id");
            blockId = Identifier.tryParse(entityTypeString);

            if (blockId != null)
            {
                Optional<BlockEntityType<?>> optionalType = Registries.BLOCK_ENTITY_TYPE.getOrEmpty(blockId);

                if (optionalType.isPresent())
                {
                    blockType = optionalType.get();
                }
                else
                {
                    Litematica.logger.warn("fromBlockEntityNBT() received nbt for invalid blockType id: {}", blockId.toString());
                    return null;
                }
            }
            else
            {
                Litematica.logger.warn("fromBlockEntityNBT() received nbt for invalid blockId: {}", entityTypeString);
                return null;
            }

            //nbt.remove("id");
        }
        else
        {
            // Invalid NBT data
            Litematica.logger.error("fromBlockEntityNBT() received invalid NBT data, \"id\" is missing!");
            return null;
        }

        ComponentMap.Builder compResult = ComponentMap.builder();
        String lootTableId = null;
        long lootSeed = -1;

        // Common "CustomName" data
        if (nbt.contains("CustomName"))
        {
            // Many Entities use the "CustomName" NBT, and it still exists in vanilla ...
            // for now?
            customName = Text.Serialization.fromJson(nbt.getString("CustomName"), registryLookup);
            compResult.add(DataComponentTypes.CUSTOM_NAME, customName);
            //nbt.remove("CustomName");
        }

        // Common "Loot-able Container" values
        if (nbt.contains("loot_table"))
        {
            lootTableId = nbt.getString("loot_table");

            //nbt.remove("loot_table");
        }
        else if (nbt.contains("LootTable"))
        {
            lootTableId = nbt.getString("LootTable");

            nbt.remove("LootTable");
            nbt.putString("loot_table", lootTableId);
        }
        if (nbt.contains("seed") && !blockType.equals(BlockEntityType.STRUCTURE_BLOCK))
        {
            lootSeed = nbt.getLong("seed");

            //nbt.remove("seed");
        }
        else if (nbt.contains("LootTableSeed"))
        {
            lootSeed = nbt.getLong("LootTableSeed");

            nbt.remove("LootTableSeed");
            nbt.putLong("seed", lootSeed);
        }

        // Loot table
        if (lootTableId != null && !lootTableId.isEmpty() && lootSeed > 0)
        {
            Identifier lootId = Identifier.tryParse(lootTableId);
            RegistryKey<LootTable> regKey = LootTables.EMPTY;
            // RegistryLookup by Id ?
            ContainerLootComponent lootContainer = new ContainerLootComponent(regKey, lootSeed);
            compResult.add(DataComponentTypes.CONTAINER_LOOT, lootContainer);
        }

        // Common "Container Locks"
        if (nbt.contains("Lock", 8))
        {
            String keyLock = nbt.getString("Lock");
            ContainerLock lock = new ContainerLock(keyLock);

            compResult.add(DataComponentTypes.LOCK, lock);

            nbt.remove("Lock");
        }

        if (blockType.equals(BlockEntityType.FURNACE) || blockType.equals(BlockEntityType.BLAST_FURNACE) || blockType.equals(BlockEntityType.SMOKER))
        {
            // No Components to Map ...
            // "Loot-able Container"
        }
        else if (blockType.equals(BlockEntityType.BANNER))
        {
            BannerPatternsComponent patternsComp = getBannerPatternsFromNBT(nbt, registryLookup);

            if (!patternsComp.equals(BannerPatternsComponent.DEFAULT))
            {
                compResult.add(DataComponentTypes.BANNER_PATTERNS, patternsComp);
            }
        }
        else if (blockType.equals(BlockEntityType.BARREL))
        {
            // It is currently not using ComponentMap to store its Inventory
            // "Loot-able Container"

            if (nbt.contains("Items"))
            {
                NbtList itemList = nbt.getList("Items", 10);
                DefaultedList<ItemStack> items = getItemStackListFromNbt(itemList, registryLookup);

                if (!items.isEmpty())
                {
                    // Process it based on block-type?
                    ContainerComponent container = ContainerComponent.fromStacks(items);
                    compResult.add(DataComponentTypes.CONTAINER, container);
                }
                //nbt.remove("Items");
            }
        }
        else if (blockType.equals(BlockEntityType.BEACON))
        {
            // Vanilla still reads this, but let's handle it also;
            // but I really don't know why a "Beacon" has a ContainerLock...

            // Lock --> Container Lock? --> Handled above.
            // primary_effect   --> no Components (String of Identifier)
            // secondary_effect --> no Components (String of Identifier)
            // Levels -->  No Components (Int)
        }
        else if (blockType.equals(BlockEntityType.BED))
        {
            // No NBT
        }
        else if (blockType.equals(BlockEntityType.BEEHIVE))
        {
            List<BeehiveBlockEntity.BeeData> beeList = getBeeDataFromNbt(nbt, registryLookup);

            if (beeList.isEmpty())
            {
                compResult.add(DataComponentTypes.BEES, beeList);
            }

            // BeeHiveBlock entity data, pre 24w09a
            if (nbt.contains("FlowerPos"))
            {
                // The FlowerPos isn't used in the ComponentData, but it is part of the Beehive Block Entity Data.
                int[] flowerArray;
                BlockPos flowerPos;

                flowerArray = nbt.getIntArray("FlowerPos");
                if (flowerArray.length == 3)
                {
                    flowerPos = new BlockPos(flowerArray[0], flowerArray[1], flowerArray[2]);
                }
                nbt.remove("FlowerPos");

                nbt.putIntArray("flower_pos", flowerArray);
            }
            // BeeHiveBlock Entity Data, post 24w10a
            else if (nbt.contains("flower_pos"))
            {
                // The FlowerPos isn't used in the ComponentData, but it is part of the Beehive Block Entity Data.
                int[] flowerArray;
                BlockPos flowerPos;

                flowerArray = nbt.getIntArray("flower_pos");
                if (flowerArray.length == 3) {
                    flowerPos = new BlockPos(flowerArray[0], flowerArray[1], flowerArray[2]);
                }
                //nbt.remove("flower_pos");
            }
            // Nowhere to put the "flower_pos" data, but the NbtFormat has changed.
            // But do we care about it?
        }
        else if (blockType.equals(BlockEntityType.BELL) || blockType.equals(BlockEntityType.CALIBRATED_SCULK_SENSOR))
        {
            // No NBT
        }
        else if (blockType.equals(BlockEntityType.BREWING_STAND))
        {
            // Has an inventory, but we can't place Skulls in there, so let vanilla handle it.
            // BrewTime --> ShortNbt
            // Fuel --> ByteNbt
        }
        else if (blockType.equals(BlockEntityType.BRUSHABLE_BLOCK))
        {
            // These have a Loot Table, and I would expect it to become a "Loot-able Container" Component
            // LootTable, type 8
            // LootTableSeed, LongNbt
            // hit_direction, IntNbt
            // item, CompoundNbt (The item that is the loot)
        }
        else if (blockType.equals(BlockEntityType.CAMPFIRE))
        {
            // Skulls can't be cooked, but this item uses the CONTAINER Component
            if (nbt.contains("Items"))
            {
                NbtList itemList = nbt.getList("Items", 10);
                DefaultedList<ItemStack> items = getItemStackListFromNbt(itemList, registryLookup);

                if (!items.isEmpty())
                {
                    ContainerComponent container = ContainerComponent.fromStacks(items);
                    compResult.add(DataComponentTypes.CONTAINER, container);
                }

                nbt.remove("Items");
            }
            // CookingTimes, type 11 (IntArray)
            // CookingTotalTimes, type 11, (IntArray)
        }
        else if (blockType.equals(BlockEntityType.CHEST))
        {
            // This doesn't use Components, but parse them anyway,
            // "Loot-able Container"

            if (nbt.contains("Items"))
            {
                NbtList itemList = nbt.getList("Items", 10);
                DefaultedList<ItemStack> items = getItemStackListFromNbt(itemList, registryLookup);

                if (!items.isEmpty())
                {
                    ContainerComponent container = ContainerComponent.fromStacks(items);
                    compResult.add(DataComponentTypes.CONTAINER, container);
                }

                //nbt.remove("Items");
            }
        }
        else if (blockType.equals(BlockEntityType.CHISELED_BOOKSHELF))
        {
            // Skulls can't be books, but this item uses the CONTAINER Component
            if (nbt.contains("Items"))
            {
                NbtList itemList = nbt.getList("Items", 10);
                DefaultedList<ItemStack> items = getItemStackListFromNbt(itemList, registryLookup);

                if (!items.isEmpty())
                {
                    ContainerComponent container = ContainerComponent.fromStacks(items);
                    compResult.add(DataComponentTypes.CONTAINER, container);
                }

                nbt.remove("Items");
            }
            // last_interacted_slot, IntNbt
        }
        else if (blockType.equals(BlockEntityType.COMMAND_BLOCK))
        {
            // This uses CUSTOM_NAME Components

            // powered, booleanNbt
            // conditionMet, booleanNbt
            // auto, booleanNbt
        }
        else if (blockType.equals(BlockEntityType.COMPARATOR))
        {
            // OutputSignal, IntNbt
        }
        else if (blockType.equals(BlockEntityType.CONDUIT))
        {
            // Target, UUID Nbt
        }
        else if (blockType.equals(BlockEntityType.CRAFTER))
        {
            // Has an inventory, but it doesn't use Components ... yet, but it probably will
            // "Loot-able Container"

            if (nbt.contains("Items"))
            {
                NbtList itemList = nbt.getList("Items", 10);
                DefaultedList<ItemStack> items = getItemStackListFromNbt(itemList, registryLookup);

                if (!items.isEmpty())
                {
                    ContainerComponent container = ContainerComponent.fromStacks(items);
                    compResult.add(DataComponentTypes.CONTAINER, container);
                }

                //nbt.remove("Items");
            }
            // crafting_ticks_remaining, IntNbt
            // disabled_slots, IntArrayNbt
            // triggered, IntNbt
        }
        else if (blockType.equals(BlockEntityType.DAYLIGHT_DETECTOR))
        {
            // No NBT Data
        }
        else if (blockType.equals(BlockEntityType.DECORATED_POT))
        {
            if (nbt.contains("item"))
            {
                NbtCompound items = nbt.getCompound("item");
                ItemStack itemStack = getItemStackFromNbt(items, registryLookup);

                if (!itemStack.isEmpty())
                {
                    DefaultedList<ItemStack> itemList = DefaultedList.of();
                    itemList.add(itemStack);
                    ContainerComponent container = ContainerComponent.fromStacks(itemList);
                    compResult.add(DataComponentTypes.CONTAINER, container);
                }

                nbt.remove("item");
            }
            if (nbt.contains("sherds", 9))
            {
                Sherds sherds = getSherdsFromNbt(nbt, registryLookup);

                compResult.add(DataComponentTypes.POT_DECORATIONS, sherds);

                nbt.remove("sherds");
            }
        }
        else if (blockType.equals(BlockEntityType.DISPENSER) || blockType.equals(BlockEntityType.DROPPER))
        {
            // Has an inventory, but it doesn't use Components ... yet, but it probably will
            // "Loot-able Container"

            if (nbt.contains("Items"))
            {
                NbtList itemList = nbt.getList("Items", 10);
                DefaultedList<ItemStack> items = getItemStackListFromNbt(itemList, registryLookup);

                if (!items.isEmpty())
                {
                    ContainerComponent container = ContainerComponent.fromStacks(items);
                    compResult.add(DataComponentTypes.CONTAINER, container);
                }

                //nbt.remove("Items");
            }
        }
        else if (blockType.equals(BlockEntityType.ENCHANTING_TABLE))
        {
            // Only has a CustomName
        }
        else if (blockType.equals(BlockEntityType.END_GATEWAY))
        {
            BlockPos exitPos;
            int[] exitArray = new int[3];

            // Pre 24w09a format
            if (nbt.contains("ExitPortal", 10))
            {
                NbtCompound exitPotal = nbt.getCompound("ExitPortal");
                exitPos = new BlockPos(exitPotal.getInt("X"), exitPotal.getInt("Y"), exitPotal.getInt("Z"));

                nbt.remove("ExitPortal");

                exitArray[0] = exitPos.getX();
                exitArray[1] = exitPos.getY();
                exitArray[2] = exitPos.getZ();

                //nbt.putIntArray("exit_portal", exitArray);
            }
            // Post 24w10a format
            else if (nbt.contains("exit_portal", 11))
            {
                exitArray = nbt.getIntArray("exit_portal");
                exitPos = new BlockPos(exitArray[0], exitArray[1], exitArray[2]);

                nbt.remove("exit_portal");
            }
            // Age, LongNbt
            // ExactTeleport, booleanNbt
        }
        else if (blockType.equals(BlockEntityType.END_PORTAL))
        {
            // No NBT Data
        }
        else if (blockType.equals(BlockEntityType.ENDER_CHEST))
        {
            // No NBT Data
        }
        else if (blockType.equals(BlockEntityType.HANGING_SIGN) || blockType.equals(BlockEntityType.SIGN))
        {
            // front_text, CompoundNbt
            // back_text, CompoundNbt
            // is_waxed, booleanNbt
        }
        else if (blockType.equals(BlockEntityType.HOPPER))
        {
            // Has an inventory, but it doesn't use Components ... yet, but it probably will
            // "Loot-able Container"

            if (nbt.contains("Items"))
            {
                NbtList itemList = nbt.getList("Items", 10);
                DefaultedList<ItemStack> items = getItemStackListFromNbt(itemList, registryLookup);

                if (!items.isEmpty())
                {
                    ContainerComponent container = ContainerComponent.fromStacks(items);
                    compResult.add(DataComponentTypes.CONTAINER, container);
                }

                //nbt.remove("Items");

                // TransferCooldown, IntNbt
            }
        }
        else if (blockType.equals(BlockEntityType.JIGSAW))
        {
            // name, stringNbt
            // target, stringNbt
            // pool, stringNbt
            // final_state, stringNbt
            // joint, stringNbt
            // placement_priority, intNbt
            // selection_priority, intNbt
        }
        else if (blockType.equals(BlockEntityType.JUKEBOX))
        {
            // RecordItem, CompoundNbt (Item Stack)
            // IsPlaying, booleanNbt
            // RecordStartTick, longNbt
            // TickCount, longNbt
        }
        else if (blockType.equals(BlockEntityType.LECTERN))
        {
            if (nbt.contains("Book", 10))
            {
                //NbtCompound bookNbt = nbt.getCompound("Book");
                //ItemStack bookStack = ItemStack.fromNbtOrEmpty(registryLookup, bookNbt);

                // WrittenBookContentComponent, but as an Item Component,
                // it is attached to "WrittenBookItem"
            }
            // Page / Int
        }
        else if (blockType.equals(BlockEntityType.MOB_SPAWNER))
        {
            // MobSpawnerLogic:
            // Delay, ShortNbt (type 99)
            // SpawnData, CompoundNbt (type 10)
            // SpawnPotentials, ListNbt (type 9)
            // MinSpawnDelay, ShortNbt (type 99)
            // MaxSpawnDelay, ShortNbt (type 99)
            // SpawnCount, ShortNbt (type 99)
            // MaxNearbyEntities, ShortNbt (type 99)
            // RequiredPlayerRange, ShortNbt (type 99)
            // SpawnRange, ShortNbt (type 99)
        }
        else if (blockType.equals(BlockEntityType.PISTON))
        {
            // blockState, Compound
            // facing, intNbt
            // progress, floatNbt
            // extending, booleanNbt
            // source, booleanNbt
        }
        else if (blockType.equals(BlockEntityType.SCULK_CATALYST))
        {
            // cursors, NbtList (type 9 / get 10 ?)
            // SculkSpreadManager -->
            // pos
            // charge
            // decay_delay
            // update_delay
            // facings
        }
        else if (blockType.equals(BlockEntityType.SCULK_SENSOR))
        {
            // last_vibration_frequency, intNbt
            // listener, Compound (type 10)
        }
        else if (blockType.equals(BlockEntityType.SCULK_SHRIEKER))
        {
            // warning_level, intNbt (type 99 ?)
            // listener, Compound (type 10)
        }
        else if (blockType.equals(BlockEntityType.SHULKER_BOX))
        {
            // "Loot-able Container"

            if (nbt.contains("Items"))
            {
                NbtList itemList = nbt.getList("Items", 10);
                DefaultedList<ItemStack> items = getItemStackListFromNbt(itemList, registryLookup);

                if (!items.isEmpty())
                {
                    ContainerComponent container = ContainerComponent.fromStacks(items);
                    compResult.add(DataComponentTypes.CONTAINER, container);
                }

                //nbt.remove("Items");
            }
        }
        else if (blockType.equals(BlockEntityType.SKULL))
        {
            ProfileComponent skullProfile = null;

            // Pre 24w09a type
            if (nbt.contains("SkullOwner", 10))
            {
                NbtCompound skullNbt = nbt.getCompound("SkullOwner");
                skullProfile = getSkullProfileFromNBT(skullNbt);

                nbt.remove("SkullOwner");
            }
            // Pre 24w09a type
            else if (nbt.contains("ExtraType", 8))
            {
                String extraUUID = nbt.getString("ExtraType");
                skullProfile = getSkullProfileFromString(extraUUID);

                nbt.remove("ExtraType");
            }
            // Post 24w10a type
            else if (nbt.contains("profile"))
            {
                NbtCompound skullNbt = nbt.getCompound("profile");
                skullProfile = getSkullProfileFromProfile(skullNbt);

                //nbt.remove("profile");
            }
            else
            {
                Litematica.debugLog("fromBlockEntityNBT(): Skull Entity does not have a Player profile under Nbt data");
            }
            if (skullProfile != null)
            {
                compResult.add(DataComponentTypes.PROFILE, skullProfile);
            }

            // note_block_sound, stringNbt
        }
        else if (blockType.equals(BlockEntityType.STRUCTURE_BLOCK))
        {
            // name, stringNbt
            // author, stringNbt
            // metadata, stringNbt
            // posX, intNbt
            // posY, intNbt
            // posZ, intNbt
            // sizeX, intNbt
            // sizeY, intNbt
            // sizeZ, intNbt
            // rotation, stringNbt
            // mirror, stringNbt
            // mode, stringNbt
            // ignoreEntities, booleanNbt
            // powered, booleanNbt
            // showair, booleanNbt
            // showboundingbox, booleanNbt
            // integrity, floatNbt
            // seed, longNbt <-- doesn't this conflict with the "Loot Table Seed" ?
        }
        else if (blockType.equals(BlockEntityType.TRAPPED_CHEST))
        {
            // "Loot-able Container"

            if (nbt.contains("Items"))
            {
                NbtList itemList = nbt.getList("Items", 10);
                DefaultedList<ItemStack> items = getItemStackListFromNbt(itemList, registryLookup);

                if (!items.isEmpty())
                {
                    ContainerComponent container = ContainerComponent.fromStacks(items);
                    compResult.add(DataComponentTypes.CONTAINER, container);
                }

                //nbt.remove("Items");
            }
        }
        else if (blockType.equals(BlockEntityType.TRIAL_SPAWNER))
        {
            // required_player_range, int
            // spawn_range, int
            // total_mobs, float
            // simultaneous_mobs, float
            // total_mobs_added_per_player, float
            // simultaneous_mobs_added_per_player, float
            // ticks_between_spawn, int
            // target_cooldown_length, int
            // spawn_potentials (MobSpawnerEntry) -->
                // entity, CompoundNbt
                // custom_spawn_rules (CustomSpawnRules) -->
                    // id, string
                    // block_light_limit
                    // sky_light_limit
            // loot_tables_to_eject, identifiers
            //
        }
        else if (blockType.equals(BlockEntityType.VAULT))
        {
            // server_data, NbtElement -->
                // rewarded_players, Set.of(UUID)
                // state_updating_resumes_at, long
                // items_to_eject, List.of(ItemStack)
                // total_ejections_needed, int
            // config, NbtElement -->
                // loot_table, identifier
                // activation_range, double
                // deactivation_range, double
                // key_item, ItemStack
                // override_loot_table_to_display, identifier
            // shared_data, NbtElement -->
                // display_item, ItemStack
                // connected_players, Set.of(UUID)
                // connected_particles_range, double
        }
        else
        {
            Litematica.logger.warn("fromBlockEntityNBT(): Unhandled Block Entity Type {}", blockId.toString());
        }

        return compResult.build();
    }
     */

    /**
     * In use by ItemUtils -> PickTE from Schematic World.
     */
    public static List<BeehiveBlockEntity.BeeData> getBeesDataFromNbt(NbtList beeNbtList)
    {
        List<BeehiveBlockEntity.BeeData> beeList = new ArrayList<>();

        for (int i = 0; i < beeNbtList.size(); i++)
        {
            NbtCompound beeNbt = beeNbtList.getCompound(i);
            BeehiveBlockEntity.BeeData beeData;

            if (beeNbt.contains("entity_data"))
            {
                NbtCompound beeEnt2 = beeNbt.getCompound("entity_data");
                String beeId = beeEnt2.getString("id");
                // beeId = Registries.ENTITY_TYPE.getId(EntityType.BEE).toString();
                int beeTicksInHive2 = beeNbt.getInt("ticks_in_hive");
                int occupationTicks2 = beeNbt.getInt("min_ticks_in_hive");

                beeData = new BeehiveBlockEntity.BeeData(NbtComponent.of(beeEnt2), beeTicksInHive2, occupationTicks2);

                beeList.add(beeData);
            }
        }
        if (beeList.isEmpty())
        {
            return List.of();
        }
        else
        {
            return beeList;
        }
    }

    /**
     *  Is there a simpler way to do this to translate from pre-1.20.5?
     */
    @Nullable
    public static RegistryEntry<BannerPattern> getBannerPatternEntryByIdPre1205(String patternId, DynamicRegistryManager registryManager)
    {
        RegistryKey<BannerPattern> key;

        switch (patternId)
        {
            case "b"   -> key = BannerPatterns.BASE;
            case "bl"  -> key = BannerPatterns.SQUARE_BOTTOM_LEFT;
            case "br"  -> key = BannerPatterns.SQUARE_BOTTOM_RIGHT;
            case "tl"  -> key = BannerPatterns.SQUARE_TOP_LEFT;
            case "tr"  -> key = BannerPatterns.SQUARE_TOP_RIGHT;
            case "bs"  -> key = BannerPatterns.STRIPE_BOTTOM;
            case "ts"  -> key = BannerPatterns.STRIPE_TOP;
            case "ls"  -> key = BannerPatterns.STRIPE_LEFT;
            case "rs"  -> key = BannerPatterns.STRIPE_RIGHT;
            case "cs"  -> key = BannerPatterns.STRIPE_CENTER;
            case "ms"  -> key = BannerPatterns.STRIPE_MIDDLE;
            case "drs" -> key = BannerPatterns.STRIPE_DOWNRIGHT;
            case "dls" -> key = BannerPatterns.STRIPE_DOWNLEFT;
            case "ss"  -> key = BannerPatterns.SMALL_STRIPES;
            case "cr"  -> key = BannerPatterns.CROSS;
            case "sc"  -> key = BannerPatterns.STRAIGHT_CROSS;
            case "bt"  -> key = BannerPatterns.TRIANGLE_BOTTOM;
            case "tt"  -> key = BannerPatterns.TRIANGLE_TOP;
            case "bts" -> key = BannerPatterns.TRIANGLES_BOTTOM;
            case "tts" -> key = BannerPatterns.TRIANGLES_TOP;
            case "ld"  -> key = BannerPatterns.DIAGONAL_LEFT;
            case "rd"  -> key = BannerPatterns.DIAGONAL_UP_RIGHT;
            case "lud" -> key = BannerPatterns.DIAGONAL_UP_LEFT;
            case "rud" -> key = BannerPatterns.DIAGONAL_RIGHT;
            case "mc"  -> key = BannerPatterns.CIRCLE;
            case "mr"  -> key = BannerPatterns.RHOMBUS;
            case "vh"  -> key = BannerPatterns.HALF_VERTICAL;
            case "hh"  -> key = BannerPatterns.HALF_HORIZONTAL;
            case "vhr" -> key = BannerPatterns.HALF_VERTICAL_RIGHT;
            case "hhb" -> key = BannerPatterns.HALF_HORIZONTAL_BOTTOM;
            case "bo"  -> key = BannerPatterns.BORDER;
            case "cbo" -> key = BannerPatterns.CURLY_BORDER;
            case "gra" -> key = BannerPatterns.GRADIENT;
            case "gru" -> key = BannerPatterns.GRADIENT_UP;
            case "bri" -> key = BannerPatterns.BRICKS;
            case "glb" -> key = BannerPatterns.GLOBE;
            case "cre" -> key = BannerPatterns.CREEPER;
            case "sku" -> key = BannerPatterns.SKULL;
            case "flo" -> key = BannerPatterns.FLOWER;
            case "moj" -> key = BannerPatterns.MOJANG;
            case "pig" -> key = BannerPatterns.PIGLIN;
            default ->
            {
                Litematica.logger.warn("getBannerPatternEntryByIdPre1205(): invalid banner pattern of id {}", patternId);
                return null;
            }
        }
        
        return registryManager.get(RegistryKeys.BANNER_PATTERN).entryOf(key);
    }

    /**
     * In use by ItemUtils -> PickTE from Schematic World.
     */
    @Nullable
    public static ProfileComponent getSkullProfileFromProfile(NbtCompound profile)
    {
        UUID skullUUID = Util.NIL_UUID;
        String skullName = "";
        GameProfile skullProfile;

        if (profile.contains("id"))
        {
            skullUUID = profile.getUuid("id");
        }
        if (profile.contains("name"))
        {
            skullName = profile.getString("name");
        }
        Litematica.debugLog("getSkullProfileFromProfile(): try uuid: {} // name: {}", skullUUID.toString(), skullName);
        try
        {
            skullProfile = new GameProfile(skullUUID, skullName);
        }
        catch (Exception failure)
        {
            Litematica.logger.error("getSkullProfileFromProfile() failed to retrieve GameProfile");
            return null;
        }

        if (profile.contains("properties"))
        {
            NbtList propList = profile.getList("properties", 10);

            for (int i = 0; i < propList.size(); i++)
            {
                NbtCompound propNbt = propList.getCompound(i);

                String propValue = propNbt.getString("value");
                String propName = propNbt.getString("name");

                if (propNbt.contains("signature", 8))
                {
                    String propSignature = propNbt.getString("signature");

                    skullProfile.getProperties().put(propName, new Property(propName, propValue, propSignature));
                }
                else
                {
                    skullProfile.getProperties().put(propName, new Property(propName, propValue));
                }
            }
        }

        return new ProfileComponent(skullProfile);
    }

    /*
    @Nullable
    private ProfileComponent getSkullProfileFromNBT(NbtCompound skullOwner)
    {
        UUID uuid = Util.NIL_UUID;
        String name = "";
        NbtCompound properties;
        GameProfile skullProfile;

        // 24w09- "SkullOwner" Format
        if (skullOwner.contains("Id"))
        {
            uuid = skullOwner.getUuid("Id");
        }
        if (skullOwner.contains("Name"))
        {
            name = skullOwner.getString("Name");
        }
        if (!name.isEmpty())
        {
            try
            {
                skullProfile = new GameProfile(uuid, name);
                if (skullOwner.contains("Properties"))
                {
                    properties = skullOwner.getCompound("Properties");

                    for (String key : properties.getKeys())
                    {
                        NbtList propList = properties.getList(key, 10);

                        for (int i = 0; i < propList.size(); i++)
                        {
                            NbtCompound propNbt = propList.getCompound(i);

                            String value = propNbt.getString("Value");
                            if (propNbt.contains("Signature", 8))
                            {
                                skullProfile.getProperties().put(key, new Property(key, value, propNbt.getString("Signature")));
                            }
                            else
                            {
                                skullProfile.getProperties().put(key, new Property(key, value));
                            }
                        }
                    }
                }

                return new ProfileComponent(skullProfile);
            }
            catch (Exception failure)
            {
                Litematica.logger.error("getSkullProfileFromNBT() failed to retrieve GameProfile from pre-24w09a type data");
            }
        }
        else
        {
            Litematica.logger.warn("getSkullProfileFromNBT() failed to retrieve GameProfile from pre-24w09a type data (name or id is empty)");
        }

        return null;
    }
     */
}
