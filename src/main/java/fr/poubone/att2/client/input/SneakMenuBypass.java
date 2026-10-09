package fr.poubone.att2.client.input;

import fr.poubone.att2.client.hud.HUDConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * Holding Sneak while talking to an NPC keeps the map's own chat menu: the mod neither sends the menu trigger
 * nor captures the reply into a window.
 */
public final class SneakMenuBypass {
    /** The map's reply arrives a few ticks after the click, possibly after Sneak is released. */
    private static final int GRACE_TICKS = 10;
    private static int ticksSinceSneak = Integer.MAX_VALUE;

    private SneakMenuBypass() {
    }

    public static void tick(Minecraft client) {
        if (client.options.keyShift.isDown()) {
            ticksSinceSneak = 0;
        } else if (ticksSinceSneak < Integer.MAX_VALUE) {
            ticksSinceSneak++;
        }
    }

    public static boolean isActive() {
        if (!HUDConfig.get().sneakSkipsMenus) return false;
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) return false;
        // An open menu keeps receiving its own updates, even with toggle-sneak on.
        if (client.screen != null && !(client.screen instanceof ChatScreen)) return false;
        return client.options.keyShift.isDown() || ticksSinceSneak <= GRACE_TICKS;
    }
}
