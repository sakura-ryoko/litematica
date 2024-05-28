package fi.dy.masa.litematica.render.test.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.util.math.ChunkSectionPos;
import fi.dy.masa.litematica.render.test.SchematicOverlayType;
import fi.dy.masa.litematica.render.test.buffer.BlockBufferCache;
import fi.dy.masa.litematica.render.test.dispatch.SchematicBlockRenderManager;
import fi.dy.masa.litematica.render.test.dispatch.SchematicEntityRenderDispatch;

@Environment(value = EnvType.CLIENT)
public class SchematicSectionBuilder
{
    SchematicBlockRenderManager blockRenderManager;
    SchematicEntityRenderDispatch entityRenderDispatch;

    public SchematicSectionBuilder(SchematicBlockRenderManager blocksManager, SchematicEntityRenderDispatch entityRenderer)
    {
        this.blockRenderManager = blocksManager;
        this.entityRenderDispatch = entityRenderer;
    }

    public SchematicRenderData build(ChunkSectionPos sectionPos, ChunkRendererRegion renderRegion, VertexSorter vertexSorter, BlockBufferCache allocatorStorage)
    {
        // NO OP
         return null;
    }

    public static final class SchematicRenderData
    {
        public final List<BlockEntity> noCullingBlockEntities = new ArrayList<BlockEntity>();
        public final List<BlockEntity> blockEntityList = new ArrayList<BlockEntity>();
        public final Map<RenderLayer, BuiltBuffer> layerBuffers = new Reference2ObjectArrayMap<>();
        public final Map<SchematicOverlayType, BuiltBuffer> overlayBuffers = new Reference2ObjectArrayMap<>();
        public ChunkOcclusionData occlusionData = new ChunkOcclusionData();
        @Nullable
        public BuiltBuffer.SortState sortingData;

        public void close()
        {
            this.layerBuffers.values().forEach(BuiltBuffer::close);
            this.overlayBuffers.values().forEach(BuiltBuffer::close);
        }
    }
}
