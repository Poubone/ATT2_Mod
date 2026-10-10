package fr.poubone.att2.client.shop;

import net.minecraft.ChatFormatting;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Optional stall ordering: highest rarity first, then by name. Items without a rarity come last. */
public final class ShopOrdering {
    private static final Collator NAMES = Collator.getInstance(Locale.ROOT);

    static {
        NAMES.setStrength(Collator.PRIMARY); // ignore case and accents
    }

    private ShopOrdering() {
    }

    /** Rank of a {@code custom_data.Rarity} id; higher is rarer, 0 for none. */
    static int tier(String rarity) {
        if (rarity == null) return 0;
        return switch (rarity) {
            case "myt" -> 7;
            case "ult" -> 6;
            case "leg", "leg_armset" -> 5;
            case "epi", "epi_esc", "epi_set", "spe" -> 4;
            case "rar" -> 3;
            case "unc" -> 2;
            case "com" -> 1;
            default -> 0;
        };
    }

    static <T> Comparator<T> byTierThenName(Function<T, String> rarity, Function<T, String> name) {
        return Comparator.<T>comparingInt(item -> -tier(rarity.apply(item)))
                .thenComparing(name, NAMES);
    }

    /** Sorted copy of {@code offers} (stable, so equal entries keep the map's order). */
    public static List<ShopOffer> byTier(List<ShopOffer> offers) {
        return offers.stream()
                .sorted(byTierThenName(ShopOffer::rarityId,
                        offer -> ChatFormatting.stripFormatting(offer.name().getString())))
                .toList();
    }
}
