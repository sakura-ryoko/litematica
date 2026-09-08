package fi.dy.masa.litematica.mixin.render;

import java.util.Map;
import org.jetbrains.annotations.ApiStatus;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.*;
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

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.compat.iris.IrisCompat;
import fi.dy.masa.litematica.render.LitematicaPipelines;

@ApiStatus.Internal
@Mixin(value = RenderPipelines.class, priority = 600)
public abstract class MixinRenderPipelines
{
	@Shadow @Final private static Map<Identifier, RenderPipeline> PIPELINES_BY_LOCATION;

	@Shadow
	private static RenderPipeline register(RenderPipeline pipeline)
	{
		PIPELINES_BY_LOCATION.put(pipeline.getLocation(), pipeline);
		return pipeline;
	}

	@Unique
	private static Identifier getId(String id)
	{
		return Identifier.fromNamespaceAndPath(Reference.MOD_ID, id);
	}

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void litematica_onRegisterPipelines(CallbackInfo ci)
	{
		// todo LEGACY_TERRAIN Snippet
		LitematicaPipelines.LEGACY_TERRAIN_STAGE =
				RenderPipeline.builder()
				              .withVertexShader(getId("legacy_terrain"))
				              .withFragmentShader(getId("legacy_terrain"))
				              .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
				              .withBindGroupLayout(BindGroupLayouts.PROJECTION)
				              .withBindGroupLayout(BindGroupLayouts.FOG)
				              .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
				              .withBindGroupLayout(LitematicaPipelines.LEGACY_TERRAIN_GROUP)
                              .withVertexBinding(0, DefaultVertexFormat.BLOCK)
                              .withPrimitiveTopology(PrimitiveTopology.QUADS)
                              .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
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
				                       .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 0.3f, 0.6f))
				                       .build());

		LitematicaPipelines.LEGACY_WIREFRAME_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/legacy/wireframe/offset"))
				                       .withPolygonMode(PolygonMode.WIREFRAME)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 0.3f, 0.6f))
				                       .build());

		LitematicaPipelines.LEGACY_CUTOUT_TERRAIN_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
				                       .withLocation(getId("pipeline/legacy/cutout/offset"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.5F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 0.3f, 0.6f))
				                       .build());

		// todo LEGACY_TERRAIN_TRANSLUCENT Snippet
		LitematicaPipelines.LEGACY_TERRAIN_TRANSLUCENT_STAGE =
				RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_STAGE)
				              .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
				              .buildSnippet();

		// todo LEGACY_TERRAIN_TRANSLUCENT
		LitematicaPipelines.LEGACY_TRANSLUCENT =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_TRANSLUCENT_STAGE)
				                       .withLocation(getId("pipeline/legacy/translucent"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.1F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
				                       .build());

		LitematicaPipelines.LEGACY_TRANSLUCENT_OFFSET =
				register(RenderPipeline.builder(LitematicaPipelines.LEGACY_TERRAIN_TRANSLUCENT_STAGE)
				                       .withLocation(getId("pipeline/legacy/translucent/offset"))
				                       .withShaderDefine("ALPHA_CUTOUT", 0.1F)
				                       .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 0.3f, 0.6f))
				                       .build());

		// todo -- Try registering with Iris.
		IrisCompat.registerPipelines();
	}
}
