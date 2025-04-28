package fi.dy.masa.litematica.schematic;

import javax.annotation.Nonnull;

public record SchematicSchema(int litematicVersion, int minecraftDataVersion)
{
    @Override
    public @Nonnull String toString()
    {
        return "V" + this.litematicVersion() + " / DataVersion " + this.minecraftDataVersion();
    }
}
