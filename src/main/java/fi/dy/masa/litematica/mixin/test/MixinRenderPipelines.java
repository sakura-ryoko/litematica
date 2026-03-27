package fi.dy.masa.litematica.mixin.test;

import java.util.Map;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.compat.iris.IrisCompat;
import fi.dy.masa.litematica.render.LitematicaPipelines;

@ApiStatus.Internal
@VisibleForTesting
@Mixin(value = RenderPipelines.class, priority = 990)
public abstract class MixinRenderPipelines
{
	@Shadow @Final private static Map<Identifier, RenderPipeline> PIPELINES_BY_LOCATION;

	@Shadow @Final private static RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET;          // TRANSFORMS_AND_PROJECTION_SNIPPET
	@Shadow @Final private static RenderPipeline.Snippet FOG_SNIPPET;                          // FOG
	@Shadow @Final private static RenderPipeline.Snippet GLOBALS_SNIPPET;                      // GLOBALS_SNIPPET
	@Shadow @Final private static RenderPipeline.Snippet MATRICES_FOG_SNIPPET;                 // TRANSFORMS_PROJECTION_FOG_SNIPPET
	@Shadow @Final private static RenderPipeline.Snippet MATRICES_FOG_LIGHT_DIR_SNIPPET;       // TRANSFORMS_PROJECTION_FOG_LIGHTING_SNIPPET
	@Shadow @Final private static RenderPipeline.Snippet GENERIC_BLOCKS_SNIPPET;               // FOG_AND_SAMPLERS_SNIPPET
	@Shadow @Final private static RenderPipeline.Snippet TERRAIN_SNIPPET;                      // TERRAIN
	@Shadow @Final private static RenderPipeline.Snippet BLOCK_SNIPPET;                        // BLOCK

	@Unique private static final BlendFunction MASA_BLEND = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
	@Unique private static final BlendFunction MASA_BLEND_SIMPLE = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);

	@Shadow
	private static RenderPipeline register(RenderPipeline renderPipeline)
	{
		PIPELINES_BY_LOCATION.put(renderPipeline.getLocation(), renderPipeline);
		return renderPipeline;
	}

	@Unique
	private static Identifier getId(String id)
	{
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, id);
	}

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void litematica_onRegisterPipelines(CallbackInfo ci)
	{
		// todo TERRAIN Snippet
		LitematicaPipelines.TERRAIN_STAGE =
				RenderPipeline.builder(GENERIC_BLOCKS_SNIPPET)
				              .withVertexShader("core/terrain")
				              .withFragmentShader("core/terrain")
				              .withUniform("Projection", UniformType.UNIFORM_BUFFER)
				              .withUniform("ChunkSection", UniformType.UNIFORM_BUFFER)
			                  .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
                              .buildSnippet();

		// todo TERRAIN --> PRE-REGISTER
		LitematicaPipelines.SOLID_TERRAIN =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/solid_terrain"))
				                       .build());

		LitematicaPipelines.WIREFRAME =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/wireframe"))
				                       .withPolygonMode(PolygonMode.WIREFRAME)
				                       .build());

		LitematicaPipelines.CUTOUT_TERRAIN =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/cutout_terrain"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .build());

		// todo TERRAIN_OFFSET --> PRE-REGISTER
		LitematicaPipelines.SOLID_TERRAIN_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/solid_terrain/offset"))
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		LitematicaPipelines.WIREFRAME_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/wireframe/offset"))
				                       .withPolygonMode(PolygonMode.WIREFRAME)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		LitematicaPipelines.CUTOUT_TERRAIN_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/cutout_terrain/offset"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		// todo TERRAIN_MASA Snippet
		LitematicaPipelines.TERRAIN_MASA_STAGE =
				RenderPipeline.builder(LitematicaPipelines.TERRAIN_STAGE)
				              .withColorTargetState(new ColorTargetState(MASA_BLEND))
				              .buildSnippet();

		// todo TERRAIN_MASA --> PRE-REGISTER
		LitematicaPipelines.SOLID_TERRAIN_MASA =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/solid_terrain/masa"))
				                       .build());

		LitematicaPipelines.WIREFRAME_MASA =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/wireframe/masa"))
				                       .withPolygonMode(PolygonMode.WIREFRAME)
				                       .build());

		LitematicaPipelines.CUTOUT_TERRAIN_MASA =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/cutout_terrain/masa"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .build());

		// todo TERRAIN_MASA_OFFSET --> PRE-REGISTER
		LitematicaPipelines.SOLID_TERRAIN_MASA_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/solid_terrain/masa/offset"))
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		LitematicaPipelines.WIREFRAME_MASA_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/wireframe/masa/offset"))
				                       .withPolygonMode(PolygonMode.WIREFRAME)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		LitematicaPipelines.CUTOUT_TERRAIN_MASA_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/cutout_terrain/masa/offset"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		// todo TERRAIN_TRANSLUCENT Snippet
		LitematicaPipelines.TERRAIN_TRANSLUCENT_STAGE =
				RenderPipeline.builder(LitematicaPipelines.TERRAIN_STAGE)
				              .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
				              .buildSnippet();

		// todo TERRAIN_TRANSLUCENT
		LitematicaPipelines.TRANSLUCENT =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_TRANSLUCENT_STAGE)
				                       .withLocation(getId("pipeline/translucent"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.01F)
				                       .build());

		LitematicaPipelines.TRANSLUCENT_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.TERRAIN_TRANSLUCENT_STAGE)
				                       .withLocation(getId("pipeline/translucent/offset"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.01F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		// todo BLOCK Snippet
		LitematicaPipelines.BLOCK_STAGE =
				RenderPipeline.builder(GENERIC_BLOCKS_SNIPPET, MATRICES_PROJECTION_SNIPPET)
				              .withVertexShader("core/block")
				              .withFragmentShader("core/block")
//			                  .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
                              .buildSnippet();

		// todo BLOCK
		LitematicaPipelines.SOLID_BLOCK =
				register(RenderPipeline.builder(LitematicaPipelines.BLOCK_STAGE)
				                       .withLocation(getId("pipeline/solid_block/masa"))
				                       .build());

		LitematicaPipelines.CUTOUT_BLOCK =
				register(RenderPipeline.builder(LitematicaPipelines.BLOCK_STAGE)
				                       .withLocation(getId("pipeline/cutout_block/masa"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .build());

		// todo BLOCK_OFFSET
		LitematicaPipelines.SOLID_BLOCK_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.BLOCK_STAGE)
				                       .withLocation(getId("pipeline/solid_block/offset"))
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		LitematicaPipelines.CUTOUT_BLOCK_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.BLOCK_STAGE)
				                       .withLocation(getId("pipeline/cutout_block/offset"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		// todo BLOCK_MASA Snippet
		LitematicaPipelines.BLOCK_MASA_STAGE =
				RenderPipeline.builder(LitematicaPipelines.BLOCK_STAGE)
			                  .withColorTargetState(new ColorTargetState(MASA_BLEND))
                              .buildSnippet();

		// todo BLOCK_MASA
		LitematicaPipelines.SOLID_BLOCK_MASA =
				register(RenderPipeline.builder(LitematicaPipelines.BLOCK_MASA_STAGE)
				                       .withLocation(getId("pipeline/solid_block/masa"))
				                       .build());

		LitematicaPipelines.CUTOUT_BLOCK_MASA =
				register(RenderPipeline.builder(LitematicaPipelines.BLOCK_MASA_STAGE)
				                       .withLocation(getId("pipeline/cutout_block/masa"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .build());

		// todo BLOCK_MASA_OFFSET
		LitematicaPipelines.SOLID_BLOCK_MASA_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.BLOCK_MASA_STAGE)
				                       .withLocation(getId("pipeline/solid_block/masa/offset"))
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		LitematicaPipelines.CUTOUT_BLOCK_MASA_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.BLOCK_MASA_STAGE)
				                       .withLocation(getId("pipeline/cutout_block/masa/offset"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		// todo BLOCK_TRANSLUCENT Snippet
		LitematicaPipelines.BLOCK_TRANSLUCENT_STAGE =
				RenderPipeline.builder(LitematicaPipelines.BLOCK_STAGE)
				              .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
				              .buildSnippet();

		// todo BLOCK_TRANSLUCENT
		LitematicaPipelines.TRANSLUCENT_BLOCK =
				register(RenderPipeline.builder(LitematicaPipelines.BLOCK_TRANSLUCENT_STAGE)
				                       .withLocation(getId("pipeline/translucent_block"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.01F)
				                       .withDepthStencilState(DepthStencilState.DEFAULT)
				                       .build());

		LitematicaPipelines.TRANSLUCENT_BLOCK_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.BLOCK_TRANSLUCENT_STAGE)
				                       .withLocation(getId("pipeline/translucent_block/offset"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.01F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true, -0.3f, -0.6f))
				                       .build());

		// todo LEGACY_TERRAIN Snippet
		LitematicaPipelines.LEGACY_TERRAIN_STAGE =
				RenderPipeline.builder(MATRICES_FOG_SNIPPET)
				              .withVertexShader(getId("legacy_terrain"))
				              .withFragmentShader(getId("legacy_terrain"))
				              .withSampler("Sampler0")
				              .withSampler("Sampler2")
				              .withUniform("ChunkFix", UniformType.UNIFORM_BUFFER)
				              .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
				              .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, true))
				              .buildSnippet();

		// todo LEGACY_TERRAIN
		LitematicaPipelines.LEGACY_SOLID_TERRAIN =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/legacy/solid"))
				                       .build());

		LitematicaPipelines.LEGACY_WIREFRAME =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/legacy/wireframe"))
				                       .withPolygonMode(PolygonMode.WIREFRAME)
				                       .build());

		LitematicaPipelines.LEGACY_CUTOUT_TERRAIN =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/legacy/cutout"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .build());

		// todo LEGACY_TERRAIN_OFFSET --> PRE-REGISTER
		LitematicaPipelines.LEGACY_SOLID_TERRAIN_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/legacy/solid/masa/offset"))
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, true, -0.3f, -0.6f))
				                       .build());

		LitematicaPipelines.LEGACY_WIREFRAME_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/legacy/wireframe/offset"))
				                       .withPolygonMode(PolygonMode.WIREFRAME)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, true, -0.3f, -0.6f))
				                       .build());

		LitematicaPipelines.LEGACY_CUTOUT_TERRAIN_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/legacy/cutout/offset"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, true, -0.3f, -0.6f))
				                       .build());

		// todo LEGACY_TERRAIN_MASA Snippet
		LitematicaPipelines.LEGACY_TERRAIN_MASA_STAGE =
				RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
				              .withColorTargetState(new ColorTargetState(MASA_BLEND))
				              .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, true))
				              .buildSnippet();

		// todo LEGACY_TERRAIN_MASA --> PRE-REGISTER
		LitematicaPipelines.LEGACY_SOLID_TERRAIN_MASA =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/legacy/solid/masa"))
				                       .build());

		LitematicaPipelines.LEGACY_WIREFRAME_MASA =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/legacy/wireframe/masa"))
				                       .withPolygonMode(PolygonMode.WIREFRAME)
				                       .build());

		LitematicaPipelines.LEGACY_CUTOUT_TERRAIN_MASA =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/legacy/cutout/masa"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .build());

		// todo LEGACY_TERRAIN_MASA_OFFSET --> PRE-REGISTER
		LitematicaPipelines.LEGACY_SOLID_TERRAIN_MASA_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/legacy/solid/masa/offset"))
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, true, -0.3f, -0.6f))
				                       .build());

		LitematicaPipelines.LEGACY_WIREFRAME_MASA_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/legacy/wireframe/masa/offset"))
				                       .withPolygonMode(PolygonMode.WIREFRAME)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, true, -0.3f, -0.6f))
				                       .build());

		LitematicaPipelines.LEGACY_CUTOUT_TERRAIN_MASA_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_MASA_STAGE)
				                       .withLocation(getId("pipeline/legacy/cutout/masa/offset"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, true, -0.3f, -0.6f))
				                       .build());

		// todo LEGACY_TERRAIN_TRANSLUCENT Snippet
		LitematicaPipelines.LEGACY_TERRAIN_TRANSLUCENT_STAGE =
				RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
				              .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
				              .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, true))
				              .buildSnippet();

		// todo LEGACY_TERRAIN_TRANSLUCENT
		LitematicaPipelines.LEGACY_TRANSLUCENT =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_TRANSLUCENT_STAGE)
				                       .withLocation(getId("pipeline/legacy/translucent"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.01F)
				                       .build());

		LitematicaPipelines.LEGACY_TRANSLUCENT_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_TRANSLUCENT_STAGE)
				                       .withLocation(getId("pipeline/legacy/translucent/offset"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.01F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN, true, -0.3f, -0.6f))
				                       .build());


		// todo -- Try registering with Iris.
		IrisCompat.registerPipelines();
	}
}
