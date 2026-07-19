package fi.dy.masa.litematica.network;

import java.util.Optional;
import javax.annotation.Nullable;
import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.PacketSplitter;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.data.tag.BaseData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.util.DataByteBufUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.EntityDataManager;

@Environment(EnvType.CLIENT)
public abstract class ServuxLitematicaHandler<T extends CustomPacketPayload> implements IPluginClientPlayHandler<T>
{
    private final static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> INSTANCE = new ServuxLitematicaHandler<>()
    {
        @Override
        public void receive(ServuxLitematicaPacket.@NonNull Payload payload, ClientPlayNetworking.@NonNull Context context)
        {
            ServuxLitematicaHandler.INSTANCE.receivePlayPayload(payload, context);
        }
    };
    public static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> getInstance() { return INSTANCE; }

    public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath("servux", "litematics");

    private boolean servuxRegistered;
    private boolean payloadRegistered = false;
    private int failures = 0;
    private long readingSessionKey = -1;

    @Override
    public Identifier getPayloadChannel() { return CHANNEL_ID; }

    @Override
    public boolean isPlayRegistered(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID))
        {
            return this.payloadRegistered;
        }

        return false;
    }

    @Override
    public void setPlayRegistered(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID))
        {
            this.payloadRegistered = true;
        }
    }

    @Override
    public <P extends IClientPayloadData> void decodeClientData(Identifier channel, P data)
    {
        if (!channel.equals(CHANNEL_ID))
        {
            return;
        }
        if (!EntityDataManager.getInstance().isEnabled() || !this.checkFailures())
        {
            return;
        }

        if (data instanceof ServuxLitematicaPacket packet)
        {
            switch (packet.getType())
            {
                case PACKET_S2C_METADATA ->
                {
                    if (EntityDataManager.getInstance().receiveServuxMetadata(packet.getCompound()))
                    {
                        this.servuxRegistered = true;
                    }
                }
                // TODO
//                case PACKET_S2C_TASK_RESPONSE ->
//                {
//                    if (this.servuxRegistered)
//                    {
//                        EntityDataManager.getInstance().receiveServuxTaskResponse(packet.getCompound());
//                    }
//                }
                case PACKET_S2C_TASK_STATUS_SYNC ->
                {
                    if (this.servuxRegistered)
                    {
                        EntityDataManager.getInstance().receiveServuxTaskStatusSync(packet.getCompound());
                    }
                }
                case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
                {
                    if (this.servuxRegistered)
                    {
                        EntityDataManager.getInstance().handleBlockEntityData(packet.getPos(), packet.getCompound());
                    }
                }
                case PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE ->
                {
                    if (this.servuxRegistered)
                    {
                        EntityDataManager.getInstance().handleEntityData(packet.getEntityId(), packet.getCompound());
                    }
                }
                case PACKET_S2C_NBT_RESPONSE_DATA ->
                {
                    if (!this.servuxRegistered)
                    {
                        return;
                    }
                    if (this.readingSessionKey == -1)
                    {
                        this.readingSessionKey = RandomSource.create(Util.getMillis()).nextLong();
                    }

                    Litematica.debugLog("ServuxLitematicaHandler#decodeClientData(): received Litematic Data Packet Slice of size {} (in bytes) // reading session key [{}]", packet.getTotalSize(), this.readingSessionKey);
                    FriendlyByteBuf fullPacket = PacketSplitter.receive(this, this.readingSessionKey, packet.getBuffer());

                    if (fullPacket != null)
                    {
                        try
                        {
                            final int packetSize = fullPacket.readableBytes();
                            this.readingSessionKey = -1;
                            Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(fullPacket);

	                        opt.ifPresent(baseData -> this.handleBulkData((CompoundData) baseData, packetSize));
                        }
                        catch (Exception e)
                        {
                            Litematica.LOGGER.error("ServuxLitematicaHandler#decodeClientData(): Entity Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
                        }
                    }
                }
                default ->
                        Litematica.LOGGER.warn("ServuxLitematicaHandler#decodeClientData(): received unhandled packetType {} of size {} bytes.", packet.getPacketType(), packet.getTotalSize());
            }
        }
    }

    private void handleBulkData(@Nullable CompoundData data, final int packetSize)
    {
        if (data == null || data.isEmpty())
        {
            return;
        }

        String task = data.getStringOrDefault("Task", "BulkEntityReply");
        Litematica.debugLog("handleBulkData: received task: {} [Bytes: {} / {}]", task, packetSize, data.sizeInBytes());

        // For future Granular Task Management
//        switch (task)
//        {
//            // File-Transmit support
//            case "Litematic-TransmitStart", "Litematic-TransmitCancel", "Litematic-TransmitData", "Litematic-TransmitEnd" ->
//            {
//                Pair<LitematicaSchematic, CompoundData> schemPair = LitematicaSchematic.receiveFileTransmit(nbt);
//
//                if (schemPair != null && schemPair.getLeft().getFile() != null)
//                {
//                    Litematica.LOGGER.info("handleBulkData(): Received litematic '{}' from the server", schemPair.getLeft().getFile().toAbsolutePath().toString());
//
//                    SchematicPlacement placement = SchematicPlacement.createFromData(schemPair.getLeft(), schemPair.getRight());
//
//                    if (placement != null)
//                    {
//                        DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, true);
//                    }
//                }
//            }
//            default -> EntityDataManager.getInstance().handleBulkEntityData(-1, data);
//        }

        EntityDataManager.getInstance().handleBulkEntityData(-1, data);
    }

    @Override
    public void reset(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID) && this.servuxRegistered)
        {
            this.servuxRegistered = false;
            this.failures = 0;
            this.readingSessionKey = -1;
        }
    }

    public void resetFailures(Identifier channel)
    {
        if (channel.equals(CHANNEL_ID) && this.failures > 0)
        {
            this.failures = 0;
        }
    }

    @Override
    public void receivePlayPayload(T payload, ClientPlayNetworking.Context ctx)
    {
        if (payload.type().id().equals(CHANNEL_ID))
        {
            ServuxLitematicaHandler.INSTANCE.decodeClientData(CHANNEL_ID, ((ServuxLitematicaPacket.Payload) payload).data());
        }
    }

    @Override
    public void encodeWithSplitter(FriendlyByteBuf buffer, ClientPacketListener handler)
    {
        // Send each PacketSplitter buffer slice
        ServuxLitematicaHandler.INSTANCE.sendPlayPayload(new ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.ResponseC2SData(buffer)));
    }

    @Override
    public <P extends IClientPayloadData> void encodeClientData(P data)
    {
        if (!EntityDataManager.getInstance().isEnabled() || !this.checkFailures())
        {
            return;
        }
        if (data instanceof ServuxLitematicaPacket packet)
        {
            // Send Response Data via Packet Splitter
            if (packet.getType().equals(ServuxLitematicaPacket.Type.PACKET_C2S_NBT_RESPONSE_START))
            {
                final int maxSize = PacketSplitter.DEFAULT_MAX_RECEIVE_SIZE_S2C - 4096;

                try
                {
//                    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
//                    buffer.writeNbt(packet.getCompound());
                    ByteBuf buffer = DataByteBufUtils.toByteBuf(packet.getCompound(), "");

                    if (buffer.readableBytes() > maxSize)
                    {
//                            Litematica.LOGGER.warn("[Servux Paste]: Slicing Oversided Schematic for Servux Paste ...");
//                            this.sliceForServux(schematicPlacement.getSchematic(), nbt, maxSize, printMessage);
                        InfoUtils.showGuiOrInGameMessage(Message.MessageType.ERROR, "litematica.message.error.placement_paste_too_large_for_servux");
                    }
                    else
                    {
                        PacketSplitter.send(this, new FriendlyByteBuf(buffer), Minecraft.getInstance().getConnection());
                        // PacketSplitter releases the ByteBuf at the end
                    }
                }
                catch (Exception e)
                {
                    Litematica.LOGGER.error("ServuxLitematicaHandler#encodeServerData(): Exception encoding packet for PacketSplitter; {}", e.getLocalizedMessage());
                }
            }
            else if (!ServuxLitematicaHandler.INSTANCE.sendPlayPayload(new ServuxLitematicaPacket.Payload(packet)))
            {
                this.tickFailures();
            }
        }
    }

    @Override
    public boolean checkFailures()
    {
        return !(this.failures > this.maxFailures());
    }

    @Override
    public void tickFailures()
    {
        if (this.failures > this.maxFailures())
        {
            Litematica.LOGGER.warn("ServuxLitematicaHandler$tickFailures(): encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", this.maxFailures());
            this.servuxRegistered = false;
            ServuxLitematicaHandler.INSTANCE.unregisterPlayReceiver();
            EntityDataManager.getInstance().onPacketFailure();
        }
        else
        {
            this.failures++;
        }
    }
}
