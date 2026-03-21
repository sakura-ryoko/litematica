package fi.dy.masa.litematica.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import com.google.common.collect.Iterables;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.AbortableIterationConsumer;
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
    private final ConcurrentHashMap<Long, List<UUID>> chunkMap;

    protected SchematicEntityLookup()
    {
        this.entityMap = new ConcurrentHashMap<>(128, 0.9f, 1);
        this.uuidMap = new ConcurrentHashMap<>(128, 0.9f, 1);
        this.chunkMap = new ConcurrentHashMap<>(128, 0.9f, 1);
    }

    protected String getDebugString()
    {
        return String.format("E: %02d, U: %02d, C: %02d",
                             this.entityMap.size(), this.uuidMap.size(), this.chunkMap.size()
        );
    }

    protected synchronized void put(T entity, ChunkPos pos)
    {
        T tmp = this.get(entity.getUUID());

        if (tmp != null)
        {
            this.remove(entity.getUUID());
        }

        synchronized (this.chunkMap)
        {
            List<UUID> list = this.chunkMap.getOrDefault(pos.pack(), new ArrayList<>());

            list.add(entity.getUUID());
            this.chunkMap.put(pos.pack(), list);
        }

        synchronized (this.uuidMap)
        {
            this.uuidMap.put(entity.getUUID(), entity.getId());
        }

        synchronized (this.entityMap)
        {
            this.entityMap.put(entity.getId(), entity);
        }
    }

    protected synchronized int size()
    {
        return this.entityMap.size();
    }

    protected synchronized boolean remove(UUID uuid)
    {
        Integer key = this.uuidMap.get(uuid);

        synchronized (this.chunkMap)
        {
            for (Long longPos : this.chunkMap.keySet())
            {
                List<UUID> list = this.chunkMap.get(longPos);

                if (list.remove(uuid))
                {
                    if (list.isEmpty())
                    {
                        this.chunkMap.remove(longPos);
                    }
                    else
                    {
                        this.chunkMap.put(longPos, list);
                    }
                }
            }
        }

        if (key != null)
        {
            synchronized (this.uuidMap)
            {
                this.uuidMap.remove(uuid);
            }

            synchronized (this.entityMap)
            {
                T e = this.entityMap.remove(key);

                if (e != null)
                {
                    return true;
                }
            }
        }
        else
        {
            synchronized (this.entityMap)
            {
                for (Integer id : this.entityMap.keySet())
                {
                    T e = this.entityMap.get(id);

                    if (e.getUUID().equals(uuid))
                    {
                        this.entityMap.remove(id);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected synchronized int removeByChunk(ChunkPos pos)
    {
        final Long longPos = pos.pack();
        int count = 0;

        synchronized (this.chunkMap)
        {
            List<UUID> list = this.chunkMap.get(longPos);

            if (list == null || list.isEmpty())
            {
                return count;
            }

            for (UUID uuid : list)
            {
                Integer key;

                synchronized (this.uuidMap)
                {
                    key = this.uuidMap.remove(uuid);
                }

                if (key != null)
                {
                    synchronized (this.entityMap)
                    {
                        this.entityMap.remove(key);
                        count++;
                    }
                }
                else
                {
                    synchronized (this.entityMap)
                    {
                        for (Integer id : this.entityMap.keySet())
                        {
                            T e = this.entityMap.get(id);

                            if (e.getUUID().equals(uuid))
                            {
                                this.entityMap.remove(id);
                                count++;
                            }
                        }
                    }
                }
            }

            this.chunkMap.remove(longPos);
        }

        return count;
    }

    @Override
    public synchronized @Nullable T get(int id)
    {
        if (this.entityMap.containsKey(id))
        {
            T e = this.entityMap.get(id);

            if (!this.uuidMap.containsKey(e.getUUID()))
            {
                synchronized (this.uuidMap)
                {
                    this.uuidMap.put(e.getUUID(), id);
                }
            }

            return e;
        }

        return null;
    }

    @Override
    public synchronized @Nullable T get(@Nonnull UUID uuid)
    {
        if (this.uuidMap.containsKey(uuid))
        {
            int key = this.uuidMap.get(uuid);

            if (this.entityMap.containsKey(key))
            {
                return this.entityMap.get(key);
            }

            synchronized (this.uuidMap)
            {
                this.uuidMap.remove(uuid);
            }

            return null;
        }

        for (Integer id : this.entityMap.keySet())
        {
            T e = this.entityMap.get(id);

            if (e.getUUID().equals(uuid))
            {
                if (!this.uuidMap.containsKey(uuid))
                {
                    synchronized (this.uuidMap)
                    {
                        this.uuidMap.put(uuid, id);
                    }
                }

                return e;
            }
        }

        return null;
    }

    public synchronized Iterable<T> getAllByChunk(ChunkPos pos)
    {
        synchronized (this.chunkMap)
        {
            final List<UUID> list = this.chunkMap.get(pos.pack());

            if (list == null || list.isEmpty())
            {
                return Collections.emptyList();
            }

            synchronized (this.entityMap)
            {
                return Iterables.unmodifiableIterable(
                        this.entityMap.values().stream()
                                      .filter(e -> list.contains(e.getUUID()))
                                      .toList()
                );
            }
        }
    }

    @Override
    public synchronized @Nonnull Iterable<T> getAll()
    {
        return Iterables.unmodifiableIterable(this.entityMap.values());
    }

    @Override
    public synchronized void get(@Nonnull AABB box, @Nonnull Consumer<T> action)
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
    public synchronized <U extends T> void get(@Nonnull EntityTypeTest<T, U> filter, @Nonnull AABB box, @Nonnull AbortableIterationConsumer<U> consumer)
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
    public synchronized <U extends T> void get(@Nonnull EntityTypeTest<T, U> filter, @Nonnull AbortableIterationConsumer<U> consumer)
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

    public synchronized boolean contains(int id)
    {
        return this.entityMap.containsKey(id);
    }

    public synchronized boolean contains(UUID uuid)
    {
        return this.uuidMap.containsKey(uuid);
    }

    public synchronized boolean contains(ChunkPos pos)
    {
        return this.chunkMap.containsKey(pos.pack());
    }

    protected void reset()
    {
        synchronized (this.entityMap)
        {
            this.entityMap.clear();
        }

        synchronized (this.uuidMap)
        {
            this.uuidMap.clear();
        }

        synchronized (this.chunkMap)
        {
            this.chunkMap.clear();
        }
    }

    @Override
    public void close() throws Exception
    {
        this.reset();
    }
}
