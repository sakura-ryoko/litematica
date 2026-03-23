package fi.dy.masa.litematica.render.schematic;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.UberGpuBuffer;

public record ChunkRenderUberBuffers(UberGpuBuffer<ChunkMeshDataSchematic> vertexBuffer, @Nullable UberGpuBuffer<ChunkMeshDataSchematic> indexBuffer) {}
