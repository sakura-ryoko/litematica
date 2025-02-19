package fi.dy.masa.litematica.schematic;

import java.util.Objects;
import javax.annotation.Nullable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import fi.dy.masa.malilib.util.Schema;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.litematica.util.FileType;

public class SchematicMetadata
{
    private String name = "?";
    private String author = "?";
    private String description = "";
    private Vec3i enclosingSize = Vec3i.ZERO;
    private long timeCreated;
    private long timeModified;
    protected int minecraftDataVersion;
    protected int schematicVersion;
    protected Schema schema;
    protected FileType type;
    private int regionCount;
    protected int entityCount;
    protected int blockEntityCount;
    private int totalVolume = -1;
    private int totalBlocks = -1;
    private boolean modifiedSinceSaved;
    @Nullable protected int[] thumbnailPixelData;

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
        return this.thumbnailPixelData;
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
        this.thumbnailPixelData = pixelData;
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
            this.thumbnailPixelData = new int[other.thumbnailPixelData.length];
            System.arraycopy(other.thumbnailPixelData, 0, this.thumbnailPixelData, 0, this.thumbnailPixelData.length);
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

        nbt.put("EnclosingSize", NbtUtils.createBlockPosTag(this.enclosingSize));

        if (this.thumbnailPixelData != null)
        {
            nbt.putIntArray("PreviewImageData", this.thumbnailPixelData);
        }

        return nbt;
    }

    public void readFromNBT(NbtCompound nbt)
    {
        this.name = nbt.getString("Name");
        this.author = nbt.getString("Author");
        this.description = nbt.getString("Description");
        this.regionCount = nbt.getInt("RegionCount");
        this.timeCreated = nbt.getLong("TimeCreated");
        this.timeModified = nbt.getLong("TimeModified");

        if (nbt.contains("TotalVolume", Constants.NBT.TAG_ANY_NUMERIC))
        {
            this.totalVolume = nbt.getInt("TotalVolume");
        }

        if (nbt.contains("TotalBlocks", Constants.NBT.TAG_ANY_NUMERIC))
        {
            this.totalBlocks = nbt.getInt("TotalBlocks");
        }

        if (nbt.contains("EnclosingSize", Constants.NBT.TAG_COMPOUND))
        {
            Vec3i size = NbtUtils.readVec3iFromTag(nbt.getCompound("EnclosingSize"));

            if (size != null)
            {
                this.enclosingSize = size;
            }
        }

        if (nbt.contains("PreviewImageData", Constants.NBT.TAG_INT_ARRAY))
        {
            this.thumbnailPixelData = nbt.getIntArray("PreviewImageData");
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
