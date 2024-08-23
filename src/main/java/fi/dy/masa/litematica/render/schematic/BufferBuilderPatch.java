package fi.dy.masa.litematica.render.schematic;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.BufferAllocator;

public interface BufferBuilderPatch
{
    void setOffsetY(float offset);
}
