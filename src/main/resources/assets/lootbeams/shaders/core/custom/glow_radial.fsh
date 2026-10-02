#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec2 uvCenter;
in vec2 uvSize;
in vec2 gradientBounds;
in vec4 vertexColor0;
in vec4 vertexColor1;

out vec4 fragColor;

void main()
{
	float startRadius = gradientBounds.x;
	float endRadius = gradientBounds.y;
	vec2 normDiff = (texCoord0 - uvCenter) / (uvSize * 0.5);
	float dist = length(normDiff);
	float gradientDist = clamp((dist - startRadius) / max(endRadius - startRadius, 0.0001), 0.0, 1.0);

	vec4 second = vertexColor1;
	if (dot(second.rgb, second.rgb) < 0.0001) {
		second = vec4(ColorModulator.rgb, vertexColor0.a);
	}
	vec4 gradientColor = mix(vertexColor0, second, gradientDist);
	fragColor = vec4(gradientColor.rgb, vertexColor0.a * (1.0 - gradientDist) * 0.5 * ColorModulator.a);
}
