package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightStorage;
import net.minecraft.world.chunk.light.LightingProvider;

public class FakeLightingProvider extends LightingProvider
{
    private final FakeLightingView lightingView;
    private static final ChunkNibbleArray chunkNibbleArray = new ChunkNibbleArray(15);

    public FakeLightingProvider(ChunkProvider chunkProvider)
    {
        super(chunkProvider, false, false);

        this.lightingView = new FakeLightingView();
    }

    @Override
    public ChunkLightingView get(LightType type)
    {
        return this.lightingView;
    }

    @Override
    public int getLight(BlockPos pos, int defaultValue)
    {
        return 15;
    }

    public static ChunkNibbleArray getChunkNibbleArray() { return chunkNibbleArray; }

    @Override
    public boolean isLightingEnabled(long sectionPos)
    {
        return true;
    }

    @Override
    public LightStorage.Status getStatus(LightType lightType, ChunkSectionPos pos)
    {
        return LightStorage.Status.LIGHT_ONLY;
    }

    @Override
    public String displaySectionLevel(LightType lightType, ChunkSectionPos pos)
    {
        return Integer.toString(1);
    }

    public static class FakeLightingView implements ChunkLightingView
    {
        @Nullable
        @Override
        public ChunkNibbleArray getLightSection(ChunkSectionPos pos)
        {
            return FakeLightingProvider.chunkNibbleArray;
        }

        @Override
        public int getLightLevel(BlockPos pos)
        {
            return 15;
        }

        @Override
        public void checkBlock(BlockPos pos)
        {
            // Checked
        }

        @Override
        public void propagateLight(ChunkPos chunkPos)
        {
            // Done
        }

        @Override
        public boolean hasUpdates()
        {
            return false;
        }

        @Override
        public int doLightUpdates()
        {
            return 0;
        }

        @Override
        public void setSectionStatus(ChunkSectionPos pos, boolean notReady)
        {
            // NO-OP
        }

        @Override
        public void setColumnEnabled(ChunkPos chunkPos, boolean bl)
        {
            // NO-OP
        }
    }
}
