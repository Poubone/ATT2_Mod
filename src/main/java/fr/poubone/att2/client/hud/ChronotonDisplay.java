package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.data.ScoreCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;

public class ChronotonDisplay {
    private static int displayedValue = 0;
    private static int animationTimer = 0;
    private static final int MAX_TICKS = 20;
    public static float iconX = 400f;
    public static float iconY = 40f;
    private static int lastScore = Integer.MIN_VALUE;

    public static void reset() {
        displayedValue = 0;
        animationTimer = 0;
        lastScore = Integer.MIN_VALUE;
    }

    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        var live = ScoreCache.get("CHRONOTON");
        HudLayout.Box box = HudLayout.box(HudLayout.CHRONOTON);
        float scale = HudLayout.contentScale(HudLayout.CHRONOTON, 120, 20);
        iconX = box.x();
        iconY = box.y();
        if (live.isEmpty()) {
            lastScore = Integer.MIN_VALUE;
            displayedValue = 0;
            animationTimer = 0;
            HudDrawUtils.drawHUDValue(context, "null", 0, MAX_TICKS,
                    box.x(), box.y(), scale, Items.GOLD_NUGGET, 0xFFFFD54A, false);
            return;
        }
        int score = live.getAsInt();
        if (lastScore == Integer.MIN_VALUE) displayedValue = score;
        if (lastScore != Integer.MIN_VALUE && score > lastScore && !HudFx.recentlySpawnedPickupFx()) {
            int extra = Math.min(8, Math.max(2, (score - lastScore) / 4));
            float fromX = client.getWindow().getGuiScaledWidth() * 0.5f;
            float fromY = client.getWindow().getGuiScaledHeight() * 0.55f;
            for (int i = 0; i < extra; i++) {
                HudFx.flyingCoin(fromX + (float) (Math.random() - 0.5) * 40, fromY + (float) (Math.random() - 0.5) * 20, iconX, iconY);
            }
        }
        if (displayedValue != score) {
            if (score > displayedValue && animationTimer == 0) {
                client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.55f, 0.22f));
            }
            animationTimer = MAX_TICKS;
            int diff = score - displayedValue;
            int step = Math.max(1, Math.abs(diff) / 10);
            if (Math.abs(diff) < 20) step = 1;
            displayedValue += Integer.signum(diff) * step;
            if ((diff > 0 && displayedValue > score) || (diff < 0 && displayedValue < score)) {
                displayedValue = score;
            }
        }
        lastScore = score;

        HudDrawUtils.drawHUDValue(context, displayedValue, animationTimer, MAX_TICKS,
                box.x(), box.y(), scale, Items.GOLD_NUGGET, 0xFFFFD54A, true);
        if (animationTimer > 0) animationTimer--;
    }
}
