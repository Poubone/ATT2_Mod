#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor0;
in vec4 vertexColor1;
in float beamAlpha;
in float alphaMultiplier;

out vec4 fragColor;

void main()
{
	vec4 color = texture(Sampler0, texCoord0);
	// Official 1.21.4: color *= ColorModulator with ColorModulator = (1,1,1,beamAlpha).
	// The beam alpha is a vertex attribute here, so every beam can share one draw.
	vec3 second = vertexColor1.rgb;
	float secondAlpha = vertexColor1.a;
	color.a *= beamAlpha;
	color.rgb = (color.a * vertexColor0.rgb) + (1.0 - color.a) * second;
	color.a *= vertexColor0.a * secondAlpha * alphaMultiplier;
	fragColor = color;
}
