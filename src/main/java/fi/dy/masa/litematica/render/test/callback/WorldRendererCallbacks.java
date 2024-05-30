package fi.dy.masa.litematica.render.test.callback;

import org.joml.Matrix4f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import fi.dy.masa.litematica.render.test.SchematicWorldRenderer;
import fi.dy.masa.litematica.render.test.buffer.SchematicBuilderStorage;

public class WorldRendererCallbacks
{
    SchematicWorldRenderer renderer;

    private void onInit(MinecraftClient client, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, BufferBuilderStorage bufferBuilders)
    {
        if (renderer == null)
        {
            // Creates multiple instances of the buffers based on available CPU threads
            int threads = Runtime.getRuntime().availableProcessors();
            this.renderer = new SchematicWorldRenderer(client, client.getBlockRenderManager(), entityRenderDispatcher, blockEntityRenderDispatcher, new SchematicBuilderStorage(threads), bufferBuilders);
        }

        this.renderer.init();
    }

    private void onReload(ClientWorld world, BufferBuilderStorage buffers, BlockRenderManager blockRenderer, BlockEntityRenderDispatcher blockEntityRender,
                         int viewDistance, Entity entity)
    {
        if (renderer == null)
        {
            return;
        }

        renderer.reloadChunkBuilder(world, buffers, blockRenderer, blockEntityRender);
        renderer.setFancyGraphics();
        renderer.setViewDistance(viewDistance);
        renderer.clearChunks();
        renderer.resetChunkBuilder();
        renderer.clearCulling();
        renderer.createBuiltChunkStorage();
        renderer.clearBuiltChunks();
        renderer.updateCameraEntity(entity);
    }

    private void onPostSetupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator)
    {
        if (renderer == null)
        {
            return;
        }

        renderer.setupTerrain(camera, frustum, hasForcedFrustum, spectator);
    }

    private void onRenderLayer(RenderLayer layer, double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix)
    {
        if (renderer == null)
        {
            return;
        }

        renderer.renderLayer(layer, x, y, z, matrix4f, positionMatrix);
        renderer.renderOverlays(x, y, z, matrix4f, positionMatrix);
    }

    private void onPostRenderEntities(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2)
    {
        if (renderer == null)
        {
            return;
        }

        renderer.renderEntities(tickCounter, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f, matrix4f2);
    }

    private void onPostRenderBlockEntities(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2)
    {
        if (renderer == null)
        {
            return;
        }

        renderer.renderBlockEntities(tickCounter, renderBlockOutline, camera, gameRenderer, lightmapTextureManager, matrix4f, matrix4f2);
    }

    private void onUpdateChunks(Camera camera)
    {
        if (renderer == null)
        {
            return;
        }

        renderer.updateChunks(camera, MinecraftClient.getInstance().options.getChunkBuilderMode().getValue());
    }

    private void onClose()
    {
        if (renderer == null)
        {
            return;
        }
    }
}
