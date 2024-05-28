package fi.dy.masa.litematica.render.test.buffer;

import java.util.SortedMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import fi.dy.masa.litematica.render.test.SchematicOverlayType;

public class SchematicBuilderStorage
{
    private final BlockBufferCache blockBufferBuilders = new BlockBufferCache();
    private final BlockBufferPool blockBufferBuildersPool;
    private final VertexConsumerProvider.Immediate entityVertexConsumers;
    private final VertexConsumerProvider.Immediate effectVertexConsumers;
    private final OutlineVertexConsumerProvider outlineVertexConsumers;

    public SchematicBuilderStorage(int maxBlockBuildersPoolSize)
    {
        this.blockBufferBuildersPool = BlockBufferPool.createPool(maxBlockBuildersPoolSize);
        SortedMap sortedMap = Util.make(new Object2ObjectLinkedOpenHashMap(), map ->
        {
            map.put(TexturedRenderLayers.getEntitySolid(), this.blockBufferBuilders.getBufferByLayer(RenderLayer.getSolid()));
            map.put(TexturedRenderLayers.getEntityCutout(), this.blockBufferBuilders.getBufferByLayer(RenderLayer.getCutout()));
            map.put(TexturedRenderLayers.getBannerPatterns(), this.blockBufferBuilders.getBufferByLayer(RenderLayer.getCutoutMipped()));
            map.put(TexturedRenderLayers.getEntityTranslucentCull(), this.blockBufferBuilders.getBufferByLayer(RenderLayer.getTranslucent()));
            SchematicBuilderStorage.assignBufferByOverlay(map, SchematicOverlayType.OUTLINE);
            SchematicBuilderStorage.assignBufferByOverlay(map, SchematicOverlayType.QUAD);
            SchematicBuilderStorage.assignBufferByLayer(map, TexturedRenderLayers.getShieldPatterns());
            SchematicBuilderStorage.assignBufferByLayer(map, TexturedRenderLayers.getBeds());
            SchematicBuilderStorage.assignBufferByLayer(map, TexturedRenderLayers.getShulkerBoxes());
            SchematicBuilderStorage.assignBufferByLayer(map, TexturedRenderLayers.getSign());
            SchematicBuilderStorage.assignBufferByLayer(map, TexturedRenderLayers.getHangingSign());
            map.put(TexturedRenderLayers.getChest(), new BufferAllocator(786432));
            SchematicBuilderStorage.assignBufferByLayer(map, RenderLayer.getArmorEntityGlint());
            SchematicBuilderStorage.assignBufferByLayer(map, RenderLayer.getGlint());
            SchematicBuilderStorage.assignBufferByLayer(map, RenderLayer.getGlintTranslucent());
            SchematicBuilderStorage.assignBufferByLayer(map, RenderLayer.getEntityGlint());
            SchematicBuilderStorage.assignBufferByLayer(map, RenderLayer.getDirectEntityGlint());
            SchematicBuilderStorage.assignBufferByLayer(map, RenderLayer.getWaterMask());
            ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.forEach(renderLayer -> SchematicBuilderStorage.assignBufferByLayer(map, renderLayer));
        });
        this.effectVertexConsumers = VertexConsumerProvider.immediate(new BufferAllocator(1536));
        this.entityVertexConsumers = VertexConsumerProvider.immediate(sortedMap, new BufferAllocator(786432));
        this.outlineVertexConsumers = new OutlineVertexConsumerProvider(this.entityVertexConsumers);
    }

    private static void assignBufferByLayer(Object2ObjectLinkedOpenHashMap<RenderLayer, BufferAllocator> builderStorage, RenderLayer layer)
    {
        builderStorage.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
    }

    private static void assignBufferByOverlay(Object2ObjectLinkedOpenHashMap<SchematicOverlayType, BufferAllocator> builderStorage, SchematicOverlayType type)
    {
        builderStorage.put(type, new BufferAllocator(type.getExpectedBufferSize()));
    }

    public BlockBufferCache getBlockBufferBuilders() {
        return this.blockBufferBuilders;
    }

    public BlockBufferPool getBlockBufferBuildersPool()
    {
        return this.blockBufferBuildersPool;
    }

    public VertexConsumerProvider.Immediate getEntityVertexConsumers()
    {
        return this.entityVertexConsumers;
    }

    public VertexConsumerProvider.Immediate getEffectVertexConsumers()
    {
        return this.effectVertexConsumers;
    }

    public OutlineVertexConsumerProvider getOutlineVertexConsumers()
    {
        return this.outlineVertexConsumers;
    }
}
