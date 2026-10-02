package fr.poubone.att2.client.miner;

import fr.poubone.att2.client.compat.FlashbackCompat;
import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.CurrencyModel;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.shop.ShopSellers;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Intercepts Eldric's prisoner-shop tellraws and shows {@link MinerShopScreen}.
 * Dialog speech stays in chat; only catalog / exchange dumps are hidden.
 */
public final class MinerShopModel {
    private static final MinerShopModel INSTANCE = new MinerShopModel();
    private static final int BURST_TICKS = 40;

    private int burstTicks;
    private boolean openedThisBurst;
    private boolean npcSession;
    private boolean useWasDown;
    private long lastNpcOpenMs;
    private MinerPrefetch prefetch;
    private final List<MinerCatalog.ExchangeOffer> exchanges = new ArrayList<>();
    private final Map<Integer, Integer> prices = new HashMap<>();
    private int resinCount = -1;
    private int version;

    private MinerShopModel() {
    }

    public static MinerShopModel get() {
        return INSTANCE;
    }

    public void reset() {
        cancelPrefetch();
        npcSession = false;
        useWasDown = false;
        lastNpcOpenMs = 0;
        burstTicks = 0;
        openedThisBurst = false;
        exchanges.clear();
        prices.clear();
        resinCount = -1;
        version++;
        if (Minecraft.getInstance().screen instanceof MinerShopScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    public void tick() {
        if (burstTicks > 0 && --burstTicks == 0) {
            openedThisBurst = false;
        }
        if (prefetch != null) {
            prefetch.tick().ifPresent(this::send);
            if (prefetch.isDone()) {
                prefetch = null;
            }
        }
    }

    /**
     * ATT2 talks via {@code pnj_talk} — {@code UseEntityCallback} often never fires.
     * Edge-detect use while the crosshair is on Eldric; do not consume the key so dialog still runs.
     */
    public void pollNpcUse(Minecraft client) {
        if (client == null || client.player == null || client.level == null) return;
        boolean down = client.options.keyUse.isDown();
        boolean pressed = down && !useWasDown;
        useWasDown = down;
        if (!pressed || client.screen != null) return;
        if (!(client.hitResult instanceof EntityHitResult hit)) return;
        if (!ShopSellers.isEldric(hit.getEntity())) return;
        openFromNpc();
    }

    public void onScreenOpened() {
        CurrencyModel.request();
    }

    public void onScreenClosed() {
        cancelPrefetch();
        npcSession = false;
        CurrencyModel.close();
    }

    public void send(int trigger) {
        Att2Triggers.sendAlways(trigger);
    }

    public void openFromNpc() {
        if (FlashbackCompat.isInReplay()) return;
        if (!HUDConfig.get().minerMenuEnabled) return;
        long now = System.currentTimeMillis();
        if (now - lastNpcOpenMs < 400) return;
        lastNpcOpenMs = now;
        npcSession = true;
        if (isScreenShowing()) {
            onScreenOpened();
            return;
        }
        exchanges.clear();
        cancelPrefetch();
        prefetch = new MinerPrefetch(MinerPrefetch.DEFAULT_TRIGGERS, MinerPrefetch.DEFAULT_DELAY_TICKS);
        MinerShopScreen.open(MinerCatalog.Tab.BUY);
    }

    public void requestExchangeRefresh() {
        send(MinerCatalog.MENU_EXCHANGE);
    }

    private void cancelPrefetch() {
        if (prefetch != null) {
            prefetch.cancel();
            prefetch = null;
        }
    }

    public List<MinerCatalog.ExchangeOffer> exchanges() {
        return List.copyOf(exchanges);
    }

    public int version() {
        return version;
    }

    public OptionalInt price(int trigger) {
        Integer value = prices.get(trigger);
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    public OptionalInt resinCount() {
        return resinCount < 0 ? OptionalInt.empty() : OptionalInt.of(resinCount);
    }

    public boolean onSystemMessage(Component message) {
        return onSystemMessage(message, fr.poubone.att2.client.hud.HUDConfig.get().minerMenuEnabled);
    }

    /** Disabled menus leave both the map message and their captured state untouched. */
    public boolean onSystemMessage(Component message, boolean enabled) {
        if (!enabled) return false;
        try {
            return handle(message);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean handle(Component message) {
        if (isDialogSpeech(message)) {
            // Map talk path: speech tellraw is the reliable "clicked Eldric" signal.
            if (isEldricSpeech(message)) {
                Minecraft.getInstance().execute(this::openFromNpc);
            }
            return false;
        }

        List<Integer> triggers = collectTriggers(message);
        if (triggers.stream().anyMatch(fr.poubone.att2.client.rune.RuneCatalog::isWorkshopTrigger)) return false;
        boolean catalogKey = hasCatalogKey(message);
        boolean resinLine = containsKey(message, "att2.dark_resin");
        boolean powderStock = burstTicks > 0 && isPowderStock(message);
        boolean exchangeLine = hasExchange(message, triggers);
        boolean prisoner = catalogKey || resinLine || powderStock || exchangeLine;
        if (!prisoner) {
            for (int trigger : triggers) {
                if (MinerCatalog.isBuyTrigger(trigger) || MinerCatalog.isPowderTrigger(trigger)
                        || MinerCatalog.isExchangeTrigger(trigger)) {
                    prisoner = true;
                    break;
                }
            }
        }
        if (!prisoner) return false;

        burstTicks = BURST_TICKS;
        MinerCatalog.Tab tab = inferTab(triggers, catalogKey, exchangeLine);
        Map<Integer, Integer> parsedPrices = MinerPriceParser.parse(message);
        if (!parsedPrices.isEmpty()) {
            prices.putAll(parsedPrices);
            version++;
        }
        int parsedResin = parseTranslatedNumber(message, "att2.dark_resin");
        if (parsedResin >= 0) {
            resinCount = parsedResin;
            version++;
        }
        if (exchangeLine) {
            List<MinerCatalog.ExchangeOffer> parsed = parseExchanges(message);
            if (!parsed.isEmpty()) {
                exchanges.clear();
                exchanges.addAll(parsed);
                version++;
            }
        }
        if (!(npcSession && isScreenShowing())) {
            if (!openedThisBurst) {
                openedThisBurst = true;
                if (!isScreenShowing()) {
                    MinerShopScreen.open(tab);
                } else if (Minecraft.getInstance().screen instanceof MinerShopScreen screen) {
                    screen.switchTab(tab);
                }
            } else if (Minecraft.getInstance().screen instanceof MinerShopScreen screen
                    && (exchangeLine || tab != screen.tab())) {
                screen.switchTab(tab);
            }
        }
        return isScreenShowing();
    }

    private static MinerCatalog.Tab inferTab(List<Integer> triggers, boolean catalogKey, boolean exchangeLine) {
        if (exchangeLine) return MinerCatalog.Tab.EXCHANGE;
        for (int trigger : triggers) {
            if (MinerCatalog.isPrisonerGameplayTrigger(trigger)) {
                return MinerCatalog.tabForTrigger(trigger);
            }
        }
        if (catalogKey) return MinerCatalog.Tab.BUY;
        return MinerCatalog.Tab.BUY;
    }

    private static boolean isDialogSpeech(Component message) {
        return message.getString().contains("°-°");
    }

    private static boolean isEldricSpeech(Component message) {
        String raw = message.getString().toLowerCase(Locale.ROOT);
        return raw.contains("°-°") && raw.contains("eldric");
    }

    private static boolean hasCatalogKey(Component message) {
        return containsKey(message, "att2.rune.list")
                || containsKey(message, "att2.rune_powder.list");
    }

    private static int parseTranslatedNumber(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents t) {
            if (t.getKey().equals(key)) {
                for (Object arg : t.getArgs()) {
                    String value = arg instanceof Component c ? c.getString() : String.valueOf(arg);
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(value);
                    if (matcher.find()) return Integer.parseInt(matcher.group());
                }
            }
            for (Object arg : t.getArgs()) if (arg instanceof Component c) {
                int found = parseTranslatedNumber(c, key);
                if (found >= 0) return found;
            }
        }
        for (Component sibling : component.getSiblings()) {
            int found = parseTranslatedNumber(sibling, key);
            if (found >= 0) return found;
        }
        return -1;
    }

    private static boolean hasExchange(Component message, List<Integer> triggers) {
        if (containsKey(message, "att2.rune_exchange")) return true;
        for (int trigger : triggers) {
            if (MinerCatalog.isExchangeTrigger(trigger)) return true;
        }
        return false;
    }

    private static boolean isPowderStock(Component message) {
        if (visitScore(message)) return true;
        String lower = message.getString().toLowerCase(Locale.ROOT);
        return lower.contains("stock total de poudres")
                || lower.contains("total stock runic")
                || lower.contains("poudres runiques")
                || lower.contains("runic powder")
                || lower.contains("runenpulver");
    }

    static List<MinerCatalog.ExchangeOffer> parseExchanges(Component message) {
        List<MinerCatalog.ExchangeOffer> out = new ArrayList<>();
        List<String> ingredients = new ArrayList<>();
        String result = null;
        boolean resultNext = false;
        int trigger = -1;
        boolean inRecipe = false;

        List<Component> parts = new ArrayList<>();
        visit(message, parts::add);
        for (Component part : parts) {
            if (part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand run) {
                int parsed = Att2Triggers.parseTriggerCommand(run.command());
                if (MinerCatalog.isExchangeTrigger(parsed)) {
                    trigger = parsed;
                }
            }
            if (part.getContents() instanceof TranslatableContents translatable
                    && "att2.rune_exchange".equals(translatable.getKey())) {
                inRecipe = true;
            }
            if (!(part.getContents() instanceof PlainTextContents plain)) {
                continue;
            }
            String text = plain.text();
            if (text.contains("⚙") || text.contains("\n")) {
                flushRecipe(out, ingredients, result, trigger, inRecipe || trigger >= 0);
                ingredients.clear();
                result = null;
                resultNext = false;
                trigger = -1;
                inRecipe = text.contains("⚙");
                if (!text.contains("⚙")) continue;
                text = text.replace("⚙", "");
            }
            String token = text.replace("+", " ").replace("=>", " => ").strip();
            if (token.isEmpty()) {
                if (text.contains("=>")) resultNext = true;
                continue;
            }
            if (text.contains("=>")) {
                resultNext = true;
            }
            for (String word : token.split("[\\s|]+")) {
                String clean = word.replace("⚙", "").strip();
                if (clean.isEmpty() || clean.equals("+") || clean.equals("=>")) {
                    if (clean.equals("=>")) resultNext = true;
                    continue;
                }
                if (MinerCatalog.runeByName(clean).isPresent()) {
                    inRecipe = true;
                    String id = clean.toLowerCase(Locale.ROOT);
                    if (resultNext && result == null) {
                        result = id;
                        resultNext = false;
                    } else {
                        ingredients.add(id);
                    }
                }
            }
        }
        flushRecipe(out, ingredients, result, trigger, inRecipe || trigger >= 0);
        return out;
    }

    private static void flushRecipe(List<MinerCatalog.ExchangeOffer> out, List<String> ingredients,
                                   String result, int trigger, boolean started) {
        if (!started) return;
        if (result == null || ingredients.isEmpty() || trigger < 0) return;
        out.add(new MinerCatalog.ExchangeOffer(trigger, List.copyOf(ingredients), result));
    }

    private static boolean visitScore(Component component) {
        if (component.getContents() instanceof ScoreContents score
                && "RUNE_POWDER".equals(score.objective())) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (visitScore(sibling)) return true;
        }
        return false;
    }

    private static boolean containsKey(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents translatable
                && key.equals(translatable.getKey())) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (containsKey(sibling, key)) return true;
        }
        return false;
    }

    private static List<Integer> collectTriggers(Component message) {
        List<Integer> triggers = new ArrayList<>();
        visit(message, part -> {
            if (part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand run) {
                int trigger = Att2Triggers.parseTriggerCommand(run.command());
                if (trigger >= 0) triggers.add(trigger);
            }
        });
        return triggers;
    }

    private static void visit(Component component, Consumer<Component> visitor) {
        visitor.accept(component);
        for (Component sibling : component.getSiblings()) visit(sibling, visitor);
    }

    private static boolean isScreenShowing() {
        return Minecraft.getInstance().screen instanceof MinerShopScreen;
    }
}
