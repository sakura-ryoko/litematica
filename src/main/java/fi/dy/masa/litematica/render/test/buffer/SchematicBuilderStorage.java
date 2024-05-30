package fi.dy.masa.litematica.render.test.buffer;

import java.util.SequencedMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;
import fi.dy.masa.litematica.render.test.SchematicOverlayType;
import fi.dy.masa.litematica.render.test.SchematicRenderLayers;

@Environment(EnvType.CLIENT)
public class SchematicBuilderStorage
{
    private final SchematicBlockAllocatorStorage blockBufferBuilders = new SchematicBlockAllocatorStorage();
    private final SchematicBlockBuilderPool blockBufferBuildersPool;
    private final VertexConsumerProvider.Immediate entityVertexConsumers;
    private final VertexConsumerProvider.Immediate effectVertexConsumers;
    private final OutlineVertexConsumerProvider outlineVertexConsumers;

    public SchematicBuilderStorage(int maxBlockBuildersPoolSize)
    {
        this.blockBufferBuildersPool = SchematicBlockBuilderPool.createPool(maxBlockBuildersPoolSize);
        SequencedMap sortedMap = Util.make(new Object2ObjectLinkedOpenHashMap(), (map) ->
        {
            for (RenderLayer layer : SchematicRenderLayers.LAYERS)
            {
                SchematicBuilderStorage.assignBufferByLayer(map, layer);
            }
            for (SchematicOverlayType type : SchematicOverlayType.values())
            {
                SchematicBuilderStorage.assignBufferByOverlay(map, type);
            }
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

    public SchematicBlockAllocatorStorage getBlockBufferBuilders() {
        return this.blockBufferBuilders;
    }

    public SchematicBlockBuilderPool getBlockBufferBuildersPool()
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
