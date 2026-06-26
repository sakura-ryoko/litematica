package fi.dy.masa.litematica.compat.iris;

import java.util.OptionalDouble;
import javax.annotation.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.FogType;

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.mixin.client.IMixinActiveProfiler;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

public class IrisRenderingFix
{
	public static final IrisRenderingFix INSTANCE = new IrisRenderingFix();
//	private final Minecraft mc;
//	private final CameraRenderState state;
//	private Camera camera;
//	private @Nullable GpuSampler sampler;
//	private GpuBufferSlice fogBuffer;
//	private ProfilerFiller profiler;
//	public boolean wasCalled = false;
	public boolean wasWarned = false;

	private IrisRenderingFix()
	{
//		this.mc = Minecraft.getInstance();
//		this.state = new CameraRenderState();
	}

//	private void prepareProfiler()
//	{
//		if (this.profiler == null)
//		{
//			this.profiler = Profiler.get();
//		}
//		if (this.profiler instanceof ActiveProfiler ps && !((IMixinActiveProfiler) ps).litematica_isStarted())
//		{
//			this.profiler.startTick();
//		}
//	}
//
//	private LevelRenderState levelRenderState()
//	{
//		return this.mc.gameRenderer.gameRenderState().levelRenderState;
//	}
//
//	private DeltaTracker deltaTracker()
//	{
//		return this.mc.getDeltaTracker();
//	}
//
//	private boolean hasWorld()
//	{
//		return SchematicWorldHandler.getSchematicWorld() != null;
//	}
//
//	private void extractCameraWithShadersOn(float worldTicks, float cameraTicks)
//	{
////		Litematica.LOGGER.warn("[IrisFix] extractCameraWithShadersOn()");
//		CameraRenderState state = this.state;
//
//		this.camera.extractRenderState(state, cameraTicks);
//		state.fogType = FogType.NONE;
//		state.fogData = new FogData();
//		state.fogData.environmentalStart = Float.MAX_VALUE - 4.0F;
//		state.fogData.renderDistanceStart = Float.MAX_VALUE - 4.0F;
//		state.fogData.environmentalEnd = Float.MAX_VALUE;
//		state.fogData.renderDistanceEnd = Float.MAX_VALUE;
//		state.fogData.skyEnd = Float.MAX_VALUE;
//		state.fogData.cloudEnd = Float.MAX_VALUE;
//		state.fogData.color = new Vector4f(0.0F);
//
//		LitematicaRenderer.getInstance().updateCameraState(this.camera, cameraTicks, this.state);
//	}
//
//	private void extractFogBufferWithShadersOn()
//	{
////		Litematica.LOGGER.warn("[IrisFix] extractFogBufferWithShadersOn()");
//		LitematicaRenderer.getInstance().getWorldRenderer().getFogRenderer().updateBuffer(this.state.fogData);
//		GpuBufferSlice fogBuffer = LitematicaRenderer.getInstance().getWorldRenderer().getFogRenderer().getBuffer(FogRenderer.FogMode.NONE);
//		this.fogBuffer = fogBuffer;
//		LitematicaRenderer.getInstance().capturePreMainValues(this.state, fogBuffer, this.profiler);
//	}
//
//	public void setCamera(Camera camera)
//	{
////		Litematica.LOGGER.warn("[IrisFix] setCamera()");
//		this.camera = camera;
//	}
//
//	// Update & Extract Phase
//	public void extractAndCompileSectionsWithShadersOn()
//	{
//		if (IrisCompat.isShaderActive() && this.hasWorld() && this.mc.isGameLoadFinished())
//		{
////			Litematica.LOGGER.warn("[IrisFix] extractAndCompileSectionsWithShadersOn()");
//			this.prepareProfiler();
//			Matrix4f modelViewMatrix = new Matrix4f();
//			Frustum frustum = this.camera.getCullFrustum();
//			float worldTicks = this.deltaTracker().getGameTimeDeltaPartialTick(false);
//			float cameraTicks = this.camera.getCameraEntityPartialTicks(this.deltaTracker());
//			if (!this.wasCalled) { this.wasCalled = true; }
//
//			this.extractCameraWithShadersOn(worldTicks, cameraTicks);
//			this.camera.getViewRotationMatrix(modelViewMatrix);
//
//			LitematicaRenderer.getInstance().piecewisePrepareEntities(this.camera, frustum, this.levelRenderState(), this.deltaTracker(), this.profiler);
//			LitematicaRenderer.getInstance().piecewisePrepareBlockEntities(this.camera, this.levelRenderState(), worldTicks, this.profiler);
//
//			this.extractFogBufferWithShadersOn();
//			LitematicaRenderer.getInstance().piecewisePrepareBlockLayers(modelViewMatrix, this.profiler);
//		}
//	}
//
//	// Draw Phase
//	public void renderMainPassWithShadersOn(FrameGraphBuilder frame, LevelTargetBundle targets, FeatureRenderDispatcher.PreparedFrame features, ProfilerFiller profiler)
//	{
//		if (IrisCompat.isShaderActive() && this.hasWorld() && this.mc.isGameLoadFinished())
//		{
////			Litematica.LOGGER.error("[IrisFix] renderMainPassWithShadersOn()");
//			FramePass framePass = frame.addPass("litematica_frame_pass");
//			targets.main = framePass.readsAndWrites(targets.main);
//
//			if (targets.translucent != null)
//			{
//				targets.translucent = framePass.readsAndWrites(targets.translucent);
//			}
//
//			ResourceHandle<RenderTarget> main = targets.main;
//			ResourceHandle<RenderTarget> translucent = targets.translucent;
//
//			framePass.executes(
//					() ->
//					{
//						GpuDevice device = RenderSystem.getDevice();
////						GpuBufferSlice origFog = RenderSystem.getShaderFog();
////						RenderSystem.setShaderFog(this.fogBuffer);
//
//						if (this.sampler != null)
//						{
//							this.sampler.close();
//						}
//
//						this.sampler = device.createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
//
//						profiler.push("litematica_opaque");
//						LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(ChunkSectionLayerGroup.OPAQUE, this.sampler);
//
//						if (translucent != null)
//						{
//							translucent.get().copyDepthFrom(main.get());
//						}
//
//						profiler.popPush("litematica_translucent");
//						LitematicaRenderer.getInstance().piecewiseDrawBlockLayerGroup(ChunkSectionLayerGroup.TRANSLUCENT, this.sampler);
//						profiler.pop();
//
////						if (origFog != null)
////						{
////							RenderSystem.setShaderFog(origFog);
////						}
//					}
//			);
//		}
//	}
}
