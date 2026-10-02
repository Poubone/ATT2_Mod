package fr.poubone.att2.client.miner;

import fr.poubone.att2.client.rune.RuneCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Static Eldric (prisoner miner) offers. Exchange recipes stay server-side. */
public final class MinerCatalog {
    public enum Tab { BUY, POWDER, EXCHANGE, ACTIONS }

    public static final int MENU_POWDER = 3488;
    public static final int MENU_BUY = 3489;
    public static final int ACTION_RESIN = 3490;
    public static final int ACTION_ORE = 3536;
    public static final int MENU_EXCHANGE = 3535;

    public static final int BUY_FIRST = 3495;
    public static final int BUY_LAST = 3521;
    public static final int POWDER_FIRST = 3491;
    public static final int POWDER_LAST = 3494;

    public record BuyOffer(RuneCatalog.RuneDef rune, int trigger, int resinRequired, String priceHolder) {
    }

    public record PowderOffer(int powder, int esc, int trigger) {
    }

    public record ExchangeOffer(int trigger, List<String> ingredients, String result) {
    }

    private static final List<BuyOffer> BUYS;
    private static final List<PowderOffer> POWDERS = List.of(
            new PowderOffer(50, 1, 3491),
            new PowderOffer(250, 5, 3492),
            new PowderOffer(500, 10, 3493),
            new PowderOffer(1000, 20, 3494)
    );

    static {
        List<BuyOffer> buys = new ArrayList<>();
        for (RuneCatalog.RuneDef rune : RuneCatalog.allRunes()) {
            int n = rune.level() + 1;
            buys.add(new BuyOffer(rune, BUY_FIRST + rune.level(), n, n + "_" + rune.id() + "_chronoton"));
        }
        BUYS = List.copyOf(buys);
    }

    private MinerCatalog() {
    }

    public static List<BuyOffer> buys() {
        return BUYS;
    }

    public static List<PowderOffer> powders() {
        return POWDERS;
    }

    public static Optional<RuneCatalog.RuneDef> runeByName(String raw) {
        if (raw == null) return Optional.empty();
        String id = raw.strip().toLowerCase();
        return RuneCatalog.byId(id);
    }

    public static boolean isMenuTrigger(int trigger) {
        return trigger == MENU_POWDER || trigger == MENU_BUY || trigger == MENU_EXCHANGE;
    }

    public static boolean isBuyTrigger(int trigger) {
        return trigger >= BUY_FIRST && trigger <= BUY_LAST;
    }

    public static boolean isPowderTrigger(int trigger) {
        return trigger >= POWDER_FIRST && trigger <= POWDER_LAST;
    }

    public static boolean isExchangeTrigger(int trigger) {
        return trigger == 3532 || trigger == 3533 || trigger == 3534
                || (trigger >= 3576 && trigger <= 3582);
    }

    public static boolean isActionTrigger(int trigger) {
        return trigger == ACTION_RESIN || trigger == ACTION_ORE;
    }

    public static boolean isPrisonerGameplayTrigger(int trigger) {
        return isMenuTrigger(trigger) || isBuyTrigger(trigger) || isPowderTrigger(trigger)
                || isExchangeTrigger(trigger) || isActionTrigger(trigger);
    }

    public static Tab tabForTrigger(int trigger) {
        if (isPowderTrigger(trigger) || trigger == MENU_POWDER) return Tab.POWDER;
        if (isExchangeTrigger(trigger) || trigger == MENU_EXCHANGE) return Tab.EXCHANGE;
        if (isActionTrigger(trigger)) return Tab.ACTIONS;
        return Tab.BUY;
    }

}
