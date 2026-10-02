package fr.poubone.att2.client.data;

import fr.poubone.att2.client.compat.FlashbackCompat;
import fr.poubone.att2.client.input.DropLock;
import fr.poubone.att2.client.screen.SpellLevelRadialScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;

/** Resolves a fresh grimoire when the radial opens, then restores the held launcher. */
public final class SpellXpRefresh {
    private enum Phase { IDLE, DROP_LAUNCHER, WAIT_BOOK, WAIT_RESOLVED, DROP_BOOK, WAIT_LAUNCHER }

    private static Phase phase = Phase.IDLE;
    private static int spellId;
    private static int wait;
    private static int pendingLevel;
    private static int selectedSlot;

    private SpellXpRefresh() {}

    public static void start(int id) {
        if (FlashbackCompat.isInReplay()) return;
        if (phase != Phase.IDLE) return;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.isPaused()) return;
        SpellLauncherTracker.LauncherState held = SpellLauncherTracker.parse(player.getMainHandItem());
        if (held == null || held.spellId != id || held.stack.getCount() != 1) return;
        if (!SpellSelectTriggers.isSupported(id)) return;
        spellId = id;
        selectedSlot = player.getInventory().getSelectedSlot();
        pendingLevel = 0;
        GrimoireXp.invalidateXp(id);
        // The map's launcher drop already runs the same obtain function as the book's Refresh link.
        phase = Phase.DROP_LAUNCHER;
    }

    public static boolean isRefreshing(int id) {
        return phase != Phase.IDLE && spellId == id;
    }

    /** The server must resolve the book, but its UI should not replace the radial. */
    public static boolean suppressBookScreen() {
        LocalPlayer player = Minecraft.getInstance().player;
        return (phase == Phase.WAIT_RESOLVED || phase == Phase.DROP_BOOK) && player != null
                && GrimoireXp.isGrimoire(player.getMainHandItem(), spellId);
    }

    public static void selectWhenReady(int id, int level) {
        if (isRefreshing(id)) pendingLevel = level;
        else SpellLevelSelector.select(id, level);
    }

    public static void tick(Minecraft client) {
        if (FlashbackCompat.isInReplay()) {
            reset();
            return;
        }
        if (phase == Phase.IDLE || Att2Triggers.isGameplayPaused(client)) return;
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            reset();
            return;
        }
        if (player.getInventory().getSelectedSlot() != selectedSlot) {
            finish(player);
            return;
        }
        ItemStack held = player.getMainHandItem();
        switch (phase) {
            case DROP_LAUNCHER -> {
                SpellLauncherTracker.LauncherState launcher = SpellLauncherTracker.parse(held);
                if (!(client.screen instanceof SpellLevelRadialScreen)
                        || launcher == null || launcher.spellId != spellId || held.getCount() != 1) {
                    finish(player);
                } else if (drop(player)) {
                    phase = Phase.WAIT_BOOK;
                    wait = 30;
                } else {
                    finish(player);
                }
            }
            case WAIT_BOOK -> {
                if (GrimoireXp.isGrimoire(held, spellId)) {
                    phase = Phase.WAIT_RESOLVED;
                    wait = 30;
                    client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                } else if (--wait <= 0) {
                    finish(player);
                }
            }
            case WAIT_RESOLVED -> {
                if (!GrimoireXp.isGrimoire(held, spellId)) {
                    finish(player);
                } else {
                    WrittenBookContent content = held.get(DataComponents.WRITTEN_BOOK_CONTENT);
                    if (content != null && content.resolved()) {
                        GrimoireXp.remember(held, spellId);
                        phase = Phase.DROP_BOOK;
                    } else if (--wait <= 0) {
                        phase = Phase.DROP_BOOK;
                    }
                }
            }
            case DROP_BOOK -> {
                if (!GrimoireXp.isGrimoire(held, spellId)) {
                    finish(player);
                } else if (drop(player)) {
                    phase = Phase.WAIT_LAUNCHER;
                    wait = 30;
                } else {
                    finish(player);
                }
            }
            case WAIT_LAUNCHER -> {
                SpellLauncherTracker.LauncherState launcher = SpellLauncherTracker.parse(held);
                if ((launcher != null && launcher.spellId == spellId) || --wait <= 0) finish(player);
            }
            default -> { }
        }
    }

    public static void reset() {
        phase = Phase.IDLE;
        spellId = 0;
        wait = 0;
        pendingLevel = 0;
        selectedSlot = 0;
    }

    private static boolean drop(LocalPlayer player) {
        DropLock.allowOnce();
        return player.drop(false);
    }

    private static void finish(LocalPlayer player) {
        int id = spellId;
        int selection = pendingLevel;
        reset();
        SpellLauncherTracker.LauncherState launcher = SpellLauncherTracker.parse(player.getMainHandItem());
        if (selection > 0 && launcher != null && launcher.spellId == id) {
            SpellLevelSelector.select(id, selection);
        }
    }
}
