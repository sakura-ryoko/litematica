package fi.dy.masa.litematica.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import fi.dy.masa.litematica.Litematica;
import net.minecraft.block.entity.*;
import net.minecraft.component.type.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Util;

import javax.annotation.Nullable;
import java.util.*;

/**
 * This file is meant to be a new central place for managing Component <-> NBT data
 */
public class ComponentUtils
{
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
        //Litematica.debugLog("getSkullProfileFromProfile(): try uuid: {} // name: {}", skullUUID.toString(), skullName);
        try
        {
            skullProfile = new GameProfile(skullUUID, skullName);
        }
        catch (Exception failure)
        {
            Litematica.debugLog("getSkullProfileFromProfile() failed to retrieve GameProfile");
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
}
