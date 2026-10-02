package fr.poubone.att2.client.shop;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Menu-specific ornaments over the supplied shop components. */
final class ShopIdentity {
    private ShopIdentity() {}

    static void render(GuiGraphics g, ShopType theme, int width, int height) {
        int color = theme.titleColor;
        g.fill(22, 21, 51, 50, 0xFFA78455);
        g.fill(22, 21, 51, 22, color);
        g.fill(22, 49, 51, 50, color);
        g.fill(22, 21, 23, 50, color);
        g.fill(50, 21, 51, 50, color);
        if (theme == ShopType.CHARLES) {
            // A five-pip die, echoed along the lower frame.
            die(g, 29, 28, color);
            for (int x = width / 2 - 28; x <= width / 2 + 28; x += 28) die(g, x, height - 27, color);
        } else if (theme == ShopType.RUNES) {
            rune(g, 30, 27, color);
            for (int x = width / 2 - 28; x <= width / 2 + 28; x += 28) rune(g, x, height - 28, color);
        } else {
            g.renderItem(new ItemStack(Items.IRON_PICKAXE), 29, 28);
            for (int x = width / 2 - 28; x <= width / 2 + 28; x += 28) {
                g.fill(x, height - 22, x + 8, height - 18, color);
                g.fill(x + 2, height - 24, x + 6, height - 16, color);
            }
        }
    }

    private static void die(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 15, y + 15, 0xFF735138);
        for (int[] p : new int[][] {{2,2},{10,2},{6,6},{2,10},{10,10}})
            g.fill(x + p[0], y + p[1], x + p[0] + 3, y + p[1] + 3, color);
    }

    private static void rune(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 6, y, x + 8, y + 18, color);
        for (int i = 0; i < 6; i++) {
            g.fill(x + i, y + i + 2, x + i + 2, y + i + 4, color);
            g.fill(x + 12 - i, y + i + 2, x + 14 - i, y + i + 4, color);
        }
    }
}
