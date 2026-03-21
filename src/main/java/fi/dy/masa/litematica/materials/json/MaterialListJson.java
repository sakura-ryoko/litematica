package fi.dy.masa.litematica.materials.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;

public class MaterialListJson
{
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<MaterialListJsonBase> data;

    public MaterialListJson()
    {
        this.data = new ArrayList<>();
    }

    public List<MaterialListJsonBase> getMaterials()
    {
        return this.data;
    }

    public boolean readMaterialListAll(MaterialListBase materialList, MaterialListJsonCache cache, boolean craftingOnly)
    {
        ImmutableList<MaterialListEntry> materials = materialList.getMaterialsAll();

        if (materials.isEmpty())
        {
            return false;
        }

        this.data.clear();

        materials.forEach(
                (entry) ->
                {
                    Holder<Item> resultItem = entry.getStack().typeHolder();
                    final int total = (entry.getStack().getCount() * entry.getCountTotal());
                    MaterialListJsonBase base = new MaterialListJsonBase(resultItem, total, null, craftingOnly);

                    this.data.add(base);
                    cache.buildStepsBase(base, new ArrayList<>(), new MaterialListJsonCache.Result(resultItem, total));
                }
        );

        cache.simplifyFlatEntrySteps();
        cache.repackCombinedEntries();

        return true;
    }

    public boolean readMaterialListMissingOnly(MaterialListBase materialList, MaterialListJsonCache cache, boolean craftingOnly)
    {
        List<MaterialListEntry> materials = materialList.getMaterialsMissingOnly(false);

        if (materials.isEmpty())
        {
            return false;
        }

        this.data.clear();

        materials.forEach(
                (entry) ->
                {
                    Holder<Item> resultItem = entry.getStack().typeHolder();
                    final int total = (entry.getStack().getCount() * entry.getCountTotal());
                    MaterialListJsonBase base = new MaterialListJsonBase(resultItem, total, null, craftingOnly);

                    this.data.add(base);
                    cache.buildStepsBase(base, new ArrayList<>(), new MaterialListJsonCache.Result(resultItem, total));
                }
        );

        cache.simplifyFlatEntrySteps();
        cache.repackCombinedEntries();

        return true;
    }

    public boolean writeRecipeDetailJson(Path file, Minecraft mc)
    {
        if (this.data.isEmpty() || mc.level == null)
        {
            return false;
        }

        if (Files.exists(file))
        {
            try
            {
                Files.delete(file);
            }
            catch (IOException err)
            {
                Litematica.LOGGER.error("MaterialListJson#toJson(): Exception deleting file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
                return false;
            }
        }

        try
        {
            Files.writeString(file, GSON.toJson(this.toJson(mc.level.registryAccess())));
            Litematica.LOGGER.info("MaterialListJson#toJson(): Exported Materials file '{}' successfully.", file.toAbsolutePath().toString());
            return true;
        }
        catch (IOException err)
        {
            Litematica.LOGGER.error("MaterialListJson#toJson(): Exception writing file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
            return false;
        }
    }

    public boolean writeCacheFlatJson(MaterialListJsonCache cache, Path file, Minecraft mc)
    {
        if (cache.isEmptyFlat() || mc.level == null)
        {
            return false;
        }

        if (Files.exists(file))
        {
            try
            {
                Files.delete(file);
            }
            catch (IOException err)
            {
                Litematica.LOGGER.error("MaterialListJson#writeCacheFlatJson(): Exception deleting file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
                return false;
            }
        }

        try
        {
            Files.writeString(file, GSON.toJson(cache.toFlatJson(mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE))));
            Litematica.LOGGER.info("MaterialListJson#writeCacheFlatJson(): Exported Materials Cache file '{}' successfully.", file.toAbsolutePath().toString());
            return true;
        }
        catch (IOException err)
        {
            Litematica.LOGGER.error("MaterialListJson#writeCacheFlatJson(): Exception writing file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
            return false;
        }
    }

    public boolean writeCacheCombinedJson(MaterialListJsonCache cache, Path file, Minecraft mc)
    {
        if (cache.isEmptyCombined() || mc.level == null)
        {
            return false;
        }

        if (Files.exists(file))
        {
            try
            {
                Files.delete(file);
            }
            catch (IOException err)
            {
                Litematica.LOGGER.error("MaterialListJson#writeCacheCombinedJson(): Exception deleting file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
                return false;
            }
        }

        try
        {
            Files.writeString(file, GSON.toJson(cache.toCombinedJson(mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE))));
            Litematica.LOGGER.info("MaterialListJson#writeCacheCombinedJson(): Exported Materials Cache file '{}' successfully.", file.toAbsolutePath().toString());
            return true;
        }
        catch (IOException err)
        {
            Litematica.LOGGER.error("MaterialListJson#writeCacheCombinedJson(): Exception writing file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
            return false;
        }
    }

    public JsonElement toJson(RegistryAccess registry)
    {
        RegistryOps<?> ops = registry.createSerializationContext(JsonOps.INSTANCE);
        JsonArray arr = new JsonArray();

        this.data.forEach(
                (entry) ->
                        arr.add(entry.toJson(ops))
        );

        return arr;
    }

    public void clear()
    {
        this.data.clear();
    }
}
