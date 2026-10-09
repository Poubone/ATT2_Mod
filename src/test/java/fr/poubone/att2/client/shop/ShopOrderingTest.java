package fr.poubone.att2.client.shop;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class ShopOrderingTest {
    private record Item(String rarity, String name) {
    }

    @Test void higherTiersComeFirstThenNames() {
        List<Item> items = List.of(
                new Item("com", "Wooden Bow"), new Item(null, "Bread"), new Item("leg", "Sun Blade"),
                new Item("ult", "Starfall"), new Item("epi_set", "Ancient Helm"), new Item("leg_armset", "Aegis"),
                new Item("rar", "Longbow"), new Item("epi", "Zephyr"), new Item("unc", "Axe"), new Item("", "Arrows"));
        List<String> sorted = items.stream()
                .sorted(ShopOrdering.byTierThenName(Item::rarity, Item::name))
                .map(Item::name).toList();
        assertEquals(List.of("Starfall", "Aegis", "Sun Blade", "Ancient Helm", "Zephyr", "Longbow", "Axe",
                "Wooden Bow", "Arrows", "Bread"), sorted);
    }

    @Test void namesIgnoreCaseAndAccents() {
        List<String> sorted = List.of(new Item("rar", "épée"), new Item("rar", "Dague"), new Item("rar", "arc"))
                .stream().sorted(ShopOrdering.byTierThenName(Item::rarity, Item::name)).map(Item::name).toList();
        assertEquals(List.of("arc", "Dague", "épée"), sorted);
    }

    @Test void unknownRaritiesRankWithUntieredItems() {
        assertEquals(0, ShopOrdering.tier("que"));
        assertEquals(0, ShopOrdering.tier(null));
        assertTrue(ShopOrdering.tier("myt") > ShopOrdering.tier("ult"));
        assertTrue(ShopOrdering.tier("ult") > ShopOrdering.tier("leg"));
    }
}
