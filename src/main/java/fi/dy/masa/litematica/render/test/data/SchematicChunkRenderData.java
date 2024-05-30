package fi.dy.masa.litematica.render.test.data;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.compress.utils.Lists;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.util.math.Direction;
import fi.dy.masa.litematica.render.test.SchematicOverlayType;

@Environment(EnvType.CLIENT)
public class SchematicChunkRenderData
{
    public static final SchematicChunkRenderData EMPTY_1 = new SchematicChunkRenderData()
    {
        @Override
        public boolean isVisibleThrough(Direction from, Direction to)
        {
            return false;
        }
    };
    public static final SchematicChunkRenderData EMPTY_2 = new SchematicChunkRenderData()
    {
        @Override
        public boolean isVisibleThrough(Direction from, Direction to)
        {
            return true;
        }
    };
    public final Set<RenderLayer> usedLayers = new ObjectArraySet<>(RenderLayer.getBlockLayers().size());
    public final Set<SchematicOverlayType> usedOverlays = new ObjectArraySet<>(SchematicOverlayType.values().length);
    public final List<BlockEntity> blockEntityList = Lists.newArrayList();
    public ChunkOcclusionData occlusionData = new ChunkOcclusionData();
    @Nullable
    public BuiltBuffer.SortState sortingData;

    public boolean isEmpty() { return this.usedLayers.isEmpty() && this.usedOverlays.isEmpty(); }

    public boolean isEmpty(RenderLayer layer) { return !this.usedLayers.contains(layer); }

    public boolean isEmpty(SchematicOverlayType type) { return !this.usedOverlays.contains(type); }

    public List<BlockEntity> getBlockEntityList() { return this.blockEntityList; }

    public boolean isVisibleThrough(Direction from, Direction to)
    {
        return this.occlusionData.isVisibleThrough(from, to);
    }

    public void close()
    {

    }
}
