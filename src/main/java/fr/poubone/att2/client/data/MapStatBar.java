package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads the remaster's {@code stat_display_*} bossbar (stats in the name)
 * and optionally hides it while the custom HUD is showing those same numbers.
 */
public final class MapStatBar {
    private MapStatBar() {
    }

    private static float dahalProgress = Float.NaN;

    public static void reset() {
        clearTotals();
        sawPointTotals = false;
        sawDetailedValues = false;
        dahalProgress = Float.NaN;
    }

    public static boolean hasDahalProgress() {
        return !Float.isNaN(dahalProgress);
    }

    public static float dahalProgress() {
        return dahalProgress;
    }

    private static boolean sawPointTotals;
    private static boolean sawDetailedValues;

    public static boolean sawPointTotals() {
        return sawPointTotals;
    }

    public static boolean sawDetailedValues() {
        return sawDetailedValues;
    }

    public static boolean isStatBar(BossEvent event) {
        if (event == null) return false;
        return hasPointTotals(event.getName());
    }

    static boolean isDahalBar(Component name) {
        if (name == null) return false;
        if (hasPointTotals(name) || hasDetailedValues(name)) return true;
        String flat = name.getString();
        return flat.contains("Dahal") && flat.contains("|");
    }

    /**
     * Hide only when the bar actually carries attribute points. The intro {@code Dahal |}
     * label and detailed-value mode stay visible.
     */
    public static boolean hasPointTotals(Component name) {
        return name != null && !parse(name).isEmpty();
    }

    public static boolean hasDetailedValues(Component name) {
        return containsDetailedValue(name);
    }

    public static void capture(Map<UUID, ? extends BossEvent> events, boolean expireIfAbsent) {
        boolean found = false;
        boolean detailed = false;
        boolean dahalFound = false;
        if (events != null) {
            for (BossEvent event : events.values()) {
                if (event == null) continue;
                if (isDahalBar(event.getName())) {
                    dahalProgress = event.getProgress();
                    dahalFound = true;
                }
                if (isStatBar(event)) {
                    Map<String, Integer> parsed = parse(event.getName());
                    if (parsed.isEmpty()) continue;
                    found = true;
                    parsed.forEach(ScoreCache::put);
                } else if (hasDetailedValues(event.getName())) {
                    detailed = true;
                }
            }
        }
        sawPointTotals = found;
        sawDetailedValues = detailed && !found;
        if (expireIfAbsent && !found) {
            clearTotals();
        }
        if (expireIfAbsent && !dahalFound) {
            dahalProgress = Float.NaN;
        }
    }

    static Map<String, Integer> parse(Component name) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (name == null) return out;
        if (containsDetailedValue(name)) return out;
        walk(name, out);
        MapStatBarParser.collectFromFlattened(name.getString(), out);
        return out;
    }

    private static boolean containsDetailedValue(Component component) {
        if (component == null) return false;
        if (component.getContents() instanceof TranslatableContents translatable
                && MapStatBarParser.isDetailedValueTranslation(translatable.getKey())) {
            return true;
        }
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner && containsDetailedValue(inner)) return true;
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (containsDetailedValue(sibling)) return true;
        }
        return false;
    }

    private static void clearTotals() {
        for (String key : StatManager.STAT_KEYS) {
            ScoreCache.remove(key + "_TOT");
        }
    }

    private static void walk(Component component, Map<String, Integer> out) {
        if (component == null) return;
        var contents = component.getContents();
        if (contents instanceof TranslatableContents translatable) {
            List<String> args = new ArrayList<>();
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner) {
                    args.add(flattenArg(inner));
                    walk(inner, out);
                } else if (arg != null) {
                    args.add(String.valueOf(arg));
                }
            }
            MapStatBarParser.collect(translatable.getKey(), args, out);
        } else if (contents instanceof ScoreContents score) {
            String objective = score.objective();
            if (objective != null && objective.endsWith("_TOT")) {
                ScoreCache.readLive(objective).ifPresent(value -> out.put(objective, value));
            }
        }
        for (Component sibling : component.getSiblings()) {
            walk(sibling, out);
        }
    }

    private static String flattenArg(Component component) {
        if (component.getContents() instanceof PlainTextContents text) {
            return text.text();
        }
        if (component.getContents() instanceof ScoreContents score) {
            var live = ScoreCache.readLive(score.objective());
            if (live.isPresent()) return Integer.toString(live.getAsInt());
        }
        return component.getString();
    }
}
