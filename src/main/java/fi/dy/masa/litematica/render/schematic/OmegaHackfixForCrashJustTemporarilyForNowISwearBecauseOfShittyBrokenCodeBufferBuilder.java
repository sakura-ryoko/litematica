package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nullable;
import net.minecraft.class_9799;

// Thanks plusls for this hack fix :p
@Deprecated(forRemoval = true)
public class OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder extends class_9799
{
    //@Nullable public BufferBuilder.BuiltBuffer lastRenderBuildBuffer;
    @Nullable public class_9799.class_9800 lastRenderBuildBuffer;
    public boolean first = true;
    private double offsetY;

    public OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder(int initialCapacity)
    {
        super(initialCapacity);
    }

    /*
    public void setYOffset(double offsetY)
    {
        this.offsetY = offsetY;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
        return super.vertex(x, y + this.offsetY, z);
    }

    @Override
    public void begin(VertexFormat.DrawMode drawMode, VertexFormat format)
    {
        if (this.lastRenderBuildBuffer == null)
        {
            if (this.first == false)
            {
                this.end();
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

        super.begin(drawMode, format);
    }

    @Override
    public class_9799.class_9800 close()
    {
        this.lastRenderBuildBuffer.close();
        return this.lastRenderBuildBuffer;
    }
     */
}
