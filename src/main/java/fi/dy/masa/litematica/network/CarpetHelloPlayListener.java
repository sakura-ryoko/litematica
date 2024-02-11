package fi.dy.masa.litematica.network;

import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.MaLiLibReference;
import fi.dy.masa.malilib.network.handler.ClientCommonHandlerRegister;
import fi.dy.masa.malilib.network.handler.ClientPlayHandler;
import fi.dy.masa.malilib.network.handler.IPluginPlayHandler;
import fi.dy.masa.malilib.network.packet.PacketType;
import fi.dy.masa.malilib.network.payload.PayloadCodec;
import fi.dy.masa.malilib.network.payload.PayloadType;
import fi.dy.masa.malilib.network.payload.PayloadTypeRegister;
import fi.dy.masa.malilib.network.payload.channel.CarpetHelloPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.CustomPayload;

import java.util.HashMap;
import java.util.Map;

public abstract class CarpetHelloPlayListener<T extends CustomPayload> implements IPluginPlayHandler<T>
{
    public final static CarpetHelloPlayListener<CarpetHelloPayload> INSTANCE = new CarpetHelloPlayListener<>()
    {
        @Override
        public void receive(CarpetHelloPayload payload, ClientPlayNetworking.Context context)
        {
            //Litematica.debugLog("CarpetHelloPlayListener#receive(): received a Carpet Hello payload.");
            //((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).receiveS2CPlayPayload(PayloadType.CARPET_HELLO, payload, context);
            CarpetHelloPlayListener.INSTANCE.receiveS2CPlayPayload(PayloadType.CARPET_HELLO, payload, context);
        }
    };
    private final Map<PayloadType, Boolean> registered = new HashMap<>();
    @Override
    public PayloadType getPayloadType() { return PayloadType.CARPET_HELLO; }

    @Override
    public void reset(PayloadType type)
    {
        // Don't unregister
        unregisterPlayHandler(type);
        if (this.registered.containsKey(type))
            this.registered.replace(type, false);
        else
            this.registered.put(type, false);
    }

    @Override
    public <P extends CustomPayload> void receiveS2CPlayPayload(PayloadType type, P payload, ClientPlayNetworking.Context ctx)
    {
        //IPluginPlayHandler.super.receiveS2CPlayPayload(type, payload, ctx);
        CarpetHelloPayload packet = (CarpetHelloPayload) payload;
        //Litematica.debugLog("CarpetHelloPlayListener#receiveS2CPlayPayload(): received a CarpetHello payload.");
        ((ClientPlayHandler<?>) ClientPlayHandler.getInstance()).decodeS2CNbtCompound(PayloadType.CARPET_HELLO, packet.data());
    }

    @Override
    public void decodeS2CNbtCompound(PayloadType type, NbtCompound data)
    {
        //IPluginPlayHandler.super.decodeS2CNbtCompound(type, data);

        // Handle packet.
        String carpetVersion = data.getString(PacketType.CarpetHello.HI);
        Litematica.debugLog("CarpetHelloPlayListener#decodeS2CNbtCompound(): received Carpet Hello packet. (Carpet Server {})", carpetVersion);
        if (!DataManager.isCarpetServer())
            DataManager.setIsCarpetServer(true);

        this.registered.replace(type, true);
        NbtCompound nbt = new NbtCompound();
        nbt.putString(PacketType.CarpetHello.HELLO, MaLiLibReference.MOD_ID+"-"+MaLiLibReference.MOD_VERSION);
        encodeC2SNbtCompound(type, nbt);
    }

    @Override
    public void encodeC2SNbtCompound(PayloadType type, NbtCompound data)
    {
        //IPluginPlayHandler.super.encodeC2SNbtCompound(type, data);

        //Litematica.debugLog("CarpetHelloPlayListener#encodeC2SNbtCompound(): encoding Carpet Hello packet.");

        // Encode Payload
        CarpetHelloPayload payload = new CarpetHelloPayload(data);
        sendC2SPlayPayload(type, payload);
    }
    //@Override
    public void sendC2SPlayPayload(PayloadType type, CarpetHelloPayload payload)
    {
        //IPluginPlayHandler.super.sendC2SPlayPayload(type, payload);
        Litematica.debugLog("CarpetHelloPlayListener#sendC2SPlayPayload(): sending Carpet Hello packet.");

        if (ClientPlayNetworking.canSend(payload.getId()))
        {
            //Litematica.debugLog("CarpetHelloPlayListener#sendC2SPlayPayload(): canSend = true;");
            ClientPlayNetworking.send(payload);
        }
        else
            Litematica.debugLog("CarpetHelloPlayListener#sendC2SPlayPayload(): [ERROR] canSend = false;");
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
            Litematica.debugLog("CarpetHelloPlayListener#registerPlayHandler(): received for type {}", type.toString());
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
            Litematica.debugLog("CarpetHelloPlayListener#unregisterPlayHandler(): received for type {}", type.toString());
            //PayloadTypeRegister.getInstance().registerPlayChannel(type, ClientCommonHandlerRegister.getInstance().getPayload(type), ClientCommonHandlerRegister.getInstance().getPacketCodec(type));
            ClientCommonHandlerRegister.getInstance().unregisterPlayHandler((CustomPayload.Id<T>) CarpetHelloPayload.TYPE);
            if (this.registered.containsKey(type))
                this.registered.replace(type, false);
            else
                this.registered.put(type, false);
        }
    }
}
