package fi.dy.masa.litematica.schematic.transmit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;

public class SchematicBufferManager
{
    private final ConcurrentHashMap<Long, SchematicBuffer> fileBuffers;
    private final ConcurrentHashMap<Long, CompoundTag> optionalNbt;

    public SchematicBufferManager()
    {
        this.fileBuffers = new ConcurrentHashMap<>(16, 0.9f, 1);
        this.optionalNbt = new ConcurrentHashMap<>(16, 0.9f, 1);
    }

    public void createBuffer(int totalExpectedSlices, long totalExpectedSize, final long sessionKey)
    {
        this.createBuffer(totalExpectedSlices, totalExpectedSize, FileType.LITEMATICA_SCHEMATIC, sessionKey, null);
    }

    public void createBuffer(int totalExpectedSlices, long totalExpectedSize, final long sessionKey, @Nullable CompoundTag optional)
    {
        this.createBuffer(totalExpectedSlices, totalExpectedSize, FileType.LITEMATICA_SCHEMATIC, sessionKey, optional);
    }

    public void createBuffer(int totalExpectedSlices, long totalExpectedSize, FileType type, final long sessionKey, @Nullable CompoundTag optional)
    {
        if (this.fileBuffers.containsKey(sessionKey) || this.optionalNbt.containsKey(sessionKey))
        {
            Litematica.LOGGER.warn("createBuffer: Cannot create a new buffer for an existing session key!");
            return;
        }

        SchematicBuffer newBuf = new SchematicBuffer(totalExpectedSlices, totalExpectedSize, type);
        this.fileBuffers.put(sessionKey, newBuf);

        if (optional != null && !optional.isEmpty())
        {
            this.optionalNbt.put(sessionKey, optional.copy());
        }
    }

    private @Nullable SchematicBuffer getBuffer(final long sessionKey)
    {
        if (this.fileBuffers.containsKey(sessionKey))
        {
            return this.fileBuffers.get(sessionKey);
        }

        return null;
    }

    public CompoundTag getOptionalNbt(final long sessionKey)
    {
        if (this.optionalNbt.containsKey(sessionKey))
        {
            return this.optionalNbt.get(sessionKey);
        }

        return new CompoundTag();
    }

    public void receiveSlice(final long sessionKey, final int slice, byte[] dataIn, final int size)
    {
        if (this.fileBuffers.containsKey(sessionKey))
        {
            this.fileBuffers.get(sessionKey).receiveSlice(slice, new SchematicBuffer.Slice(dataIn, size));
        }
        else
        {
            Litematica.LOGGER.error("receiveSlice: Error; cannot receive a slice for a non-existing session");
        }
    }

    public void cancelBuffer(final long sessionKey)
    {
        if (this.fileBuffers.containsKey(sessionKey))
        {
            try
            {
                this.fileBuffers.remove(sessionKey);
            }
            catch (Exception ignored) {}
        }

        this.optionalNbt.remove(sessionKey);
    }

    public @Nullable LitematicaSchematic finishBuffer(final long sessionKey, @Nullable Path dir)
    {
        if (this.fileBuffers.containsKey(sessionKey))
        {
            SchematicBuffer buffer = this.fileBuffers.get(sessionKey);

            if (dir == null)
            {
                dir = DataManager.getSchematicTransmitDirectory();
            }

            Path file = buffer.writeFile(dir);

            if (file == null)
            {
                Litematica.LOGGER.error("finishBuffer: Failed writing Schematic Buffer to file: '{}'", buffer.getFileNameWithExt());
                return null;
            }

            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(dir, buffer.getFileName(), buffer.getType());
            this.cancelBuffer(sessionKey);

            if (schematic == null)
            {
                try
                {
                    Files.delete(file);
                }
                catch (Exception ignored) {}
            }

            return schematic;
        }

        return null;
    }
}
