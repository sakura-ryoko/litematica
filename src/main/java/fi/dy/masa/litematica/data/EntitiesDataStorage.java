package fi.dy.masa.litematica.data;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.NBTUtils;
import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.mixin.IMixinDataQueryHandler;
import fi.dy.masa.litematica.network.ServuxLitematicaHandler;
import fi.dy.masa.litematica.network.ServuxLitematicaPacket;
import fi.dy.masa.litematica.util.EntityUtils;

public class EntitiesDataStorage implements IClientTickHandler
{
    private static final EntitiesDataStorage INSTANCE = new EntitiesDataStorage();

    public static EntitiesDataStorage getInstance()
    {
        return INSTANCE;
    }

    private final static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> HANDLER = ServuxLitematicaHandler.getInstance();
    private final static MinecraftClient mc = MinecraftClient.getInstance();
    private int uptimeTicks = 0;
    private boolean servuxServer = false;
    private boolean hasInValidServux = false;
    private String servuxVersion;

    private long serverTickTime = 0;
    // Requests to be executed
    private Set<BlockPos> pendingBlockEntitiesQueue = new LinkedHashSet<>();
    private Set<Integer> pendingEntitiesQueue = new LinkedHashSet<>();
    private Set<ChunkPos> pendingChunks = new LinkedHashSet<>();
    private Set<ChunkPos> completedChunks = new LinkedHashSet<>();
    // To save vanilla query packet transaction
    private Map<Integer, Either<BlockPos, Integer>> transactionToBlockPosOrEntityId = new HashMap<>();

    @Nullable
    public World getWorld()
    {
        return mc.world;
    }

    private EntitiesDataStorage()
    {
    }

    @Override
    public void onClientTick(MinecraftClient mc)
    {
        uptimeTicks++;
        if (System.currentTimeMillis() - serverTickTime > 50)
        {
            // In this block, we do something every server tick

            if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() == false)
            {
                serverTickTime = System.currentTimeMillis();
                return;
            }

            // 5 queries / server tick
            for (int i = 0; i < Configs.Generic.SERVER_NBT_REQUEST_RATE.getIntegerValue(); i++)
            {
                if (!pendingBlockEntitiesQueue.isEmpty())
                {
                    var iter = pendingBlockEntitiesQueue.iterator();
                    BlockPos pos = iter.next();
                    iter.remove();
                    if (this.hasServuxServer())
                    {
                        requestServuxBlockEntityData(pos);
                    }
                    else
                    {
                        requestQueryBlockEntity(pos);
                    }
                }
                if (!pendingEntitiesQueue.isEmpty())
                {
                    var iter = pendingEntitiesQueue.iterator();
                    int entityId = iter.next();
                    iter.remove();
                    if (this.hasServuxServer())
                    {
                        requestServuxEntityData(entityId);
                    }
                    else
                    {
                        requestQueryEntityData(entityId);
                    }
                }
            }
            serverTickTime = System.currentTimeMillis();
        }
    }

    public Identifier getNetworkChannel()
    {
        return ServuxLitematicaHandler.CHANNEL_ID;
    }

    private static ClientPlayNetworkHandler getVanillaHandler()
    {
        if (mc.player != null)
        {
            return mc.player.networkHandler;
        }

        return null;
    }

    public IPluginClientPlayHandler<ServuxLitematicaPacket.Payload> getNetworkHandler()
    {
        return HANDLER;
    }

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            Litematica.debugLog("EntitiesDataStorage#reset() - log-out");
            HANDLER.reset(this.getNetworkChannel());
            HANDLER.resetFailures(this.getNetworkChannel());
            this.servuxServer = false;
            this.hasInValidServux = false;
        }
        else
        {
            Litematica.debugLog("EntitiesDataStorage#reset() - dimension change or log-in");
        }
        // Clear data
    }

    public void setIsServuxServer()
    {
        this.servuxServer = true;
        this.hasInValidServux = false;
    }

    public boolean hasServuxServer()
    {
        return this.servuxServer;
    }

    public void setServuxVersion(String ver)
    {
        if (ver != null && ver.isEmpty() == false)
        {
            this.servuxVersion = ver;
            Litematica.debugLog("entityDataChannel: joining Servux version {}", ver);
        }
        else
        {
            this.servuxVersion = "unknown";
        }
    }

    public void onGameInit()
    {
        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxLitematicaPacket.Payload.ID, ServuxLitematicaPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

    public void onWorldPre()
    {
        if (DataManager.getInstance().hasIntegratedServer() == false)
        {
            HANDLER.registerPlayReceiver(ServuxLitematicaPacket.Payload.ID, HANDLER::receivePlayPayload);
        }
    }

    public void onWorldJoin()
    {
        // NO-OP
    }

    public void requestMetadata()
    {
        if (DataManager.getInstance().hasIntegratedServer() == false &&
            Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(ServuxLitematicaPacket.MetadataRequest(nbt));
        }
    }

    public boolean receiveServuxMetadata(NbtCompound data)
    {
        if (DataManager.getInstance().hasIntegratedServer() == false)
        {
            Litematica.debugLog("EntitiesDataStorage#receiveServuxMetadata(): received METADATA from Servux");

            if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
            {
                if (data.getInt("version") != ServuxLitematicaPacket.PROTOCOL_VERSION)
                {
                    Litematica.logger.warn("entityDataChannel: Mis-matched protocol version!");
                }

                this.setServuxVersion(data.getString("servux"));
                this.setIsServuxServer();

                return true;
            }
        }

        return false;
    }

    public void onPacketFailure()
    {
        this.servuxServer = false;
        this.hasInValidServux = true;
    }

    public void requestBlockEntity(World world, BlockPos pos)
    {
        if (world.getBlockState(pos).getBlock() instanceof BlockEntityProvider)
        {
            pendingBlockEntitiesQueue.add(pos);
        }
    }

    public void requestEntity(int entityId)
    {
        pendingEntitiesQueue.add(entityId);
    }

    private void requestQueryBlockEntity(BlockPos pos)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() == false)
        {
            return;
        }

        ClientPlayNetworkHandler handler = this.getVanillaHandler();

        if (handler != null)
        {
            handler.getDataQueryHandler().queryBlockNbt(pos, nbtCompound ->
            {
                handleBlockEntityData(pos, nbtCompound, null);
            });
            transactionToBlockPosOrEntityId.put(((IMixinDataQueryHandler) handler.getDataQueryHandler()).currentTransactionId(), Either.left(pos));
        }
    }

    private void requestQueryEntityData(int entityId)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() == false)
        {
            return;
        }

        ClientPlayNetworkHandler handler = this.getVanillaHandler();

        if (handler != null)
        {
            handler.getDataQueryHandler().queryEntityNbt(entityId, nbtCompound ->
            {
                handleEntityData(entityId, nbtCompound);
            });
            transactionToBlockPosOrEntityId.put(((IMixinDataQueryHandler) handler.getDataQueryHandler()).currentTransactionId(), Either.right(entityId));
        }
    }

    private void requestServuxBlockEntityData(BlockPos pos)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() == false)
        {
            return;
        }

        HANDLER.encodeClientData(ServuxLitematicaPacket.BlockEntityRequest(pos));
    }

    private void requestServuxEntityData(int entityId)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() == false)
        {
            return;
        }

        HANDLER.encodeClientData(ServuxLitematicaPacket.EntityRequest(entityId));
    }

    // The minY, maxY should be calculated based on the Selection Box...  But for now, we can just grab the entire chunk.
    public void requestServuxBulkEntityData(ChunkPos chunkPos, int minY, int maxY)
    {
        if (this.hasServuxServer() == false)
        {
            return;
        }

        NbtCompound req = new NbtCompound();

        if (this.completedChunks.contains(chunkPos))
        {
            this.completedChunks.remove(chunkPos);
        }

        this.pendingChunks.add(chunkPos);

        if (minY < -60)
        {
            minY = -60;
        }
        if (maxY > 319)
        {
            maxY = 319;
        }

        req.putInt("minY", minY);
        req.putInt("maxY", maxY);

        Litematica.debugLog("EntitiesDataStorage#requestServuxBulkEntityData(): for chunkPos [{}] to Servux (minY [{}], maxY [{}])", chunkPos.toString(), minY, maxY);
        HANDLER.encodeClientData(ServuxLitematicaPacket.BulkNbtRequest(chunkPos, req));
    }

    @Nullable
    public BlockEntity handleBlockEntityData(BlockPos pos, NbtCompound nbt, @Nullable Identifier type)
    {
        pendingBlockEntitiesQueue.remove(pos);
        if (nbt == null || this.getWorld() == null) return null;

        BlockEntity blockEntity = this.getWorld().getBlockEntity(pos);
        if (blockEntity != null && (type == null || type.equals(BlockEntityType.getId(blockEntity.getType()))))
        {
            blockEntity.read(nbt, this.getWorld().getRegistryManager());
            return blockEntity;
        }

        BlockEntityType<?> beType = Registries.BLOCK_ENTITY_TYPE.get(type);
        if (beType != null && beType.supports(this.getWorld().getBlockState(pos)))
        {
            BlockEntity blockEntity2 = beType.instantiate(pos, this.getWorld().getBlockState(pos));
            if (blockEntity2 != null)
            {
                blockEntity2.read(nbt, this.getWorld().getRegistryManager());
                this.getWorld().addBlockEntity(blockEntity2);
                return blockEntity2;
            }
        }

        return null;
    }

    @Nullable
    public Entity handleEntityData(int entityId, NbtCompound nbt)
    {
        pendingEntitiesQueue.remove(entityId);
        if (nbt == null || this.getWorld() == null) return null;
        Entity entity = this.getWorld().getEntityById(entityId);
        if (entity != null)
        {
            EntityUtils.loadNbtIntoEntity(entity, nbt);
        }
        return entity;
    }

    public void handleBulkEntityData(int transactionId, @Nullable NbtCompound nbt)
    {
        if (nbt == null)
        {
            return;
        }
        NbtList tileList = nbt.contains("TileEntities") ? nbt.getList("TileEntities", Constants.NBT.TAG_COMPOUND) : new NbtList();
        NbtList entityList = nbt.contains("Entities") ? nbt.getList("Entities", Constants.NBT.TAG_COMPOUND) : new NbtList();
        ChunkPos chunkPos = new ChunkPos(nbt.getInt("chunkX"), nbt.getInt("chunkZ"));

        for (int i = 0; i < tileList.size(); ++i)
        {
            NbtCompound te = tileList.getCompound(i);
            BlockPos pos = NBTUtils.readBlockPos(te);
            Identifier type = Identifier.of(te.getString("id"));

            handleBlockEntityData(pos, te, type);
        }

        for (int i = 0; i < entityList.size(); ++i)
        {
            NbtCompound ent = entityList.getCompound(i);
            Vec3d pos = NBTUtils.readEntityPositionFromTag(ent);
            int entityId = ent.getInt("entityId");

            handleEntityData(entityId, ent);
        }

        if (this.pendingChunks.contains(chunkPos))
        {
            this.pendingChunks.remove(chunkPos);
        }

        this.completedChunks.add(chunkPos);

        Litematica.debugLog("EntitiesDataStorage#handleBulkEntityData(): [ChunkPos {}] received TE: [{}], and E: [{}] entiries from Servux", chunkPos.toString(), tileList.size(), entityList.size());
    }

    public void handleVanillaQueryNbt(int transactionId, NbtCompound nbt)
    {
        Either<BlockPos, Integer> either = transactionToBlockPosOrEntityId.remove(transactionId);
        if (either != null)
        {
            either.ifLeft(pos -> handleBlockEntityData(pos, nbt, null))
                    .ifRight(entityId -> handleEntityData(entityId, nbt));
        }
    }

    public boolean hasPendingChunk(ChunkPos pos)
    {
        if (this.hasServuxServer())
        {
            return this.pendingChunks.contains(pos);
        }
        else
        {
            return false;
        }
    }

    public boolean hasCompletedChunk(ChunkPos pos)
    {
        if (this.hasServuxServer())
        {
            return this.completedChunks.contains(pos);
        }
        else
        {
            return true;
        }
    }

    public void markCompletedChunkDirty(ChunkPos pos)
    {
        if (this.hasServuxServer())
        {
            this.completedChunks.remove(pos);
        }
    }

    // TODO --> Only in case we need to save config settings in the future
    public JsonObject toJson()
    {
        return new JsonObject();
    }

    public void fromJson(JsonObject obj)
    {
        // NO-OP
    }
}
