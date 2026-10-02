package fr.poubone.att2.client.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * City arrival banners: swallow the vanilla title/subtitle and show a Kenney {@link ModToast}.
 */
public final class CityToast {
    private CityToast() {
    }

    public static void reset() {
        // Banner lives in ModToast; nothing extra to clear beyond join/disconnect ModToast.reset().
    }

    public static void tick() {
        // ModToast.tick() is driven from the client tick loop.
    }

    public static boolean isActive() {
        return ModToast.isActive();
    }

    /** @return {@code true} when the title belongs to a city arrival and the vanilla overlay should be dropped. */
    public static boolean consumeTitle(Component text) {
        String key = translationKey(text);
        if (!isCitywalkTitle(key)) {
            return false;
        }
        String id = key.substring("att2.citywalk.".length(), key.length() - ".title".length());
        ModToast.showBanner(text, Component.translatable("att2.citywalk." + id + ".subtitle"));
        return true;
    }

    /** @return {@code true} when the subtitle is the matching citywalk line (already shown on the toast). */
    public static boolean consumeSubtitle(Component text) {
        return isCitywalkSubtitle(translationKey(text));
    }

    public static void render(net.minecraft.client.gui.GuiGraphics graphics) {
        // Rendering unified in ModToast.render (called from HudRenderer / ShopPanelScreen).
    }

    private static boolean isCitywalkTitle(String key) {
        return key.startsWith("att2.citywalk.") && key.endsWith(".title");
    }

    private static boolean isCitywalkSubtitle(String key) {
        return key.startsWith("att2.citywalk.") && key.endsWith(".subtitle");
    }

    private static String translationKey(Component component) {
        if (component == null) {
            return "";
        }
        if (component.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        for (Component sibling : component.getSiblings()) {
            String inner = translationKey(sibling);
            if (!inner.isEmpty()) {
                return inner;
            }
        }
        return "";
    }
}
