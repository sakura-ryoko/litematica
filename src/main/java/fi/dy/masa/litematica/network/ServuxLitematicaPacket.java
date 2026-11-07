package fi.dy.masa.litematica.network;

import javax.annotation.Nullable;

import io.netty.buffer.Unpooled;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * 1.20.1 port of the Servux Litematica packet (client side).
 * Wire format: varint type + NBT payload, sent through the packet splitter.
 */
public class ServuxLitematicaPacket
{
    public static final Identifier CHANNEL = new Identifier("servux", "litematics");
    public static final int PROTOCOL_VERSION = 1;

    private final Type type;
    private final NbtCompound nbt;

    private ServuxLitematicaPacket(Type type, @Nullable NbtCompound nbt)
    {
        this.type = type;
        this.nbt = nbt != null ? nbt.copy() : new NbtCompound();
    }

    public static ServuxLitematicaPacket MetadataRequest()
    {
        return new ServuxLitematicaPacket(Type.PACKET_C2S_METADATA_REQUEST, null);
    }

    public static ServuxLitematicaPacket MetadataResponse(NbtCompound nbt)
    {
        return new ServuxLitematicaPacket(Type.PACKET_S2C_METADATA, nbt);
    }

    public static ServuxLitematicaPacket PasteRequest(NbtCompound nbt)
    {
        return new ServuxLitematicaPacket(Type.PACKET_C2S_PASTE_REQUEST, nbt);
    }

    public static ServuxLitematicaPacket PasteResult(boolean success, String message)
    {
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("success", success);
        nbt.putString("message", message);
        return new ServuxLitematicaPacket(Type.PACKET_S2C_PASTE_RESULT, nbt);
    }

    public Type getType()
    {
        return this.type;
    }

    public NbtCompound getCompound()
    {
        return this.nbt;
    }

    public void toPacket(PacketByteBuf output)
    {
        output.writeVarInt(this.type.ordinal());
        output.writeNbt(this.nbt);
    }

    @Nullable
    public static ServuxLitematicaPacket fromPacket(PacketByteBuf input)
    {
        int id = input.readVarInt();
        Type[] values = Type.values();

        if (id < 0 || id >= values.length)
        {
            return null;
        }

        NbtCompound nbt = input.readNbt();
        return new ServuxLitematicaPacket(values[id], nbt);
    }

    public static PacketByteBuf toBuffer(ServuxLitematicaPacket packet)
    {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        packet.toPacket(buf);
        return buf;
    }

    public enum Type
    {
        PACKET_C2S_METADATA_REQUEST,
        PACKET_S2C_METADATA,
        PACKET_C2S_PASTE_REQUEST,
        PACKET_S2C_PASTE_RESULT
    }
}
