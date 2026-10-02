package fr.poubone.att2.client.hud;

/**
 * Places spell-launcher icons against the near edge of their HUD box
 * (left when the box sits on the left half of the screen, right otherwise).
 */
public final class SpellBarLayout {
    private SpellBarLayout() {
    }

    public static int iconX(int boxX, int boxW, int cell, int screenW, int column, int stride) {
        boolean left = boxX + boxW / 2 < screenW / 2;
        if (left) {
            return boxX + column * stride;
        }
        return boxX + boxW - cell - column * stride;
    }
}
