package fi.dy.masa.litematica.render.test.builder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import fi.dy.masa.litematica.render.test.data.SchematicRenderedChunk;
import fi.dy.masa.litematica.render.test.data.SchematicRendererRegion;
import fi.dy.masa.litematica.render.test.data.SchematicWorldChunk;
import fi.dy.masa.litematica.world.WorldSchematic;

public class SchematicRegionBuilder
{
    private final Long2ObjectMap<SchematicWorldChunk> chunks = new Long2ObjectOpenHashMap<>();

    @Nullable
    public SchematicRendererRegion build(WorldSchematic world, ChunkSectionPos sectionPos)
    {
        SchematicWorldChunk clientChunk = this.computeClientChunk(world, sectionPos.getSectionX(), sectionPos.getSectionZ());
        if (clientChunk.getChunk().isSectionEmpty(sectionPos.getSectionY())) {
            return null;
        }
        int i = sectionPos.getSectionX() - 1;
        int j = sectionPos.getSectionZ() - 1;
        int k = sectionPos.getSectionX() + 1;
        int l = sectionPos.getSectionZ() + 1;

        SchematicRenderedChunk[] renderedChunks = new SchematicRenderedChunk[9];
        for (int m = j; m <= l; ++m) {
            for (int n = i; n <= k; ++n) {
                int o = SchematicRendererRegion.getIndex(i, j, n, m);
                SchematicWorldChunk clientChunk2 = n == sectionPos.getSectionX() && m == sectionPos.getSectionZ() ? clientChunk : this.computeClientChunk(world, n, m);
                renderedChunks[o] = clientChunk2.getRenderedChunk();
            }
        }
        return new SchematicRendererRegion(world, i, j, renderedChunks);
    }

    private SchematicWorldChunk computeClientChunk(WorldSchematic world, int chunkX, int chunkZ)
    {
        return this.chunks.computeIfAbsent(ChunkPos.toLong(chunkX, chunkZ), chunkPos -> new SchematicWorldChunk(world.getChunk(ChunkPos.getPackedX(chunkPos), ChunkPos.getPackedZ(chunkPos))));
    }
}
