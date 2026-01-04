package fi.dy.masa.litematica.world;

import java.util.Iterator;
import java.util.function.BooleanSupplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import fi.dy.masa.litematica.config.Configs;

public class ChunkManagerSchematic extends ChunkSource
{
    private final WorldSchematic world;
    private final Long2ObjectMap<ChunkSchematic> loadedChunks = new Long2ObjectOpenHashMap<>(8192);
    private final ChunkSchematic blankChunk;
    private final LevelLightEngine lightingProvider;
    private final FakeLightingProvider fakeLightingProvider;

    public ChunkManagerSchematic(WorldSchematic world)
    {
        this.world = world;
        this.blankChunk = new ChunkSchematic(world, new ChunkPos(0, 0));
        this.blankChunk.setState(ChunkSchematicState.EMPTY);
        this.lightingProvider = new LevelLightEngine(this, true, world.dimensionType().hasSkyLight());
        this.fakeLightingProvider = new FakeLightingProvider(this);
    }

    @Override
    public @Nonnull WorldSchematic getLevel()
    {
        return this.world;
    }

    public synchronized void loadChunk(int chunkX, int chunkZ)
    {
        ChunkSchematic chunk = new ChunkSchematic(this.world, new ChunkPos(chunkX, chunkZ));
        chunk.setState(ChunkSchematicState.LOADED);
        this.loadedChunks.put(ChunkPos.asLong(chunkX, chunkZ), chunk);
    }

    @Override
    public synchronized boolean hasChunk(int chunkX, int chunkZ)
    {
        return this.loadedChunks.containsKey(ChunkPos.asLong(chunkX, chunkZ));
    }

    public synchronized ChunkSchematicState getChunkState(int chunkX, int chunkZ)
    {
        long key = ChunkPos.asLong(chunkX, chunkZ);

        if (this.loadedChunks.containsKey(key))
        {
            return this.loadedChunks.get(key).getState();
        }
        else
        {
            return ChunkSchematicState.UNLOADED;
        }
    }

    public synchronized void setChunkState(int chunkX, int chunkZ, ChunkSchematicState state)
    {
        long key = ChunkPos.asLong(chunkX, chunkZ);

        if (this.loadedChunks.containsKey(key))
        {
            this.loadedChunks.get(key).setState(state);
        }
    }

    @Override
    public @Nonnull String gatherStats()
    {
        return "Schematic Chunk Manager: " + this.getLoadedChunksCount();
    }

    @Override
    public synchronized int getLoadedChunksCount()
    {
        return this.loadedChunks.size();
    }

    public synchronized Long2ObjectMap<ChunkSchematic> getLoadedChunks()
    {
        return this.loadedChunks;
    }

    @Override
    public synchronized LevelChunk getChunk(int chunkX, int chunkZ, @Nonnull ChunkStatus status, boolean fallbackToEmpty)
    {
        ChunkSchematic chunk = this.getChunkForLighting(chunkX, chunkZ);
        return chunk == null && fallbackToEmpty ? this.blankChunk : chunk;
    }

    @Override
    public synchronized ChunkSchematic getChunkForLighting(int chunkX, int chunkZ)
    {
        ChunkSchematic chunk = this.loadedChunks.get(ChunkPos.asLong(chunkX, chunkZ));
        return chunk == null ? this.blankChunk : chunk;
    }

    @Nullable
    public synchronized ChunkSchematic getChunkIfExists(int chunkX, int chunkZ)
    {
        return this.loadedChunks.get(ChunkPos.asLong(chunkX, chunkZ));
    }

    public synchronized void unloadChunk(int chunkX, int chunkZ)
    {
        ChunkSchematic chunk = this.loadedChunks.remove(ChunkPos.asLong(chunkX, chunkZ));

        if (chunk != null)
        {
//            this.world.unloadedEntities(chunk.getEntityCount());
            this.world.unloadEntitiesByChunk(chunkX, chunkZ);
//            chunk.clearEntities();
            chunk.setState(ChunkSchematicState.UNLOADED);
        }
    }

    @Override
    public @Nonnull LevelLightEngine getLightEngine()
    {
        if (Configs.Visuals.ENABLE_SCHEMATIC_FAKE_LIGHTING.getBooleanValue())
        {
            return this.fakeLightingProvider;
        }

        return this.lightingProvider;
    }

    @Override
    public void tick(@Nonnull BooleanSupplier shouldKeepTicking, boolean tickChunks)
    {
        // NO-OP
    }

    public synchronized int getTileEntityCount()
    {
        int count = 0;

        Iterator<ChunkSchematic> iter = this.loadedChunks.values().stream().iterator();

        while (iter.hasNext())
        {
            count += iter.next().getTileEntityCount();
        }

        return count;
    }
}
