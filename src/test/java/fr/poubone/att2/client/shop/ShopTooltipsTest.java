package fr.poubone.att2.client.shop;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopTooltipsTest {

    @Test
    void prefersItemStatsOverBuyHint() {
        List<Component> item = List.of(
                Component.literal("Traverse déformée"),
                Component.literal("STR : +12"),
                Component.literal("Rare"));
        List<Component> out = ShopTooltips.merge(item, true,
                Component.literal("Click to buy"),
                Component.literal("[269 Chronotons]"),
                Component.literal("Traverse déformée"));
        assertTrue(out.stream().anyMatch(c -> c.getString().contains("STR")));
        assertFalse(out.stream().anyMatch(c -> c.getString().toLowerCase().contains("click to buy")));
        assertEquals("[269 Chronotons]", out.getLast().getString());
    }

    @Test
    void splitsMultilineLoreWhenThereIsNoItemTooltip() {
        List<Component> out = ShopTooltips.merge(List.of(), false,
                Component.literal("A fire spell\nDamage : 8"),
                Component.literal("[100]"),
                Component.literal("Fireball"));
        assertEquals("Fireball", out.getFirst().getString());
        assertTrue(out.stream().anyMatch(c -> c.getString().contains("Damage")));
        assertEquals(4, out.size());
    }
}
