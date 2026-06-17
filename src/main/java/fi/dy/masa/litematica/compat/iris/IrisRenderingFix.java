package fi.dy.masa.litematica.compat.iris;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.FogType;

import fi.dy.masa.malilib.compat.iris.IrisCompat;
import fi.dy.masa.litematica.mixin.client.IMixinActiveProfiler;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.world.SchematicWorldHandler;

@Deprecated
public class IrisRenderingFix
{
	public static final IrisRenderingFix INSTANCE = new IrisRenderingFix();
	private final Minecraft mc;
	private final CameraRenderState state;
	private Camera camera;
	private ProfilerFiller profiler;
	public boolean wasCalled = false;
	public boolean wasWarned = false;

	private IrisRenderingFix()
	{
		this.mc = Minecraft.getInstance();
		this.state = new CameraRenderState();
	}

	private void prepareProfiler()
	{
		if (this.profiler == null)
		{
			this.profiler = Profiler.get();
		}
		if (this.profiler instanceof ActiveProfiler ps && !((IMixinActiveProfiler) ps).litematica_isStarted())
		{
			this.profiler.startTick();
		}
	}

	private LevelRenderState levelRenderState()
	{
		return this.mc.gameRenderer.gameRenderState().levelRenderState;
	}

	private DeltaTracker deltaTracker()
	{
		return this.mc.getDeltaTracker();
	}

	private boolean hasWorld()
	{
		return SchematicWorldHandler.getSchematicWorld() != null;
	}

	private void extractCameraWithShadersOn(float worldTicks, float cameraTicks)
	{
//		Litematica.LOGGER.warn("[IrisFix] extractCameraWithShadersOn()");
		CameraRenderState state = this.state;

		this.camera.extractRenderState(state, cameraTicks);
		state.fogType = FogType.NONE;
		state.fogData = new FogData();
		state.fogData.environmentalStart = Float.MAX_VALUE - 4.0F;
		state.fogData.renderDistanceStart = Float.MAX_VALUE - 4.0F;
		state.fogData.environmentalEnd = Float.MAX_VALUE;
		state.fogData.renderDistanceEnd = Float.MAX_VALUE;
		state.fogData.skyEnd = Float.MAX_VALUE;
		state.fogData.cloudEnd = Float.MAX_VALUE;
		state.fogData.color = new Vector4f(0.0F);

		LitematicaRenderer.getInstance().updateCameraState(this.camera, cameraTicks, this.state);
	}

	private void extractFogBufferWithShadersOn()
	{
		LitematicaRenderer.getInstance().getWorldRenderer().getFogRenderer().updateBuffer(this.state.fogData);
		GpuBufferSlice fogBuffer = LitematicaRenderer.getInstance().getWorldRenderer().getFogRenderer().getBuffer(FogRenderer.FogMode.NONE);
		LitematicaRenderer.getInstance().capturePreMainValues(this.state, fogBuffer, this.profiler);
	}

	public void setCamera(Camera camera)
	{
//		Litematica.LOGGER.warn("[IrisFix] setCamera()");
		this.camera = camera;
	}

	public void extractAndCompileSectionsWithShadersOn()
	{
		if (IrisCompat.isShaderActive() && this.hasWorld() && this.mc.isGameLoadFinished())
		{
//			Litematica.LOGGER.warn("[IrisFix] extractAndCompileSectionsWithShadersOn()");
			this.prepareProfiler();
			Matrix4f modelViewMatrix = new Matrix4f();
			Frustum frustum = this.camera.getCullFrustum();
			float worldTicks = this.deltaTracker().getGameTimeDeltaPartialTick(false);
			float cameraTicks = this.camera.getCameraEntityPartialTicks(this.deltaTracker());
			if (!this.wasCalled) { this.wasCalled = true; }

			this.extractCameraWithShadersOn(worldTicks, cameraTicks);
			this.camera.getViewRotationMatrix(modelViewMatrix);

			LitematicaRenderer.getInstance().piecewisePrepareEntities(this.camera, frustum, this.levelRenderState(), this.deltaTracker(), this.profiler);
			LitematicaRenderer.getInstance().piecewisePrepareBlockEntities(this.camera, this.levelRenderState(), worldTicks, this.profiler);

			this.extractFogBufferWithShadersOn();
			LitematicaRenderer.getInstance().piecewisePrepareBlockLayers(modelViewMatrix, this.profiler);
		}
	}
}
