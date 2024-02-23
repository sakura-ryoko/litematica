package fi.dy.masa.litematica.network;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.network.handlers.CarpetHelloPlayListener;
import fi.dy.masa.malilib.network.handler.play.ClientPlayHandler;
import fi.dy.masa.malilib.network.payload.channel.CarpetHelloPayload;

public class PacketUtils
{
    static CarpetHelloPlayListener<CarpetHelloPayload> litematica_CarpetHelloListener = CarpetHelloPlayListener.INSTANCE;
    private static boolean payloadsRegistered = false;
    public static void registerPayloads()
    {
        if (payloadsRegistered)
            return;
        // Register Client Payload Listeners
        Litematica.debugLog("PacketUtils#litematica_registerPayloads(): registerCarpetHandler()");

        ClientPlayHandler.getInstance().registerClientPlayHandler(litematica_CarpetHelloListener);

        payloadsRegistered = true;
    }

    public static void unregisterPayloads()
    {
        Litematica.debugLog("PacketUtils#litematica_unregisterPayloads(): unregisterCarpetHandler()");

        ClientPlayHandler.getInstance().unregisterClientPlayHandler(litematica_CarpetHelloListener);

        payloadsRegistered = false;
    }
}
