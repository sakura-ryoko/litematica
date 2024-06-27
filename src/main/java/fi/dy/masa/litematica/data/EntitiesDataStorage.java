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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.mixin.IMixinDataQueryHandler;
import fi.dy.masa.litematica.network.ServuxEntitiesHandler;
import fi.dy.masa.litematica.network.ServuxEntitiesPacket;
import fi.dy.masa.litematica.util.EntityUtils;

public class EntitiesDataStorage implements IClientTickHandler
{
    private static final EntitiesDataStorage INSTANCE = new EntitiesDataStorage();

    public static EntitiesDataStorage getInstance()
    {
        return INSTANCE;
    }

    private final static ServuxEntitiesHandler<ServuxEntitiesPacket.Payload> HANDLER = ServuxEntitiesHandler.getInstance();
    private final static MinecraftClient mc = MinecraftClient.getInstance();
    private int uptimeTicks = 0;
    private boolean servuxServer = false;
    private boolean hasInValidServux = false;
    private String servuxVersion;

    private long serverTickTime = 0;
    // Requests to be executed
    private Set<BlockPos> pendingBlockEntitiesQueue = new LinkedHashSet<>();
    private Set<Integer> pendingEntitiesQueue = new LinkedHashSet<>();
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
        return ServuxEntitiesHandler.CHANNEL_ID;
    }

    private static ClientPlayNetworkHandler getVanillaHandler()
    {
        if (mc.player != null)
        {
            return mc.player.networkHandler;
        }

        return null;
    }

    public IPluginClientPlayHandler<ServuxEntitiesPacket.Payload> getNetworkHandler()
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
        HANDLER.registerPlayPayload(ServuxEntitiesPacket.Payload.ID, ServuxEntitiesPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

    public void onWorldPre()
    {
        if (DataManager.getInstance().hasIntegratedServer() == false)
        {
            HANDLER.registerPlayReceiver(ServuxEntitiesPacket.Payload.ID, HANDLER::receivePlayPayload);
        }
    }

    public void onWorldJoin()
    {
        // NO-OP
    }

    public void requestMetadata()
    {
        if (DataManager.getInstance().hasIntegratedServer() == false)
        {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(ServuxEntitiesPacket.MetadataRequest(nbt));
        }
    }

    public boolean receiveServuxMetadata(NbtCompound data)
    {
        if (DataManager.getInstance().hasIntegratedServer() == false)
        {
            Litematica.debugLog("EntitiesDataStorage#receiveServuxMetadata(): received METADATA from Servux");

            if (data.getInt("version") != ServuxEntitiesPacket.PROTOCOL_VERSION)
            {
                Litematica.logger.warn("entityDataChannel: Mis-matched protocol version!");
            }
            this.setServuxVersion(data.getString("servux"));
            this.setIsServuxServer();

            return true;
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
        ClientPlayNetworkHandler handler = this.getVanillaHandler();

        if (handler != null)
        {
            handler.getDataQueryHandler().queryBlockNbt(pos, nbtCompound ->
            {
                handleBlockEntityData(pos, nbtCompound);
            });
            transactionToBlockPosOrEntityId.put(((IMixinDataQueryHandler) handler.getDataQueryHandler()).currentTransactionId(), Either.left(pos));
        }
    }

    private void requestQueryEntityData(int entityId)
    {
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
        HANDLER.encodeClientData(ServuxEntitiesPacket.BlockEntityRequest(pos));
    }

    private void requestServuxEntityData(int entityId)
    {
        HANDLER.encodeClientData(ServuxEntitiesPacket.EntityRequest(entityId));
    }

    // BlockEntity.createNbtWithIdentifyingData
    @Nullable
    public BlockEntity handleBlockEntityData(BlockPos pos, NbtCompound nbt)
    {
        pendingBlockEntitiesQueue.remove(pos);
        if (nbt == null || this.getWorld() == null) return null;

        BlockEntity blockEntity = this.getWorld().getBlockEntity(pos);
        if (blockEntity != null)
        {
            blockEntity.read(nbt, this.getWorld().getRegistryManager());
            return blockEntity;
        }
        else
        {
            BlockEntity blockEntity2 = BlockEntity.createFromNbt(pos, this.getWorld().getBlockState(pos), nbt, mc.world.getRegistryManager());
            if (blockEntity2 != null)
            {
                this.getWorld().addBlockEntity(blockEntity2);
                return blockEntity2;
            }
        }

        return null;
    }

    // Entity.saveSelfNbt
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

    public void handleVanillaQueryNbt(int transactionId, NbtCompound nbt)
    {
        Either<BlockPos, Integer> either = transactionToBlockPosOrEntityId.remove(transactionId);
        if (either != null)
        {
            either.ifLeft(pos -> handleBlockEntityData(pos, nbt))
                    .ifRight(entityId -> handleEntityData(entityId, nbt));
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
