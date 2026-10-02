package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.quest.QuestBookSkin;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

/**
 * Quantity picker popup for Codex crafts — Kenney page panel + button sprites.
 */
public final class CraftQuantityMenu {
    private static final int CELL_W = 36;
    private static final int CELL_H = 22;
    private static final int GAP = 4;
    private static final int PAD = 10;
    private static final int TITLE_H = 14;

    private final int panelW;
    private final int panelH;

    private int max;
    private int menuX;
    private int menuY;
    private IntConsumer onPick;
    private boolean open;
    private int hoverIndex = -1;

    public CraftQuantityMenu(int panelW, int panelH) {
        this.panelW = panelW;
        this.panelH = panelH;
    }

    public boolean isOpen() {
        return open;
    }

    public void open(int max, int anchorX, int anchorY, IntConsumer onPick) {
        this.max = max;
        this.onPick = onPick;
        this.hoverIndex = -1;
        int totalW = rowWidth();
        int totalH = TITLE_H + CELL_H + 2 * PAD + 4;
        int x = anchorX;
        int y = anchorY + 6;
        if (x + totalW > panelW) {
            x = panelW - totalW;
        }
        if (y + totalH > panelH) {
            y = panelH - totalH;
        }
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        this.menuX = x;
        this.menuY = y;
        this.open = true;
    }

    public void close() {
        open = false;
        onPick = null;
        hoverIndex = -1;
    }

    public boolean keyEscape() {
        if (!open) {
            return false;
        }
        close();
        return true;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!open || button != 0) {
            return false;
        }
        if (!containsMenu(mx, my)) {
            return false;
        }
        for (int i = 0; i < 5; i++) {
            if (!cellHit(i, mx, my)) {
                continue;
            }
            if (cellEnabled(i)) {
                int preset = i < CraftQuantity.PRESETS.length ? CraftQuantity.PRESETS[i] : 0;
                int qty = CraftQuantity.resolveChoice(preset, max);
                if (qty > 0 && onPick != null) {
                    onPick.accept(qty);
                }
                close();
            }
            return true;
        }
        return true;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!open) {
            return;
        }
        hoverIndex = -1;
        for (int i = 0; i < 5; i++) {
            if (cellEnabled(i) && cellHit(i, mouseX, mouseY)) {
                hoverIndex = i;
                break;
            }
        }

        int totalW = rowWidth();
        int totalH = TITLE_H + CELL_H + 2 * PAD + 4;
        graphics.fill(menuX + 2, menuY + 3, menuX + totalW + 2, menuY + totalH + 3, 0x50000000);
        QuestBookSkin.panel(graphics, "cover", menuX, menuY, totalW, totalH, 100, 100, 6);
        QuestBookSkin.panel(graphics, "page", menuX + 3, menuY + 3, totalW - 6, totalH - 6, 100, 100, 4);

        Component title = ModLanguageManager.get("rune_codex.qty.title");
        ShopTheme.text(graphics, title, menuX + PAD, menuY + PAD - 1, totalW - 2 * PAD, 1f, QuestBookSkin.INK, true);

        for (int i = 0; i < 5; i++) {
            int cx = cellX(i);
            int cy = cellY();
            boolean enabled = cellEnabled(i);
            boolean hovered = i == hoverIndex;
            QuestBookSkin.button(graphics, cx, cy, CELL_W, CELL_H, false, hovered && enabled);
            if (!enabled) {
                graphics.fill(cx + 2, cy + 2, cx + CELL_W - 2, cy + CELL_H - 2, 0x88EDE2C8);
            }
            int color = enabled ? QuestBookSkin.INK : QuestBookSkin.MUTED;
            ShopTheme.text(graphics, cellLabel(i), cx, cy + 6, CELL_W, 1f, color, true);
        }
    }

    private int rowWidth() {
        return 5 * CELL_W + 4 * GAP + 2 * PAD;
    }

    private int cellX(int index) {
        return menuX + PAD + index * (CELL_W + GAP);
    }

    private int cellY() {
        return menuY + PAD + TITLE_H;
    }

    private boolean cellHit(int index, double mx, double my) {
        int cx = cellX(index);
        int cy = cellY();
        return mx >= cx && mx < cx + CELL_W && my >= cy && my < cy + CELL_H;
    }

    private boolean containsMenu(double mx, double my) {
        int totalW = rowWidth();
        int totalH = TITLE_H + CELL_H + 2 * PAD + 4;
        return mx >= menuX && mx < menuX + totalW && my >= menuY && my < menuY + totalH;
    }

    private boolean cellEnabled(int index) {
        if (index < CraftQuantity.PRESETS.length) {
            return CraftQuantity.presetEnabled(CraftQuantity.PRESETS[index], max);
        }
        return max > 0;
    }

    private Component cellLabel(int index) {
        if (index < CraftQuantity.PRESETS.length) {
            return Component.literal(String.valueOf(CraftQuantity.PRESETS[index]));
        }
        return ModLanguageManager.get("rune_codex.qty.all");
    }
}
