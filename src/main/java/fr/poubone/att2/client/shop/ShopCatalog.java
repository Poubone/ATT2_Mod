package fr.poubone.att2.client.shop;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Snapshot of one merchant conversation: items, special actions, discount and reset timer. */
public final class ShopCatalog {
    private final List<ShopOffer> offers = new ArrayList<>();
    private final List<ShopAction> actions = new ArrayList<>();
    private final LinkedHashSet<String> categories = new LinkedHashSet<>();
    private final Set<Integer> ownedTriggers = new HashSet<>();
    private Component discount;
    private Component remaining;
    private Component powderStock;
    private ShopType type = ShopType.GENERAL;
    private ShopType lockedType;
    private boolean lastUsesEsc;

    void addOffer(ShopOffer offer) {
        offers.removeIf(existing -> existing.trigger() == offer.trigger());
        offers.add(offer);
        if (offer.category() != null && !offer.category().isBlank()) {
            categories.add(offer.category());
        }
        lastUsesEsc = computeUsesEsc();
    }

    /** Offers whose line carried the map's "already owned" marker, see {@link ShopTellraws#hasOwnedMarker}. */
    void setOwned(int trigger, boolean owned) {
        if (owned) ownedTriggers.add(trigger);
        else ownedTriggers.remove(trigger);
    }

    public boolean isMarkedOwned(int trigger) {
        return ownedTriggers.contains(trigger);
    }

    void addAction(ShopAction action) {
        actions.removeIf(existing -> existing.trigger() == action.trigger());
        actions.add(action);
    }

    void addCategory(String category) {
        if (category != null && !category.isBlank()) {
            categories.add(category.trim());
        }
    }

    void setDiscount(Component discount) {
        this.discount = discount;
    }

    void setPowderStock(Component powderStock) {
        this.powderStock = powderStock;
    }

    void setRemaining(Component remaining) {
        this.remaining = remaining;
    }

    void lockType(ShopType type) {
        if (type == null) return;
        this.lockedType = type;
        this.type = type;
    }

    void inferType() {
        if (lockedType != null) {
            this.type = lockedType;
            return;
        }
        this.type = ShopType.infer(this);
    }

    void clearOffers() {
        offers.clear();
        ownedTriggers.clear();
    }

    void clear() {
        offers.clear();
        ownedTriggers.clear();
        actions.clear();
        categories.clear();
        discount = null;
        remaining = null;
        powderStock = null;
        type = lockedType != null ? lockedType : ShopType.GENERAL;
        lastUsesEsc = false;
    }

    void clearLock() {
        lockedType = null;
        type = ShopType.GENERAL;
    }

    boolean isEmpty() {
        return offers.isEmpty() && actions.isEmpty();
    }

    boolean hasMending() {
        return actions.stream().anyMatch(ShopAction::isMending);
    }

    public List<ShopOffer> offers() {
        return Collections.unmodifiableList(offers);
    }

    public List<ShopAction> actions() {
        return Collections.unmodifiableList(actions);
    }

    public Set<String> categories() {
        return Collections.unmodifiableSet(categories);
    }

    public Component discount() {
        return discount;
    }

    public Component remaining() {
        return remaining;
    }

    public Component powderStock() {
        return powderStock;
    }

    public ShopType type() {
        return type;
    }

    public boolean usesEsc() {
        if (offers.isEmpty()) return lastUsesEsc;
        return computeUsesEsc();
    }

    private boolean computeUsesEsc() {
        int esc = 0;
        for (ShopOffer offer : offers) {
            if (priceLooksEsc(offer)) esc++;
        }
        return esc > 0 && esc * 2 >= offers.size();
    }

    private static boolean priceLooksEsc(ShopOffer offer) {
        String key = ShopModel.translationKey(offer.price()).toLowerCase();
        if (key.contains("shop.esc") || key.endsWith(".esc")) return true;
        String text = offer.price() == null ? "" : offer.price().getString().toLowerCase();
        return text.contains("esc") && !text.contains("chronoton");
    }
}
