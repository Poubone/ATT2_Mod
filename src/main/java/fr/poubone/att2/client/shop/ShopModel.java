package fr.poubone.att2.client.shop;

import fr.poubone.att2.client.compat.FlashbackCompat;
import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.CurrencyModel;
import fr.poubone.att2.client.gambling.CharlesScreen;
import fr.poubone.att2.client.gambling.GamblingModel;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.input.SneakMenuBypass;
import fr.poubone.att2.client.miner.MinerCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-side mirror of ATT2 merchant {@code tellraw}s.
 * <p>
 * The map prints clickable shop lines and {@code /trigger ScoreTrigger set N}. This model rebuilds those
 * lines for {@link ShopScreen}; the screen sends the exact same triggers. A line is hidden from chat only
 * once it has been understood and the menu is (or will be) showing it. Anything unrecognised stays in chat.
 */
public final class ShopModel {
    public static final int TRIGGER_RESET = 423;
    public static final int TRIGGER_MENDING_PRICE = 413;
    public static final int TRIGGER_MENDING_REPAIR = 414;
    public static final int TRIGGER_MENDING_TOOLS = 415;
    public static final int TRIGGER_TOOL_FIRST = 406;
    public static final int TRIGGER_TOOL_LAST = 412;

    private static final int COLLECT_TICKS = 18;
    private static final int OPEN_AFTER_TICKS = 7;
    private static final Pattern CATEGORY_HEADER = Pattern.compile("(?:<-°->|<=>|<->)\\s*(.+?)\\s*(?:<-°->|<=>|<->)");
    private static final Pattern MENDING_RARITY = Pattern.compile(
            "(?i)(commun|peu commun|uncommon|rare|souverain|sovereign|epique|épique|epic"
                    + "|legendaire|légendaire|legendary|supreme|suprême|common)\\s*:");
    private static final ShopModel INSTANCE = new ShopModel();

    private final ShopCatalog catalog = new ShopCatalog();
    private final List<ShopOffer> repairOffers = new ArrayList<>();
    private final List<Component> repairLines = new ArrayList<>();
    private String currentCategory = "";
    private int collectTicks;
    private int openAfterTicks = -1;
    private int version;
    private boolean openedThisBatch;
    private ShopSeller seller;
    private int lastOpeningTrigger = -1;
    private boolean repairView;
    private Component repairTitle;
    private long lastNpcOpenMs;
    /** Ticks left to leave a disabled stall's tellraws in the chat. */
    private int suppressShopTicks;

    private ShopModel() {
    }

    public static ShopModel get() {
        return INSTANCE;
    }

    public void reset() {
        catalog.clear();
        catalog.clearLock();
        currentCategory = "";
        collectTicks = 0;
        openAfterTicks = -1;
        version = 0;
        openedThisBatch = false;
        seller = null;
        lastOpeningTrigger = -1;
        suppressShopTicks = 0;
        exitRepairView();
    }

    public void tick(Minecraft client) {
        if (suppressShopTicks > 0) suppressShopTicks--;
        if (collectTicks > 0 && --collectTicks == 0) {
            catalog.inferType();
            version++;
        }
        if (openAfterTicks > 0 && --openAfterTicks == 0) {
            maybeOpen();
        }
    }

    /**
     * @return {@code true} when the line belongs to a shop and is shown by {@link ShopScreen}
     * (the chat line is then dropped).
     */
    public boolean onSystemMessage(Component message) {
        return onSystemMessage(message, true);
    }

    /** Disabled menus leave both the map message and their captured state untouched. */
    public boolean onSystemMessage(Component message, boolean enabled) {
        if (!enabled) return false;
        try {
            return handleMessage(message);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean handleMessage(Component message) {
        int opening = extractShopOpeningTrigger(message);
        if (opening >= 0) {
            ShopSeller known = ShopSellers.byTrigger(opening);
            if (known != null && !known.gambling() && !menuEnabled(known.type())) {
                suppressShopTicks = 40;
                return false;
            }
            suppressShopTicks = 0;
            lockSeller(opening);
            if (opening != GamblingModel.TRIGGER_BETS_MENU) {
                Att2Triggers.send(opening);
            }
        }
        if (suppressShopTicks > 0 && (looksLikeShop(message) || isMendingProposal(message)
                || isRepairOverlay(message) || isMendingLine(message.getString()))) {
            return false;
        }

        if (collectTriggers(message).stream().anyMatch(fr.poubone.att2.client.rune.RuneCatalog::isWorkshopTrigger)) return false;
        if (collectTicks == 0 && !isScreenShowing() && isPowderStock(message.getString(), message)) return false;
        if (isGambling(message) || isQuestNoise(message) || isPrisonerCatalog(message)
                || isEnchantmentCatalog(message)) {
            return false;
        }
        if (isResetConfirmation(message)) return false;
        if (isMerchandiseInvitation(message, opening)) return false;

        if (isMendingProposal(message)) {
            ingestActions(message);
            catalog.inferType();
            version++;
            return isScreenShowing();
        }

        if (isRepairOverlay(message) || (repairView && isMendingLine(message.getString()))) {
            ingestRepair(message);
            version++;
            if (activeMenuEnabled() && canAutoOpen() && !catalog.isEmpty()) {
                ShopScreen.open();
            }
            return isScreenShowing();
        }

        boolean shopLike = looksLikeShop(message);
        if (!shopLike) return false;

        if (collectTicks == 0) {
            if (isScreenShowing() && seller != null) {
                catalog.clearOffers();
                currentCategory = "";
                exitRepairView();
            } else {
                catalog.clear();
                currentCategory = "";
                openedThisBatch = false;
                exitRepairView();
            }
        }

        boolean accepted = ingest(message);
        if (!accepted) return false;

        collectTicks = COLLECT_TICKS;
        catalog.inferType();
        version++;
        if (!catalog.isEmpty() && activeMenuEnabled() && canAutoOpen()) {
            ShopScreen.open();
            openedThisBatch = true;
            openAfterTicks = -1;
        }
        // Hide the line only when the menu is showing it (fallback = leave chat).
        return isScreenShowing();
    }

    private boolean ingest(Component message) {
        String raw = message.getString();
        Matcher header = CATEGORY_HEADER.matcher(raw);
        if (header.find()) {
            String title = header.group(1).trim();
            if (isRepairHeader(title)) {
                enterRepairView(Component.literal(title));
                return true;
            }
            currentCategory = title;
            catalog.addCategory(currentCategory);
            return true;
        }
        if (isDiscount(raw, message)) {
            catalog.setDiscount(stripClicks(message));
            return true;
        }
        if (isPowderStock(raw, message)) {
            catalog.setPowderStock(stripClicks(message));
            return true;
        }
        if (isRemaining(raw, message)) {
            if (!ShopTellraws.isForceResetAction(raw)) {
                catalog.setRemaining(stripClicks(message));
            }
            ingestActions(message);
            return true;
        }

        List<ItemStack> items = new ArrayList<>();
        List<Component> names = new ArrayList<>();
        List<Integer> itemOrder = new ArrayList<>();
        List<Integer> triggers = new ArrayList<>();
        List<Component> triggerLabels = new ArrayList<>();
        int[] index = {0};
        visit(message, part -> {
            if (part.getStyle().getHoverEvent() instanceof HoverEvent.ShowItem show) {
                ItemStack stack = show.item();
                if (stack != null && !stack.isEmpty()) {
                    items.add(stack.copy());
                    names.add(stripClicks(part));
                    itemOrder.add(index[0]);
                }
            }
            if (part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand run) {
                int trigger = Att2Triggers.parseTriggerCommand(run.command());
                if (trigger >= 0 && !isGamblingTrigger(trigger)) {
                    triggers.add(trigger);
                    triggerLabels.add(stripClicks(part));
                }
            }
            index[0]++;
        });

        Component price = findPrice(message);
        boolean added = false;
        if (!items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                int trigger = triggerAfter(triggers, itemOrder.get(i), i);
                if (trigger < 0) continue;
                String key = translationKey(names.get(i));
                Component fallbackPrice = triggerLabels.isEmpty() ? Component.empty()
                        : triggerLabels.get(Math.min(i, triggerLabels.size() - 1));
                Identifier sprite = customSprite(findSprite(message));
                catalog.addOffer(new ShopOffer(items.get(i), names.get(i),
                        price == null ? fallbackPrice : price, trigger, currentCategory, key, sprite, findShowText(message)));
                added = true;
            }
        } else if (!triggers.isEmpty() && !isSpecialOnly(triggers)) {
            added = ingestNamedOffer(message, triggers.getFirst(), price);
        }
        if (ingestActions(message)) added = true;
        return added;
    }

    /**
     * Spells, mounts and a few stalls print a name + sprite + price without {@code show_item}.
     */
    private boolean ingestNamedOffer(Component message, int trigger, Component price) {
        if (isGamblingTrigger(trigger) || kindOfTrigger(trigger, message, message) != null
                && kindOfTrigger(trigger, message, message) != ShopAction.Kind.OTHER) {
            return false;
        }
        Identifier sprite = findSprite(message);
        Component name = findAngleName(message);
        if (name == null) name = findShopName(message);
        if (name == null && sprite == null) return false;
        if (name == null) name = Component.literal("?");
        Component lore = findShowText(message);
        ItemStack fake = fakeStackFor(sprite, name);
        String key = translationKey(name);
        catalog.addOffer(new ShopOffer(fake, name, price == null ? Component.empty() : price,
                trigger, currentCategory, key, ShopIcons.keepNamedSprite(sprite), lore));
        return true;
    }

    private static boolean isSpecialOnly(List<Integer> triggers) {
        for (int trigger : triggers) {
            if (trigger == TRIGGER_RESET || trigger == TRIGGER_MENDING_PRICE
                    || trigger == TRIGGER_MENDING_REPAIR || trigger == TRIGGER_MENDING_TOOLS
                    || (trigger >= Att2Triggers.REPAIR_HELMET && trigger <= Att2Triggers.REPAIR_ALL)) {
                continue;
            }
            return false;
        }
        return !triggers.isEmpty();
    }

    private boolean ingestActions(Component message) {
        boolean added = false;
        List<Component> flat = flatten(message);
        for (Component part : flat) {
            if (!(part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand run)) continue;
            int trigger = Att2Triggers.parseTriggerCommand(run.command());
            if (trigger < 0 || isGamblingTrigger(trigger)) continue;
            ShopAction.Kind kind = kindOfTrigger(trigger, part, message);
            if (kind == null) continue;
            Component tip = hoverText(part);
            catalog.addAction(new ShopAction(trigger, actionLabel(part, kind), tip, kind));
            added = true;
        }
        return added;
    }

    private static ShopAction.Kind kindOfTrigger(int trigger, Component part, Component root) {
        if (trigger == TRIGGER_RESET) return ShopAction.Kind.RESET;
        if (trigger == TRIGGER_MENDING_PRICE) return ShopAction.Kind.MENDING_PRICE;
        if (trigger == TRIGGER_MENDING_REPAIR) return ShopAction.Kind.MENDING_REPAIR;
        if (trigger == TRIGGER_MENDING_TOOLS) return ShopAction.Kind.MENDING_TOOLS;
        if (trigger >= Att2Triggers.REPAIR_HELMET && trigger <= Att2Triggers.REPAIR_ALL) {
            return ShopAction.Kind.MENDING_SLOT;
        }
        String key = translationKey(part);
        if (key.startsWith("att2.shop.mending")) {
            return ShopAction.Kind.MENDING_SLOT;
        }
        if (part.getStyle().getHoverEvent() instanceof HoverEvent.ShowItem) {
            return null;
        }
        if (containsKey(root, "att2.shop.chronotons.buy") || containsKey(root, "att2.shop.hover_event.buy")
                || containsKey(root, "att2.shop.esc.buy") || containsKey(root, "att2.shop.esc.hover_event.buy")) {
            return null;
        }
        if (looksLikePurchase(root)) return null;
        return ShopAction.Kind.OTHER;
    }

    private static boolean looksLikePurchase(Component message) {
        String text = message.getString().toLowerCase(Locale.ROOT);
        return text.contains("chronoton") || text.contains(" esc") || text.contains("[") && text.contains("esc")
                || text.contains("poudre") || text.contains("runic") || text.contains("runen")
                || (text.contains("[") && text.contains("]"));
    }

    private static Component actionLabel(Component part, ShopAction.Kind kind) {
        return switch (kind) {
            case RESET -> Component.translatable("shop.action.reset");
            case BACK -> Component.translatable("shop.action.back");
            case MENDING_PRICE -> Component.translatable("shop.action.mending_price");
            case MENDING_REPAIR -> Component.translatable("shop.action.mending_repair");
            case MENDING_TOOLS -> Component.translatable("shop.action.mending_tools");
            default -> stripClicks(part);
        };
    }

    private static int triggerAfter(List<Integer> triggers, int ignoredOrder, int itemIndex) {
        if (triggers.isEmpty()) return -1;
        if (itemIndex < triggers.size()) return triggers.get(itemIndex);
        return triggers.getLast();
    }

    private static Identifier findSprite(Component message) {
        Identifier[] found = {null};
        visit(message, part -> {
            if (found[0] != null) return;
            if (part.getContents() instanceof ObjectContents object
                    && object.contents() instanceof AtlasSprite sprite) {
                found[0] = sprite.sprite();
            }
        });
        return found[0];
    }

    private static Component findAngleName(Component message) {
        Component[] found = {null};
        visit(message, part -> {
            if (found[0] != null) return;
            if (part.getContents() instanceof PlainTextContents plain) {
                String text = plain.text().strip();
                if (text.length() >= 3 && text.startsWith("<") && text.endsWith(">")) {
                    found[0] = Component.literal(text).withStyle(part.getStyle().withClickEvent(null));
                }
            }
        });
        return found[0];
    }

    private static Component findShopName(Component message) {
        Component[] found = {null};
        visit(message, part -> {
            if (found[0] != null) return;
            if (part.getContents() instanceof TranslatableContents translatable) {
                String key = translatable.getKey();
                if (isItemNameKey(key)) {
                    found[0] = stripClicks(part);
                }
            }
        });
        return found[0];
    }

    private static Component findShowText(Component message) {
        Component[] found = {null};
        visit(message, part -> {
            if (found[0] != null) return;
            if (part.getStyle().getHoverEvent() instanceof HoverEvent.ShowText show) {
                found[0] = show.value();
            }
        });
        return found[0];
    }

    private static ItemStack fakeStackFor(Identifier sprite, Component name) {
        if (ShopIcons.spellId(translationKey(name), sprite) > 0) {
            return new ItemStack(Items.ENCHANTED_BOOK);
        }
        ItemStack fromSprite = ShopIcons.itemFromSprite(sprite);
        if (fromSprite != null) return fromSprite;
        String path = sprite == null ? "" : sprite.getPath().toLowerCase(Locale.ROOT);
        String text = (name == null ? "" : name.getString() + " " + translationKey(name)).toLowerCase(Locale.ROOT);
        if (path.contains("spell") || text.contains("spell") || text.contains("dahal") || text.contains("rayon")) {
            return new ItemStack(Items.ENCHANTED_BOOK);
        }
        if (path.contains("horse") || text.contains("horse") || text.contains("camel") || text.contains("ride")
                || text.contains("monture") || text.contains("pig") || text.contains("mule") || text.contains(".pig.")) {
            return new ItemStack(Items.SADDLE);
        }
        if (text.contains("fish") || text.contains("poisson") || text.contains("fishing") || text.contains("bait")
                || path.contains("fishing") || path.contains("bait")) {
            return new ItemStack(Items.FISHING_ROD);
        }
        if (path.contains("item/rabbit") || text.contains("shop.rabbit")) {
            return new ItemStack(Items.RABBIT);
        }
        if (path.contains("item/chicken") || text.contains("shop.chicken")) {
            return new ItemStack(Items.CHICKEN);
        }
        if (path.contains("item/pork") || text.contains("shop.pork")) {
            return new ItemStack(Items.PORKCHOP);
        }
        if (path.contains("item/beef") || text.contains("shop.beef")) {
            return new ItemStack(Items.BEEF);
        }
        if (path.contains("item/mutton") || text.contains("shop.mutton")) {
            return new ItemStack(Items.MUTTON);
        }
        if (path.contains("coal") || text.contains("coal") || text.contains("charbon")) {
            return new ItemStack(Items.COAL);
        }
        if (path.contains("torch") || text.contains("torch") || text.contains("torche")) {
            return new ItemStack(Items.TORCH);
        }
        if (path.contains("scaffold") || text.contains("chair") || text.contains("chaise")) {
            return new ItemStack(Items.SCAFFOLDING);
        }
        if (path.contains("bundle") || text.contains("bundle")) {
            return new ItemStack(Items.BUNDLE);
        }
        if (path.contains("carrot_on_a_stick") || text.contains("carrot_on_a_stick")) {
            return new ItemStack(Items.CARROT_ON_A_STICK);
        }
        if (path.contains("map") || text.contains(".map.") || text.contains("sylberland")) {
            return new ItemStack(Items.MAP);
        }
        if (text.contains("chronoton") || path.contains("diamond")) {
            return new ItemStack(Items.GOLD_NUGGET);
        }
        return new ItemStack(Items.BOOK);
    }

    /** Atlas sprites under {@code item/custom/} are real PNG files; vanilla {@code item/beef} is atlas-only. */
    static Identifier customSprite(Identifier sprite) {
        if (sprite == null) return null;
        String path = sprite.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("custom") || path.contains("spell") || path.contains("bait")
                || path.contains("fishing")) {
            return sprite;
        }
        return null;
    }

    private static boolean isItemNameKey(String key) {
        if (key == null || key.isEmpty()) return false;
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.contains("hover") || lower.contains("mending") || lower.contains("remain")
                || lower.endsWith(".buy") || lower.contains("discount") || lower.contains("chronotons.buy")) {
            return false;
        }
        return lower.startsWith("att2.shop.") || lower.startsWith("att2.misc.") || lower.startsWith("att2.fishing.")
                || lower.startsWith("weapon") || lower.startsWith("armor") || lower.contains("potion")
                || lower.contains("spell") || lower.contains("horse") || lower.contains("pig")
                || lower.contains("mule") || lower.contains("camel");
    }

    static Identifier spriteTexture(Identifier sprite) {
        if (sprite == null) return null;
        String path = sprite.getPath();
        if (path.startsWith("textures/") && path.endsWith(".png")) {
            return sprite;
        }
        return Identifier.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + path + ".png");
    }

    private static Component findPrice(Component message) {
        Component[] found = {null};
        visit(message, part -> {
            if (found[0] != null) return;
            if (part.getContents() instanceof TranslatableContents translatable
                    && (translatable.getKey().equals("att2.shop.chronotons.buy")
                    || translatable.getKey().contains("shop.chronoton")
                    || translatable.getKey().contains("shop.esc")
                    || translatable.getKey().contains("runepowder"))) {
                found[0] = stripClicks(part);
            }
        });
        if (found[0] != null) return found[0];
        visit(message, part -> {
            if (found[0] != null) return;
            String text = part.getString();
            String lower = text.toLowerCase(Locale.ROOT);
            if (lower.contains("chronoton") || lower.contains(" runes") || lower.contains("esc")
                    || lower.contains("poudre") || lower.contains("powder") || lower.contains("runen")
                    || (text.contains("[") && text.matches(".*\\d+.*"))) {
                if (part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand) {
                    found[0] = stripClicks(part);
                }
            }
        });
        return found[0];
    }

    private boolean looksLikeShop(Component message) {
        if (containsKeyPrefix(message, "att2.shop.")) return true;
        if (containsKeyPrefix(message, "att2.misc.") && hasShopTrigger(message)) return true;
        if (containsKeyPrefix(message, "att2.fishing.") && hasShopTrigger(message)) return true;
        if (containsKeyPrefix(message, "weapon") || containsKeyPrefix(message, "armor")) {
            return hasShowItem(message) && hasShopTrigger(message);
        }
        String raw = message.getString();
        if (raw.contains("<-°->")) return true;
        if (isDiscount(raw, message) || isRemaining(raw, message) || isPowderStock(raw, message)) return true;
        if (hasShowItem(message) && hasShopTrigger(message)) return true;
        if (hasShopTrigger(message) && (findSprite(message) != null || looksLikePurchase(message))) return true;
        List<Integer> triggers = collectTriggers(message);
        for (int trigger : triggers) {
            if (trigger == TRIGGER_RESET || trigger == TRIGGER_MENDING_PRICE
                    || trigger == TRIGGER_MENDING_REPAIR || trigger == TRIGGER_MENDING_TOOLS
                    || (trigger >= Att2Triggers.REPAIR_HELMET && trigger <= Att2Triggers.REPAIR_ALL)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPowderStock(String raw, Component message) {
        if (hasShopTrigger(message)) return false;
        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.contains("poudres runiques") || lower.contains("runic powder")
                || lower.contains("runenpulver") || lower.contains("polvos r")
                || lower.contains("pós rúnicos") || lower.contains("руническ")
                || lower.contains("符文粉末") || lower.contains("ルーン") || lower.contains("룬")
                || lower.contains("مساحيق الرون") || lower.contains("रूनिक")
                || lower.contains("stock total de poudres") || lower.contains("total stock runic");
    }

    private static boolean isDiscount(String raw, Component message) {
        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.contains("remise actuelle") || lower.contains("current discount")
                || lower.contains("descuento actual") || containsKey(message, "att2.shop.discount");
    }

    private static boolean isRemaining(String raw, Component message) {
        if (containsKey(message, "att2.shop.remain")) return true;
        return ShopTellraws.isRemainingTimer(raw);
    }

    private static boolean isResetConfirmation(Component message) {
        String raw = message.getString().toLowerCase(Locale.ROOT);
        return raw.contains("magasins réinitialisés") || raw.contains("stores reset")
                || raw.contains("tiendas reiniciadas");
    }

    private static boolean isGambling(Component message) {
        if (containsKeyPrefix(message, "matching_game") || containsKeyPrefix(message, "att2.gambling")) {
            return true;
        }
        for (int trigger : collectTriggers(message)) {
            if (isGamblingTrigger(trigger)) return true;
        }
        return false;
    }

    static boolean isGamblingTrigger(int trigger) {
        if (trigger == GamblingModel.TRIGGER_BETS_MENU || trigger == GamblingModel.TRIGGER_GRID_MENU) return true;
        if (trigger >= GamblingModel.TRIGGER_BET_FIRST && trigger <= GamblingModel.TRIGGER_BET_LAST) return true;
        if (trigger >= GamblingModel.TRIGGER_GRID_CONTINUE && trigger <= GamblingModel.TRIGGER_GRID_RESET) return true;
        return trigger >= GamblingModel.TRIGGER_CELL_FIRST && trigger <= GamblingModel.TRIGGER_CELL_LAST;
    }

    private static boolean isQuestNoise(Component message) {
        return containsKeyPrefix(message, "consciousness.")
                || containsKeyPrefix(message, "att2.mainquest")
                || containsKeyPrefix(message, "att2.dailyquest")
                || containsKeyPrefix(message, "att2.sidequest");
    }

    private static boolean isPrisonerCatalog(Component message) {
        if (containsKeyPrefix(message, "att2.rune.list")
                || containsKeyPrefix(message, "att2.rune_powder.list")
                || containsKeyPrefix(message, "att2.rune_exchange")) {
            return true;
        }
        for (int trigger : collectTriggers(message)) {
            if (MinerCatalog.isPrisonerGameplayTrigger(trigger)) return true;
        }
        return false;
    }

    /** Enchanting stays on the table; do not swallow its tellraw catalog into the generic shop. */
    private static boolean isEnchantmentCatalog(Component message) {
        return containsKeyPrefix(message, "enchantment.data.")
                || containsKeyPrefix(message, "enchantment.att2.");
    }

    private static boolean canAutoOpen() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        if (client.screen instanceof CharlesScreen) return false;
        // Tellraws often arrive the same tick the chat closes, or while it is still up.
        return client.screen == null || client.screen instanceof ShopScreen
                || client.screen instanceof ChatScreen;
    }

    private void maybeOpen() {
        if (catalog.isEmpty()) {
            openAfterTicks = -1;
            return;
        }
        if (!activeMenuEnabled()) return;
        if (!canAutoOpen()) return;
        catalog.inferType();
        ShopScreen.open();
        openedThisBatch = true;
    }

    private static boolean isScreenShowing() {
        return Minecraft.getInstance().screen instanceof ShopScreen;
    }

    public ShopCatalog catalog() {
        return catalog;
    }

    public int version() {
        return version;
    }

    public boolean isCollecting() {
        return collectTicks > 0;
    }

    public void buy(ShopOffer offer) {
        if (offer == null || offer.trigger() < 0) return;
        Att2Triggers.send(offer.trigger());
        if (catalog.usesEsc()) {
            CurrencyModel.close();
            CurrencyModel.request();
        }
    }

    public void run(ShopAction action) {
        if (action == null) return;
        if (action.kind() == ShopAction.Kind.BACK) {
            exitRepairView();
            if (catalog.offers().isEmpty() && lastOpeningTrigger > 0) {
                Att2Triggers.send(lastOpeningTrigger);
            }
            version++;
            return;
        }
        if (action.trigger() < 0) return;
        if (action.kind() == ShopAction.Kind.RESET || action.trigger() == TRIGGER_RESET) {
            startCatalogRefresh();
            Att2Triggers.send(TRIGGER_RESET);
            if (lastOpeningTrigger > 0) {
                Att2Triggers.send(lastOpeningTrigger);
            }
            return;
        }
        Att2Triggers.send(action.trigger());
    }

    private void startCatalogRefresh() {
        catalog.clearOffers();
        currentCategory = "";
        exitRepairView();
        collectTicks = COLLECT_TICKS;
        openedThisBatch = true;
        version++;
    }

    public String menuTypeId() {
        if (seller != null && !seller.gambling()) return seller.type().id;
        return catalog.type().id;
    }

    private static boolean menuEnabled(ShopType type) {
        return type != null && HUDConfig.get().isShopMenuEnabled(type.id);
    }

    private boolean activeMenuEnabled() {
        if (seller != null && !seller.gambling()) return menuEnabled(seller.type());
        return menuEnabled(catalog.type());
    }

    public ShopSeller seller() {
        return seller;
    }

    public boolean hasRepair() {
        if (catalog.usesEsc()) return false;
        if (seller != null) return seller.repair();
        return catalog.type() == ShopType.BLACKSMITH;
    }

    public boolean hasReset() {
        if (catalog.usesEsc()) return false;
        if (seller != null) return seller.reset();
        if (catalog.actions().stream().anyMatch(action -> action.kind() == ShopAction.Kind.RESET)) return true;
        ShopType type = catalog.type();
        return catalog.discount() != null || catalog.remaining() != null
                || type == ShopType.BLACKSMITH || type == ShopType.ALCHEMIST || type == ShopType.FLETCHER;
    }

    public boolean isRepairView() {
        return repairView;
    }

    public Component repairTitle() {
        return repairTitle;
    }

    public List<Component> repairLines() {
        return List.copyOf(repairLines);
    }

    public List<ShopOffer> repairOffers() {
        return List.copyOf(repairOffers);
    }

    public void exitRepairView() {
        repairView = false;
        repairTitle = null;
        repairLines.clear();
        repairOffers.clear();
    }

    public void openFromNpc(ShopSeller target) {
        if (FlashbackCompat.isInReplay()) return;
        if (target == null) return;
        if (!(target.gambling() ? HUDConfig.get().charlesAutoOpen : menuEnabled(target.type()))) return;
        if (SneakMenuBypass.isActive()) return;
        long now = System.currentTimeMillis();
        if (now - lastNpcOpenMs < 400) return;
        lastNpcOpenMs = now;
        if (target.gambling()) {
            Att2Triggers.send(target.trigger());
            return;
        }
        if (target.trigger() <= 0) return;
        lockSeller(target.trigger());
        Att2Triggers.send(target.trigger());
    }

    void lockSeller(int openingTrigger) {
        ShopSeller next = ShopSellers.byTrigger(openingTrigger);
        if (next != null && next.gambling()) {
            return;
        }
        if (next != null && (seller == null || seller.trigger() != next.trigger())) {
            seller = next;
            lastOpeningTrigger = openingTrigger;
            catalog.lockType(next.type());
            exitRepairView();
        } else if (next == null && openingTrigger > 0) {
            lastOpeningTrigger = openingTrigger;
        }
    }

    private void enterRepairView(Component title) {
        if (!repairView) {
            repairLines.clear();
            repairOffers.clear();
        }
        repairView = true;
        if (title != null && !title.getString().isBlank()) {
            repairTitle = title;
        }
    }

    private void ingestRepair(Component message) {
        String raw = message.getString();
        Matcher header = CATEGORY_HEADER.matcher(raw);
        if (header.find() && isRepairHeader(header.group(1))) {
            enterRepairView(Component.literal(header.group(1).trim()));
            return;
        }
        if (isMendingLine(raw) && collectTriggers(message).isEmpty()) {
            enterRepairView(repairTitle);
            repairLines.add(stripClicks(message));
            return;
        }
        List<Integer> triggers = collectTriggers(message);
        boolean toolLine = false;
        for (int trigger : triggers) {
            if (isRepairToolTrigger(trigger)) {
                toolLine = true;
                break;
            }
        }
        if (!toolLine && !raw.toLowerCase(Locale.ROOT).contains("outil")
                && !raw.toLowerCase(Locale.ROOT).contains("repair tool")
                && !raw.toLowerCase(Locale.ROOT).contains("tool price")) {
            if (isMendingLine(raw)) {
                enterRepairView(repairTitle);
                repairLines.add(stripClicks(message));
            }
            return;
        }
        enterRepairView(repairTitle);
        ingestRepairOffer(message, triggers);
    }

    private void ingestRepairOffer(Component message, List<Integer> triggers) {
        List<ItemStack> items = new ArrayList<>();
        List<Component> names = new ArrayList<>();
        int[] index = {0};
        List<Integer> itemOrder = new ArrayList<>();
        visit(message, part -> {
            if (part.getStyle().getHoverEvent() instanceof HoverEvent.ShowItem show) {
                ItemStack stack = show.item();
                if (stack != null && !stack.isEmpty()) {
                    items.add(stack.copy());
                    names.add(stripClicks(part));
                    itemOrder.add(index[0]);
                }
            }
            index[0]++;
        });
        Component price = findPrice(message);
        if (!items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                int trigger = triggerAfter(triggers, itemOrder.get(i), i);
                if (trigger < 0) continue;
                repairOffers.removeIf(existing -> existing.trigger() == trigger);
                repairOffers.add(new ShopOffer(items.get(i), names.get(i),
                        price == null ? Component.empty() : price, trigger, currentCategory,
                        translationKey(names.get(i)), customSprite(findSprite(message)), findShowText(message)));
            }
            return;
        }
        if (triggers.isEmpty()) return;
        int trigger = triggers.getFirst();
        Identifier sprite = findSprite(message);
        Component name = findAngleName(message);
        if (name == null) name = findShopName(message);
        if (name == null) name = stripClicks(message);
        ItemStack fake = fakeStackFor(sprite, name);
        repairOffers.removeIf(existing -> existing.trigger() == trigger);
        repairOffers.add(new ShopOffer(fake, name, price == null ? Component.empty() : price,
                trigger, currentCategory, translationKey(name), customSprite(sprite), findShowText(message)));
    }

    private static boolean isRepairToolTrigger(int trigger) {
        return trigger >= TRIGGER_TOOL_FIRST && trigger <= TRIGGER_TOOL_LAST;
    }

    private static boolean isRepairHeader(String title) {
        if (title == null) return false;
        String upper = title.toUpperCase(Locale.ROOT);
        return upper.contains("REPAIR PRICES") || title.contains("维修价格")
                || title.toLowerCase(Locale.ROOT).contains("prix de réparation")
                || title.toLowerCase(Locale.ROOT).contains("prix des outils");
    }

    private static boolean isMendingLine(String raw) {
        if (raw == null) return false;
        return MENDING_RARITY.matcher(raw).find();
    }

    private boolean isRepairOverlay(Component message) {
        String raw = message.getString();
        Matcher header = CATEGORY_HEADER.matcher(raw);
        if (header.find() && isRepairHeader(header.group(1))) return true;
        if (isMendingLine(raw) && collectTriggers(message).isEmpty()) return true;
        for (int trigger : collectTriggers(message)) {
            if (isRepairToolTrigger(trigger)) return true;
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.contains("outil de réparation") || lower.contains("repair tool")
                || lower.contains("repairing tool");
    }

    private static boolean isMendingProposal(Component message) {
        List<Integer> triggers = collectTriggers(message);
        boolean mending = false;
        for (int trigger : triggers) {
            if (trigger == TRIGGER_MENDING_PRICE || trigger == TRIGGER_MENDING_REPAIR
                    || trigger == TRIGGER_MENDING_TOOLS) {
                mending = true;
            } else if (!isRepairToolTrigger(trigger)) {
                return false;
            }
        }
        if (!mending) return false;
        return !hasShowItem(message) && findSprite(message) == null;
    }

    private static boolean isMerchandiseInvitation(Component message, int opening) {
        if (opening < 0) return false;
        if (hasShowItem(message)) return false;
        if (containsKeyPrefix(message, "att2.shop.")) return false;
        if (containsKeyPrefix(message, "att2.misc.")) return false;
        if (containsKeyPrefix(message, "att2.fishing.")) return false;
        String raw = message.getString();
        if (CATEGORY_HEADER.matcher(raw).find()) return false;
        if (isDiscount(raw, message) || isRemaining(raw, message) || isPowderStock(raw, message)) return false;
        return true;
    }

    private static int extractShopOpeningTrigger(Component message) {
        for (int trigger : collectTriggers(message)) {
            if (ShopSellers.isShopOpening(trigger) || trigger == GamblingModel.TRIGGER_BETS_MENU) {
                return trigger;
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------- component helpers

    private static boolean hasShowItem(Component message) {
        boolean[] found = {false};
        visit(message, part -> {
            if (!found[0] && part.getStyle().getHoverEvent() instanceof HoverEvent.ShowItem) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static boolean hasShopTrigger(Component message) {
        for (int trigger : collectTriggers(message)) {
            if (!isGamblingTrigger(trigger)) return true;
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

    private static boolean containsKey(Component message, String key) {
        boolean[] found = {false};
        visit(message, part -> {
            if (!found[0] && part.getContents() instanceof TranslatableContents translatable
                    && translatable.getKey().equals(key)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static boolean containsKeyPrefix(Component message, String prefix) {
        boolean[] found = {false};
        visit(message, part -> {
            if (!found[0] && part.getContents() instanceof TranslatableContents translatable
                    && translatable.getKey().startsWith(prefix)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    static String translationKey(Component component) {
        if (component == null) return "";
        if (component.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        for (Component sibling : component.getSiblings()) {
            String inner = translationKey(sibling);
            if (!inner.isEmpty()) return inner;
        }
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner) {
                    String key = translationKey(inner);
                    if (!key.isEmpty()) return key;
                }
            }
        }
        return "";
    }

    private static Component hoverText(Component part) {
        if (part.getStyle().getHoverEvent() instanceof HoverEvent.ShowText show) {
            return show.value();
        }
        return null;
    }

    private static Component stripClicks(Component component) {
        if (component == null) return Component.empty();
        return component.copy().withStyle(style -> style.withClickEvent(null));
    }

    private static List<Component> flatten(Component component) {
        List<Component> list = new ArrayList<>();
        visit(component, list::add);
        return list;
    }

    private static void visit(Component component, Consumer<Component> visitor) {
        if (component == null) return;
        visitor.accept(component);
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner) visit(inner, visitor);
            }
        }
        if (component.getContents() instanceof PlainTextContents) {
            // no nested contents
        }
        for (Component sibling : component.getSiblings()) {
            visit(sibling, visitor);
        }
    }
}
