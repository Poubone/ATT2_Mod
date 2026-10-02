package fr.poubone.att2.client.data;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Last known value of the local player's scores, and of named holders on those objectives.
 * <p>
 * The server sends scores for displayed objectives. The five HUD values below are read
 * only while their reserved team sidebars are active, so clearing a slot yields no value.
 * Other objectives may still use the last snapshot supplied by the map's UI.
 */
public final class ScoreCache {
    private static final Map<String, Integer> VALUES = new HashMap<>();
    private static final Map<String, DisplaySlot> HUD_SLOTS = Map.of(
            "CHRONOTON", DisplaySlot.TEAM_BLACK,
            "GAMELEVEL", DisplaySlot.TEAM_DARK_BLUE,
            "LEVELMASTER", DisplaySlot.TEAM_DARK_AQUA,
            "LVL_UPGRADE_REQ", DisplaySlot.TEAM_GRAY,
            "DAHALMAX", DisplaySlot.TEAM_DARK_GRAY
    );
    /** objective → holder name → last seen value. */
    private static final Map<String, Map<String, Integer>> HOLDERS = new HashMap<>();

    private ScoreCache() {
    }

    public static void clear() {
        VALUES.clear();
        HOLDERS.clear();
    }

    /** Stores a value that was read outside the live scoreboard (map stat bossbar). */
    public static void put(String objectiveName, int value) {
        if (objectiveName == null || objectiveName.isBlank()) return;
        VALUES.put(objectiveName, value);
    }

    public static void putHolder(String objectiveName, String holderName, int value) {
        if (objectiveName == null || objectiveName.isBlank() || holderName == null) return;
        HOLDERS.computeIfAbsent(objectiveName, key -> new HashMap<>()).put(holderName, value);
    }

    public static void remove(String objectiveName) {
        if (objectiveName == null) return;
        VALUES.remove(objectiveName);
    }

    /** Reads the live scoreboard if the objective is currently synced, otherwise the cached value. */
    public static OptionalInt get(String objectiveName) {
        OptionalInt live = readLive(objectiveName);
        if (HUD_SLOTS.containsKey(objectiveName)) return live;
        if (live.isPresent()) {
            VALUES.put(objectiveName, live.getAsInt());
            return live;
        }
        Integer cached = VALUES.get(objectiveName);
        return cached == null ? OptionalInt.empty() : OptionalInt.of(cached);
    }

    public static int getOrDefault(String objectiveName, int fallback) {
        return get(objectiveName).orElse(fallback);
    }

    public static boolean has(String objectiveName) {
        return get(objectiveName).isPresent();
    }

    public static OptionalInt readLive(String objectiveName) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return OptionalInt.empty();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return OptionalInt.empty();
        return readFromScoreboard(client.level.getScoreboard(), player, objectiveName);
    }

    static OptionalInt readFromScoreboard(Scoreboard scoreboard, ScoreHolder player, String objectiveName) {
        if (scoreboard == null || player == null || objectiveName == null) return OptionalInt.empty();
        Objective objective = scoreboard.getObjective(objectiveName);
        DisplaySlot requiredSlot = HUD_SLOTS.get(objectiveName);
        if (requiredSlot != null && (objective == null || scoreboard.getDisplayObjective(requiredSlot) != objective)) {
            return OptionalInt.empty();
        }
        Objective listObjective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
        if (objective == null && listObjective != null && objectiveName.equals(listObjective.getName())) {
            objective = listObjective;
        }
        if (objective == null) return OptionalInt.empty();

        ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(player, objective);
        Integer direct = info == null ? null : info.value();
        return ScoreboardLiveRead.read(
                objectiveName,
                player.getScoreboardName(),
                objective.getName(),
                listObjective == null ? null : listObjective.getName(),
                direct,
                listedEntries(scoreboard, objective)
        );
    }

    /** Live score of a named holder (fake players such as {@code cap2} on {@code SPELLN_LVL}). */
    public static OptionalInt readHolder(String objectiveName, ScoreHolder holder) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || holder == null) return OptionalInt.empty();
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) return OptionalInt.empty();
        ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(holder, objective);
        if (info == null) return OptionalInt.empty();
        return OptionalInt.of(info.value());
    }

    public static OptionalInt readHolder(String objectiveName, String holderName) {
        return readHolder(objectiveName, ScoreHolder.forNameOnly(holderName));
    }

    /**
     * Live holder score if the objective is currently synced, otherwise the last value
     * {@link #refresh} or a previous live read stored for that holder.
     */
    public static OptionalInt getHolder(String objectiveName, String holderName) {
        OptionalInt live = readHolder(objectiveName, holderName);
        if (live.isPresent()) {
            HOLDERS.computeIfAbsent(objectiveName, key -> new HashMap<>()).put(holderName, live.getAsInt());
            return live;
        }
        Map<String, Integer> byHolder = HOLDERS.get(objectiveName);
        if (byHolder == null) return OptionalInt.empty();
        Integer cached = byHolder.get(holderName);
        return cached == null ? OptionalInt.empty() : OptionalInt.of(cached);
    }

    public static OptionalInt getHolder(String objectiveName, ScoreHolder holder) {
        if (holder == null) return OptionalInt.empty();
        return getHolder(objectiveName, holder.getScoreboardName());
    }

    /** Snapshot every currently synced objective so values survive slot rotation. */
    static void refresh(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return;
        Scoreboard scoreboard = client.level.getScoreboard();
        for (Objective objective : scoreboard.getObjectives()) {
            cacheObjective(player, scoreboard, objective);
        }
        Objective listObjective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
        if (listObjective != null) {
            cacheObjective(player, scoreboard, listObjective);
        }
    }

    private static void cacheObjective(LocalPlayer player, Scoreboard scoreboard, Objective objective) {
        if (HUD_SLOTS.containsKey(objective.getName())) return;
        Objective listObjective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
        ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(player, objective);
        Integer direct = info == null ? null : info.value();
        List<PlayerListScore.Entry> listed = listedEntries(scoreboard, objective);
        ScoreboardLiveRead.read(
                objective.getName(),
                player.getScoreboardName(),
                objective.getName(),
                null,
                direct,
                listed
        ).ifPresent(value -> VALUES.put(objective.getName(), value));
        Map<String, Integer> holders = HOLDERS.computeIfAbsent(objective.getName(), key -> new HashMap<>());
        for (PlayerListScore.Entry entry : listed) {
            holders.put(entry.owner(), entry.value());
        }
    }

    private static List<PlayerListScore.Entry> listedEntries(Scoreboard scoreboard, Objective objective) {
        List<PlayerListScore.Entry> listed = new ArrayList<>();
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            listed.add(new PlayerListScore.Entry(entry.owner(), entry.value()));
        }
        return listed;
    }
}
