package fr.poubone.att2.client.rune;

import org.junit.jupiter.api.Test;

import static fr.poubone.att2.client.rune.HopperCraftGuards.Outcome;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HopperCraftGuardsTest {
    @Test void outOfRangeWins() {
        assertEquals(Outcome.OUT_OF_RANGE,
                HopperCraftGuards.evaluate(false, true, true, true));
    }

    @Test void occupiedHopperWinsWhenInRange() {
        assertEquals(Outcome.HOPPER_OCCUPIED,
                HopperCraftGuards.evaluate(true, false, true, false));
    }

    @Test void readyWhenInvOk() {
        assertEquals(Outcome.READY,
                HopperCraftGuards.evaluate(true, true, true, false));
    }

    @Test void pouchOnlyWhenInvShortButPouchCovers() {
        assertEquals(Outcome.POUCH_ONLY,
                HopperCraftGuards.evaluate(true, true, false, true));
    }

    @Test void missingWhenNeitherInvNorPouch() {
        assertEquals(Outcome.MISSING_INV,
                HopperCraftGuards.evaluate(true, true, false, false));
    }

    @Test void arrowCraftMissingWhenArrowsAbsentEvenIfRunesInPouch() {
        assertEquals(Outcome.MISSING_INV,
                HopperCraftGuards.evaluateArrowCraft(true, true, false, false, true));
    }

    @Test void arrowCraftReadyWhenArrowsAndRuneInvOk() {
        assertEquals(Outcome.READY,
                HopperCraftGuards.evaluateArrowCraft(true, true, true, true, false));
    }

    @Test void arrowCraftPouchOnlyWhenArrowsOkButRunesInPouch() {
        assertEquals(Outcome.POUCH_ONLY,
                HopperCraftGuards.evaluateArrowCraft(true, true, true, false, true));
    }

    @Test void arrowCraftMissingWhenArrowsOkButRunesMissing() {
        assertEquals(Outcome.MISSING_INV,
                HopperCraftGuards.evaluateArrowCraft(true, true, true, false, false));
    }
}
