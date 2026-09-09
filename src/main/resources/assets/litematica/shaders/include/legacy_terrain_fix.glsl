layout(std140) uniform LegacyTerrainFix {
    ivec2 TextureSize;
//    ivec3 ChunkPosition;
    float ChunkVisibility;
    int UseRgss;
    int hasShadersOn;
};
