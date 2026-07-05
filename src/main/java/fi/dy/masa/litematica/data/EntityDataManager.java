package fi.dy.masa.litematica.data;

import java.util.*;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;

import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.interfaces.IDataSyncer;
import fi.dy.masa.malilib.mixin.entity.IMixinAbstractHorseEntity;
import fi.dy.masa.malilib.mixin.entity.IMixinAbstractNautilus;
import fi.dy.masa.malilib.mixin.entity.IMixinPiglinEntity;
import fi.dy.masa.malilib.mixin.network.IMixinDataQueryHandler;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.MathUtils;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.DataEntityUtils;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.converter.DataConverterNbt;
import fi.dy.masa.malilib.util.data.tag.util.DataTypeUtils;
import fi.dy.masa.malilib.util.data_syncer.EntityDataCache;
import fi.dy.masa.malilib.util.data_syncer.EntityDataRequestTracker;
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

public class EntityDataManager implements IClientTickHandler, IDataSyncer
{
    private static final EntityDataManager INSTANCE = new EntityDataManager();
    public static EntityDataManager getInstance()
    {
        return INSTANCE;
    }

    private final static ServuxLitematicaHandler<ServuxLitematicaPacket.Payload> HANDLER = ServuxLitematicaHandler.getInstance();
    private final static long LONG_CACHE_TIMEOUT = 30L;
    private final static long CHUNK_TIMEOUT_MS = 7500L;
    private final Minecraft mc;
    private ClientLevel clientWorld;
    private boolean servuxServer = false;
    private boolean hasInValidServux = false;
    private String servuxVersion;
    // Wait 5 seconds for loaded Client Chunks to receive Entity Data
    private boolean checkOpStatus = true;
    private boolean hasOpStatus = false;
    private long lastOpCheck;
    private boolean sentBackupPackets = false;
    private boolean receivedBackupPackets = false;
    private boolean shouldUseLongTimeout = false;

    private final EntityDataCache cache;
    private final EntityDataRequestTracker requestTracker;
    private final Map<Integer, Either<BlockPos, Integer>> transactionToBlockPosOrEntityId = new HashMap<>();
    private long lastTickTime;

    // Needs a long cache timeout for saving schematics
    private final Set<ChunkPos> pendingChunks = new LinkedHashSet<>();
    private final Set<ChunkPos> completedChunks = new LinkedHashSet<>();
    private final Map<ChunkPos, Long> pendingChunkTimeout = new HashMap<>();

    // Backup Chunk Saving task
    private final HashMap<ChunkPos, Set<BlockPos>> pendingBackupChunk_BlockEntities = new HashMap<>();
    private final HashMap<ChunkPos, Set<Integer>>  pendingBackupChunk_Entities      = new HashMap<>();

    @Override
    @Nullable
    public Level getBestWorld()
    {
        return fi.dy.masa.malilib.util.WorldUtils.getBestWorld(this.mc);
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

    private EntityDataManager()
    {
        this.mc = Minecraft.getInstance();
        this.cache = new EntityDataCache(Reference.MOD_ID, this.getCacheTimeoutLong());
        this.requestTracker = new EntityDataRequestTracker();
        this.lastTickTime = System.currentTimeMillis();
        this.lastOpCheck = 0L;
        Registry.ENTITY_DATA_REGISTRY.registerEntityDataCache(this.cache);
    }

    @Override
    public void onClientTick(Minecraft mc)
    {
        final long now = System.currentTimeMillis();

        if ((now - this.lastTickTime) > 50)
        {
            // In this block, we do something every server tick
            if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() == false)
            {
                this.lastTickTime = now;

                if (DataManager.getInstance().hasIntegratedServer() == false && this.hasServuxServer())
                {
                    this.servuxServer = false;
                    HANDLER.unregisterPlayReceiver();
                }

                if (Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() == false)
                {
                    this.requestTracker.clearAll();
                    return;
                }
            }
            else if (Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue() &&
                     DataManager.getInstance().hasIntegratedServer() == false &&
                     this.hasServuxServer() == false &&
                     this.hasInValidServux == false &&
                     this.getBestWorld() != null)
            {
                // Make sure we're Play Registered, and request Metadata
                HANDLER.registerPlayReceiver(ServuxLitematicaPacket.Payload.ID, HANDLER::receivePlayPayload);
                this.requestMetadata();
            }

            // Expire cached NBT
            this.tickCache(now);

            // 5 queries / server tick
            final int limit = Configs.Generic.SERVER_NBT_REQUEST_RATE.getIntegerValue();

            for (int i = 0; i < limit; i++)
            {
                BlockPos nextPos = this.requestTracker.pollNextBlockEntity();
                Integer nextId = this.requestTracker.pollNextEntity();

                if (nextPos == null && nextId == null) { break; }

                if (nextPos != null)
                {
                    if (this.hasServuxServer())
                    {
                        this.requestServuxBlockEntityData(nextPos);
                    }
                    else if (this.shouldUseQuery())
                    {
                        // Only check once if we have OP
                        this.requestQueryBlockEntity(nextPos);
                    }
                }

                if (nextId != null)
                {
                    if (this.hasServuxServer())
                    {
                        this.requestServuxEntityData(nextId);
                    }
                    else if (this.shouldUseQuery())
                    {
                        // Only check once if we have OP
                        this.requestQueryEntityData(nextId);
                    }
                }
            }

            this.lastTickTime = now;
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
            Litematica.debugLog("EntityDataManager#reset() - log-out");
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
            Litematica.debugLog("EntityDataManager#reset() - dimension change or log-in");
            long now = System.currentTimeMillis();
            this.lastTickTime = now - (this.getCacheTimeout() + 5000L);
            this.tickCache(now);
            this.lastTickTime = now;
            this.clientWorld = mc.level;
            this.checkOpStatus = true;
            this.lastOpCheck = now;
        }

        // Clear data
        this.clearAll();

        // Litematic Save values
        this.completedChunks.clear();
        this.pendingChunks.clear();
        this.pendingChunkTimeout.clear();
        this.pendingBackupChunk_BlockEntities.clear();
        this.pendingBackupChunk_Entities.clear();
    }

    private boolean shouldUseQuery()
    {
        if (this.hasOpStatus) { return true; }
        if (this.checkOpStatus)
        {
            // Check for 15 minutes after login, or changing dimensions
            if ((System.currentTimeMillis() - this.lastOpCheck) < 900000L) { return true; }
            this.checkOpStatus = false;
        }

        return false;
    }

    public void resetOpCheck()
    {
        this.hasOpStatus = false;
        this.checkOpStatus = true;
        this.lastOpCheck = System.currentTimeMillis();
    }

    @Override
    public EntityDataCache getCache()
    {
        return this.cache;
    }

    @Override
    public EntityDataRequestTracker getRequestTracker()
    {
        return this.requestTracker;
    }

    @Override
    public boolean isEnabled()
    {
        return Configs.Generic.ENTITY_DATA_SYNC.getBooleanValue();
    }

    @Override
    public boolean isBackupEnabled()
    {
        return Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue();
    }

    @Override
    public boolean loadContainerBlockEntities()
    {
        return true;
    }

    @Override
    public long getRefreshTime()
    {
//        long result = (long) (Mth.clamp(Configs.Generic.ENTITY_DATA_SYNC_CACHE_REFRESH.getFloatValue(), 0.05f, 1.0f) * 1000L);
//        long clamp = (this.getCacheTimeout() / 2);
//        return MathUtils.min(result, clamp);
        return (this.getCacheTimeout() / 4);
    }

    @Override
    public long getCacheTimeout()
    {
        // Increase cache timeout when in Backup Mode.
        int modifier = Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() ? 5 : 1;
        return (long) (MathUtils.clamp((Configs.Generic.ENTITY_DATA_SYNC_CACHE_TIMEOUT.getFloatValue() * modifier), 1.0f, 50.0f) * 1000L);
    }

    private long getCacheTimeoutLong()
    {
        // Increase cache timeout when in Backup Mode.
        final int modifier = Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue() ? 5 : 1;
        final long result = (long) (MathUtils.clamp(((Configs.Generic.ENTITY_DATA_SYNC_CACHE_TIMEOUT.getFloatValue() * modifier) * LONG_CACHE_TIMEOUT), 120.0f, (300.0f * modifier)) * 1000L);

        // Add extra time if using QueryNbt only
        if (!this.hasServuxServer() && this.getIfReceivedBackupPackets())
        {
            return result + 3000L;
        }

        return result;
    }

    // Litematica still needs to manage the "Long" timeout logic.
    private void tickCache(final long nowTime)
    {
        if (this.shouldUseLongTimeout)
        {
            if (this.cache.getTimeout() != this.getCacheTimeoutLong())
            {
                this.cache.setTimeout(this.getCacheTimeoutLong());
            }
        }
        else
        {
            if (this.cache.getTimeout() != this.getCacheTimeout())
            {
                this.cache.setTimeout(this.getCacheTimeout());
            }
        }

        this.cache.tickCache(nowTime);

        boolean beEmpty = this.cache.blockEntityCount() == 0;
        boolean entEmpty = this.cache.entityCount() == 0;

        // End Long timeout phase
        if (beEmpty && entEmpty && this.shouldUseLongTimeout)
        {
            this.shouldUseLongTimeout = false;
        }
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

    public boolean hasOperatorStatus()
    {
        return this.hasOpStatus;
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
        return servuxVersion;
    }

    public int getPendingBlockEntitiesCount()
    {
        return this.requestTracker.getPendingBlockEntityCount();
    }

    public int getPendingEntitiesCount()
    {
        return this.requestTracker.getPendingEntityCount();
    }

    public int getBlockEntityCacheCount()
    {
        return this.cache.blockEntityCount();
    }

    public int getEntityCacheCount()
    {
        return this.cache.entityCount();
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

    public void onEntityDataSyncToggled(ConfigBoolean config)
    {
        if (this.hasInValidServux)
        {
            this.reset(true);
        }

        // Do something?
    }

    /**
     * These are required due to the Schematic World
     * -
     * @param world -
     * @param pos -
     * @return -
     */
    public @Nullable Pair<BlockEntity, CompoundData> requestBlockEntityWrapped(Level world, BlockPos pos)
    {
        // Don't cache/request a BE for the Schematic World
        if (world instanceof WorldSchematic)
        {
            return this.refreshBlockEntityFromWorld(world, pos);
        }

        return this.requestBlockEntity(world, pos);
    }

    /**
     * These are required due to the Schematic World
     * -
     * @param world -
     * @param entityId -
     * @return -
     */
    public @Nullable Pair<Entity, CompoundData> requestEntityWrapped(Level world, int entityId)
    {
        // Don't cache/request for the Schematic World
        if (world instanceof WorldSchematic)
        {
            return this.refreshEntityFromWorld(world, entityId);
        }

        return this.requestEntity(world, entityId);
    }

    /**
     * These are required due to the Schematic World
     * -
     * @param world -
     * @param pos -
     * @param useNbt -
     * @return -
     */
    @Nullable
    public Container getBlockInventoryWrapped(Level world, BlockPos pos, boolean useNbt)
    {
        if (world instanceof WorldSchematic)
        {
            return InventoryUtils.getInventory(world, pos);
        }

        return this.getBlockInventory(world, pos, useNbt);
    }

    /**
     * These are required due to the Schematic World
     * -
     * @param world -
     * @param entityId -
     * @param useNbt -
     * @return -
     */
    @Nullable
    public Container getEntityInventoryWrapped(Level world, int entityId, boolean useNbt)
    {
        if (world instanceof WorldSchematic ws)
        {
            Container inv = null;
            Entity entity = ws.getEntity(entityId);

            if (entity != null)
            {
                if (useNbt)
                {
                    CompoundData data = DataEntityUtils.invokeEntityDataTagNoPassengers(entity, entityId);
                    inv = InventoryUtils.getDataInventory(data, -1, ws.registryAccess());
                }
                else
                {
	                switch (entity)
	                {
		                case Container itemStacks -> inv = itemStacks;
		                case Player player when player != null ->
				                inv = new SimpleContainer(player.getInventory().getNonEquipmentItems().toArray(new ItemStack[36]));
		                case Villager villager -> inv = villager.getInventory();
		                case AbstractHorse abstractHorse ->
				                inv = ((IMixinAbstractHorseEntity) entity).malilib_getHorseInventory();
		                case AbstractNautilus abstractNautilus ->
				                inv = ((IMixinAbstractNautilus) entity).malilib_getNautilusInventory();
		                case Piglin piglin -> inv = ((IMixinPiglinEntity) entity).malilib_getInventory();
		                default -> {}
	                }
                }

	            return inv;
            }

            return null;
        }

        return this.getEntityInventory(world, entityId, useNbt);
    }

	private void requestQueryBlockEntity(BlockPos pos)
    {
        if (!Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            return;
        }

        ClientPacketListener handler = this.getVanillaHandler();

        if (handler != null)
        {
            this.sentBackupPackets = true;
            handler.getDebugQueryHandler().queryBlockEntityTag(pos, nbtCompound -> this.handleBlockEntityData(pos, nbtCompound));
            this.transactionToBlockPosOrEntityId.put(((IMixinDataQueryHandler) handler.getDebugQueryHandler()).malilib_currentTransactionId(), Either.left(pos));
        }
    }

    private void requestQueryEntityData(int entityId)
    {
        if (!Configs.Generic.ENTITY_DATA_SYNC_BACKUP.getBooleanValue())
        {
            return;
        }

        ClientPacketListener handler = this.getVanillaHandler();

        if (handler != null)
        {
            this.sentBackupPackets = true;
            handler.getDebugQueryHandler().queryEntityTag(entityId, nbtCompound -> this.handleEntityData(entityId, nbtCompound));
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
        if (!this.hasServuxServer()) { return; }
        CompoundTag req = new CompoundTag();

        this.completedChunks.remove(chunkPos);
        this.pendingChunks.add(chunkPos);
        this.pendingChunkTimeout.put(chunkPos, System.currentTimeMillis());

        minY = Mth.clamp(minY, -60, 319);
        maxY = Mth.clamp(maxY, -60, 319);

        req.putString("Task", "BulkEntityRequest");
        req.putInt("minY", minY);
        req.putInt("maxY", maxY);

        Litematica.debugLog("EntityDataManager#requestServuxBulkEntityData(): for chunkPos {} to Servux (minY [{}], maxY [{}])", chunkPos.toString(), minY, maxY);
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
        ChunkAccess chunk = world != null ? world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) : null;

        if (chunk == null)
        {
            return;
        }

        BlockPos pos1 = new BlockPos(chunkPos.getMinBlockX(), minY, chunkPos.getMinBlockZ());
        BlockPos pos2 = new BlockPos(chunkPos.getMaxBlockX(),   maxY, chunkPos.getMaxBlockZ());
        AABB bb = PositionUtils.createEnclosingAABB(pos1, pos2);
        Set<BlockPos> teSet = new HashSet<>(chunk.getBlockEntitiesPos());
	    List<Entity> entList = world.getEntities((Entity) null, bb, EntityUtils.NOT_PLAYER);

        Litematica.debugLog("EntityDataManager#requestBackupBulkEntityData(): for chunkPos {} (minY [{}], maxY [{}]) // Request --> TE: [{}], E: [{}]", chunkPos.toString(), minY, maxY, teSet.size(), entList.size());
        //System.out.printf("0: ChunkPos [%s], Box [%s] // teSet [%d], entList [%d]\n", chunkPos.toString(), bb.toString(), teSet.size(), entList.size());

        for (BlockPos tePos : teSet)
        {
            if ((tePos.getX() < chunkPos.getMinBlockX() || tePos.getX() > chunkPos.getMaxBlockX()) ||
                (tePos.getZ() < chunkPos.getMinBlockZ() || tePos.getZ() > chunkPos.getMaxBlockZ()) ||
                (tePos.getY() < minY || tePos.getY() > maxY))
            {
                continue;
            }

            this.requestBlockEntityWrapped(world, tePos);
        }

        if (teSet.size() > 0)
        {
            this.pendingBackupChunk_BlockEntities.put(chunkPos, teSet);
        }

        Set<Integer> entSet = new LinkedHashSet<>();

        for (Entity entity : entList)
        {
            this.requestEntityWrapped(world, entity.getId());
            entSet.add(entity.getId());
        }

        if (entSet.size() > 0)
        {
            this.pendingBackupChunk_Entities.put(chunkPos, entSet);
        }

        if (teSet.size() > 0 || entSet.size() > 0)
        {
            this.pendingChunks.add(chunkPos);
            this.pendingChunkTimeout.put(chunkPos, System.currentTimeMillis());
        }
        else
        {
            this.completedChunks.add(chunkPos);
        }
    }

    private boolean markBackupBlockEntityComplete(ChunkPos chunkPos, BlockPos pos)
    {
        if (!this.getIfReceivedBackupPackets() || this.hasServuxServer())
        {
            return true;
        }

        if (Reference.DEBUG_MODE)
        {
            Litematica.LOGGER.warn("EntityDataManager#markBackupBlockEntityComplete() - Marking ChunkPos {} - Block Entity at [{}] as complete.", chunkPos.toString(), pos.toShortString());
        }

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
                        Litematica.debugLog("EntityDataManager#markBackupBlockEntityComplete(): ChunkPos {} - Block Entity List Complete!", chunkPos.toString());
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

        if (Reference.DEBUG_MODE)
        {
            Litematica.LOGGER.warn("EntityDataManager#markBackupEntityComplete() - Marking ChunkPos {} - EntityId [{}] as complete.", chunkPos.toString(), entityId);
        }

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
                        Litematica.debugLog("EntityDataManager#markBackupEntityComplete(): ChunkPos {} - EntitiyList Complete!", chunkPos.toString());
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
    public BlockEntity handleBlockEntityData(BlockPos pos, CompoundData data)
    {
        this.getRequestTracker().removeScheduledBlockEntity(pos);
        this.getRequestTracker().setPendingLocalBlockEntityRequest(pos, false);
        if (data == null || this.getClientWorld() == null) { return null; }

        BlockEntity be = this.getClientWorld().getBlockEntity(pos);

        if (be != null)
        {
            if (!data.contains(NbtKeys.ID, Constants.NBT.TAG_STRING))
            {
                Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());

                if (id != null)
                {
					data.putString(NbtKeys.ID, id.toString());
                }
            }

            this.getCache().removeFromCache(pos);
            this.getCache().addToCache(pos, be, data);

            ChunkPos chunkPos = new ChunkPos(pos);

            if ((this.loadContainerBlockEntities() && be instanceof Container) || this.hasPendingChunk(chunkPos))
            {
                NbtView view = NbtView.getReader(data, this.getClientWorld().registryAccess());
                be.loadWithComponents(view.getReader());
            }

            if (this.hasPendingChunk(chunkPos) && !this.hasServuxServer())
            {
                this.markBackupBlockEntityComplete(chunkPos, pos);
            }

            return be;
        }

        return null;
    }

    @Override
    @Nullable
    public Entity handleEntityData(int entityId, CompoundData data)
    {
        this.getRequestTracker().removeScheduledEntity(entityId);
        this.getRequestTracker().setPendingLocalEntityRequest(entityId, false);
        if (data == null || this.getClientWorld() == null) { return null; }
        Entity entity = this.getClientWorld().getEntity(entityId);

        if (entity != null)
        {
            if (!data.contains(NbtKeys.ID, Constants.NBT.TAG_STRING))
            {
                Identifier id = EntityType.getKey(entity.getType());

                if (id != null)
                {
					data.putString(NbtKeys.ID, id.toString());
                }
            }

            this.getCache().removeFromCache(entityId);
            this.getCache().addToCache(entityId, entity, data);

            if (this.hasPendingChunk(entity.chunkPosition()) && !this.hasServuxServer())
            {
                this.markBackupEntityComplete(entity.chunkPosition(), entityId);
            }
        }

        return entity;
    }

	public void handleBulkEntityData(int transactionId, @Nullable CompoundData data)
	{
        if (data == null || this.getClientWorld() == null) { return; }
        String task = data.getStringOrDefault("Task", "BulkEntityReply");

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
//                Identifier type = Identifier.parse(te.getString("id"));
                this.handleBlockEntityData(pos, te);
            }

            for (int i = 0; i < entityList.size(); ++i)
            {
				CompoundData ent = entityList.getCompoundAt(i);
//                Vec3 pos = DataTypeUtils.readVec3dFromListTag(ent);
                int entityId = ent.getInt("entityId");

                this.handleEntityData(entityId, ent);
            }

            this.pendingChunks.remove(chunkPos);
            this.pendingChunkTimeout.remove(chunkPos);
            this.completedChunks.add(chunkPos);

            Litematica.debugLog("EntityDataManager#handleBulkEntityData(): chunkPos {} received TE: [{}], and E: [{}] entiries from Servux", chunkPos.toString(), tileList.size(), entityList.size());
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
			either.ifLeft(pos ->     this.handleBlockEntityData(pos, data))
				  .ifRight(entityId -> this.handleEntityData(entityId, data));
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
            (this.getIfReceivedBackupPackets() && this.hasPendingChunk(pos)))
        {
            ClientLevel cw = this.getClientWorld();
            long now = System.currentTimeMillis();

            // Take no action when ChunkPos is not loaded by the ClientWorld.
            if (cw != null && !WorldUtils.isClientChunkLoaded(cw, pos.x, pos.z))
            {
                this.pendingChunkTimeout.replace(pos, now);
                return;
            }

            long duration = now - this.pendingChunkTimeout.get(pos);

            if (duration > (this.getChunkTimeoutMs()))
            {
                Litematica.debugLog("EntityDataManager#checkForPendingChunkTimeout(): chunkPos {} has timed out waiting for data, marking complete without Receiving Entity Data.", pos.toString());
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
            return CHUNK_TIMEOUT_MS;
        }
        else if (this.getIfReceivedBackupPackets())
        {
            return CHUNK_TIMEOUT_MS + 3000L;
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
