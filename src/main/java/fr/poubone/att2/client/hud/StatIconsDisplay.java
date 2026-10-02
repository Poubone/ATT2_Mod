package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.data.StatManager;
import fr.poubone.att2.client.util.ModTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class StatIconsDisplay {
    private static final int[] LAST_VALUES = new int[9];
    private static boolean primed;

    public static void reset() {
        primed = false;
    }

    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        Font font = client.font;
        HudLayout.Box box = HudLayout.box(HudLayout.STATS);
        float scale = HudLayout.contentScale(HudLayout.STATS, 126, 40);
        float gap = HudLayout.slot(HudLayout.STATS).gap;
        int numberWidth = font.width("-99");
        StatIconsLayout layout = StatIconsLayout.of(scale, gap, numberWidth);
        int iconSize = layout.iconSize();
        int spacingX = layout.spacingX();
        int spacingY = layout.spacingY();
        int columns = 3;
        int startX = box.x();
        int startY = box.y();

        for (int i = 0; i < StatManager.STAT_KEYS.size(); i++) {
            String key = StatManager.STAT_KEYS.get(i);
            int value = StatManager.total(key);

            int col = i % columns;
            int row = i / columns;
            int x = startX + col * spacingX;
            int y = startY + row * spacingY;

            if (primed && value != LAST_VALUES[i]) {
                int delta = value - LAST_VALUES[i];
                String label = (delta > 0 ? "+" : "") + delta;
                HudFx.floatingText(x + layout.textOffsetX() + 4, y - 2, label, delta > 0 ? 0x55FF55 : 0xFF5555);
            }
            LAST_VALUES[i] = value;

            if (key.equals("DAR")) {
                int frame = (int) ((System.currentTimeMillis() / 100) % 32);
                context.blit(RenderPipelines.GUI_TEXTURED, ModTextures.DAR_SPRITE, x, y, 0f, frame * 32f, iconSize, iconSize, 32, 32, 32, 1024);
            } else {
                Identifier icon = ModTextures.STAT_ICONS.get(key);
                if (icon != null) {
                    context.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0f, 0f, iconSize, iconSize, iconSize, iconSize);
                }
            }

            context.drawString(font, Component.literal(String.valueOf(value)), x + layout.textOffsetX(), y, 0xFFFFFFFF, false);
        }
        primed = true;
    }
}
