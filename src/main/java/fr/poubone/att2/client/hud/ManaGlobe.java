package fr.poubone.att2.client.hud;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Dahäl globe following Allematic's Diablo-like UI orb compositing:
 * RGB cloud noise with spherical lens UVs and dual opposing wave masks.
 * A dark glass chamber stays visible above the blue liquid, including when empty.
 * The disc is shaded in the same 256×256 space as cadre_vide.png so it lines up
 * with the metal aperture (which is not the texture center).
 * <p>
 * The liquid is shaded either on the GPU by {@link GlobeShader} into a render-target texture, or by
 * the original CPU path into a {@link NativeImage} that is then uploaded ({@link HUDConfig#orbGpuRendering}).
 * Both produce the same image; the HUD draws the texture the same way either way.
 */
final class ManaGlobe {
    private final Identifier TEXTURE_ID;
    private final int palette;
    private GlobeTexture texture;
    private GpuBuffer params;
    private NativeImage cpuPixels;
    private boolean lastGpu;
    private int texSize;
    private double lastBakeTime = Double.NEGATIVE_INFINITY;
    private float lastRatio = -1;
    private final int[] ROW_MIN = new int[256];
    private final int[] ROW_MAX = new int[256];
    private int rowsSize;
    private float rowsCx = Float.NaN;
    private float rowsCy = Float.NaN;
    private float rowsR = Float.NaN;

    ManaGlobe(String name, int palette) {
        TEXTURE_ID = Identifier.fromNamespaceAndPath("att2", "dynamic/" + name);
        this.palette = palette;
    }

    void reset() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        Runnable close = () -> {
            if (texture != null) {
                client.getTextureManager().release(TEXTURE_ID);
                texture.close();
                texture = null;
            }
            if (params != null) {
                params.close();
                params = null;
            }
            if (cpuPixels != null) {
                cpuPixels.close();
                cpuPixels = null;
            }
            texSize = 0;
            lastBakeTime = Double.NEGATIVE_INFINITY;
            lastRatio = -1;
            rowsSize = 0;
        };
        if (!RenderSystem.isOnRenderThread()) {
            client.execute(close);
            return;
        }
        close.run();
    }

    Identifier bake(float ratio, float danger, double time, int size, float holeCx, float holeCy, float holeR) {
        HUDConfig config = HUDConfig.get();
        boolean gpu = config.orbsOnGpu();
        int fps = config.effectiveOrbFps();
        double interval = fps > 0 ? 1.0 / fps : 0.0;
        if (texture != null && texSize == size && ratio == lastRatio && gpu == lastGpu
                && time >= lastBakeTime && time - lastBakeTime < interval) return TEXTURE_ID;
        ensure(size);
        lastBakeTime = time;
        lastRatio = ratio;
        lastGpu = gpu;
        updateRows(size, holeCx, holeCy, holeR);
        if (gpu) {
            writeParams(holeCx, holeCy, holeR, ratio, (float) time);
            GlobeShader.render(texture.getTextureView(), params);
        } else {
            NativeImage image = cpuImage(size);
            image.fillRect(0, 0, size, size, 0);
            shade(image, size, ratio, danger, (float) time, holeCx, holeCy, holeR);
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture.getTexture(), image);
        }
        return TEXTURE_ID;
    }

    private NativeImage cpuImage(int size) {
        if (cpuPixels == null || cpuPixels.getWidth() != size) {
            if (cpuPixels != null) {
                cpuPixels.close();
            }
            cpuPixels = new NativeImage(size, size, true);
        }
        return cpuPixels;
    }

    /**
     * Blit only the circular glass spans. Empty texels of a GPU-updated image render as an
     * opaque black bowl in the HUD pass even when their alpha is 0.
     */
    void blitLiquid(GuiGraphics gui, int destX, int destY, int destSize) {
        if (texture == null || texSize <= 0) {
            return;
        }
        float scale = destSize / (float) texSize;
        for (int row = 0; row < texSize; row++) {
            int x0 = ROW_MIN[row];
            int x1 = ROW_MAX[row];
            if (x1 < x0) {
                continue;
            }
            int srcW = x1 - x0 + 1;
            int dx0 = destX + Math.round(x0 * scale);
            int dy0 = destY + Math.round(row * scale);
            int dx1 = destX + Math.round((x0 + srcW) * scale);
            int dy1 = destY + Math.round((row + 1) * scale);
            int dw = Math.max(1, dx1 - dx0);
            int dh = Math.max(1, dy1 - dy0);
            gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID,
                    dx0, dy0, (float) x0, (float) row, dw, dh, srcW, 1, texSize, texSize);
        }
    }

    int resolution() {
        return texSize;
    }

    private void ensure(int size) {
        if (texture != null && texSize == size) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (texture != null) {
            client.getTextureManager().release(TEXTURE_ID);
            texture.close();
        }
        texture = new GlobeTexture(size);
        texSize = size;
        client.getTextureManager().register(TEXTURE_ID, texture);
        if (params == null) {
            params = RenderSystem.getDevice().createBuffer(() -> "att2 globe params",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, GlobeShader.PARAMS_SIZE);
        }
    }

    private void writeParams(float cx, float cy, float r, float ratio, float time) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, GlobeShader.PARAMS_SIZE)
                    .putVec4(cx, cy, r, 0f)
                    .putVec4(ratio, time, palette, 0f)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(params.slice(), data);
        }
    }

    /**
     * Texel spans with non-zero alpha, using the same edge test as the shader. They only depend on
     * the hole geometry, so they're recomputed only when that changes.
     */
    private void updateRows(int size, float cx, float cy, float r) {
        if (rowsSize == size && rowsCx == cx && rowsCy == cy && rowsR == r) {
            return;
        }
        rowsSize = size;
        rowsCx = cx;
        rowsCy = cy;
        rowsR = r;
        Arrays.fill(ROW_MIN, 0, size, size);
        Arrays.fill(ROW_MAX, 0, size, -1);
        for (int y = 0; y < size; y++) {
            float v = (y + 0.5f - cy) / r;
            for (int x = 0; x < size; x++) {
                float u = (x + 0.5f - cx) / r;
                float d2 = u * u + v * v;
                if (d2 >= 1f) {
                    continue;
                }
                float radial = (float) Math.sqrt(d2);
                float edge = smooth(1f - radial, 0f, 0.01f);
                if (Mth.clamp((int) (edge * 255f), 0, 255) <= 0) {
                    continue;
                }
                if (x < ROW_MIN[y]) {
                    ROW_MIN[y] = x;
                }
                if (x > ROW_MAX[y]) {
                    ROW_MAX[y] = x;
                }
            }
        }
    }

    // CPU path: the original per-texel shading, kept for the CPU/GPU setting and as the fallback.

    private void shade(NativeImage image, int size, float ratio, float danger, float time,
                              float cx, float cy, float r) {
        // Mana stays cobalt blue at every level; the counter supplies the low-resource warning.
        float deepR = 3, deepG = 10, deepB = 48;
        float midR = 12, midG = 62, midB = 190;
        float hotR = 35, hotG = 133, hotB = 248;
        float coreR = 116, coreG = 204, coreB = 255;
        if (palette == 1) {
            deepR = 75; deepG = 16; deepB = 2;
            midR = 225; midG = 85; midB = 8;
            hotR = 255; hotG = 175; hotB = 30;
            coreR = 255; coreG = 240; coreB = 135;
        } else if (palette == 2) {
            deepR = 3; deepG = 30; deepB = 60;
            midR = 20; midG = 135; midB = 215;
            hotR = 90; hotG = 210; hotB = 255;
            coreR = 205; coreG = 245; coreB = 255;
        }


        // Fill line in v-space: +1 empty (bottom), -1 full (top).
        float fill = 1f - 2f * ratio;
        float rot = time * 0.14f;
        float cs = Mth.cos(rot);
        float sn = Mth.sin(rot);

        for (int y = 0; y < size; y++) {
            float v = (y + 0.5f - cy) / r;
            for (int x = 0; x < size; x++) {
                float u = (x + 0.5f - cx) / r;
                float d2 = u * u + v * v;
                if (d2 >= 1f) {
                    continue;
                }
                float radial = (float) Math.sqrt(d2);
                float hemi = (float) Math.sqrt(Math.max(0f, 1f - d2));

                float tile = lerp(hemi, 1f, 2f);
                float lu = u * tile;
                float lv = v * tile;
                float ru = lu * cs - lv * sn;
                float rv = lu * sn + lv * cs;

                float nR = clouds2(ru * 1.85f + time * 0.07f, rv * 1.85f + time * 0.035f, 11);
                float nG = clouds2(ru * 2.70f - time * 0.055f, rv * 2.70f + time * 0.09f, 29);
                float nB = clouds2(ru * 3.85f + time * 0.11f, rv * 3.85f - time * 0.06f, 47);

                float wave1 = fill
                        + 0.022f * Mth.sin(u * 6.1f + time * 1.85f)
                        + 0.009f * Mth.sin(u * 13.4f + time * 2.45f);
                float wave2 = fill
                        + 0.018f * Mth.sin(-u * 7.6f + time * 2.18f)
                        + 0.007f * Mth.sin(-u * 15.2f + time * 1.62f);
                float mask1 = smooth(v - wave1, -0.016f, 0.016f);
                float mask2 = smooth(v - wave2, -0.016f, 0.016f);
                float backWave = Mth.clamp(mask1 - mask2, 0f, 1f);
                float liquid = ratio <= 0f ? 0f : ratio >= 1f ? 1f : mask1;

                float density = nR * 0.42f + nG * 0.35f + nB * 0.23f;
                density = Mth.clamp(density * 1.28f, 0f, 1f);
                float wisps = smooth(nG, 0.46f, 0.90f);
                float cores = (float) Math.pow(smooth(nB, 0.64f, 0.96f) * wisps, 1.4);

                float bgShade = 0.62f + 0.38f * radial;
                float br = lerp(backWave * 0.55f, deepR * bgShade, midR);
                float bg = lerp(backWave * 0.55f, deepG * bgShade, midG);
                float bb = lerp(backWave * 0.55f, deepB * bgShade, midB);

                float cr = lerp(density, deepR, midR);
                float cg = lerp(density, deepG, midG);
                float cb = lerp(density, deepB, midB);
                cr = lerp(wisps * 0.72f, cr, hotR);
                cg = lerp(wisps * 0.72f, cg, hotG);
                cb = lerp(wisps * 0.72f, cb, hotB);
                cr = lerp(cores * 0.55f, cr, coreR);
                cg = lerp(cores * 0.55f, cg, coreG);
                cb = lerp(cores * 0.55f, cb, coreB);
                cr = lerp(liquid, br, cr);
                cg = lerp(liquid, bg, cg);
                cb = lerp(liquid, bb, cb);

                float axis = (u + v) * 0.70710678f;
                float volume = smooth(axis, -0.90f, 1.00f) * hemi;
                float shadeMul = lerp(volume, 1.08f, 0.70f);
                cr *= shadeMul;
                cg *= shadeMul;
                cb *= shadeMul;

                float dist = v - wave1;
                if (dist > -0.02f && dist < 0.11f) {
                    float s = 1f - Mth.clamp(dist / 0.11f, 0f, 1f);
                    cr = lerp(s * 0.32f, cr, hotR);
                    cg = lerp(s * 0.32f, cg, hotG);
                    cb = lerp(s * 0.32f, cb, hotB);
                }

                // Dark rim and brighter lower belly give the liquid the depth of a glass globe.
                float rimShade = 0.30f + 0.70f * hemi;
                float belly = (float) Math.exp(-((u - 0.10f) * (u - 0.10f) * 4f
                        + (v - 0.45f) * (v - 0.45f) * 7f));
                cr = cr * rimShade + belly * (palette == 1 ? 34f : 7f);
                cg = cg * rimShade + belly * 25f;
                cb = cb * rimShade + belly * (palette == 1 ? 7f : 34f);
                float meniscus = ratio > 0f && ratio < 1f
                        ? (float) Math.exp(-Math.pow((v - wave1) / 0.014f, 2)) * hemi : 0f;
                cr = lerp(meniscus * 0.65f, cr, palette == 0 ? 120 : coreR);
                cg = lerp(meniscus * 0.65f, cg, palette == 0 ? 215 : coreG);
                cb = lerp(meniscus * 0.65f, cb, coreB);
                // The empty chamber is smoked glass, not a transparent hole through the HUD.
                cr = lerp(liquid, 5f + hemi * 5f, cr);
                cg = lerp(liquid, 8f + hemi * 8f, cg);
                cb = lerp(liquid, 17f + hemi * 13f, cb);
                float reflection = (float) Math.exp(-((u + 0.36f) * (u + 0.36f) / 0.018f
                        + (v + 0.48f) * (v + 0.48f) / 0.040f));
                float glassRim = smooth(radial, 0.84f, 0.98f) * (1f - smooth(radial, 0.985f, 1f))
                        * Mth.clamp((-u - v) * 0.5f, 0f, 1f);
                float glass = Mth.clamp(reflection * 0.72f + glassRim * 0.36f, 0f, 0.8f);
                cr = lerp(glass, cr, 198);
                cg = lerp(glass, cg, 225);
                cb = lerp(glass, cb, coreB);
                float edge = smooth(1f - radial, 0f, 0.01f);
                int alpha = Mth.clamp((int) (edge * 255f), 0, 255);
                if (alpha <= 0) {
                    continue;
                }

                image.setPixel(x, y, ARGB.color(alpha,
                        Mth.clamp((int) cr, 0, 255),
                        Mth.clamp((int) cg, 0, 255),
                        Mth.clamp((int) cb, 0, 255)));
            }
        }
    }

    /** Substance-like Clouds 2: a few value-noise octaves, unique seed per RGB channel. */
    private static float clouds2(float x, float y, int seed) {
        float n = 0.50f * valueNoise(x + seed * 0.37f, y + seed * 0.19f)
                + 0.28f * valueNoise(x * 2.03f + seed * 1.17f, y * 2.03f)
                + 0.15f * valueNoise(x * 4.07f + seed * 2.41f, y * 4.07f)
                + 0.07f * valueNoise(x * 8.13f + seed * 0.63f, y * 8.13f);
        return Mth.clamp(n, 0f, 1f);
    }

    private static float valueNoise(float x, float y) {
        int x0 = Mth.floor(x);
        int y0 = Mth.floor(y);
        float fx = x - x0;
        float fy = y - y0;
        fx = fx * fx * (3f - 2f * fx);
        fy = fy * fy * (3f - 2f * fy);
        float a = hash(x0, y0);
        float b = hash(x0 + 1, y0);
        float c = hash(x0, y0 + 1);
        float d = hash(x0 + 1, y0 + 1);
        return lerp(fy, lerp(fx, a, b), lerp(fx, c, d));
    }

    private static float hash(int x, int y) {
        int n = x * 374761393 + y * 668265263;
        n = (n ^ (n >> 13)) * 1274126177;
        return ((n ^ (n >> 16)) & 0x7fffffff) * (1f / 2147483647f);
    }

    private static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }


    private static float smooth(float v, float a, float b) {
        float t = Mth.clamp((v - a) / (b - a), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    /**
     * Same GPU setup as a GUI resource texture (clamp + nearest). It can be rendered into by
     * {@link GlobeShader} or written from the CPU path's {@link NativeImage}.
     */
    private static final class GlobeTexture extends AbstractTexture {
        private GlobeTexture(int size) {
            var device = RenderSystem.getDevice();
            this.texture = device.createTexture(
                    () -> "att2_dahal_globe",
                    GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_COPY_DST
                            | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_SRC,
                    TextureFormat.RGBA8,
                    size,
                    size,
                    1,
                    1);
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            this.textureView = device.createTextureView(this.texture);
        }
    }
}
