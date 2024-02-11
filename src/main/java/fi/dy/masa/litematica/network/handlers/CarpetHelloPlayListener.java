package fi.dy.masa.litematica.network.handlers;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.network.PacketType;
import fi.dy.masa.malilib.network.handler.ClientCommonHandlerRegister;
import fi.dy.masa.malilib.network.handler.ClientPlayHandler;
import fi.dy.masa.malilib.network.handler.IPluginPlayHandler;
import fi.dy.masa.malilib.network.payload.PayloadCodec;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.malilib.network.payload.PayloadTypeRegister;
import fi.dy.masa.malilib.network.payload.channel.CarpetHelloPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;

import java.util.HashMap;
import java.util.Map;

public abstract class CarpetHelloPlayListener<T extends CustomPayload> implements IPluginPlayHandler<T>
{
    public final static CarpetHelloPlayListener<CarpetHelloPayload> INSTANCE = new CarpetHelloPlayListener<>()
    {
        @Override
        public void receive(CarpetHelloPayload payload, ClientPlayNetworking.Context context)
        {
            ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();

            if (handler != null)
            {
                CarpetHelloPlayListener.INSTANCE.receiveS2CPlayPayload(PayloadType.CARPET_HELLO, payload, handler);
                // TODO --> the networkHandler interface must be used for Carpet Server
                //  because they don't use Fabric API.
            }
            else
                CarpetHelloPlayListener.INSTANCE.receiveS2CPlayPayload(PayloadType.CARPET_HELLO, payload, context);
        }
    };
    private final Map<PayloadType, Boolean> registered = new HashMap<>();
    private final boolean carpetRespond = true;
    private boolean carpetRegister;
    private String carpetVersion;
    @Override
    public PayloadType getPayloadType() { return PayloadType.CARPET_HELLO; }

    @Override
    public void reset(PayloadType type)
    {
        // Don't unregister
        this.carpetRegister = false;
        this.carpetVersion = "";
        CarpetHelloPlayListener.INSTANCE.unregisterPlayHandler(type);
        if (this.registered.containsKey(type))
            this.registered.replace(type, false);
        else
            this.registered.put(type, false);
    }

    @Override
    public <P extends CustomPayload> void receiveS2CPlayPayload(PayloadType type, P payload, ClientPlayNetworking.Context ctx)
    {
        Litematica.debugLog("CarpetHelloPlayListener#receiveS2CPlayPayload(): handling packet via Fabric Network API.");

        CarpetHelloPayload packet = (CarpetHelloPayload) payload;
        ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeS2CNbtCompound(PayloadType.CARPET_HELLO, packet.data());
    }

    @Override
    public <P extends CustomPayload> void receiveS2CPlayPayload(PayloadType type, P payload, ClientPlayNetworkHandler handler)
    {
        // Store the network handler here if wanted
        Litematica.debugLog("CarpetHelloPlayListener#receiveS2CPlayPayload(): handling packet via network handler interface.");

        CarpetHelloPayload packet = (CarpetHelloPayload) payload;
        ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeS2CNbtCompound(PayloadType.CARPET_HELLO, packet.data());
    }

    @Override
    public void decodeS2CNbtCompound(PayloadType type, NbtCompound data)
    {
        // Handle packet.
        if (!this.carpetRegister)
        {
            String carpetVersion = data.getString(PacketType.CarpetHello.HI);
            Litematica.debugLog("CarpetHelloPlayListener#decodeS2CNbtCompound(): received Carpet Hello packet. (Carpet Server {})", carpetVersion);

            // We have a Carpet server.
            this.carpetRegister = true;
            this.carpetVersion = carpetVersion;
            this.registered.replace(type, true);
            if (!DataManager.isCarpetServer())
                DataManager.setIsCarpetServer(true);

            // Respond to Carpet's HI packet.  Set to false if you don't want to participate.
            if (this.carpetRespond)
            {
                // TODO --> Say HELLO back to Mr Gnembon's mod :),
                //  We can fully implement various Carpet Hello packets from here on out directly.
                NbtCompound nbt = new NbtCompound();
                nbt.putString(PacketType.CarpetHello.HELLO, Reference.MOD_ID + "-" + Reference.MOD_VERSION);
                CarpetHelloPlayListener.INSTANCE.encodeC2SNbtCompound(type, nbt);
            }
        }
        else
        {
            // TODO --> Handle additional Carpet Packets
            Litematica.debugLog("CarpetHelloPlayListener#decodeS2CNbtCompound(): received unhandled Carpet Hello packet. (size: {})", data.getSizeInBytes());
        }
    }

    @Override
    public void encodeC2SNbtCompound(PayloadType type, NbtCompound data)
    {
        // Encode Payload
        CarpetHelloPayload newPayload = new CarpetHelloPayload(data);

        // TODO --> Try the NetworkHandler method first for carpet servers
        //  should we store it somewhere?
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler != null)
            CarpetHelloPlayListener.INSTANCE.sendC2SPlayPayload(type, newPayload, handler);
        else
            CarpetHelloPlayListener.INSTANCE.sendC2SPlayPayload(type, newPayload);
    }
    //@Override
    public void sendC2SPlayPayload(PayloadType type, CarpetHelloPayload payload)
    {
        if (ClientPlayNetworking.canSend(payload.getId()))
        {
            ClientPlayNetworking.send(payload);
        }
        else
            Litematica.debugLog("CarpetHelloPlayListener#sendC2SPlayPayload(): [ERROR] canSend = false;");
    }
    //@Override
    public void sendC2SPlayPayload(PayloadType type, CarpetHelloPayload payload, ClientPlayNetworkHandler handler)
    {
        Packet<?> packet = new CustomPayloadC2SPacket(payload);

        if (handler == null)
        {
            Litematica.debugLog("CarpetHelloPlayListener#sendC2SPlayPayload(): [ERROR] networkHandler = null");
            return;
        }
        if (handler.accepts(packet))
        {
            handler.sendPacket(packet);
        }
        else
            Litematica.debugLog("CarpetHelloPlayListener#sendC2SPlayPayload(): [ERROR] accepts() = false");
    }
    @Override
    public void registerPlayPayload(PayloadType type)
    {
        PayloadCodec codec = PayloadTypeRegister.getInstance().getPayloadCodec(type);

        if (codec == null)
        {
            return;
        }
        if (!codec.isPlayRegistered())
        {
            PayloadTypeRegister.getInstance().registerPlayChannel(type, ClientCommonHandlerRegister.getInstance().getPayloadType(type), ClientCommonHandlerRegister.getInstance().getPacketCodec(type));
        }
        //ClientDebugSuite.checkGlobalPlayChannels();
    }
    @Override
    @SuppressWarnings("unchecked")
    public void registerPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadTypeRegister.getInstance().getPayloadCodec(type);

        if (codec == null)
        {
            return;
        }
        if (codec.isPlayRegistered())
        {
            //Litematica.debugLog("CarpetHelloPlayListener#registerPlayHandler(): received for type {}", type.toString());
            ClientCommonHandlerRegister.getInstance().registerPlayHandler((CustomPayload.Id<T>) CarpetHelloPayload.TYPE, this);
            if (this.registered.containsKey(type))
                this.registered.replace(type, true);
            else
                this.registered.put(type, true);
        }
        //ClientDebugSuite.checkGlobalPlayChannels();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void unregisterPlayHandler(PayloadType type)
    {
        PayloadCodec codec = PayloadTypeRegister.getInstance().getPayloadCodec(type);

        if (codec == null)
        {
            return;
        }
        if (codec.isPlayRegistered())
        {
            //Litematica.debugLog("CarpetHelloPlayListener#unregisterPlayHandler(): received for type {}", type.toString());
            //PayloadTypeRegister.getInstance().registerPlayChannel(type, ClientCommonHandlerRegister.getInstance().getPayload(type), ClientCommonHandlerRegister.getInstance().getPacketCodec(type));
            ClientCommonHandlerRegister.getInstance().unregisterPlayHandler((CustomPayload.Id<T>) CarpetHelloPayload.TYPE);
            if (this.registered.containsKey(type))
                this.registered.replace(type, false);
            else
                this.registered.put(type, false);
        }
    }
}
