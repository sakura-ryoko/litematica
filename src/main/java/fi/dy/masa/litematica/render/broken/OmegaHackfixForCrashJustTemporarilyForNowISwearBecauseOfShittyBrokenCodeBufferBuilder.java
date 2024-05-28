package fi.dy.masa.litematica.render.broken;

// Thanks plusls for this hack fix :p
@Deprecated
public class OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder
//public class OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder extends BufferBuilder
{
    /*
    @Nullable public BufferBuilder.BuiltBuffer lastRenderBuildBuffer;
    public boolean first = true;
    private double offsetY;

    public OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder(int initialCapacity)
    {
        super(initialCapacity);
    }

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
    public BufferBuilder.BuiltBuffer end()
    {
        this.lastRenderBuildBuffer = super.end();
        return this.lastRenderBuildBuffer;
    }
     */
}
