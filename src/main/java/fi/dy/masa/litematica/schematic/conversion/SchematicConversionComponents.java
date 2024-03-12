package fi.dy.masa.litematica.schematic.conversion;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.util.ComponentUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.*;

public class SchematicConversionComponents
{
    public static NbtCompound processBlockStateTags_1_20_4_to_1_20_5(@Nonnull NbtCompound oldBlockStateTag)
    {
        NbtCompound newBlockStateTag = new NbtCompound();

        if (MinecraftClient.getInstance().world != null)
        {
            World clientWorld = MinecraftClient.getInstance().world;
            DynamicRegistryManager registryManager = clientWorld.getRegistryManager();

            Litematica.debugLog("processBlockStateTags_1_20_4_to_1_20_5(): NBT in: {}", oldBlockStateTag.toString());

            newBlockStateTag = processBlockStateTags_1_20_4_to_1_20_5_Each(oldBlockStateTag, registryManager);

            newBlockStateTag.copyFrom(oldBlockStateTag);
            return newBlockStateTag;
        }
        else
        {
            Litematica.logger.error("processBlockStateTags_1_20_4_to_1_20_5(): called while clientWorld == null");

            return oldBlockStateTag;
        }
    }

    private static NbtCompound processBlockStateTags_1_20_4_to_1_20_5_Each(@Nonnull NbtCompound oldBlockStateTag,
                                                                           @Nonnull DynamicRegistryManager registryLookup)
    {
        NbtCompound newBlockStateTag = new NbtCompound();

        if (oldBlockStateTag.contains("Name", 8))
        {
            String blockName = oldBlockStateTag.getString("Name");
            Identifier blockId = new Identifier(blockName);
            Block block = Registries.BLOCK.get(blockId);

            Litematica.debugLog("processBlockStateTags_1_20_4_to_1_20_5_Each(): Name {}, blockId {}, block {}", blockName, blockId.toString(), block.toString());

            if (!block.equals(Blocks.AIR))
            {
                BlockState blockState = block.getDefaultState();

                if (oldBlockStateTag.contains("Properties", 10))
                {
                    NbtCompound blockStateTag = oldBlockStateTag.getCompound("Properties");

                    //Litematica.debugLog("processBlockStateTags_1_20_4_to_1_20_5_Each(): Properties NBT {}", blockStateTag.toString());

                    for (String key : blockStateTag.getKeys())
                    {
                        NbtElement blockStateElement = blockStateTag.get(key);

                        if (blockStateElement != null)
                        {
                            Litematica.debugLog("processBlockStateTags_1_20_4_to_1_20_5_Each(): Properties -> Element[{}//{}]: {}", blockStateElement.getType(), key, blockStateElement.toString());
                        }
                        else
                        {
                            Litematica.logger.error("processBlockStateTags_1_20_4_to_1_20_5_Each(): element failed to be read");
                        }
                    }
                }

                newBlockStateTag.copyFrom(oldBlockStateTag);
            }
            else
            {
                Litematica.debugLog("processBlockStateTags_1_20_4_to_1_20_5_Each(): ignoring AIR block.");
                return oldBlockStateTag;
            }
        }

        return newBlockStateTag;
    }

    public static NbtCompound processTileEntityTags_1_20_4_to_1_20_5(@Nonnull BlockPos oldPos,
                                                                     @Nonnull NbtCompound oldTE)
    {
        if (MinecraftClient.getInstance().world != null)
        {
            World clientWorld = MinecraftClient.getInstance().world;
            DynamicRegistryManager registryManager = clientWorld.getRegistryManager();

            //Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5(): pos: {}, NBT in: {}", oldPos.toShortString(), oldTE.toString());

            String teString;
            Identifier teId = null;
            BlockPos tePos;

            if (oldTE.contains("id"))
            {
                teString = oldTE.getString("id");
                teId = new Identifier(teString);
            }
            else
            {
                Litematica.logger.error("processTileEntityTags_1_20_4_to_1_20_5(): pos: {} does not contain an \"id\" (not fixing)", oldPos.toShortString());
                return oldTE;
            }
            if (oldTE.contains("x") && oldTE.contains("y") && oldTE.contains("z"))
            {
                int teX = oldTE.getInt("x");
                int teY = oldTE.getInt("y");
                int teZ = oldTE.getInt("z");
                tePos = new BlockPos(teX, teY, teZ);
            }
            else
            {
                Litematica.logger.error("processTileEntityTags_1_20_4_to_1_20_5(): pos: {} does not contain \"x, y, z\" values", oldPos.toShortString());
                return oldTE;
            }

            if (teId != null)
            {
                Litematica.debugLog("Old TE: id {}, pos {}", teId.toString(), tePos.toShortString());

                NbtCompound newTE = processTileEntityTags_1_20_4_to_1_20_5_Each(tePos, teId, oldTE, registryManager);

                Litematica.debugLog("New TE: id {}, nbt {}", teId.toString(), newTE.toString());

                return newTE;
            }
            else
            {
                return oldTE;
            }
        }
        else
        {
            Litematica.logger.error("processBlockStateTags_1_20_4_to_1_20_5(): called while clientWorld == null");

            return oldTE;
        }
    }

    private static NbtCompound processTileEntityTags_1_20_4_to_1_20_5_Each(@Nonnull BlockPos inPos, @Nonnull Identifier inId,
                                                                           @Nonnull NbtCompound inTE,
                                                                           @Nonnull DynamicRegistryManager registryLookup)
    {
        NbtCompound outTE = new NbtCompound();

        if (!inTE.isEmpty())
        {
            Text customName = null;
            Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_Each(): inTE {}", inTE.toString());

            if (inTE.contains("CustomName"))
            {
                customName = Text.Serialization.fromJson(inTE.getString("CustomName"), registryLookup);
            }
            if (inTE.contains("Items"))
            {
                NbtList inItemList = inTE.getList("Items", 10);
                NbtList outItemList;

                outItemList = processTileEntityTags_1_20_4_to_1_20_5_ItemList(inPos, inItemList, registryLookup);

                inTE.remove("Items");
                outTE.put("Items", outItemList);
            }
            else if (inTE.contains("Patterns"))
            {
                NbtList inPatternList = inTE.getList("Patterns", 10);
                NbtList outPatternList;

                outPatternList = processTileEntityTags_1_20_4_to_1_20_5_PatternList(inPatternList, registryLookup);

                Identifier testId = new Identifier("minecraft", "white_banner");
                Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_Each(): inId {} // hash {} (white_banner: {} // hash: {})", inId.toString(), inId.hashCode(), testId.toString(), testId.hashCode());

                if (outPatternList != null && !outPatternList.isEmpty())
                {
                    Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_Each(): outPatternList {}", outPatternList);
                    inTE.remove("Patterns");
                    outTE.put("patterns", outPatternList);
                }
                else
                {
                    Litematica.logger.warn("processTileEntityTags_1_20_4_to_1_20_5_Each(): id {} no components built!", inId.toString());
                }
            }
            else if (inTE.contains("Bees"))
            {
                NbtList inBeeList = inTE.getList("Bees", 10);
                NbtList outBeeList;

                outBeeList = processTileEntityTags_1_20_4_to_1_20_5_BeeList(inBeeList, registryLookup);

                inTE.remove("Bees");
                outTE.put("bees", outBeeList);
            }
            else if (inTE.contains("SkullOwner"))
            {
                NbtCompound inSkullOwner = inTE.getCompound("SkullOwner");
                NbtCompound outProfile;

                outProfile = processTileEntityTags_1_20_4_to_1_20_5_SkullOwner(inSkullOwner,registryLookup);

                inTE.remove("SkullOwner");
                outTE.put("profile", outProfile);
            }
            else
            {
                outTE.copyFrom(inTE);
            }
        }

        return outTE;
    }

    private static NbtList processTileEntityTags_1_20_4_to_1_20_5_ItemList(@Nonnull BlockPos inPos,
                                                                           @Nonnull NbtList inItemList,
                                                                           @Nonnull DynamicRegistryManager registryLookup)
    {
        NbtList outList = new NbtList();

        for (int i = 0; i < inItemList.size(); i++)
        {
            NbtCompound itemEntry = inItemList.getCompound(i);
            NbtCompound newItemEntry = new NbtCompound();

            int itemSlot = itemEntry.getByte("Slot") & 255;
            byte itemCount = itemEntry.getByte("Count");

            if (itemEntry.contains("id"))
            {
                String itemString = itemEntry.getString("id");
                Identifier itemId = new Identifier(itemString);

                newItemEntry.putString("id", itemString);
                newItemEntry.putInt("count", itemCount);
                newItemEntry.putByte("Slot", (byte) itemSlot);

                if (itemEntry.contains("tag"))
                {
                    NbtCompound itemTag = itemEntry.getCompound("tag");
                    NbtCompound outTags;

                    Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_ItemList(): [Slot:{}//Count:{}] id {} // each item NBT tag {}", itemSlot, itemCount, itemId, itemTag.toString());

                    outTags = processTileEntityTags_1_20_4_to_1_20_5_Each(inPos, itemId, itemTag, registryLookup);

                    // FIXME Minecraft outputs it's "Components" in a different format under the Items {} tag!
                    //  It will need a "second tier" filter mechanism, or just ignore the NBTdata inside of ItemStacks from old versions.
                    //newItemEntry.put("components", outTags);
                }
                else
                {
                    Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_ItemList(): [Slot:{}//Count:{}] id {} // each item NBT (all/other) {}", itemSlot, itemCount, itemId, itemEntry.toString());
                }
            }
            else
            {
                Litematica.logger.error("processTileEntityTags_1_20_4_to_1_20_5_ItemList(): [Slot:{}//Count:{}] id is missing!", itemSlot, itemCount);
            }

            outList.add(newItemEntry);
        }

        return outList;
    }

    private static NbtList processTileEntityTags_1_20_4_to_1_20_5_PatternList(@Nonnull NbtList inPatternList,
                                                                              @Nonnull DynamicRegistryManager registryLookup)
    {
        NbtList outList = new NbtList();
        //List<BannerPatternsComponent.Layer> layerList = new ArrayList<>();
        //BannerPatternsComponent patternsComp;

        for (int i = 0; i < inPatternList.size(); i++)
        {
            NbtCompound patternEntry = inPatternList.getCompound(i);
            DyeColor dyeColor = DyeColor.byId(patternEntry.getInt("Color"));
            String patternId = patternEntry.getString("Pattern");
            RegistryEntry<BannerPattern> patternReg = ComponentUtils.getBannerPatternEntryByIdPre1205(patternId, registryLookup);

            if (patternReg != null)
            {
                BannerPatternsComponent.Layer patternLayer = new BannerPatternsComponent.Layer(patternReg, dyeColor);
                NbtCompound compElement = new NbtCompound();

                Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_PatternList(): pattern[{}]: layer {}", i, patternLayer.toString());

                compElement.putString("pattern", patternReg.getIdAsString());
                compElement.putString("color", dyeColor.getName());

                Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_PatternList(): compElement[{}]: {}", i, compElement.toString());

                outList.add(compElement);
            }
            else
            {
                Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_PatternList(): invalid Pattern id {}", patternId);

                outList.add(patternEntry);
            }

            //Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_PatternList(): each pattern NBT {}", patternEntry.toString());

        }
        if (!outList.isEmpty())
        {
            //patternsComp = new BannerPatternsComponent(layerList);

            //Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_PatternList(): patterns component {}", patternsComp.toString());

            Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_PatternList(): outList {}", outList.toString());

            return outList;
        }
        else
        {
            return null;
        }
    }

    private static NbtList processTileEntityTags_1_20_4_to_1_20_5_BeeList(@Nonnull NbtList inBeeList,
                                                                          @Nonnull DynamicRegistryManager registryLookup)
    {
        NbtList outBeeList = new NbtList();

        for (int i = 0; i < inBeeList.size(); i++)
        {
            NbtCompound beeEntry = inBeeList.getCompound(i);
            NbtCompound beeData = new NbtCompound();

            //Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_BeeList(): each pattern NBT {}", beeEntry.toString());

            if (beeEntry.contains("EntityData"))
            {
                NbtCompound beeEnt = beeEntry.getCompound("EntityData").copy();
                //String beeId = beeEntry.getString("id");
                int beeTicksInHive = beeEntry.getInt("TicksInHive");
                int occupationTicks = beeEntry.getInt("MinOccupationTicks");

                Litematica.debugLog("Bee[{}] ticks {}, occupationTicks {}", i, beeTicksInHive, occupationTicks);

                beeData.put("entity_data", beeEnt);
                beeData.putInt("ticks_in_hive", beeTicksInHive);
                beeData.putInt("min_ticks_in_hive", occupationTicks);
                //beeData.putString("id", beeId);

                outBeeList.add(beeData);
            }
        }

        return outBeeList;
    }

    private static NbtCompound processTileEntityTags_1_20_4_to_1_20_5_SkullOwner(@Nonnull NbtCompound inSkullOwner,
                                                                                 @Nonnull DynamicRegistryManager registryLookup)
    {
        NbtCompound skullProperties = new NbtCompound();
        NbtCompound newSkullProfile = new NbtCompound();

        UUID skullUUID = Util.NIL_UUID;
        String skullName = "";
        GameProfile skullProfile;

        //Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_SkullOwner(): in NBT {}", inSkullOwner);

        if (inSkullOwner.contains("Id"))
        {
            skullUUID = inSkullOwner.getUuid("Id");
        }
        if (inSkullOwner.contains("Name"))
        {
            skullName = inSkullOwner.getString("Name");
        }

        Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_SkullOwner(): try uuid: {} // name: {}", skullUUID.toString(), skullName);

        try
        {
            skullProfile = new GameProfile(skullUUID, skullName);
        }
        catch (Exception failure)
        {
            Litematica.logger.warn("processTileEntityTags_1_20_4_to_1_20_5_SkullOwner(): failed too create Game Profile from UUID {} // Name {}", skullUUID, skullName);
            return inSkullOwner;
        }
        if (inSkullOwner.contains("Properties"))
        {
            skullProperties = inSkullOwner.getCompound("Properties");

            //Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_SkullOwner(): old properties in: {}", skullProperties.toString());

            for (String key : skullProperties.getKeys())
            {
                NbtList propList = skullProperties.getList(key, 10);

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
        // Build new "profile" nbt
        newSkullProfile.putString("name", skullProfile.getName());
        newSkullProfile.putUuid("id", skullProfile.getId());

        PropertyMap skullProfileProperties = skullProfile.getProperties();
        NbtList newSkullProperties = new NbtList();

        for (String key : skullProfileProperties.keySet())
        {
            Collection<Property> valueCol = skullProfileProperties.get(key);
            Iterator<Property> iterator = valueCol.iterator();

            while (iterator.hasNext())
            {
                NbtCompound propComp = new NbtCompound();
                Property value = iterator.next();

                propComp.putString("name", value.name());
                propComp.putString("value", value.value());
                if (value.hasSignature())
                {
                    propComp.putString("signature", value.signature());
                }
                newSkullProperties.add(propComp);
            }
        }

        //Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_SkullOwner(): newProperties -> {}", newSkullProperties.toString());

        newSkullProfile.put("properties", newSkullProperties);

        //Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_SkullOwner(): newProfile -> {}", newSkullProfile.toString());

        return newSkullProfile;
    }
}
