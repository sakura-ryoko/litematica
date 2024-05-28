package fi.dy.masa.litematica.render.test.buffer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import com.google.common.collect.Queues;
import fi.dy.masa.litematica.Litematica;

public class BlockBufferPool
{
    private final Queue<BlockBufferCache> bufferCaches;
    private volatile int bufferCacheCount;

    public BlockBufferPool(List<BlockBufferCache> bufferCaches)
    {
        this.bufferCaches = Queues.newArrayDeque(bufferCaches);
        this.bufferCacheCount = this.bufferCaches.size();
    }

    public static BlockBufferPool createPool(int max)
    {
        int i = Math.max(1, (int)((double) Runtime.getRuntime().maxMemory() * 0.3) / BlockBufferCache.TOTAL_SIZE);
        int j = Math.max(1, Math.min(max, i));
        ArrayList<BlockBufferCache> list = new ArrayList<>(j);

        try
        {
            for (int k = 0; k < j; ++k)
            {
                list.add(new BlockBufferCache());
            }
        }
        catch (OutOfMemoryError outOfMemoryError)
        {
            Litematica.logger.warn("Allocated only {}/{} schematic block buffers", list.size(), j);
            int l = Math.min(list.size() * 2 / 3, list.size() - 1);

            for (int m = 0; m < l; ++m)
            {
                list.removeLast().close();
            }
        }

        return new BlockBufferPool(list);
    }

    @Nullable
    public BlockBufferCache newCache()
    {
        BlockBufferCache cache = this.bufferCaches.poll();

        if (cache != null)
        {
            this.bufferCacheCount = this.bufferCaches.size();
            return cache;
        }
        return null;
    }

    public void addCache(BlockBufferCache cache)
    {
        this.bufferCaches.add(cache);
        this.bufferCacheCount = this.bufferCaches.size();
    }

    public boolean isEmpty()
    {
        return this.bufferCaches.isEmpty();
    }

    public int getCacheCount()
    {
        return this.bufferCacheCount;
    }
}
