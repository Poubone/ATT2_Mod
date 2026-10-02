#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    float brightness = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));
    vec4 blendedColor = vec4(vertexColor.rgb * brightness, texColor.a * vertexColor.a * brightness) * ColorModulator;

    if (blendedColor.a < 0.0039) {
        discard;
    }

    fragColor = apply_fog(blendedColor, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
