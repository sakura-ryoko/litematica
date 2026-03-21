package fi.dy.masa.litematica.materials;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.*;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;

import fi.dy.masa.malilib.util.time.TimeFormat;
import fi.dy.masa.litematica.Litematica;

public class MaterialListJsonExporter
{
	private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final List<Entry> results;
	private String name;
	private String title;
	private int multiplier;

	public MaterialListJsonExporter(MaterialListBase materialList)
	{
		this();
		this.readMaterialList(materialList);
	}

	public MaterialListJsonExporter()
	{
		this.results = new ArrayList<>();
		this.name = "";
		this.title = "";
		this.multiplier = 1;
	}

	public void putEntry(Entry entry)
	{
		this.results.add(entry);
	}

	public boolean isEmpty()
	{
		return this.results.isEmpty();
	}

	public int size()
	{
		return this.results.size();
	}

	public void clear()
	{
		this.results.clear();
	}

	public boolean readMaterialList(MaterialListBase materialList)
	{
		List<MaterialListEntry> materials = materialList.getMaterialsFiltered(false);
		final int mul = materialList.getMultiplier();

		materials.sort(new MaterialListSorter(materialList));
		this.multiplier = mul;
		this.name = materialList.getName();
		this.title = materialList.getTitle();

		if (materials.isEmpty())
		{
			return false;
		}

		materials.forEach(
				entry ->
				{
					int total = entry.getCountTotal() * mul;
					int missing = mul > 1 ? total : entry.getCountMissing();
					int mismatched = entry.getCountMismatched() * mul;
					int available = entry.getCountAvailable();

					this.putEntry(new Entry(entry.getStack().typeHolder(), total, missing, mismatched, available));
				}
		);

		return this.size() > 0;
	}

	public JsonElement toJson(RegistryAccess registry)
	{
		JsonArray arr = new JsonArray();

		try
		{
			this.results.forEach(
					entry ->
							arr.add(Entry.CODEC.encodeStart(
									registry.createSerializationContext(JsonOps.INSTANCE), entry).getPartialOrThrow()
							)
			);

			return arr;
		}
		catch (Exception e)
		{
			Litematica.LOGGER.error("MaterialListJsonExporter: Exception writing Cache to JSON; {}", e.getMessage());
		}

		return new JsonArray();
	}

	public boolean writeCacheToFile(Path file)
	{
		return this.writeCacheToFile(file, Minecraft.getInstance());
	}

	public boolean writeCacheToFile(Path file, Minecraft mc)
	{
		return this.writeCacheToFile(file, TimeFormat.RFC1123, mc);
	}

	public boolean writeCacheToFile(Path file, TimeFormat fmt, Minecraft mc)
	{
		if (this.isEmpty() || mc.level == null)
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
				Litematica.LOGGER.error("MaterialListJsonExporter#writeCacheToFile(): Exception deleting file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
				return false;
			}
		}

		try
		{
			JsonElement arr = this.toJson(mc.level.registryAccess());
			JsonObject obj = new JsonObject();

			obj.addProperty("Name", this.name);
			obj.addProperty("Title", this.title);
			obj.addProperty("Multiplier", this.multiplier);
			obj.addProperty("Date", fmt.formatNow());
			obj.add("Materials", arr);

			Files.writeString(file, GSON.toJson(obj));
			Litematica.LOGGER.info("MaterialListJsonExporter#writeCacheToFile(): Exported Materials file '{}' successfully.", file.toAbsolutePath().toString());
			return true;
		}
		catch (IOException err)
		{
			Litematica.LOGGER.error("MaterialListJsonExporter#writeCacheToFile(): Exception writing file '{}'; {}", file.toAbsolutePath().toString(), err.getLocalizedMessage());
			return false;
		}
	}

	public record Entry(Holder<Item> resultItem, int total, int missing, int mismatched, int available)
	{
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(
				inst -> inst.group(
						Item.CODEC.fieldOf("Item").forGetter(get -> get.resultItem),
						PrimitiveCodec.INT.fieldOf("Total").forGetter(get -> get.total),
						PrimitiveCodec.INT.fieldOf("Missing").forGetter(get -> get.missing),
						PrimitiveCodec.INT.fieldOf("Mismatched").forGetter(get -> get.mismatched),
						PrimitiveCodec.INT.fieldOf("Available").forGetter(get -> get.available)
				).apply(inst, Entry::new)
		);
	}
}
