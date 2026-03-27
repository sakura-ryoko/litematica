package fi.dy.masa.litematica.render.schematic;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.buffers.GpuBuffer;

/**
 * @deprecated Mojang uses something like this, but we're not.
 * @param vertexBuffer
 * @param vertexBufferOffset
 * @param indexBuffer
 * @param indexBufferOffset
 */
@Deprecated
public record ChunkRenderBufferSlice(GpuBuffer vertexBuffer, long vertexBufferOffset, @Nullable GpuBuffer indexBuffer, long indexBufferOffset)
{
}
