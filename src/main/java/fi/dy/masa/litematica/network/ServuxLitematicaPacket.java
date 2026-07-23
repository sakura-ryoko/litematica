package fi.dy.masa.litematica.network;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.netty.buffer.Unpooled;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.ChunkPos;

import fi.dy.masa.malilib.network.IClientPayloadData;
import fi.dy.masa.malilib.util.data.tag.BaseData;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.util.DataByteBufUtils;
import fi.dy.masa.litematica.Litematica;

public class ServuxLitematicaPacket implements IClientPayloadData
{
	private Type packetType;
	private int entityId;
	private BlockPos pos;
	private CompoundData nbt;
	private ChunkPos chunkPos;
	private FriendlyByteBuf buffer;
	public static final int PROTOCOL_VERSION = 2;

	private ServuxLitematicaPacket(Type type)
	{
		this.packetType = type;
		this.entityId = -1;
		this.pos = BlockPos.ZERO;
		this.chunkPos = ChunkPos.ZERO;
		this.nbt = new CompoundData();
		this.clearPacket();
	}

	public static ServuxLitematicaPacket MetadataRequest(@Nullable CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_METADATA_REQUEST);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxLitematicaPacket MetadataResponse(@Nullable CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_METADATA);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxLitematicaPacket UnregisterReply(@Nullable CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_UNREGISTER_REPLY);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	// Entity simple response
	public static ServuxLitematicaPacket SimpleEntityResponse(int entityId, @Nullable CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		packet.entityId = entityId;
		return packet;
	}

	public static ServuxLitematicaPacket SimpleBlockResponse(BlockPos pos, @Nullable CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		packet.pos = pos.immutable();
		return packet;
	}

	public static ServuxLitematicaPacket BlockEntityRequest(BlockPos pos)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_BLOCK_ENTITY_REQUEST);
		packet.pos = pos.immutable();
		return packet;
	}

	public static ServuxLitematicaPacket EntityRequest(int entityId)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_ENTITY_REQUEST);
		packet.entityId = entityId;
		return packet;
	}

	public static ServuxLitematicaPacket TaskRequest(@Nullable CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_TASK_REQUEST);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxLitematicaPacket TaskResponse(@Nullable CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_TASK_RESPONSE);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxLitematicaPacket TaskStatusSync(@Nullable CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_TASK_STATUS_SYNC);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxLitematicaPacket TaskCancel(@Nullable CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_TASK_CANCEL);
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	public static ServuxLitematicaPacket BulkNbtRequest(ChunkPos chunkPos, @Nullable CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_BULK_ENTITY_NBT_REQUEST);
		packet.chunkPos = chunkPos;
		if (nbt != null)
		{
			packet.nbt.combine(nbt);
		}
		return packet;
	}

	// Nbt Packet, using Packet Splitter
	public static ServuxLitematicaPacket ResponseS2CStart(@Nonnull CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_NBT_RESPONSE_START);
		packet.nbt.combine(nbt);
		return packet;
	}

	public static ServuxLitematicaPacket ResponseS2CData(@Nonnull FriendlyByteBuf buffer)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_S2C_NBT_RESPONSE_DATA);
		packet.buffer = new FriendlyByteBuf(buffer.copy());
		packet.nbt = new CompoundData();
		return packet;
	}

	public static ServuxLitematicaPacket ResponseC2SStart(@Nonnull CompoundData nbt)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_NBT_RESPONSE_START);
		packet.nbt.combine(nbt);
		return packet;
	}

	public static ServuxLitematicaPacket ResponseC2SData(@Nonnull FriendlyByteBuf buffer)
	{
		var packet = new ServuxLitematicaPacket(Type.PACKET_C2S_NBT_RESPONSE_DATA);
		packet.buffer = new FriendlyByteBuf(buffer.copy());
		packet.nbt = new CompoundData();
		return packet;
	}

	private void clearPacket()
	{
		if (this.buffer != null)
		{
			this.buffer.clear();
			this.buffer = new FriendlyByteBuf(Unpooled.buffer());
		}
	}

	@Override
	public int getVersion()
	{
		return PROTOCOL_VERSION;
	}

	@Override
	public int getPacketType()
	{
		return this.packetType.get();
	}

	@Override
	public int getTotalSize()
	{
		int total = 2;

		if (this.nbt != null && !this.nbt.isEmpty())
		{
			total += this.nbt.sizeInBytes();
		}
		if (this.buffer != null)
		{
			total += this.buffer.readableBytes();
		}

		return total;
	}

	public Type getType()
	{
		return this.packetType;
	}

	public int getEntityId() {return this.entityId;}

	public BlockPos getPos() {return this.pos;}

	public CompoundData getCompound()
	{
		return this.nbt;
	}

	public ChunkPos getChunkPos() {return this.chunkPos;}

	public FriendlyByteBuf getBuffer()
	{
		return this.buffer;
	}

	public boolean hasBuffer() {return this.buffer != null && this.buffer.isReadable();}

	public boolean hasNbt() {return this.nbt != null && !this.nbt.isEmpty();}

	@Override
	public boolean isEmpty()
	{
		return !this.hasBuffer() && !this.hasNbt();
	}

	@Override
	public void toPacket(FriendlyByteBuf output)
	{
		output.writeVarInt(this.packetType.get());

		switch (this.packetType)
		{
			case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
			{
				// Write BE Request
				try
				{
					output.writeBlockPos(this.pos);
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing Block Entity Request to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_ENTITY_REQUEST ->
			{
				// Write Entity Request
				try
				{
					output.writeVarInt(this.entityId);
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing Entity Request to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
			{
				try
				{
					output.writeBlockPos(this.pos);
//                    output.writeNbt(this.nbt);
					DataByteBufUtils.toByteBuf(output, this.nbt, "");
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing Block Entity Response to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE ->
			{
				try
				{
					output.writeVarInt(this.entityId);
//                    output.writeNbt(this.nbt);
					DataByteBufUtils.toByteBuf(output, this.nbt, "");
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing Entity Response to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_BULK_ENTITY_NBT_REQUEST ->
			{
				try
				{
					output.writeChunkPos(this.chunkPos);
//                    output.writeNbt(this.nbt);
					DataByteBufUtils.toByteBuf(output, this.nbt, "");
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing Bulk Entity Request to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_NBT_RESPONSE_DATA, PACKET_C2S_NBT_RESPONSE_DATA ->
			{
				// Write Packet Buffer (Slice)
				try
				{
					output.writeBytes(this.buffer.copy());
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing buffer data to packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_METADATA_REQUEST, PACKET_S2C_METADATA, PACKET_C2S_UNREGISTER_REPLY, PACKET_C2S_TASK_REQUEST, PACKET_S2C_TASK_RESPONSE, PACKET_S2C_TASK_STATUS_SYNC, PACKET_C2S_TASK_CANCEL ->
			{
				// Write NBT
				try
				{
//                    output.writeNbt(this.nbt);
					DataByteBufUtils.toByteBuf(output, this.nbt, "");
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#toPacket: error writing NBT to packet: [{}]", e.getLocalizedMessage());
				}
			}
			default -> Litematica.LOGGER.error("ServuxEntitiesPacket#toPacket: Unknown packet type!");
		}
	}

	@Nullable
	public static ServuxLitematicaPacket fromPacket(FriendlyByteBuf input)
	{
		int i = input.readVarInt();
		Type type = getType(i);

		if (type == null)
		{
			// Invalid Type
			Litematica.LOGGER.warn("ServuxEntitiesPacket#fromPacket: invalid packet type received");
			return null;
		}
		switch (type)
		{
			case PACKET_C2S_BLOCK_ENTITY_REQUEST ->
			{
				// Read Packet Buffer
				try
				{
					return ServuxLitematicaPacket.BlockEntityRequest(input.readBlockPos());
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Block Entity Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_ENTITY_REQUEST ->
			{
				// Read Packet Buffer
				try
				{
					return ServuxLitematicaPacket.EntityRequest(input.readVarInt());
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Entity Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE ->
			{
				try
				{
					BlockPos pos = input.readBlockPos();
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxLitematicaPacket.SimpleBlockResponse(input.readBlockPos(), (CompoundTag) input.readNbt(NbtAccounter.unlimitedHeap()));
					if (opt.isPresent())
					{
						return ServuxLitematicaPacket.SimpleBlockResponse(pos, (CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Block Entity Response from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE ->
			{
				try
				{
					final int entityId = input.readVarInt();
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxLitematicaPacket.SimpleEntityResponse(input.readVarInt(), (CompoundTag) input.readNbt(NbtAccounter.unlimitedHeap()));
					if (opt.isPresent())
					{
						return ServuxLitematicaPacket.SimpleEntityResponse(entityId, (CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Entity Response from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_BULK_ENTITY_NBT_REQUEST ->
			{
				try
				{
					ChunkPos pos = input.readChunkPos();
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxLitematicaPacket.BulkNbtRequest(input.readChunkPos(), (CompoundTag) input.readNbt(NbtAccounter.unlimitedHeap()));
					if (opt.isPresent())
					{
						return ServuxLitematicaPacket.BulkNbtRequest(pos, (CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Bulk Entity Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_NBT_RESPONSE_DATA ->
			{
				// Read Packet Buffer Slice
				try
				{
					return ServuxLitematicaPacket.ResponseS2CData(new FriendlyByteBuf(input.readBytes(input.readableBytes())));
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading S2C Bulk Response Buffer from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_NBT_RESPONSE_DATA ->
			{
				// Read Packet Buffer Slice
				try
				{
					return ServuxLitematicaPacket.ResponseC2SData(new FriendlyByteBuf(input.readBytes(input.readableBytes())));
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading C2S Bulk Response Buffer from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_METADATA_REQUEST ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxLitematicaPacket.MetadataRequest(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxLitematicaPacket.MetadataRequest((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Metadata Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_METADATA ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxLitematicaPacket.MetadataResponse(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxLitematicaPacket.MetadataResponse((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxEntitiesPacket#fromPacket: error reading Metadata Response from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_UNREGISTER_REPLY ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);
//                    return ServuxLitematicaPacket.UnregisterReply(input.readNbt());
					if (opt.isPresent())
					{
						return ServuxLitematicaPacket.UnregisterReply((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Unregister Reply from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_TASK_REQUEST ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);

					if (opt.isPresent())
					{
						return ServuxLitematicaPacket.TaskRequest((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Task Request from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_TASK_RESPONSE ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);

					if (opt.isPresent())
					{
						return ServuxLitematicaPacket.TaskResponse((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Task Response from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_S2C_TASK_STATUS_SYNC ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);

					if (opt.isPresent())
					{
						return ServuxLitematicaPacket.TaskStatusSync((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Task Status Sync from packet: [{}]", e.getLocalizedMessage());
				}
			}
			case PACKET_C2S_TASK_CANCEL ->
			{
				// Read Nbt
				try
				{
					Optional<BaseData> opt = DataByteBufUtils.fromByteBuf(input);

					if (opt.isPresent())
					{
						return ServuxLitematicaPacket.TaskCancel((CompoundData) opt.get());
					}
				}
				catch (Exception e)
				{
					Litematica.LOGGER.error("ServuxLitematicaPacket#fromPacket: error reading Task Cancel from packet: [{}]", e.getLocalizedMessage());
				}
			}
			default -> Litematica.LOGGER.error("ServuxEntitiesPacket#fromPacket: Unknown packet type!");
		}

		return null;
	}

	@Override
	public void clear()
	{
		if (this.nbt != null && !this.nbt.isEmpty())
		{
			this.nbt = new CompoundData();
		}
		this.clearPacket();
		this.entityId = -1;
		this.pos = BlockPos.ZERO;
		this.packetType = null;
	}

	@Nullable
	public static Type getType(int input)
	{
		for (Type type : Type.values())
		{
			if (type.get() == input)
			{
				return type;
			}
		}

		return null;
	}

	public enum Type
	{
		PACKET_S2C_METADATA(1),
		PACKET_C2S_METADATA_REQUEST(2),
		PACKET_C2S_BLOCK_ENTITY_REQUEST(3),
		PACKET_C2S_ENTITY_REQUEST(4),
		PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE(5),
		PACKET_S2C_ENTITY_NBT_RESPONSE_SIMPLE(6),
		PACKET_C2S_BULK_ENTITY_NBT_REQUEST(7),
		PACKET_C2S_UNREGISTER_REPLY(8),
		// For Packet Splitter (Oversize Packets, S2C)
		PACKET_S2C_NBT_RESPONSE_START(10),
		PACKET_S2C_NBT_RESPONSE_DATA(11),
		// For Packet Splitter (Oversize Packets, C2S)
		PACKET_C2S_NBT_RESPONSE_START(12),
		PACKET_C2S_NBT_RESPONSE_DATA(13),
		// Task Scheduler Items
		PACKET_C2S_TASK_REQUEST(14),
		PACKET_S2C_TASK_RESPONSE(15),
		PACKET_S2C_TASK_STATUS_SYNC(16),
		PACKET_C2S_TASK_CANCEL(17),
		;

		private final int type;

		Type(int type)
		{
			this.type = type;
		}

		int get() {return this.type;}
	}

	public record Payload(ServuxLitematicaPacket data) implements CustomPacketPayload
	{
		public static final CustomPacketPayload.Type<Payload> ID = new CustomPacketPayload.Type<>(ServuxLitematicaHandler.CHANNEL_ID);
		public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = CustomPacketPayload.codec(Payload::write, Payload::new);

		public Payload(FriendlyByteBuf input)
		{
			this(fromPacket(input));
		}

		private void write(FriendlyByteBuf output)
		{
			data.toPacket(output);
		}

		@Override
		public @Nonnull CustomPacketPayload.Type<? extends CustomPacketPayload> type()
		{
			return ID;
		}
	}
}
