package fi.dy.masa.litematica.render.test.data;

import org.jetbrains.annotations.Nullable;
import fi.dy.masa.litematica.world.ChunkSchematic;

public class SchematicWorldChunk
{
    private final ChunkSchematic chunk;
    @Nullable
    private SchematicRenderedChunk renderedChunk;

    public SchematicWorldChunk(ChunkSchematic chunk)
    {
        this.chunk = chunk;
    }

    public ChunkSchematic getChunk()
    {
        return this.chunk;
    }

    public SchematicRenderedChunk getRenderedChunk()
    {
        if (this.renderedChunk == null)
        {
            this.renderedChunk = new SchematicRenderedChunk(this.chunk);
        }

        return this.renderedChunk;
    }
}
