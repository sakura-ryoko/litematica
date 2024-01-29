package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.network.test.ClientDebugSuite;
import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.malilib.network.ClientNetworkPlayInitHandler;
import fi.dy.masa.malilib.network.ClientNetworkPlayRegister;
import net.minecraft.server.MinecraftServer;

public class ServerListener implements IServerListener
{
    /**
     * This interface for IntegratedServers() works much more reliably than invoking a WorldLoadHandler
     * @param minecraftServer
     */

    public void onServerStarting(MinecraftServer minecraftServer)
    {
        ClientNetworkPlayInitHandler.registerPlayChannels();
        ClientDebugSuite.checkGlobalChannels();
        Litematica.debugLog("MinecraftServerEvents#onServerStarting(): invoked.");
    }
    public void onServerStarted(MinecraftServer minecraftServer)
    {
        ClientNetworkPlayRegister.registerDefaultReceivers();
        ClientDebugSuite.checkGlobalChannels();
        Litematica.debugLog("MinecraftServerEvents#onServerStarted(): invoked.");
    }
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        ClientDebugSuite.checkGlobalChannels();
        Litematica.debugLog("MinecraftServerEvents#onServerStopping(): invoked.");
    }
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        ClientNetworkPlayRegister.unregisterDefaultReceivers();
        ClientDebugSuite.checkGlobalChannels();
        Litematica.debugLog("MinecraftServerEvents#onServerStopped(): invoked.");
    }
}
