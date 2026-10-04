#version 330

// GPU port of ManaGlobe's liquid shading. Each fragment is one texel of the globe texture, so
// gl_FragCoord (texel centre) matches the CPU loop's (x + 0.5, y + 0.5) sample position.

layout(std140) uniform GlobeParams {
    vec4 Geometry; // hole centre x, hole centre y, hole radius, unused
    vec4 State;    // fill ratio, time in seconds, palette, unused
};

out vec4 fragColor;

float hash(int x, int y) {
    // Unsigned arithmetic wraps like Java's int; >> on int sign-extends like Java's >>
    int n = int(uint(x) * 374761393u + uint(y) * 668265263u);
    n = int(uint(n ^ (n >> 13)) * 1274126177u);
    return float((n ^ (n >> 16)) & 0x7fffffff) * (1.0 / 2147483647.0);
}

float lerpf(float t, float a, float b) {
    return a + (b - a) * t;
}

float smoothf(float v, float a, float b) {
    float t = clamp((v - a) / (b - a), 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

float valueNoise(float x, float y) {
    int x0 = int(floor(x));
    int y0 = int(floor(y));
    float fx = x - float(x0);
    float fy = y - float(y0);
    fx = fx * fx * (3.0 - 2.0 * fx);
    fy = fy * fy * (3.0 - 2.0 * fy);
    float a = hash(x0, y0);
    float b = hash(x0 + 1, y0);
    float c = hash(x0, y0 + 1);
    float d = hash(x0 + 1, y0 + 1);
    return lerpf(fy, lerpf(fx, a, b), lerpf(fx, c, d));
}

// Substance-like Clouds 2: a few value-noise octaves, unique seed per RGB channel.
float clouds2(float x, float y, float seed) {
    float n = 0.50 * valueNoise(x + seed * 0.37, y + seed * 0.19)
            + 0.28 * valueNoise(x * 2.03 + seed * 1.17, y * 2.03)
            + 0.15 * valueNoise(x * 4.07 + seed * 2.41, y * 4.07)
            + 0.07 * valueNoise(x * 8.13 + seed * 0.63, y * 8.13);
    return clamp(n, 0.0, 1.0);
}

void main() {
    float cx = Geometry.x;
    float cy = Geometry.y;
    float r = Geometry.z;
    float ratio = State.x;
    float time = State.y;
    int palette = int(State.z + 0.5);

    // Mana stays cobalt blue at every level; the counter supplies the low-resource warning.
    vec3 deep = vec3(3.0, 10.0, 48.0);
    vec3 mid = vec3(12.0, 62.0, 190.0);
    vec3 hot = vec3(35.0, 133.0, 248.0);
    vec3 core = vec3(116.0, 204.0, 255.0);
    if (palette == 1) {
        deep = vec3(75.0, 16.0, 2.0);
        mid = vec3(225.0, 85.0, 8.0);
        hot = vec3(255.0, 175.0, 30.0);
        core = vec3(255.0, 240.0, 135.0);
    } else if (palette == 2) {
        deep = vec3(3.0, 30.0, 60.0);
        mid = vec3(20.0, 135.0, 215.0);
        hot = vec3(90.0, 210.0, 255.0);
        core = vec3(205.0, 245.0, 255.0);
    }

    // Fill line in v-space: +1 empty (bottom), -1 full (top).
    float fill = 1.0 - 2.0 * ratio;
    float rot = time * 0.14;
    float cs = cos(rot);
    float sn = sin(rot);

    float u = (gl_FragCoord.x - cx) / r;
    float v = (gl_FragCoord.y - cy) / r;
    float d2 = u * u + v * v;
    if (d2 >= 1.0) {
        fragColor = vec4(0.0);
        return;
    }
    float radial = sqrt(d2);
    float hemi = sqrt(max(0.0, 1.0 - d2));

    float tile = lerpf(hemi, 1.0, 2.0);
    float lu = u * tile;
    float lv = v * tile;
    float ru = lu * cs - lv * sn;
    float rv = lu * sn + lv * cs;

    float nR = clouds2(ru * 1.85 + time * 0.07, rv * 1.85 + time * 0.035, 11.0);
    float nG = clouds2(ru * 2.70 - time * 0.055, rv * 2.70 + time * 0.09, 29.0);
    float nB = clouds2(ru * 3.85 + time * 0.11, rv * 3.85 - time * 0.06, 47.0);

    float wave1 = fill
            + 0.022 * sin(u * 6.1 + time * 1.85)
            + 0.009 * sin(u * 13.4 + time * 2.45);
    float wave2 = fill
            + 0.018 * sin(-u * 7.6 + time * 2.18)
            + 0.007 * sin(-u * 15.2 + time * 1.62);
    float mask1 = smoothf(v - wave1, -0.016, 0.016);
    float mask2 = smoothf(v - wave2, -0.016, 0.016);
    float backWave = clamp(mask1 - mask2, 0.0, 1.0);
    float liquid = ratio <= 0.0 ? 0.0 : ratio >= 1.0 ? 1.0 : mask1;

    float density = nR * 0.42 + nG * 0.35 + nB * 0.23;
    density = clamp(density * 1.28, 0.0, 1.0);
    float wisps = smoothf(nG, 0.46, 0.90);
    float cores = pow(smoothf(nB, 0.64, 0.96) * wisps, 1.4);

    float bgShade = 0.62 + 0.38 * radial;
    vec3 back = vec3(
            lerpf(backWave * 0.55, deep.r * bgShade, mid.r),
            lerpf(backWave * 0.55, deep.g * bgShade, mid.g),
            lerpf(backWave * 0.55, deep.b * bgShade, mid.b));

    vec3 c = mix(deep, mid, density);
    c = mix(c, hot, wisps * 0.72);
    c = mix(c, core, cores * 0.55);
    c = mix(back, c, liquid);

    float axis = (u + v) * 0.70710678;
    float volume = smoothf(axis, -0.90, 1.00) * hemi;
    c *= lerpf(volume, 1.08, 0.70);

    float dist = v - wave1;
    if (dist > -0.02 && dist < 0.11) {
        float s = 1.0 - clamp(dist / 0.11, 0.0, 1.0);
        c = mix(c, hot, s * 0.32);
    }

    // Dark rim and brighter lower belly give the liquid the depth of a glass globe.
    float rimShade = 0.30 + 0.70 * hemi;
    float belly = exp(-((u - 0.10) * (u - 0.10) * 4.0 + (v - 0.45) * (v - 0.45) * 7.0));
    c = c * rimShade + belly * vec3(palette == 1 ? 34.0 : 7.0, 25.0, palette == 1 ? 7.0 : 34.0);
    float meniscus = 0.0;
    if (ratio > 0.0 && ratio < 1.0) {
        float m = (v - wave1) / 0.014;
        meniscus = exp(-(m * m)) * hemi;
    }
    vec3 meniscusColor = palette == 0 ? vec3(120.0, 215.0, core.b) : core;
    c = mix(c, meniscusColor, meniscus * 0.65);
    // The empty chamber is smoked glass, not a transparent hole through the HUD.
    c = mix(vec3(5.0 + hemi * 5.0, 8.0 + hemi * 8.0, 17.0 + hemi * 13.0), c, liquid);
    float reflection = exp(-((u + 0.36) * (u + 0.36) / 0.018 + (v + 0.48) * (v + 0.48) / 0.040));
    float glassRim = smoothf(radial, 0.84, 0.98) * (1.0 - smoothf(radial, 0.985, 1.0))
            * clamp((-u - v) * 0.5, 0.0, 1.0);
    float glass = clamp(reflection * 0.72 + glassRim * 0.36, 0.0, 0.8);
    c = mix(c, vec3(198.0, 225.0, core.b), glass);

    float edge = smoothf(1.0 - radial, 0.0, 0.01);
    int alpha = clamp(int(edge * 255.0), 0, 255);
    if (alpha <= 0) {
        fragColor = vec4(0.0);
        return;
    }
    // Truncate like the CPU path's (int) cast before normalising to the RGBA8 target
    vec3 rgb = clamp(trunc(c), 0.0, 255.0);
    fragColor = vec4(rgb, float(alpha)) / 255.0;
}
