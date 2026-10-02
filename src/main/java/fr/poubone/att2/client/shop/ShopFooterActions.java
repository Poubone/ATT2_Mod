package fr.poubone.att2.client.shop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Footer buttons stay in a stable order. Catalog tellraws may add extras, but they
 * cannot reshuffle or relabel the canonical repair / reset / back actions.
 */
final class ShopFooterActions {
    private ShopFooterActions() {
    }

    static List<ShopAction> merge(List<ShopAction> canonical, List<ShopAction> fromCatalog) {
        List<ShopAction> out = new ArrayList<>(canonical);
        Set<Integer> triggers = new HashSet<>();
        Set<ShopAction.Kind> kinds = new HashSet<>();
        for (ShopAction action : canonical) {
            triggers.add(action.trigger());
            if (action.kind() != ShopAction.Kind.OTHER) {
                kinds.add(action.kind());
            }
        }
        for (ShopAction extra : fromCatalog) {
            if (triggers.contains(extra.trigger())) continue;
            if (extra.kind() != ShopAction.Kind.OTHER && kinds.contains(extra.kind())) continue;
            out.add(extra);
            triggers.add(extra.trigger());
        }
        return out;
    }
}
