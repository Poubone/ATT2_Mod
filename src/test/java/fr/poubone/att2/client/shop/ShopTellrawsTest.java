package fr.poubone.att2.client.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopTellrawsTest {

    @Test
    void forceStoreResetLineIsActionNotTimer() {
        String line = "[Force store reset for 250 Chronotons -->]";
        assertTrue(ShopTellraws.isForceResetAction(line));
        assertFalse(ShopTellraws.isRemainingTimer(line));
    }

    @Test
    void playerPrefixedResetLineIsActionNotTimer() {
        String line = "Poubone : [Force store reset for 250 Chronotons -->]";
        assertTrue(ShopTellraws.isForceResetAction(line));
        assertFalse(ShopTellraws.isRemainingTimer(line));
    }

    @Test
    void frenchForceResetLineIsActionNotTimer() {
        String line = "[Forcer la ré-initialisation pour 250 Chronotons -->]";
        assertTrue(ShopTellraws.isForceResetAction(line));
        assertFalse(ShopTellraws.isRemainingTimer(line));
    }

    @Test
    void shopRestockTimerIsRemaining() {
        assertTrue(ShopTellraws.isRemainingTimer("Ré-initialisation des magasins dans 12 min"));
        assertTrue(ShopTellraws.isRemainingTimer("Shop reset in 5 minutes"));
        assertFalse(ShopTellraws.isForceResetAction("Shop reset in 5 minutes"));
    }
}
