package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;

/**
 * They keep moving where the effective CustomPayload handling is... keeping them both
 */
@Mixin(ClientCommonNetworkHandler.class)
public class MixinClientCommonNetworkHandler
{
    @Inject(method = "onCustomPayload(Lnet/minecraft/network/packet/s2c/common/CustomPayloadS2CPacket;)V", at = @At("HEAD"))
    private void litematica_onCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo ci)
    {
        if (packet.payload().getId().id().equals(DataManager.CARPET_HELLO))
        {
            Litematica.debugLog("ClientCommonNetworkHandler#litematica_onCustomPayload(): received carpet hello packet");
            DataManager.setIsCarpetServer(true);
        }
    }
}
