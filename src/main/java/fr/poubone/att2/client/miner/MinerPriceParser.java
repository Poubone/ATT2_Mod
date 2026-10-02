package fr.poubone.att2.client.miner;

import fr.poubone.att2.client.data.Att2Triggers;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads Eldric buy prices from the intercepted {@code 3489} tellraw (resolved numbers),
 * without {@code /scoreboard} on {@code PRICES}.
 */
public final class MinerPriceParser {
    private static final Pattern INT = Pattern.compile("(\\d+)");

    private MinerPriceParser() {
    }

    public static Map<Integer, Integer> parse(Component message) {
        Map<Integer, Integer> prices = new LinkedHashMap<>();
        if (message == null) return prices;
        visit(message, prices);
        return prices;
    }

    private static void visit(Component component, Map<Integer, Integer> prices) {
        if (component.getStyle().getClickEvent() instanceof ClickEvent.RunCommand run) {
            int trigger = Att2Triggers.parseTriggerCommand(run.command());
            if (MinerCatalog.isBuyTrigger(trigger)) {
                int price = resolvedNumber(component);
                if (price >= 0) prices.put(trigger, price);
            }
        }
        for (Component sibling : component.getSiblings()) {
            visit(sibling, prices);
        }
    }

    private static int resolvedNumber(Component component) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component nested) {
                    int value = resolvedNumber(nested);
                    if (value >= 0) return value;
                } else {
                    Matcher matcher = INT.matcher(String.valueOf(arg));
                    if (matcher.find()) return Integer.parseInt(matcher.group(1));
                }
            }
        }
        Matcher matcher = INT.matcher(component.getString());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }
}
