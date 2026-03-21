package fi.dy.masa.litematica.data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.interfaces.IDataSyncer;
import fi.dy.masa.malilib.mixin.entity.IMixinAbstractHorseEntity;
import fi.dy.masa.malilib.mixin.entity.IMixinPiglinEntity;
import fi.dy.masa.malilib.mixin.network.IMixinDataQueryHandler;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.data.tag.util.DataTypeUtils;
import fi.dy.masa.malilib.util.nbt.NbtKeys;
import fi.dy.masa.malilib.util.nbt.NbtView;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.network.ServuxLitematicaHandler;
import fi.dy.masa.litematica.network.ServuxLitematicaPacket;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.WorldSchematic;

public class EntitiesDataStorage implements IClientTickHandler, IDataSyncer
{
    private static final EntitiesDataStorage INSTANCE = new EntitiesDataStorage();

    public static EntitiesDataStorage getInstance()
    {
        return INSTANCE;
    }

    private final static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> HANDLER = ServuxLitematicaHandler.getInstance();
    private final Minecraft mc;
    //private int uptimeTicks = 0;
    private boolean servuxServer = false;
    private boolean hasInValidServux = false;
    private String servuxVersion;
    private final long chunkTimeoutMs = 5000;
    // Wait 5 seconds for loaded Client Chunks to receive Entity Data
    private boolean checkOpStatus = true;
    private boolean hasOpStatus = false;
    private long lastOpCheck = 0L;

    // Data Cache
    private final ConcurrentHashMap<BlockPos, Pair<Long, Pair<BlockEntity, CompoundData>>> blockEntityCache = new ConcurrentHashMap<>(16, 0.9f, 1);
    private final ConcurrentHashMap<Integer,  Pair<Long, Pair<Entity,      CompoundData>>> entityCache      = new ConcurrentHashMap<>(16, 0.9f, 1);
    private final long cacheTimeout = 4;
    private final long longCacheTimeout = 30;
    private boolean shouldUseLongTimeout = false;
    // Needs a long cache timeout for saving schematics
    private long serverTickTime = 0;
    // Requests to be executed
    private final Set<BlockPos> pendingBlockEntitiesQueue = new LinkedHashSet<>();
    private final Set<Integer> pendingEntitiesQueue = new LinkedHashSet<>();
    private final Set<ChunkPos> pendingChunks = new LinkedHashSet<>();
    private final Set<ChunkPos> completedChunks = new LinkedHashSet<>();
    private final Map<ChunkPos, Long> pendingChunkTimeout = new HashMap<>();
    // To save vanilla query packet transaction
    private final Map<Integer, Either<BlockPos, Integer>> transactionToBlockPosOrEntityId = new HashMap<>();
    private ClientLevel clientWorld;

    // Backup Chunk Saving task
    private boolean sentBackupPackets = false;
    private boolean receivedBackupPackets = false;
    private final HashMap<ChunkPos, Set<BlockPos>> pendingBackupChunk_BlockEntities = new HashMap<>();
    private final HashMap<ChunkPos, Set<Integer>>  pendingBackupChunk_Entities      = new HashMap<>();

    @Override
    @Nullable
    public Level getWorld()
    {
        return fi.dy.masa.malilib.util.WorldUtils.getBestWorld(mc);
    }

    @Override
    public ClientLevel getClientWorld()
    {
        if (this.clientWorld == null)
        {
            this.clientWorld = this.mc.level;
        }

        return this.clientWorld;
    }

    private EntitiesDataStorage()
    {
        this.mc = Minecraft.getInstance();
    }

    @Override
    public void onClientTick(Minecraft mc)
    {
        long now = System.currentTimeMillis();
        //this.uptimeTicks++;

        if (now - this.serverTickTime > 50)
        {
            // In this block, we do something every server tick
            if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() == false)
            {
                this.serverTickTime = now;

                if (DataManager.getInstance().hasIntegratedServer() == false && this.hasServuxServer())
                {
                    this.servuxServer = false;
                    HANDLER.unregisterPlayReceiver();
                }

                if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() == false)
                {
                    // Expire cached NBT and clear pending Queue if both are disabled
                    if (!this.pendingBlockEntitiesQueue.isEmpty())
                    {
                        this.pendingBlockEntitiesQueue.clear();
                    }

                    if (!this.pendingEntitiesQueue.isEmpty())
                    {
                        this.pendingEntitiesQueue.clear();
                    }

//                    this.tickCache(now);
                    return;
                }
            }
            else if (DataManager.getInstance().hasIntegratedServer() == false &&
                    this.hasServuxServer() == false &&
                    this.hasInValidServux == false &&
                    this.getWorld() != null)
            {
                // Make sure we're Play Registered, and request Metadata
                HANDLER.registerPlayReceiver(ServuxLitematicaPacket.Payload.ID, HANDLER::receivePlayPayload);
                this.requestMetadata();
            }

            // Expire cached NBT
            this.tickCache(now);

            // 5 queries / server tick
            for (int i = 0; i < Configs.Generic.SERVER_NBT_REQUEST_RATE.getIntegerValue(); i++)
            {
                if (!this.pendingBlockEntitiesQueue.isEmpty())
                {
                    var iter = this.pendingBlockEntitiesQueue.iterator();
                    BlockPos pos = iter.next();
                    iter.remove();

                    if (this.hasServuxServer())
                    {
                        requestServuxBlockEntityData(pos);
                    }
                    else if (this.shouldUseQuery())
                    {
                        // Only check once if we have OP
                        requestQueryBlockEntity(pos);
                    }
                }
                if (!this.pendingEntitiesQueue.isEmpty())
                {
                    var iter = this.pendingEntitiesQueue.iterator();
                    int entityId = iter.next();
                    iter.remove();
                    if (this.hasServuxServer())
                    {
                        requestServuxEntityData(entityId);
                    }
                    else if (this.shouldUseQuery())
                    {
                        // Only check once if we have OP
                        requestQueryEntityData(entityId);
                    }
                }
            }
            this.serverTickTime = System.currentTimeMillis();
        }
    }

    public Identifier getNetworkChannel()
    {
        return ServuxLitematicaHandler.CHANNEL_ID;
    }

    private ClientPacketListener getVanillaHandler()
    {
        if (this.mc.player != null)
        {
            return this.mc.player.connection;
        }

        return null;
    }

    public IPluginClientPlayHandler<ServuxLitematicaPacket.Payload> getNetworkHandler()
    {
        return HANDLER;
    }

    @Override
    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            Litematica.debugLog("EntitiesDataStorage#reset() - log-out");
            HANDLER.reset(this.getNetworkChannel());
            HANDLER.resetFailures(this.getNetworkChannel());
            this.servuxServer = false;
            this.hasInValidServux = false;
            this.sentBackupPackets = false;
            this.receivedBackupPackets = false;
            this.checkOpStatus = false;
            this.hasOpStatus = false;
            this.lastOpCheck = 0L;
        }
        else
        {
            Litematica.debugLog("EntitiesDataStorage#reset() - dimension change or log-in");
            long now = System.currentTimeMillis();
            this.serverTickTime = now - (this.getCacheTimeout() + 5000L);
            this.tickCache(now);
            this.serverTickTime = now;
            this.clientWorld = mc.level;
            this.checkOpStatus = true;
            this.lastOpCheck = now;
        }

        // Clear data
        this.blockEntityCache.clear();
        this.entityCache.clear();
        this.pendingBlockEntitiesQueue.clear();
        this.pendingEntitiesQueue.clear();

        // Litematic Save values
        this.completedChunks.clear();
        this.pendingChunks.clear();
        this.pendingChunkTimeout.clear();
        this.pendingBackupChunk_BlockEntities.clear();
        this.pendingBackupChunk_Entities.clear();
    }

    private boolean shouldUseQuery()
    {
        if (this.hasOpStatus)
        {
//            System.out.printf("shouldUseQuery: HAS OP\n");
            return true;
        }

        if (this.checkOpStatus)
        {
            // Check for 15 minutes after login, or changing dimensions
            if ((System.currentTimeMillis() - this.lastOpCheck) < 900000L)
            {
//                System.out.printf("shouldUseQuery: CHECK OP\n");
                return true;
            }

            this.checkOpStatus = false;
        }

//        System.out.printf("shouldUseQuery: NOT-OP\n");
        return false;
    }

    public void resetOpCheck()
    {
        this.hasOpStatus = false;
        this.checkOpStatus = true;
        this.lastOpCheck = System.currentTimeMillis();
    }

    private long getCacheTimeout()
    {
        // Increase cache timeout when in Backup Mode.
        int modifier = Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() ? 5 : 1;
        return (long) (Mth.clamp((Configs.Generic.ENTITY_DATA_SYNC_CACHE_TIMEOUT.getFloatValue() * modifier), 1.0f, 500.0f) * 1000L);
    }

    private long getCacheTimeoutLong()
    {
        // Increase cache timeout when in Backup Mode.
        int modifier = Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() ? 5 : 1;
        return (long) (Mth.clamp(((Configs.Generic.ENTITY_DATA_SYNC_CACHE_TIMEOUT.getFloatValue() * modifier) * this.longCacheTimeout), 120.0f, (300.0f * modifier)) * 1000L);
    }

    private void tickCache(long nowTime)
    {
        long blockTimeout = this.getCacheTimeout();
        long entityTimeout = this.getCacheTimeout();
        int count;
        boolean beEmpty = false;
        boolean entEmpty = false;

        // Use LongTimeouts when saving a Litematic Selection,
        // which is pretty much the standard value x 30 (min 120, max 300 seconds)
        if (this.shouldUseLongTimeout)
        {
            blockTimeout = this.getCacheTimeoutLong();
            entityTimeout = this.getCacheTimeoutLong();

            // Add extra time if using QueryNbt only
            if (this.hasServuxServer() == false && this.getIfReceivedBackupPackets())
            {
                blockTimeout += 3000L;
                entityTimeout += 3000L;
            }
        }

        synchronized (this.blockEntityCache)
        {
            count = 0;

            for (BlockPos pos : this.blockEntityCache.keySet())
            {
                Pair<Long, Pair<BlockEntity, CompoundData>> pair = this.blockEntityCache.get(pos);

                if (pair != null)
                {
                    if (nowTime - pair.getLeft() > blockTimeout || pair.getLeft() > nowTime)
                    {
//                        Litematica.debugLog("litematicEntityCache: be at pos [{}] has timed out by [{}] ms", pos.toShortString(), blockTimeout);
                        this.blockEntityCache.remove(pos);
                    }
                    else
                    {
                        count++;
                    }
                }
            }

            if (count == 0)
            {
                beEmpty = true;
            }
        }

        synchronized (this.entityCache)
        {
            count = 0;

            for (Integer entityId : this.entityCache.keySet())
            {
                Pair<Long, Pair<Entity, CompoundData>> pair = this.entityCache.get(entityId);

                if (pair != null)
                {
                    if (nowTime - pair.getLeft() > entityTimeout || pair.getLeft() > nowTime)
                    {
                        Litematica.debugLog("litematicEntityCache: entity Id [{}] has timed out by [{}] ms", entityId, entityTimeout);
                        this.entityCache.remove(entityId);
                    }
                    else
                    {
                        count++;
                    }
                }
            }

            if (count == 0)
            {
                entEmpty = true;
            }
        }

        // End Long timeout phase
        if (beEmpty && entEmpty && this.shouldUseLongTimeout)
        {
            this.shouldUseLongTimeout = false;
        }
    }

    @Override
    public @Nullable CompoundData getFromBlockEntityCacheData(BlockPos pos)
    {
        if (this.blockEntityCache.containsKey(pos))
        {
            return this.blockEntityCache.get(pos).getRight().getRight();
        }

        return null;
    }

	@Override
	public @Nullable CompoundTag getFromBlockEntityCacheNbt(BlockPos pos)
	{
        CompoundData data =this.getFromBlockEntityCacheData(pos);

        if (data != null)
        {
            return DataConverterNbt.toVanillaCompound(data);
        }

        return null;
	}

	@Override
    public @Nullable BlockEntity getFromBlockEntityCache(BlockPos pos)
    {
        if (this.blockEntityCache.containsKey(pos))
        {
            return this.blockEntityCache.get(pos).getRight().getLeft();
        }

        return null;
    }

	@Override
    public @Nullable CompoundData getFromEntityCacheData(int entityId)
    {
        if (this.entityCache.containsKey(entityId))
        {
            return this.entityCache.get(entityId).getRight().getRight();
        }

        return null;
    }

	@Override
	public @Nullable CompoundTag getFromEntityCacheNbt(int entityId)
	{
        CompoundData data = this.getFromEntityCacheData(entityId);

        if (data != null)
        {
            return DataConverterNbt.toVanillaCompound(data);
        }

        return null;
	}

	@Override
    public @Nullable Entity getFromEntityCache(int entityId)
    {
        if (this.entityCache.containsKey(entityId))
        {
            return this.entityCache.get(entityId).getRight().getLeft();
        }

        return null;
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

    public boolean hasBackupStatus()
    {
        return Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() && this.hasOpStatus;
    }

    public void setServuxVersion(String ver)
    {
        if (ver != null && ver.isEmpty() == false)
        {
            this.servuxVersion = ver;
            Litematica.debugLog("LitematicDataChannel: joining Servux version {}", ver);
        }
        else
        {
            this.servuxVersion = "unknown";
        }
    }

    public String getServuxVersion()
    {
        return this.servuxVersion;
    }

    public int getPendingBlockEntitiesCount()
    {
        return this.pendingBlockEntitiesQueue.size();
    }

    public int getPendingEntitiesCount()
    {
        return this.pendingEntitiesQueue.size();
    }

    public int getBlockEntityCacheCount()
    {
        return this.blockEntityCache.size();
    }

    public int getEntityCacheCount()
    {
        return this.entityCache.size();
    }

    public boolean getIfReceivedBackupPackets()
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            return this.sentBackupPackets & this.receivedBackupPackets;
        }

        return false;
    }

    @Override
    public void onGameInit()
    {
        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxLitematicaPacket.Payload.ID, ServuxLitematicaPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

    @Override
    public void onWorldPre()
    {
        if (DataManager.getInstance().hasIntegratedServer() == false)
        {
            HANDLER.registerPlayReceiver(ServuxLitematicaPacket.Payload.ID, HANDLER::receivePlayPayload);
        }
    }

    @Override
    public void onWorldJoin()
    {
        EntityUtils.initEntityUtils();
        // NO-OP
    }

	public void requestMetadata()
    {
        if (DataManager.getInstance().hasIntegratedServer() == false &&
            Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(ServuxLitematicaPacket.MetadataRequest(nbt));
        }
    }

    public boolean receiveServuxMetadata(CompoundTag data)
    {
        if (DataManager.getInstance().hasIntegratedServer() == false)
        {
            Litematica.debugLog("LitematicDataChannel: received METADATA from Servux");

            if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
            {
                if (data.getIntOr("version", -1) != ServuxLitematicaPacket.PROTOCOL_VERSION)
                {
                    Litematica.LOGGER.warn("LitematicDataChannel: Mis-matched protocol version!");
                }

                this.setServuxVersion(data.getStringOr("servux", "?"));
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

    @Override
    public @Nullable Pair<BlockEntity, CompoundTag> requestBlockEntityNbt(Level world, BlockPos pos)
    {
        Pair<BlockEntity, CompoundData> pair = this.requestBlockEntity(world, pos);

        if (pair != null)
        {
            return Pair.of(pair.getLeft(), DataConverterNbt.toVanillaCompound(pair.getRight()));
        }

        return null;
    }

    @Override
    public @Nullable Pair<BlockEntity, CompoundData> requestBlockEntity(Level world, BlockPos pos)
    {
        // Don't cache/request a BE for the Schematic World
        if (world instanceof WorldSchematic)
        {
            return this.refreshBlockEntityFromWorld(world, pos);
        }
        if (this.blockEntityCache.containsKey(pos))
        {
            // Refresh at 25%
            if (!DataManager.getInstance().hasIntegratedServer() &&
                (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() ||
                 Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue()))
            {
                if (System.currentTimeMillis() - this.blockEntityCache.get(pos).getLeft() > (this.getCacheTimeout() / 4))
                {
//                    Litematica.debugLog("requestBlockEntity: be at pos [{}] requeue at [{}] ms", pos.toShortString(), this.getCacheTimeout() / 4);
                    this.pendingBlockEntitiesQueue.add(pos);
                }
            }

            if (world instanceof ServerLevel)
            {
                return this.refreshBlockEntityFromWorld(world, pos);
            }

            return this.blockEntityCache.get(pos).getRight();
        }
        else if (world.getBlockState(pos).getBlock() instanceof EntityBlock)
        {
            if (DataManager.getInstance().hasIntegratedServer() == false &&
                (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() ||
                 Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue()))
            {
//                Litematica.debugLog("requestBlockEntity: be at pos [{}] queue at [{}] ms", pos.toShortString(), this.getCacheTimeout() / 4);
                this.pendingBlockEntitiesQueue.add(pos);
            }

            return this.refreshBlockEntityFromWorld(this.getClientWorld(), pos);
        }

        return null;
    }

    private @Nullable Pair<BlockEntity, CompoundData> refreshBlockEntityFromWorld(Level world, BlockPos pos)
    {
        if (world != null && world.getBlockState(pos).hasBlockEntity())
        {
            BlockEntity be = world.getChunkAt(pos).getBlockEntity(pos);

            if (be != null)
            {
				CompoundData nbt = DataConverterNbt.fromVanillaCompound(be.saveWithFullMetadata(world.registryAccess()));
                Pair<BlockEntity, CompoundData> pair = Pair.of(be, nbt);

                if (!(world instanceof WorldSchematic))
                {
                    synchronized (this.blockEntityCache)
                    {
                        this.blockEntityCache.put(pos, Pair.of(System.currentTimeMillis(), pair));
                    }
                }

                return pair;
            }
        }

        return null;
    }

    @Override
    public @Nullable Pair<Entity, CompoundTag> requestEntityNbt(Level world, int entityId)
    {
        Pair<Entity, CompoundData> pair = this.requestEntity(world, entityId);

        if (pair != null)
        {
            return Pair.of(pair.getLeft(), DataConverterNbt.toVanillaCompound(pair.getRight()));
        }

        return null;
    }

    @Override
    public @Nullable Pair<Entity, CompoundData> requestEntity(Level world, int entityId)
    {
        // Don't cache/request for the Schematic World
        if (world instanceof WorldSchematic)
        {
            return this.refreshEntityFromWorld(world, entityId);
        }
        if (this.entityCache.containsKey(entityId))
        {
            // Refresh at 25%
            if (!DataManager.getInstance().hasIntegratedServer() &&
                (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() ||
                 Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue()))
            {
                if (System.currentTimeMillis() - this.entityCache.get(entityId).getLeft() > (this.getCacheTimeout() / 4))
                {
                    //Litematica.debugLog("requestEntity: entity Id [{}] requeue at [{}] ms", entityId, this.getCacheTimeout() / 4);
                    this.pendingEntitiesQueue.add(entityId);
                }
            }

            // Refresh from Server World
            if (world instanceof ServerLevel)
            {
                return this.refreshEntityFromWorld(world, entityId);
            }

            return this.entityCache.get(entityId).getRight();
        }
        if (DataManager.getInstance().hasIntegratedServer() == false &&
            (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() ||
             Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue()))
        {
            this.pendingEntitiesQueue.add(entityId);
        }

        return this.refreshEntityFromWorld(this.getClientWorld(), entityId);
    }

    private @Nullable Pair<Entity, CompoundData> refreshEntityFromWorld(Level world, int entityId)
    {
        if (world != null)
        {
            Entity entity = world.getEntity(entityId);

            if (entity != null)
            {
                if (world instanceof WorldSchematic)
                {
                    NbtView view = NbtView.getWriter(world.registryAccess());
                    entity.saveWithoutId(view.getWriter());
					CompoundData data = view.readData();
                    Identifier id = EntityType.getKey(entity.getType());

                    if (data != null && id != null)
                    {
						data.putString("id", id.toString());
                        Pair<Entity, CompoundData> pair = Pair.of(entity, data.copy());

                        return pair;
                    }
                }
                else
                {
					CompoundData data = DataEntityUtils.invokeEntityDataTagNoPassengers(entity, entityId);

                    if (!data.isEmpty())
                    {
                        Pair<Entity, CompoundData> pair = Pair.of(entity, data);

                        synchronized (this.entityCache)
                        {
                            this.entityCache.put(entityId, Pair.of(System.currentTimeMillis(), pair));
                        }

                        return pair;
                    }
                }
            }
        }

        return null;
    }

    @Override
    @Nullable
    public Container getBlockInventory(Level world, BlockPos pos, boolean useNbt)
    {
        if (world instanceof WorldSchematic)
        {
            return InventoryUtils.getInventory(world, pos);
        }
        if (this.blockEntityCache.containsKey(pos))
        {
            Container inv = null;

            if (useNbt)
            {
                inv = InventoryUtils.getDataInventory(this.blockEntityCache.get(pos).getRight().getRight(), -1, world.registryAccess());
            }
            else
            {
                BlockEntity be = this.blockEntityCache.get(pos).getRight().getLeft();
                BlockState state = world.getBlockState(pos);

                if (state.is(BlockTags.AIR) || !state.hasBlockEntity())
                {
                    synchronized (this.blockEntityCache)
                    {
                        this.blockEntityCache.remove(pos);
                    }

                    // Don't keep requesting if we're tick warping or something.
                    return null;
                }

                if (be instanceof Container inv1)
                {
                    if (be instanceof ChestBlockEntity && state.hasProperty(ChestBlock.TYPE))
                    {
                        ChestType type = state.getValue(ChestBlock.TYPE);

                        if (type != ChestType.SINGLE)
                        {
                            BlockPos posAdj = pos.relative(ChestBlock.getConnectedDirection(state));
                            if (!world.hasChunkAt(posAdj)) return null;
                            BlockState stateAdj = world.getBlockState(posAdj);

                            var dataAdj = this.getFromBlockEntityCache(posAdj);

                            if (dataAdj == null)
                            {
                                this.requestBlockEntity(world, posAdj);
                            }

                            if (stateAdj.getBlock() == state.getBlock() &&
                                dataAdj instanceof ChestBlockEntity inv2 &&
                                stateAdj.getValue(ChestBlock.TYPE) != ChestType.SINGLE &&
                                stateAdj.getValue(ChestBlock.FACING) == state.getValue(ChestBlock.FACING))
                            {
                                Container invRight = type == ChestType.RIGHT ? inv1 : inv2;
                                Container invLeft = type == ChestType.RIGHT ? inv2 : inv1;

                                inv = new CompoundContainer(invRight, invLeft);
                            }
                        }
                        else
                        {
                            inv = inv1;
                        }
                    }
                    else
                    {
                        inv = inv1;
                    }
                }
            }

            if (inv != null)
            {
                return inv;
            }
        }

        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() ||
            Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            this.requestBlockEntity(world, pos);
        }

        return null;
    }

    @Override
    @Nullable
    public Container getEntityInventory(Level world, int entityId, boolean useNbt)
    {
        if (world instanceof WorldSchematic)
        {
            return null;
        }

        if (this.entityCache.containsKey(entityId) && this.getWorld() != null)
        {
            Container inv = null;

            if (useNbt)
            {
                inv = InventoryUtils.getDataInventory(this.entityCache.get(entityId).getRight().getRight(), -1, this.getWorld().registryAccess());
            }
            else
            {
                Entity entity = this.entityCache.get(entityId).getRight().getLeft();

                if (entity instanceof Container)
                {
                    inv = (Container) entity;
                }
                else if (entity instanceof Player player && player != null)
                {
                    inv = new SimpleContainer(player.getInventory().getNonEquipmentItems().toArray(new ItemStack[36]));
                }
                else if (entity instanceof Villager)
                {
                    inv = ((Villager) entity).getInventory();
                }
                else if (entity instanceof AbstractHorse)
                {
                    inv = ((IMixinAbstractHorseEntity) entity).malilib_getHorseInventory();
                }
                else if (entity instanceof Piglin)
                {
                    inv = ((IMixinPiglinEntity) entity).malilib_getInventory();
                }
            }

            if (inv != null)
            {
                return inv;
            }
        }

        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() ||
            Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            this.requestEntity(world, entityId);
        }

        return null;
    }

	@Override
	public BlockEntity handleBlockEntityData(BlockPos pos, CompoundTag nbt,
											 @Nullable Identifier type)
	{
		return this.handleBlockEntityData(pos, DataConverterNbt.fromVanillaCompound(nbt), type);
	}

	@Override
	public Entity handleEntityData(int entityId, CompoundTag nbt)
	{
		return this.handleEntityData(entityId, DataConverterNbt.fromVanillaCompound(nbt));
	}

	private void requestQueryBlockEntity(BlockPos pos)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() == false)
        {
            return;
        }

        ClientPacketListener handler = this.getVanillaHandler();

        if (handler != null)
        {
            this.sentBackupPackets = true;
            handler.getDebugQueryHandler().queryBlockEntityTag(pos, nbtCompound ->
            {
                this.handleBlockEntityData(pos, nbtCompound, null);
            });
            this.transactionToBlockPosOrEntityId.put(((IMixinDataQueryHandler) handler.getDebugQueryHandler()).malilib_currentTransactionId(), Either.left(pos));
        }
    }

    private void requestQueryEntityData(int entityId)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() == false)
        {
            return;
        }

        ClientPacketListener handler = this.getVanillaHandler();

        if (handler != null)
        {
            this.sentBackupPackets = true;
            handler.getDebugQueryHandler().queryEntityTag(entityId, nbtCompound ->
            {
                this.handleEntityData(entityId, nbtCompound);
            });
            this.transactionToBlockPosOrEntityId.put(((IMixinDataQueryHandler) handler.getDebugQueryHandler()).malilib_currentTransactionId(), Either.right(entityId));
        }
    }

    private void requestServuxBlockEntityData(BlockPos pos)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            HANDLER.encodeClientData(ServuxLitematicaPacket.BlockEntityRequest(pos));
        }
    }

    private void requestServuxEntityData(int entityId)
    {
        if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue())
        {
            HANDLER.encodeClientData(ServuxLitematicaPacket.EntityRequest(entityId));
        }
    }

    // The minY, maxY should be calculated based on the Selection Box...  But for now, we can just grab the entire chunk.
    public void requestServuxBulkEntityData(ChunkPos chunkPos, int minY, int maxY)
    {
        if (this.hasServuxServer() == false)
        {
            return;
        }

        CompoundTag req = new CompoundTag();

        this.completedChunks.remove(chunkPos);
        this.pendingChunks.add(chunkPos);
        this.pendingChunkTimeout.put(chunkPos, Util.getMillis());

        minY = Mth.clamp(minY, -60, 319);
        maxY = Mth.clamp(maxY, -60, 319);

        req.putString("Task", "BulkEntityRequest");
        req.putInt("minY", minY);
        req.putInt("maxY", maxY);

        Litematica.debugLog("EntitiesDataStorage#requestServuxBulkEntityData(): for chunkPos [{}] to Servux (minY [{}], maxY [{}])", chunkPos.toString(), minY, maxY);
        HANDLER.encodeClientData(ServuxLitematicaPacket.BulkNbtRequest(chunkPos, req));
    }

    public void requestBackupBulkEntityData(ChunkPos chunkPos, int minY, int maxY)
    {
        if (this.getIfReceivedBackupPackets() == false || this.hasServuxServer())
        {
            return;
        }

        this.completedChunks.remove(chunkPos);
        minY = Mth.clamp(minY, -60, 319);
        maxY = Mth.clamp(maxY, -60, 319);

        ClientLevel world = this.getClientWorld();
        ChunkAccess chunk = world != null ? world.getChunk(chunkPos.x(), chunkPos.z(), ChunkStatus.FULL, false) : null;

        if (chunk == null)
        {
            return;
        }

        BlockPos pos1 = new BlockPos(chunkPos.getMinBlockX(), minY, chunkPos.getMinBlockZ());
        BlockPos pos2 = new BlockPos(chunkPos.getMaxBlockX(),   maxY, chunkPos.getMaxBlockZ());
        AABB bb = PositionUtils.createEnclosingAABB(pos1, pos2);
        Set<BlockPos> teSet = chunk.getBlockEntitiesPos();
        List<Entity> entList = world.getEntities((Entity) null, bb, EntityUtils.NOT_PLAYER);

        Litematica.debugLog("EntitiesDataStorage#requestBackupBulkEntityData(): for chunkPos {} (minY [{}], maxY [{}]) // Request --> TE: [{}], E: [{}]", chunkPos.toString(), minY, maxY, teSet.size(), entList.size());
        //System.out.printf("0: ChunkPos [%s], Box [%s] // teSet [%d], entList [%d]\n", chunkPos.toString(), bb.toString(), teSet.size(), entList.size());

        for (BlockPos tePos : teSet)
        {
            if ((tePos.getX() < chunkPos.getMinBlockX() || tePos.getX() > chunkPos.getMaxBlockX()) ||
                (tePos.getZ() < chunkPos.getMinBlockZ() || tePos.getZ() > chunkPos.getMaxBlockZ()) ||
                (tePos.getY() < minY || tePos.getY() > maxY))
            {
                continue;
            }

            this.requestBlockEntity(world, tePos);
        }

        if (teSet.size() > 0)
        {
            this.pendingBackupChunk_BlockEntities.put(chunkPos, teSet);
        }

        Set<Integer> entSet = new LinkedHashSet<>();

        for (Entity entity : entList)
        {
            this.requestEntity(world, entity.getId());
            entSet.add(entity.getId());
        }

        if (entSet.size() > 0)
        {
            this.pendingBackupChunk_Entities.put(chunkPos, entSet);
        }

        if (teSet.size() > 0 || entSet.size() > 0)
        {
            this.pendingChunks.add(chunkPos);
            this.pendingChunkTimeout.put(chunkPos, Util.getMillis());
        }
        else
        {
            this.completedChunks.add(chunkPos);
        }
    }

    private boolean markBackupBlockEntityComplete(ChunkPos chunkPos, BlockPos pos)
    {
        if (this.getIfReceivedBackupPackets() == false || this.hasServuxServer())
        {
            return true;
        }

        //Litematica.debugLog("EntitiesDataStorage#markBackupBlockEntityComplete() - Marking ChunkPos {} - Block Entity at [{}] as complete.", chunkPos.toString(), pos.toShortString());

        if (this.pendingChunks.contains(chunkPos))
        {
            if (this.pendingBackupChunk_BlockEntities.containsKey(chunkPos))
            {
                Set<BlockPos> teSet = this.pendingBackupChunk_BlockEntities.get(chunkPos);

                if (teSet.contains(pos))
                {
                    teSet.remove(pos);

                    if (teSet.isEmpty())
                    {
                        Litematica.debugLog("EntitiesDataStorage#markBackupBlockEntityComplete(): ChunkPos {} - Block Entity List Complete!", chunkPos.toString());
                        this.pendingBackupChunk_BlockEntities.remove(chunkPos);
                        this.pendingChunks.remove(chunkPos);
                        this.pendingChunkTimeout.remove(chunkPos);
                        this.completedChunks.add(chunkPos);
                        return true;
                    }
                    else
                    {
                        this.pendingBackupChunk_BlockEntities.replace(chunkPos, teSet);
                    }
                }
            }
        }

        return false;
    }

    private boolean markBackupEntityComplete(ChunkPos chunkPos, int entityId)
    {
        if (this.getIfReceivedBackupPackets() == false || this.hasServuxServer())
        {
            return true;
        }

        //Litematica.debugLog("EntitiesDataStorage#markBackupEntityComplete() - Marking ChunkPos {} - EntityId [{}] as complete.", chunkPos.toString(), entityId);

        if (this.pendingChunks.contains(chunkPos))
        {
            if (this.pendingBackupChunk_Entities.containsKey(chunkPos))
            {
                Set<Integer> entSet = this.pendingBackupChunk_Entities.get(chunkPos);

                if (entSet.contains(entityId))
                {
                    entSet.remove(entityId);

                    if (entSet.isEmpty())
                    {
                        Litematica.debugLog("EntitiesDataStorage#markBackupEntityComplete(): ChunkPos {} - EntitiyList Complete!", chunkPos.toString());
                        this.pendingBackupChunk_Entities.remove(chunkPos);
                        this.pendingChunks.remove(chunkPos);
                        this.pendingChunkTimeout.remove(chunkPos);
                        this.completedChunks.add(chunkPos);
                        return true;
                    }
                    else
                    {
                        this.pendingBackupChunk_Entities.replace(chunkPos, entSet);
                    }
                }
            }
        }

        return false;
    }

    @Override
    @Nullable
    public BlockEntity handleBlockEntityData(BlockPos pos, CompoundData data, @Nullable Identifier type)
    {
        this.pendingBlockEntitiesQueue.remove(pos);
        if (data == null || this.getClientWorld() == null) return null;

        BlockEntity blockEntity = this.getClientWorld().getBlockEntity(pos);

        if (blockEntity != null && (type == null || type.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()))))
        {
            if (data.contains(NbtKeys.ID, Constants.NBT.TAG_STRING) == false)
            {
                Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());

                if (id != null)
                {
					data.putString(NbtKeys.ID, id.toString());
                }
            }

            synchronized (this.blockEntityCache)
            {
                this.blockEntityCache.put(pos, Pair.of(System.currentTimeMillis(), Pair.of(blockEntity, data)));
            }

            NbtView view = NbtView.getReader(data, this.getClientWorld().registryAccess());
            blockEntity.loadWithComponents(view.getReader());
            ChunkPos chunkPos = ChunkPos.containing(pos);

            if (this.hasPendingChunk(chunkPos) && this.hasServuxServer() == false)
            {
                this.markBackupBlockEntityComplete(chunkPos, pos);
            }

            return blockEntity;
        }

        Optional<Holder.Reference<BlockEntityType<?>>> opt = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(type);

        if (opt.isPresent())
        {
            BlockEntityType<?> beType = opt.get().value();

            if (beType.isValid(this.getClientWorld().getBlockState(pos)))
            {
                BlockEntity blockEntity2 = beType.create(pos, this.getClientWorld().getBlockState(pos));

                if (blockEntity2 != null)
                {
                    if (data.contains(NbtKeys.ID, Constants.NBT.TAG_STRING) == false)
                    {
                        Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(beType);

                        if (id != null)
                        {
							data.putString(NbtKeys.ID, id.toString());
                        }
                    }
                    synchronized (this.blockEntityCache)
                    {
                        this.blockEntityCache.put(pos, Pair.of(System.currentTimeMillis(), Pair.of(blockEntity2, data)));
                    }

                    if (Configs.Generic.ENTITY_DATA_LOAD_NBT.getBooleanValue())
                    {
                        NbtView view = NbtView.getReader(data, this.getClientWorld().registryAccess());
                        blockEntity2.loadWithComponents(view.getReader());
                        this.getClientWorld().setBlockEntity(blockEntity2);
                    }

                    ChunkPos chunkPos = ChunkPos.containing(pos);

                    if (this.hasPendingChunk(chunkPos) && this.hasServuxServer() == false)
                    {
                        this.markBackupBlockEntityComplete(chunkPos, pos);
                    }

                    return blockEntity2;
                }
            }
        }

        return null;
    }

    @Override
    @Nullable
    public Entity handleEntityData(int entityId, CompoundData data)
    {
        this.pendingEntitiesQueue.remove(entityId);
        if (data == null || this.getClientWorld() == null) return null;
        Entity entity = this.getClientWorld().getEntity(entityId);

        if (entity != null)
        {
            if (data.contains(NbtKeys.ID, Constants.NBT.TAG_STRING) == false)
            {
                Identifier id = EntityType.getKey(entity.getType());

                if (id != null)
                {
					data.putString(NbtKeys.ID, id.toString());
                }
            }
            synchronized (this.entityCache)
            {
                this.entityCache.put(entityId, Pair.of(System.currentTimeMillis(), Pair.of(entity, data)));
            }

            if (Configs.Generic.ENTITY_DATA_LOAD_NBT.getBooleanValue())
            {
                EntityUtils.loadNbtIntoEntity(entity, DataConverterNbt.toVanillaCompound(data));
            }

            if (this.hasPendingChunk(entity.chunkPosition()) && this.hasServuxServer() == false)
            {
                this.markBackupEntityComplete(entity.chunkPosition(), entityId);
            }
        }

        return entity;
    }

	@Override
    public void handleBulkEntityData(int transactionId, @Nullable CompoundTag nbt)
	{
		if (nbt == null)
		{
			return;
		}

		this.handleBulkEntityData(transactionId, DataConverterNbt.fromVanillaCompound(nbt));
	}

	public void handleBulkEntityData(int transactionId, @Nullable CompoundData data)
	{
        String task = data.getStringOrDefault("Task", "BulkEntityReply");

        // TODO --> Split out the task this way (I should have done this under sakura.12, etc),
        //  So we need to check if the "Task" is not included for now... (Wait for the updates to bake in)
        if (task.equals("BulkEntityReply"))
        {
            ListData tileList = data.containsLenient("TileEntities") ? data.getList("TileEntities") : new ListData();
			ListData entityList = data.containsLenient("Entities") ? data.getList("Entities") : new ListData();
            ChunkPos chunkPos = new ChunkPos(data.getInt("chunkX"), data.getInt("chunkZ"));

            this.shouldUseLongTimeout = true;

            for (int i = 0; i < tileList.size(); ++i)
            {
				CompoundData te = tileList.getCompoundAt(i);
                BlockPos pos = DataTypeUtils.readBlockPos(te);
                Identifier type = Identifier.parse(te.getString("id"));

                this.handleBlockEntityData(pos, te, type);
            }

            for (int i = 0; i < entityList.size(); ++i)
            {
				CompoundData ent = entityList.getCompoundAt(i);
                Vec3 pos = DataTypeUtils.readVec3dFromListTag(ent);
                int entityId = ent.getInt("entityId");

                this.handleEntityData(entityId, ent);
            }

            this.pendingChunks.remove(chunkPos);
            this.pendingChunkTimeout.remove(chunkPos);
            this.completedChunks.add(chunkPos);

            Litematica.debugLog("EntitiesDataStorage#handleBulkEntityData(): [ChunkPos {}] received TE: [{}], and E: [{}] entiries from Servux", chunkPos.toString(), tileList.size(), entityList.size());
        }
    }

	@Override
	public void handleVanillaQueryNbt(int transactionId, CompoundData data)
	{
		if (this.checkOpStatus)
		{
			this.hasOpStatus = true;
			this.checkOpStatus = false;
			this.lastOpCheck = System.currentTimeMillis();
		}

		Either<BlockPos, Integer> either = this.transactionToBlockPosOrEntityId.remove(transactionId);

		if (either != null)
		{
			this.receivedBackupPackets = true;
			either.ifLeft(pos ->     handleBlockEntityData(pos, data, null))
				  .ifRight(entityId -> handleEntityData(entityId, data));
		}
	}

	@Override
    public void handleVanillaQueryNbt(int transactionId, CompoundTag nbt)
    {
		this.handleVanillaQueryNbt(transactionId, DataConverterNbt.fromVanillaCompound(nbt));
    }

    public boolean hasPendingChunk(ChunkPos pos)
    {
        if (this.hasServuxServer() || this.getIfReceivedBackupPackets())
        {
            return this.pendingChunks.contains(pos);
        }

        return false;
    }

    private void checkForPendingChunkTimeout(ChunkPos pos)
    {
        if ((this.hasServuxServer() && this.hasPendingChunk(pos)) ||
            (this.getIfReceivedBackupPackets() && this.hasPendingChunk(pos)) &&
             this.mc.level != null)
        {
            long now = Util.getMillis();

            // Take no action when ChunkPos is not loaded by the ClientWorld.
            if (WorldUtils.isClientChunkLoaded(this.mc.level, pos.x(), pos.z()) == false)
            {
                this.pendingChunkTimeout.replace(pos, now);
                return;
            }

            long duration = now - this.pendingChunkTimeout.get(pos);

            if (duration > (this.getChunkTimeoutMs()))
            {
                Litematica.debugLog("EntitiesDataStorage#checkForPendingChunkTimeout(): [ChunkPos {}] has timed out waiting for data, marking complete without Receiving Entity Data.", pos.toString());
                this.pendingChunkTimeout.remove(pos);
                this.pendingChunks.remove(pos);
                this.completedChunks.add(pos);
            }
        }
    }

    private long getChunkTimeoutMs()
    {
        if (this.hasServuxServer())
        {
            return this.chunkTimeoutMs;
        }
        else if (this.getIfReceivedBackupPackets())
        {
            return this.chunkTimeoutMs + 3000L;
        }

        return 1000L;
    }

    public boolean hasCompletedChunk(ChunkPos pos)
    {
        if (this.hasServuxServer() || this.getIfReceivedBackupPackets())
        {
            this.checkForPendingChunkTimeout(pos);
            return this.completedChunks.contains(pos);
        }

        return true;
    }

    public void markCompletedChunkDirty(ChunkPos pos)
    {
        if (this.hasServuxServer() || this.getIfReceivedBackupPackets())
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
