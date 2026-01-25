package fi.dy.masa.litematica.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

import fi.dy.masa.malilib.util.WorldUtils;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.render.IWorldSchematicRenderer;

public class WorldSchematic extends Level
{
    protected static final ResourceKey<Level> REGISTRY_KEY = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Reference.MOD_ID, "schematic_world"));

    protected final Minecraft mc;
    protected final ChunkManagerSchematic chunkManagerSchematic;
    @Nullable protected final IWorldSchematicRenderer worldRenderer;
    private final TickRateManager tickManager;
    private final Holder<DimensionType> dimensionType;
    private final SchematicEntityLookup<Entity> entityLookup;
    protected Holder<Biome> biome;
    private LevelData.RespawnData properties;
    protected int nextEntityId;

    public WorldSchematic(WritableLevelData properties,
                          @Nonnull RegistryAccess registryManager,
                          Holder<DimensionType> dimension,
                          @Nullable IWorldSchematicRenderer worldRenderer)
    {
        super(properties, REGISTRY_KEY, !registryManager.equals(RegistryAccess.EMPTY) ? registryManager : SchematicWorldHandler.INSTANCE.getRegistryManager(), dimension, true, false, 0L, 0);

        this.mc = Minecraft.getInstance();

        if (this.mc == null || this.mc.level == null)
        {
            throw new RuntimeException("WorldSchematic invoked when MinecraftClient.getInstance() or mc.world is null");
        }

        this.worldRenderer = worldRenderer;
        this.chunkManagerSchematic = new ChunkManagerSchematic(this);
        this.dimensionType = dimension;

        if (!registryManager.equals(RegistryAccess.EMPTY))
        {
            this.setDimension(registryManager);
        }
        else
        {
            this.setDimension(this.mc.level.registryAccess());
        }

        this.tickManager = new TickRateManager();
        this.entityLookup = new SchematicEntityLookup<>();
        this.properties = LevelData.RespawnData.DEFAULT;
    }

    @Override
    public String toString()
    {
        return "SchematicWorld["+REGISTRY_KEY.identifier().toString()+"]";
    }

    private void setDimension(RegistryAccess registryManager)
    {
        registryManager.lookup(Registries.DIMENSION_TYPE).ifPresent(entryLookup -> {
            Holder<DimensionType> nether = entryLookup.get(BuiltinDimensionTypes.NETHER).orElse(null);
            Holder<DimensionType> end = entryLookup.get(BuiltinDimensionTypes.END).orElse(null);
    
            if (nether != null && this.dimensionType.equals(nether))
            {
                this.biome = WorldUtils.getWastes(registryManager);
            }
            else if (end != null && this.dimensionType.equals(end))
            {
                this.biome = WorldUtils.getTheEnd(registryManager);
            }
            else
            {
                this.biome = WorldUtils.getPlains(registryManager);
            }
        });
    
//        this.dimensionEffects = DimensionEffects.byDimensionType(this.dimensionType.value());
    }

    @Deprecated(forRemoval = true)
    public ChunkManagerSchematic getChunkProvider()
    {
        return this.getChunkSource();
    }

    @Override
    public @Nonnull ChunkManagerSchematic getChunkSource()
    {
        return this.chunkManagerSchematic;
    }

    @Override
    public @Nonnull TickRateManager tickRateManager()
    {
        return this.tickManager;
    }

    @Nullable
    @Override
    public MapItemSavedData getMapData(@NonNull MapId id) { return null; }

    @Override
    public @Nonnull LevelTickAccess<Block> getBlockTicks()
    {
        return BlackholeTickAccess.emptyLevelList();
    }

    @Override
    public @Nonnull LevelTickAccess<Fluid> getFluidTicks()
    {
        return BlackholeTickAccess.emptyLevelList();
    }

    public int getRegularEntityCount()
    {
        return this.entityLookup.size();
    }

    public String getEntityDebug()
    {
        return String.format("%s", this.entityLookup.getDebugString());
    }

    @Override
    public @Nonnull LevelChunk getChunkAt(BlockPos pos)
    {
        return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    @Override
    public @Nonnull ChunkSchematic getChunk(int chunkX, int chunkZ)
    {
        return this.chunkManagerSchematic.getChunkForLighting(chunkX, chunkZ);
    }

    @Override
    public ChunkAccess getChunk(int chunkX, int chunkZ, @Nonnull ChunkStatus status, boolean required)
    {
        return this.getChunk(chunkX, chunkZ);
    }

    @Override
    public @Nonnull Holder<Biome> getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ)
    {
        return this.biome;
    }

    @Override
    public int getSeaLevel()
    {
        if (this.mc != null && this.mc.level != null)
        {
            return this.mc.level.getSeaLevel();
        }

        return 0;
    }

    @Override
    public boolean setBlock(BlockPos pos, @Nonnull BlockState newState, int flags)
    {
        if (pos.getY() < this.getMinY() || pos.getY() >= this.getMaxY())
        {
            return false;
        }
        else
        {
            return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4).setBlockState(pos, newState, 3) != null;
        }
    }

    @Override
    public boolean addFreshEntity(Entity entity)
    {
        int chunkX = Mth.floor(entity.getX() / 16.0D);
        int chunkZ = Mth.floor(entity.getZ() / 16.0D);

        if (this.entityLookup.contains(entity.getUUID()))
        {
            Entity e = this.entityLookup.get(entity.getUUID());

            if (e != null && e.getType().equals(entity.getType()))
            {
                if (e.position().equals(entity.position()))
                {
                    return false;
                }

                this.entityLookup.remove(entity.getUUID());
            }
            else
            {
                return false;
            }
        }

        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

        if (this.entityLookup.contains(entity.getId()) ||
            entity.getId() < 0)
        {
            entity.setId(this.nextEntityId++);
        }

        this.entityLookup.put(entity, chunkPos);
        return true;
    }

    public void unloadEntitiesByChunk(int chunkX, int chunkZ)
    {
        if (!this.hasChunk(chunkX, chunkZ))
        {
            return;
        }

        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        int count = this.entityLookup.removeByChunk(pos);

        this.checkForStaleEntities();
    }

    private void checkForStaleEntities()
    {
        if (this.entityLookup.size() < 1)
        {
            this.entityLookup.reset();
            this.nextEntityId = 0;
        }
    }

    @Nullable
    @Override
    public Entity getEntity(int id)
    {
        return this.getEntities().get(id);
    }

    @Nullable
    @Override
    public Entity getEntity(@Nonnull UUID uuid)
    {
        return this.getEntities().get(uuid);
    }

    protected void closeEntityLookup() throws Exception
    {
        this.entityLookup.close();
    }

    public void clearEntities()
    {
        try
        {
            this.closeEntityLookup();
        }
        catch (Exception ignored) { }

        this.nextEntityId = 0;
    }

    @Override
    public @Nonnull Collection<EnderDragonPart> dragonParts()
    {
        return List.of();
    }

    @Override
    public @Nonnull List<? extends Player> players()
    {
        return ImmutableList.of();
    }

    @Override
    public long getGameTime()
    {
        return this.mc.level != null ? this.mc.level.getGameTime() : 0;
    }

    @Override
    public @Nonnull Scoreboard getScoreboard()
    {
        return this.mc.level != null ? this.mc.level.getScoreboard() : null;
    }

    @Override
    public @Nonnull RecipeAccess recipeAccess()
    {
        return this.mc.level != null ? this.mc.level.recipeAccess() : null;
    }

    @Override
    protected @Nonnull LevelEntityGetter<Entity> getEntities()
    {
        return this.entityLookup;
    }

    public ImmutableList<Entity> getEntitiesByChunk(int cx, int cz, @Nonnull Predicate<? super Entity> predicate)
    {
        if (!this.hasChunk(cx, cz))
        {
            return ImmutableList.of();
        }

        ImmutableList.Builder<Entity> builder = ImmutableList.builder();

	    for (Entity e : this.entityLookup.getAllByChunk(new ChunkPos(cx, cz)))
	    {
		    if (e != null && predicate.test(e))
		    {
			    builder.add(e);
		    }
	    }

        return builder.build();
    }

    @Override
    public @Nonnull List<Entity> getEntities(@Nullable final Entity except, @Nonnull final AABB box, @Nonnull Predicate<? super Entity> predicate)
    {
        final List<Entity> list = new ArrayList<>();

        this.getEntities().get(box, e ->
        {
            if (e != except && predicate.test(e))
            {
                list.add(e);
            }
        });

        return list;
    }

    @Override
    public @Nonnull <T extends Entity> List<T> getEntities(@Nonnull EntityTypeTest<Entity, T> arg, @Nonnull AABB box, @Nonnull Predicate<? super T> predicate)
    {
        ArrayList<T> list = new ArrayList<>();

        for (Entity e : this.getEntities(arg, box, e -> true))
        {
            T t = arg.tryCast(e);

            if (t != null && predicate.test(t))
            {
                list.add(t);
            }
        }

        return list;
    }

    public List<ChunkSchematic> getChunksWithinBox(AABB box)
    {
        final int minX = Mth.floor(box.minX / 16.0);
        final int minZ = Mth.floor(box.minZ / 16.0);
        final int maxX = Mth.floor(box.maxX / 16.0);
        final int maxZ = Mth.floor(box.maxZ / 16.0);

        List<ChunkSchematic> chunks = new ArrayList<>();

        for (int cx = minX; cx <= maxX; ++cx)
        {
            for (int cz = minZ; cz <= maxZ; ++cz)
            {
                ChunkSchematic chunk = this.chunkManagerSchematic.getChunkIfExists(cx, cz);

                if (chunk != null)
                {
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    @Override
    public void setBlocksDirty(@Nonnull BlockPos pos, @Nonnull BlockState stateOld, @Nonnull BlockState stateNew)
    {
        if (stateNew != stateOld)
        {
            this.scheduleChunkRenders(pos.getX() >> 4, pos.getZ() >> 4, true);
        }
    }

    @Override
    public void playSeededSound(@Nullable Entity source, double x, double y, double z, @Nonnull Holder<SoundEvent> sound, @Nonnull SoundSource category, float volume, float pitch, long seed)
    {
        // NO-OP
    }

    @Override
    public void playSeededSound(@Nullable Entity source, @Nonnull Entity entity, @Nonnull Holder<SoundEvent> sound, @Nonnull SoundSource category, float volume, float pitch, long seed)
    {
        // NO-OP
    }

	@Override
	public void explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator behavior,
	                    double x, double y, double z, float power, boolean createFire,
	                    @Nonnull ExplosionInteraction explosionSourceType, @Nonnull ParticleOptions smallParticle, @Nonnull ParticleOptions largeParticle,
	                    @Nonnull WeightedList<ExplosionParticleInfo> blockParticles, @Nonnull Holder<SoundEvent> soundEvent)
	{
		// NO-OP
	}

    public void scheduleChunkRenders(int chunkX, int chunkZ)
    {
        this.scheduleChunkRenders(chunkX, chunkZ, false);
    }

	public void scheduleChunkRenders(int chunkX, int chunkZ, boolean immediate)
    {
        if (this.worldRenderer != null)
        {
            this.worldRenderer.scheduleChunkRenders(chunkX, chunkZ, immediate);
        }
    }

    @Override
    public int getMinY()
    {
        return this.mc.level != null ? this.mc.level.getMinY() : -64;
    }

    @Override
    public int getHeight()
    {
        return this.mc.level != null ? this.mc.level.getHeight() : 384;
    }

    // The following HeightLimitView overrides are to work around an incompatibility with Lithium 0.7.4+

    @Override
    public int getMaxY()
    {
        return this.getMinY() + this.getHeight();
    }

    @Override
    public int getMinSectionY()
    {
        return this.getMinY() >> 4;
    }

    @Override
    public int getMaxSectionY()
    {
        return this.getMaxY() >> 4;
    }

    @Override
    public int getSectionsCount()
    {
        return this.getMaxSectionY() - this.getMinSectionY();
    }

    @Override
    public boolean isOutsideBuildHeight(BlockPos pos)
    {
        return this.isOutsideBuildHeight(pos.getY());
    }

    @Override
    public boolean isOutsideBuildHeight(int y)
    {
        return (y < this.getMinY()) || (y >= this.getMaxY());
    }

    @Override
    public int getSectionIndex(int y)
    {
        return (y >> 4) - (this.getMinY() >> 4);
    }

    @Override
    public int getSectionIndexFromSectionY(int coord)
    {
        return coord - (this.getMinY() >> 4);
    }

    @Override
    public int getSectionYFromSectionIndex(int index)
    {
        return index + (this.getMinY() >> 4);
    }

    // For AO compatibility
    public Holder<DimensionType> getDimensionType()
    {
        return this.dimensionType;
    }

    @Override
    public float getShade(@Nonnull Direction direction, boolean shaded)
    {
	    DimensionType.CardinalLightType cardinalLightType = this.dimensionType().cardinalLightType();

        if (!shaded)
        {
            return cardinalLightType == DimensionType.CardinalLightType.NETHER ? 0.9F : 1.0F;
        }
        else
        {
            return switch (direction)
            {
	            case DOWN -> cardinalLightType == DimensionType.CardinalLightType.NETHER ? 0.9F : 0.5F;
	            case UP -> cardinalLightType == DimensionType.CardinalLightType.NETHER ? 0.9F : 1.0F;
	            case NORTH, SOUTH -> 0.8F;
	            case WEST, EAST -> 0.6F;
            };
        }
    }

    @Override
    public @Nonnull LevelLightEngine getLightEngine()
    {
        // Returns the Fake Lighting Provider, if configured to do so
        return this.getChunkSource().getLightEngine();
    }

    @Override
    public void sendBlockUpdated(@Nonnull BlockPos blockPos_1, @Nonnull BlockState blockState_1, @Nonnull BlockState blockState_2, int flags)
    {
        // NO-OP
    }

    @Override
    public void destroyBlockProgress(int entityId, @Nonnull BlockPos pos, int progress)
    {
        // NO-OP
    }

    @Override
    public void globalLevelEvent(int eventId, @Nonnull BlockPos pos, int data)
    {
        // NO-OP
    }
    
    @Override
    public void gameEvent(@Nonnull Holder<GameEvent> event, @Nonnull Vec3 emitterPos, @Nonnull GameEvent.Context emitter)
    {
        // NO-OP
    }

    @Override
    public void levelEvent(@Nullable Entity entity, int eventId, @Nonnull BlockPos pos, int data)
    {
        // NO-OP
    }

    @Override
    public @Nonnull RegistryAccess registryAccess()
    {
        if (this.mc != null && this.mc.level != null)
        {
            return this.mc.level.registryAccess();
        }
        else if (!SchematicWorldHandler.INSTANCE.getRegistryManager().equals(RegistryAccess.EMPTY))
        {
            return SchematicWorldHandler.INSTANCE.getRegistryManager();
        }
        else
        {
            return RegistryAccess.EMPTY;
        }
    }

	@Override
	public @NonNull EnvironmentAttributeSystem environmentAttributes()
	{
		if (this.mc != null && this.mc.level != null)
		{
			return this.mc.level.environmentAttributes();
		}

		return null;
	}

	@Override
    public @Nonnull PotionBrewing potionBrewing()
    {
        if (this.mc != null && this.mc.level != null)
        {
            return this.mc.level.potionBrewing();
        }
        else
        {
            return PotionBrewing.EMPTY;
        }
    }

    @Override
    public @Nonnull FuelValues fuelValues()
    {
        if (this.mc != null && this.mc.level != null)
        {
            return this.mc.level.fuelValues();
        }
        else
        {
            return null;
        }
    }

    @Override
    public @Nonnull FeatureFlagSet enabledFeatures()
    {
        if (this.mc != null && this.mc.level != null)
        {
            return this.mc.level.enabledFeatures();
        }
        else
        {
            return FeatureFlagSet.of();
        }
    }

    @Override
    public @Nonnull String gatherChunkSourceStats()
    {
        // +"/"+ this.entityMap.size()
        return "Chunks[SCH] W: "+this.getChunkSource().gatherStats()+" E: "+this.getRegularEntityCount()+" (eL: "+this.entityLookup.size()+")";
    }

    @Override
    public void setRespawnData(LevelData.RespawnData arg)
    {
        this.properties = new LevelData.RespawnData(arg.globalPos(), arg.pitch(), arg.yaw());
    }

    @Override
    public @Nonnull LevelData.RespawnData getRespawnData()
    {
        return this.properties;
    }

    @Override
    public void gameEvent(@Nullable Entity entity, @Nonnull Holder<GameEvent> event, @Nonnull Vec3 pos)
    {
        // NO-OP
    }

    @Override
    public void gameEvent(@Nullable Entity entity, @Nonnull Holder<GameEvent> event, @Nonnull BlockPos pos)
    {
        // NO-OP
    }

    @Override
    public void gameEvent(@Nonnull ResourceKey<GameEvent> event, @Nonnull BlockPos pos, @Nullable GameEvent.Context emitter)
    {
        // NO-OP
    }

	@Override
	public @Nonnull WorldBorder getWorldBorder()
	{
		return new WorldBorder();
	}
}
