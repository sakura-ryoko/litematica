package fi.dy.masa.litematica.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Deprecated(forRemoval = true)
@Environment(EnvType.CLIENT)
public class PacketListenerRegister
{
    //static CarpetHelloPlayListener<CarpetHelloPayload> litematica_CarpetHelloListener = CarpetHelloPlayListener.INSTANCE;
    private static boolean payloadsRegistered = false;
    public static void registerListeners()
    {
        if (payloadsRegistered)
            return;
        // Register Client Payload Listeners
        //Litematica.debugLog("PacketListenerRegister#litematica_registerPayloads(): registerCarpetHandler()");

        //ClientPlayHandler.getInstance().registerClientPlayHandler(litematica_CarpetHelloListener);

        payloadsRegistered = true;
    }

    public static void unregisterListeners()
    {
        //Litematica.debugLog("PacketListenerRegister#litematica_unregisterPayloads(): unregisterCarpetHandler()");

        //ClientPlayHandler.getInstance().unregisterClientPlayHandler(litematica_CarpetHelloListener);

        payloadsRegistered = false;
    }
}
