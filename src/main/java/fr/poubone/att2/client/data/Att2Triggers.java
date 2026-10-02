package fr.poubone.att2.client.data;

import fr.poubone.att2.client.compat.FlashbackCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

import java.util.Map;

/**
 * ScoreTrigger ids exposed by the ATT2 datapack (remaster 1.1.1, see
 * {@code data/att2/function/trigger/command/<id>.mcfunction}).
 * <p>
 * Every gameplay action the mod used to run through {@code /function ...} is now reachable with
 * {@code /trigger ScoreTrigger set <id>}, which does not need operator permissions.
 */
public final class Att2Triggers {
    public static final String OBJECTIVE = "ScoreTrigger";

    // Repair shop (gameplay/shop/mending/tools/trigger_*)
    public static final int REPAIR_HELMET = 416;
    public static final int REPAIR_CHESTPLATE = 417;
    public static final int REPAIR_LEGGINGS = 418;
    public static final int REPAIR_BOOTS = 419;
    public static final int REPAIR_OFFHAND = 420;
    public static final int REPAIR_MAINHAND = 421;
    public static final int REPAIR_ALL = 422;

    // Conscience information dialogs (ATT2 Definitive Edition)
    public static final int TOOLS_NUMBER_INFO = 3607;
    public static final int RUNE_BUNDLE_INFO = 3608;

    // Quests
    public static final int MAIN_QUEST_GO = 1068;
    public static final int CONSCIOUSNESS_SIDEQUEST_LIST = 2315;
    public static final int CONSCIOUSNESS_DAILYQUEST_LIST = 3280;
    public static final int CONSCIOUSNESS_CLEAR = 2310;

    // Misc
    public static final int HORSE_WHISTLE = 1070;
    public static final int COLLECT_ITEMS = 1072;
    public static final int CONSCIOUSNESS_ATTRIBUTE = 2313;
    public static final int CONSCIOUSNESS_CURRENCY = 2312;
    /** Map trigger/command/3610: expose the five HUD scores through reserved team sidebar slots. */
    public static final int HUD_SCORE_SYNC = 3610;

    /** Map glow-all (Conscience / trigger list). Does not need operator permission. */
    public static final int PLAYER_GLOW = 1085;

    /**
     * Conscience → attribute display. {@code all_run} turns every stat on in point-total mode
     * ({@code STAT_DISPLAY = 1}); {@code count} only switches the already-on bar to that mode.
     */
    public static final int STAT_DISPLAY_ALL_RUN = 556;
    public static final int STAT_DISPLAY_ALL_STOP = 557;
    public static final int STAT_DISPLAY_COUNT = 558;

    /** Stat key -> upgrade trigger (gameplay/stat/<name>/upgrade). */
    public static final Map<String, Integer> STAT_UPGRADE = Map.of(
            "STR", 1077,
            "RES", 1078,
            "HAS", 1079,
            "SPD", 1080,
            "HER", 1081,
            "DAR", 1082,
            "LUC", 1083,
            "HUN", 1084,
            "CRT", 3529
    );

    private Att2Triggers() {
    }

    public static String command(int trigger) {
        return "trigger " + OBJECTIVE + " set " + trigger;
    }

    private static final java.util.ArrayDeque<Integer> QUEUE = new java.util.ArrayDeque<>();
    /** The datapack reads ScoreTrigger once per tick: two triggers sent in the same tick would overwrite each other. */
    private static final int SEND_INTERVAL_TICKS = 2;
    private static int cooldown = 0;
    private static final int HUD_SYNC_READY_DELAY_TICKS = 40;
    private static int hudSyncReadyTicks;
    private static boolean hudSyncRequested;

    /** Queues a trigger; triggers are sent one at a time, {@value #SEND_INTERVAL_TICKS} ticks apart. */
    public static void send(int trigger) {
        if (FlashbackCompat.isInReplay()) return;
        if (!QUEUE.contains(trigger)) {
            QUEUE.addLast(trigger);
        }
    }

    /** Always enqueue, even if this trigger is already waiting (rune-word confirm). */
    public static void sendAlways(int trigger) {
        if (FlashbackCompat.isInReplay()) return;
        QUEUE.addLast(trigger);
    }

    /** A paused singleplayer server cannot re-enable the map's ScoreTrigger objective. */
    public static boolean isGameplayPaused(Minecraft client) {
        return client.isPaused() || (client.screen != null && client.screen.isPauseScreen());
    }

    public static void tick(Minecraft client) {
        if (FlashbackCompat.isInReplay()) {
            reset();
            return;
        }
        if (isGameplayPaused(client)) return;
        LocalPlayer player = client.player;
        if (player == null || player.connection == null || client.level == null) {
            QUEUE.clear();
            return;
        }
        if (!hudSyncRequested) {
            if (MapReady.isReady() || hasMapDisplay(client)) {
                if (++hudSyncReadyTicks >= HUD_SYNC_READY_DELAY_TICKS) {
                    send(HUD_SCORE_SYNC);
                    hudSyncRequested = true;
                }
            } else {
                hudSyncReadyTicks = 0;
            }
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        Integer trigger = QUEUE.pollFirst();
        if (trigger == null) return;
        cooldown = SEND_INTERVAL_TICKS;
        RepairDialogSuppressor.onTriggerSent(trigger);
        player.connection.sendCommand(command(trigger));
    }

    /** Existing map displays are available before the five HUD objectives are exposed. */
    private static boolean hasMapDisplay(Minecraft client) {
        if (client.level == null) return false;
        var scoreboard = client.level.getScoreboard();
        Objective list = scoreboard.getDisplayObjective(DisplaySlot.LIST);
        if (list != null && "TEMPERATURE".equals(list.getName())) return true;
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return false;
        return switch (sidebar.getName()) {
            case "Sidebar_Display", "Sidebar_Info", "DAHAL" -> true;
            default -> false;
        };
    }

    public static void reset() {
        QUEUE.clear();
        RepairDialogSuppressor.reset();
        cooldown = 0;
        hudSyncReadyTicks = 0;
        hudSyncRequested = false;
    }

    /**
     * Parses {@code /trigger ScoreTrigger set N} (with or without leading slash) and returns N,
     * or -1 when the command is something else.
     */
    public static int parseTriggerCommand(String command) {
        if (command == null) return -1;
        String normalized = command.strip();
        if (normalized.startsWith("/")) normalized = normalized.substring(1).stripLeading();
        String[] parts = normalized.split("\\s+");
        if (parts.length != 4) return -1;
        if (!parts[0].equalsIgnoreCase("trigger") && !parts[0].equalsIgnoreCase("minecraft:trigger")) return -1;
        if (!parts[1].equals(OBJECTIVE) || !parts[2].equalsIgnoreCase("set")) return -1;
        try {
            return Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
