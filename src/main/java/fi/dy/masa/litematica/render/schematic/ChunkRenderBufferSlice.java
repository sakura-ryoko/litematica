package fi.dy.masa.litematica.render.schematic;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.buffers.GpuBuffer;

public record ChunkRenderBufferSlice(GpuBuffer vertexBuffer, long vertexBufferOffset, @Nullable GpuBuffer indexBuffer, long indexBufferOffset)
{}
