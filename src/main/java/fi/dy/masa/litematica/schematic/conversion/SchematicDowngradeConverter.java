package fi.dy.masa.litematica.schematic.conversion;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.DropChances;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.DataBlockUtils;
import fi.dy.masa.malilib.util.data.tag.*;
import fi.dy.masa.malilib.util.data.tag.util.DataOps;
import fi.dy.masa.malilib.util.data.tag.util.DataTypeUtils;
import fi.dy.masa.litematica.Litematica;

public class SchematicDowngradeConverter
{
//    private static final AnsiLogger LOGGER = new AnsiLogger(SchematicDowngradeConverter.class, true, true);

    public static CompoundData downgradeEntity_to_1_20_4(CompoundData oldEntity, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData newEntity = new CompoundData();

        if (!oldEntity.contains("id", Constants.NBT.TAG_STRING))
        {
            return oldEntity;
        }
        for (String key : oldEntity.getKeys())
        {
            switch (key)
            {
                case "x" -> newEntity.putInt("x", oldEntity.getIntOrDefault("x", 0));
                case "y" -> newEntity.putInt("y", oldEntity.getIntOrDefault("y", 0));
                case "z" -> newEntity.putInt("z", oldEntity.getIntOrDefault("z", 0));
                case "id" -> newEntity.putString("id", oldEntity.getStringOrDefault("id", ""));
                case "attributes" -> newEntity.put("Attributes", processAttributes(oldEntity.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "flower_pos" -> newEntity.put("FlowerPos", processFlowerPos(oldEntity, key, minecraftDataVersion, registry));
                case "hive_pos" -> newEntity.put("HivePos", processFlowerPos(oldEntity, key, minecraftDataVersion, registry));
                case "ArmorItems" -> newEntity.put("ArmorItems", processEntityItems(oldEntity.getList(key), minecraftDataVersion, registry, 4));
                case "HandItems" -> newEntity.put("HandItems", processEntityItems(oldEntity.getList(key), minecraftDataVersion, registry, 2));
                case "Item" -> newEntity.put("Item", processEntityItem(oldEntity.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "Inventory" -> newEntity.put("Inventory", processEntityItems(oldEntity.getList(key), minecraftDataVersion, registry, 1));
                // 1.21.5+ tags
                case "equipment" -> newEntity.combine(processEntityEquipment(oldEntity.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "drop_chances" -> newEntity.combine(processEntityDropChances(oldEntity.getData(key).orElse(new CompoundData())));
                case "fall_distance" -> newEntity.putFloat("FallDistance", oldEntity.getFloatOrDefault(key, 0f));
                // NbtUtils.readBlockPosFromArrayTag() // get(key, BlockPos.CODEC).orElse(null)
                case "anchor_pos" -> processBlockPosTag(DataTypeUtils.readBlockPosFromArrayTag(oldEntity, key), "A", newEntity);
                case "block_pos" -> processBlockPosTag(DataTypeUtils.readBlockPosFromArrayTag(oldEntity, key), "Tile", newEntity);
                case "bound_pos" -> processBlockPosTag(DataTypeUtils.readBlockPosFromArrayTag(oldEntity, key), "Bound", newEntity);
                case "home_pos" -> processBlockPosTag(DataTypeUtils.readBlockPosFromArrayTag(oldEntity, key), "HomePos", newEntity);
                case "sleeping_pos" -> processBlockPosTag(DataTypeUtils.readBlockPosFromArrayTag(oldEntity, key), "Sleeping", newEntity);
                case "has_egg" -> newEntity.putBoolean("HasEgg", oldEntity.getBooleanOrDefault(key, false));
                case "life_ticks" -> newEntity.putInt("LifeTicks", oldEntity.getIntOrDefault(key, 0));
                case "size" -> newEntity.putInt("Size", oldEntity.getIntOrDefault(key, 0));
                default -> newEntity.put(key, oldEntity.getData(key).orElse(new CompoundData()));
            }
        }

        return newEntity;
    }

    private static void processBlockPosTag(@Nullable BlockPos oldPos, String prefix, CompoundData newTags)
    {
        if (oldPos != null)
        {
            newTags.putInt(prefix+"X", oldPos.getX());
            newTags.putInt(prefix+"Y", oldPos.getY());
            newTags.putInt(prefix+"Z", oldPos.getZ());
        }
    }

    private static CompoundData processEntityDropChances(BaseData nbtElement)
    {
        CompoundData oldTags = nbtElement.asCompound().orElse(new CompoundData());
//        CompoundData oldTags = nbtElement.getType() == Constants.NBT.TAG_COMPOUND ? (CompoundData) nbtElement : new CompoundData();
        CompoundData newTags = new CompoundData();
        ListData handDrops = new ListData();
        ListData armorDrops = new ListData();

        for (int i = 0; i < 2; i++)
        {
            handDrops.add(new FloatData(DropChances.DEFAULT_EQUIPMENT_DROP_CHANCE));
        }

        for (int i = 0; i < 4; i++)
        {
            armorDrops.add(new FloatData(DropChances.DEFAULT_EQUIPMENT_DROP_CHANCE));
        }

        for (String key : oldTags.getKeys())
        {
            switch (key)
            {
                case "mainhand" -> handDrops.set(0, oldTags.getData(key).orElse(new CompoundData()));
                case "offhand" -> handDrops.set(1, oldTags.getData(key).orElse(new CompoundData()));
                case "feet" -> armorDrops.set(0, oldTags.getData(key).orElse(new CompoundData()));
                case "legs" -> armorDrops.set(1, oldTags.getData(key).orElse(new CompoundData()));
                case "chest" -> armorDrops.set(2, oldTags.getData(key).orElse(new CompoundData()));
                case "head" -> armorDrops.set(3, oldTags.getData(key).orElse(new CompoundData()));
                // Not used
                //case "body" -> newTags.put("body_armor_drop_chance", oldTags.getData(key).orElse(new CompoundData()));
                //case "saddle" -> newTags.put("SaddleItem", oldTags.getData(key).orElse(new CompoundData()));
                default -> {}
            }
        }

        newTags.put("HandDropChances", handDrops);
        newTags.put("ArmorDropChances", armorDrops);

        return newTags;
    }

    private static CompoundData processEntityEquipment(BaseData equipmentEntries, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData oldTags = equipmentEntries.asCompound().orElse(new CompoundData());
        CompoundData newTags = new CompoundData();
        ListData newHandItems = new ListData();
        ListData newArmorItems = new ListData();

        for (int i = 0; i < 2; i++)
        {
            newHandItems.add(new CompoundData());
        }

        for (int i = 0; i < 4; i++)
        {
            newArmorItems.add(new CompoundData());
        }

        for (String key : oldTags.getKeys())
        {
            switch (key)
            {
                case "mainhand" -> newHandItems.set(0, processEntityItem(oldTags.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "offhand" -> newHandItems.set(1, processEntityItem(oldTags.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "feet" -> newArmorItems.set(0, processEntityItem(oldTags.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "legs" -> newArmorItems.set(1, processEntityItem(oldTags.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "chest" -> newArmorItems.set(2, processEntityItem(oldTags.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "head" -> newArmorItems.set(3, processEntityItem(oldTags.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "body" ->
                {
                    // Why is this duplicated in 1.20.4?  the world may never know...
                    BaseData ele = processEntityItem(oldTags.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry);
                    newArmorItems.set(2, ele);
                    newTags.put("ArmorItem", ele);
                }
                case "saddle" -> newTags.put("SaddleItem", processEntityItem(oldTags.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                default -> {}
            }
        }

        newTags.put("HandItems", newHandItems);
        newTags.put("ArmorItems", newArmorItems);

        return newTags;
    }

    private static BaseData processEntityItem(BaseData itemEntry, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData oldItem = itemEntry.asCompound().orElse(new CompoundData());
        CompoundData newItem = new CompoundData();

        if (!oldItem.contains("id", Constants.NBT.TAG_STRING))
        {
            return itemEntry;
        }
        String idName = oldItem.getStringOrDefault("id", "");
        newItem.putString("id", idName);
        if (oldItem.contains("count", Constants.NBT.TAG_INT))
        {
            newItem.putByte("Count", (byte) oldItem.getIntOrDefault("count", 1));
        }
        if (oldItem.contains("components", Constants.NBT.TAG_COMPOUND))
        {
            newItem.put("tag", processComponentsTag(oldItem.getCompound("components"), idName, minecraftDataVersion, registry));
        }
        else
        {
            if (needsDamageTag(idName))
            {
                CompoundData newTag = new CompoundData();
                newTag.putInt("Damage", 0);
                newItem.put("tag", newTag);
            }
        }

        return newItem;
    }

    private static ListData processEntityItems(ListData oldItems, int minecraftDataVersion, RegistryAccess registry, int expectedSize)
    {
        ListData newItems = new ListData();

        for (int i = 0; i < oldItems.size(); i++)
        {
            CompoundData itemEntry = oldItems.getCompoundAt(i);
            CompoundData newEntry = new CompoundData();

            if (itemEntry.contains("id", Constants.NBT.TAG_STRING))
            {
                String idName = itemEntry.getStringOrDefault("id", "");
                newEntry.putString("id", idName);

                if (itemEntry.contains("count", Constants.NBT.TAG_INT))
                {
                    newEntry.putByte("Count", (byte) itemEntry.getIntOrDefault("count", 1));
                }
                else
                {
                    newEntry.putByte("Count", (byte) 1);
                }
                if (itemEntry.contains("components", Constants.NBT.TAG_COMPOUND))
                {
                    newEntry.put("tag", processComponentsTag(itemEntry.getCompound("components"), idName, minecraftDataVersion, registry));
                }
                else
                {
                    if (needsDamageTag(idName))
                    {
                        CompoundData newTag = new CompoundData();
                        newTag.putInt("Damage", 0);
                        newEntry.put("tag", newTag);
                    }
                }
            }

            newItems.add(newEntry);
        }

        if (newItems.size() < expectedSize)
        {
            int addTotal = expectedSize - newItems.size();

            for (int i = 0; i < addTotal; i++)
            {
                newItems.add(i, new CompoundData());
            }
        }

        return newItems;
    }

    private static BaseData processAttributes(BaseData attrib, int minecraftDataVersion, RegistryAccess registry)
    {
        ListData oldAttr = attrib.asList().orElse(new ListData());

        if (oldAttr.isEmpty())
        {
            CompoundData oldTag = attrib.asCompound().orElse(new CompoundData());

            if (oldTag.isEmpty()) return attrib;

            for (String key : oldTag.getKeys())
            {
                if (key.equals("modifiers"))
                {
                    return processAttributeModifiers(oldTag.getList(key), minecraftDataVersion, registry);
                }
            }
        }

        return processAttributeBase(oldAttr, minecraftDataVersion, registry);
    }

    private static BaseData processAttributeBase(ListData oldAttr, int minecraftDataVersion, RegistryAccess registry)
    {
        ListData newAttr = new ListData();

        for (int i = 0; i < oldAttr.size(); i++)
        {
            CompoundData attrEntry = oldAttr.getCompoundAt(i);
            CompoundData newEntry = new CompoundData();

            if (attrEntry.contains("type", Constants.NBT.TAG_STRING))
            {
                newEntry.putString("Name", attributeRename(attrEntry.getStringOrDefault("type", "")));
                newEntry.putDouble("Base", attrEntry.getDoubleOrDefault("amount", 0D));
            }
            else
            {
                newEntry.putString("Name", attributeRename(attrEntry.getStringOrDefault("id", "")));
                newEntry.putDouble("Base", attrEntry.getDoubleOrDefault("base", 0D));
            }

            ListData listEntry = attrEntry.getList("modifiers");
            ListData newMods = processAttributeModifiers(listEntry, minecraftDataVersion, registry);

            if (!newMods.isEmpty())
            {
                newEntry.put("Modifiers", newMods);
            }
            newAttr.add(newEntry);
        }

        return newAttr;
    }

    private static ListData processAttributeModifiers(ListData modifiers, int minecraftDataVersion, RegistryAccess registry)
    {
        ListData newMods = new ListData();

        if (modifiers.isEmpty()) return modifiers;

        for (int y = 0; y < modifiers.size(); y++)
        {
            CompoundData modEntry = modifiers.getCompoundAt(y);
            CompoundData newMod = new CompoundData();

            if (modEntry.contains("type", Constants.NBT.TAG_STRING))
            {
                newMod.putString("Name", attributeRename(modEntry.getStringOrDefault("type", "")));
                newMod.putDouble("Base", modEntry.getDoubleOrDefault("amount", 0D));
            }
            else
            {
                newMod.putDouble("Amount", modEntry.getDoubleOrDefault("amount", 0D));
                newMod.putString("Name", modifierIdToName(modEntry.getStringOrDefault("id", "")));
            }

            newMod.putInt("Operation", modifierOperationToInt(modEntry.getStringOrDefault("operation", "")));
            //newMod.putUuid("UUID", modEntry.contains("UUID") ? modEntry.getUuid("UUID") : UUID.randomUUID());
            newMod.putCodec("UUID", UUIDUtil.AUTHLIB_CODEC, modEntry.getCodec("UUID", UUIDUtil.AUTHLIB_CODEC, registry.createSerializationContext(DataOps.INSTANCE)).orElse(UUID.randomUUID()));
            newMods.add(newMod);
        }

        return newMods;
    }

    private static String attributeRename(String idIn)
    {
        switch (idIn)
        {
            case "minecraft:armor" ->
            {
                return "minecraft:generic.armor";
            }
            case "minecraft:armor_toughness" ->
            {
                return "minecraft:generic.armor_toughness";
            }
            case "minecraft:attack_damage" ->
            {
                return "minecraft:generic.attack_damage";
            }
            case "minecraft:attack_knockback" ->
            {
                return "minecraft:generic.attack_knockback";
            }
            case "minecraft:attack_speed" ->
            {
                return "minecraft:generic.attack_speed";
            }
            case "minecraft:flying_speed" ->
            {
                return "minecraft:generic.flying_speed";
            }
            case "minecraft:follow_range" ->
            {
                return "minecraft:generic.follow_range";
            }
            case "minecraft:jump_strength" ->
            {
                return "minecraft:horse.jump_strength";
                // return "minecraft:generic.jump_strength"; --> (1.20.6 / 1.21 only)
            }
            case "minecraft:knockback_resistance" ->
            {
                return "minecraft:generic.knockback_resistance";
            }
            case "minecraft:luck" ->
            {
                return "minecraft:generic.luck";
            }
            case "minecraft:max_absorption" ->
            {
                return "minecraft:generic.max_absorption";
            }
            case "minecraft:max_health" ->
            {
                return "minecraft:generic.max_health";
            }
            case "minecraft:movement_speed" ->
            {
                return "minecraft:generic.movement_speed";
            }
            case "minecraft:spawn_reinforcements" ->
            {
                return "minecraft:zombie.spawn_reinforcements";
            }

            // tempt_range --> No match
            // These don't exist in 1.20.4 (1.20.6 / 1.21 only)
            case "minecraft:block_break_speed" ->
            {
                return "minecraft:player.block_break_speed";
            }
            case "minecraft:block_interaction_range" ->
            {
                return "minecraft:player.block_interaction_range";
            }
            case "minecraft:burning_time" ->
            {
                return "minecraft:generic.burning_time";
            }
            case "minecraft:explosion_knockback_resistance" ->
            {
                return "minecraft:generic.explosion_knockback_resistance";
            }
            case "minecraft:entity_interaction_range" ->
            {
                return "minecraft:player.entity_interaction_range";
            }
            case "minecraft:fall_damage_multiplier" ->
            {
                return "minecraft:generic.fall_damage_multiplier";
            }
            case "minecraft:gravity" ->
            {
                return "minecraft:generic.gravity";
            }
            case "minecraft:mining_efficiency" ->
            {
                return "minecraft:player.mining_efficiency";
            }
            case "minecraft:movement_efficiency" ->
            {
                return "minecraft:generic.movement_efficiency";
            }
            case "minecraft:oxygen_bonus" ->
            {
                return "minecraft:generic.oxygen_bonus";
            }
            case "minecraft:safe_fall_distance" ->
            {
                return "minecraft:generic.safe_fall_distance";
            }
            case "minecraft:scale" ->
            {
                return "minecraft:generic.scale";
            }
            case "minecraft:sneaking_speed" ->
            {
                return "minecraft:player.sneaking_speed";
            }
            case "minecraft:step_height" ->
            {
                return "minecraft:generic.step_height";
            }
            case "minecraft:submerged_mining_speed" ->
            {
                return "minecraft:player.submerged_mining_speed";
            }
            case "minecraft:sweeping_damage_ratio" ->
            {
                return "minecraft:player.sweeping_damage_ratio";
            }
            case "minecraft:water_movement_efficiency" ->
            {
                return "minecraft:generic.water_movement_efficiency";
            }
        }

        return idIn;
    }

    private static String modifierIdToName(String idIn)
    {
        if (idIn.equals("minecraft:random_spawn_bonus"))
        {
            return "Random spawn bonus";
        }

        return "";
    }

    private static int modifierOperationToInt(String op)
    {
        switch (op)
        {
            case "add_value" ->
            {
                return 0;
            }
            case "add_multiplied_base" ->
            {
                return 1;
            }
            case "add_multiplied_total" ->
            {
                return 2;
            }
        }

        return 0;
    }

    public static CompoundData downgradeBlockEntity_to_1_20_4(CompoundData oldTE, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData newTE = new CompoundData();

        if (!oldTE.contains("id", Constants.NBT.TAG_STRING))
        {
            oldTE.combine(SchematicConversionMaps.checkForIdTag(oldTE));
        }
        for (String key : oldTE.getKeys())
        {
            switch (key)
            {
                case "x" -> newTE.putInt("x", oldTE.getIntOrDefault("x", 0));
                case "y" -> newTE.putInt("y", oldTE.getIntOrDefault("y", 0));
                case "z" -> newTE.putInt("z", oldTE.getIntOrDefault("z", 0));
                case "id" -> newTE.putString("id", oldTE.getStringOrDefault("id", ""));
                case "Items" -> newTE.put("Items", processItemsTag(oldTE.getList("Items"), minecraftDataVersion, registry));
                case "patterns" -> newTE.put("Patterns", processBannerPatterns(oldTE.getData(key).orElse(new CompoundData())));
                case "profile" -> newTE.put("SkullOwner", processSkullProfile(oldTE.getData(key).orElse(new CompoundData()), newTE, minecraftDataVersion, registry));
                case "flower_pos" -> newTE.put("FlowerPos", processFlowerPos(oldTE, key, minecraftDataVersion, registry));
                case "bees" -> newTE.put("Bees", processBeesTag(oldTE.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "item" -> newTE.put("item", processDecoratedPot(oldTE.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "last_interacted_slot" -> newTE.put("last_interacted_slot", oldTE.getData(key).orElse(new CompoundData()));
                case "ticks_since_song_started" ->
                {
                    newTE.putLong("RecordStartTick", 0L);
                    newTE.putLong("TickCount", oldTE.getLongOrDefault(key, 0L));
                    newTE.putByte("IsPlaying", (byte) 0);
                }
                case "RecordItem" -> newTE.put("RecordItem", processRecordItem(oldTE.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "Book" -> newTE.put("Book", processBookTag(oldTE.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                // 1.21.5+
                //case "RecipesUsed" -> newTE.put("RecipesUsed", processRecipesUsedTag(oldTE));
                case "CustomName" -> newTE.putString("CustomName", processCustomNameTag(oldTE, key, registry));
                case "custom_name" -> newTE.putString("CustomName", processCustomNameTag(oldTE, key, registry));
                default -> newTE.put(key, oldTE.getData(key).orElse(new CompoundData()));
            }
        }

        return newTE;
    }

    // 1.21.5+ Only ?  Might not even be needed
    private static CompoundData processRecipesUsedTag(BaseData nbtIn, @Nonnull RegistryAccess registry)
    {
        CompoundData oldNbt = nbtIn.asCompound().orElse(new CompoundData());
        CompoundData newNbt = new CompoundData();
        Codec<Map<ResourceKey<Recipe<?>>, Integer>> CODEC = Codec.unboundedMap(Recipe.KEY_CODEC, Codec.INT);
        Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> recipesUsed = new Reference2IntOpenHashMap<>();

        // todo -- make sure this even needed
        recipesUsed.putAll(oldNbt.getCodec("RecipesUsed", CODEC, registry.createSerializationContext(DataOps.INSTANCE)).orElse(Map.of()));
        recipesUsed.forEach((id, count) ->
        {
            newNbt.putInt(id.identifier().toString(), count);
        });

        return newNbt;
    }

    private static ListData processItemsTag(ListData oldItems, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        ListData newItems = new ListData();

        for (int i = 0; i < oldItems.size(); i++)
        {
            CompoundData itemEntry = oldItems.getCompoundAt(i);
            CompoundData newEntry = new CompoundData();

            if (!itemEntry.contains("id", Constants.NBT.TAG_STRING))
            {
                continue;
            }
            String idName = itemEntry.getStringOrDefault("id", "");

            newEntry.putString("id", idName);
            if (itemEntry.contains("count", Constants.NBT.TAG_INT))
            {
                newEntry.putByte("Count", (byte) itemEntry.getIntOrDefault("count", 1));
            }
            if (itemEntry.contains("Slot", Constants.NBT.TAG_BYTE))
            {
                newEntry.putByte("Slot", itemEntry.getByteOrDefault("Slot", (byte) 1));
            }
            if (itemEntry.contains("components", Constants.NBT.TAG_COMPOUND))
            {
                newEntry.put("tag", processComponentsTag(itemEntry.getCompound("components"), idName, minecraftDataVersion, registry));
            }
            else
            {
                if (needsDamageTag(idName))
                {
                    CompoundData newTag = new CompoundData();
                    newTag.putInt("Damage", 0);
                    newEntry.put("tag", newTag);
                }
            }

            newItems.add(newEntry);
        }

        return newItems;
    }

    private static ListData processItemsTag_Nested(ListData oldItems, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        ListData newItems = new ListData();

        for (int i = 0; i < oldItems.size(); i++)
        {
            CompoundData itemEntry = oldItems.getCompoundAt(i);
            CompoundData newEntry = new CompoundData();

            int slotNum = itemEntry.getIntOrDefault("slot", 0);
            CompoundData itemSlot = itemEntry.getCompound("item");

            if (!itemSlot.contains("id", Constants.NBT.TAG_STRING))
            {
                continue;
            }
            String idName = itemSlot.getStringOrDefault("id", "");

            newEntry.putString("id", idName);
            if (itemSlot.contains("count", Constants.NBT.TAG_INT))
            {
                newEntry.putByte("Count", (byte) itemSlot.getIntOrDefault("count", 1));
            }
            newEntry.putByte("Slot", (byte) slotNum);

            if (itemSlot.contains("components", Constants.NBT.TAG_COMPOUND))
            {
                newEntry.put("tag", processComponentsTag(itemSlot.getCompound("components"), idName, minecraftDataVersion, registry));
            }
            else
            {
                if (needsDamageTag(idName))
                {
                    CompoundData newTag = new CompoundData();
                    newTag.putInt("Damage", 0);
                    newEntry.put("tag", newTag);
                }
            }

            newItems.add(newEntry);
        }

        return newItems;
    }

    private static CompoundData processDecoratedPot_Nested(ListData oldItems, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData itemEntry = oldItems.getCompoundAt(0);
        CompoundData newEntry = new CompoundData();

        int slotNum = itemEntry.getIntOrDefault("slot", 1);
        CompoundData itemSlot = itemEntry.getCompound("item");

        if (!itemSlot.contains("id", Constants.NBT.TAG_STRING))
        {
            return itemEntry;
        }
        String idName = itemSlot.getStringOrDefault("id", "");
        newEntry.putString("id", idName);
        newEntry.putByte("Count", (byte) (itemSlot.contains("count", Constants.NBT.TAG_INT) ? itemSlot.getInt("count") : 1));

        if (itemSlot.contains("components", Constants.NBT.TAG_COMPOUND))
        {
            newEntry.put("tag", processComponentsTag(itemSlot.getCompound("components"), idName, minecraftDataVersion, registry));
        }
        else
        {
            if (needsDamageTag(idName))
            {
                CompoundData newTag = new CompoundData();
                newTag.putInt("Damage", 0);
                newEntry.put("tag", newTag);
            }
        }

        return newEntry;
    }

    private static boolean needsDamageTag(String id)
    {
        ItemStack stack = InventoryUtils.getItemStackFromString(id);

        return stack != null && !stack.isEmpty() && stack.isDamageableItem();
    }

    private static CompoundData processComponentsTag(CompoundData nbt, String itemId, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData outNbt = new CompoundData();
        CompoundData beNbt = new CompoundData();
        CompoundData dispNbt = new CompoundData();
        boolean needsDamage = needsDamageTag(itemId);

        for (String key : nbt.getKeys())
        {
            switch (key)
            {
                case "minecraft:attribute_modifiers" -> outNbt.put("AttributeModifiers", processAttributes(nbt.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "minecraft:banner_patterns" ->
                {
                    beNbt.put("Patterns", processBannerPatterns(nbt.getData(key).orElse(new CompoundData())));
                    beNbt.putString("id", "minecraft:banner");
                }
                case "minecraft:bees" ->
                {
                    beNbt.put("Bees", processBeesTag(nbt.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                    beNbt.putString("id", itemId);
                }
                case "minecraft:block_state" -> outNbt.put("BlockStateTag", processBlockState(nbt.getData(key).orElse(new CompoundData())));
                case "minecraft:block_entity_data" -> processBlockEntityData(nbt.getData(key).orElse(new CompoundData()), beNbt, minecraftDataVersion, registry);       // TODO --> check that this works or not
                case "minecraft:bucket_entity_data" -> processBucketEntityData(nbt.getData(key).orElse(new CompoundData()), beNbt, minecraftDataVersion, registry);
                case "minecraft:bundle_contents" -> outNbt.put("Items", processItemsTag(nbt.getList(key), minecraftDataVersion, registry));
                case "minecraft:can_break" -> outNbt.put("CanDestroy", nbt.getData(key).orElse(new CompoundData()));
                case "minecraft:can_place_on" -> outNbt.put("CanPlaceOn", nbt.getData(key).orElse(new CompoundData()));
                case "minecraft:container" ->
                {
                    if (itemId.contains("decorated_pot"))
                    {
                        beNbt.put("item", processDecoratedPot_Nested(nbt.getList(key), minecraftDataVersion, registry));
                    }
                    else
                    {
                        beNbt.put("Items", processItemsTag_Nested(nbt.getList(key), minecraftDataVersion, registry));
                    }
                    if (itemId.contains("shulker"))
                    {
                        beNbt.putString("id", "minecraft:shulker_box");
                    }
                    else
                    {
                        beNbt.putString("id", itemId);
                    }
                }
                case "minecraft:charged_projectiles" ->
                {
                    outNbt.put("ChargedProjectiles", processChargedProjectile(nbt.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                    outNbt.putBoolean("Charged", true);
                }
                case "minecraft:container_loot" ->
                {
                    beNbt.put("LootTable", processLootTable(nbt.getData(key).orElse(new CompoundData())));
                    beNbt.putString("id", itemId);
                }
                case "minecraft:custom_data" -> processCustomData(nbt.getData(key).orElse(new CompoundData()), outNbt);
                case "minecraft:custom_model_data" -> outNbt.putInt("CustomModelData", nbt.getIntOrDefault(key, 0));
                case "minecraft:custom_name" -> dispNbt.putString("Name", processCustomNameTag(nbt, key, registry));
                case "minecraft:damage" -> outNbt.putInt("Damage", nbt.getIntOrDefault(key, 0));
                case "minecraft:debug_stick_state" -> outNbt.put("DebugProperty", nbt.getData(key).orElse(new CompoundData()));
                case "minecraft:dyed_color" -> dispNbt.putInt("color", processDyedColor(nbt.getData(key).orElse(new CompoundData())));
                case "minecraft:enchantments" -> outNbt.put("Enchantments", processEnchantments(nbt.getData(key).orElse(new CompoundData()), true, true));
                case "minecraft:entity_data" -> outNbt.put("EntityTag", downgradeEntity_to_1_20_4((CompoundData) nbt.getData(key).orElse(new CompoundData()), minecraftDataVersion, registry));
                case "minecraft:stored_enchantments" -> outNbt.put("StoredEnchantments", processEnchantments(nbt.getData(key).orElse(new CompoundData()), true, true));
                case "minecraft:fireworks" -> outNbt.put("Fireworks", processFireworks(nbt.getData(key).orElse(new CompoundData())));
                case "minecraft:firework_explosion" -> outNbt.put("Explosion", processFireworkExplosion(nbt.getData(key).orElse(new CompoundData())));
                // "minecraft:hide_additional_tooltip" --> ignore
                case "minecraft:instrument" -> outNbt.put("instrument", processInstrument(nbt.getData(key).orElse(new CompoundData())));
                case "minecraft:item_name" -> dispNbt.putString("Name", processItemName(nbt.getData(key).orElse(new CompoundData()), registry));
                case "minecraft:lock" ->
                {
                    beNbt.put("Lock", nbt.getData(key).orElse(new CompoundData()));
                    beNbt.putString("id", itemId);
                }
                case "minecraft:lodestone_tracker" -> processLodestoneTracker(nbt.getData(key).orElse(new CompoundData()), outNbt);
                case "minecraft:lore" -> dispNbt.put("Lore", nbt.getData(key).orElse(new CompoundData()));
                case "minecraft:map_id" -> outNbt.put("map", processMapId(nbt.getData(key).orElse(new CompoundData())));
                case "minecraft:map_color" -> dispNbt.put("MapColor", nbt.getData(key).orElse(new CompoundData()));
                case "minecraft:map_decorations" -> outNbt.put("Decorations", processMapDecorations(nbt.getData(key).orElse(new CompoundData())));
                case "minecraft:note_block_sound" -> beNbt.put("note_block_sound", nbt.getData(key).orElse(new CompoundData()));
                case "minecraft:pot_decorations" ->
                {
                    beNbt.put("sherds", processSherds(nbt.getData(key).orElse(new CompoundData())));
                    beNbt.putString("id", itemId);
                }
                case "minecraft:potion_contents" -> processPotions(nbt.getData(key).orElse(new CompoundData()), outNbt);
                case "minecraft:profile" -> outNbt.put("SkullOwner", processSkullProfile(nbt.getData(key).orElse(new CompoundData()), dispNbt, minecraftDataVersion, registry));
                case "minecraft:repair_cost" -> outNbt.putInt("RepairCost", nbt.getIntOrDefault(key, 0));
                case "minecraft:recipes" -> outNbt.put("Recipes", processRecipes(nbt.getData(key).orElse(new CompoundData())));
                case "minecraft:suspicious_stew_effects" -> outNbt.put("effects", processSuspiciousStewEffects(nbt.getData(key).orElse(new CompoundData())));
                case "minecraft:trim" -> outNbt.put("Trim", processTrim(nbt.getData(key).orElse(new CompoundData())));
                case "minecraft:writable_book_content" ->
                {
                    CompoundData bookNbt = nbt.getCompound(key);
                    bookNbt = processWritableBookContent(bookNbt, minecraftDataVersion, registry);
                    for (String bookKey : bookNbt.getKeys())
                    {
                        outNbt.put(bookKey, bookNbt.getData(bookKey).orElse(new CompoundData()));
                    }
                }
                case "minecraft:written_book_content" ->
                {
                    CompoundData bookNbt = nbt.getCompound(key);
                    bookNbt = processWrittenBookContent(bookNbt, minecraftDataVersion, registry);
                    for (String bookKey : bookNbt.getKeys())
                    {
                        outNbt.put(bookKey, bookNbt.getData(bookKey).orElse(new CompoundData()));
                    }
                }
                case "minecraft:unbreakable" -> outNbt.putBoolean("Unbreakable", processUnbreakable(nbt.getData(key).orElse(new CompoundData())));
            }
        }
        if (!beNbt.isEmpty())
        {
            outNbt.put("BlockEntityTag", beNbt);
        }
        if (!dispNbt.isEmpty())
        {
            outNbt.put("display", dispNbt);
        }
        if (!outNbt.contains("RepairCost", Constants.NBT.TAG_INT) && (itemId.equals("minecraft:dragon_head") || needsDamage))
        {
            outNbt.putInt("RepairCost", 0);
        }
        if (!outNbt.contains("Damage", Constants.NBT.TAG_INT) && needsDamage)
        {
            outNbt.putInt("Damage", 0);
        }

        return outNbt;
    }

    private static void processCustomData(BaseData oldNbt, CompoundData outNbt)
    {
        CompoundData origData = oldNbt.asCompound().orElse(new CompoundData());

        for (String keyData : origData.getKeys())
        {
            outNbt.put(keyData, origData.getData(keyData).orElse(new CompoundData()));
        }
    }

    private static void processLodestoneTracker(BaseData oldEle, CompoundData outNbt)
    {
        CompoundData oldNbt = oldEle.asCompound().orElse(new CompoundData());

        if (oldNbt.contains("tracked", Constants.NBT.TAG_BYTE))
        {
            outNbt.putBoolean("LodestoneTracked", oldNbt.getBooleanOrDefault("tracked", false));
        }
        if (oldNbt.contains("target", Constants.NBT.TAG_COMPOUND))
        {
            CompoundData target = oldNbt.getCompound("target");

            outNbt.put("LodestoneDimension", target.getData("dimension").orElse(new CompoundData()));
            outNbt.put("LodestonePos", target.getData("pos").orElse(new CompoundData()));
        }
    }

    private static void processBucketEntityData(BaseData oldTags, CompoundData beNbt, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData oldNbt = oldTags.asCompound().orElse(new CompoundData());

//        NbtCompound newNbt = downgradeEntity_to_1_20_4(oldNbt, minecraftDataVersion, registry);
//        beNbt.copyFrom(newNbt);

        for (String key : oldNbt.getKeys())
        {
            beNbt.put(key, oldNbt.getData(key).orElse(new CompoundData()));
        }
    }

    private static void processPotions(BaseData oldPots, CompoundData outNbt)
    {
        CompoundData oldNbt = oldPots.asCompound().orElse(new CompoundData());

        if (oldNbt.contains("potion", Constants.NBT.TAG_STRING))
        {
            outNbt.putString("Potion", oldNbt.getStringOrDefault("potion", ""));
        }
        if (oldNbt.containsLenient("custom_color"))
        {
            outNbt.put("CustomPotionColor", Objects.requireNonNull(oldNbt.getData("custom_color").orElse(new CompoundData())));
        }
        if (oldNbt.containsLenient("custom_effects"))
        {
            outNbt.put("custom_potion_effects", Objects.requireNonNull(oldNbt.getData("custom_effects").orElse(new CompoundData())));
        }
    }

    private static BaseData processMapDecorations(BaseData oldDeco)
    {
        CompoundData oldTag = oldDeco.asCompound().orElse(new CompoundData());
        ListData newTags = new ListData();

        for (String key : oldTag.getKeys())
        {
            CompoundData entryOld = oldTag.getCompound(key);
            CompoundData entryNew = new CompoundData();

            entryNew.putString("id", key);
            entryNew.putDouble("x", entryOld.contains("x", Constants.NBT.TAG_DOUBLE) ? entryOld.getDoubleOrDefault("x", 0d) : 0.0);
            entryNew.putDouble("z", entryOld.contains("z", Constants.NBT.TAG_DOUBLE) ? entryOld.getDoubleOrDefault("z", 0d) : 0.0);
            entryNew.putDouble("rot", entryOld.contains("rotation", Constants.NBT.TAG_FLOAT) ? (double) entryOld.getFloatOrDefault("rotation", 0f) : 0.0);
            entryNew.putByte("type", (byte) (entryOld.contains("type", Constants.NBT.TAG_STRING) ? convertMapDecoration(entryOld.getStringOrDefault("type", "")) : 0));

            newTags.add(entryNew);
        }

        return newTags;
    }

    private static int convertMapDecoration(String type)
    {
        return switch (type)
        {
            case "minecraft:player" -> 0;
            case "minecraft:frame" -> 1;
            case "minecraft:red_marker" -> 2;
            case "minecraft:blue_marker" -> 3;
            case "minecraft:target_x" -> 4;
            case "minecraft:target_point" -> 5;
            case "minecraft:player_off_map" -> 6;
            case "minecraft:player_off_limits" -> 7;
            case "minecraft:mansion" -> 8;
            case "minecraft:monument" -> 9;
            case "minecraft:banner_white" -> 10;
            case "minecraft:banner_orange" -> 11;
            case "minecraft:banner_magenta" -> 12;
            case "minecraft:banner_light_blue" -> 13;
            case "minecraft:banner_yellow" -> 14;
            case "minecraft:banner_lime" -> 15;
            case "minecraft:banner_pink" -> 16;
            case "minecraft:banner_gray" -> 17;
            case "minecraft:banner_light_gray" -> 18;
            case "minecraft:banner_cyan" -> 19;
            case "minecraft:banner_purple" -> 20;
            case "minecraft:banner_blue" -> 21;
            case "minecraft:banner_brown" -> 22;
            case "minecraft:banner_green" -> 23;
            case "minecraft:banner_red" -> 24;
            case "minecraft:banner_black" -> 25;
            case "minecraft:red_x" -> 26;
            case "minecraft:village_desert" -> 27;
            case "minecraft:village_plains" -> 28;
            case "minecraft:village_savanna" -> 29;
            case "minecraft:village_snowy" -> 30;
            case "minecraft:village_taiga" -> 31;
            case "minecraft:jungle_temple" -> 32;
            case "minecraft:swamp_hut" -> 33;
            default -> 0;
        };
    }

    private static BaseData processSherds(BaseData oldSherds)
    {
        return oldSherds;
    }

    private static BaseData processLootTable(BaseData oldLoot)
    {
        CompoundData oldTable = oldLoot.asCompound().orElse(new CompoundData());
        CompoundData newTable = new CompoundData();

        if (oldTable.contains("loot_table", Constants.NBT.TAG_COMPOUND))
        {
            CompoundData loot = oldTable.getCompound("loot_table");
            newTable.combine(loot);
        }
        if (oldTable.contains("seed", Constants.NBT.TAG_LONG))
        {
            newTable.putLong("LootTableSeed", oldTable.getLongOrDefault("seed", 0L));
        }

        return newTable;
    }

    private static String processItemName(BaseData oldName, RegistryAccess registry)
    {
        if (oldName != null)
        {
            return oldName.toString();
        }

        return "minecraft:air";
    }

    private static int processDyedColor(BaseData oldDye)
    {
        CompoundData oldColor = oldDye.asCompound().orElse(new CompoundData());

        if (oldColor.contains("rgb", Constants.NBT.TAG_INT))
        {
            return oldColor.getIntOrDefault("rgb", 10511680);
        }

        // Default
        return 10511680;
    }

    private static BaseData processRecipes(BaseData oldRecipes)
    {
        return oldRecipes;
    }

    private static BaseData processInstrument(BaseData oldGoat)
    {
        return oldGoat;
    }

    private static BaseData processSuspiciousStewEffects(BaseData oldEffects)
    {
        return oldEffects;
    }

    private static BaseData processMapId(BaseData oldMapId)
    {
        return oldMapId;
    }

    private static BaseData processTrim(BaseData oldTrim)
    {
        return oldTrim;
    }

    private static ListData processChargedProjectile(BaseData oldProjectiles, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        ListData oldNbt = oldProjectiles.asList().orElse(new ListData());
        ListData newNbt = new ListData();

        for (int i = 0; i < oldNbt.size(); i++)
        {
            CompoundData itemEntry = oldNbt.getCompoundAt(i);
            CompoundData newEntry = new CompoundData();

            if (!itemEntry.contains("id", Constants.NBT.TAG_STRING))
            {
                continue;
            }
            String idName = itemEntry.getStringOrDefault("id", "");
            newEntry.putString("id", idName);
            newEntry.putByte("Count", (byte) (itemEntry.contains("count", Constants.NBT.TAG_INT) ? itemEntry.getInt("count") : 1));
            if (itemEntry.contains("components", Constants.NBT.TAG_COMPOUND))
            {
                newEntry.put("tag", processComponentsTag(itemEntry.getCompound("components"), idName, minecraftDataVersion, registry));
            }

            newNbt.add(newEntry);
        }

        return newNbt;
    }

    private static boolean processUnbreakable(BaseData oldNbt)
    {
        CompoundData oldUnbr = oldNbt.asCompound().orElse(new CompoundData());

        if (oldUnbr.contains("show_in_tooltip", Constants.NBT.TAG_BYTE))
        {
            return oldUnbr.getBooleanOrDefault("show_in_tooltip", false);
        }

        return false;
    }

    private static void processBlockEntityData(BaseData oldBeData, CompoundData beNbt, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData newData = downgradeBlockEntity_to_1_20_4((CompoundData) oldBeData, minecraftDataVersion, registry);

        for (String key : newData.getKeys())
        {
            beNbt.put(key, newData.getData(key).orElse(new CompoundData()));
        }
    }

    private static BaseData processDecoratedPot(BaseData oldPot, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData oldNbt = oldPot.asCompound().orElse(new CompoundData());
        CompoundData newNbt = new CompoundData();

        for (String key : oldNbt.getKeys())
        {
            switch (key)
            {
                case "id" -> newNbt.putString("id", oldNbt.getStringOrDefault("id", ""));
                case "count" -> newNbt.putByte("Count", (byte) oldNbt.getIntOrDefault("count", 1));
                case "components" -> newNbt.put("tag", processComponentsTag(oldNbt.getCompound("components"), oldNbt.getStringOrDefault("id", ""), minecraftDataVersion, registry));
            }
        }

        if (!newNbt.contains("tag", Constants.NBT.TAG_COMPOUND) && oldNbt.contains("id", Constants.NBT.TAG_STRING) && needsDamageTag(oldNbt.getStringOrDefault("id", "")))
        {
            CompoundData newTag = new CompoundData();
            newTag.putInt("Damage", 0);
            newNbt.put("tag", newTag);
        }

        return newNbt;
    }

    private static BaseData processEnchantments(BaseData oldNbt, boolean fullId, boolean shortInt)
    {
        CompoundData oldEnchants = oldNbt.asCompound().orElse(new CompoundData());
        CompoundData oldLevels = oldEnchants.getCompound("levels");
        ListData newEnchants = new ListData();
        boolean showTooltip = false;

        if (oldEnchants.contains("show_in_tooltip", Constants.NBT.TAG_BYTE))
        {
            showTooltip = oldEnchants.getBooleanOrDefault("show_in_tooltip", false);
            // todo - Has no function under 1.20.4
        }

        for (String key : oldLevels.getKeys())
        {
            CompoundData newEntry = new CompoundData();
            Identifier id = Identifier.parse(key);
            if (shortInt)
            {
                newEntry.putShort("lvl", (short) oldLevels.getIntOrDefault(key, 1));
            }
            else
            {
                newEntry.putInt("lvl", oldLevels.getIntOrDefault(key, 1));
            }
            newEntry.putString("id", fullId ? id.toString() : id.getPath());
            newEnchants.add(newEntry);
        }

        return newEnchants;
    }

    private static String processCustomNameTag(CompoundData nameTag, String key, @Nonnull RegistryAccess registry)
    {
        // Sometimes this is missing the 'text' designation ?

        /*
        String oldNameString = nameTag.getString(key);
        MutableText oldCustomName = Text.Serialization.fromJson(oldNameString, registry);

        //System.out.printf("processCustomNameTag(): oldName [%s], text: [%s], newString [%s]\n", oldNameString, oldCustomName.getString(), newCustomName);

         */

        MutableComponent oldName = (MutableComponent) DataBlockUtils.getCustomName(nameTag, registry, key);
        return legacyTextDeserializer(oldName, registry);
    }

    private static String legacyTextDeserializer(MutableComponent oldText, @Nonnull RegistryAccess registry)
    {
        try
        {
            JsonElement element = ComponentSerialization.CODEC.encodeStart(registry.createSerializationContext(JsonOps.INSTANCE), oldText).getOrThrow();
            return new GsonBuilder().disableHtmlEscaping().create().toJson(element);
        }
        catch (Exception err)
        {
            Litematica.LOGGER.error("legacyTextDeserializer: Failed to convert MutableText to JSON; (falling back to just 'getString'); {}", err.getLocalizedMessage());
            return oldText.getString();
        }
    }

    private static @Nullable MutableComponent legacyTextSerializer(String json, @Nonnull RegistryAccess registry)
    {
        try
        {
            return (MutableComponent) ComponentSerialization.CODEC.parse(registry.createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json)).getOrThrow();
        }
        catch (Exception err)
        {
            Litematica.LOGGER.error("legacyTextSerializer: Failed to convert JSON to MutableText; {}", err.getLocalizedMessage());
            return null;
        }
    }

    private static BaseData processBlockState(BaseData bsTag)
    {
        CompoundData oldBS = bsTag.asCompound().orElse(new CompoundData());
        CompoundData newBS = new CompoundData();

        for (String key : oldBS.getKeys())
        {
            newBS.put(key, oldBS.getData(key).orElse(new CompoundData()));
        }

        return bsTag;
    }

    private static BaseData processFireworks(BaseData rocket)
    {
        CompoundData oldRocket = rocket.asCompound().orElse(new CompoundData());
        CompoundData newRocket = new CompoundData();

        if (oldRocket.contains("flight_duration", Constants.NBT.TAG_BYTE))
        {
            newRocket.putByte("Flight", oldRocket.getByteOrDefault("flight_duration", (byte) 1));
        }
        if (oldRocket.containsList("explosions", Constants.NBT.TAG_COMPOUND))
        {
            ListData oldExplosions = oldRocket.getList("explosions");
            ListData newExplosions = new ListData();

            for (int i = 0; i < oldExplosions.size(); i++)
            {
                newExplosions.add(processFireworkExplosion(oldExplosions.getCompoundAt(i)));
            }

            newRocket.put("Explosions", newExplosions);
        }

        return newRocket;
    }

    private static BaseData processFireworkExplosion(BaseData explosion)
    {
        CompoundData oldExplosion = explosion.asCompound().orElse(new CompoundData());
        CompoundData newExplosion = new CompoundData();

        if (oldExplosion.contains("shape", Constants.NBT.TAG_STRING))
        {
            newExplosion.putByte("Type", (byte) convertFireworkShape(oldExplosion.getStringOrDefault("shape", "")));
        }
        if (oldExplosion.contains("colors", Constants.NBT.TAG_INT_ARRAY))
        {
            newExplosion.putIntArray("Colors", oldExplosion.getIntArrayOrDefault("colors", new int[0]));
        }
        if (oldExplosion.contains("fade_colors", Constants.NBT.TAG_INT_ARRAY))
        {
            newExplosion.putIntArray("FadeColors", oldExplosion.getIntArrayOrDefault("fade_colors", new int[0]));
        }
        if (oldExplosion.contains("has_trail", Constants.NBT.TAG_BYTE))
        {
            newExplosion.putBoolean("Trail", oldExplosion.getBooleanOrDefault("has_trail", false));
        }
        if (oldExplosion.contains("has_twinkle", Constants.NBT.TAG_BYTE))
        {
            newExplosion.putBoolean("Flicker", oldExplosion.getBooleanOrDefault("has_twinkle", false));
        }

        return newExplosion;
    }

    private static int convertFireworkShape(String shape)
    {
        return switch (shape)
        {
            case "small_ball" -> 0;
            case "large_ball" -> 1;
            case "star" -> 2;
            case "creeper" -> 3;
            case "burst" -> 4;
            default -> 0;
        };
    }

    private static BaseData processRecordItem(BaseData itemIn, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData oldRecord = itemIn.asCompound().orElse(new CompoundData());
        CompoundData recordOut = new CompoundData();

        recordOut.putString("id", oldRecord.getStringOrDefault("id", ""));
        recordOut.putByte("Count", (byte) oldRecord.getIntOrDefault("count", 1));

        if (oldRecord.contains("components", Constants.NBT.TAG_COMPOUND))
        {
            recordOut.put("tag", processComponentsTag(oldRecord.getCompound("components"), oldRecord.getStringOrDefault("id", ""), minecraftDataVersion, registry));
        }

        return recordOut;
    }

    private static BaseData processBookTag(BaseData bookNbt, int minecraftDataVersion, RegistryAccess registry)
    {
        CompoundData oldBook = bookNbt.asCompound().orElse(new CompoundData());
        CompoundData newBook = new CompoundData();

        newBook.putString("id", oldBook.getStringOrDefault("id", ""));
        newBook.putByte("Count", (byte) oldBook.getIntOrDefault("count", 1));

        if (oldBook.contains("Page", Constants.NBT.TAG_INT))
        {
            newBook.putInt("Page", oldBook.getIntOrDefault("Page", 1));
        }
        if (oldBook.contains("components", Constants.NBT.TAG_COMPOUND))
        {
            newBook.put("tag", processComponentsTag(oldBook.getCompound("components"), oldBook.getStringOrDefault("id", ""), minecraftDataVersion, registry));
        }

        return newBook;
    }

    private static CompoundData processWritableBookContent(CompoundData bookNbt, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData newBook = new CompoundData();
        ListData newPages = new ListData();

        if (bookNbt.containsList("pages", Constants.NBT.TAG_COMPOUND))
        {
            ListData pages = bookNbt.getList("pages");

            for (int i = 0; i < pages.size(); i++)
            {
                CompoundData page = pages.getCompoundAt(i);
                String oldPage = page.getStringOrDefault("raw", "");

                try
                {
                    MutableComponent oldText = legacyTextSerializer(oldPage, registry);
//                    MutableText oldText = Text.Serialization.fromJson(oldPage, registry);
//                    String newPage = Text.Serialization.toJsonString(oldText, registry);
                    String newPage = legacyTextDeserializer(oldText, registry);
                    newPages.add(i, new StringData(newPage));
                }
                catch (Exception e)
                {
                    newPages.add(i, new StringData(oldPage));
                }
            }
        }
        if (!newPages.isEmpty())
        {
            newBook.put("pages", newPages);
        }

        return newBook;
    }

    private static CompoundData processWrittenBookContent(CompoundData bookNbt, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData newBook = new CompoundData();
        CompoundData filtered = new CompoundData();
        ListData newPages = new ListData();

        if (bookNbt.contains("author", Constants.NBT.TAG_STRING))
        {
            newBook.putString("author", bookNbt.getStringOrDefault("author", "?"));
        }
        if (bookNbt.contains("title", Constants.NBT.TAG_COMPOUND))
        {
            CompoundData title = bookNbt.getCompound("title");
            newBook.putString("title", title.getStringOrDefault("raw", ""));
        }
        if (bookNbt.contains("resolved", Constants.NBT.TAG_BYTE))
        {
            newBook.putBoolean("resolved", bookNbt.getBooleanOrDefault("resolved", false));
        }
        if (bookNbt.contains("generation", Constants.NBT.TAG_INT))
        {
            newBook.putInt("generation", bookNbt.getIntOrDefault("generation", 1));
        }

        if (bookNbt.containsList("pages", Constants.NBT.TAG_COMPOUND))
        {
            ListData pages = bookNbt.getList("pages");

            for (int i = 0; i < pages.size(); i++)
            {
                CompoundData page = pages.getCompoundAt(i);
                String oldPage = page.getStringOrDefault("raw", "");

                if (page.contains("filtered", Constants.NBT.TAG_STRING))
                {
                    String filterPage = page.getStringOrDefault("filtered", "");
                    try
                    {
                        MutableComponent filteredText = legacyTextSerializer(filterPage, registry);
//                        MutableText filteredText = Text.Serialization.fromJson(filterPage, registry);
//                        String newFilterPage = Text.Serialization.toJsonString(filteredText, registry);
                        String newFilterPage = legacyTextDeserializer(filteredText, registry);
                        filtered.putString(filterPage, newFilterPage);
                        // This seems like A terrible idea
                    }
                    catch (Exception e)
                    {
                        filtered.putString(filterPage, filterPage);
                    }
                }
                try
                {
                    MutableComponent oldText = legacyTextSerializer(oldPage, registry);
//                    MutableText oldText = Text.Serialization.fromJson(oldPage, registry);
//                    String newPage = Text.Serialization.toJsonString(oldText, registry);
                    String newPage = legacyTextDeserializer(oldText, registry);
                    newPages.add(i, new StringData(newPage));
                }
                catch (Exception e)
                {
                    newPages.add(i, new StringData(oldPage));
                }
            }
        }
        if (!newPages.isEmpty())
        {
            newBook.put("pages", newPages);
        }
        if (!filtered.isEmpty())
        {
            newBook.put("filtered_pages", filtered);
        }

        return newBook;
    }

    private static BaseData processBannerPatterns(BaseData oldPatterns)
    {
        ListData oldList = oldPatterns.asList().orElse(new ListData());
        ListData newList = new ListData();

        for (int i = 0; i < oldList.size(); i++)
        {
            CompoundData oldEntry = oldList.getCompoundAt(i);
            CompoundData newEntry = new CompoundData();
            String color = oldEntry.getStringOrDefault("color", "");
            String pattern = oldEntry.getStringOrDefault("pattern", "");
            DyeColor dye = DyeColor.byName(color, DyeColor.WHITE);

            newEntry.putString("Pattern", convertBannerPattern(pattern));
            newEntry.putInt("Color", dye.getId());

            newList.add(newEntry);
        }

        return newList;
    }

    private static String convertBannerPattern(String patternId)
    {
        return switch (patternId)
        {
            case "minecraft:base" -> "b";
            case "minecraft:square_bottom_left" -> "bl";
            case "minecraft:square_bottom_right" -> "br";
            case "minecraft:square_top_left" -> "tl";
            case "minecraft:square_top_right" -> "tr";
            case "minecraft:stripe_bottom" -> "bs";
            case "minecraft:stripe_top" -> "ts";
            case "minecraft:stripe_left" -> "ls";
            case "minecraft:stripe_right" -> "rs";
            case "minecraft:stripe_center" -> "cs";
            case "minecraft:stripe_middle" -> "ms";
            case "minecraft:stripe_downright" -> "drs";
            case "minecraft:stripe_downleft" -> "dls";
            case "minecraft:small_stripes" -> "ss";
            case "minecraft:cross" -> "cr";
            case "minecraft:straight_cross" -> "sc";
            case "minecraft:triangle_bottom" -> "bt";
            case "minecraft:triangle_top" -> "tt";
            case "minecraft:triangles_bottom" -> "bts";
            case "minecraft:triangles_top" -> "tts";
            case "minecraft:diagonal_left" -> "ld";
            case "minecraft:diagonal_up_right" -> "rd";
            case "minecraft:diagonal_up_left" -> "lud";
            case "minecraft:diagonal_right" -> "rud";
            case "minecraft:circle" -> "mc";
            case "minecraft:rhombus" -> "mr";
            case "minecraft:half_vertical" -> "vh";
            case "minecraft:half_horizontal" -> "hh";
            case "minecraft:half_vertical_right" -> "vhr";
            case "minecraft:half_horizontal_bottom" -> "hhb";
            case "minecraft:border" -> "bo";
            case "minecraft:curly_border" -> "cbo";
            case "minecraft:gradient" -> "gra";
            case "minecraft:gradient_up" -> "gru";
            case "minecraft:bricks" -> "bri";
            case "minecraft:globe" -> "glb";
            case "minecraft:creeper" -> "cre";
            case "minecraft:skull" -> "sku";
            case "minecraft:flower" -> "flo";
            case "minecraft:mojang" -> "moj";
            case "minecraft:piglin" -> "pig";
            // Doesn't exist in 1.20.4
            //case "minecraft:flow" -> "flo";
            //case "minecraft:guster" -> "gus";
            default -> "b";
        };
    }

    private static BaseData processSkullProfile(BaseData oldProfile, CompoundData dispNbt, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData profile = oldProfile.asCompound().orElse(new CompoundData());
        CompoundData newProfile = new CompoundData();
        String customName1 = dispNbt.getStringOrDefault("Name", "");         // Can be either an Item Name or Custom Name Data Component
        String customName2 = dispNbt.getStringOrDefault("CustomName", "");   // Only if invoked without it being stored in a Chest
        String name = profile.getStringOrDefault("name", "");                // The regular Skull Owner Name
        //UUID uuid = profile.getUuid("id");
        UUID uuid = profile.getCodec("id", UUIDUtil.AUTHLIB_CODEC, registry.createSerializationContext(DataOps.INSTANCE)).orElse(Util.NIL_UUID);

//        LOGGER.debug("processSkullProfile(): oldNBT [{}]", profile.toString());
        if (name.isEmpty() && !customName1.isEmpty())
        {
            try
            {
//                Text disp = Text.Serialization.fromJson(customName1, registry);
                MutableComponent disp = legacyTextSerializer(customName1, registry);

                if (disp != null)
                {
                    name = disp.tryCollapseToString();
                }

                if (name == null)
                {
                    name = customName1;
                }

//                LOGGER.debug("processSkullProfile(): customName1 [{}], disp [{}] // name [{}]", customName1, disp != null ? disp.getString() : "<null>", name);
            }
            catch (Exception e)
            {
                Litematica.LOGGER.warn("processSkullProfile(): Exception deserializing CustomName1 for Head Name.");
                name = customName1;
            }
        }
        else if (name.isEmpty() && !customName2.isEmpty())
        {
            try
            {
//                Text disp = Text.Serialization.fromJson(customName2, registry);
                MutableComponent disp = legacyTextSerializer(customName2, registry);

                if (disp != null)
                {
                    name = disp.tryCollapseToString();
                }

                if (name == null)
                {
                    name = customName2;
                }

//                LOGGER.debug("processSkullProfile(): customName2 [{}], disp[{}] // name [{}]", customName2, disp != null ? disp.getString() : "<null>", name);
            }
            catch (Exception e)
            {
                Litematica.LOGGER.warn("processSkullProfile(): Exception deserializing CustomName2 for Head Name.");
                name = customName2;
            }
        }

        newProfile.putString("Name", name);
        //newProfile.putUuid("Id", uuid);
        newProfile.putCodec("Id", UUIDUtil.CODEC, uuid);

//        LOGGER.debug("processSkullProfile(): name [{}], uuid [{}]", name, uuid.toString());

        ListData properties = profile.getList("properties");
        CompoundData newProperties = new CompoundData();

        for (int i = 0; i < properties.size(); i++)
        {
            CompoundData property = properties.getCompoundAt(i);
            String propName = property.getStringOrDefault("name", "");
            String propValue = property.getStringOrDefault("value", "");

//            LOGGER.debug("processSkullProfile(): entry[{}], name [{}]", i, propName);

            if (propName.equals("textures"))
            {
                ListData textures = new ListData();
                CompoundData value = new CompoundData();
                value.putString("Value", propValue);
                textures.add(value);
                newProperties.put("textures", textures);
            }
        }

        newProfile.put("Properties", newProperties);
//        LOGGER.debug("processSkullProfile(): newNBT [{}]", newProfile.toString());

        return newProfile;
    }

    private static BaseData processFlowerPos(CompoundData oldNbt, String key, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        CompoundData flowerOut = new CompoundData();
        //BlockPos flowerPos = NbtHelper.toBlockPos(oldNbt, key).orElse(null);
        BlockPos flowerPos = oldNbt.getCodec(key, BlockPos.CODEC, registry.createSerializationContext(DataOps.INSTANCE)).orElse(null);

        if (flowerPos != null)
        {
            flowerOut.putInt("X", flowerPos.getX());
            flowerOut.putInt("Y", flowerPos.getY());
            flowerOut.putInt("Z", flowerPos.getZ());
        }

        return flowerOut;
    }

    private static BaseData processBeesTag(BaseData beesTag, int minecraftDataVersion, @Nonnull RegistryAccess registry)
    {
        ListData oldBees = beesTag.asList().orElse(new ListData());
        ListData newBees = new ListData();

        for (int i = 0; i < oldBees.size(); i++)
        {
            CompoundData oldEntry = oldBees.getCompoundAt(i);
            CompoundData newEntry = new CompoundData();

            newEntry.putInt("TicksInHive", oldEntry.getIntOrDefault("ticks_in_hive", 0));
            newEntry.putInt("MinOccupationTicks", oldEntry.getIntOrDefault("min_ticks_in_hive", 0));
            newEntry.put("EntityData", downgradeEntity_to_1_20_4(oldEntry.getCompound("entity_data"), minecraftDataVersion, registry));

            newBees.add(newEntry);
        }

        return newBees;
    }
}
