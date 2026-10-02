package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.data.TemperatureModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Temperature globe using Dahal glass: warm or cold liquid and the exact signed TAB value. */
public final class TemperatureDisplay {
    private static final ManaGlobe WARM = new ManaGlobe("temperature_warm", 1);
    private static final ManaGlobe COLD = new ManaGlobe("temperature_cold", 2);

    private TemperatureDisplay() {}

    public static void reset() {
        WARM.reset();
        COLD.reset();
    }

    public static void render(GuiGraphics graphics, boolean preview) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        var score = TemperatureModel.read(client.level.getScoreboard(), client.player);
        if (score.isEmpty() && !preview) return;
        int value = score.orElse(650); // Only the HUD editor gets an illustrative value when TAB is unavailable.
        int stage = TemperatureModel.stage(value);
        float seconds = (client.level.getGameTime()
                + client.getDeltaTracker().getGameTimeDeltaPartialTick(false)) / 20f;
        String label = TemperatureModel.label(value);
        int textWidth = client.font.width(label);
        int contentWidth = Math.max(42, textWidth);
        var box = HudLayout.box(HudLayout.TEMPERATURE);
        float scale = HudLayout.contentScale(HudLayout.TEMPERATURE, contentWidth, 44 + client.font.lineHeight);
        graphics.pose().pushMatrix();
        graphics.pose().translate(box.x() + (box.w() - contentWidth * scale) / 2f, box.y());
        graphics.pose().scale(scale, scale);
        graphics.pose().translate((contentWidth - 42) / 2f, 0);
        float ratio = TemperatureModel.fillRatio(value);
        ManaGlobe globe = value < 0 ? COLD : WARM;
        // A centered glass aperture with its own compact, wingless steel bezel.
        globe.bake(ratio, 0, seconds, 128, 64, 64, 55.5f);
        globe.blitLiquid(graphics, 0, 0, 42);
        drawFrame(graphics);
        int color = stage == 0 ? 0xFFBCE7BC : value < 0 ? 0xFF8CDFFF : 0xFFFFAA55;
        if (stage == 3) color = value < 0 ? 0xFFD7F7FF : 0xFFFF6650;
        graphics.drawString(client.font, label, (42 - textWidth) / 2, 44, color, true);
        graphics.pose().popMatrix();
    }

    /** Pixel-aligned concentric bevels; the upper-left highlight matches the glass lighting. */
    private static void drawFrame(GuiGraphics graphics) {
        for (int y = 0; y < 42; y++) {
            for (int x = 0; x < 42; x++) {
                float dx = x + 0.5f - 21;
                float dy = y + 0.5f - 21;
                float radius = (float) Math.sqrt(dx * dx + dy * dy);
                if (radius < 18 || radius >= 21) continue;
                float light = (-dx - dy) / (radius * 1.414214f);
                int grey = radius >= 20 ? 35
                        : radius >= 19 ? (int) (130 + light * 65)
                        : (int) (65 - light * 25);
                int color = 0xFF000000 | (grey << 16) | ((grey + 5) << 8) | (grey + 10);
                graphics.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

}
