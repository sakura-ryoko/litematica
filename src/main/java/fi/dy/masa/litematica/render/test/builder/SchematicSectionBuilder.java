package fi.dy.masa.litematica.render.test.builder;

import java.util.Map;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import fi.dy.masa.litematica.render.test.SchematicOverlayType;
import fi.dy.masa.litematica.render.test.buffer.SchematicBlockAllocatorStorage;
import fi.dy.masa.litematica.render.test.data.SchematicRendererRegion;
import fi.dy.masa.litematica.render.test.data.SchematicSectionRenderData;
import fi.dy.masa.litematica.render.test.dispatch.SchematicBlockRenderManager;

public class SchematicSectionBuilder
{
    SchematicBlockRenderManager blockRenderManager;
    BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    public SchematicSectionBuilder(SchematicBlockRenderManager blocksManager, BlockEntityRenderDispatcher entityRenderer)
    {
        this.blockRenderManager = blocksManager;
        this.blockEntityRenderDispatcher = entityRenderer;
    }

    public SchematicSectionRenderData build(ChunkSectionPos sectionPos, SchematicRendererRegion renderRegion, VertexSorter sorter, SchematicBlockAllocatorStorage allocators)
    {
        SchematicSectionRenderData renderData = new SchematicSectionRenderData();
        BlockPos blockPos = sectionPos.getMinPos();
        BlockPos blockPos2 = blockPos.add(15, 15, 15);
        ChunkOcclusionDataBuilder chunkOcclusionDataBuilder = new ChunkOcclusionDataBuilder();
        MatrixStack matrixStack = new MatrixStack();
        BlockModelRenderer.enableBrightnessCache();
        Reference2ObjectArrayMap<RenderLayer, BufferBuilder> mapLayers = new Reference2ObjectArrayMap<>(RenderLayer.getBlockLayers().size());
        Reference2ObjectArrayMap<SchematicOverlayType, BufferBuilder> mapTypes = new Reference2ObjectArrayMap<>(SchematicOverlayType.values().length);
        Random random = Random.create();

        for (BlockPos blockPos3 : BlockPos.iterate(blockPos, blockPos2))
        {
            BufferBuilder bufferBuilder;
            RenderLayer renderLayer;
            SchematicOverlayType type;
            FluidState fluidState;
            BlockEntity blockEntity;
            BlockState blockState = renderRegion.getBlockState(blockPos3);

            if (blockState.isOpaqueFullCube(renderRegion, blockPos3))
            {
                chunkOcclusionDataBuilder.markClosed(blockPos3);
            }
            if (blockState.hasBlockEntity() && (blockEntity = renderRegion.getBlockEntity(blockPos3)) != null)
            {
                this.addBlockEntity(renderData, blockEntity);
            }
            if (!(fluidState = blockState.getFluidState()).isEmpty())
            {
                renderLayer = RenderLayers.getFluidLayer(fluidState);
                bufferBuilder = this.beginBufferBuildingByLayer(mapLayers, allocators, renderLayer);

                this.blockRenderManager.renderFluid(blockPos3, renderRegion, bufferBuilder, blockState, fluidState);
            }
            if (blockState.getRenderType() != BlockRenderType.MODEL)
            {
                continue;
            }

            renderLayer = RenderLayers.getBlockLayer(blockState);
            bufferBuilder = this.beginBufferBuildingByLayer(mapLayers, allocators, renderLayer);

            matrixStack.push();

            matrixStack.translate(ChunkSectionPos.getLocalCoord(blockPos3.getX()), ChunkSectionPos.getLocalCoord(blockPos3.getY()), ChunkSectionPos.getLocalCoord(blockPos3.getZ()));
            this.blockRenderManager.renderBlock(blockState, blockPos3, renderRegion, matrixStack, bufferBuilder, true, random);

            matrixStack.pop();
        }
        for (Map.Entry entry : mapLayers.entrySet())
        {
            RenderLayer renderLayer2 = (RenderLayer) entry.getKey();
            BuiltBuffer builtBuffer = ((BufferBuilder) entry.getValue()).endNullable();

            if (builtBuffer == null) continue;
            if (renderLayer2 == RenderLayer.getTranslucent())
            {
                renderData.sortingData = builtBuffer.sortQuads(allocators.getBufferByLayer(RenderLayer.getTranslucent()), sorter);
            }

            renderData.usedLayers.put(renderLayer2, builtBuffer);
        }
        BlockModelRenderer.disableBrightnessCache();
        renderData.occlusionData = chunkOcclusionDataBuilder.build();

        return renderData;
    }

    private <E extends BlockEntity> void addBlockEntity(SchematicSectionRenderData data, E blockEntity)
    {
        BlockEntityRenderer<E> blockEntityRenderer = this.blockEntityRenderDispatcher.get(blockEntity);

        if (blockEntityRenderer != null)
        {
            data.blockEntityList.add(blockEntity);

            if (blockEntityRenderer.rendersOutsideBoundingBox(blockEntity))
            {
                data.noCullingBlockEntities.add(blockEntity);
            }
        }
    }

    private BufferBuilder beginBufferBuildingByLayer(Map<RenderLayer, BufferBuilder> builders, SchematicBlockAllocatorStorage allocatorStorage, RenderLayer layer)
    {
        BufferBuilder bufferBuilder = builders.get(layer);

        if (bufferBuilder == null)
        {
            BufferAllocator bufferAllocator = allocatorStorage.getBufferByLayer(layer);

            bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
            builders.put(layer, bufferBuilder);
        }

        return bufferBuilder;
    }

    private BufferBuilder beginBufferBuildingByType(Map<SchematicOverlayType, BufferBuilder> builders, SchematicBlockAllocatorStorage allocatorStorage, SchematicOverlayType type)
    {
        BufferBuilder bufferBuilder = builders.get(type);

        if (bufferBuilder == null)
        {
            BufferAllocator bufferAllocator = allocatorStorage.getBufferByOverlay(type);

            bufferBuilder = new BufferBuilder(bufferAllocator, type.getDrawMode(), type.getVertexFormat());
            builders.put(type, bufferBuilder);
        }

        return bufferBuilder;
    }
}
