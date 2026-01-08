package fi.dy.masa.litematica.world;

import com.google.common.collect.Iterables;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

public class SchematicEntityLookup<T extends EntityAccess> implements LevelEntityGetter<T>, AutoCloseable
{
    private final ConcurrentHashMap<Integer, T> entityMap;
    private final ConcurrentHashMap<UUID, Integer> uuidMap;

    protected SchematicEntityLookup()
    {
        this.entityMap = new ConcurrentHashMap<>();
        this.uuidMap = new ConcurrentHashMap<>();
    }

    protected synchronized void put(T entity)
    {
        T tmp = this.get(entity.getUUID());

        if (tmp != null)
        {
            this.remove(entity.getUUID());
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

    protected synchronized void remove(UUID uuid)
    {
        Integer key = this.uuidMap.get(uuid);

        if (key != null)
        {
            synchronized (this.entityMap)
            {
                this.entityMap.remove(key);
            }

            synchronized (this.uuidMap)
            {
                this.uuidMap.remove(uuid);
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
                        return;
                    }
                }
            }
        }
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

    @Override
    public synchronized @Nonnull Iterable<T> getAll()
    {
        return Iterables.unmodifiableIterable(this.entityMap.values());
    }

    @Override
    public synchronized void get(@Nonnull AABB box, @Nonnull Consumer<T> action)
    {
        this.entityMap.forEach(
                (id, e) ->
                {
                    if (box.intersects(e.getBoundingBox()))
                    {
                        AbortableIterationConsumer<T> consumer = AbortableIterationConsumer.forConsumer(action);

                        if (consumer.accept(e).shouldAbort())
                        {
                            return;
                        }
                    }
                });
    }

    @Override
    public synchronized <U extends T> void get(@Nonnull EntityTypeTest<T, U> filter, @Nonnull AABB box, @Nonnull AbortableIterationConsumer<U> consumer)
    {
        this.entityMap.forEach(
                (id, e) ->
                {
                    U filtered = filter.tryCast(e);

                    if (filtered != null && box.intersects(filtered.getBoundingBox()))
                    {
                        if (consumer.accept(filtered).shouldAbort())
                        {
                            return;
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
    }

    @Override
    public void close() throws Exception
    {
        this.reset();
    }
}
