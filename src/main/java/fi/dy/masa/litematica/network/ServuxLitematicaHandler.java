package fi.dy.masa.litematica.network;

import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.NonNull;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.network.PacketSplitter;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntityDataManager;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

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
    private static final int MAX_FAILURES = 4;
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
        ServuxLitematicaPacket packet = (ServuxLitematicaPacket) data;

        if (!channel.equals(CHANNEL_ID))
        {
            return;
        }
        switch (packet.getType())
        {
            case PACKET_S2C_METADATA ->
            {
                if (EntityDataManager.getInstance().receiveServuxMetadata(packet.getCompound()))
                {
                    this.servuxRegistered = true;
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

                //Litematica.debugLog("ServuxLitematicaHandler#decodeClientData(): received Entity Data Packet Slice of size {} (in bytes) // reading session key [{}]", packet.getTotalSize(), this.readingSessionKey);
                FriendlyByteBuf fullPacket = PacketSplitter.receive(this, this.readingSessionKey, packet.getBuffer());

                if (fullPacket != null)
                {
                    try
                    {
                        this.readingSessionKey = -1;
                        this.handleBulkData(fullPacket.readVarInt(), (CompoundTag) fullPacket.readNbt(NbtAccounter.unlimitedHeap()));
                    }
                    catch (Exception e)
                    {
                        Litematica.LOGGER.error("ServuxLitematicaHandler#decodeClientData(): Entity Data: error reading fullBuffer [{}]", e.getLocalizedMessage());
                    }
                }
            }
            default -> Litematica.LOGGER.warn("ServuxLitematicaHandler#decodeClientData(): received unhandled packetType {} of size {} bytes.", packet.getPacketType(), packet.getTotalSize());
        }
    }

    private void handleBulkData(final int type, @Nullable CompoundTag nbt)
    {
        if (nbt == null || nbt.isEmpty())
        {
            return;
        }

        String task = nbt.getStringOr("Task", "BulkEntityReply");
        Litematica.debugLog("handleBulkData: received task: {}", task);

        // For future Granular Task Management
//        switch (task)
//        {
//            // File-Transmit support
//            case "Litematic-TransmitStart", "Litematic-TransmitCancel", "Litematic-TransmitData", "Litematic-TransmitEnd" ->
//            {
//                Pair<LitematicaSchematic, CompoundTag> schemPair = LitematicaSchematic.receiveFileTransmit(nbt);
//
//                if (schemPair != null && schemPair.getLeft().getFile() != null)
//                {
//                    Litematica.LOGGER.info("handleBulkData(): Received litematic '{}' from the server", schemPair.getLeft().getFile().toAbsolutePath().toString());
//
//                    SchematicPlacement placement = SchematicPlacement.createFromNbt(schemPair.getLeft(), schemPair.getRight());
//
//                    if (placement != null)
//                    {
//                        DataManager.getSchematicPlacementManager().addSchematicPlacement(placement, true);
//                    }
//                }
//            }
//            default -> EntityDataManager.getInstance().handleBulkEntityData(type, DataConverterNbt.fromVanillaCompound(nbt));
//        }

        EntityDataManager.getInstance().handleBulkEntityData(type, DataConverterNbt.fromVanillaCompound(nbt));
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
        ServuxLitematicaPacket packet = (ServuxLitematicaPacket) data;

        if (packet.getType().equals(ServuxLitematicaPacket.Type.PACKET_C2S_NBT_RESPONSE_START))
        {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeVarInt(packet.getTransactionId());
            buffer.writeNbt(packet.getCompound());
            PacketSplitter.send(this, buffer, Minecraft.getInstance().getConnection());
        }
        else if (!ServuxLitematicaHandler.INSTANCE.sendPlayPayload(new ServuxLitematicaPacket.Payload(packet)))
        {
            if (this.failures > MAX_FAILURES)
            {
                Litematica.LOGGER.warn("encodeClientData(): encountered [{}] sendPayload failures, cancelling any Servux join attempt(s)", MAX_FAILURES);
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
}
