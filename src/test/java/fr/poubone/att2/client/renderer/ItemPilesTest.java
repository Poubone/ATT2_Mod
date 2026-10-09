package fr.poubone.att2.client.renderer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ItemPilesTest {
    private record Item(int id, int cell, String rarity) {
    }

    /** The chosen items, compared by value (the result itself is an identity set). */
    private static Set<Item> piles(List<Item> items) {
        return new HashSet<>(ItemPiles.representatives(items, item -> List.of(item.cell(), item.rarity()), Item::id));
    }

    @Test void onePerRarityInEachCell() {
        List<Item> items = List.of(
                new Item(5, 0, "com"), new Item(3, 0, "com"), new Item(9, 0, "com"),
                new Item(7, 0, "leg"), new Item(4, 0, "rar"), new Item(8, 0, "rar"),
                new Item(2, 1, "com"));
        Set<Item> chosen = piles(items);
        assertEquals(4, chosen.size());
        assertTrue(chosen.contains(new Item(3, 0, "com")));
        assertTrue(chosen.contains(new Item(7, 0, "leg")));
        assertTrue(chosen.contains(new Item(4, 0, "rar")));
        assertTrue(chosen.contains(new Item(2, 1, "com")));
    }

    @Test void theLowestIdStandsForThePileWhateverTheOrder() {
        assertEquals(Set.of(new Item(1, 0, "epi")),
                piles(List.of(new Item(6, 0, "epi"), new Item(1, 0, "epi"), new Item(4, 0, "epi"))));
    }

    @Test void separateItemsStaySeparate() {
        List<Item> items = List.of(new Item(1, 0, "com"), new Item(2, 1, "com"), new Item(3, 2, "com"));
        assertEquals(3, piles(items).size());
    }
}
