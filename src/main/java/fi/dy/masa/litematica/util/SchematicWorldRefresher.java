package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.malilib.interfaces.IRangeChangeListener;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class SchematicWorldRefresher implements IRangeChangeListener
{
    public static final SchematicWorldRefresher INSTANCE = new SchematicWorldRefresher();

    private final Minecraft mc = Minecraft.getInstance();

    @Override
    public void updateAll()
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
//            PlacementManagerDaemonHandler.INSTANCE.clearAllTasks();
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
            final int minY = world.getMinY();
            final int maxY = world.getMaxY() - 1;
            this.updateBetweenY(minY, maxY);
        }
    }

    @Override
    public void updateBetweenX(int minX, int maxX)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
//            Long2ObjectMap<ChunkSchematic> schematicChunks = world.getChunkSource().getLoadedChunks();
            ImmutableList<ChunkPos> keySet = world.getChunkSource().getLoadedNonEmptyChunkPosSet();

            final int cxMin = (Math.min(minX, maxX) >> 4);
            final int cxMax = (Math.max(minX, maxX) >> 4);

//            for (ChunkSchematic chunk : schematicChunks.values())
            for (ChunkPos pos : keySet)
            {
//                ChunkPos pos = chunk.getPos();

                // && chunk.isEmpty() == false
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (pos.x >= cxMin && pos.x <= cxMax &&
                    WorldUtils.isClientChunkLoaded(this.mc.level, pos.x, pos.z))
                {
                    world.scheduleChunkRenders(pos.x, pos.z);
                }
            }
        }
    }

    @Override
    public void updateBetweenY(int minY, int maxY)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
//            Long2ObjectMap<ChunkSchematic> schematicChunks = world.getChunkSource().getLoadedChunks();
            ImmutableList<ChunkPos> keySet = world.getChunkSource().getLoadedNonEmptyChunkPosSet();

//            for (ChunkSchematic chunk : schematicChunks.values())
            for (ChunkPos pos : keySet)
            {
//                ChunkPos pos = chunk.getPos();
                // chunk.isEmpty() == false &&
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (WorldUtils.isClientChunkLoaded(this.mc.level, pos.x, pos.z))
                {
                    world.scheduleChunkRenders(pos.x, pos.z);
                }
            }
        }
    }

    @Override
    public void updateBetweenZ(int minZ, int maxZ)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
            DataManager.getSchematicPlacementManager().setVisibleSubChunksNeedsUpdate();
//            Long2ObjectMap<ChunkSchematic> schematicChunks = world.getChunkSource().getLoadedChunks();
            ImmutableList<ChunkPos> keySet = world.getChunkSource().getLoadedNonEmptyChunkPosSet();
            final int czMin = (Math.min(minZ, maxZ) >> 4);
            final int czMax = (Math.max(minZ, maxZ) >> 4);

//            for (ChunkSchematic chunk : schematicChunks.values())
            for (ChunkPos pos : keySet)
            {
//                ChunkPos pos = chunk.getPos();

                //  && chunk.isEmpty() == false
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (pos.z >= czMin && pos.z <= czMax &&
                    WorldUtils.isClientChunkLoaded(this.mc.level, pos.x, pos.z))
                {
                    world.scheduleChunkRenders(pos.x, pos.z);
                }
            }
        }
    }

    public void markSchematicChunksForRenderUpdate(int chunkX, int chunkZ)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
            if (world.getChunkSource().hasChunk(chunkX, chunkZ) &&
                WorldUtils.isClientChunkLoaded(this.mc.level, chunkX, chunkZ))
            {
                world.scheduleChunkRenders(chunkX, chunkZ);
            }
        }
    }

    public void markSchematicChunkForRenderUpdate(BlockPos pos)
    {
        WorldSchematic world = SchematicWorldHandler.getSchematicWorld();

        if (world != null && this.mc.level != null)
        {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            //Litematica.debugLog("SchematicWorldRefresher#markSchematicChunkForRenderUpdate({}, {})", chunkX, chunkZ);

            if (world.getChunkSource().hasChunk(chunkX, chunkZ) &&
                WorldUtils.isClientChunkLoaded(this.mc.level, chunkX, chunkZ))
            {
                world.scheduleChunkRenders(chunkX, chunkZ);
            }
        }
    }
}
