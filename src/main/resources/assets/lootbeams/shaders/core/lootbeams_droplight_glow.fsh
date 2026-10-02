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

void main()
{
	vec4 texColor = texture(Sampler0, texCoord0);
	float brightness = dot(texColor.rgb, vec3(0.299, 0.587, 0.114));

	float startRadius = gradientBounds.x;
	float endRadius = gradientBounds.y;
	float dist = distance(texCoord0, uvCenter);
	float scale = uvSize.x * 0.5;
	dist = clamp((dist - startRadius * scale) / (scale * (endRadius - startRadius)), 0.0, 1.0);


	vec4 second = vertexColor1;
	if (dot(second.rgb, second.rgb) < 0.0001) {
		second = vec4(ColorModulator.rgb, vertexColor0.a);
	}
	vec4 gradientColor = mix(vertexColor0, second, dist);

	vec4 blendedColor = vec4(gradientColor.rgb * brightness, texColor.a * gradientColor.a * brightness);

	if (blendedColor.a < 0.0039) {
		discard;
	}

	fragColor = blendedColor;
}
