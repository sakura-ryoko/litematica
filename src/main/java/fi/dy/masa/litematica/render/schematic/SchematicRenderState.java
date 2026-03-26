package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import fi.dy.masa.malilib.render.uniform.ChunkFixUniform;

public class SchematicRenderState
{
	protected CameraRenderState cameraState;
	protected final List<EntityRenderState> entityStates;
	protected final List<BlockEntityRenderState> tileEntityStates;
	protected ChunkRenderBatchDraw batchDraw;
	protected ChunkFixUniform chunkFixUniform;

	protected SchematicRenderState()
	{
		this.cameraState = new CameraRenderState();
		this.entityStates = new ArrayList<>();
		this.tileEntityStates = new ArrayList<>();
		this.batchDraw = null;
		this.chunkFixUniform = new ChunkFixUniform();
	}

	protected boolean hasBatchDraw()
	{
		return this.batchDraw != null;
	}

	protected ChunkRenderBatchDraw getBatchDraw()
	{
		return this.batchDraw;
	}

	protected void clear()
	{
		this.entityStates.clear();
		this.tileEntityStates.clear();
		this.batchDraw = null;
	}

	protected void clearChunkFixUniform()
	{
		try
		{
			this.chunkFixUniform.close();
		}
		catch (Exception _) {}
		this.chunkFixUniform = new ChunkFixUniform();
	}
}
