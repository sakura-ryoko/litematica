#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:texture_sampling.glsl>

layout(std140) uniform LegacyTerrainFix {
    ivec2 TextureSize;
    ivec3 ChunkPosition;
    float ChunkVisibility;
    int UseRgss;
    int hasShadersOn;
};

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;
//layout(location = 4) in float chunkVisibility;

layout(location = 0) out vec4 fragColor;

vec4 calculateFinalColor(vec4 color) {
    vec4 fogColor = FogColor;
    return apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
}

void main() {
    vec4 color = vec4(1, 1, 1, 1);
    color = sampleNearest(Sampler0, texCoord0, 1.0f / TextureSize) * vertexColor * ColorModulator;
//    if (hasShadersOn == 1) {
//      color = sampleNearest(Sampler0, texCoord0, 1.0f / TextureSize) * vertexColor * ColorModulator;
//    } else {
//      color = (UseRgss == 1 ? sampleRGSS(Sampler0, texCoord0, 1.0f / TextureSize) : sampleNearest(Sampler0, texCoord0, 1.0f / TextureSize)) * vertexColor * ColorModulator;
//      color = mix(FogColor * vec4(1, 1, 1, color.a), color, ChunkVisibility);
//    }
    #ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
    #endif
    fragColor = calculateFinalColor(color);
}
