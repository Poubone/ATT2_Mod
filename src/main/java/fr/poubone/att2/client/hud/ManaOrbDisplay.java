package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.data.DahalAmount;
import fr.poubone.att2.client.data.MapStatBar;
import fr.poubone.att2.client.data.ScoreCache;
import fr.poubone.att2.client.util.ModTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

import java.util.OptionalInt;

/**
 * Glass Dahäl orb. Liquid is a spherical globe (Diablo / Path of Exile style), not a flat fill.
 */
public class ManaOrbDisplay {
    private static final int ORB_SRC = 256;
    private static final ManaGlobe GLOBE = new ManaGlobe("dahal_globe", 0);
    /**
     * Inner metal aperture of {@code cadre_vide.png}. The wings and knotwork sit the
     * circle above the texture center; leftover black from the hole punch is gone.
     */
    private static final float HOLE_CX = 127.47f;
    private static final float HOLE_CY = 114.75f;
    private static final float HOLE_R = 84.0f;

    public static void reset() {
        GLOBE.reset();
        TemperatureDisplay.reset();
    }

    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        OptionalInt currentOpt = ScoreCache.get("DAHAL");
        OptionalInt maxOpt = ScoreCache.get("DAHALMAX");
        if (currentOpt.isEmpty() && maxOpt.isPresent() && MapStatBar.hasDahalProgress()) {
            currentOpt = DahalAmount.current(MapStatBar.dahalProgress(), maxOpt.getAsInt());
        }
        int current = currentOpt.orElse(0);
        int max = Math.max(1, maxOpt.orElse(1));

        HudLayout.Box box = HudLayout.box(HudLayout.MANA);
        int size = Math.max(24, Math.round(ORB_SRC * HudLayout.contentScale(HudLayout.MANA, ORB_SRC, ORB_SRC + 12)));
        int x = box.x() + (box.w() - size) / 2;
        int y = box.y() + (box.h() - size) / 2;

        float ratio = currentOpt.isPresent() && maxOpt.isPresent()
                ? Mth.clamp(current / (float) max, 0f, 1f) : 0f;
        double time = Util.getMillis() / 1000.0;
        float danger = ratio < 0.28f ? Mth.clamp((0.28f - ratio) / 0.28f, 0f, 1f) : 0f;

        float scale = size / (float) ORB_SRC;
        float cx = x + HOLE_CX * scale;
        float cy = y + HOLE_CY * scale;
        float radius = HOLE_R * scale;

        if (ratio > 0.15f) {
            float pulse = ratio >= 0.98f ? 0.55f + 0.45f * (float) Math.sin(time * 4.2) : 0.35f + 0.15f * ratio;
            int glowSize = Math.max(8, (int) (radius * 2.7f * (1.04f + 0.08f * pulse)));
            int gx = Math.round(cx - glowSize * 0.5f);
            int gy = Math.round(cy - glowSize * 0.5f);
            int glowColor = ARGB.color((int) (pulse * (ratio >= 0.98f ? 120 : 70)),
                    55, 120, 255);
            context.blit(RenderPipelines.GUI_TEXTURED, ModTextures.FX_GLOW,
                    gx, gy, 0f, 0f, glowSize, glowSize, glowSize, glowSize, glowColor);
        }

        GLOBE.bake(ratio, danger, time, ORB_SRC, HOLE_CX, HOLE_CY, HOLE_R);
        GLOBE.blitLiquid(context, x, y, size);

        context.blit(RenderPipelines.GUI_TEXTURED, ModTextures.MANA_FRAME,
                x, y, 0f, 0f, size, size, ORB_SRC, ORB_SRC, ORB_SRC, ORB_SRC);
        String text = (currentOpt.isPresent() ? Integer.toString(current) : "null") + "/"
                + (maxOpt.isPresent() ? Integer.toString(maxOpt.getAsInt()) : "null");
        int textWidth = client.font.width(text);
        int textColor = danger > 0.4f ? lerpRgb(0xFFFFFFFF, 0xFFFF6B6B, danger) : 0xFFFFFFFF;
        context.drawString(client.font, text, x + (size - textWidth) / 2, y + size - 4, textColor, true);
    }

    private static int lerpRgb(int from, int to, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
