package fr.poubone.att2.client.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

class ShopOwnedSpellsTest {
    /** The resolved Dahal tellraw: the " ° " marker is the root, the offer follows as siblings. */
    private static Component line(ChatFormatting markerColor) {
        return Component.literal(" ° ").withStyle(markerColor, ChatFormatting.BOLD)
                .append(Component.literal("<Fireball>").withStyle(ChatFormatting.RED))
                .append(Component.literal(" [250 Chronotons]").withStyle(ChatFormatting.YELLOW));
    }

    @Test void aGreenMarkerMeansOwned() {
        assertTrue(ShopTellraws.hasOwnedMarker(line(ChatFormatting.DARK_GREEN)));
        assertFalse(ShopTellraws.hasOwnedMarker(line(ChatFormatting.DARK_RED)));
        assertFalse(ShopTellraws.hasOwnedMarker(Component.literal("<Fireball> [250 Chronotons]")));
        assertFalse(ShopTellraws.hasOwnedMarker(null));
    }

    @Test void theMarkerIsFoundInsideNestedSiblings() {
        Component nested = Component.empty().append(line(ChatFormatting.DARK_GREEN));
        assertTrue(ShopTellraws.hasOwnedMarker(nested));
    }

    @Test void markedOffersAreOwnedWhateverTheInventory() {
        assertTrue(ShopOwnedSpells.isOwned(528, 1, true, Set.of()));
        assertTrue(ShopOwnedSpells.isOwned(426, 2, true, Set.of()));
    }

    @Test void carryingASpellHidesItsPurchaseButNotItsEnhancement() {
        assertTrue(ShopOwnedSpells.isOwned(528, 1, false, Set.of(1)));
        assertFalse(ShopOwnedSpells.isOwned(426, 2, false, Set.of(2)));
        assertFalse(ShopOwnedSpells.isOwned(528, 1, false, Set.of(2)));
        assertFalse(ShopOwnedSpells.isOwned(528, 0, false, Set.of(0)));
    }

    @Test void enhancementTriggersAreTheStallsNineUpgradeLines() {
        assertTrue(ShopOwnedSpells.isEnhancement(424));
        assertTrue(ShopOwnedSpells.isEnhancement(432));
        assertFalse(ShopOwnedSpells.isEnhancement(433));
        assertFalse(ShopOwnedSpells.isEnhancement(528));
    }
}
