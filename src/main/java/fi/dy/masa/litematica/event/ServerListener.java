package fi.dy.masa.litematica.event;

import net.minecraft.server.integrated.IntegratedServer;
import fi.dy.masa.malilib.interfaces.IServerListener;
import fi.dy.masa.litematica.data.DataManager;

public class ServerListener implements IServerListener
{
    @Override
    public void onServerIntegratedSetup(IntegratedServer server)
    {
        DataManager.getInstance().setHasIntegratedServer(true);
    }
}
