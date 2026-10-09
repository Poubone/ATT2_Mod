#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor0;
in vec4 vertexColor1;
in float beamAlpha;
in float beamWidth;
in float beamHeight;
in float animationSpeed;
in float itemTime;

out vec4 fragColor;

float timeInSeconds = GameTime * 1200.0; // 1 game cycle = 1/20 sec.

vec4 permute_3d(vec4 x){ return mod(((x*34.0)+1.0)*x, 289.0); }
vec4 taylorInvSqrt3d(vec4 r){ return 1.79284291400159 - 0.85373472095314 * r; }

float simplexNoise3d(vec3 v)
{
	const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
	const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);

	// First corner
	vec3 i  = floor(v + dot(v, C.yyy) );
	vec3 x0 =   v - i + dot(i, C.xxx) ;

	// Other corners
	vec3 g = step(x0.yzx, x0.xyz);
	vec3 l = 1.0 - g;
	vec3 i1 = min( g.xyz, l.zxy );
	vec3 i2 = max( g.xyz, l.zxy );

	//  x0 = x0 - 0. + 0.0 * C
	vec3 x1 = x0 - i1 + 1.0 * C.xxx;
	vec3 x2 = x0 - i2 + 2.0 * C.xxx;
	vec3 x3 = x0 - 1. + 3.0 * C.xxx;

	// Permutations
	i = mod(i, 289.0 );
	vec4 p = permute_3d( permute_3d( permute_3d( i.z + vec4(0.0, i1.z, i2.z, 1.0 )) + i.y + vec4(0.0, i1.y, i2.y, 1.0 ))  + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));

	// Gradients
	// ( N*N points uniformly over a square, mapped onto an octahedron.)
	float n_ = 1.0/7.0; // N=7
	vec3  ns = n_ * D.wyz - D.xzx;

	vec4 j = p - 49.0 * floor(p * ns.z *ns.z);  //  mod(p,N*N)

	vec4 x_ = floor(j * ns.z);
	vec4 y_ = floor(j - 7.0 * x_ );    // mod(j,N)

	vec4 x = x_ *ns.x + ns.yyyy;
	vec4 y = y_ *ns.x + ns.yyyy;
	vec4 h = 1.0 - abs(x) - abs(y);

	vec4 b0 = vec4( x.xy, y.xy );
	vec4 b1 = vec4( x.zw, y.zw );

	vec4 s0 = floor(b0)*2.0 + 1.0;
	vec4 s1 = floor(b1)*2.0 + 1.0;
	vec4 sh = -step(h, vec4(0.0));

	vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
	vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;

	vec3 p0 = vec3(a0.xy,h.x);
	vec3 p1 = vec3(a0.zw,h.y);
	vec3 p2 = vec3(a1.xy,h.z);
	vec3 p3 = vec3(a1.zw,h.w);

	// Normalise gradients
	vec4 norm = taylorInvSqrt3d(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
	p0 *= norm.x;
	p1 *= norm.y;
	p2 *= norm.z;
	p3 *= norm.w;

	// Mix final noise value
	vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
	m = m * m;
	return 42.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3) ) );
}

const vec2 mainSpeed = vec2(0.0, 0.5);
const vec2 noiseSpeed = vec2(0.0, 0.7);
const float baseSpeed = 4.0; // 4.0
const float noiseScale = 5.0; // 5.0
const float noisePower = 2.0; // 2.0
const float noiseAmount = 0.15; // 0.127
const float dissolveAmount = 0.5; // 0.5
const float alphaMultiplier = 4.0; // 8.0

void renderNewBeam()
{
	float speedMultiplier = animationSpeed / beamHeight * baseSpeed; // 4.0

	// Beam Speed
	vec2 beamSpeed = mainSpeed * itemTime * speedMultiplier;

	// NoiseSpeed * Time Nodes
	vec2 noiseBeamSpeed = noiseSpeed * itemTime * speedMultiplier;

	// UV0 + Noise Offset
	vec2 noiseTexCoord = (texCoord0 + noiseBeamSpeed);

	// Apply scale to coords
	vec2 scaledNoiseCoord = (noiseTexCoord * noiseScale);

	// Get Simple Noise
	float noise = simplexNoise3d(vec3(scaledNoiseCoord, 0.0));

	// Noise Power
	float powerNoise = pow(noise, noisePower);

	// Masking
	float mask = 1.0 - abs(2.0 * texCoord0.x - 1.0); // // [0, 1, 0]
	float constrainedMask = mask; //includeCenterGradient(0.3, 0.7, mask);
	float invertedMask = 1.0 - constrainedMask; // [1, 0, 1]

	// Mixed noise and texture by noiseAmount
	vec2 mixedTexture = mix(texCoord0, vec2(powerNoise), noiseAmount * invertedMask);

	// Sample the texture
	vec4 color = texture(Sampler0, mixedTexture + beamSpeed);

	// Alpha blending and dissolve
	float alphaBlend = color.a * powerNoise;
	color = mix(color, vec4(alphaBlend), dissolveAmount);

	// Official 1.21.4 mix, with the beam alpha from a vertex attribute instead of ColorModulator.a.
	vec3 second = vertexColor1.rgb;
	float secondAlpha = vertexColor1.a;
	color.a *= beamAlpha;
	color.rgb = (color.a * vertexColor0.rgb) + (1.0 - color.a) * second;
	color.a *= vertexColor0.a * secondAlpha;

	// Strengthen effect
	color.a *= alphaMultiplier;

	// Output the final color
	fragColor = color;
}


// Main shader logic
void main()
{
	renderNewBeam();
}