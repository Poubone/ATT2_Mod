package fr.poubone.att2.client.rune;

import net.minecraft.ChatFormatting;

import java.util.OptionalInt;

/** Pure helpers for Codex card grid + stock coloring (no GUI). */
public final class RuneCardLogic {
    public static final int CARDS_PER_PAGE = 6;

    private RuneCardLogic() {
    }

    public static int pageCount(int itemCount) {
        if (itemCount <= 0) return 1;
        return (itemCount + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE;
    }

    public static int clampPage(int page, int pageCount) {
        if (pageCount <= 0) return 0;
        return Math.max(0, Math.min(page, pageCount - 1));
    }

    public static int gridCol(int localIndex) {
        return localIndex % 3;
    }

    public static int gridRow(int localIndex) {
        return localIndex / 3;
    }

    /** Green = inv enough; gold = pouch covers; red = still short; gray = pouch unknown. */
    public static ChatFormatting stockColor(int have, int need, OptionalInt pouch) {
        if (have >= need) return ChatFormatting.GREEN;
        if (pouch.isEmpty()) return ChatFormatting.GRAY;
        if (have + pouch.getAsInt() >= need) return ChatFormatting.GOLD;
        return ChatFormatting.RED;
    }
}
