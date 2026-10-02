package fr.poubone.att2.client.screen;

import fr.poubone.att2.client.quest.QuestBookSkin;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Kenney float panel for stat effect preview on card hover. */
public final class StatHoverTooltip {
    private static final int PAD = 10;
    private static final int TITLE_H = 14;
    private static final int LINE_H = 11;
    private static final int MIN_BOX_W = 120;
    /** Same idea as shop / vanilla item tips: sit next to the cursor, not far below the card. */
    private static final int CURSOR_OFFSET_X = 12;
    private static final int CURSOR_OFFSET_Y = 12;

    private StatHoverTooltip() {
    }

    /**
     * Draws a Kenney tip near the mouse (shop-style), clamped to the screen.
     */
    public static void render(GuiGraphics g, int screenW, int screenH,
                              int mouseX, int mouseY,
                              Component current, Component next, Component costOrNull) {
        render(g, screenW, screenH, mouseX, mouseY, current, next, costOrNull, List.of());
    }

    public static void render(GuiGraphics g, int screenW, int screenH,
                              int mouseX, int mouseY,
                              Component current, Component next, Component costOrNull,
                              List<Component> sources) {
        Font font = Minecraft.getInstance().font;
        Component title = ModLanguageManager.get("screen.stat_upgrade.tooltip.title");
        if (sources == null) sources = List.of();

        int textW = font.width(title);
        textW = Math.max(textW, font.width(current));
        textW = Math.max(textW, font.width(next));
        if (costOrNull != null) textW = Math.max(textW, font.width(costOrNull));
        for (Component line : sources) {
            textW = Math.max(textW, font.width(line));
        }
        int boxW = Math.max(MIN_BOX_W, textW + 2 * PAD);

        int bodyLines = (costOrNull == null ? 2 : 3) + sources.size();
        int boxH = TITLE_H + bodyLines * LINE_H + 2 * PAD + 4;

        int x = mouseX + CURSOR_OFFSET_X;
        int y = mouseY + CURSOR_OFFSET_Y;
        if (x + boxW > screenW) {
            x = mouseX - CURSOR_OFFSET_X - boxW;
        }
        if (y + boxH > screenH) {
            y = mouseY - CURSOR_OFFSET_Y - boxH;
        }
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        if (x + boxW > screenW) {
            x = Math.max(0, screenW - boxW);
        }
        if (y + boxH > screenH) {
            y = Math.max(0, screenH - boxH);
        }

        g.fill(x + 2, y + 3, x + boxW + 2, y + boxH + 3, 0x50000000);
        QuestBookSkin.panel(g, "cover", x, y, boxW, boxH, 100, 100, 6);
        QuestBookSkin.panel(g, "page", x + 3, y + 3, boxW - 6, boxH - 6, 100, 100, 4);

        int innerW = boxW - 2 * PAD;
        ShopTheme.text(g, title, x + PAD, y + PAD - 1, innerW, 1f, QuestBookSkin.MUTED, false);

        int lineY = y + PAD + TITLE_H;
        if (costOrNull != null) {
            ShopTheme.text(g, costOrNull, x + PAD, lineY, innerW, 1f, QuestBookSkin.INK, false);
            lineY += LINE_H;
        }
        ShopTheme.text(g, current, x + PAD, lineY, innerW, 1f, QuestBookSkin.INK, false);
        lineY += LINE_H;
        ShopTheme.text(g, next, x + PAD, lineY, innerW, 1f, QuestBookSkin.INK, false);
        for (Component line : sources) {
            lineY += LINE_H;
            ShopTheme.text(g, line, x + PAD, lineY, innerW, 1f, QuestBookSkin.INK, false);
        }
    }
}
