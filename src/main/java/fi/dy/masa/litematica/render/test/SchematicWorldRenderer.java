package fi.dy.masa.litematica.render.test;

import java.util.HashSet;
import org.jetbrains.annotations.Nullable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.litematica.render.test.buffer.SchematicBuilderStorage;
import fi.dy.masa.litematica.render.test.builder.SchematicChunkBuilder;
import fi.dy.masa.litematica.render.test.dispatch.SchematicBlockEntityRenderDispatcher;
import fi.dy.masa.litematica.render.test.dispatch.SchematicEntityRenderDispatch;
import fi.dy.masa.litematica.world.WorldSchematic;

public class SchematicWorldRenderer
{
    private MinecraftClient mc;
    private WorldSchematic world;
    private SchematicEntityRenderDispatch entityRenderDispatch;
    private SchematicBlockEntityRenderDispatcher blockEntityRenderDispatch;
    private SchematicBuilderStorage bufferBuilders;
    @Nullable
    private SchematicChunkBuilder chunkBuilder;

    public SchematicWorldRenderer(MinecraftClient mc, SchematicEntityRenderDispatch entityDispatch, SchematicBlockEntityRenderDispatcher blockEntityDispatch, SchematicBuilderStorage bufferBuilders)
    {
        this.mc = mc;
        this.entityRenderDispatch = entityDispatch;
        this.blockEntityRenderDispatch = blockEntityDispatch;
        this.bufferBuilders = bufferBuilders;
    }

    public void addBuiltChunk(SchematicChunkBuilder.SchematicBuiltChunk schematicBuiltChunk)
    {
        // NO-OP
    }

    public void updateNoCullingBlockEntities(HashSet<BlockEntity> set2, HashSet<BlockEntity> set)
    {
        // NO-OP
    }

    public void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator)
    {
        Vec3d camVec3d = camera.getPos();

        //this.chunkBuilder.setCameraVec3d(camVec3d);
    }

    public boolean isTerrainRenderComplete()
    {
        return this.chunkBuilder.isEmpty();
    }

    private void updateChunks(Camera camera)
    {
        // TODO (for each built chunk)
        //SchematicChunkBuilder.SchematicBuiltChunk builtChunk;
        //this.chunkBuilder.rebuildTask(builtchunk,  chunkRegionBuilder);

        //this.chunkBuilder.uploadTask();
        // TODO (for each built chunk)
        //SchematicChunkBuilder.SchematicBuiltChunk builtChunk;
        //builtChunk.scheduleRebuild(this.chunkBuilder, chunkRegionBuilder);
    }

    public void setWorld(@Nullable WorldSchematic world)
    {
        this.world = world;

        // TODO clear chunks
        this.chunkBuilder.stop();
        this.chunkBuilder = null;
        // TODO clear built chunks
    }

    public void reload()
    {
        if (this.world == null)
        {
            return;
        }
        //this.chunkBuilder.setSchematicWorld(this.world);

        //this.chunkBuilder.reset();

        // TODO build chunks
    }
}
