package fi.dy.masa.litematica.network.packet;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.malilib.event.CarpetHelloHandler;

public class PacketProvider
{
    static CarpetHelloListener litematica_carpetListener = new CarpetHelloListener();
    public static void registerPayloads()
    {
        // Register Client Payload Listeners
        Litematica.debugLog("PacketProvider#litematica_registerPayloads(): registerCarpetHandler()");
        CarpetHelloHandler.getInstance().registerCarpetHelloHandler(litematica_carpetListener);
    }

    public static void unregisterPayloads()
    {
        Litematica.debugLog("PacketProvider#litematica_unregisterPayloads(): unregisterCarpetHandler()");
        CarpetHelloHandler.getInstance().unregisterCarpetHelloHandler(litematica_carpetListener);
    }
}