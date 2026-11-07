package fi.dy.masa.litematica.network;

import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import fi.dy.masa.malilib.network.IPluginChannelHandler;
import fi.dy.masa.malilib.network.PacketSplitter;
import fi.dy.masa.litematica.Litematica;

/**
 * Client side handler for the Servux {@code servux:litematics} channel (1.20.1 port).
 * Used to send schematic paste requests to a server that has the Servux 1.20.1 port installed.
 */
public class ServuxLitematicaHandler implements IPluginChannelHandler
{
    public static final Identifier CHANNEL = ServuxLitematicaPacket.CHANNEL;
    public static final ServuxLitematicaHandler INSTANCE = new ServuxLitematicaHandler();

    private boolean serverHasServux;
    private ClientPlayNetworkHandler lastNetworkHandler;

    private ServuxLitematicaHandler()
    {
    }

    @Override
    public List<Identifier> getChannels()
    {
        return List.of(CHANNEL);
    }

    @Override
    public boolean registerToServer()
    {
        return false;
    }

    @Override
    public boolean usePacketSplitter()
    {
        return true;
    }

    @Override
    public void onPacketReceived(PacketByteBuf buf)
    {
        ServuxLitematicaPacket packet = ServuxLitematicaPacket.fromPacket(buf);

        if (packet == null)
        {
            return;
        }

        switch (packet.getType())
        {
            case PACKET_S2C_METADATA:
                this.lastNetworkHandler = MinecraftClient.getInstance().getNetworkHandler();
                this.serverHasServux = true;
                Litematica.logger.info("ServuxLitematicaHandler: server has the Servux litematics channel");
                break;
            case PACKET_S2C_PASTE_RESULT:
                if (packet.getCompound().getBoolean("success") == false)
                {
                    Litematica.logger.warn("Servux paste failed: {}", packet.getCompound().getString("message"));
                }
                break;
            default:
                break;
        }
    }

    public boolean hasServuxServer()
    {
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();

        if (handler == null)
        {
            return false;
        }

        if (handler != this.lastNetworkHandler)
        {
            // A different server/world connection than the one that sent the metadata
            this.serverHasServux = false;
        }

        return this.serverHasServux;
    }

    public boolean requestMetadata()
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();

        if (handler == null)
        {
            return false;
        }

        this.lastNetworkHandler = handler;
        PacketSplitter.send(handler, CHANNEL, ServuxLitematicaPacket.toBuffer(ServuxLitematicaPacket.MetadataRequest()));
        return true;
    }

    public void reset()
    {
        this.serverHasServux = false;
        this.lastNetworkHandler = null;
    }

    public boolean sendPasteRequest(NbtCompound nbt)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();

        if (handler == null)
        {
            return false;
        }

        PacketSplitter.send(handler, CHANNEL, ServuxLitematicaPacket.toBuffer(ServuxLitematicaPacket.PasteRequest(nbt)));
        return true;
    }

    public static void register()
    {
        fi.dy.masa.malilib.network.ClientPacketChannelHandler.getInstance()
                .registerClientChannelHandler(INSTANCE);
    }
}
