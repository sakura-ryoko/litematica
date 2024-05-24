package fi.dy.masa.litematica.render.schematic;

import net.minecraft.class_9799;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

public class BufferBuilderPatch extends BufferBuilder
{
    private float offsetY;

    public BufferBuilderPatch(class_9799 arg, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat)
    {
        super(arg, drawMode, vertexFormat);
        this.offsetY = 0;
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
