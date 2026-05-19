package fi.dy.masa.litematica.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import com.google.common.collect.Iterables;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

/**
 * Vanilla-compatible Entity Storage
 * @param <T> ()
 */
public class SchematicEntityLookup<T extends EntityAccess> implements LevelEntityGetter<T>, AutoCloseable
{
    private final ConcurrentHashMap<Integer, T> entityMap;
    private final ConcurrentHashMap<UUID, Integer> uuidMap;
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<UUID>> chunkMap;

    protected SchematicEntityLookup()
    {
        this.entityMap = new ConcurrentHashMap<>(256, 0.9f, 1);
        this.uuidMap = new ConcurrentHashMap<>(256, 0.9f, 1);
        this.chunkMap = new ConcurrentHashMap<>(256, 0.9f, 1);
    }

    protected String getDebugString()
    {
        return String.format("E: %02d, U: %02d, C: %02d",
                             this.entityMap.size(), this.uuidMap.size(), this.chunkMap.size()
        );
    }

    protected void put(T entity, ChunkPos pos, @Nonnull WorldSchematic world)
    {
        T tmp = this.get(entity.getUUID());

        if (tmp != null)
        {
            this.remove(entity.getUUID(), world);
        }

        this.uuidMap.put(entity.getUUID(), entity.getId());
        this.entityMap.put(entity.getId(), entity);

        // Special case handling for entities potentially larger than a chunk (ie; the Ender Dragon)
        if (entity instanceof EnderDragon)
        {
            AABB bb = entity.getBoundingBox();
            int minChunkX = ((int) Math.floor(bb.minX)) >> 4;
            int maxChunkX = ((int) Math.floor(bb.maxX)) >> 4;
            int minChunkZ = ((int) Math.floor(bb.minZ)) >> 4;
            int maxChunkZ = ((int) Math.floor(bb.maxZ)) >> 4;

            for (int cx = minChunkX; cx <= maxChunkX; cx++)
            {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++)
                {
                    final long cp = ChunkPos.asLong(cx, cz);
                    CopyOnWriteArrayList<UUID> list = this.chunkMap.computeIfAbsent(cp, k -> new CopyOnWriteArrayList<>());

                    list.addIfAbsent(entity.getUUID());

                    if (list.size() == 1)
                    {
                        this.chunkMap.put(cp, list);
                    }
                }
            }
        }
        else
        {
            final long cp = pos.toLong();
            CopyOnWriteArrayList<UUID> list = this.chunkMap.computeIfAbsent(cp, k -> new CopyOnWriteArrayList<>());

            list.addIfAbsent(entity.getUUID());

            if (list.size() == 1)
            {
                this.chunkMap.put(cp, list);
            }
        }

        world.onTrackingStart((Entity) entity);
    }

    protected int size()
    {
        return this.entityMap.size();
    }

    protected boolean remove(UUID uuid, @Nonnull WorldSchematic world)
    {
        Integer key = this.uuidMap.get(uuid);

        for (var entry : this.chunkMap.entrySet())
        {
            Long longPos = entry.getKey();
            CopyOnWriteArrayList<UUID> list = entry.getValue();

            if (list.remove(uuid))
            {
                if (list.isEmpty())
                {
                    this.chunkMap.remove(longPos);
                }
//                else
//                {
//                    this.chunkMap.put(longPos, list);
//                }
            }
        }

        if (key != null)
        {
            this.uuidMap.remove(uuid);
            T e = this.entityMap.remove(key);

            if (e != null)
            {
                world.onTrackingStop((Entity) e);
                return true;
            }
        }
        else
        {
            for (var entry : this.entityMap.entrySet())
            {
                Integer id = entry.getKey();
                T e = entry.getValue();

                if (e.getUUID().equals(uuid))
                {
                    this.entityMap.remove(id);
                    world.onTrackingStop((Entity) e);
                    return true;
                }
            }
        }

        return false;
    }

    protected int removeByChunk(ChunkPos pos, @Nonnull WorldSchematic world)
    {
        final Long longPos = pos.toLong();
        int count = 0;
        CopyOnWriteArrayList<UUID> list = this.chunkMap.get(longPos);

        if (list == null || list.isEmpty())
        {
            return count;
        }

        for (UUID uuid : list)
        {
            Integer key;

            key = this.uuidMap.remove(uuid);

            if (key != null)
            {
                T entry = this.entityMap.remove(key);
                world.onTrackingStop((Entity) entry);
                count++;
            }
            else
            {
                for (var entry : this.entityMap.entrySet())
                {
                    Integer id = entry.getKey();
                    T e = entry.getValue();

                    if (e.getUUID().equals(uuid))
                    {
                        this.entityMap.remove(id);
                        world.onTrackingStop((Entity) e);
                        count++;
                    }
                }
            }
        }

        this.chunkMap.remove(longPos);

        return count;
    }

    @Override
    public @Nullable T get(int id)
    {
        if (this.entityMap.containsKey(id))
        {
            T e = this.entityMap.get(id);

            if (!this.uuidMap.containsKey(e.getUUID()))
            {
                this.uuidMap.put(e.getUUID(), id);
            }

            return e;
        }

        return null;
    }

    @Override
    public @Nullable T get(@Nonnull UUID uuid)
    {
        if (this.uuidMap.containsKey(uuid))
        {
            int key = this.uuidMap.get(uuid);

            if (this.entityMap.containsKey(key))
            {
                return this.entityMap.get(key);
            }

            this.uuidMap.remove(uuid);

            return null;
        }

        for (var entry : this.entityMap.entrySet())
        {
            Integer id = entry.getKey();
            T e = entry.getValue();

            if (e.getUUID().equals(uuid))
            {
                if (!this.uuidMap.containsKey(uuid))
                {
                    this.uuidMap.put(uuid, id);
                }

                return e;
            }
        }

        return null;
    }

    public Iterable<T> getAllByChunk(ChunkPos pos)
    {
        final CopyOnWriteArrayList<UUID> list = this.chunkMap.get(pos.toLong());

        if (list == null || list.isEmpty())
        {
            return Collections.emptyList();
        }

        return Iterables.unmodifiableIterable(
                this.entityMap.values().stream()
                              .filter(e -> list.contains(e.getUUID()))
                              .toList()
        );
    }

    @Override
    public @Nonnull Iterable<T> getAll()
    {
        return Iterables.unmodifiableIterable(this.entityMap.values());
    }

    @Override
    public void get(@Nonnull AABB box, @Nonnull Consumer<T> action)
    {
        AABB adjBox = new AABB(box.minX-2, box.minY-4, box.minZ-2,
                               box.maxX+2, box.maxY+0, box.maxZ+2);
        List<UUID> added = new ArrayList<>();

        // Expand the BB slightly, but then filter out duplicate UUID.
        this.entityMap.forEach(
                (id, e) ->
                {
                    if (adjBox.intersects(e.getBoundingBox()))
                    {
                        AbortableIterationConsumer<T> consumer = AbortableIterationConsumer.forConsumer(action);

                        if (!added.contains(e.getUUID()))
                        {
                            added.add(e.getUUID());

                            if (consumer.accept(e).shouldAbort())
                            {
                                return;
                            }
                        }
                    }
                });
    }

    @Override
    public <U extends T> void get(@Nonnull EntityTypeTest<T, U> filter, @Nonnull AABB box, @Nonnull AbortableIterationConsumer<U> consumer)
    {
        AABB adjBox = new AABB(box.minX-2, box.minY-4, box.minZ-2,
                               box.maxX+2, box.maxY+0, box.maxZ+2);
        List<UUID> added = new ArrayList<>();

        // Expand the BB slightly, but then filter out duplicate UUID.
        this.entityMap.forEach(
                (id, e) ->
                {
                    U filtered = filter.tryCast(e);

                    if (filtered != null && adjBox.intersects(filtered.getBoundingBox()))
                    {
                        if (!added.contains(e.getUUID()))
                        {
                            added.add(e.getUUID());

                            if (consumer.accept(filtered).shouldAbort())
                            {
                                return;
                            }
                        }
                    }
                });
    }

    @Override
    public <U extends T> void get(@Nonnull EntityTypeTest<T, U> filter, @Nonnull AbortableIterationConsumer<U> consumer)
    {
        this.entityMap.forEach(
                (id, e) ->
                {
                    U filtered = filter.tryCast(e);

                    if (filtered != null)
                    {
                        if (consumer.accept(filtered).shouldAbort())
                        {
                            return;
                        }
                    }
                });
    }

    public boolean contains(int id)
    {
        return this.entityMap.containsKey(id);
    }

    public boolean contains(UUID uuid)
    {
        return this.uuidMap.containsKey(uuid);
    }

    public boolean contains(ChunkPos pos)
    {
        return this.chunkMap.containsKey(pos.toLong());
    }

    protected void reset()
    {
        this.entityMap.clear();
        this.uuidMap.clear();
        this.chunkMap.clear();
    }

    @Override
    public void close() throws Exception
    {
        this.reset();
    }
}
