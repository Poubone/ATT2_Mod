package fr.poubone.att2.client.quest;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Unmodified Kenney RPG UI sprites (CC0); borders are kept intact with nine-slice drawing. */
public final class QuestBookSkin {
    public static final int INK = 0xFF382B21;
    public static final int MUTED = 0xFF705E47;
    public static final int RULE = 0xFFBFA97B;
    public static final int CREAM = 0xFFF5EAD0;
    private QuestBookSkin() {}

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath("att2", "textures/quest_book/kenney/" + name + ".png");
    }

    public static void book(GuiGraphics g, QuestBookLayout l) {
        g.fill(l.x() + 5, l.y() + 6, l.x() + l.width() + 5, l.y() + l.height() + 6, 0x60000000);
        panel(g, "cover", l.x(), l.y(), l.width(), l.height(), 100, 100, 6);
        // Layered paper edges and two separately drawn pages, without negative scaling.
        int py = l.y() + 36, ph = l.height() - 44, pw = l.pageWidth();
        for (int px : new int[]{l.x() + 8, l.x() + l.width() / 2 + 5}) {
            panel(g, "page", px, py + 3, pw, ph, 100, 100, 5);
            panel(g, "page", px, py, pw, ph - 2, 100, 100, 5);
        }
        int spine = l.x() + l.width() / 2;
        g.fill(spine - 4, py + 3, spine + 4, py + ph - 2, 0xFF735138);
        g.fill(spine - 2, py + 4, spine + 1, py + ph - 3, 0xFFA78455);
        // The bookmark is a simple GUI accent, not a generated bitmap.
        g.fill(spine + 12, py - 5, spine + 22, py + 3, 0xFF7C3430);
    }

    public static void button(GuiGraphics g, int x, int y, int w, int h, boolean selected, boolean hovered) {
        String name = selected ? "button_selected" : hovered ? "button_pressed" : "button";
        panel(g, name, x, y, w, h, 190, hovered && !selected ? 45 : 49, 6);
    }

    public static void icon(GuiGraphics g, String name, int x, int y, int w, int h) {
        int sw = name.equals("previous") || name.equals("next") ? 22 : 16;
        int sh = sw == 22 ? 21 : 15;
        g.blit(RenderPipelines.GUI_TEXTURED, texture(name), x, y, 0f, 0f, w, h, sw, sh, sw, sh);
    }

    public static int statusColor(QuestStatus status) {
        return switch (status) {
            case STARTED -> 0xFF845715;
            case COMPLETED -> 0xFF365D35;
            case FAILED -> 0xFF923D32;
            case UNKNOWN -> MUTED;
        };
    }

    public static void panel(GuiGraphics g, String name, int x, int y, int w, int h, int sw, int sh, int edge) {
        int[] dx = {x, x + edge, x + w - edge}, dy = {y, y + edge, y + h - edge};
        int[] dw = {edge, w - 2 * edge, edge}, dh = {edge, h - 2 * edge, edge};
        int[] sx = {0, edge, sw - edge}, sy = {0, edge, sh - edge};
        int[] rw = {edge, sw - 2 * edge, edge}, rh = {edge, sh - 2 * edge, edge};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            if (dw[col] > 0 && dh[row] > 0)
                g.blit(RenderPipelines.GUI_TEXTURED, texture(name), dx[col], dy[row], (float) sx[col], (float) sy[row],
                        dw[col], dh[row], rw[col], rh[row], sw, sh);
        }
    }
}
