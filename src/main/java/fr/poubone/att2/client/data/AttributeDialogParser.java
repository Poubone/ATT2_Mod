package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Reads skill points and per-stat upgrade costs from the Conscience attribute dialog
 * ({@code /trigger ScoreTrigger set 2313}), which the datapack fills server-side.
 */
public final class AttributeDialogParser {
    public static final String TITLE_KEY = "consciousness.stat.title";
    public static final String ADD_KEY = "consciousness.stat.point.add";
    public static final String ADD_ERROR_KEY = "consciousness.stat.point.add.error";
    public static final String MAX_KEY = "consciousness.stat.point.max";
    /** Stat row button label in the attribute dialog (tooltip holds {@link #BASE_KEY} and bonuses). */
    public static final String POINT_KEY = "consciousness.stat.point";
    public static final String BASE_KEY = "consciousness.stat.point.base";
    /** Suffixes of {@code consciousness.stat.point.*} lines, in tooltip order. */
    public static final List<String> SOURCE_SUFFIXES = List.of(
            "base", "equipment", "spell", "enchantment", "potion", "environment", "food", "legendary");
    public static final List<String> CONSCIENCE_ORDER = List.of(
            "STR", "CRT", "RES", "HAS", "SPD", "HER", "DAR", "LUC", "HUN");

    private AttributeDialogParser() {
    }

    public record Action(Component label, int trigger) {
    }

    public record Snapshot(int skillPoints, Map<String, Integer> costs) {
        public int cost(String statKey) {
            return costs.getOrDefault(statKey, 0);
        }
    }

    public static Optional<Snapshot> parse(Component title, List<Action> actions) {
        if (title == null || !isAttributeTitle(title)) return Optional.empty();
        int skillPoints = skillPoints(title).orElse(0);
        Map<String, Integer> costs = new HashMap<>();
        if (actions != null) {
            for (Action action : actions) {
                if (action == null) continue;
                String stat = statKeyForTrigger(action.trigger());
                if (stat == null) continue;
                if (isMaxed(action.label())) {
                    costs.put(stat, 0);
                    continue;
                }
                upgradeCost(action.label()).ifPresent(cost -> costs.put(stat, cost));
            }
        }
        return Optional.of(new Snapshot(skillPoints, Map.copyOf(costs)));
    }

    public static Map<String, Integer> parseBases(Component body) {
        List<TranslatableContents> found = new ArrayList<>();
        collectTranslatable(body, BASE_KEY, found);
        Map<String, Integer> bases = new HashMap<>();
        for (int i = 0; i < found.size() && i < CONSCIENCE_ORDER.size(); i++) {
            TranslatableContents contents = found.get(i);
            if (contents.getArgs().length == 0) continue;
            String statKey = CONSCIENCE_ORDER.get(i);
            argToInt(contents.getArgs()[0]).ifPresent(value -> bases.put(statKey, value));
        }
        return Map.copyOf(bases);
    }

    /**
     * Groups {@code consciousness.stat.point.*} lines from the attribute-row tooltips.
     * Each {@code .base} line starts the next stat in {@link #CONSCIENCE_ORDER}.
     * Absent stats are omitted; a source is present only when the datapack included that line.
     */
    public static Map<String, Map<String, Integer>> parseSources(Component tooltips) {
        List<TranslatableContents> found = new ArrayList<>();
        collectSources(tooltips, found);
        List<Map<String, Integer>> groups = new ArrayList<>();
        Map<String, Integer> current = null;
        for (TranslatableContents contents : found) {
            String suffix = sourceSuffix(contents.getKey());
            if (suffix == null || contents.getArgs().length == 0) continue;
            OptionalInt value = argToInt(contents.getArgs()[0]);
            if (value.isEmpty()) continue;
            if ("base".equals(suffix) || current == null) {
                current = new java.util.LinkedHashMap<>();
                groups.add(current);
            }
            current.put(suffix, value.getAsInt());
        }
        Map<String, Map<String, Integer>> out = new HashMap<>();
        for (int i = 0; i < groups.size() && i < CONSCIENCE_ORDER.size(); i++) {
            out.put(CONSCIENCE_ORDER.get(i), Map.copyOf(groups.get(i)));
        }
        return Map.copyOf(out);
    }

    public static boolean isAttributeTitle(Component title) {
        return findTranslatable(title, TITLE_KEY) != null;
    }

    public static OptionalInt skillPoints(Component title) {
        TranslatableContents contents = findTranslatable(title, TITLE_KEY);
        if (contents == null || contents.getArgs().length == 0) return OptionalInt.empty();
        return argToInt(contents.getArgs()[0]);
    }

    public static OptionalInt upgradeCost(Component label) {
        TranslatableContents add = findTranslatable(label, ADD_KEY);
        if (add == null) add = findTranslatable(label, ADD_ERROR_KEY);
        if (add == null || add.getArgs().length == 0) return OptionalInt.empty();
        return argToInt(add.getArgs()[0]);
    }

    public static boolean isMaxed(Component label) {
        return findTranslatable(label, MAX_KEY) != null;
    }

    public static String statKeyForTrigger(int trigger) {
        for (Map.Entry<String, Integer> entry : Att2Triggers.STAT_UPGRADE.entrySet()) {
            if (entry.getValue() == trigger) return entry.getKey();
        }
        return null;
    }

    private static TranslatableContents findTranslatable(Component component, String key) {
        if (component == null) return null;
        if (component.getContents() instanceof TranslatableContents translatable
                && translatable.getKey().equals(key)) {
            return translatable;
        }
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner) {
                    TranslatableContents nested = findTranslatable(inner, key);
                    if (nested != null) return nested;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            TranslatableContents nested = findTranslatable(sibling, key);
            if (nested != null) return nested;
        }
        return null;
    }

    private static String sourceSuffix(String key) {
        String prefix = "consciousness.stat.point.";
        if (key == null || !key.startsWith(prefix)) return null;
        String suffix = key.substring(prefix.length());
        return SOURCE_SUFFIXES.contains(suffix) ? suffix : null;
    }

    private static void collectSources(Component component, List<TranslatableContents> out) {
        if (component == null) return;
        if (component.getContents() instanceof TranslatableContents translatable) {
            if (sourceSuffix(translatable.getKey()) != null) {
                out.add(translatable);
            }
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner) {
                    collectSources(inner, out);
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            collectSources(sibling, out);
        }
    }

    private static void collectTranslatable(Component component, String key, List<TranslatableContents> out) {
        if (component == null) return;
        if (component.getContents() instanceof TranslatableContents translatable
                && translatable.getKey().equals(key)) {
            out.add(translatable);
            return;
        }
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner) {
                    collectTranslatable(inner, key, out);
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            collectTranslatable(sibling, key, out);
        }
    }

    private static OptionalInt argToInt(Object arg) {
        String text = arg instanceof Component component ? component.getString() : String.valueOf(arg);
        try {
            return OptionalInt.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
