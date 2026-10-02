#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec2 uvCenter;
in vec2 uvSize;
in vec2 gradientBounds;
in vec4 vertexColor0;
in vec4 vertexColor1;

out vec4 fragColor;

vec4 overlay(vec4 base, vec4 blend) {
	float brightness = dot(base.rgb, vec3(0.299, 0.587, 0.114));
	vec3 condition = step(0.5, base.rgb);
	vec3 resultColor = mix(
		2.0 * base.rgb * blend.rgb,
		1.0 - 2.0 * (1.0 - base.rgb) * (1.0 - blend.rgb),
		condition
	);
	return vec4(resultColor, base.a * blend.a * max(brightness, 0.15));
}

void main()
{
	vec4 second = vertexColor1;
	if (dot(second.rgb, second.rgb) < 0.0001) {
		second = vec4(ColorModulator.rgb, vertexColor0.a);
	}
	vec2 normDiff = (texCoord0 - uvCenter) / max(uvSize * 0.5, vec2(0.0001));
	float dist = clamp(length(normDiff), 0.0, 1.0);
	vec4 blend = mix(vertexColor0, second, dist);
	vec4 color = overlay(texture(Sampler0, texCoord0), blend);
	color.a *= ColorModulator.a;
	if (color.a < 0.0039) {
		discard;
	}
	fragColor = color;
}
