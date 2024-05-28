package fi.dy.masa.litematica.render.cache;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.BufferAllocator;

public class BufferBuilderPatch extends BufferBuilder
{
    //@Nullable
    //public BuiltBuffer lastRenderBuildBuffer;
    public boolean first = true;
    private float offsetY;

    public BufferBuilderPatch(BufferAllocator arg, VertexFormat.DrawMode drawMode, VertexFormat vertexFormat)
    {
        super(arg, drawMode, vertexFormat);

/*
        if (this.lastRenderBuildBuffer == null)
        {
            if (this.first == false)
            {
                this.lastRenderBuildBuffer = this.end();
            }
            else
            {
                this.first = false;
            }
        }
        else
        {
            this.lastRenderBuildBuffer = null;
        }
 */
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

    /*
    @Override
    public BuiltBuffer end()
    {
        this.lastRenderBuildBuffer = super.end();
        return this.lastRenderBuildBuffer;
    }
     */
}
