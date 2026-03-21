package fi.dy.masa.litematica.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.PlacementManagerDaemonHandler;

@Mixin(value = Minecraft.class)
public abstract class MixinMinecraft extends ReentrantBlockableEventLoop<Runnable>
{
    public MixinMinecraft(String name, boolean propagatesCrashes)
    {
        super(name, propagatesCrashes);
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void litematica_onRunTickStart(CallbackInfo ci)
    {
        DataManager.onClientTickStart();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void litematica_onRunStop(CallbackInfo ci)
    {
        PlacementManagerDaemonHandler.INSTANCE.endAll();
    }
}
