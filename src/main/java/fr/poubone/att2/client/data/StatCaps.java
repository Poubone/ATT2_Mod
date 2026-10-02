package fr.poubone.att2.client.data;

import java.util.Map;

public final class StatCaps {
    private static final Map<String, Integer> MAX_BASE = Map.of(
            "STR", 14, "CRT", 10, "RES", 8, "HAS", 12, "SPD", 12,
            "HER", 8, "DAR", 12, "LUC", 10, "HUN", 14
    );

    private StatCaps() {}

    public static int maxBase(String statKey) {
        return MAX_BASE.getOrDefault(statKey, 0);
    }
}
