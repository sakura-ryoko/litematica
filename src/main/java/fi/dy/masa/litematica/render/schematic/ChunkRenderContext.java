package fi.dy.masa.litematica.render.schematic;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureContents;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.math.ColorHelper;

import fi.dy.masa.malilib.mixin.render.IMixinAbstractTexture;
import fi.dy.masa.malilib.mixin.render.IMixinBufferBuilder;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.litematica.Litematica;

@Deprecated(forRemoval = true)
public class ChunkRenderContext implements AutoCloseable
{
    private Supplier<String> name;
    private BufferUsage usage;
    private GpuBuffer gpuBuffer;
    private RenderSystem.ShapeIndexBuffer shapeIndex;
    private BufferAllocator alloc;
    private BufferBuilder builder;
    private RenderPipeline shader;
    private VertexFormat format;
    private VertexFormat.DrawMode drawMode;
    private ResourceTexture texture;
    private int textureId;
    private float[] offset;
    private float lineWidth;
    private int color;
    private boolean started;
    private int bufferIndex;

    public ChunkRenderContext(RenderPipeline shader)
    {
        this(shader, BufferUsage.DYNAMIC_WRITE);
    }

    public ChunkRenderContext(RenderPipeline shader, BufferUsage usage)
    {
        this(() -> "ChunkRenderContext", shader, usage);
    }

    public ChunkRenderContext(Supplier<String> name, RenderPipeline shader, BufferUsage usage)
    {
        this.name = name;
        this.alloc = new BufferAllocator(shader.getVertexFormat().getVertexSize() * 4);
        this.builder = new BufferBuilder(this.alloc, shader.getVertexFormatMode(), shader.getVertexFormat());
        this.shapeIndex = RenderSystem.getSequentialBuffer(shader.getVertexFormatMode());
        this.format = shader.getVertexFormat();
        this.drawMode = shader.getVertexFormatMode();
        this.shader = shader;
        this.usage = usage;
        this.gpuBuffer = null;
        this.bufferIndex = -1;
        // We don't need to reset this, in case we need to re-use the texture
//        this.texture = null;
        this.textureId = -1;
        this.offset = new float[]{0f, 0f, 0f};
        this.color = -1;
        this.lineWidth = 1.0f;
        this.started = false;
    }

    public BufferBuilder start(RenderPipeline shader)
    {
        return this.start(shader, BufferUsage.STATIC_WRITE);
    }

    public BufferBuilder start(RenderPipeline shader, BufferUsage usage)
    {
        return this.start(() -> "ChunkRenderContext", shader, usage);
    }

    public BufferBuilder start(Supplier<String> name, RenderPipeline shader, BufferUsage usage)
    {
        this.reset();
        this.name = name;
        this.alloc = new BufferAllocator(shader.getVertexFormat().getVertexSize() * 4);
        this.builder = new BufferBuilder(this.alloc, shader.getVertexFormatMode(), shader.getVertexFormat());
        this.shapeIndex = RenderSystem.getSequentialBuffer(shader.getVertexFormatMode());
        this.format = shader.getVertexFormat();
        this.drawMode = shader.getVertexFormatMode();
        this.shader = shader;
        this.usage = usage;
        this.gpuBuffer = null;
        this.bufferIndex = -1;
        // We don't need to reset this, in case we need to re-use the texture
//        this.texture = null;
        this.textureId = -1;
        this.offset = new float[]{0f, 0f, 0f};
        this.color = -1;
        this.lineWidth = 1.0f;
        this.started = true;
        return this.builder;
    }

    public String getName()
    {
        return this.name.get();
    }

    public BufferBuilder getBuilder()
    {
        return this.builder;
    }

    public BufferUsage getUsage()
    {
        return this.usage;
    }

    public VertexFormat getVertexFormat()
    {
        return this.format;
    }

    public VertexFormat.DrawMode getDrawMode()
    {
        return this.drawMode;
    }

    public VertexFormat getShaderFormat()
    {
        if (this.shader != null)
        {
            return this.shader.getVertexFormat();
        }

        return this.format;
    }

    public VertexFormat.DrawMode getShaderDrawMode()
    {
        if (this.shader != null)
        {
            return this.shader.getVertexFormatMode();
        }

        return this.drawMode;
    }

    /**
     * BUILDER PHASE --
     * -
     * This is to simply ensure that the builder is stored again
     * @param builder ()
     * @return ()
     */
    public ChunkRenderContext setBuilder(BufferBuilder builder) throws RuntimeException
    {
        this.ensureBuilding(builder);
        this.builder = builder;
        return this;
    }

    public ChunkRenderContext lineWidth(float width)
    {
        this.lineWidth = Math.clamp(width, 0.0f, 25.0f);
        return this;
    }

    public ChunkRenderContext offset(float[] value)
    {
        if (value.length != 3)
        {
            value = new float[]{0f, 0f, 0f};
        }

        this.offset[0] = value[0];
        this.offset[1] = value[1];
        this.offset[2] = value[2];

        return this;
    }

    public ChunkRenderContext color(int color)
    {
        this.color = color;
        return this;
    }

    /**
     * UPLOAD PHASE --
     * -
     * This uploads the BufferBuilder to the GpuBuffer for Drawing
     */
    public void upload() throws RuntimeException
    {
        this.ensureSafeNoShader();
        this.ensureBuilding(this.builder);
        this.upload(this.name, this.builder.endNullable(), BufferType.VERTICES);
    }

    public void upload(BufferBuilder builder) throws RuntimeException
    {
        this.ensureSafeNoShader();
        this.ensureBuilding(builder);
        this.builder = builder;
        this.upload(this.name, this.builder.endNullable(), BufferType.VERTICES);
    }

    public void upload(BuiltBuffer meshData) throws RuntimeException
    {
        this.ensureSafeNoShader();
        this.upload(this.name, meshData, BufferType.VERTICES);
    }

    public void upload(BuiltBuffer meshData, BufferType target) throws RuntimeException
    {
        this.ensureSafeNoShader();
        this.upload(this.name, meshData, target);
    }

    public void upload(Supplier<String> name, BuiltBuffer meshData, BufferType target) throws RuntimeException
    {
        this.name = name;
        this.ensureSafeNoShader();

        if (RenderSystem.isOnRenderThread() && meshData != null)
        {
            int expectedSize = meshData.getBuffer().remaining();

            if (this.gpuBuffer != null)
            {
                this.gpuBuffer.close();
            }

            this.gpuBuffer = RenderSystem.getDevice().createBuffer(this.name, target, this.usage, expectedSize);

            RenderSystem.getDevice()
                        .createCommandEncoder()
                        .writeToBuffer(this.gpuBuffer, meshData.getBuffer(), 0);

            this.bufferIndex = meshData.getDrawParameters().indexCount();
            meshData.close();
        }
    }

    /**
     * BIND TEXTURE PHASE --
     * -
     * Performs the Texture Binding/Unbind for the "Shader Texture" layer
     */
    public void bindTexture(Identifier id, int textureId, int width, int height) throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (textureId < 0 || textureId > 12)
        {
            throw new RuntimeException("Invalid textureId of: "+textureId+" for texture: "+id.toString());
        }

        try
        {
            // Verify that we potentially have the correct texture by checking various values
            while (!this.isTextureValid(width, height))
            {
                this.texture = (ResourceTexture) RenderUtils.tex().getTexture(id);

                if (this.isTextureValid(width, height))
                {
                    if (this.texture != null)
                    {
                        this.texture.setFilter(TriState.DEFAULT, false);
                        RenderSystem.setShaderTexture(textureId, this.texture.getGlTexture());
                    }

                    break;
                }
            }
        }
        catch (Exception err)
        {
            throw new RuntimeException("Exception reading Texture ["+id.toString()+"]: "+err.getMessage());
        }

        // General failure & cleanup
        if (this.texture != null)
        {
            // Simple texture rebind since we already have a valid texture
            this.textureId = textureId;
            RenderSystem.setShaderTexture(this.textureId, this.texture.getGlTexture());
            return;
        }

        Litematica.LOGGER.error("bindTexture: Error uploading texture [{}]", id.toString());

        if (this.texture != null)
        {
            this.texture.close();
        }

        this.texture = null;
        this.textureId = -1;
    }

    private boolean isTextureValid(int width, int height)
    {
        if (this.texture == null)
        {
            return false;
        }

        try (TextureContents content = this.texture.loadContents(RenderUtils.mc().getResourceManager()))
        {
            NativeImage image = content.image();

            if (image == null || image.getWidth() != width || image.getHeight() != height)
            {
                this.texture.close();
                this.texture = null;
                return false;
            }
        }
        catch (Exception e)
        {
            this.texture.close();
            this.texture = null;
            return false;
        }

        if (((IMixinAbstractTexture) this.texture).malilib_getGlTexture() == null ||
                this.texture.getGlTexture().isClosed())
        {
            this.texture.close();
            this.texture = null;
            return false;
        }

        return true;
    }

//    private @Nullable NativeImageBackedTexture loadFile(Identifier texture)
//    {
//        try
//        {
//            InputStream inputStream = RenderUtils.mc().getResourceManager().open(texture);
//
//            try (NativeImage image = NativeImage.read(inputStream))
//            {
//                return new NativeImageBackedTexture(texture::toString, image.getWidth(), image.getHeight(), false);
//            }
//            catch (Exception err)
//            {
//                Litematica.LOGGER.error("RenderContext: Failed to read texture: '{}'; Exception: {}", texture.toString(), err.getMessage());
//            }
//        }
//        catch (Exception err)
//        {
//            Litematica.LOGGER.error("RenderContext: Error opening input stream for texture: '{}'; Exception: {}", texture.toString(), err.getMessage());
//        }
//
//        return null;
//    }

//    private void dumpTexture(Identifier id)
//    {
//        Path dir = FileUtils.getConfigDirectoryAsPath().resolve(Reference.MOD_ID).resolve("textures");
//
//        try (TextureContents content = this.texture.loadContents(RenderUtils.mc().getResourceManager()))
//        {
//            if (!Files.isDirectory(dir))
//            {
//                Files.createDirectory(dir);
//            }
//
//            content.image().writeTo(dir.resolve(FileNameUtils.generateSimpleSafeFileName(id.toString())));
//        }
//        catch (Exception err)
//        {
//            Litematica.LOGGER.error("bindTexture: Error saving debug texture for [{}]", id.toString());
//        }
//    }

//    private void dumpTextureManager()
//    {
//        Path dir = FileUtils.getConfigDirectoryAsPath().resolve(Reference.MOD_ID).resolve("textures/manager_dump");
//
//        try
//        {
//            if (!Files.isDirectory(dir))
//            {
//                Files.createDirectory(dir);
//            }
//
//            RenderUtils.tex().dumpDynamicTextures(dir);
//        }
//        catch (Exception ignored) {}
//    }

    public void unbindTexture(@Nullable Identifier id)
    {
        if (id != null)
        {
            RenderUtils.tex().destroyTexture(id);
        }

        RenderSystem.setShaderTexture(0, null);
    }

    /**
     * DRAW PHASE --
     * -
     * Performs the Renderer draw to the specified Frame Buffer
     */
    public void draw() throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.ensureBuilding(this.builder);
        this.draw(this.builder.endNullable());
    }

    public void draw(BuiltBuffer meshData) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.draw(null, BufferType.VERTICES, meshData, false, false);
    }

    public void draw(BuiltBuffer meshData, boolean setLineWidth) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.draw(null, BufferType.VERTICES, meshData, false, setLineWidth);
    }

    public void draw(@Nullable Framebuffer otherFb, BuiltBuffer meshData) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.draw(otherFb, BufferType.VERTICES, meshData, false, false);
    }

    public void draw(@Nullable Framebuffer otherFb, BuiltBuffer meshData, boolean setLineWidth) throws RuntimeException
    {
        this.ensureSafeNoBuffer();
        this.draw(otherFb, BufferType.VERTICES, meshData, false, setLineWidth);
    }

    public void draw(@Nullable Framebuffer otherFb, BufferType target,
                     BuiltBuffer meshData, boolean useOffset, boolean setLineWidth) throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (RenderSystem.isOnRenderThread())
        {
            if (meshData == null)
            {
                this.bufferIndex = 0;
            }
            else
            {
                // Create & upload buffer
                if (this.bufferIndex < 1)
                {
                    //Litematica.LOGGER.warn("RenderContext#draw() [{}] --> upload()", this.name.get());
                    this.upload(name, meshData, target);
                }
            }

            // Draw
            if (this.bufferIndex > 0)
            {
                float[] rgba = {ColorHelper.getRedFloat(this.color), ColorHelper.getGreenFloat(this.color), ColorHelper.getBlueFloat(this.color), ColorHelper.getAlphaFloat(this.color)};

                //Litematica.LOGGER.warn("RenderContext#drawPost() [{}] --> drawInternal()", this.name.get());
                RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
                this.drawInternal(otherFb, useOffset, setLineWidth);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    public void drawPost() throws RuntimeException
    {
        this.ensureSafeNoTexture();
        this.drawPost(null, false, false);
    }

    public void drawPost(boolean setLineWidth) throws RuntimeException
    {
        this.ensureSafeNoTexture();
        this.drawPost(null, false, setLineWidth);
    }

    public void drawPost(@Nullable Framebuffer otherFb, boolean setLineWidth) throws RuntimeException
    {
        this.ensureSafeNoTexture();
        this.drawPost(otherFb, false, setLineWidth);
    }

    public void drawPost(@Nullable Framebuffer otherFb, boolean useOffset, boolean setLineWidth) throws RuntimeException
    {
        this.ensureSafeNoTexture();

        if (this.bufferIndex > 0)
        {
            float[] rgba = new float[]{ColorHelper.getRedFloat(this.color), ColorHelper.getGreenFloat(this.color), ColorHelper.getBlueFloat(this.color), ColorHelper.getAlphaFloat(this.color)};

            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], rgba[3]);
            this.drawInternal(otherFb, useOffset, setLineWidth);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void drawInternal(@Nullable Framebuffer otherFb, boolean useOffset, boolean setLineWidth) throws RuntimeException
    {
        this.ensureSafeNoTexture();

        if (RenderSystem.isOnRenderThread())
        {
            if (useOffset)
            {
                RenderSystem.setModelOffset(-this.offset[0], this.offset[1], -this.offset[2]);
                //RenderSystem.setModelOffset(offset[0], offset[1], offset[2]);
            }
            Framebuffer mainFb = RenderUtils.fb();
            GpuTexture texture1;
            GpuTexture texture2;

            if (otherFb != null)
            {
                texture1 = otherFb.getColorAttachment();
                texture2 = otherFb.useDepthAttachment ? otherFb.getDepthAttachment() : null;
            }
            else
            {
                texture1 = mainFb.getColorAttachment();
                texture2 = mainFb.useDepthAttachment ? mainFb.getDepthAttachment() : null;
            }

            //Litematica.LOGGER.warn("RenderContext#drawInternal() [{}] --> new renderPass", this.name.get());
            GpuBuffer indexBuffer = this.shapeIndex.getIndexBuffer(this.bufferIndex);

            // Attach Frame buffers
            try (RenderPass pass = RenderSystem.getDevice()
                                               .createCommandEncoder()
                                               .createRenderPass(texture1, OptionalInt.empty(),
                                                                 texture2, OptionalDouble.empty()))
            {
                //Litematica.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> setPipeline() [{}]", this.name.get(), this.shader.getLocation().toString());
                pass.setPipeline(this.shader);

//                for (int i = 0; i < 12; i++)
//                {
//                    GpuTexture drawableTexture = RenderSystem.getShaderTexture(i);
//
//                    if (drawableTexture != null)
//                    {
//                        //Litematica.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> bindSampler() [{}]", this.name.get(), i);
//                        pass.bindSampler("Sampler"+i, drawableTexture);
//                    }
//                }

                if (this.textureId > -1 && this.textureId < 12 && this.texture != null)
                {
                    Litematica.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> bindSampler({}) [{}]", this.name.get(), this.textureId, this.texture.getGlTexture().getLabel());
                    pass.bindSampler("Sampler"+this.textureId, this.texture.getGlTexture());
                }

                if (setLineWidth)
                {
                    float width = this.lineWidth > 0.0f ? this.lineWidth : RenderSystem.getShaderLineWidth();
                    //Litematica.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> setUniform() // lineWidth [{}]", this.name.get(), width);
                    pass.setUniform("LineWidth", width);
                }

                //Litematica.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> setVertexBuffer() [0]", this.name.get());
                pass.setVertexBuffer(0, this.gpuBuffer);
                //Litematica.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> setIndexBuffer() [{}]", this.name.get(), this.bufferIndex);
                pass.setIndexBuffer(indexBuffer, this.shapeIndex.getIndexType());
                //Litematica.LOGGER.warn("RenderContext#drawInternal() [{}] renderPass --> drawIndexed() [0, {}]", this.name.get(), this.bufferIndex);
                pass.drawIndexed(0, this.bufferIndex);
            }

            //Litematica.LOGGER.warn("RenderContext#drawInternal() [{}] --> END", this.name.get());

            if (useOffset)
            {
                RenderSystem.resetModelOffset();
            }
        }
    }

    private void ensureBuilding(BufferBuilder builder) throws RuntimeException
    {
        // Check BufferBuilder status
        if (!((IMixinBufferBuilder) builder).malilib_isBuilding())
        {
            throw new RuntimeException("Buffer Builder is not building!");
        }

        if (((IMixinBufferBuilder) builder).malilib_getVertexCount() == 0)
        {
            throw new RuntimeException("Buffer Builder vertices are zero!");
        }

        if (((IMixinBufferBuilder) builder).malilib_getVertexPointer() == -1L)
        {
            throw new RuntimeException("Buffer Builder has no vertices!");
        }
    }

    private void ensureSafeNoShader() throws RuntimeException
    {
        if (!this.started)
        {
            throw new RuntimeException("Context not started!");
        }

        if (this.alloc == null)
        {
            throw new RuntimeException("Allocator not valid!");
        }

        if (this.builder == null)
        {
            throw new RuntimeException("Buffer Builder not valid!");
        }

        if (this.name.get().isEmpty())
        {
            this.name = () -> "RenderContext";
        }
    }

    private void ensureSafeNoBuffer() throws RuntimeException
    {
        this.ensureSafeNoShader();

        if (this.shader == null)
        {
            throw new RuntimeException("Shader Pipeline not valid!");
        }
    }

    private void ensureSafeNoTexture() throws RuntimeException
    {
        this.ensureSafeNoBuffer();

        if (this.gpuBuffer == null)
        {
            throw new RuntimeException("GpuBuffer not uploaded!");
        }
    }

    private void ensureSafe()
    {
        this.ensureSafeNoTexture();

        if (this.texture == null)
        {
            throw new RuntimeException("A Texture Object is expected to be bound");
        }
    }

    public void reset()
    {
        if (this.gpuBuffer != null)
        {
            this.gpuBuffer.close();
            this.gpuBuffer = null;
        }

        if (this.builder != null)
        {
            if (((IMixinBufferBuilder) this.builder).malilib_isBuilding() &&
                ((IMixinBufferBuilder) this.builder).malilib_getVertexCount() != 0)
            {
                try
                {
                    BuiltBuffer meshData = this.builder.endNullable();

                    if (meshData != null)
                    {
                        meshData.close();
                    }
                }
                catch (Exception ignored) { }
            }

            this.builder = null;
        }

        if (this.alloc != null)
        {
            this.alloc.close();
            this.alloc = null;
        }

        this.bufferIndex = -1;
        this.offset = new float[]{0f, 0f, 0f};
        this.color = -1;
        this.lineWidth = 1.0f;
        this.started = false;
    }

    @Override
    public void close() throws Exception
    {
        if (this.texture != null)
        {
            this.unbindTexture(this.texture.getId());
            this.texture.close();
        }

        this.reset();
    }
}
