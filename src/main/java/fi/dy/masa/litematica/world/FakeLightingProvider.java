package fi.dy.masa.litematica.world;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;
import fi.dy.masa.litematica.config.Configs;

public class FakeLightingProvider extends LevelLightEngine
{
    private final FakeLightingView lightingView;
    private static final DataLayer chunkNibbleArray = new DataLayer(15);

    public FakeLightingProvider(LightChunkGetter chunkProvider)
    {
        super(chunkProvider, false, false);

        this.lightingView = new FakeLightingView();
    }

    @Override
    public @Nonnull LayerLightEventListener getLayerListener(@Nonnull LightLayer type)
    {
        return this.lightingView;
    }

    @Override
    public int getRawBrightness(@Nonnull BlockPos pos, int ambientDarkness)
    {
        //return 15;
        return Configs.Visuals.RENDER_FAKE_LIGHTING_LEVEL.getIntegerValue();
    }

    public static DataLayer getChunkNibbleArray() { return chunkNibbleArray; }

    @Override
    public boolean lightOnInColumn(long sectionPos)
    {
        return true;
    }

    @Override
    public @Nonnull LayerLightSectionStorage.SectionType getDebugSectionType(@Nonnull LightLayer lightType, @Nonnull SectionPos pos)
    {
        return LayerLightSectionStorage.SectionType.LIGHT_ONLY;
    }

    @Override
    public @Nonnull String getDebugData(@Nonnull LightLayer lightType, @Nonnull SectionPos pos)
    {
        return Integer.toString(1);
    }

    public static class FakeLightingView implements LayerLightEventListener
    {
        @Nullable
        @Override
        public DataLayer getDataLayerData(@Nonnull SectionPos pos)
        {
            return FakeLightingProvider.getChunkNibbleArray();
        }

        @Override
        public int getLightValue(@Nonnull BlockPos pos)
        {
            //return 15;
            return Configs.Visuals.RENDER_FAKE_LIGHTING_LEVEL.getIntegerValue();
        }

        @Override
        public void checkBlock(@Nonnull BlockPos pos)
        {
            // Checked
        }

        @Override
        public void propagateLightSources(@Nonnull ChunkPos chunkPos)
        {
            // Done
        }

        @Override
        public boolean hasLightWork()
        {
            return false;
        }

        @Override
        public int runLightUpdates()
        {
            return 0;
        }

        @Override
        public void updateSectionStatus(@Nonnull SectionPos pos, boolean notReady)
        {
            // NO-OP
        }

        @Override
        public void setLightEnabled(@Nonnull ChunkPos chunkPos, boolean bl)
        {
            // NO-OP
        }
    }
}
