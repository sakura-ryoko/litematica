package fi.dy.masa.litematica.render;

import javax.annotation.Nullable;

import fi.dy.masa.litematica.compat.iris.IrisCompat;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.profiler.Profiler;

import fi.dy.masa.litematica.compat.iris.IrisCompat;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.world.WorldSchematic;

public class LitematicaRenderer
{
    private static final LitematicaRenderer INSTANCE = new LitematicaRenderer();

    private MinecraftClient mc;
    private WorldRendererSchematic worldRenderer;
    private Frustum frustum;
    private int frameCount;
    private long finishTimeNano;

    private boolean renderCollidingSchematicBlocks;
    private boolean renderPiecewiseSchematic;
    private boolean renderPiecewiseBlocks;

    private LitematicaRenderer()
    {
    }

    public static LitematicaRenderer getInstance()
    {
        return INSTANCE;
    }

    public WorldRendererSchematic getWorldRenderer()
    {
        if (this.worldRenderer == null)
        {
            this.mc = MinecraftClient.getInstance();
            this.worldRenderer = new WorldRendererSchematic(this.mc);
        }

        return this.worldRenderer;
    }

    public WorldRendererSchematic resetWorldRenderer()
    {
        if (this.worldRenderer != null)
        {
            this.worldRenderer.setWorldAndLoadRenderers(null);
            this.worldRenderer = null;
        }

        return this.getWorldRenderer();
    }

    public void loadRenderers(@Nullable Profiler profiler)
    {
        this.getWorldRenderer().loadRenderers(profiler);
    }

    public void onSchematicWorldChanged(@Nullable WorldSchematic worldClient)
    {
        this.getWorldRenderer().setWorldAndLoadRenderers(worldClient);
    }

    private void calculateFinishTime()
    {
        // TODO 1.15+
        long fpsTarget = 60L;

        if (Configs.Generic.RENDER_THREAD_NO_TIMEOUT.getBooleanValue())
        {
            this.finishTimeNano = Long.MAX_VALUE;
        }
        else
        {
            this.finishTimeNano = System.nanoTime() + 1000000000L / fpsTarget / 2L;
        }
    }

    /*
    public void renderSchematicWorld(MatrixStack matrices, Matrix4f matrix, float partialTicks)
    {
        if (this.mc.skipGameRender == false)
        {
            this.mc.getProfiler().push("litematica_schematic_world_render");

            if (this.mc.getCameraEntity() == null)
            {
                this.mc.setCameraEntity(this.mc.player);
            }

            RenderSystem.pushMatrix();
            RenderSystem.enableDepthTest();

            this.calculateFinishTime();
            this.renderWorld(matrices, matrix, partialTicks, this.finishTimeNano);
            this.cleanup();

            RenderSystem.popMatrix();

            this.mc.getProfiler().pop();
        }
    }

    private void renderWorld(MatrixStack matrices, Matrix4f matrix, float partialTicks, long finishTimeNano)
    {
        this.mc.getProfiler().push("culling");

        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        Camera camera = this.getCamera();
        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        Frustum frustum = new Frustum(matrices.peek().getModel(), matrix);
        frustum.setPosition(x, y, z);

        this.mc.getProfiler().swap("prepare_terrain");
        this.mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        fi.dy.masa.malilib.render.RenderUtils.disableDiffuseLighting();
        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        this.mc.getProfiler().swap("terrain_setup");
        worldRenderer.setupTerrain(camera, frustum, this.frameCount++, this.mc.player.isSpectator());

        this.mc.getProfiler().swap("update_chunks");
        worldRenderer.updateChunks(finishTimeNano);

        this.mc.getProfiler().swap("terrain");
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.disableAlphaTest();

        if (Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue())
        {
            RenderSystem.pushMatrix();

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.2f, -0.4f);
            }

            this.setupAlphaShader();
            this.enableAlphaShader();

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderBlockLayer(RenderLayer.getSolid(), matrices, camera);
            worldRenderer.renderBlockLayer(RenderLayer.getCutoutMipped(), matrices, camera);
            worldRenderer.renderBlockLayer(RenderLayer.getCutout(), matrices, camera);

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            RenderSystem.disableBlend();
            RenderSystem.shadeModel(GL11.GL_FLAT);
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01F);

            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.popMatrix();

            this.mc.getProfiler().swap("entities");

            RenderSystem.pushMatrix();

            fi.dy.masa.malilib.render.RenderUtils.enableDiffuseLightingForLevel(matrices);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderEntities(camera, frustum, matrices, partialTicks);

            RenderSystem.disableFog(); // Fixes Structure Blocks breaking all rendering
            RenderSystem.disableBlend();
            fi.dy.masa.malilib.render.RenderUtils.disableDiffuseLighting();

            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.popMatrix();

            RenderSystem.enableCull();
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.1F);
            this.mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            RenderSystem.shadeModel(GL11.GL_SMOOTH);

            this.mc.getProfiler().swap("translucent");
            RenderSystem.depthMask(false);

            RenderSystem.pushMatrix();

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderBlockLayer(RenderLayer.getTranslucent(), matrices, camera);

            RenderSystem.popMatrix();

            this.disableAlphaShader();
        }

        this.mc.getProfiler().swap("overlay");
        this.renderSchematicOverlay(matrices);

        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableCull();

        this.mc.getProfiler().pop();
    }
    */

    public void renderSchematicOverlay(Matrix4f viewMatrix, Matrix4f posMatrix, Profiler profiler)
    {
        boolean invert = Hotkeys.INVERT_OVERLAY_RENDER_STATE.getKeybind().isKeybindHeld();

        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue() != invert)
        {
            boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();
            float lineWidth = (float) (renderThrough ? Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.getDoubleValue() : Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue());

            profiler.push("litematica_schematic_overlay");
            RenderSystem.disableCull();
            //TODO: RenderSystem.alphaFunc(GL11.GL_GREATER, 0.001F);
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-0.4f, -0.8f);
            RenderSystem.lineWidth(lineWidth);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            //TODO: RenderSystem.glMultiTexCoord2f(GL13.GL_TEXTURE1, 240.0F, 240.0F);

            if (!IrisCompat.isShadowPassActive())
            {
                this.getWorldRenderer().renderBlockOverlays(viewMatrix, this.getCamera(), posMatrix, profiler);
            }

            RenderSystem.enableDepthTest();
            RenderSystem.polygonOffset(0f, 0f);
            RenderSystem.disablePolygonOffset();
            RenderSystem.enableCull();
            profiler.pop();
        }
    }

    public void piecewisePrepareAndUpdate(Frustum frustum, Profiler profiler)
    {
        boolean render = Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() &&
                         Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                         this.mc.getCameraEntity() != null;
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        if (render && frustum != null && worldRenderer.hasWorld())
        {
            boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();
            this.renderPiecewiseSchematic = Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert;
            this.renderPiecewiseBlocks = this.renderPiecewiseSchematic && Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue();
            this.renderCollidingSchematicBlocks = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();

            if (this.renderPiecewiseSchematic)
            {
                profiler.push("litematica_culling");

                this.calculateFinishTime();

                profiler.swap("litematica_terrain_setup");
                worldRenderer.setupTerrain(this.getCamera(), frustum, this.frameCount++, this.mc.player.isSpectator(), profiler);

                profiler.swap("litematica_update_chunks");
                worldRenderer.updateChunks(this.finishTimeNano, profiler);

                profiler.pop();

                this.frustum = frustum;
            }
        }
    }

    public void piecewiseRenderSolid(Matrix4f viewMatrix, Matrix4f posMatrix, Profiler profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push("litematica_blocks_solid");

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            //RenderSystem.setShader(GameRenderer::getRenderTypeSolidProgram);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_SOLID);
            this.getWorldRenderer().renderBlockLayer(RenderLayer.getSolid(), viewMatrix, this.getCamera(), posMatrix, profiler);

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            profiler.pop();
        }
    }

    public void piecewiseRenderCutoutMipped(Matrix4f viewMatrix, Matrix4f posMatrix, Profiler profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push("litematica_blocks_cutout_mipped");

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            //RenderSystem.setShader(GameRenderer::getRenderTypeCutoutMippedProgram);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_CUTOUT_MIPPED);
            this.getWorldRenderer().renderBlockLayer(RenderLayer.getCutoutMipped(), viewMatrix, this.getCamera(), posMatrix, profiler);

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            profiler.pop();
        }
    }

    public void piecewiseRenderCutout(Matrix4f viewMatrix, Matrix4f posMatrix, Profiler profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push("litematica_blocks_cutout");

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            //RenderSystem.setShader(GameRenderer::getRenderTypeCutoutProgram);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_CUTOUT);
            this.getWorldRenderer().renderBlockLayer(RenderLayer.getCutout(), viewMatrix, this.getCamera(), posMatrix, profiler);

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            profiler.pop();
        }
    }

    public void piecewiseRenderTranslucent(Matrix4f viewMatrix, Matrix4f posMatrix, Profiler profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push("litematica_translucent");

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            //RenderSystem.setShader(GameRenderer::getRenderTypeTranslucentProgram);
            RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_TRANSLUCENT);
            this.getWorldRenderer().renderBlockLayer(RenderLayer.getTranslucent(), viewMatrix, this.getCamera(), posMatrix, profiler);

            if (this.renderCollidingSchematicBlocks)
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            profiler.pop();
        }
    }

    public void piecewiseRenderOverlay(Matrix4f viewMatrix, Matrix4f posMatrix, Profiler profiler)
    {
        if (this.renderPiecewiseSchematic)
        {
            profiler.push("litematica_overlay");

            Framebuffer fb = MinecraftClient.isFabulousGraphicsOrBetter() ? this.mc.worldRenderer.getTranslucentFramebuffer() : null;

            if (fb != null)
            {
                fb.beginWrite(false);
            }

            this.renderSchematicOverlay(viewMatrix, posMatrix, profiler);

            if (fb != null)
            {
                this.mc.getFramebuffer().beginWrite(false);
            }

            profiler.pop();
        }

        this.cleanup();
    }

    public void piecewiseRenderEntities(Matrix4f posMatrix, float partialTicks, Profiler profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push("litematica_entities");

            this.getWorldRenderer().renderEntities(this.getCamera(), this.frustum, posMatrix, partialTicks, profiler);

            profiler.pop();
        }
    }

    private Camera getCamera()
    {
        return this.mc.gameRenderer.getCamera();
    }

    private void cleanup()
    {
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
    }
}
