package fi.dy.masa.litematica.schematic.conversion;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.util.ComponentUtils;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * I was coding with this until masa taught me to research how to use the Vanilla Data Fixer.
 * I would keep it for now for reference.
 */
@Deprecated
public class SchematicConversionComponents
{
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

                if (customName != null)
                {
                    Litematica.logger.warn("processTileEntityTags_1_20_4_to_1_20_5_Each(): CustomName detected: {}", customName.getLiteralString());
                }
                else
                {
                    Litematica.logger.error("processTileEntityTags_1_20_4_to_1_20_5_Each(): CustomName detected but value is null");
                }
            }
            if (inTE.contains("Items"))
            {
                NbtList inItemList = inTE.getList("Items", 10);
                NbtList outItemList;

                outItemList = processTileEntityTags_1_20_4_to_1_20_5_ItemList(inPos, inItemList, registryLookup);

                inTE.remove("Items");
                outTE.put("Items", outItemList);
            }
            // Decorated pots uses the "item" tag
            else if (inTE.contains("item"))
            {
                NbtCompound inItemPot = inTE.getCompound("item");
                NbtCompound outItemPot;

                outItemPot = processTileEntityTags_1_20_4_to_1_20_5_PotItem(inPos, inItemPot, registryLookup);

                Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_Each(): potItem out: {}", outItemPot.toString());

                inTE.remove("item");
                outTE.put("item", outItemPot);
            }
            else if (inTE.contains("Patterns"))
            {
                NbtList inPatternList = inTE.getList("Patterns", 10);
                NbtList outPatternList;

                outPatternList = processTileEntityTags_1_20_4_to_1_20_5_PatternList(inPatternList, registryLookup);

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

                outProfile = processTileEntityTags_1_20_4_to_1_20_5_SkullOwner(inSkullOwner, registryLookup);

                inTE.remove("SkullOwner");
                outTE.put("profile", outProfile);
            }
            else if (inTE.contains("pages", 9) && inTE.contains("author") && inTE.contains("title"))
            {
                NbtList inPages = inTE.getList("pages", 8);
                NbtCompound bookData = new NbtCompound();
                NbtList outPagesWritten;
                String author = inTE.getString("author");
                String title = inTE.getString("title");

                bookData.putString("author", author);
                bookData.putString("title", title);
                if (inTE.contains("generation"))
                {
                    bookData.putInt("generation",inTE.getInt("generation"));
                    inTE.remove("generation");
                }
                if (inTE.contains("resolved"))
                {
                    bookData.putBoolean("resolved",inTE.getBoolean("resolved"));
                    inTE.remove("resolved");
                }
                if (inTE.contains("filtered_pages", 10))
                {
                    NbtCompound filter_pages = inTE.getCompound("filtered_pages");
                    bookData.put("filtered_pages", filter_pages);
                    inTE.remove("filtered_pages");
                }

                outPagesWritten = processTileEntityTags_1_20_4_to_1_20_5_Pages_Written(inPages, bookData, registryLookup);

                inTE.remove("pages");
                inTE.remove("author");
                inTE.remove("title");
                outTE.put("written_book_content", outPagesWritten);
            }
            else if (inTE.contains("pages", 9))
            {
                NbtList inPages = inTE.getList("pages", 8);
                NbtList outPages;

                outPages = processTileEntityTags_1_20_4_to_1_20_5_Pages(inPages, registryLookup);

                inTE.remove("pages");
                outTE.put("writable_book_content", outPages);
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
                    NbtCompound outComps;

                    outTags = processTileEntityTags_1_20_4_to_1_20_5_Each(inPos, itemId, itemTag, registryLookup);

                    // Minecraft outputs it's "Components" in a different format under the Items {} tag!
                    //  It will need a "second tier" filter mechanism, or just ignore the NBT inside of ItemStacks from old versions.
                    outComps = processTileEntityTags_1_20_4_to_1_20_5_EachComponent(inPos, itemId, outTags, registryLookup);

                    itemEntry.remove("tag");
                    newItemEntry.put("components", outComps);
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

    private static NbtCompound processTileEntityTags_1_20_4_to_1_20_5_EachComponent(@Nonnull BlockPos inPos,
                                                                                    @Nonnull Identifier inId,
                                                                                    @Nonnull NbtCompound inTags,
                                                                                    @Nonnull DynamicRegistryManager registryLookup)
    {
        NbtCompound compList = new NbtCompound();

        for (String key : inTags.getKeys())
        {
            NbtCompound component = inTags.getCompound(key);

            switch (key)
            {
                case "profile":
                {
                    Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_EachComponent(): item id {} // translation profile -> \"minecraft:profile\"", inId.toString());

                    compList.put("minecraft:profile", component);
                    break;
                }
                case "patterns":
                {
                    Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_EachComponent(): item id {} // translation patterns -> \"minecraft:banner_patterns\"", inId.toString());

                    compList.put("minecraft:banner_patterns", component);
                    break;
                }
                case "writeable_book_content":
                {
                    Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_EachComponent(): item id {} // translation writeable_book_content -> \"minecraft:writeable_book_content\"", inId.toString());

                    compList.put("minecraft:writeable_book_content", component);
                    break;
                }
                case "written_book_content":
                {
                    Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_EachComponent(): item id {} // translation written_book_content -> \"minecraft:written_book_content\"", inId.toString());

                    compList.put("minecraft:written_book_content", component);
                    break;
                }
                default:
                {
                    Litematica.logger.warn("processTileEntityTags_1_20_4_to_1_20_5_EachComponent(): unhandled components key tag: {}", key);
                    break;
                }
            }
        }
        if (!compList.isEmpty())
        {
            Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_EachComponent(): item id {} // End of components list", inId.toString());

            return compList;
        }
        else
        {
            Litematica.logger.warn("processTileEntityTags_1_20_4_to_1_20_5_EachComponent(): item id {} // Empty components list", inId.toString());

            return compList;
        }
    }

    private static NbtCompound processTileEntityTags_1_20_4_to_1_20_5_PotItem(@Nonnull BlockPos inPos,
                                                                              @Nonnull NbtCompound inItemPot,
                                                                              @Nonnull DynamicRegistryManager registryLookup)
    {
        NbtCompound outItemPot = new NbtCompound();
        String itemIdString;
        Identifier itemId;
        int itemCount;

        if (inItemPot.contains("id"))
        {
            itemIdString = inItemPot.getString("id");
            itemId = new Identifier(itemIdString);
            itemCount = inItemPot.getByte("Count");

            Litematica.debugLog("processTileEntityTags_1_20_4_to_1_20_5_PotItem(): item id {}, count {}", itemId.toString(), itemCount);

            if (inItemPot.contains("tag"))
            {
                NbtCompound inPotTag = inItemPot.getCompound("tag");
                NbtCompound outPotTag;
                NbtCompound outPotComp;

                outPotTag = processTileEntityTags_1_20_4_to_1_20_5_Each(inPos, itemId, inPotTag, registryLookup);
                outPotComp = processTileEntityTags_1_20_4_to_1_20_5_EachComponent(inPos, itemId, outPotTag, registryLookup);

                outItemPot.put("components", outPotComp);
            }

            outItemPot.putString("id", itemIdString);
            outItemPot.putInt("count", itemCount);
        }
        else
        {
            Litematica.logger.error("processTileEntityTags_1_20_4_to_1_20_5_PotItem(): item \"id\" tag is missing");
            return inItemPot;
        }

        return outItemPot;
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
                //BannerPatternsComponent.Layer patternLayer = new BannerPatternsComponent.Layer(patternReg, dyeColor);
                NbtCompound compElement = new NbtCompound();

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
        }
        if (!outList.isEmpty())
        {
            //patternsComp = new BannerPatternsComponent(layerList);

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
                /*
                if (value.hasSignature())
                {
                    propComp.putString("signature", value.signature());
                }
                 */
                newSkullProperties.add(propComp);
            }
        }

        newSkullProfile.put("properties", newSkullProperties);

        return newSkullProfile;
    }

    private static NbtList processTileEntityTags_1_20_4_to_1_20_5_Pages(NbtList inPages, DynamicRegistryManager registryLookup)
    {
        NbtList outPages = new NbtList();
        int pageCount = inPages.size();

        for (int i = 0; i < pageCount; i++)
        {
            NbtCompound outPage = inPages.getCompound(i);
        }

        return inPages;
    }

    private static NbtList processTileEntityTags_1_20_4_to_1_20_5_Pages_Written(NbtList inPages, NbtCompound bookData,
                                                                                DynamicRegistryManager registryLookup)
    {
        // This is where I stopped writing this code, working on the Book NBT.
        return null;
    }
}
