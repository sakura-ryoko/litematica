package fi.dy.masa.litematica.network.test;

import fi.dy.masa.litematica.Litematica;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.Identifier;

import java.util.Iterator;
import java.util.Set;

public class ClientDebugSuite {
    public static void checkGlobalChannels() {
        Litematica.debugLog("ClientDebugSuite#checkGlobalChannels(): Start.");
        Set<Identifier> channels = ClientPlayNetworking.getGlobalReceivers();
        Iterator<Identifier> iterator = channels.iterator();
        int i = 0;
        while (iterator.hasNext())
        {
            Identifier id = iterator.next();
            i++;
            Litematica.debugLog("ClientDebugSuite#checkGlobalChannels(): id("+i+") hash: "+id.hashCode()+" //name: "+id.getNamespace()+" path: "+id.getPath());
        }
        Litematica.debugLog("ClientDebugSuite#checkGlobalChannels(): END. Total Channels: "+i);
    }
}
