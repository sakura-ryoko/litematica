package fi.dy.masa.litematica.deprecated;

import fi.dy.masa.litematica.Litematica;
import net.minecraft.server.MinecraftServer;

@Deprecated(forRemoval = true)
public class ServerListener
        //implements IServerListener
{
    /**
     * This interface for IntegratedServers() works much more reliably than invoking a WorldLoadHandler
     */

    public void onServerStarting(MinecraftServer minecraftServer)
    {
        //PacketUtils.registerPayloads();

        //ClientDebugSuite.checkGlobalPlayChannels();
        //ClientDebugSuite.checkGlobalConfigChannels();

        Litematica.debugLog("MinecraftServerEvents#onServerStarting(): invoked.");
    }
    public void onServerStarted(MinecraftServer minecraftServer)
    {
        //ClientDebugSuite.checkGlobalPlayChannels();
        //ClientDebugSuite.checkGlobalConfigChannels();
        Litematica.debugLog("MinecraftServerEvents#onServerStarted(): invoked.");
    }
    public void onServerStopping(MinecraftServer minecraftServer)
    {
        //ClientDebugSuite.checkGlobalPlayChannels();
        //ClientDebugSuite.checkGlobalConfigChannels();
        Litematica.debugLog("MinecraftServerEvents#onServerStopping(): invoked.");
    }
    public void onServerStopped(MinecraftServer minecraftServer)
    {
        //ClientDebugSuite.checkGlobalPlayChannels();
        //ClientDebugSuite.checkGlobalConfigChannels();
        Litematica.debugLog("MinecraftServerEvents#onServerStopped(): invoked.");
    }
}
