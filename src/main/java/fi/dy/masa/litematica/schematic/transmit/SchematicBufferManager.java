package fi.dy.masa.litematica.schematic.transmit;

import java.nio.file.Path;
import java.util.HashMap;
import javax.annotation.Nullable;

import net.minecraft.nbt.NbtCompound;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;

public class SchematicBufferManager
{
    private final HashMap<Long, SchematicBuffer> fileBuffers;
    private final HashMap<Long, NbtCompound> optionalNbt;

    public SchematicBufferManager()
    {
        this.fileBuffers = new HashMap<>();
        this.optionalNbt = new HashMap<>();
    }

    public void createBuffer(String name, final long sessionKey)
    {
        this.createBuffer(name, FileType.LITEMATICA_SCHEMATIC, sessionKey, null);
    }

    public void createBuffer(String name, final long sessionKey, @Nullable NbtCompound optional)
    {
        this.createBuffer(name, FileType.LITEMATICA_SCHEMATIC, sessionKey, optional);
    }

    public void createBuffer(String name, FileType type, final long sessionKey, @Nullable NbtCompound optional)
    {
        if (this.fileBuffers.containsKey(sessionKey) || this.optionalNbt.containsKey(sessionKey))
        {
            Litematica.LOGGER.warn("createBuffer: Cannot create a new buffer for an existing session key!");
            return;
        }

        SchematicBuffer newBuf = new SchematicBuffer(name, type);
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

    public NbtCompound getOptionalNbt(final long sessionKey)
    {
        if (this.optionalNbt.containsKey(sessionKey))
        {
            return this.optionalNbt.get(sessionKey);
        }

        return new NbtCompound();
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
            try (SchematicBuffer buffer = this.fileBuffers.remove(sessionKey))
            {
                buffer.close();
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
                Litematica.LOGGER.error("finishBuffer: Failed writing Schematic Buffer to file: '{}'", buffer.getFileName());
                return null;
            }

            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(dir, buffer.getName(), buffer.getType());
            this.cancelBuffer(sessionKey);
            return schematic;
        }

        return null;
    }
}
