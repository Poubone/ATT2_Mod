package fr.poubone.att2.client.data;

import fr.poubone.att2.client.compat.FlashbackCompat;
import fr.poubone.att2.client.input.DropLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sends the grimoire's "select level" trigger, then the book's Refresh ({@code obtain}) so the
 * written grimoire is regenerated. {@code launcher/get} only rewrites the item if the grimoire is
 * in hand, so with a launcher held the map's vanilla drop cycle is replayed: drop the launcher
 * (becomes the book) then drop the book (becomes the launcher at the new {@code SPELLN_SLCT}).
 * <p>
 * Watches the held launcher's name to learn whether the server accepted the level. A refused
 * selection leaves the name unchanged: after a timeout the level is remembered as locked for the
 * session and greyed in the radial. A later acceptance unlearns the lock.
 */
public final class SpellLevelSelector {
    /**
     * selectlvl + obtain (2 ticks apart) plus the drop/book/drop cycle and the map rewriting the
     * launcher name.
     */
    private static final int VERIFY_TICKS = 50;
    /** Let both queued triggers reach the server before touching the held item. */
    private static final int WAIT_TRIGGERS_TICKS = 12;
    private static final int WAIT_ITEM_TICKS = 20;

    private static final int RELOAD_IDLE = 0;
    private static final int RELOAD_WAIT_TRIGGERS = 1;
    private static final int RELOAD_DROP_LAUNCHER = 2;
    private static final int RELOAD_WAIT_BOOK = 3;
    private static final int RELOAD_DROP_BOOK = 4;
    private static final int RELOAD_WAIT_LAUNCHER = 5;

    private static final Map<Integer, Set<Integer>> locked = new HashMap<>();
    private static int pendingSpell;
    private static int pendingLevel;
    private static int verifyTicks;
    private static int reloadPhase;
    private static int reloadWait;

    private SpellLevelSelector() {
    }

    public static void select(int spellId, int level) {
        if (FlashbackCompat.isInReplay()) return;
        int trigger = SpellSelectTriggers.levelSelectTrigger(spellId, level);
        if (trigger < 0) return;
        Att2Triggers.send(trigger);
        int obtain = SpellSelectTriggers.obtainTrigger(spellId);
        if (obtain >= 0) Att2Triggers.send(obtain);
        pendingSpell = spellId;
        pendingLevel = level;
        verifyTicks = VERIFY_TICKS;
        reloadPhase = RELOAD_WAIT_TRIGGERS;
        reloadWait = WAIT_TRIGGERS_TICKS;
    }

    public static boolean isLocked(int spellId, int level) {
        Set<Integer> levels = locked.get(spellId);
        return levels != null && levels.contains(level);
    }

    /** Call once per client tick, after SpellLauncherTracker.tick. */
    public static void tick() {
        if (FlashbackCompat.isInReplay()) {
            reset();
            return;
        }
        if (verifyTicks <= 0 && reloadPhase == RELOAD_IDLE) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            verifyTicks = 0;
            reloadPhase = RELOAD_IDLE;
            return;
        }
        if (accepted(player)) {
            Set<Integer> levels = locked.get(pendingSpell);
            if (levels != null) levels.remove(pendingLevel);
            verifyTicks = 0;
            reloadPhase = RELOAD_IDLE;
            return;
        }
        tickReload(player);
        if (verifyTicks <= 0) return;
        verifyTicks--;
        if (verifyTicks == 0) {
            locked.computeIfAbsent(pendingSpell, k -> new HashSet<>()).add(pendingLevel);
            reloadPhase = RELOAD_IDLE;
        }
    }

    public static void reset() {
        locked.clear();
        verifyTicks = 0;
        reloadPhase = RELOAD_IDLE;
        reloadWait = 0;
    }

    private static boolean accepted(LocalPlayer player) {
        SpellLauncherTracker.LauncherState held = SpellLauncherTracker.parse(player.getMainHandItem());
        return held != null && held.spellId == pendingSpell && held.level == pendingLevel;
    }

    private static void tickReload(LocalPlayer player) {
        if (reloadPhase == RELOAD_IDLE) return;
        if (Minecraft.getInstance().screen != null) return;
        switch (reloadPhase) {
            case RELOAD_WAIT_TRIGGERS -> {
                if (--reloadWait <= 0) {
                    reloadPhase = RELOAD_DROP_LAUNCHER;
                    reloadWait = WAIT_ITEM_TICKS;
                }
            }
            case RELOAD_DROP_LAUNCHER -> dropLauncher(player);
            case RELOAD_WAIT_BOOK -> {
                if (GrimoireXp.isGrimoire(player.getMainHandItem(), pendingSpell)) {
                    reloadPhase = RELOAD_DROP_BOOK;
                    reloadWait = WAIT_ITEM_TICKS;
                    dropBook(player);
                } else if (--reloadWait <= 0) {
                    reloadPhase = RELOAD_IDLE;
                }
            }
            case RELOAD_DROP_BOOK -> dropBook(player);
            case RELOAD_WAIT_LAUNCHER -> {
                if (--reloadWait <= 0) reloadPhase = RELOAD_IDLE;
            }
            default -> reloadPhase = RELOAD_IDLE;
        }
    }

    private static void dropLauncher(LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (GrimoireXp.isGrimoire(held, pendingSpell)) {
            reloadPhase = RELOAD_DROP_BOOK;
            reloadWait = WAIT_ITEM_TICKS;
            dropBook(player);
            return;
        }
        SpellLauncherTracker.LauncherState launcher = SpellLauncherTracker.parse(held);
        if (launcher == null || launcher.spellId != pendingSpell) {
            reloadPhase = RELOAD_IDLE;
            return;
        }
        if (tryDrop(player)) {
            reloadPhase = RELOAD_WAIT_BOOK;
            reloadWait = WAIT_ITEM_TICKS;
        } else if (--reloadWait <= 0) {
            reloadPhase = RELOAD_IDLE;
        }
    }

    private static void dropBook(LocalPlayer player) {
        if (!GrimoireXp.isGrimoire(player.getMainHandItem(), pendingSpell)) {
            if (--reloadWait <= 0) reloadPhase = RELOAD_IDLE;
            return;
        }
        if (tryDrop(player)) {
            reloadPhase = RELOAD_WAIT_LAUNCHER;
            reloadWait = WAIT_ITEM_TICKS;
        } else if (--reloadWait <= 0) {
            reloadPhase = RELOAD_IDLE;
        }
    }

    private static boolean tryDrop(LocalPlayer player) {
        DropLock.allowOnce();
        return player.drop(false);
    }
}
