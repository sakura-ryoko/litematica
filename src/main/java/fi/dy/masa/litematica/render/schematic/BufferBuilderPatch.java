package fi.dy.masa.litematica.render.schematic;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.BufferAllocator;

public class BufferBuilderPatch extends BufferBuilder
{
    private float offsetY;

    public BufferBuilderPatch(BufferAllocator arg, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat)
    {
        super(arg, drawMode, vertexFormat);
        this.offsetY = (float) 0;
    }

    public void setOffsetY(float offset)
    {
        this.offsetY = offset;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z)
    {
        return super.vertex(x, (y + this.offsetY), z);
    }
}
