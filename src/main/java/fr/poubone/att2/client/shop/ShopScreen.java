package fr.poubone.att2.client.shop;

import fr.poubone.att2.client.compat.FlashbackCompat;
import fr.poubone.att2.client.data.CurrencyModel;
import fr.poubone.att2.client.data.ScoreCache;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.input.KeybindManager;
import fr.poubone.att2.client.util.LoadingText;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The map owns the catalog and actions; this screen only lays out their presentation. */
public class ShopScreen extends Screen {
    private static final SoundEvent OPEN_SOUND = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("att2", "ui.book.open"));
    private int offsetX, offsetY, page, pages = 1, lastVersion = -1;
    private float scale;
    private ShopType type = ShopType.GENERAL;
    private String categoryFilter = "";
    private List<String> categories = List.of();
    private List<String> chromeCategories = List.of();
    private ShopType chromeType;
    private boolean chromeRepair;
    /** Spell stalls: whether the chrome has the "hide owned spells" toggle, the carried spells, and what is hidden. */
    private boolean chromeSpells;
    private Set<Integer> carriedSpells = Set.of();
    private boolean allOwnedHidden;
    private ShopActionButton prevNav;
    private ShopActionButton nextNav;

    private boolean comparisonHeld() {
        return ShopComparison.isHeld(minecraft.getWindow().handle(), KeybindManager.compareKeyCode());
    }

    private ShopScreen() { super(ModLanguageManager.get("shop.title")); }

    public static void open() {
        if (FlashbackCompat.isInReplay()) return;
        if (!fr.poubone.att2.client.hud.HUDConfig.get().isShopMenuEnabled(ShopModel.get().menuTypeId())) return;
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof ShopScreen open) { open.rebuild(); return; }
        if (client.screen != null && !(client.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) return;
        client.setScreen(new ShopScreen());
        client.getSoundManager().play(SimpleSoundInstance.forUI(OPEN_SOUND, 1.0F));
    }

    public static boolean isOpen() { return Minecraft.getInstance().screen instanceof ShopScreen; }
    @Override public boolean isPauseScreen() { return false; }

    @Override protected void init() {
        chromeType = null; // Every control must be repositioned after a GUI-scale change or resize.
        scale = ShopViewport.fit(width, height, 640, 392).scale() * (640f / 1440f);
        offsetX = (width - size(1440)) / 2;
        offsetY = (height - size(880)) / 2;
        rebuild();
    }

    private int size(int value) { return Math.max(1, Math.round(value * scale)); }
    private int x(int value) { return offsetX + Math.round(value * scale); }
    private int y(int value) { return offsetY + Math.round(value * scale); }
    private float textScale() { return scale * 2.7f; }

    @Override public void tick() {
        if (ShopModel.get().version() != lastVersion) {
            rebuild();
        } else if (chromeSpells && !ShopOwnedSpells.carried(minecraft.player).equals(carriedSpells)) {
            rebuild(); // a spell book was just bought or dropped
        }
    }

    private static boolean sellsSpells(ShopCatalog catalog) {
        return catalog.offers().stream().anyMatch(offer -> ShopIcons.spellId(offer) > 0);
    }

    private boolean isOwnedSpell(ShopCatalog catalog, ShopOffer offer) {
        return ShopOwnedSpells.isOwned(offer.trigger(), ShopIcons.spellId(offer),
                catalog.isMarkedOwned(offer.trigger()), carriedSpells);
    }

    @Override public void removed() {
        CurrencyModel.close();
        super.removed();
    }

    private void rebuild() {
        ShopModel model = ShopModel.get();
        ShopCatalog catalog = model.catalog();
        catalog.inferType();
        if (type != catalog.type()) { categoryFilter = ""; page = 0; }
        type = catalog.type();
        lastVersion = model.version();
        CurrencyModel.request();
        if (catalog.offers().isEmpty() && model.isCollecting() && !catalog.categories().isEmpty()) {
            categories = List.copyOf(catalog.categories());
        } else {
            categories = catalog.categories().stream().filter(c -> catalog.offers().stream()
                    .anyMatch(o -> c.equals(o.category()))).toList();
        }
        if (!categories.contains(categoryFilter)) categoryFilter = "";
        boolean spells = !model.isRepairView() && sellsSpells(catalog);
        boolean keepChrome = chromeType == type && chromeRepair == model.isRepairView()
                && chromeCategories.equals(categories) && chromeSpells == spells && !children().isEmpty();
        if (keepChrome) {
            for (var child : List.copyOf(children())) {
                if (child instanceof ShopSlotButton slot) removeWidget(slot);
            }
        } else {
            clearWidgets();
            chromeCategories = List.copyOf(categories);
            chromeType = type;
            chromeRepair = model.isRepairView();
            chromeSpells = spells;
        }
        List<ShopOffer> offers = model.isRepairView() ? model.repairOffers() : catalog.offers().stream()
                .filter(o -> categoryFilter.isEmpty() || categoryFilter.equals(o.category())).toList();
        carriedSpells = spells ? ShopOwnedSpells.carried(minecraft.player) : Set.of();
        allOwnedHidden = false;
        if (spells && HUDConfig.get().shopHideOwnedSpells) {
            List<ShopOffer> unowned = offers.stream().filter(o -> !isOwnedSpell(catalog, o)).toList();
            allOwnedHidden = unowned.isEmpty() && !offers.isEmpty();
            offers = unowned;
        }
        pages = Math.max(1, (offers.size() + 5) / 6);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = page * 6; i < Math.min(offers.size(), page * 6 + 6); i++) {
            int local = i - page * 6;
            ShopOffer offer = offers.get(i);
            addRenderableWidget(new ShopSlotButton(x(364 + local % 3 * 330), y(211 + local / 3 * 270),
                    size(312), size(252), offer, () -> ShopModel.get().buy(offer), type));
        }
        if (!keepChrome) {
            addChrome(model, catalog);
        } else if (prevNav != null && nextNav != null) {
            prevNav.active = page > 0;
            nextNav.active = page + 1 < pages;
        }
    }

    private void addChrome(ShopModel model, ShopCatalog catalog) {
        if (!model.isRepairView() && categories.size() > 1) {
            List<String> filters = new ArrayList<>();
            filters.add(""); filters.addAll(categories);
            int step = Math.min(72, 500 / filters.size());
            for (int i = 0; i < filters.size(); i++) {
                String filter = filters.get(i);
                Component label = filter.isEmpty() ? ModLanguageManager.get("shop.category.all")
                        : Component.literal(prettyCategory(filter));
                button(48, 202 + i * step, 250, Math.min(58, step - 6),
                        new ShopAction(-1, label, null, ShopAction.Kind.OTHER),
                        ShopActionButton.Chrome.TAB, () -> filter.equals(categoryFilter), () -> {
                            categoryFilter = filter; page = 0; rebuild();
                        });
            }
        }
        List<ShopAction> canonical = new ArrayList<>();
        if (model.hasRepair()) {
            addIfMissing(canonical, ShopModel.TRIGGER_MENDING_PRICE, "shop.action.mending_price", ShopAction.Kind.MENDING_PRICE);
            addIfMissing(canonical, ShopModel.TRIGGER_MENDING_REPAIR, "shop.action.mending_repair", ShopAction.Kind.MENDING_REPAIR);
            addIfMissing(canonical, ShopModel.TRIGGER_MENDING_TOOLS, "shop.action.mending_tools", ShopAction.Kind.MENDING_TOOLS);
        }
        if (model.hasReset()) addIfMissing(canonical, ShopModel.TRIGGER_RESET, "shop.action.reset", ShopAction.Kind.RESET);
        if (model.isRepairView()) addIfMissing(canonical, -2, "shop.action.back", ShopAction.Kind.BACK);
        List<ShopAction> actions = ShopFooterActions.merge(canonical, catalog.actions());
        int count = actions.size() + (chromeSpells ? 1 : 0);
        int columns = Math.min(3, Math.max(1, count));
        int rows = Math.max(1, (count + columns - 1) / columns);
        for (int i = 0; i < actions.size(); i++) {
            ShopAction action = actions.get(i);
            button(48 + i % columns * 438, 774 + i / columns * (82 / rows), 420, Math.min(50, 76 / rows),
                    action, ShopActionButton.Chrome.ACTION, () -> false, () -> ShopModel.get().run(action));
        }
        if (chromeSpells) {
            int i = actions.size();
            button(48 + i % columns * 438, 774 + i / columns * (82 / rows), 420, Math.min(50, 76 / rows),
                    new ShopAction(-1, ModLanguageManager.get("shop.hide_owned"), ModLanguageManager.get("shop.hide_owned.tip"),
                            ShopAction.Kind.OTHER),
                    ShopActionButton.Chrome.CHECK, () -> HUDConfig.get().shopHideOwnedSpells, () -> {
                        HUDConfig config = HUDConfig.get();
                        config.shopHideOwnedSpells = !config.shopHideOwnedSpells;
                        HUDConfig.save();
                        rebuild();
                    });
        }
        prevNav = button(1122, 158, 42, 38, navigation("‹"), ShopActionButton.Chrome.NAV,
                () -> false, () -> { page--; rebuild(); });
        prevNav.active = page > 0;
        nextNav = button(1294, 158, 42, 38, navigation("›"), ShopActionButton.Chrome.NAV,
                () -> false, () -> { page++; rebuild(); });
        nextNav.active = page + 1 < pages;
        button(1350, 51, 42, 42, new ShopAction(-1, ModLanguageManager.get("ui.close"), null, ShopAction.Kind.OTHER),
                ShopActionButton.Chrome.CLOSE, () -> false, this::onClose);
    }

    private static ShopAction navigation(String label) {
        return new ShopAction(-1, Component.literal(label), null, ShopAction.Kind.OTHER);
    }

    private ShopActionButton button(int x, int y, int w, int h, ShopAction action, ShopActionButton.Chrome chrome,
                                    java.util.function.BooleanSupplier selected, Runnable callback) {
        return addRenderableWidget(new ShopActionButton(x(x), y(y), size(w), size(h), action,
                0xFF382B21, 0xFFF5EAD0, callback, chrome, selected, type));
    }

    private static void addIfMissing(List<ShopAction> actions, int trigger, String key, ShopAction.Kind kind) {
        if (actions.stream().anyMatch(a -> a.trigger() == trigger)) return;
        actions.add(new ShopAction(trigger, ModLanguageManager.get(key),
                trigger == ShopModel.TRIGGER_RESET ? ModLanguageManager.get("shop.action.reset.tip") : null, kind));
    }

    private static String prettyCategory(String category) {
        String cleaned = category.replace('<', ' ').replace('>', ' ').replace('-', ' ').strip();
        return cleaned.isEmpty() ? category : cleaned;
    }

    private void asset(GuiGraphics graphics, String asset, int x, int y, int w, int h, int sw, int sh) {
        ShopSkin.texture(graphics, type, asset, x(x), y(y), size(w), size(h), sw, sh);
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blurBeforeThisStratum();
        graphics.fillGradient(0, 0, width, height, 0xB0182024, 0xDB080C10);
        asset(graphics, "panel_main", 0, 0, 1440, 880, 1440, 880);
        asset(graphics, "emblem", 48, 48, 67, 67, 128, 128);
        asset(graphics, "wallet", 1094, 38, 232, 70, 232, 70);
        // Without filters the sidebar becomes the merchant's emblem, not a list of fictional categories.
        int decorY = !ShopModel.get().isRepairView() && categories.size() > 1 ? 521 : 325;
        if (categories.size() <= 4 || ShopModel.get().isRepairView()) {
            asset(graphics, "separator", 48, decorY - 22, 250, 13, 250, 13);
            asset(graphics, "medallion", 48, decorY, 250, 218, 250, 218);
            asset(graphics, "emblem", 104, decorY + 46, 138, 138, 128, 128);
            asset(graphics, "ambiance", 48, decorY + 97, 250, 130, 250, 130);
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        ShopModel model = ShopModel.get();
        ShopCatalog catalog = model.catalog();
        Component title = model.isRepairView() && model.repairTitle() != null ? model.repairTitle()
                : catalog.usesEsc() ? model.seller() != null
                    ? Component.translatable("att2.npc.name." + model.seller().npcId())
                    : ModLanguageManager.get("shop.title")
                : ModLanguageManager.get(type.titleKey());
        ShopTheme.text(graphics, title, x(137), y(61), size(900), scale * 4.5f, 0xFFF0E3CC, false);
        boolean esc = catalog.usesEsc();
        var balanceValue = ScoreCache.get(esc ? "ESC" : "CHRONOTON");
        Component balance = Component.literal(LoadingText.of(balanceValue));
        ShopTheme.text(graphics, Component.literal(esc ? "ESC" : "Chronotons"), x(1110), y(47), size(200),
                scale * 1.8f, type.titleColor, false);
        int icon = size(26);
        graphics.pose().pushMatrix();
        graphics.pose().translate(x(1110), y(72));
        graphics.pose().scale(icon / 16f, icon / 16f);
        graphics.renderItem(ShopIcons.currency(esc), 0, 0);
        graphics.pose().popMatrix();
        ShopTheme.text(graphics, balance, x(1110 + 34), y(73), size(1310 - 1144), textScale(), 0xFFF0CE8C, false);
        ShopTheme.text(graphics, Component.literal((page + 1) + " / " + pages), x(1171), y(166), size(116),
                textScale(), 0xFF705E47, true);
        Component meta = catalog.discount() != null ? catalog.discount() : catalog.powderStock();
        if (meta != null) ShopTheme.text(graphics, meta, x(364), y(164), size(725), textScale(), 0xFF705E47, false);
        if (catalog.remaining() != null && !ShopTellraws.isForceResetAction(catalog.remaining().getString())) {
            ShopTheme.text(graphics, catalog.remaining(), x(137), y(109), size(880),
                    textScale(), 0xFFD9C59E, false);
        }
        if (model.isRepairView() && model.repairOffers().isEmpty()) {
            int lineY = 224;
            for (Component line : model.repairLines()) {
                for (var wrapped : font.split(line, Math.max(1, (int) (size(956) / textScale())))) {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(x(364), y(lineY));
                    graphics.pose().scale(textScale(), textScale());
                    graphics.drawString(font, wrapped, 0, 0, 0xFF382B21, false);
                    graphics.pose().popMatrix();
                    lineY += Math.max(32, (int) (10 * textScale() / scale));
                }
            }
        } else if (catalog.offers().isEmpty()) {
            ShopTheme.text(graphics, ModLanguageManager.get(model.isCollecting() ? "shop.loading" : "shop.empty"),
                    x(364), y(400), size(972), textScale(), 0xFF705E47, true);
        } else if (allOwnedHidden) {
            ShopTheme.text(graphics, ModLanguageManager.get("shop.all_owned"),
                    x(364), y(400), size(972), textScale(), 0xFF705E47, true);
        }
        ShopSlotButton hoveredSlot = null;
        ShopActionButton hoveredAction = null;
        for (var child : children()) {
            if (child instanceof ShopSlotButton slot
                    && ShopHover.showsTooltip(slot.isHovered(), slot.isFocused())) {
                hoveredSlot = slot;
            } else if (child instanceof ShopActionButton action
                    && ShopHover.showsTooltip(action.isHovered(), action.isFocused())) {
                hoveredAction = action;
            }
        }
        if (hoveredSlot != null) {
            var kind = ShopComparison.kind(hoveredSlot.offer());
            var lines = new ArrayList<>(hoveredSlot.tooltipLines());
            boolean held = comparisonHeld();
            if (kind != null && minecraft.player != null && held) {
                ShopComparison.render(graphics, lines, minecraft.player, kind, ItemStack.EMPTY, mouseX, mouseY, width, height);
            } else {
                if (ShopComparison.shouldShowHint(kind, held)) {
                    lines.add(Component.translatable("att2.ui.compare", KeybindManager.compareKeyLabel())
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
                graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY);
            }
        } else if (hoveredAction != null) {
            graphics.setTooltipForNextFrame(font, hoveredAction.tooltipLines(), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int next = ShopPaging.afterScroll(page, pages, deltaY);
        if (next != page) {
            page = next;
            rebuild();
            return true;
        }
        if (deltaY != 0) return true;
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
}
