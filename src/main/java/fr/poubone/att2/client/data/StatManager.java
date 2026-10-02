package fr.poubone.att2.client.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The 9 player stats and their {@code *_TOT} totals (potion included; the bossbar already sums {@code *_PO}). */
public class StatManager {
    public static final List<String> STAT_KEYS = List.of("STR", "CRT", "RES", "SPD", "HAS", "HER", "DAR", "LUC", "HUN");

    public static final Map<String, Integer> stats = new HashMap<>();

    public static void updateAll() {
        for (String key : STAT_KEYS) {
            ScoreCache.get(key + "_TOT").ifPresent(value -> stats.put(key + "_TOT", value));
        }
    }

    public static int total(String key) {
        return stats.getOrDefault(key + "_TOT", 0);
    }

    public static String[] getKeys() {
        return STAT_KEYS.stream().map(k -> k + "_TOT").toArray(String[]::new);
    }
}
