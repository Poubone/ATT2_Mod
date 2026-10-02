package fr.poubone.att2.client.input;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.renderer.ItemRarity;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Blocks the first attempt at dropping a mythic ("myt") or quest ("que") item and asks for a
 * second identical action within 2 seconds. The map trains players to drop items (dropping the
 * Dahal grimoire refreshes its data), so a confirmation — not a hard block — is the right tool.
 */
public final class DropLock {
    private static final long CONFIRM_WINDOW_MS = 2000;

    private static ItemStack pending = ItemStack.EMPTY;
    private static long pendingUntilMs;
    /** Next {@link #shouldBlock} is skipped (programmatic drop of a Dahäl launcher/book). */
    private static boolean allowOnce;

    private DropLock() {
    }

    /** Permits the next drop attempt, even if the rarity would normally require confirmation. */
    public static void allowOnce() {
        allowOnce = true;
    }

    /** True when the drop must be cancelled (a confirmation prompt has been shown). */
    public static boolean shouldBlock(Minecraft client, ItemStack stack) {
        if (allowOnce) {
            allowOnce = false;
            return false;
        }
        HUDConfig config = HUDConfig.get();
        if (!config.dropLockEnabled || stack == null || stack.isEmpty()) return false;
        ItemRarity rarity = ItemRarity.fromStack(stack);
        if (rarity == null || !config.dropLockRarities.contains(rarity.id)) return false;

        long now = System.currentTimeMillis();
        if (!pending.isEmpty() && now < pendingUntilMs && ItemStack.isSameItemSameComponents(pending, stack)) {
            pending = ItemStack.EMPTY;
            return false; // deuxième action dans la fenêtre : confirmé
        }
        pending = stack.copy();
        pendingUntilMs = now + CONFIRM_WINDOW_MS;
        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.literal("\u00a7c").append(ModLanguageManager.get("drop_lock.confirm")), true);
        }
        return true;
    }
}
