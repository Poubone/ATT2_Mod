package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Optional;

/**
 * Reads ESC / chronotons / rune powder from Conscience currency (trigger 2312).
 */
public final class CurrencyDialogParser {
    public static final String TITLE_KEY = "consciousness.currency.title";
    public static final String CHRONOTONS_KEY = "consciousness.currency.chronotons";
    public static final String ESC_KEY = "consciousness.currency.esc";
    public static final String POWDER_KEY = "consciousness.currency.rune_material";
    public static final String BANK_KEY = "consciousness.currency.bank";

    private CurrencyDialogParser() {
    }

    public record Snapshot(int chronotons, int esc, int runePowder, int bank) {
    }

    public static Optional<Snapshot> parse(Component body) {
        if (body == null) return Optional.empty();
        if (!DialogComponents.hasKey(body, CHRONOTONS_KEY)) return Optional.empty();
        return Optional.of(new Snapshot(
                firstArg(body, CHRONOTONS_KEY),
                firstArg(body, ESC_KEY),
                firstArg(body, POWDER_KEY),
                firstArg(body, BANK_KEY)
        ));
    }

    private static int firstArg(Component body, String key) {
        TranslatableContents contents = DialogComponents.findTranslatable(body, key);
        return DialogComponents.argInt(contents, 0).orElse(0);
    }
}
