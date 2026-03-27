package fi.dy.masa.litematica.render;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import com.mojang.blaze3d.pipeline.RenderPipeline;

@ApiStatus.Internal
@VisibleForTesting
public class LitematicaPipelines
{
	// todo TERRAIN Snippet
	public static RenderPipeline.Snippet TERRAIN_STAGE;
	public static RenderPipeline.Snippet TERRAIN_TRANSLUCENT_STAGE;
	public static RenderPipeline.Snippet TERRAIN_MASA_STAGE;

	// TERRAIN
	public static RenderPipeline SOLID_TERRAIN;
	public static RenderPipeline WIREFRAME;
	public static RenderPipeline CUTOUT_TERRAIN;

	// TERRAIN_OFFSET
	public static RenderPipeline SOLID_TERRAIN_OFFSET;
	public static RenderPipeline WIREFRAME_OFFSET;
	public static RenderPipeline CUTOUT_TERRAIN_OFFSET;

	// TERRAIN_TRANSLUCENT
	public static RenderPipeline TRANSLUCENT;
	public static RenderPipeline TRANSLUCENT_OFFSET;

	// TERRAIN_MASA
	public static RenderPipeline SOLID_TERRAIN_MASA;
	public static RenderPipeline WIREFRAME_MASA;
	public static RenderPipeline CUTOUT_TERRAIN_MASA;

	// TERRAIN_MASA_OFFSET
	public static RenderPipeline SOLID_TERRAIN_MASA_OFFSET;
	public static RenderPipeline WIREFRAME_MASA_OFFSET;
	public static RenderPipeline CUTOUT_TERRAIN_MASA_OFFSET;

	// todo BLOCK Snippet
	public static RenderPipeline.Snippet BLOCK_STAGE;
	public static RenderPipeline.Snippet BLOCK_TRANSLUCENT_STAGE;
	public static RenderPipeline.Snippet BLOCK_MASA_STAGE;

	// BLOCK
	public static RenderPipeline SOLID_BLOCK;
	public static RenderPipeline CUTOUT_BLOCK;

	// BLOCK_OFFSET
	public static RenderPipeline SOLID_BLOCK_OFFSET;
	public static RenderPipeline CUTOUT_BLOCK_OFFSET;

	// BLOCK_TRANSLUCENT
	public static RenderPipeline TRANSLUCENT_BLOCK;
	public static RenderPipeline TRANSLUCENT_BLOCK_OFFSET;

	// BLOCK_MASA
	public static RenderPipeline SOLID_BLOCK_MASA;
	public static RenderPipeline CUTOUT_BLOCK_MASA;

	// BLOCK_MASA_OFFSET
	public static RenderPipeline SOLID_BLOCK_MASA_OFFSET;
	public static RenderPipeline CUTOUT_BLOCK_MASA_OFFSET;

	// todo LEGACY_TERRAIN Snippet
	public static RenderPipeline.Snippet LEGACY_TERRAIN_STAGE;
	public static RenderPipeline.Snippet LEGACY_TERRAIN_TRANSLUCENT_STAGE;
	public static RenderPipeline.Snippet LEGACY_TERRAIN_MASA_STAGE;

	// LEGACY_TERRAIN
	public static RenderPipeline LEGACY_SOLID_TERRAIN;
	public static RenderPipeline LEGACY_WIREFRAME;
	public static RenderPipeline LEGACY_CUTOUT_TERRAIN;

	// LEGACY_TERRAIN_OFFSET
	public static RenderPipeline LEGACY_SOLID_TERRAIN_OFFSET;
	public static RenderPipeline LEGACY_WIREFRAME_OFFSET;
	public static RenderPipeline LEGACY_CUTOUT_TERRAIN_OFFSET;

	// LEGACY_TERRAIN_TRANSLUCENT
	public static RenderPipeline LEGACY_TRANSLUCENT;
	public static RenderPipeline LEGACY_TRANSLUCENT_OFFSET;

	// LEGACY_TERRAIN_MASA
	public static RenderPipeline LEGACY_SOLID_TERRAIN_MASA;
	public static RenderPipeline LEGACY_WIREFRAME_MASA;
	public static RenderPipeline LEGACY_CUTOUT_TERRAIN_MASA;

	// LEGACY_TERRAIN_MASA_OFFSET
	public static RenderPipeline LEGACY_SOLID_TERRAIN_MASA_OFFSET;
	public static RenderPipeline LEGACY_WIREFRAME_MASA_OFFSET;
	public static RenderPipeline LEGACY_CUTOUT_TERRAIN_MASA_OFFSET;
}
