package fr.poubone.att2.client.shop;

import fr.poubone.att2.client.quest.QuestBookSkin;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * A faded mock-up of a merchant stall at the chosen menu size and items per row, drawn over the settings
 * while those sliders move. It shares {@link ShopViewport} and {@link ShopGrid} with {@link ShopScreen}, so the
 * window and cards land where the real stall puts them; the rest of the chrome is outlined, not filled in.
 */
public final class ShopPreview {
    /** Opacity of the mock-up at full strength: about three quarters see-through. */
    private static final float OPACITY = 0.3f;

    private ShopPreview() {
    }

    /** @param fade 0..1, multiplied into the opacity so the preview can fade out */
    public static void render(GuiGraphics g, int width, int height, int columns, int sizePercent, float fade) {
        Minecraft client = Minecraft.getInstance();
        float scale = ShopViewport.fit(width, height, 640, 392, sizePercent).scale() * (640f / 1440f);
        int offsetX = (width - size(1440, scale)) / 2, offsetY = (height - size(880, scale)) / 2;
        int panelTint = tint(OPACITY * fade), partTint = tint(OPACITY * 1.2f * fade);
        int textColor = ARGB.color(Math.round(255 * Math.min(1f, OPACITY * 2f * fade)), 0x38, 0x2B, 0x21);
        int edge = Math.max(1, Math.min(6, size(880, scale) / 30));
        g.nextStratum();

        int panelW = size(1440, scale), panelH = size(880, scale);
        QuestBookSkin.panel(g, "cover", offsetX, offsetY, panelW, panelH, 100, 100, edge, panelTint);
        int inset = Math.max(3, panelW / 90), top = panelH * 140 / 880;
        QuestBookSkin.panel(g, "page", offsetX + inset, offsetY + top, panelW - 2 * inset, panelH - top - inset,
                100, 100, edge, panelTint);
        ShopTheme.text(g, ModLanguageManager.get("screen.hud_config.shop_preview"),
                offsetX + Math.round(137 * scale), offsetY + Math.round(61 * scale), size(900, scale), scale * 4.5f,
                ARGB.color(ARGB.alpha(textColor), 0xF0, 0xE3, 0xCC), false);

        for (int i = 0; i < 4; i++) {
            button(g, offsetX, offsetY, scale, 48, 202 + i * 72, 250, 58, edge, partTint);
        }
        for (int i = 0; i < 3; i++) {
            button(g, offsetX, offsetY, scale, 48 + i * 438, 774, 420, 50, edge, partTint);
        }
        button(g, offsetX, offsetY, scale, 1122, 158, 42, 38, edge, partTint);
        button(g, offsetX, offsetY, scale, 1294, 158, 42, 38, edge, partTint);

        ShopGrid grid = ShopGrid.fitting(columns, scale * (float) client.getWindow().getGuiScale());
        int cardW = size(Math.round(grid.cardWidth()), scale), cardH = size(Math.round(grid.cardHeight()), scale);
        float s = cardW / (float) ShopSlotButton.CARD_WIDTH;
        int cardEdge = Math.max(1, Math.min(6, Math.min(cardW, cardH) / 3));
        for (int i = 0; i < grid.perPage(); i++) {
            int x = offsetX + Math.round(grid.cardX(i) * scale), y = offsetY + Math.round(grid.cardY(i) * scale);
            QuestBookSkin.panel(g, "page", x, y, cardW, cardH, 100, 100, cardEdge, partTint);
            int icon = Math.max(1, Math.round(90 * s));
            g.fill(x + (cardW - icon) / 2, y + Math.round(38 * s), x + (cardW + icon) / 2, y + Math.round(38 * s) + icon,
                    ARGB.color(ARGB.alpha(textColor) / 2, 0x70, 0x5E, 0x47));
            int lineW = Math.round(180 * s), lineY = y + Math.round(146 * s);
            g.fill(x + (cardW - lineW) / 2, lineY, x + (cardW + lineW) / 2, lineY + Math.max(1, Math.round(10 * s)), textColor);
            int buyY = y + Math.round(202 * s);
            QuestBookSkin.panel(g, "button", x + Math.round(18 * s), buyY, Math.round(276 * s), Math.round(38 * s),
                    190, 49, cardEdge, partTint);
        }
        ShopTheme.text(g, Component.literal(ModLanguageManager.format("screen.hud_config.shop_preview.per_page",
                        "count", grid.perPage())),
                offsetX + Math.round(364 * scale), offsetY + Math.round(164 * scale), size(725, scale), scale * 2.7f,
                textColor, false);
    }

    private static void button(GuiGraphics g, int offsetX, int offsetY, float scale, int x, int y, int w, int h,
                               int edge, int tint) {
        QuestBookSkin.panel(g, "button", offsetX + Math.round(x * scale), offsetY + Math.round(y * scale),
                size(w, scale), size(h, scale), 190, 49, edge, tint);
    }

    private static int size(int value, float scale) {
        return Math.max(1, Math.round(value * scale));
    }

    private static int tint(float opacity) {
        return ARGB.color(Math.round(255 * Math.max(0f, Math.min(1f, opacity))), 0xFF, 0xFF, 0xFF);
    }
}
