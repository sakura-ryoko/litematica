package fi.dy.masa.litematica.mixin.server;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.litematica.scheduler.TaskScheduler;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer
{
    @Shadow public abstract boolean isDedicated();
    @Shadow public abstract boolean shouldBroadcastConsoleToOps();

    @Inject(method = "pushTickLog", at = @At("HEAD"))
    private void litematica_onServerTickOmegaHackFixBecauseLunarBreaksMinecraftEvenThoughABooleanSupplierIsAlwaysSupposedToBeThereEvenAccordingToMojangMappingsButIsNotWhenYouAreRunningLunarAndTheyCannotExplainWhyThisWouldBreakButItDoesEvenThoughNothingHasChangedInMinecraftUnlessYouArePlayingWithLunar(long tickStartTime, CallbackInfo ci)
    {
        if (!this.isDedicated() && this.shouldBroadcastConsoleToOps())
        {
            TaskScheduler.getInstanceServer().runTasks();
        }
    }
}
