package fi.dy.masa.litematica.render.test.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import fi.dy.masa.litematica.render.test.SchematicOverlayType;

@Environment(EnvType.CLIENT)
public final class SchematicSectionRenderData
{
    public final List<BlockEntity> noCullingBlockEntities = new ArrayList<>();
    public final List<BlockEntity> blockEntityList = new ArrayList<>();
    public final Map<RenderLayer, BuiltBuffer> usedLayers = new Reference2ObjectArrayMap<RenderLayer, BuiltBuffer>();
    public final Map<SchematicOverlayType, BuiltBuffer> usedOverlays = new Reference2ObjectArrayMap<SchematicOverlayType, BuiltBuffer>();
    public ChunkOcclusionData occlusionData = new ChunkOcclusionData();
    @Nullable
    public BuiltBuffer.SortState sortingData;

    public void close()
    {
        this.usedLayers.values().forEach(BuiltBuffer::close);
        this.usedOverlays.values().forEach(BuiltBuffer::close);
    }
}
