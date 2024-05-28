package fi.dy.masa.litematica.render.cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.schematic.ChunkRenderLayers;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo;

public class ChunkRenderCache implements AutoCloseable
{
    public static final List<RenderLayer> LAYERS = ChunkRenderLayers.LAYERS;
    public static final List<ChunkRendererSchematicVbo.OverlayRenderType> TYPES = ChunkRenderLayers.TYPES;
    public static final int TOTAL_ALLOCATION_SIZE = LAYERS.stream().mapToInt(RenderLayer::getExpectedBufferSize).sum() + TYPES.stream().mapToInt(ChunkRendererSchematicVbo.OverlayRenderType::getExpectedBufferSize).sum();

    private final Map<RenderLayer, BufferAllocator> layerAllocators = Util.make(new Reference2ObjectArrayMap<>(LAYERS.size()), refMap ->
    {
        for (RenderLayer layer : LAYERS)
        {
            refMap.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
        }
    });
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BufferAllocator> overlayAllocators = Util.make(new Reference2ObjectArrayMap<>(TYPES.size()), refMap ->
    {
        for (ChunkRendererSchematicVbo.OverlayRenderType type : TYPES)
        {
            refMap.put(type, new BufferAllocator(type.getExpectedBufferSize()));
        }
    });

    private final Map<RenderLayer, BufferBuilder> layerBuilders = LAYERS.stream().collect(Collectors.toMap(layer -> layer, layer -> new BufferBuilder(this.getAllocatorByLayer(layer), layer.getDrawMode(), layer.getVertexFormat())));
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BufferBuilder> overlayBuilders = TYPES.stream().collect(Collectors.toMap(type -> type, type -> new BufferBuilder(this.getAllocatorByOverlay(type), type.getDrawMode(), type.getVertexFormat())));

    private final Map<RenderLayer, BuiltBuffer> layerMeshData = new HashMap<>();
    private final Map<ChunkRendererSchematicVbo.OverlayRenderType, BuiltBuffer> overlayMeshData = new HashMap<>();

    public BufferAllocator getAllocatorByLayer(RenderLayer layer)
    {
        return this.layerAllocators.get(layer);
    }

    public BufferAllocator getAllocatorByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayAllocators.get(type);
    }

    public void clearAllocators()
    {
        this.layerAllocators.values().forEach(BufferAllocator::clear);
        this.overlayAllocators.values().forEach(BufferAllocator::clear);
    }

    public void resetAllocators()
    {
        this.layerAllocators.values().forEach(BufferAllocator::reset);
        this.overlayAllocators.values().forEach(BufferAllocator::reset);
    }

    public void closeAllocators()
    {
        this.layerAllocators.values().forEach(BufferAllocator::close);
        this.overlayAllocators.values().forEach(BufferAllocator::close);
    }

    public BufferBuilder getBuilderByLayer(RenderLayer layer)
    {
        return this.layerBuilders.get(layer);
    }

    public BufferBuilder getBuilderByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayBuilders.get(type);
    }

    public void closeBuilders()
    {
        // NO-OP
    }

    public void storeBuiltBufferByLayer(RenderLayer layer, @Nonnull BuiltBuffer newMeshData)
    {
        this.layerMeshData.put(layer, newMeshData);
    }

    public void storeBuiltBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type, @Nonnull BuiltBuffer newMeshData)
    {
        this.overlayMeshData.put(type, newMeshData);
    }

    public boolean hasBuiltBufferByLayer(RenderLayer layer)
    {
        return this.layerMeshData.containsKey(layer);
    }

    public boolean hasBuiltBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayMeshData.containsKey(type);
    }

    @Nullable
    public BuiltBuffer getBuiltBufferByLayer(RenderLayer layer)
    {
        return this.layerMeshData.get(layer);
    }

    @Nullable
    public BuiltBuffer getBuiltBufferByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type)
    {
        return this.overlayMeshData.get(type);
    }

    /**
     * Get the new SortState Parameter for the Translucent layer only
     *
     * @param layer (The Render layer used)
     * @param sorter (Vertex Sorter based on origin value)
     * @return (SortState if successful, or null)
     */
    @Nullable
    public BuiltBuffer.SortState getSortStateByLayer(RenderLayer layer, @Nonnull VertexSorter sorter)
    {
        if (this.hasBuiltBufferByLayer(layer) && layer == RenderLayer.getTranslucent())
        {
            BuiltBuffer meshData = this.getBuiltBufferByLayer(layer);

            if (meshData != null)
            {
                try
                {
                    return meshData.sortQuads(this.getAllocatorByLayer(layer), sorter);
                }
                catch (Exception ignored) {}
            }
        }

        return null;
    }

    /**
     * Get the new SortState Parameter for the Translucent layer only
     *
     * @param type (The Overlay Type)
     * @param sorter (VertexSorter based on the origin value)
     * @return (SortState if successful, or null)
     */
    @Nullable
    public BuiltBuffer.SortState getSortStateByOverlay(ChunkRendererSchematicVbo.OverlayRenderType type, @Nonnull VertexSorter sorter)
    {
        if (this.hasBuiltBufferByOverlay(type) && type == ChunkRendererSchematicVbo.OverlayRenderType.QUAD)
        {
            BuiltBuffer meshData = this.getBuiltBufferByOverlay(type);

            if (meshData != null)
            {
                try
                {
                    return meshData.sortQuads(this.getAllocatorByOverlay(type), sorter);
                }
                catch (Exception ignored) {}
            }
        }

        return null;
    }

    public void uploadSectionMesh(@Nonnull BuiltBuffer meshData, @Nonnull VertexBuffer vertexBuffer)
    {
        if (vertexBuffer.isClosed())
        {
            Litematica.logger.error("uploadSectionMesh: error uploading MeshData (VertexBuffer is closed)");

            meshData.close();
            return;
        }
        vertexBuffer.bind();
        vertexBuffer.upload(meshData);
        VertexBuffer.unbind();

        Litematica.logger.warn("uploadSectionMesh() Done");
    }

    public void uploadSectionSortedIndex(@Nonnull BufferAllocator.CloseableBuffer result, @Nonnull VertexBuffer vertexBuffer)
    {
        if (vertexBuffer.isClosed())
        {
            Litematica.logger.error("uploadSectionSortedIndex: error uploading MeshData SortState (VertexBuffer is closed)");

            result.close();
            return;
        }
        vertexBuffer.bind();
        vertexBuffer.uploadIndexBuffer(result);
        VertexBuffer.unbind();

        Litematica.logger.warn("uploadSectionSortedIndex() Done");
    }

    public void closeBuiltBuffers()
    {
        this.layerMeshData.values().forEach(BuiltBuffer::close);
        this.overlayMeshData.values().forEach(BuiltBuffer::close);
    }

    public void clearAll()
    {
        //this.closeVertexBuffers();
        this.closeAllocators();
        this.closeBuilders();
        this.closeBuiltBuffers();
    }

    @Override
    public void close() throws Exception
    {
        this.clearAll();
    }
}
