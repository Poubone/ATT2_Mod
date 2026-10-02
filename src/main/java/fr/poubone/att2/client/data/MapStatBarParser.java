package fr.poubone.att2.client.data;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure parsing of the map's {@code stat_display_*} bossbar name
 * ({@code att2.stat.display.&lt;stat&gt;} translations with a numeric argument = {@code *_TOT}).
 * Detailed-value keys ({@code *.value*}) are ignored: the HUD wants attribute points, not derived rates.
 */
public final class MapStatBarParser {
    public static final String TRANSLATION_PREFIX = "att2.stat.display.";

    private static final Set<String> POINT_SUFFIXES = Set.of(
            "str", "crt", "res", "spd", "has", "her", "dar", "luc", "hun"
    );
    private static final Pattern SIGNED_INT = Pattern.compile("-?\\d+");
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final Pattern FLAT_POINT = Pattern.compile("(STR|CRT|RES|SPD|HAS|HER|DAR|LUC|HUN):(-?\\d+)");

    private MapStatBarParser() {
    }

    public static boolean isStatDisplayTranslation(String key) {
        return key != null && key.startsWith(TRANSLATION_PREFIX);
    }

    public static boolean isDetailedValueTranslation(String key) {
        return isStatDisplayTranslation(key) && key.contains(".value");
    }

    /**
     * {@code att2.stat.display.str} → {@code STR_TOT}. Empty for {@code *.value*}, {@code *.select.*}
     * and any other suffix.
     */
    public static Optional<String> objectiveFromTranslationKey(String key) {
        if (!isStatDisplayTranslation(key)) return Optional.empty();
        String suffix = key.substring(TRANSLATION_PREFIX.length());
        if (!POINT_SUFFIXES.contains(suffix)) return Optional.empty();
        return Optional.of(suffix.toUpperCase(Locale.ROOT) + "_TOT");
    }

    public static OptionalInt parseSignedInt(String raw) {
        if (raw == null || raw.isEmpty()) return OptionalInt.empty();
        String plain = FORMATTING.matcher(raw).replaceAll("").trim();
        Matcher matcher = SIGNED_INT.matcher(plain);
        if (!matcher.find()) return OptionalInt.empty();
        try {
            return OptionalInt.of(Integer.parseInt(matcher.group()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    public static void collect(String translationKey, List<String> args, Map<String, Integer> out) {
        if (out == null || args == null || args.isEmpty()) return;
        Optional<String> objective = objectiveFromTranslationKey(translationKey);
        if (objective.isEmpty()) return;
        OptionalInt value = parseSignedInt(args.getFirst());
        if (value.isEmpty()) return;
        out.put(objective.get(), value.getAsInt());
    }

    /** Reads {@code STR:12|CRT:-1} style fragments after the server has resolved translations. */
    public static void collectFromFlattened(String flattened, Map<String, Integer> out) {
        if (out == null || flattened == null || flattened.isEmpty()) return;
        String plain = FORMATTING.matcher(flattened).replaceAll("");
        Matcher matcher = FLAT_POINT.matcher(plain);
        while (matcher.find()) {
            try {
                out.put(matcher.group(1) + "_TOT", Integer.parseInt(matcher.group(2)));
            } catch (NumberFormatException ignored) {
                // skip the malformed fragment
            }
        }
    }

    public static boolean looksLikePointTotals(String flattened) {
        if (flattened == null || flattened.isEmpty()) return false;
        Map<String, Integer> parsed = new java.util.HashMap<>();
        collectFromFlattened(flattened, parsed);
        return parsed.size() >= 2;
    }

    /**
     * The map prefixes the bar with {@code §6|} and then either the intro {@code Dahal} label
     * or {@code STR:}/{@code CRT:} style fragments. Real boss titles do not look like that.
     */
    public static boolean looksLikeStatBarText(String flattened) {
        if (flattened == null || flattened.isEmpty()) return false;
        if (looksLikePointTotals(flattened)) return true;
        String plain = FORMATTING.matcher(flattened).replaceAll("");
        return plain.contains("Dahal") && plain.contains("|");
    }
}
