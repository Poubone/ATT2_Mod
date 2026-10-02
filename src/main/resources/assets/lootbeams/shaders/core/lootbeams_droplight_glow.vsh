#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec4 Color1;
in vec2 CenterUV;
in vec2 SizeUV;
in vec4 GradientBounds;

out vec2 texCoord0;
out vec2 uvCenter;
out vec2 uvSize;
out vec2 gradientBounds;
out vec4 vertexColor0;
out vec4 vertexColor1;

void main()
{
	gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

	texCoord0 = UV0;
	uvCenter = CenterUV.xy;
	uvSize = SizeUV.xy;
	gradientBounds = GradientBounds.xy;
	vertexColor0 = Color;
	vertexColor1 = Color1;
}
