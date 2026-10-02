package fr.poubone.att2.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StatUpgradeCardLayoutTest {

    @Test
    void levelLineNeverOverlapsPlusForHeights48Through72() {
        for (int h = StatUpgradeCardLayout.MIN_CARD_H; h <= StatUpgradeCardLayout.CARD_H; h++) {
            for (boolean totDiffers : new boolean[] {false, true}) {
                StatUpgradeCardLayout.Interior layout = StatUpgradeCardLayout.layout(h, totDiffers);
                int top = StatUpgradeCardLayout.plusTop(h, layout.plusMargin());
                assertTrue(
                        layout.levelY() + 10 <= top,
                        "h=" + h + " totDiffers=" + totDiffers + " levelY=" + layout.levelY() + " plusTop=" + top);
            }
        }
    }
}
