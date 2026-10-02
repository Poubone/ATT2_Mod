package fr.poubone.att2.client.quest;

import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Deep link into the quest library at https://guide-att2.com/.
 * The site opens the matching book from {@code #rayon=…&livre=…}
 * (the same route its own catalogue builds).
 */
public final class GuideLinks {
    public static final String ORIGIN = "https://guide-att2.com";

    /** Locales that live under a path prefix. French is the site root. */
    private static final Set<String> LOCALES = Set.of(
            "en", "zh", "ja", "ko", "ar", "ru", "es", "de", "hi", "pt");

    private GuideLinks() {
    }

    /**
     * @param mainOrNumber main-quest score for {@link QuestType#MAIN} ({@code -1} when the map has not
     *                     reported it yet), otherwise the quest number inside its list
     * @param city         daily-quest city id, ignored for the other types
     * @param language     mod language code; unknown codes use the French site
     * @return absolute library URL, or null when this quest has no page on the guide
     */
    public static URI page(QuestType type, int mainOrNumber, String city, String language) {
        String hash = hash(type, mainOrNumber, city);
        if (hash == null) return null;
        String prefix = LOCALES.contains(language) ? "/" + language : "";
        return URI.create(ORIGIN + prefix + "/" + hash);
    }

    static String hash(QuestType type, int mainOrNumber, String city) {
        return switch (type) {
            case MAIN -> mainHash(mainOrNumber);
            case SIDE -> sideHash(mainOrNumber);
            case DAILY -> dailyHash(mainOrNumber, city);
        };
    }

    /**
     * Act books follow the ranges published on the guide
     * (Prologue, then scores 1–50, 51–90, 91–280, 281–300).
     * An unknown score opens the main aisle without picking a book.
     */
    private static String mainHash(int step) {
        if (step < 0) return "#rayon=main";
        String book;
        if (step == 0) book = "acte-1";
        else if (step <= 50) book = "acte-2";
        else if (step <= 90) book = "acte-3";
        else if (step <= 280) book = "acte-4";
        else book = "acte-5";
        return "#rayon=main&livre=" + book;
    }

    private static String sideHash(int number) {
        if (number < 1 || number > 60) return null;
        return "#rayon=side&livre=sq" + String.format(Locale.ROOT, "%02d", number);
    }

    private static String dailyHash(int number, String city) {
        String slug = slug(city);
        if (slug == null || number < 1 || number > 30) return null;
        return "#rayon=daily&livre=dq-" + slug + "-" + number;
    }

    /** City ids from the datapack, folded to the guide's {@code dq-<city>-<n>} slug. */
    private static String slug(String city) {
        if (city == null || city.isBlank()) return null;
        String stripped = Normalizer.normalize(city, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = stripped.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        return slug.isEmpty() ? null : slug;
    }
}
