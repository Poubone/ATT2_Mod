package fr.poubone.att2.client.hud;

/**
 * Pixel layout of the 3×3 stat icons: icon on the left, number to its right, columns spaced by {@code gap}.
 */
public final class StatIconsLayout {
    private final int iconSize;
    private final int textOffsetX;
    private final int spacingX;
    private final int spacingY;

    private StatIconsLayout(int iconSize, int textOffsetX, int spacingX, int spacingY) {
        this.iconSize = iconSize;
        this.textOffsetX = textOffsetX;
        this.spacingX = spacingX;
        this.spacingY = spacingY;
    }

    public static StatIconsLayout of(float scale, float gap, int numberWidth) {
        if (scale <= 0f) scale = 1f;
        if (gap <= 0f) gap = 1f;
        int iconSize = Math.max(6, Math.round(8 * scale));
        int textOffsetX = iconSize + 2;
        int minStrideX = textOffsetX + Math.max(0, numberWidth);
        int spacingX = Math.max(minStrideX, Math.round(42 * scale * gap));
        int spacingY = Math.max(Math.max(10, iconSize + 2), Math.round(12 * scale * gap));
        return new StatIconsLayout(iconSize, textOffsetX, spacingX, spacingY);
    }

    public int iconSize() {
        return iconSize;
    }

    public int textOffsetX() {
        return textOffsetX;
    }

    public int spacingX() {
        return spacingX;
    }

    public int spacingY() {
        return spacingY;
    }

    /** Origin of cell {@code index} on a single row (column stride). */
    public int[] cell(int index, int startX, int startY) {
        return new int[]{startX + index * spacingX, startY};
    }
}
