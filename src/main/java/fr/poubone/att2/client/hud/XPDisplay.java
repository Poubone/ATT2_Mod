package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.data.ScoreCache;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.util.OptionalInt;

public class XPDisplay {
    static float iconX;
    static float iconY;
    private static int lastLevel = Integer.MIN_VALUE;
    private static int lastMasterLevel = Integer.MIN_VALUE;
    private static float flash = 0f;

    public static void reset() {
        lastLevel = Integer.MIN_VALUE;
        lastMasterLevel = Integer.MIN_VALUE;
        flash = 0f;
    }

    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        OptionalInt requiredXpOpt = ScoreCache.get("LVL_UPGRADE_REQ");
        OptionalInt currentLvlOpt = ScoreCache.get("GAMELEVEL");
        OptionalInt masterLvlOpt = ScoreCache.get("LEVELMASTER");
        String requiredXp = requiredXpOpt.isPresent() ? Integer.toString(requiredXpOpt.getAsInt()) : "null";
        Integer currentLvl = currentLvlOpt.isPresent() ? currentLvlOpt.getAsInt() : null;
        Integer masterLvl = masterLvlOpt.isPresent() ? masterLvlOpt.getAsInt() : null;
        int currentXp = client.player.experienceLevel;

        boolean levelIncreased = currentLvl != null && lastLevel != Integer.MIN_VALUE && currentLvl > lastLevel;
        boolean masterIncreased = masterLvl != null && lastMasterLevel != Integer.MIN_VALUE && masterLvl > lastMasterLevel;
        if (levelIncreased || masterIncreased) {
            flash = 1.0f;
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.35f, 0.35f));
            HudFx.burstToward(iconX, iconY, 18, 0x4DFF4D, true);
        }
        lastLevel = currentLvl == null ? Integer.MIN_VALUE : currentLvl;
        lastMasterLevel = masterLvl == null ? Integer.MIN_VALUE : masterLvl;
        if (flash > 0.02f) {
            flash *= 0.88f;
        } else {
            flash = 0f;
        }

        String display = ModLanguageManager.get("level.label").getString() + " "
                + (currentLvl == null ? "null" : currentLvl) + " : " + currentXp + "/" + requiredXp;
        if (masterLvl != null && masterLvl > 0) {
            display += "  " + ModLanguageManager.format("level.master", "n", masterLvl);
        }
        int color = flash > 0.05f
                ? ARGB.color(255,
                (int) Mth.lerp(flash, 0, 255),
                (int) Mth.lerp(flash, 255, 255),
                (int) Mth.lerp(flash, 0, 255))
                : 0xFF00FF00;
        HudLayout.Box box = HudLayout.box(HudLayout.XP);
        float scale = HudLayout.contentScale(HudLayout.XP, 210, 18);
        iconX = box.x();
        iconY = box.y();
        HudDrawUtils.drawXPHUDValue(context, display, box.x(), box.y(), scale, color);
        if (flash > 0.08f) {
            int a = (int) (flash * 90);
            context.fill(box.x() - 2, box.y() - 2, box.x() + box.w() + 2, box.y() + box.h() + 2,
                    ARGB.color(a, 180, 255, 120));
        }
    }
}
