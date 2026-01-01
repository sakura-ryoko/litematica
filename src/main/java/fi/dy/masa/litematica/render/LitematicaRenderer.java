package fi.dy.masa.litematica.render;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayerGroup;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import com.mojang.blaze3d.buffers.GpuBufferSlice;

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.litematica.Reference;
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

    // Moved to ChunkRenderBatchDraw
//    private boolean renderCollidingSchematicBlocks;
    private boolean renderPiecewiseSchematic;
    private boolean renderPiecewiseBlocks;
    private boolean renderPiecewiseEntities;
    private boolean renderPiecewiseTileEntities;

    public static LitematicaRenderer getInstance()
    {
        return INSTANCE;
    }

	private LitematicaRenderer()
	{
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

    public void renderSchematicOverlays(Camera camera, Profiler profiler)
    {
        boolean invert = Hotkeys.INVERT_OVERLAY_RENDER_STATE.getKeybind().isKeybindHeld();

        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue() != invert)
        {
            boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();
            float lineWidth = (float) (renderThrough ? Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.getDoubleValue() : Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue());

            profiler.push("schematic_overlay");

            if (!IrisCompat.isShadowPassActive())
            {
                // this.getCamera()
                this.getWorldRenderer().renderBlockOverlays(camera, lineWidth, profiler);
            }

            profiler.pop();
        }
    }

	public void updateCameraState(Camera camera, float tickProgress)
	{
		this.getWorldRenderer().updateCameraState(camera, tickProgress);
	}

    public void piecewisePrepare(Frustum frustum, Profiler profiler)
    {
		// Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() &&
        boolean render = Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                         this.mc.getCameraEntity() != null;
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
        this.renderPiecewiseEntities = false;
        this.renderPiecewiseTileEntities = false;
        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        if (render && frustum != null && worldRenderer.hasWorld() && this.mc.player != null)
        {
            boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();
            this.renderPiecewiseSchematic = Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert;
            this.renderPiecewiseBlocks = this.renderPiecewiseSchematic && Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue();
//            this.renderCollidingSchematicBlocks = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();
            this.renderPiecewiseEntities = this.renderPiecewiseSchematic && Configs.Visuals.RENDER_SCHEMATIC_ENTITIES.getBooleanValue();
            this.renderPiecewiseTileEntities = this.renderPiecewiseSchematic && Configs.Visuals.RENDER_SCHEMATIC_TILE_ENTITIES.getBooleanValue();

            if (this.renderPiecewiseSchematic)
            {
                profiler.push(Reference.MOD_ID+"_culling");
                this.calculateFinishTime();

                profiler.swap(Reference.MOD_ID+"_terrain_setup");
                worldRenderer.setupTerrain(this.getCamera(), frustum, this.frameCount++, this.mc.player.isSpectator(), profiler);

//                profiler.swap(Reference.MOD_ID+"_update_chunks");
//                worldRenderer.updateChunks(this.finishTimeNano, profiler);

                profiler.pop();

                this.frustum = frustum;
            }
        }
    }

    public void piecewiseUpdate(Camera camera, Profiler profiler)
    {
        boolean render = Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                camera != null;
        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        if (render && this.frustum != null && worldRenderer.hasWorld() &&
            this.renderPiecewiseSchematic)
        {
            profiler.push(Reference.MOD_ID+"_update_chunks");
            worldRenderer.updateChunks(this.finishTimeNano, profiler);
            profiler.pop();
        }
    }

    public void scheduleTranslucentSorting(Vec3d camera, Profiler profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push(Reference.MOD_ID + "_schedule_translucent_sorting");
            this.getWorldRenderer().scheduleTranslucentSorting(camera, profiler);
            profiler.pop();
        }
    }

    public void capturePreMainValues(Camera camera, GpuBufferSlice fogBuffer, Profiler profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push(Reference.MOD_ID+"_pre_main_capture");
            this.getWorldRenderer().capturePreMainValues(camera, fogBuffer, profiler);
            profiler.pop();
        }
    }

    public void piecewisePrepareBlockLayers(Matrix4fc matrix4fc, double cameraX, double cameraY, double cameraZ, Profiler profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push(Reference.MOD_ID + "_prepare_block_layers");
            this.getWorldRenderer().prepareBlockLayers(matrix4fc, cameraX, cameraY, cameraZ, profiler);
            profiler.pop();
        }
    }

    public void piecewiseDrawBlockLayerGroup(BlockRenderLayerGroup group)
    {
        if (this.renderPiecewiseBlocks)
        {
            // Use Saved Profiler later
            this.getWorldRenderer().drawBlockLayerGroup(group);
        }
    }

    public void piecewisePrepareEntities(Camera camera, Frustum frustum, WorldRenderState renderStates, RenderTickCounter tickCounter, Profiler profiler)
    {
        if (this.renderPiecewiseEntities)
        {
            profiler.push(Reference.MOD_ID+"_prepare_entities");
            this.getWorldRenderer().prepareEntities(this.getCamera(), this.frustum, renderStates, tickCounter, profiler);
            profiler.pop();
        }
    }

	public void piecewiseRenderEntities(MatrixStack matrices, WorldRenderState renderStates, OrderedRenderCommandQueue queue, Profiler profiler)
	{
		if (this.renderPiecewiseEntities)
		{
			profiler.push(Reference.MOD_ID+"_render_entities");
			this.getWorldRenderer().renderEntities(this.getCamera(), this.frustum, matrices, renderStates, queue, profiler);
			profiler.pop();
		}
	}

	public void piecewisePrepareBlockEntities(Camera camera, Frustum frustum, WorldRenderState renderStates, float tickProgress, Profiler profiler)
    {
        if (this.renderPiecewiseTileEntities)
        {
            profiler.push(Reference.MOD_ID+"_prepare_block_entities");
			MatrixStack matrices = new MatrixStack();
            this.getWorldRenderer().prepareBlockEntities(this.getCamera(), this.frustum, renderStates, matrices, tickProgress, profiler);
            profiler.pop();
        }
    }

	public void piecewiseRenderBlockEntities(MatrixStack matrices, WorldRenderState renderStates, OrderedRenderCommandQueueImpl queue, Profiler profiler)
	{
		if (this.renderPiecewiseTileEntities)
		{
			profiler.push(Reference.MOD_ID+"_block_entities");
			this.getWorldRenderer().renderBlockEntities(this.getCamera(), this.frustum, matrices, renderStates, queue, profiler);
			profiler.pop();
		}
	}

	public void piecewiseRenderOverlay(Matrix4f posMatrix, Matrix4f projMatrix, Profiler profiler)
    {
        if (this.renderPiecewiseSchematic)
        {
            profiler.push(Reference.MOD_ID+"_schematic_overlay");
            this.renderSchematicOverlays(this.getCamera(), profiler);
            profiler.pop();
        }

        this.getWorldRenderer().clearBlockBatchDraw();
		this.getWorldRenderer().clearWorldRenderStates();
        this.cleanup();
    }

    private Camera getCamera()
    {
        return this.mc.gameRenderer.getCamera();
    }

    private void cleanup()
    {
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
        this.renderPiecewiseEntities = false;
        this.renderPiecewiseTileEntities = false;
    }
}
