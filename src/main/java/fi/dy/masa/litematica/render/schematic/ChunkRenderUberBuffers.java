package fi.dy.masa.litematica.render.schematic;

import com.mojang.blaze3d.vertex.UberGpuBuffer;

public record ChunkRenderUberBuffers(UberGpuBuffer<ChunkMeshDataSchematic> vertexBuffer, UberGpuBuffer<ChunkMeshDataSchematic> indexBuffer) {}
