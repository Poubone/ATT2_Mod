#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec4 Color1;
in vec4 CustomData;

out vec2 texCoord0;
out vec4 vertexColor0;
out vec4 vertexColor1;
out float alphaMultiplier;

void main()
{
	gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

	texCoord0 = UV0;
	vertexColor0 = Color;
	vertexColor1 = Color1;
	if (dot(vertexColor1.rgb, vertexColor1.rgb) < 0.0001) {
		vec3 packedSecond = CustomData.yzw;
		if (length(packedSecond) > 0.001) {
			vertexColor1 = vec4(packedSecond, Color.a);
		}
	}
	alphaMultiplier = CustomData.x;
	if (alphaMultiplier <= 0.0) {
		alphaMultiplier = 2.0;
	}
}
