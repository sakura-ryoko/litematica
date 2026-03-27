package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.UberGpuBuffer;

/**
 * @deprecated Mojang uses something like this; but we're not.
 * @param vertexBuffer
 * @param indexBuffer
 */
@Deprecated
public record ChunkRenderUberBuffers(UberGpuBuffer<ChunkMeshDataSchematic> vertexBuffer, @Nullable UberGpuBuffer<ChunkMeshDataSchematic> indexBuffer) {}
