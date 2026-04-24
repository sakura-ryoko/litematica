package fi.dy.masa.litematica.mixin.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.mixin.client.IMixinProfilerSystem;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.util.IEntityHitboxDebugRendererInvoker;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class MixinEntityHitboxDebugRenderer implements IEntityHitboxDebugRendererInvoker
{
	@Shadow protected abstract void showHitboxes(Entity entity, float partialTicks, boolean isServerEntity);

	@Inject(method = "emitGizmos", at = @At("TAIL"))
	private void litematica_renderEntityHitboxes(double camX, double camY, double camZ,
	                                             DebugValueAccess debugValues,
	                                             Frustum frustum, float partialTicks,
	                                             CallbackInfo ci)
	{
		ProfilerFiller profiler = Profiler.get();

		if (profiler instanceof ActiveProfiler ps && !((IMixinProfilerSystem) ps).litematica_isStarted())
		{
			profiler.startTick();
		}

		LitematicaRenderer.getInstance().renderEntityDebugHitboxes((IEntityHitboxDebugRendererInvoker) this, camX, camY, camZ, debugValues, frustum, partialTicks, profiler);
	}

	@Override
	public void litematica$addEntityHitbox(Entity entity, float partialTicks, boolean serverEntity)
	{
		this.showHitboxes(entity, partialTicks, serverEntity);
	}
}
