package fi.dy.masa.litematica.render;

import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.world.WorldSchematic;

public class LitematicaRenderer
{
    private static final LitematicaRenderer INSTANCE = new LitematicaRenderer();
//    private static final Logger LOGGER = Litematica.LOGGER;

    private Minecraft mc;
    private IWorldSchematicRenderer worldRenderer;
    private Frustum frustum;
    private int frameCount;
    private long finishTimeNano;

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

	public IWorldSchematicRenderer getWorldRenderer()
    {
        if (this.worldRenderer == null)
        {
            this.mc = Minecraft.getInstance();
            this.worldRenderer = new WorldRendererSchematic(this.mc);
        }

        return this.worldRenderer;
    }

    public IWorldSchematicRenderer resetWorldRenderer()
    {
        if (this.worldRenderer != null)
        {
            this.worldRenderer.setWorldAndLoadRenderers(null);
            this.worldRenderer = null;
        }

        return this.getWorldRenderer();
    }

    public void loadRenderers(@Nullable ProfilerFiller profiler)
    {
        this.getWorldRenderer().loadRenderers(profiler);
    }

    public void onSchematicWorldChanged(@Nullable WorldSchematic worldClient)
    {
        this.getWorldRenderer().setWorldAndLoadRenderers(worldClient);
    }

	public void onBlockModelRendererReload(BlockRenderDispatcher manager)
	{
		if (this.worldRenderer != null)
		{
			this.worldRenderer.reloadBlockRenderManager(manager);
		}
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

    public void onEndFrame()
    {
        this.getWorldRenderer().getChunkFixUniform().endFrame();
    }

    public void onClose()
    {
        this.getWorldRenderer().clearChunkFixUniform();
    }

    public void renderSchematicOverlays(Camera camera, ProfilerFiller profiler)
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

    public void piecewisePrepare(Frustum frustum, ProfilerFiller profiler)
    {
        //LOGGER.error("[LR] piecewisePrepare()");
		// Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() &&
        boolean render = Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                         this.mc.getCameraEntity() != null;
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
        this.renderPiecewiseEntities = false;
        this.renderPiecewiseTileEntities = false;
        IWorldSchematicRenderer worldRenderer = this.getWorldRenderer();

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

                profiler.popPush(Reference.MOD_ID+"_terrain_setup");
                worldRenderer.setupTerrain(this.getCamera(), frustum, this.frameCount++, this.mc.player.isSpectator(), profiler);

//                profiler.popPush(Reference.MOD_ID+"_update_chunks");
//                worldRenderer.updateChunks(this.finishTimeNano, profiler);

                profiler.pop();

                this.frustum = frustum;
            }
        }
    }

    public void piecewiseUpdate(Camera camera, ProfilerFiller profiler)
    {
        //LOGGER.error("[LR] piecewiseUpdate()");
        if (this.renderPiecewiseSchematic)
        {
            profiler.push(Reference.MOD_ID+"_update_chunks");
            this.getWorldRenderer().updateChunks(this.finishTimeNano, profiler);
            profiler.pop();
        }
    }

//    public void scheduleChunkUploads(Vec3 camera, ProfilerFiller profiler)
//    {
//        if (this.renderPiecewiseBlocks)
//        {
//            profiler.push(Reference.MOD_ID+"_schedule_chunk_uploads");
//            this.getWorldRenderer().scheduleChunkUploads(camera, profiler);
//            profiler.pop();
//        }
//    }

    public void scheduleTranslucentSorting(Vec3 camera, ProfilerFiller profiler)
    {
        //LOGGER.error("[LR] scheduleTranslucentSorting()");

        if (this.renderPiecewiseBlocks)
        {
            profiler.push(Reference.MOD_ID + "_schedule_translucent_sorting");
            this.getWorldRenderer().scheduleTranslucentSorting(camera, profiler);
            profiler.pop();
        }
    }

    public void capturePreMainValues(Camera camera, GpuBufferSlice fogBuffer, ProfilerFiller profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push(Reference.MOD_ID+"_pre_main_capture");
            this.getWorldRenderer().capturePreMainValues(camera, fogBuffer, profiler);
            profiler.pop();
        }
    }

    public void piecewisePrepareBlockLayers(Matrix4fc matrix4fc, double cameraX, double cameraY, double cameraZ, ProfilerFiller profiler)
    {
        if (this.renderPiecewiseBlocks)
        {
            profiler.push(Reference.MOD_ID + "_prepare_block_layers");
            this.getWorldRenderer().prepareBlockLayers(matrix4fc, cameraX, cameraY, cameraZ, profiler);
            profiler.pop();
        }
    }

    public void piecewiseDrawBlockLayerGroup(ChunkSectionLayerGroup group, @Nullable GpuSampler sampler)
    {
        if (this.renderPiecewiseBlocks)
        {
            // Use Saved Profiler later
            this.getWorldRenderer().drawBlockLayerGroup(group, sampler);
        }
    }

    public void piecewisePrepareEntities(Camera camera, Frustum frustum, LevelRenderState renderStates, DeltaTracker tickCounter, ProfilerFiller profiler)
    {
        if (this.renderPiecewiseEntities)
        {
            profiler.push(Reference.MOD_ID+"_prepare_entities");
            this.getWorldRenderer().prepareEntities(this.getCamera(), this.frustum, renderStates, tickCounter, profiler);
            profiler.pop();
        }
    }

	public void piecewiseRenderEntities(PoseStack matrices, LevelRenderState renderStates, SubmitNodeCollector queue, ProfilerFiller profiler)
	{
		if (this.renderPiecewiseEntities)
		{
			profiler.push(Reference.MOD_ID+"_render_entities");
			this.getWorldRenderer().renderEntities(this.getCamera(), this.frustum, matrices, renderStates, queue, profiler);
			profiler.pop();
		}
	}

	public void piecewisePrepareBlockEntities(Camera camera, Frustum frustum, LevelRenderState renderStates, float tickProgress, ProfilerFiller profiler)
    {
        if (this.renderPiecewiseTileEntities)
        {
            profiler.push(Reference.MOD_ID+"_prepare_block_entities");
			PoseStack matrices = new PoseStack();
            this.getWorldRenderer().prepareBlockEntities(this.getCamera(), this.frustum, renderStates, matrices, tickProgress, profiler);
            profiler.pop();
        }
    }

	public void piecewiseRenderBlockEntities(PoseStack matrices, LevelRenderState renderStates, SubmitNodeStorage queue, ProfilerFiller profiler)
	{
		if (this.renderPiecewiseTileEntities)
		{
			profiler.push(Reference.MOD_ID+"_block_entities");
			this.getWorldRenderer().renderBlockEntities(this.getCamera(), this.frustum, matrices, renderStates, queue, profiler);
			profiler.pop();
		}
	}

	public void piecewiseRenderOverlay(Matrix4f posMatrix, Matrix4f projMatrix, ProfilerFiller profiler)
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
        return this.mc.gameRenderer.getMainCamera();
    }

    private void cleanup()
    {
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
        this.renderPiecewiseEntities = false;
        this.renderPiecewiseTileEntities = false;
    }
}
