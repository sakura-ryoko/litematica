package fi.dy.masa.litematica.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.malilib.util.InventoryUtils;
import net.minecraft.block.entity.*;
import net.minecraft.component.*;
import net.minecraft.component.type.*;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This file is meant to be a new central place for managing Component <-> NBT data
 */
public class ComponentUtils
{
    public static final ComponentMap EMPTY = ComponentMap.EMPTY;
    public static final Pattern PATTERN_COMPONENT_BASE = Pattern.compile("^(?<name>(?:[a-z0-9\\._-]+:)[a-z0-9\\._-]+)$");

    /**
     * So far, these exist under 24w10a
     */
    public ComponentMap fromItemNBT(NbtCompound nbt, @Nonnull DynamicRegistryManager registryLookup)
    {
        ComponentMap.Builder compResult = ComponentMap.builder();

        // Standard Weapons / Armor types
        if (nbt.contains("AttributeModifiers", 9))
        {
            AttributeModifiersComponent attribs = getAttribModifiersFromNbt(nbt);
            compResult.add(DataComponentTypes.ATTRIBUTE_MODIFIERS, attribs);
            nbt.remove("AttributeModifiers");
        }
        // BlockEntityTag, pre 24w09a
        if (nbt.contains("BlockEntityTag"))
        {
            NbtCompound beTag = nbt.getCompound("BlockEntityTag");
            // Handle
            compResult.add(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(beTag));
            nbt.remove("BlockEntityTag");
        }

        return compResult.build();
    }

    /**
     * Execute this prior to "reading in" a BlockEntity's NBT tags, so that Vanilla can handle the rest.
     */
    public ComponentMap fromBlockEntityNBT(@Nonnull ItemStack stackIn, BlockEntity be, @Nonnull NbtCompound nbt, @Nonnull DynamicRegistryManager registryLookup)
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
            // FIXME RegistryLookup by Id
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

    private Sherds getSherdsFromNbt(NbtCompound nbt, DynamicRegistryManager registryLookup)
    {
        NbtList sherdList = nbt.getList("sherds", 8);
        Item backSherd = Items.BRICK;
        Item leftSherd = Items.BRICK;
        Item rightSherd = Items.BRICK;
        Item frontSherd = Items.BRICK;

        for (int i = 0; i < sherdList.size(); i++)
        {
            NbtElement sherd = sherdList.get(i);
            switch (i)
            {
                case 0:
                {
                    backSherd = Registries.ITEM.get(Identifier.tryParse(sherd.toString()));
                }
                case 1:
                {
                    leftSherd = Registries.ITEM.get(Identifier.tryParse(sherd.toString()));
                }
                case 2:
                {
                    rightSherd = Registries.ITEM.get(Identifier.tryParse(sherd.toString()));
                }
                case 3:
                {
                    frontSherd = Registries.ITEM.get(Identifier.tryParse(sherd.toString()));
                }
            }
        }
        return new Sherds(backSherd, leftSherd, rightSherd, frontSherd);
    }

    private List<BeehiveBlockEntity.BeeData> getBeeDataFromNbt(NbtCompound nbt, DynamicRegistryManager registryManager)
    {
        List<BeehiveBlockEntity.BeeData> beeList = new ArrayList<>();

        // BeeHiveBlock, pre 24w09a
        if (nbt.contains("Bees"))
        {
            NbtList beeNbtList = nbt.getList("Bees", 10);

            for (int i = 0; i < beeNbtList.size(); i++)
            {
                NbtCompound beeNbt = beeNbtList.getCompound(i);
                BeehiveBlockEntity.BeeData beeData;

                if (beeNbt.contains("EntityData"))
                {
                    NbtCompound beeEnt = beeNbt.getCompound("EntityData").copy();
                    int beeTicksInHive = beeNbt.getInt("TicksInHive");
                    int occupationTicks = beeNbt.getInt("MinOccupationTicks");

                    beeData = new BeehiveBlockEntity.BeeData(NbtComponent.of(beeEnt), beeTicksInHive, occupationTicks);
                    beeList.add(beeData);
                }
            }

            nbt.remove("Bees");
            if (!beeList.isEmpty())
            {
                return beeList;
            }
        }
        // BeeHiveBlock, post 24w10a
        else if (nbt.contains("bees"))
        {
            NbtList beeNbtList = nbt.getList("bees", 10);
            BeehiveBlockEntity.BeeData beeData;

            /*
            BeehiveBlockEntity.BeeData.LIST_CODEC.parse(NbtOps.INSTANCE, nbt.get("bees")).resultOrPartial((result) ->
            {
                // ignore
            }).ifPresent(beeList::addAll);
*/
            nbt.remove("bees");
            if (!beeList.isEmpty())
            {
                return beeList;
            }
        }

        return List.of();
    }

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

    private BannerPatternsComponent getBannerPatternsFromNBT(NbtCompound nbt, DynamicRegistryManager registryLookup)
    {
        List<BannerPatternsComponent.Layer> layerList = new ArrayList<>();
        BannerPatternsComponent patternsComp = BannerPatternsComponent.DEFAULT;

        // BannerItems, pre 24w09a
        if (nbt.contains("Patterns"))
        {
            NbtList patternList = nbt.getList("Patterns", 10);

            for (int i = 0; i < patternList.size() && i < 6; i++)
            {
                NbtCompound patternNbt = patternList.getCompound(i);
                DyeColor dyeColor = DyeColor.byId(patternNbt.getInt("Color"));
                String patternId = patternNbt.getString("Pattern");
                RegistryEntry<BannerPattern> patternReg = getBannerPatternEntryByIdPre1205(patternId, registryLookup);

                if (patternReg != null)
                {
                    BannerPatternsComponent.Layer patternLayer = new BannerPatternsComponent.Layer(patternReg, dyeColor);
                    layerList.add(patternLayer);
                }
            }
            if (!layerList.isEmpty())
            {
                patternsComp = new BannerPatternsComponent(layerList);
            }

            nbt.remove("Patterns");
        }
        // BannerItems, post 24w10a
        // --> they have some CODEC-based lookup method that fails to work here.
        else if (nbt.contains("patterns"))
        {
            NbtList patternList = nbt.getList("patterns", 10);
            for (int i = 0; i < patternList.size() && i < 6; i++)
            {
                NbtCompound patternNbt = patternList.getCompound(i);
                DyeColor dyeColor = DyeColor.byName(patternNbt.getString("color"), DyeColor.WHITE);
                String patternId = patternNbt.getString("pattern");
                RegistryEntry<BannerPattern> patternReg = getBannerPatternEntryByIdPost1205(patternId, registryLookup);

                if (patternReg != null)
                {
                    BannerPatternsComponent.Layer patternLayer = new BannerPatternsComponent.Layer(patternReg, dyeColor);
                    layerList.add(patternLayer);
                }
            }
            if (!layerList.isEmpty())
            {
                patternsComp = new BannerPatternsComponent(layerList);
            }

            nbt.remove("patterns");
        }

        return patternsComp;
    }

    /**
     * I attempted to use the Mojang method first, but it wasn't working.  Iterator it.
     */
    private RegistryEntry<BannerPattern> getBannerPatternEntryByIdPost1205(String patternId, DynamicRegistryManager registryLookup)
    {
        Iterator<BannerPattern> iter = registryLookup.get(RegistryKeys.BANNER_PATTERN).stream().iterator();
        Identifier id = Identifier.tryParse(patternId);

        while (iter.hasNext())
        {
            BannerPattern pattern = iter.next();
            if (pattern.assetId().equals(id))
            {
                return RegistryEntry.of(pattern);
            }
        }

        Litematica.logger.error("getBannerPatternEntryByIdPost1205(): invalid banner pattern of id {}", patternId);
        return null;
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

    private ProfileComponent getSkullProfileFromString(String skullString)
    {
        GameProfile skullProfile = new GameProfile(Util.NIL_UUID, skullString);
        return new ProfileComponent(skullProfile);
    }

    /**
     * I would assume there is a better way to do this, but I couldn't find it
     */
    private AttributeModifierSlot getAttribModSlot(String attribSlot)
    {
        if (AttributeModifierSlot.ANY.asString().equals(attribSlot))
        {
            return AttributeModifierSlot.ANY;
        }
        else if (AttributeModifierSlot.MAINHAND.asString().equals(attribSlot))
        {
            return AttributeModifierSlot.MAINHAND;
        }
        else if (AttributeModifierSlot.OFFHAND.asString().equals(attribSlot))
        {
            return AttributeModifierSlot.OFFHAND;
        }
        else if (AttributeModifierSlot.HAND.asString().equals(attribSlot))
        {
            return AttributeModifierSlot.HAND;
        }
        else if (AttributeModifierSlot.FEET.asString().equals(attribSlot))
        {
            return AttributeModifierSlot.FEET;
        }
        else if (AttributeModifierSlot.LEGS.asString().equals(attribSlot))
        {
            return AttributeModifierSlot.LEGS;
        }
        else if (AttributeModifierSlot.CHEST.asString().equals(attribSlot))
        {
            return AttributeModifierSlot.CHEST;
        }
        else if (AttributeModifierSlot.HEAD.asString().equals(attribSlot))
        {
            return AttributeModifierSlot.HEAD;
        }
        else if (AttributeModifierSlot.ARMOR.asString().equals(attribSlot))
        {
            return AttributeModifierSlot.ARMOR;
        }
        else
        {
            return AttributeModifierSlot.ANY;
        }
    }

    public AttributeModifiersComponent getAttribModifiersFromNbt(NbtCompound nbt)
    {
        if (nbt == null || nbt.isEmpty())
            return null;

        AttributeModifiersComponent.Builder attribs = AttributeModifiersComponent.builder();
        NbtList attribList = nbt.getList("AttributeModifiers", 10);

        for (int i = 0; i < attribList.size(); i++)
        {
            NbtCompound attribNbt = attribList.getCompound(i);
            String attribSlot = "";
            String attribName;

            if (attribNbt.contains("Slot"))
            {
                attribSlot = attribNbt.getString("Slot");
            }
            else if (attribNbt.contains("slot"))
            {
                attribSlot = attribNbt.getString("slot");
            }
            attribName = attribNbt.getString("AttributeName");

            if (!attribName.isEmpty() && !attribSlot.isEmpty())
            {
                Optional<EntityAttribute> attribValue = Registries.ATTRIBUTE.getOrEmpty(Identifier.tryParse(attribName));

                if (attribValue.isPresent())
                {
                    EntityAttributeModifier entAttribMod = EntityAttributeModifier.fromNbt(attribNbt);

                    if (entAttribMod != null)
                    {
                        AttributeModifierSlot attribModSlot = getAttribModSlot(attribSlot);
                        RegistryEntry<EntityAttribute> newAttribValue = RegistryEntry.of(attribValue.get());

                        attribs.add(newAttribValue, entAttribMod, attribModSlot);
                    }
                }
            }
        }

        return attribs.build();
    }

    public DefaultedList<ItemStack> getItemStackListFromNbt(NbtList itemList, DynamicRegistryManager registryManager)
    {
        DefaultedList<ItemStack> itemInv = DefaultedList.ofSize(itemList.size(), ItemStack.EMPTY);

        for (int i = 0; i < itemList.size(); i++)
        {
            NbtCompound itemNbt = itemList.getCompound(i);
            ProfileComponent skullProfile = null;

            int itemSlot = itemNbt.getByte("Slot") & 255;
            if (itemSlot >= 0 && itemSlot < itemInv.size())
            {
                byte itemCount = itemNbt.getByte("Count");

                // Pre 1.20.5 Inventory.fromNbt() has a "tag" option.
                if (itemNbt.contains("tag"))
                {
                    NbtCompound itemTag = itemNbt.getCompound("tag").copy();

                    // The only "Item" class that uses this is PlayerHeadItem.
                    if (itemTag.contains("SkullOwner", 10))
                    {
                        // Has GameProfile
                        NbtCompound skullOwner = itemTag.getCompound("SkullOwner");
                        skullProfile = getSkullProfileFromNBT(skullOwner);
                    }
                    else if (itemTag.contains("SkullOwner", 8))
                    {
                        // Doesn't have GameProfile
                        String skullString = itemTag.getString("SkullOwner");
                        skullProfile = getSkullProfileFromString(skullString);
                    }
                    else if (itemTag.contains("ExtraType", 8))
                    {
                        String skullUUID = itemTag.getString("ExtraType");
                        skullProfile = getSkullProfileFromString(skullUUID);
                    }
                    else if (itemTag.contains("profile"))
                    {
                        // Post 24w09a Component Profile
                        NbtCompound skullOwner = itemTag.getCompound("profile");
                        skullProfile = getSkullProfileFromProfile(skullOwner);
                    }

                    itemNbt.remove("tag");
                }
                if (itemNbt.contains("id") && !itemNbt.contains("components"))
                {
                    String itemId = itemNbt.getString("id");
                    ItemStack stack;

                    if (itemCount > 0)
                    {
                        stack = InventoryUtils.getItemStackFromString(itemId, itemCount);
                    }
                    else
                    {
                        stack = InventoryUtils.getItemStackFromString(itemId);
                    }

                    if (skullProfile != null)
                    {
                        ComponentMap.Builder components = ComponentMap.builder();
                        components.add(DataComponentTypes.PROFILE, skullProfile);

                        stack.applyComponentsFrom(components.build());
                    }

                    itemInv.add(stack);
                }
                else if (itemNbt.contains("id"))
                {
                    // Let vanilla read the "components" tags for us.
                    ItemStack stack = ItemStack.fromNbtOrEmpty(registryManager, itemNbt.copy());

                    if (!stack.equals(ItemStack.EMPTY))
                    {
                        if (skullProfile != null)
                        {
                            ComponentMap.Builder components = ComponentMap.builder();
                            components.add(DataComponentTypes.PROFILE, skullProfile);

                            stack.applyComponentsFrom(components.build());
                        }

                        itemInv.add(stack);
                    }
                }
                else
                {
                    Litematica.logger.error("fromBlockEntityNBT() received invalid Inventory data, \"id\" is missing for slot: {}", itemSlot);
                }
            }
        }

        return itemInv;
    }

    private ItemStack getItemStackFromNbt(NbtCompound itemNbt, DynamicRegistryManager registryManager)
    {
        ItemStack stack;
        ProfileComponent skullProfile = null;

        byte itemCount = itemNbt.getByte("Count");

        // Pre 1.20.5 Inventory.fromNbt() has a "tag" option.
        if (itemNbt.contains("tag"))
        {
            NbtCompound itemTag = itemNbt.getCompound("tag").copy();

            // The only "Item" class that uses this is PlayerHeadItem.
            if (itemTag.contains("SkullOwner", 10))
            {
                // Has GameProfile
                NbtCompound skullOwner = itemTag.getCompound("SkullOwner");
                skullProfile = getSkullProfileFromNBT(skullOwner);
            }
            else if (itemTag.contains("SkullOwner", 8))
            {
                // Doesn't have GameProfile
                String skullString = itemTag.getString("SkullOwner");
                skullProfile = getSkullProfileFromString(skullString);
            }
            else if (itemTag.contains("ExtraType", 8))
            {
                String skullUUID = itemTag.getString("ExtraType");
                skullProfile = getSkullProfileFromString(skullUUID);
            }
            else if (itemTag.contains("profile"))
            {
                // Post 24w09a Component Profile
                NbtCompound skullOwner = itemTag.getCompound("profile");
                skullProfile = getSkullProfileFromProfile(skullOwner);
            }

            itemNbt.remove("tag");
        }
        if (itemNbt.contains("id") && !itemNbt.contains("components"))
        {
            String itemId = itemNbt.getString("id");

            if (itemCount > 0)
            {
                stack = InventoryUtils.getItemStackFromString(itemId, itemCount);
            }
            else
            {
                stack = InventoryUtils.getItemStackFromString(itemId);
            }

            if (skullProfile != null)
            {
                ComponentMap.Builder components = ComponentMap.builder();
                components.add(DataComponentTypes.PROFILE, skullProfile);

                stack.applyComponentsFrom(components.build());
            }

            return stack;
        }
        else if (itemNbt.contains("id"))
        {
            // Let vanilla read the "components" tags for us.
            stack = ItemStack.fromNbtOrEmpty(registryManager, itemNbt.copy());

            if (!stack.equals(ItemStack.EMPTY))
            {
                if (skullProfile != null)
                {
                    ComponentMap.Builder components = ComponentMap.builder();
                    components.add(DataComponentTypes.PROFILE, skullProfile);

                    stack.applyComponentsFrom(components.build());
                }

                return stack;
            }
        }

        Litematica.logger.error("getItemStackFromNbt() received invalid Inventory data, \"id\" is missing");
        return ItemStack.EMPTY;
    }
}
