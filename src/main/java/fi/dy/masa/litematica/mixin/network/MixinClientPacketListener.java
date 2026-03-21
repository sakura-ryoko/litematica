package fi.dy.masa.litematica.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener
{
    @Inject(method = "handleLevelChunkWithLight", at = @At("RETURN"))
    private void litematica_onUpdateChunk(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci)
    {
        int chunkX = packet.getX();
        int chunkZ = packet.getZ();
        //Litematica.debugLog("MixinClientPlayNetworkHandler#litematica_onUpdateChunk({}, {})", chunkX, chunkZ);

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue())
        {
            SchematicWorldRefresher.INSTANCE.markSchematicChunksForRenderUpdate(chunkX, chunkZ);
        }

        DataManager.getSchematicPlacementManager().onClientChunkLoad(chunkX, chunkZ);
        // TODO verifier updates?
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("RETURN"))
    private void litematica_onChunkUnload(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false)
        {
            //Litematica.debugLog("MixinClientPlayNetworkHandler#litematica_onChunkUnload({}, {})", packet.pos().x, packet.pos().z);
            DataManager.getSchematicPlacementManager().onClientChunkUnload(packet.pos().x(), packet.pos().z());
        }
    }

    @Inject(method = "handleSystemChat", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/chat/ChatListener;handleSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void litematica_onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci)
    {
        if (DataManager.onChatMessage(packet.content()))
        {
            ci.cancel();
        }
    }

    /**
     * They keep moving where the effective onCustomPayload handling is... keeping them both
     */
    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void litematica_onCustomPayload(CustomPacketPayload payload, CallbackInfo ci)
    {
        if (payload.type().id().equals(DataManager.CARPET_HELLO))
        {
            Litematica.debugLog("MixinClientPlayNetworkHandler#litematica_onCustomPayload(): received carpet hello packet");
            DataManager.setIsCarpetServer(true);
        }
        else if (payload.type().id().getNamespace().equals("servux"))
        {
            DataManager.setHasServuxServer(true);
        }
    }

    @Inject(method = "handleTagQueryPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/DebugQueryHandler;handleResponse(ILnet/minecraft/nbt/CompoundTag;)Z"))
    private void litematica_onQueryResponse(ClientboundTagQueryPacket packet, CallbackInfo ci)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            EntitiesDataStorage.getInstance().handleVanillaQueryNbt(packet.getTransactionId(), packet.getTag());
        }
    }

    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void minihud_onCommandTree(CallbackInfo ci)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            // when the player becomes OP, the server sends the command tree to the client
            EntitiesDataStorage.getInstance().resetOpCheck();
        }
    }
}
