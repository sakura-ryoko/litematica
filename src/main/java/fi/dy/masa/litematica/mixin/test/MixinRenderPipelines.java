package fi.dy.masa.litematica.mixin.test;

import java.util.Map;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.render.LitematicaPipelines;

@ApiStatus.Internal
@VisibleForTesting
@Deprecated
//@Mixin(value = RenderPipelines.class, priority = 990)
public abstract class MixinRenderPipelines
{
//	@Shadow @Final private static Map<Identifier, RenderPipeline> PIPELINES_BY_LOCATION;
//
//	@Shadow @Final private static RenderPipeline.Snippet GLOBALS_SNIPPET;                      // GLOBALS_SNIPPET
//	@Shadow @Final private static RenderPipeline.Snippet MATRICES_FOG_SNIPPET;                 // TRANSFORMS_PROJECTION_FOG_SNIPPET
//
//	@Unique private static final BlendFunction MASA_BLEND = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ZERO);
//	@Unique private static final BlendFunction MASA_BLEND_SIMPLE = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
//
//	@Shadow
//	private static RenderPipeline register(RenderPipeline pipeline)
//	{
//		PIPELINES_BY_LOCATION.put(pipeline.getLocation(), pipeline);
//		return pipeline;
//	}
//
//	@Unique
//	private static Identifier getId(String id)
//	{
//		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, id);
//	}
//
//	@Inject(method = "<clinit>", at = @At("TAIL"))
//	private static void litematica_onRegisterPipelines(CallbackInfo ci)
//	{
//		// todo POSITION_COLOR Snippet
//		LitematicaPipelines.POSITION_COLOR_STAGE =
//				RenderPipeline.builder()
//				              .withVertexShader(getId("int_position_color"))
//				              .withFragmentShader(getId("int_position_color"))
//				              .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
////			                  .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
//                              .withPrimitiveTopology(PrimitiveTopology.QUADS)
//                              .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
//                              .buildSnippet();
//
//		LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_STAGE =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_STAGE)
//				              .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
//				              .buildSnippet();
//
//		LitematicaPipelines.POSITION_COLOR_MASA_STAGE =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_STAGE)
//				              .withColorTargetState(new ColorTargetState(MASA_BLEND))
//				              .buildSnippet();
//
//		// todo POSITION_COLOR_TRANSLUCENT
//		LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH_NO_CULL =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/position_color/translucent/no_depth/no_cull"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_NO_DEPTH =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/position_color/translucent/no_depth"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH_OFFSET_1 =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/position_color/translucent/lequal_depth/offset_1"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 0.3f, 0.6f))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH_OFFSET_2 =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/position_color/translucent/lequal_depth/offset_2"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 0.4f, 0.8f))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH_OFFSET_3 =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/position_color/translucent/lequal_depth/offset_3"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 3f, 3f))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH_NO_CULL =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/position_color/translucent/lequal_depth/no_cull"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_LEQUAL_DEPTH =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/position_color/translucent/lequal_depth"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_DEPTH_MASK =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/position_color/translucent/depth_mask"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_TRANSLUCENT =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/position_color/translucent"))
//				              .build();
//
//		// todo POSITION_COLOR_MASA
//		LitematicaPipelines.POSITION_COLOR_MASA_NO_DEPTH_NO_CULL =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_MASA_STAGE)
//				              .withLocation(getId("pipeline/position_color/masa/no_depth/no_cull"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_MASA_NO_DEPTH =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_MASA_STAGE)
//				              .withLocation(getId("pipeline/position_color/masa/no_depth"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_MASA_LEQUAL_DEPTH_OFFSET_1 =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_MASA_STAGE)
//				              .withLocation(getId("pipeline/position_color/masa/lequal_depth/offset_1"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 0.3f, 0.6f))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_MASA_LEQUAL_DEPTH_OFFSET_2 =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_MASA_STAGE)
//				              .withLocation(getId("pipeline/position_color/masa/lequal_depth/offset_2"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 0.4f, 0.8f))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_MASA_LEQUAL_DEPTH_OFFSET_3 =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_MASA_STAGE)
//				              .withLocation(getId("pipeline/position_color/masa/lequal_depth/offset_3"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 3f, 3f))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_MASA_LEQUAL_DEPTH_NO_CULL =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_MASA_STAGE)
//				              .withLocation(getId("pipeline/position_color/masa/lequal_depth/no_cull"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_MASA_LEQUAL_DEPTH =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_MASA_STAGE)
//				              .withLocation(getId("pipeline/position_color/masa/lequal_depth"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_MASA_DEPTH_MASK =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_MASA_STAGE)
//				              .withLocation(getId("pipeline/position_color/masa/depth_mask"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
//				              .build();
//
//		LitematicaPipelines.POSITION_COLOR_MASA =
//				RenderPipeline.builder(LitematicaPipelines.POSITION_COLOR_MASA_STAGE)
//				              .withLocation(getId("pipeline/position_color/masa"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
//				              .build();
//
//		// todo DEBUG_LINES Snippet
//		LitematicaPipelines.DEBUG_LINES_STAGE =
//				RenderPipeline.builder()
//				              .withVertexShader(getId("position_color_lines"))
//				              .withFragmentShader(getId("position_color_lines"))
//				              .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
////			                  .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH, VertexFormat.Mode.DEBUG_LINES)
//                              .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH)
//                              .withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
//                              .buildSnippet();
//
//		LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_STAGE =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_STAGE)
//				              .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
//				              .buildSnippet();
//
//		LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_STAGE =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_STAGE)
//				              .withColorTargetState(new ColorTargetState(MASA_BLEND_SIMPLE))
//				              .buildSnippet();
//
//		// todo DEBUG_LINES_TRANSLUCENT
//		LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_NO_DEPTH_NO_CULL =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/translucent/no_depth/no_cull"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_NO_DEPTH =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/translucent/no_depth"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_NO_CULL =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/translucent/no_cull"))
//				              .withCull(false)
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_LEQUAL_DEPTH =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/translucent/lequal_depth"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_OFFSET_1 =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/translucent/offset_1"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 0.8f, 1.8f))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_OFFSET_2 =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/translucent/offset_2"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 1.2f, 0.2f))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_OFFSET_3 =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/translucent/offset_3"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 3.0f, 3.0f))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_TRANSLUCENT =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_TRANSLUCENT_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/translucent"))
//				              .build();
//
//		// todo DEBUG_LINES_MASA_SIMPLE
//		LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/masa_simple/no_depth/no_cull"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/masa_simple/no_depth"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_NO_CULL =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/masa_simple/no_cull"))
//				              .withCull(false)
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_LEQUAL_DEPTH =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/masa_simple/lequal_depth"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_OFFSET_1 =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/masa_simple/offset_1"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 0.8f, 1.8f))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_OFFSET_2 =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/masa_simple/offset_2"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 1.2f, 0.2f))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_OFFSET_3 =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/masa_simple/offset_3"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false, 3.0f, 3.0f))
//				              .build();
//
//		LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE =
//				RenderPipeline.builder(LitematicaPipelines.DEBUG_LINES_MASA_SIMPLE_STAGE)
//				              .withLocation(getId("pipeline/debug_lines/masa_simple"))
//				              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
//				              .build();
//
//		// todo LEGACY_TERRAIN Snippet
//		LitematicaPipelines.LEGACY_TERRAIN_STAGE =
//				RenderPipeline.builder()
//				              .withVertexShader(getId("legacy_terrain"))
//				              .withFragmentShader(getId("legacy_terrain"))
//				              .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
//				              .withBindGroupLayout(BindGroupLayouts.FOG)
//				              .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
//				              .withBindGroupLayout(MaLiLibPipelines.LEGACY_TERRAIN_GROUP)
////			                  .withSampler("Sampler0")
////			                  .withSampler("Sampler2")
////			                  .withUniform("ChunkFix", UniformType.UNIFORM_BUFFER)
////			                  .withVertexFormat(DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS)
//                              .withVertexBinding(0, DefaultVertexFormat.BLOCK)
//                              .withPrimitiveTopology(PrimitiveTopology.QUADS)
//                              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
//                              .buildSnippet();
//
//		// todo LEGACY_TERRAIN
//		LitematicaPipelines.LEGACY_SOLID_TERRAIN =
//				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
//				                       .withLocation(getId("pipeline/legacy/solid"))
//				                       .build());
//
//		LitematicaPipelines.LEGACY_WIREFRAME =
//				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
//				                       .withLocation(getId("pipeline/legacy/wireframe"))
//				                       .withPolygonMode(PolygonMode.WIREFRAME)
//				                       .build());
//
//		LitematicaPipelines.LEGACY_CUTOUT_TERRAIN =
//				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
//				                       .withLocation(getId("pipeline/legacy/cutout"))
//				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
//				                       .build());
//
//		// todo LEGACY_TERRAIN_OFFSET --> PRE-REGISTER
//		LitematicaPipelines.LEGACY_SOLID_TERRAIN_OFFSET =
//				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
//				                       .withLocation(getId("pipeline/legacy/solid/masa/offset"))
//				                       .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 0.3f, 0.6f))
//				                       .build());
//
//		LitematicaPipelines.LEGACY_WIREFRAME_OFFSET =
//				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
//				                       .withLocation(getId("pipeline/legacy/wireframe/offset"))
//				                       .withPolygonMode(PolygonMode.WIREFRAME)
//				                       .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 0.3f, 0.6f))
//				                       .build());
//
//		LitematicaPipelines.LEGACY_CUTOUT_TERRAIN_OFFSET =
//				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
//				                       .withLocation(getId("pipeline/legacy/cutout/offset"))
//				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
//				                       .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 0.3f, 0.6f))
//				                       .build());
//
//		// todo LEGACY_TERRAIN_TRANSLUCENT Snippet
//		LitematicaPipelines.LEGACY_TERRAIN_TRANSLUCENT_STAGE =
//				RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
//				              .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
//				              .buildSnippet();
//
//		// todo LEGACY_TERRAIN_TRANSLUCENT
//		LitematicaPipelines.LEGACY_TRANSLUCENT =
//				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_TRANSLUCENT_STAGE)
//				                       .withLocation(getId("pipeline/legacy/translucent"))
//				                       .withShaderDefine("ALPHA_CUTOUT", 0.1F)
//				                       .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
//				                       .build());
//
//		LitematicaPipelines.LEGACY_TRANSLUCENT_OFFSET =
//				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_TRANSLUCENT_STAGE)
//				                       .withLocation(getId("pipeline/legacy/translucent/offset"))
//				                       .withShaderDefine("ALPHA_CUTOUT", 0.1F)
//				                       .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 0.3f, 0.6f))
//				                       .build());
//
//
//		// todo -- Try registering with Iris.
////		IrisCompat.registerPipelines();
//	}
}
