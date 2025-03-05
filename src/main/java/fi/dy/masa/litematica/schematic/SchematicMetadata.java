package fi.dy.masa.litematica.schematic;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.Schema;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.litematica.util.FileType;

public class SchematicMetadata
{
    public static final Codec<SchematicMetadata> CODEC = RecordCodecBuilder.create(
            inst -> inst.group(
                    PrimitiveCodec.STRING.fieldOf("Name").forGetter(get -> get.name),
                    PrimitiveCodec.STRING.fieldOf("Author").forGetter(get -> get.author),
                    PrimitiveCodec.STRING.fieldOf("Description").forGetter(get -> get.description),
                    PrimitiveCodec.INT.fieldOf("RegionCount").forGetter(get -> get.regionCount),
                    PrimitiveCodec.INT.fieldOf("TotalVolume").forGetter(get -> get.totalVolume),
                    PrimitiveCodec.INT.fieldOf("TotalBlocks").forGetter(get -> get.totalBlocks),
                    PrimitiveCodec.LONG.fieldOf("TimeCreated").forGetter(get -> get.timeCreated),
                    PrimitiveCodec.LONG.fieldOf("TimeModified").forGetter(get -> get.timeModified),
                    Vec3i.CODEC.fieldOf("EnclosingSize").forGetter(get -> get.enclosingSize),
                    PrimitiveCodec.INT_STREAM.optionalFieldOf("PreviewImageData", null).forGetter(get -> get.thumbnailPixelData)
            ).apply(inst, SchematicMetadata::new)
    );
    private String name;
    private String author;
    private String description;
    private Vec3i enclosingSize;
    private long timeCreated;
    private long timeModified;
    protected int minecraftDataVersion;
    protected int schematicVersion;
    protected Schema schema;
    protected FileType type;
    private int regionCount;
    protected int entityCount;
    protected int blockEntityCount;
    private int totalVolume;
    private int totalBlocks;
    private boolean modifiedSinceSaved;
    @Nullable protected IntStream thumbnailPixelData;

    public SchematicMetadata()
    {
        this.name = "?";
        this.author = "?";
        this.description = "";
        this.enclosingSize = Vec3i.ZERO;
        this.totalVolume = -1;
        this.totalBlocks = -1;
        this.thumbnailPixelData = null;
    }

    private SchematicMetadata(String name, String author, String desc, int regionCount, int volume, int blocks, long created, long modified, Vec3i size, @Nullable IntStream thumbnail)
    {
        this.name = name;
        this.author = author;
        this.description = desc;
        this.regionCount = regionCount;
        this.totalVolume = volume;
        this.totalBlocks = blocks;
        this.timeCreated = created;
        this.timeModified = modified;
        this.thumbnailPixelData = thumbnail;
    }

    public String getName()
    {
        return this.name;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public String getDescription()
    {
        return this.description;
    }

    @Nullable
    public int[] getPreviewImagePixelData()
    {
        if (thumbnailPixelData == null) return null;

        int[] result = this.thumbnailPixelData.toArray();
        this.thumbnailPixelData = Arrays.stream(result.clone());
        return result;
    }

    public int getRegionCount()
    {
        return this.regionCount;
    }

    public int getTotalVolume()
    {
        return this.totalVolume;
    }

    public int getTotalBlocks()
    {
        return this.totalBlocks;
    }

    public int getEntityCount()
    {
        return this.entityCount;
    }

    public int getBlockEntityCount()
    {
        return this.blockEntityCount;
    }

    public Vec3i getEnclosingSize()
    {
        return this.enclosingSize;
    }

    public Vec3i getEnclosingSizeAsVanilla()
    {
        return this.enclosingSize;
    }

    public BlockPos getEnclosingSizeAsBlockPos()
    {
        return new BlockPos(this.enclosingSize);
    }

    public long getTimeCreated()
    {
        return this.timeCreated;
    }

    public long getTimeModified()
    {
        return this.timeModified;
    }

    public int getSchematicVersion()
    {
        return this.schematicVersion;
    }

    public int getMinecraftDataVersion()
    {
        return this.minecraftDataVersion;
    }

    public SchematicSchema getSchematicSchema()
    {
        return new SchematicSchema(this.schematicVersion, this.minecraftDataVersion);
    }

    public Schema getSchema()
    {
        return this.schema;
    }

    public String getMinecraftVersion()
    {
        return this.schema.getString();
    }

    public String getSchemaString()
    {
        return this.schema.toString();
    }

    public FileType getFileType()
    {
        return Objects.requireNonNullElse(this.type, FileType.UNKNOWN);
    }

    public boolean hasBeenModified()
    {
        return this.timeCreated != this.timeModified;
    }

    public boolean wasModifiedSinceSaved()
    {
        return this.modifiedSinceSaved;
    }

    public void setModifiedSinceSaved()
    {
        this.modifiedSinceSaved = true;
    }

    public void clearModifiedSinceSaved()
    {
        this.modifiedSinceSaved = false;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setAuthor(String author)
    {
        this.author = author;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setPreviewImagePixelData(@Nullable int[] pixelData)
    {
        if (pixelData == null)
        {
            this.thumbnailPixelData = null;
        }
        else
        {
            this.thumbnailPixelData = IntStream.of(pixelData);
        }
    }

    public void setRegionCount(int regionCount)
    {
        this.regionCount = regionCount;
    }

    public void setTotalVolume(int totalVolume)
    {
        this.totalVolume = totalVolume;
    }

    public void setTotalBlocks(int totalBlocks)
    {
        this.totalBlocks = totalBlocks;
    }

    public void setEnclosingSize(Vec3i enclosingSize)
    {
        this.enclosingSize = enclosingSize;
    }

    public void setEnclosingSize(BlockPos enclosingSize)
    {
        this.enclosingSize = enclosingSize;
    }

    public void setTimeCreated(long timeCreated)
    {
        this.timeCreated = timeCreated;
    }

    public void setTimeModified(long timeModified)
    {
        this.timeModified = timeModified;
    }

    public void setTimeModifiedToNow()
    {
        this.timeModified = System.currentTimeMillis();
    }

    public void setTimeModifiedToNowIfNotRecentlyCreated()
    {
        long currentTime = System.currentTimeMillis();

        // Allow 10 minutes to set the description and thumbnail image etc.
        // without marking the schematic as modified
        if (currentTime - this.timeCreated > 10L * 60L * 1000L)
        {
            this.timeModified = currentTime;
        }
    }

    public void setSchematicVersion(int version)
    {
        this.schematicVersion = version;
    }

    public void setMinecraftDataVersion(int minecraftDataVersion)
    {
        this.minecraftDataVersion = minecraftDataVersion;
        this.schema = Schema.getSchemaByDataVersion(this.minecraftDataVersion);
    }

    public void setSchema()
    {
        this.schema = Schema.getSchemaByDataVersion(this.minecraftDataVersion);
    }

    public void setFileType(FileType type)
    {
        this.type = type;
    }

    public void copyFrom(SchematicMetadata other)
    {
        this.name = other.name;
        this.author = other.author;
        this.description = other.description;
        this.enclosingSize = other.enclosingSize;
        this.timeCreated = other.timeCreated;
        this.timeModified = other.timeModified;
        this.regionCount = other.regionCount;
        this.totalVolume = other.totalVolume;
        this.totalBlocks = other.totalBlocks;
        this.modifiedSinceSaved = false;

        this.schematicVersion = other.schematicVersion;
        this.minecraftDataVersion = other.minecraftDataVersion;
        this.schema = Schema.getSchemaByDataVersion(other.minecraftDataVersion);
        this.type = other.getFileType();

        if (other.thumbnailPixelData != null)
        {
            int[] result = other.thumbnailPixelData.toArray();
            //int[] result = new int[temp.length];
            //System.arraycopy(temp, 0, result, 0, temp.length);
            this.thumbnailPixelData = IntStream.of(result);
        }
        else
        {
            this.thumbnailPixelData = null;
        }
    }

    public NbtCompound writeToNBT()
    {
        NbtCompound nbt = new NbtCompound();

        nbt.putString("Name", this.name);
        nbt.putString("Author", this.author);
        nbt.putString("Description", this.description);

        if (this.regionCount > 0)
        {
            nbt.putInt("RegionCount", this.regionCount);
        }

        if (this.totalVolume > 0)
        {
            nbt.putInt("TotalVolume", this.totalVolume);
        }

        if (this.totalBlocks >= 0)
        {
            nbt.putInt("TotalBlocks", this.totalBlocks);
        }

        if (this.timeCreated > 0)
        {
            nbt.putLong("TimeCreated", this.timeCreated);
        }

        if (this.timeModified > 0)
        {
            nbt.putLong("TimeModified", this.timeModified);
        }

        nbt.put("EnclosingSize", NbtUtils.createVec3iTag(this.enclosingSize));

        if (this.thumbnailPixelData != null)
        {
            int[] result = this.thumbnailPixelData.toArray();
            //int[] result = new int[temp.length];
            //System.arraycopy(temp, 0, result, 0, temp.length);

            if (result.length > 0)
            {
                nbt.putIntArray("PreviewImageData", result);
                this.thumbnailPixelData = IntStream.of(result);
            }
            else
            {
                this.thumbnailPixelData = null;
            }
        }

        return nbt;
    }

    public void readFromNBT(NbtCompound nbt)
    {
        this.name = nbt.getString("Name", "?");
        this.author = nbt.getString("Author", "?");
        this.description = nbt.getString("Description", "");
        this.regionCount = nbt.getInt("RegionCount", 0);
        this.timeCreated = nbt.getLong("TimeCreated", -1L);
        this.timeModified = nbt.getLong("TimeModified", -1L);

        if (nbt.contains("TotalVolume"))
        {
            this.totalVolume = nbt.getInt("TotalVolume", 0);
        }

        if (nbt.contains("TotalBlocks"))
        {
            this.totalBlocks = nbt.getInt("TotalBlocks", 0);
        }

        if (nbt.contains("EnclosingSize"))
        {
            Vec3i size = NbtUtils.readVec3iFromTag(nbt.getCompoundOrEmpty("EnclosingSize"));

            if (size != null)
            {
                this.enclosingSize = size;
            }
        }

        if (nbt.contains("PreviewImageData"))
        {
            this.thumbnailPixelData = Arrays.stream(nbt.getIntArray("PreviewImageData").orElse(new int[0]));
        }
        else
        {
            this.thumbnailPixelData = null;
        }
    }

    /**
     * FOR DEBUGGING PURPOSES ONLY
     *
     * @return ()
     */
    public NbtCompound writeToNbtExtra()
    {
        NbtCompound nbt = this.writeToNBT();

        nbt.putString("FileType", this.type.name());

        if (this.minecraftDataVersion > 0)
        {
            nbt.putInt("MinecraftDataVersion", this.minecraftDataVersion);
        }

        if (this.schematicVersion > 0)
        {
            nbt.putInt("SchematicVersion", this.schematicVersion);
        }

        if (this.schema != null)
        {
            nbt.putString("Schema", this.schema.toString());
        }

        if (this.entityCount > 0)
        {
            nbt.putInt("EntityCount", this.entityCount);
        }

        if (this.blockEntityCount > 0)
        {
            nbt.putInt("BlockEntityCount", this.blockEntityCount);
        }

        nbt.putBoolean("IsModified", this.modifiedSinceSaved);

        return nbt;
    }

    /**
     * FOR DEBUGGING PURPOSES ONLY
     *
     * @return ()
     */
    @Override
    public String toString()
    {
        NbtCompound nbt = this.writeToNbtExtra();

        if (nbt.contains("PreviewImageData"))
        {
            nbt.remove("PreviewImageData");
            nbt.putBoolean("PreviewImageData", true);
        }

        return "SchematicMetadata[" + nbt.toString() + "]";
    }

    /**
     * FOR DEBUGGING PURPOSES ONLY
     *
     */
    public void dumpMetadata()
    {
        System.out.print ("SchematicMetadata() DUMP -->\n");
        System.out.printf("   %s\n", this.toString());
        System.out.print ("<END>\n");
    }
}
