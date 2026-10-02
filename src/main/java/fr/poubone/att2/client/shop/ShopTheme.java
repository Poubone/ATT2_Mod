package fr.poubone.att2.client.shop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Draw the supplied components without baking example labels or items into the UI. */
public final class ShopTheme {
    private ShopTheme() {}

    public static void text(GuiGraphics graphics, Component text, int x, int y, int width,
                     float scale, int color, boolean centered) {
        if (width <= 0) return;
        var font = Minecraft.getInstance().font;
        int limit = Math.max(1, (int) (width / scale));
        // Keep the map's formatting (rarity, translated item names, etc.).
        Component line = text;
        if (font.width(text) > limit) {
            var clipped = Component.empty();
            font.substrByWidth(text, Math.max(0, limit - font.width("…"))).visit((style, part) -> {
                clipped.append(Component.literal(part).withStyle(style));
                return java.util.Optional.empty();
            }, net.minecraft.network.chat.Style.EMPTY);
            line = clipped.append("…");
        }
        float left = centered ? x + (width - font.width(line) * scale) / 2f : x;
        graphics.pose().pushMatrix();
        graphics.pose().translate(left, y);
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, line, 0, 0, color, false);
        graphics.pose().popMatrix();
    }
}
