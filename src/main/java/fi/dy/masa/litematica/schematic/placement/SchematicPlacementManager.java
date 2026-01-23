package fi.dy.masa.litematica.schematic.placement;

import java.util.*;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfirmAction;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IConfirmationListener;
import fi.dy.masa.malilib.network.PacketSplitter;
import fi.dy.masa.malilib.util.*;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.command.PmCommand;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.EntitiesDataStorage;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.network.ServuxLitematicaHandler;
import fi.dy.masa.litematica.network.ServuxLitematicaPacket;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.infohud.StatusInfoRenderer;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkBase;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkCommand;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkDirect;
import fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicSetblockToMcfunction;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.util.*;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils.ChunkPosDistanceComparator;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.ChunkSchematicState;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

public class SchematicPlacementManager
{
    protected final List<SchematicPlacement> schematicPlacements;
    protected final ArrayListMultimap<ChunkPos, SchematicPlacement> schematicsTouchingChunk;
    protected final Long2ObjectOpenHashMap<List<PlacementPart>> touchedVolumesInChunk;
    protected final Set<ChunkPos> chunksPreChange;
    protected final List<ChunkPos> visibleChunks;
    protected final Supplier<WorldSchematic> worldSupplier;
    protected ChunkPos lastVisibleChunksSortPos;
    protected boolean visibleChunksNeedsUpdate;
    private final int tickRate = 6;      // in seconds
    private long lastTick;

    public SchematicPlacementManager()
    {
        this(SchematicWorldHandler::getSchematicWorld);
    }

    protected SchematicPlacementManager(Supplier<WorldSchematic> worldSupplier)
    {
        this.schematicPlacements = new ArrayList<>();
        this.schematicsTouchingChunk = ArrayListMultimap.create();
        this.touchedVolumesInChunk = new Long2ObjectOpenHashMap<>();
        this.chunksPreChange = new HashSet<>();
        this.visibleChunks = new ArrayList<>();
        this.lastVisibleChunksSortPos = ChunkPos.ZERO;

        this.worldSupplier = worldSupplier;
        this.lastTick = System.currentTimeMillis();
    }

    @Nullable
    private SchematicPlacement selectedPlacement;

    public void setVisibleSubChunksNeedsUpdate()
    {
        this.visibleChunksNeedsUpdate = true;
    }

    protected boolean hasTimeToExecuteMoreTasks()
    {
        return (System.nanoTime() - DataManager.getClientTickStartTime()) <= 35000000L;
    }

    protected boolean canHandleChunk(ClientLevel clientWorld, int chunkX, int chunkZ)
    {
        return Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() ||
               WorldUtils.isClientChunkLoaded(clientWorld, chunkX, chunkZ);
    }

    // This fixes when joining the world, and your placement's aren't being rendered
    public void onWorldJoin()
    {
        if (this.schematicPlacements.isEmpty())
        {
            return;
        }

        if (this.schematicsTouchingChunk.isEmpty() && !this.schematicPlacements.isEmpty())
        {
            this.schematicPlacements.forEach(
                    (schematicPlacement) ->
                            this.addTouchedChunksFor(schematicPlacement)
            );

            this.setVisibleSubChunksNeedsUpdate();
        }
    }

    private long getTickRateMs()
    {
        return this.tickRate * 1000L;
    }

    public void onClientTick(Minecraft mc)
    {
        long now = System.currentTimeMillis();

        if ((now - this.lastTick) > this.getTickRateMs())
        {
            final int offset = (mc.options.getEffectiveRenderDistance() / 2) + 1;

            if (this.hasTimeToExecuteMoreTasks() &&
                !PlacementManagerDaemonHandler.INSTANCE.hasAnyTasks())
            {
                this.checkNearbyChunksAreLoaded(mc, offset);
            }

            this.lastTick = now;
        }
    }

    // This ensures that chunks in the immediate vicinity
    // around the player are marked for loading every so often.
    private void checkNearbyChunksAreLoaded(Minecraft mc, final int offset)
    {
        if (mc.level == null) return;
        final ChunkPos cc = mc.getCameraEntity().chunkPosition();

        PlacementManagerDaemonHandler.INSTANCE.addTask(
                new PlacementManagerTaskOther(this.worldSupplier, cc.x, cc.z, () ->
                {
                    Set<ChunkPos> loaded = new HashSet<>();
                    Set<ChunkPos> notLoaded = new HashSet<>();

                    final int startcx = cc.x - offset;
                    final int startcz = cc.z - offset;
                    final int endcx = cc.x + offset;
                    final int endcz = cc.z + offset;

                    for (int cx = startcx; cx < endcx; cx++)
                    {
                        for (int cz = startcz; cz < endcz; cz++)
                        {
                            final ChunkPos cp = new ChunkPos(cx, cz);
                            final boolean isFar = cp.getChessboardDistance(cc) > 3;
                            // Don't unload nearby 9 chunks for Verifier

                            if (!this.worldSupplier.get().getChunkSource().hasChunk(cx, cz) &&
                                DataManager.getSchematicPlacementManager().canHandleChunk(Minecraft.getInstance().level, cx, cz))
                            {
                                notLoaded.add(cp);
                            }
                            else if (this.worldSupplier.get().getChunkSource().hasChunk(cx, cz))
                            {
                                if (this.worldSupplier.get().getChunkSource().getChunkState(cx, cz).atLeast(ChunkSchematicState.LOADED))
                                {
                                    if (isFar)
                                    {
                                        loaded.add(cp);
                                    }
                                }
                                else
                                {
                                    notLoaded.add(cp);
                                }
                            }
                        }
                    }

                    if (!loaded.isEmpty())
                    {
                        if (Reference.DEBUG_MODE)
                        {
                            Litematica.LOGGER.warn("FIXER: checking [{}] loaded chunks", loaded.size());
                        }

                        loaded.forEach(c ->
                                       {
                                           PlacementManagerDaemonHandler.INSTANCE.addTask(
                                                   new PlacementManagerTaskOther(this.worldSupplier, c.x, c.z, () ->
                                                   {
                                                       List<SchematicPlacement> placements = DataManager.getSchematicPlacementManager().getAllSchematicsTouchingChunk(c);
                                                       final boolean isFar = c.getChessboardDistance(cc) > 3;
                                                       // Don't unload nearby 9 chunks for Verifier

                                                       if (placements.isEmpty() && isFar)
                                                       {
                                                           DataManager.getSchematicPlacementManager().markChunkForUnload(c.x, c.z);
                                                       }
                                                       else
                                                       {
                                                           boolean unload = true;

                                                           for (SchematicPlacement s : placements)
                                                           {
                                                               if (s.isRenderingEnabled())
                                                               {
                                                                   unload = false;
                                                               }
                                                           }

                                                           if (unload && isFar)
                                                           {
                                                               DataManager.getSchematicPlacementManager().markChunkForUnload(c.x, c.z);
                                                           }
                                                       }
                                                   }));
                                       });
                    }

                    if (!notLoaded.isEmpty())
                    {
                        if (Reference.DEBUG_MODE)
                        {
                            Litematica.LOGGER.warn("FIXER: checking [{}] unloaded chunks", notLoaded.size());
                        }

                        notLoaded.forEach(c ->
                                          {
                                              PlacementManagerDaemonHandler.INSTANCE.addTask(
                                                      new PlacementManagerTaskOther(this.worldSupplier, c.x, c.z, () ->
                                                      {
                                                          List<SchematicPlacement> placements = DataManager.getSchematicPlacementManager().getAllSchematicsTouchingChunk(c);

                                                          // Load/Rebuild if Chunk is Near, no matter what for Verifier.
                                                          if (c.getChessboardDistance(cc) <= 3)
                                                          {
                                                              DataManager.getSchematicPlacementManager().markChunkForRebuild(c.x, c.z);
                                                          }
                                                          else
                                                          {
                                                              if (!placements.isEmpty())
                                                              {
                                                                  boolean rebuild = false;

                                                                  for (SchematicPlacement s : placements)
                                                                  {
                                                                      if (s.isRenderingEnabled())
                                                                      {
                                                                          rebuild = true;
                                                                      }
                                                                  }

                                                                  if (rebuild)
                                                                  {
                                                                      DataManager.getSchematicPlacementManager().markChunkForRebuild(c.x, c.z);
                                                                  }
                                                              }
                                                          }
                                                      }));
                                          });
                    }
            }));
    }

    public void onClientChunkLoad(int chunkX, int chunkZ)
    {
        this.markChunkForRebuild(chunkX, chunkZ);
    }

    public void onClientChunkUnload(int chunkX, int chunkZ)
    {
        if (Configs.Generic.LOAD_ENTIRE_SCHEMATICS.getBooleanValue() == false)
        {
            this.markChunkForUnload(chunkX, chunkZ);
        }
    }

    @Deprecated
    protected void unloadSchematicChunk(WorldSchematic worldSchematic, int chunkX, int chunkZ)
    {
        if (worldSchematic.getChunkSource().hasChunk(chunkX, chunkZ))
        {
            //System.out.printf("unloading chunk at %d, %d\n", chunkX, chunkZ);
            worldSchematic.unloadEntitiesByChunk(chunkX,chunkZ);
            worldSchematic.getChunkSource().unloadChunk(chunkX, chunkZ);
            worldSchematic.scheduleChunkRenders(chunkX, chunkZ);
            this.visibleChunksNeedsUpdate = true;
        }
    }

    public int getLastVisibleChunksCount()
    {
        return this.visibleChunks.size();
    }

    public List<ChunkPos> getAndUpdateVisibleChunks(ChunkPos viewChunk)
    {
        if (this.visibleChunksNeedsUpdate)
        {
            this.visibleChunks.clear();
            WorldSchematic worldSchematic = this.worldSupplier.get();
            LayerRange range = DataManager.getRenderLayerRange();

            if (worldSchematic != null)
            {
                int minY = worldSchematic.getMinY();
                int maxY = worldSchematic.getMaxY() - 1;

                ImmutableList<Long> keySet = worldSchematic.getChunkSource().getLoadedKeySet();

                for (long posLong : keySet)
                {
                    int minX = ChunkPos.getX(posLong) << 4;
                    int minZ = ChunkPos.getZ(posLong) << 4;
                    int maxX = minX + 15;
                    int maxZ = minZ + 15;

                    if (range.intersectsBox(minX, minY, minZ, maxX, maxY, maxZ))
                    {
                        ChunkPos pos = new ChunkPos(posLong);

                        if (worldSchematic.getChunkSource().getChunkState(pos.x, pos.z).atLeast(ChunkSchematicState.LOADED))
                        {
                            this.visibleChunks.add(pos);
                        }
                    }
                }

                this.visibleChunks.sort(new ChunkPosDistanceComparator(viewChunk));
                this.lastVisibleChunksSortPos = viewChunk;
            }

            this.visibleChunksNeedsUpdate = false;
        }
        else if (viewChunk.equals(this.lastVisibleChunksSortPos) == false)
        {
            this.visibleChunks.sort(new ChunkPosDistanceComparator(viewChunk));
            this.lastVisibleChunksSortPos = viewChunk;
        }

        return this.visibleChunks;
    }

    public List<SchematicPlacement> getAllSchematicsPlacements()
    {
        return this.schematicPlacements;
    }

    protected List<SchematicPlacement> getAllSchematicsTouchingChunk(ChunkPos pos)
    {
        return this.schematicsTouchingChunk.get(pos);
    }

    public List<PlacementPart> getPlacementPartsInChunk(int chunkX, int chunkZ)
    {
        return this.touchedVolumesInChunk.getOrDefault(ChunkPos.asLong(chunkX, chunkZ), Collections.emptyList());
    }

    public List<PlacementPart> getAllPlacementsTouchingChunk(BlockPos pos)
    {
        return this.touchedVolumesInChunk.getOrDefault(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4), Collections.emptyList());
    }

    public int getPlacementPartsInChunkCount(int chunkX, int chunkZ)
    {
        long longPos = ChunkPos.asLong(chunkX, chunkZ);

        if (this.touchedVolumesInChunk.containsKey(longPos))
        {
            return this.touchedVolumesInChunk.get(longPos).size();
        }

        return 0;
    }

    public int getTouchedChunksCount()
    {
        return this.touchedVolumesInChunk.size();
    }

    protected void onPlacementAdded()
    {
        StatusInfoRenderer.getInstance().startOverrideDelay();
    }

    public void addSchematicPlacement(SchematicPlacement placement, boolean printMessages)
    {
        if (this.schematicPlacements.contains(placement) == false)
        {
            this.schematicPlacements.add(placement);
            this.addTouchedChunksFor(placement);
            ((SchematicPlacementEventHandler) SchematicPlacementEventHandler.getInstance()).onPlacementAdded(placement);
            this.onPlacementAdded();

            if (this.selectedPlacement == null)
            {
                this.setSelectedSchematicPlacement(placement);
            }

            if (printMessages)
            {
                InfoUtils.showGuiMessage(MessageType.SUCCESS, StringUtils.translate("litematica.message.schematic_placement_created", placement.getName()));

                if (Configs.InfoOverlays.WARN_DISABLED_RENDERING.getBooleanValue())
                {
                    LayerMode mode = DataManager.getRenderLayerRange().getLayerMode();

                    if (mode != LayerMode.ALL)
                    {
                        InfoUtils.showGuiAndInGameMessage(MessageType.WARNING, "litematica.message.warn.layer_mode_currently_at", mode.getDisplayName());
                    }

                    if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() == false)
                    {
                        ConfigHotkey hotkey = Hotkeys.TOGGLE_ALL_RENDERING;
                        String configName = Configs.Visuals.ENABLE_RENDERING.getName();
                        String hotkeyName = hotkey.getName();
                        String hotkeyVal = hotkey.getKeybind().getKeysDisplayString();

                        InfoUtils.showGuiAndInGameMessage(MessageType.WARNING, 8000,
                                "litematica.message.warn.main_rendering_disabled", configName, hotkeyName, hotkeyVal);
                    }

                    if (Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() == false)
                    {
                        ConfigHotkey hotkey = Hotkeys.TOGGLE_SCHEMATIC_RENDERING;
                        String configName = Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getName();
                        String hotkeyName = hotkey.getName();
                        String hotkeyVal = hotkey.getKeybind().getKeysDisplayString();

                        InfoUtils.showGuiAndInGameMessage(MessageType.WARNING, 8000,
                                "litematica.message.warn.schematic_rendering_disabled", configName, hotkeyName, hotkeyVal);
                    }

                    if (Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue() == false)
                    {
                        ConfigHotkey hotkey = Hotkeys.TOGGLE_SCHEMATIC_BLOCK_RENDERING;
                        String configName = Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getName();
                        String hotkeyName = hotkey.getName();
                        String hotkeyVal = hotkey.getKeybind().getKeysDisplayString();

                        InfoUtils.showGuiAndInGameMessage(MessageType.WARNING, 8000,
                                "litematica.message.warn.schematic_blocks_rendering_disabled", configName, hotkeyName, hotkeyVal);
                    }
                }
            }
        }
        else if (printMessages)
        {
            InfoUtils.showGuiAndInGameMessage(MessageType.ERROR, "litematica.error.duplicate_schematic_placement");
        }
    }

    public boolean removeSchematicPlacement(SchematicPlacement placement)
    {
        return this.removeSchematicPlacement(placement, true);
    }

    public boolean removeSchematicPlacement(SchematicPlacement placement, boolean update)
    {
        if (this.selectedPlacement == placement)
        {
            this.setSelectedSchematicPlacement(null);
        }

        if (placement.hasVerifier())
        {
            placement.getSchematicVerifier().reset();
        }

        boolean ret = this.schematicPlacements.remove(placement);
        this.removeTouchedChunksFor(placement);

        if (ret)
        {
            placement.onRemoved();

            if (update)
            {
                this.onPlacementModified(placement);
            }
        }

        if (!this.schematicPlacements.isEmpty() && this.selectedPlacement == null)
        {
            this.setSelectedSchematicPlacement(this.schematicPlacements.getFirst());
        }

        return ret;
    }

    public List<SchematicPlacement> getAllPlacementsOfSchematic(LitematicaSchematic schematic)
    {
        List<SchematicPlacement> list = new ArrayList<>();

        for (SchematicPlacement placement : this.schematicPlacements)
        {
            if (placement.getSchematic() == schematic)
            {
                list.add(placement);
            }
        }

        return list;
    }

    public void removeAllPlacementsOfSchematic(LitematicaSchematic schematic)
    {
        boolean removed = false;

        for (int i = 0; i < this.schematicPlacements.size(); ++i)
        {
            SchematicPlacement placement = this.schematicPlacements.get(i);

            if (placement.getSchematic() == schematic)
            {
                if (placement.hasVerifier())
                {
                    placement.getSchematicVerifier().reset();
                }

                removed |= this.removeSchematicPlacement(placement, false);
                --i;
            }
        }

        if (removed)
        {
            OverlayRenderer.getInstance().updatePlacementCache();
        }
    }

    @Nullable
    public SchematicPlacement getSelectedSchematicPlacement()
    {
        return this.selectedPlacement;
    }

    public void setSelectedSchematicPlacement(@Nullable SchematicPlacement placement)
    {
        if (placement == null || this.schematicPlacements.contains(placement))
        {
            ((SchematicPlacementEventHandler) SchematicPlacementEventHandler.getInstance()).onPlacementSelected(this.selectedPlacement, placement);
            this.selectedPlacement = placement;
            OverlayRenderer.getInstance().updatePlacementCache();
            // Forget the last viewed material list when changing the placement selection
            DataManager.setMaterialList(null);
        }
    }

    protected void addTouchedChunksFor(SchematicPlacement placement)
    {
        if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
        {
            Set<ChunkPos> chunks = placement.getTouchedChunks();

            for (ChunkPos pos : chunks)
            {
                if (this.schematicsTouchingChunk.containsEntry(pos, placement) == false)
                {
                    this.schematicsTouchingChunk.put(pos, placement);
                    this.updateTouchedBoxesInChunk(pos);
                }

                PlacementManagerDaemonHandler.INSTANCE.removeUnloadTasksFor(pos.x, pos.z);
            }

            this.markChunksForRebuild(placement);
            this.onPlacementModified(placement);
        }
    }

    protected void removeTouchedChunksFor(SchematicPlacement placement)
    {
//        if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
//        {
            Set<ChunkPos> chunks = placement.getTouchedChunks();
            Set<ChunkPos> toUnload = new HashSet<>();

            for (ChunkPos pos : chunks)
            {
                this.schematicsTouchingChunk.remove(pos, placement);
                this.updateTouchedBoxesInChunk(pos);

                if (this.schematicsTouchingChunk.containsKey(pos) == false)
                {
                    toUnload.add(pos);
                }
            }

            this.markChunksForUnload(toUnload);
            this.markChunksForRebuild(chunks);
//        }
    }

    void onPrePlacementChange(SchematicPlacement placement)
    {
        this.chunksPreChange.clear();
        this.chunksPreChange.addAll(placement.getTouchedChunks());
    }

    void onPostPlacementChange(SchematicPlacement placement)
    {
        Set<ChunkPos> chunksPost = placement.getTouchedChunks();
        Set<ChunkPos> toRebuild = new HashSet<>(chunksPost);
        Set<ChunkPos> toUnload = new HashSet<>();

        //System.out.printf("chunkPre: %s - chunkPost: %s\n", this.chunksPreChange, chunksPost);
        this.chunksPreChange.removeAll(chunksPost);

        for (ChunkPos pos : this.chunksPreChange)
        {
            this.schematicsTouchingChunk.remove(pos, placement);
            this.updateTouchedBoxesInChunk(pos);
            //System.out.printf("removing placement from: %s\n", pos);

            if (this.schematicsTouchingChunk.containsKey(pos) == false)
            {
                //System.out.printf("unloading: %s\n", pos);
                toUnload.add(pos);
            }
            else
            {
                //System.out.printf("rebuilding: %s\n", pos);
                toRebuild.add(pos);
            }
        }

        this.markChunksForUnload(toUnload);
        this.markChunksForRebuild(toRebuild);

        for (ChunkPos pos : chunksPost)
        {
            if (this.schematicsTouchingChunk.containsEntry(pos, placement) == false)
            {
                this.schematicsTouchingChunk.put(pos, placement);
            }

            this.updateTouchedBoxesInChunk(pos);
        }

        this.onPlacementModified(placement);
    }

    protected void updateTouchedBoxesInChunk(ChunkPos pos)
    {
        long chunkPosLong = pos.toLong();
        this.touchedVolumesInChunk.remove(chunkPosLong);

        Collection<SchematicPlacement> placements = this.schematicsTouchingChunk.get(pos);

        if (placements.isEmpty() == false)
        {
            for (SchematicPlacement placement : placements)
            {
                if (placement.matchesRequirement(RequiredEnabled.RENDERING_ENABLED) == false)
                {
                    continue;
                }

                Map<String, IntBoundingBox> boxMap = placement.getBoxesWithinChunk(pos.x, pos.z);

                if (boxMap.isEmpty() == false)
                {
                    List<PlacementPart> list = this.touchedVolumesInChunk.computeIfAbsent(chunkPosLong, p -> new ArrayList<>());

                    for (Map.Entry<String, IntBoundingBox> entry : boxMap.entrySet())
                    {
                        list.add(new PlacementPart(placement, entry.getKey(), entry.getValue()));
                    }
                }
            }
        }
    }

    public void markAllPlacementsOfSchematicForRebuild(LitematicaSchematic schematic)
    {
        for (SchematicPlacement placement : this.schematicPlacements)
        {
            if (placement.getSchematic() == schematic)
            {
                this.markChunksForRebuild(placement);
            }
        }
    }

    public void markChunksForRebuild(SchematicPlacement placement)
    {
        if (placement.matchesRequirement(RequiredEnabled.PLACEMENT_ENABLED))
        {
            this.markChunksForRebuild(placement.getTouchedChunks());
        }
    }

    public void markChunkForUnload(ChunkPos pos)
    {
        this.markChunkForUnload(pos.x, pos.z);
    }

    private void markChunksForUnload(Collection<ChunkPos> chunks)
    {
        for (ChunkPos pos : chunks)
        {
            this.markChunkForUnload(pos);
        }
    }

    public void markChunkForUnload(int cx, int cz)
    {
        PlacementManagerDaemonHandler.INSTANCE.removeAllTasksFor(cx, cz);
        PlacementManagerDaemonHandler.INSTANCE.addTask(new PlacementManagerTaskUnload(this.worldSupplier, cx, cz));
    }

    private void markChunksForRebuild(Collection<ChunkPos> chunks)
    {
//        System.out.printf("rebuilding %d chunks: %s\n", chunks.size(), chunks);
        chunks.forEach(
                pos ->
                {
                    this.markChunkForRebuild(pos);
                }
        );
    }

    public void markChunkForRebuild(ChunkPos pos)
    {
        this.markChunkForRebuild(pos.x, pos.z);
    }

    public void markChunkForRebuild(int cx, int cz)
    {
        PlacementManagerDaemonHandler.INSTANCE.removeAllTasksFor(cx, cz);
        PlacementManagerDaemonHandler.INSTANCE.addTask(new PlacementManagerTaskRebuild(this.worldSupplier, cx, cz));
    }

    protected void onPlacementModified(SchematicPlacement placement)
    {
        ((SchematicPlacementEventHandler) SchematicPlacementEventHandler.getInstance()).onPlacementUpdated(placement);

        if (placement.isEnabled())
        {
            OverlayRenderer.getInstance().updatePlacementCache();
        }
    }

    public boolean changeSelection(Level world, Entity entity, int maxDistance)
    {
        if (this.schematicPlacements.size() > 0)
        {
            RayTraceWrapper trace = RayTraceUtils.getWrappedRayTraceFromEntity(world, entity, maxDistance);

            SchematicPlacement placement = this.getSelectedSchematicPlacement();

            if (placement != null)
            {
                placement.setSelectedSubRegionName(null);
            }

            if (trace.getHitType() == HitType.PLACEMENT_SUBREGION || trace.getHitType() == HitType.PLACEMENT_ORIGIN)
            {
                this.setSelectedSchematicPlacement(trace.getHitSchematicPlacement());

                boolean selectSubRegion = Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld();
                String subRegionName = selectSubRegion ? trace.getHitSchematicPlacementRegionName() : null;
                this.getSelectedSchematicPlacement().setSelectedSubRegionName(subRegionName);

                return true;
            }
            else if (trace.getHitType() == HitType.MISS)
            {
                this.setSelectedSchematicPlacement(null);
                return true;
            }
        }

        return false;
    }

    public void setPositionOfCurrentSelectionToRayTrace(Minecraft mc, double maxDistance)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            HitResult trace = RayTraceUtils.getRayTraceFromEntity(mc.level, entity, false, maxDistance);

            if (trace.getType() != HitResult.Type.BLOCK)
            {
                return;
            }

            BlockPos pos = ((BlockHitResult) trace).getBlockPos();

            // Sneaking puts the position inside the targeted block, not sneaking puts it against the targeted face
            if (mc.player.isShiftKeyDown() == false)
            {
                pos = pos.relative(((BlockHitResult) trace).getDirection());
            }

            this.setPositionOfCurrentSelectionTo(pos, mc);
        }
    }

    public void setPositionOfCurrentSelectionTo(BlockPos pos, Minecraft mc)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            if (schematicPlacement.isLocked())
            {
                InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
                return;
            }

            boolean movingBox = schematicPlacement.getSelectedSubRegionPlacement() != null;

            if (movingBox)
            {
                schematicPlacement.moveSubRegionTo(schematicPlacement.getSelectedSubRegionName(), pos, InfoUtils.INFO_MESSAGE_CONSUMER);

                String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.placement.moved_subregion_to", posStr);
            }
            // Moving the origin point
            else
            {
                BlockPos old = schematicPlacement.getOrigin();
                schematicPlacement.setOrigin(pos, InfoUtils.INFO_MESSAGE_CONSUMER);

                if (old.equals(schematicPlacement.getOrigin()) == false)
                {
                    String posStrOld = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
                    String posStrNew = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.placement.moved_placement_origin", posStrOld, posStrNew);
                }
            }
        }
    }

    public void nudgePositionOfCurrentSelection(Direction direction, int amount)
    {
        SchematicPlacement schematicPlacement = this.getSelectedSchematicPlacement();

        if (schematicPlacement != null)
        {
            if (schematicPlacement.isLocked())
            {
                InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.placement.cant_modify_is_locked");
                return;
            }

            SubRegionPlacement placement = schematicPlacement.getSelectedSubRegionPlacement();

            // Moving a sub-region
            if (placement != null)
            {
                // getPos returns a relative position, but moveSubRegionTo takes an absolute position...
                BlockPos old = PositionUtils.getTransformedBlockPos(placement.getPos(), schematicPlacement.getMirror(), schematicPlacement.getRotation());
                old = old.offset(schematicPlacement.getOrigin());

                schematicPlacement.moveSubRegionTo(placement.getName(), old.relative(direction, amount), InfoUtils.INFO_MESSAGE_CONSUMER);
            }
            // Moving the origin point
            else
            {
                BlockPos old = schematicPlacement.getOrigin();
                schematicPlacement.setOrigin(old.relative(direction, amount), InfoUtils.INFO_MESSAGE_CONSUMER);
            }
        }
    }

    public void pasteCurrentPlacementToWorld(Minecraft mc)
    {
        this.pastePlacementToWorld(this.getSelectedSchematicPlacement(), mc);
    }

    public void pastePlacementToWorld(final SchematicPlacement schematicPlacement, Minecraft mc)
    {
        this.pastePlacementToWorld(schematicPlacement, true, mc);
    }

    public void pastePlacementToWorld(final SchematicPlacement schematicPlacement, boolean changedBlocksOnly, Minecraft mc)
    {
        this.pastePlacementToWorld(schematicPlacement, changedBlocksOnly, true, mc);
    }

    public void displayChunkDebugCmd(int cx, int cz, ChatComponent chat)
    {
        if (this.worldSupplier.get() != null)
        {
            ChunkSchematic chunk = this.worldSupplier.get().getChunkSource().getChunkIfExists(cx, cz);

            if (chunk != null)
            {
                final int entCount = this.worldSupplier.get().getEntitiesByChunk(cx, cz, EntityUtils.NOT_PLAYER).size();
                final int teCount = chunk.getTileEntityCount();
                final int sectCount = chunk.getSectionsCount();
                final int height = chunk.getHeight();
                final int minY = chunk.getMinY();
                final long timeCreated = chunk.getTimeCreated();
                final int schemCount = this.getAllSchematicsTouchingChunk(new ChunkPos(cx, cz)).size();
                final int partsCount = this.getPlacementPartsInChunkCount(cx, cz);
                final boolean tasks = PlacementManagerDaemonHandler.INSTANCE.hasAnyTasksFor(cx, cz);

                chat.addMessage(
                        StringUtils.translateAsText(PmCommand.PREFIX+".display_chunk_debug.base", cx, cz, chunk.getState(), timeCreated)
                );
                chat.addMessage(
                        StringUtils.translateAsText(PmCommand.PREFIX+".display_chunk_debug.entities", entCount, teCount)
                );
                chat.addMessage(
                        StringUtils.translateAsText(PmCommand.PREFIX+".display_chunk_debug.sections", sectCount, height, minY)
                );
                chat.addMessage(
                        StringUtils.translateAsText(PmCommand.PREFIX+".display_chunk_debug.schematics", schemCount, partsCount, tasks)
                );
            }
            else
            {
                chat.addMessage(
                        StringUtils.translateAsText(PmCommand.PREFIX+".display_chunk_debug.not_loaded", cx, cz)
                );
            }
        }
    }

    public void markChunkForRebuildCmd(int cx, int cz, ChatComponent chat)
    {
        if (this.worldSupplier.get() != null)
        {
            chat.addMessage(
                    StringUtils.translateAsText(PmCommand.PREFIX+".mark_chunk_for_rebuild", cx, cz)
            );

            this.markChunkForRebuild(cx, cz);
        }
    }

    private static class PasteToCommandsListener implements IConfirmationListener
    {
        private final SchematicPlacement schematicPlacement;
        private final boolean changedBlocksOnly;

        public PasteToCommandsListener(SchematicPlacement schematicPlacement, boolean changedBlocksOnly)
        {
            this.schematicPlacement = schematicPlacement;
            this.changedBlocksOnly = changedBlocksOnly;
        }

        @Override
        public boolean onActionConfirmed()
        {
            LayerRange range = DataManager.getRenderLayerRange();
            TaskPasteSchematicSetblockToMcfunction task = new TaskPasteSchematicSetblockToMcfunction(Collections.singletonList(this.schematicPlacement), range, this.changedBlocksOnly);
            TaskScheduler.getInstanceClient().scheduleTask(task, 1);
            return true;
        }

        @Override
        public boolean onActionCancelled()
        {
            return true;
        }
    }

    public void pastePlacementToWorld(final SchematicPlacement schematicPlacement, boolean changedBlocksOnly, boolean printMessage, Minecraft mc)
    {
        if (mc.player != null && EntityUtils.isCreativeMode(mc.player))
        {
            if (schematicPlacement != null)
            {
                /*
                if (PositionUtils.isPlacementWithinWorld(mc.world, schematicPlacement, false) == false)
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.placement_paste_outside_world");
                    return;
                }
                */
                LayerRange range = DataManager.getRenderLayerRange();

                if (Configs.Generic.PASTE_TO_MCFUNCTION.getBooleanValue())
                {
                    PasteToCommandsListener cl = new PasteToCommandsListener(schematicPlacement, changedBlocksOnly);
                    GuiConfirmAction screen = new GuiConfirmAction(320, "Confirm paste to command files", cl, null, "Are you sure you want to paste the current placement as setblock commands into command/mcfunction files?");
                    GuiBase.openGui(screen);
                }
                else if (mc.hasSingleplayerServer() == false || Configs.Generic.PASTE_USING_COMMANDS_IN_SP.getBooleanValue())
                {
                    if (EntitiesDataStorage.getInstance().hasServuxServer() &&
                        Configs.Generic.PASTE_USING_SERVUX.getBooleanValue())
                    {
                        Litematica.debugLog("Found a Servux server, I am sending the Schematic Placement to it.");
                        InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.paste_with_servux");
                        CompoundTag nbt = schematicPlacement.toNbt(true);
                        final int maxSize = PacketSplitter.DEFAULT_MAX_RECEIVE_SIZE_S2C - 4096;

                        // Slice Extra-large schematics... :(
//                        if (Configs.Generic.PASTE_SERVUX_EXPERIMENTAL.getBooleanValue())
                        if (nbt.sizeInBytes() > maxSize)
                        {
                            Litematica.LOGGER.warn("[Servux Paste]: Slicing Oversided Schematic for Servux Paste ...");
                            this.sliceForServux(schematicPlacement.getSchematic(), nbt, maxSize, printMessage);
                        }
                        else
                        {
                            nbt.putString("Task", "LitematicaPaste");
                            ServuxLitematicaHandler.getInstance().encodeClientData(ServuxLitematicaPacket.ResponseC2SStart(nbt));
                        }
                    }
                    else
                    {
                        TaskPasteSchematicPerChunkBase task = new TaskPasteSchematicPerChunkCommand(Collections.singletonList(schematicPlacement), range, changedBlocksOnly);
                        TaskScheduler.getInstanceClient().scheduleTask(task, Configs.Generic.COMMAND_TASK_INTERVAL.getIntegerValue());

                        if (printMessage)
                        {
                            InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
                        }
                    }
                }
                else if (mc.hasSingleplayerServer())
                {
                    TaskPasteSchematicPerChunkBase task = new TaskPasteSchematicPerChunkDirect(Collections.singletonList(schematicPlacement), range, changedBlocksOnly);
                    TaskScheduler.getInstanceServer().scheduleTask(task, Configs.Generic.COMMAND_TASK_INTERVAL.getIntegerValue());

                    if (printMessage)
                    {
                        InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.scheduled_task_added");
                    }
                }
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.message.error.no_placement_selected");
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.generic.creative_mode_only");
        }
    }

    // Attempt to slice the schematic if oversized, and transmit it as a file.
    private void sliceForServux(LitematicaSchematic litematic, CompoundTag nbt, final int maxSize, boolean printMessage)
    {
        final long sessionKey = RandomSource.create(Util.getMillis()).nextLong();
        nbt.remove("Schematics");
        litematic.sendTransmitFile(nbt, sessionKey, printMessage);
    }

    public void clear()
    {
        PlacementManagerDaemonHandler.INSTANCE.clearAllTasks();
        this.schematicPlacements.clear();
        this.selectedPlacement = null;
        this.schematicsTouchingChunk.clear();
        this.touchedVolumesInChunk.clear();
        this.chunksPreChange.clear();
        this.visibleChunks.clear();

        SchematicHolder.getInstance().clearLoadedSchematics();
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.schematicPlacements.size() > 0)
        {
            JsonArray arr = new JsonArray();
            int selectedIndex = 0;
            boolean indexValid = false;

            for (SchematicPlacement placement : this.schematicPlacements)
            {
                if (placement.shouldBeSaved() == false)
                {
                    continue;
                }

                JsonObject objPlacement = placement.toJson();

                if (objPlacement != null)
                {
                    arr.add(objPlacement);

                    if (this.selectedPlacement == placement)
                    {
                        indexValid = true;
                    }
                    else if (indexValid == false)
                    {
                        selectedIndex++;
                    }
                }
            }

            obj.add("placements", arr);

            if (indexValid)
            {
                obj.add("selected", new JsonPrimitive(selectedIndex));
                obj.add("origin_selected", new JsonPrimitive(true));
            }
        }

        return obj;
    }

    public void loadFromJson(JsonObject obj)
    {
        this.clear();

        if (JsonUtils.hasArray(obj, "placements"))
        {
            JsonArray arr = obj.get("placements").getAsJsonArray();
            int index = JsonUtils.hasInteger(obj, "selected") ? obj.get("selected").getAsInt() : -1;
            final int size = arr.size();

            for (int i = 0; i < size; ++i)
            {
                JsonElement el = arr.get(i);

                if (el.isJsonObject())
                {
                    SchematicPlacement placement = SchematicPlacement.fromJson(el.getAsJsonObject());

                    if (placement != null)
                    {
                        this.addSchematicPlacement(placement, false);
                    }
                }
                else
                {
                    // Invalid data in the array, don't select an entry
                    index = -1;
                }
            }

            if (index >= 0 && index < this.schematicPlacements.size())
            {
                this.selectedPlacement = this.schematicPlacements.get(index);
            }
        }

        OverlayRenderer.getInstance().updatePlacementCache();
    }

    public static class PlacementPart
    {
        public final SchematicPlacement placement;
        public final String subRegionName;
        public final IntBoundingBox bb;

        public PlacementPart(SchematicPlacement placement, String subRegionName, IntBoundingBox bb)
        {
            this.placement = placement;
            this.subRegionName = subRegionName;
            this.bb = bb;
        }

        public SchematicPlacement getPlacement()
        {
            return this.placement;
        }

        public String getSubRegionName()
        {
            return this.subRegionName;
        }

        public IntBoundingBox getBox()
        {
            return this.bb;
        }
    }
}
