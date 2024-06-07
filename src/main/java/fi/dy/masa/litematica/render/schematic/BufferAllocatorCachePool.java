package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import com.google.common.collect.Queues;
import org.jetbrains.annotations.Nullable;
import fi.dy.masa.litematica.Litematica;

public class BufferAllocatorCachePool
{
    private final Queue<BufferAllocatorCache> availableBuilders;
    private volatile int availableBuilderCount;

    public BufferAllocatorCachePool(List<BufferAllocatorCache> availableBuilders)
    {
        this.availableBuilders = Queues.newArrayDeque(availableBuilders);
        this.availableBuilderCount = this.availableBuilders.size();
    }

    public static BufferAllocatorCachePool allocate(int max)
    {
        int i = Math.max(1, (int)((double) Runtime.getRuntime().maxMemory() * 0.3) / BufferAllocatorCache.EXPECTED_TOTAL_SIZE);
        int j = Math.max(1, Math.min(max, i));
        List<BufferAllocatorCache> list = new ArrayList<>(j);

        try
        {
            for(int k = 0; k < j; ++k)
            {
                list.add(new BufferAllocatorCache());
            }
        }
        catch (OutOfMemoryError e)
        {
            Litematica.logger.warn("BufferAllocatorCachePool: Allocated only {}/{} buffers", list.size(), j);
            int l = Math.min(list.size() * 2 / 3, list.size() - 1);

            for (int m = 0; m < l; m++)
            {
                list.remove(list.size() - 1).close();
            }
        }

        Litematica.debugLog("BufferAllocatorCachePool: Allocated {} buffers", list.size());

        return new BufferAllocatorCachePool(list);
    }

    @Nullable
    public BufferAllocatorCache acquire()
    {
        BufferAllocatorCache blockBufferAllocatorStorage = this.availableBuilders.poll();

        if (blockBufferAllocatorStorage != null)
        {
            //Litematica.debugLog("BufferAllocatorCachePool: acquire() -- [{}]", this.availableBuilderCount);
            this.availableBuilderCount = this.availableBuilders.size();

            return blockBufferAllocatorStorage;
        }
        else
        {
            return null;
        }
    }

    public void release(@Nonnull BufferAllocatorCache builders)
    {
        //Litematica.debugLog("BufferAllocatorCachePool: release() -- [{}]", this.availableBuilderCount);
        builders.closeAll();
        this.availableBuilders.add(builders);
        this.availableBuilderCount = this.availableBuilders.size();
    }

    public boolean hasNoAvailableBuilder()
    {
        return this.availableBuilders.isEmpty();
    }

    public int getAvailableBuilderCount()
    {
        return this.availableBuilderCount;
    }
}
