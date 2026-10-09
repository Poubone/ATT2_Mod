package fr.poubone.att2.client.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents;

import java.util.Locale;

/**
 * Distinguishes the map's restock timer from the clickable "force reset" shop line.
 */
final class ShopTellraws {
    private ShopTellraws() {
    }

    static boolean isForceResetAction(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.contains("force store reset")
                || lower.contains("forcer la ré-initialisation")
                || lower.contains("forcer la re-initialisation")
                || lower.contains("forcer la réinitialisation");
    }

    /**
     * The Dahal stall starts each spell and enhancement line with a bold " ° ": dark red, or dark green once
     * the player has learned the spell (or fully enhanced it).
     */
    static boolean hasOwnedMarker(Component message) {
        if (message == null) return false;
        if (message.getContents() instanceof PlainTextContents plain && plain.text().strip().equals("°")) {
            TextColor color = message.getStyle().getColor();
            if (color != null && color.getValue() == OWNED_MARKER_RGB) return true;
        }
        for (Component sibling : message.getSiblings()) {
            if (hasOwnedMarker(sibling)) return true;
        }
        return false;
    }

    private static final int OWNED_MARKER_RGB = 0x00AA00;

    static boolean isRemainingTimer(String raw) {
        if (raw == null || raw.isBlank()) return false;
        if (isForceResetAction(raw)) return false;
        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.contains("ré-initialisation") || lower.contains("re-initialisation")
                || lower.contains("shop reset") || lower.contains("réinitialisation des magasins");
    }
}
