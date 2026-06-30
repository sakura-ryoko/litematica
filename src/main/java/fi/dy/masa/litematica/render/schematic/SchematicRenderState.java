package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;

import fi.dy.masa.malilib.render.uniform.ChunkFixUniform;

public class SchematicRenderState
{
	protected CameraRenderState cameraState;
	protected final List<BlockEntityRenderState> blockEntityStates;
	protected final List<EntityRenderState> entityStates;
	protected ChunkRenderBatchDraw batchDraw;
	protected ChunkFixUniform chunkFixUniform;
//	protected LegacyTerrainFixUniform legacyTerrainFix;

	protected SchematicRenderState()
	{
		this.cameraState = new CameraRenderState();
		this.blockEntityStates = new ArrayList<>();
		this.entityStates = new ArrayList<>();
		this.batchDraw = null;
		this.chunkFixUniform = new ChunkFixUniform();
//		this.legacyTerrainFix = new LegacyTerrainFixUniform();
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
		this.blockEntityStates.clear();
		this.entityStates.clear();
		this.batchDraw = null;
	}

	// Performed under `endFrame()`
	protected void clearChunkFixUniform()
	{
		try
		{
			this.chunkFixUniform.close();
		}
		catch (Exception ignored) {}
		this.chunkFixUniform = new ChunkFixUniform();
	}

	// Performed under `endFrame()`
//	protected void clearLegacyTerrainFixUniform()
//	{
//		try
//		{
//			this.legacyTerrainFix.close();
//		}
//		catch (Exception _) {}
//		this.legacyTerrainFix = new LegacyTerrainFixUniform();
//	}
}
