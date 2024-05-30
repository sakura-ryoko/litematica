package fi.dy.masa.litematica.render.test.buffer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import com.google.common.collect.Queues;
import fi.dy.masa.litematica.Litematica;

public class SchematicBlockBuilderPool
{
    private final Queue<SchematicBlockAllocatorStorage> availableBuilders;
    private volatile int availableBuilderCount;

    public SchematicBlockBuilderPool(List<SchematicBlockAllocatorStorage> bufferCaches)
    {
        this.availableBuilders = Queues.newArrayDeque(bufferCaches);
        this.availableBuilderCount = this.availableBuilders.size();
    }

    public static SchematicBlockBuilderPool createPool(int max)
    {
        int i = Math.max(1, (int) ((double) Runtime.getRuntime().maxMemory() * 0.3) / SchematicBlockAllocatorStorage.EXPECTED_TOTAL_SIZE);
        int j = Math.max(1, Math.min(max, i));
        ArrayList<SchematicBlockAllocatorStorage> list = new ArrayList<>(j);

        try
        {
            for (int k = 0; k < j; k++)
            {
                list.add(new SchematicBlockAllocatorStorage());
            }
        }
        catch (OutOfMemoryError outOfMemoryError)
        {
            Litematica.logger.warn("Allocated only {}/{} schematic block buffers", list.size(), j);
            int l = Math.min(list.size() * 2 / 3, list.size() - 1);

            for (int m = 0; m < l; m++)
            {
                list.remove(list.size() - 1).close();
            }
        }

        return new SchematicBlockBuilderPool(list);
    }

    @Nullable
    public SchematicBlockAllocatorStorage newStorage()
    {
        SchematicBlockAllocatorStorage storage = this.availableBuilders.poll();

        if (storage != null)
        {
            this.availableBuilderCount = this.availableBuilders.size();
            return storage;
        }
        else
        {
            return null;
        }
    }

    public void release(SchematicBlockAllocatorStorage cache)
    {
        this.availableBuilders.add(cache);
        this.availableBuilderCount = this.availableBuilders.size();
    }

    public boolean hasNoneAvailable()
    {
        return this.availableBuilders.isEmpty();
    }

    public int getAvailableCount()
    {
        return this.availableBuilderCount;
    }
}
