package fi.dy.masa.litematica.network.packet;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.malilib.event.CarpetHandler;

public class PacketProvider
{
    static fi.dy.masa.litematica.network.packet.CarpetPayloadListener litematica_carpetListener = new fi.dy.masa.litematica.network.packet.CarpetPayloadListener();
    public static void registerPayloads()
    {
        // Register Client Payload Listeners
        Litematica.debugLog("PacketProvider#litematica_registerPayloads(): registerCarpetHandler()");
        CarpetHandler.getInstance().registerCarpetHandler(litematica_carpetListener);
    }

    public static void unregisterPayloads()
    {
        Litematica.debugLog("PacketProvider#litematica_unregisterPayloads(): unregisterCarpetHandler()");
        CarpetHandler.getInstance().unregisterCarpetHandler(litematica_carpetListener);
    }
}