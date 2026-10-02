package fr.poubone.att2.client.rune;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CraftQuantityTest {
    @Test void synthMaxIsMinOfPowderAndEsc() {
        assertEquals(5, CraftQuantity.maxSynth(50, 10, 5, 1));
        assertEquals(2, CraftQuantity.maxSynth(25, 10, 100, 1));
        assertEquals(0, CraftQuantity.maxSynth(5, 10, 10, 1));
        assertEquals(10, CraftQuantity.maxSynth(100, 10, 100, 0)); // powder-only (escNeed 0)
    }

    @Test void wordsMaxIsMinRatio() {
        assertEquals(3, CraftQuantity.maxWords(new int[]{3, 9}, new int[]{1, 2}));
        assertEquals(0, CraftQuantity.maxWords(new int[]{0, 5}, new int[]{1, 1}));
    }

    @Test void arrowsMaxRespectsInvAndHopper() {
        // 4 arrows + 1 rune per craft; inv allows 20; hopper 5 slots stack 99 → inv-limited
        assertEquals(20, CraftQuantity.maxArrows(80, 4, new int[]{20}, new int[]{1}, 5, 99));
        // stackSize 1, 5 slots, recipe 4+1+1 → per craft needs 6 slots → max 0
        assertEquals(0, CraftQuantity.maxArrows(100, 4, new int[]{100, 100}, new int[]{1, 1}, 5, 1));
        // stackSize 99, 2 slots only, recipe 4 arrows + 1 rune → 2 types fit; invMax=20
        assertEquals(20, CraftQuantity.maxArrows(80, 4, new int[]{20}, new int[]{1}, 2, 99));
        // 1 hopper slot only cannot hold 2 item types
        assertEquals(0, CraftQuantity.maxArrows(80, 4, new int[]{20}, new int[]{1}, 1, 99));
    }

    @Test void arrowsMaxHopperClampsBelowInvMax() {
        // inv allows 20 crafts (80 arrows, 20 runes); hopper budget caps below invMax
        // q=16 → ceil(64/16)+ceil(16/16)=5 slots; q=17 → 7 slots with 5 hopper slots
        assertEquals(16, CraftQuantity.maxArrows(80, 4, new int[]{20}, new int[]{1}, 5, 16));
    }

    @Test void presetsAndTout() {
        assertTrue(CraftQuantity.presetEnabled(10, 12));
        assertFalse(CraftQuantity.presetEnabled(32, 12));
        assertEquals(12, CraftQuantity.resolveChoice(0, 12)); // Tout
        assertEquals(10, CraftQuantity.resolveChoice(10, 12));
        assertEquals(0, CraftQuantity.resolveChoice(64, 12)); // disabled
        assertEquals(0, CraftQuantity.resolveChoice(0, 0));
    }
}
