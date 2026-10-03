package fr.poubone.att2.client.hud;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
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
 * The liquid is shaded on the GPU by {@link GlobeShader} into a render-target texture,
 * which the HUD then draws like any other texture.
 */
final class ManaGlobe {
    private final Identifier TEXTURE_ID;
    private final int palette;
    private GlobeTexture texture;
    private GpuBuffer params;
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
        if (texture != null && texSize == size && ratio == lastRatio
                && time >= lastBakeTime && time - lastBakeTime < 1.0 / 30.0) return TEXTURE_ID;
        ensure(size);
        lastBakeTime = time;
        lastRatio = ratio;
        updateRows(size, holeCx, holeCy, holeR);
        writeParams(holeCx, holeCy, holeR, ratio, (float) time);
        GlobeShader.render(texture.getTextureView(), params);
        return TEXTURE_ID;
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

    private static float smooth(float v, float a, float b) {
        float t = Mth.clamp((v - a) / (b - a), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    /**
     * Same GPU setup as a GUI resource texture (clamp + nearest), but rendered into by
     * {@link GlobeShader} instead of uploaded from the CPU.
     */
    private static final class GlobeTexture extends AbstractTexture {
        private GlobeTexture(int size) {
            var device = RenderSystem.getDevice();
            this.texture = device.createTexture(
                    () -> "att2_dahal_globe",
                    GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_SRC,
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
