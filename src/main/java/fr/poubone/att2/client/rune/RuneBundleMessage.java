package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.data.ScoreCache;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the resolved lines printed by ScoreTrigger 3608. */
final class RuneBundleMessage {
    private static final String PREFIX = "att2.rune_bundle.total.";
    private static final String[] BONUS_HOLDERS = {
            "#XPTotal", "#ChronotonTotal", "#HealthTotal", "#BonusDahalMax_Total",
            "#BonusSpellXP", "#CooldownTotal", "#TimePotionTotal", "#BonusLootBoss"
    };
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");
    private static final Pattern BONUS_NUMBER = Pattern.compile("[+-]?\\d+");
    private int nextBonus = -1;

    void reset() {
        nextBonus = -1;
    }

    boolean accept(Component message) {
        TranslatableContents total = findRuneTotal(message);
        if (total != null) {
            String id = total.getKey().substring(PREFIX.length());
            RuneCatalog.byId(id).ifPresent(rune -> {
                int count = firstArgument(total);
                if (count >= 0) ScoreCache.put(rune.scoreObjective(), count);
                ScoreCache.putHolder("RUNE", rune.unlockHolder(), message.getString().contains("✔") ? 1 : 0);
            });
            if ("mot".equals(id)) nextBonus = 0;
            return true;
        }
        if (nextBonus >= 0 && nextBonus < BONUS_HOLDERS.length) {
            OptionalInt value = extraNumber(message);
            if (value.isPresent()) {
                ScoreCache.putHolder("RUNE", BONUS_HOLDERS[nextBonus++], value.getAsInt());
                if (nextBonus == BONUS_HOLDERS.length) nextBonus = -1;
                return true;
            }
        }
        return false;
    }

    private static TranslatableContents findRuneTotal(Component component) {
        if (component.getContents() instanceof TranslatableContents translated) {
            if (translated.getKey().startsWith(PREFIX)) return translated;
            for (Object arg : translated.getArgs()) {
                if (arg instanceof Component nested) {
                    TranslatableContents found = findRuneTotal(nested);
                    if (found != null) return found;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            TranslatableContents found = findRuneTotal(sibling);
            if (found != null) return found;
        }
        return null;
    }

    private static int firstArgument(TranslatableContents translated) {
        if (translated.getArgs().length == 0) return -1;
        Object arg = translated.getArgs()[0];
        if (arg instanceof Component component && component.getContents() instanceof ScoreContents) return -1;
        String text = arg instanceof Component component ? component.getString() : String.valueOf(arg);
        Matcher matcher = INTEGER.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group()) : -1;
    }

    private static OptionalInt extraNumber(Component component) {
        for (Component sibling : component.getSiblings()) {
            if (sibling.getContents() instanceof PlainTextContents plain) {
                String text = plain.text().strip();
                Matcher matcher = BONUS_NUMBER.matcher(text);
                if (matcher.find()) return OptionalInt.of(Integer.parseInt(matcher.group()));
            }
            OptionalInt nested = extraNumber(sibling);
            if (nested.isPresent()) return nested;
        }
        return OptionalInt.empty();
    }
}
