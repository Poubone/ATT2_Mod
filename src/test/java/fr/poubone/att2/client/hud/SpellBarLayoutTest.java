package fr.poubone.att2.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpellBarLayoutTest {

    @Test
    void leftHalfBoxAlignsIconsToTheLeftEdge() {
        assertEquals(0, SpellBarLayout.iconX(0, 40, 16, 640, 0, 18));
        assertEquals(18, SpellBarLayout.iconX(0, 40, 16, 640, 1, 18));
    }

    @Test
    void rightHalfBoxKeepsIconsOnTheRightEdge() {
        assertEquals(624, SpellBarLayout.iconX(600, 40, 16, 640, 0, 18));
        assertEquals(606, SpellBarLayout.iconX(600, 40, 16, 640, 1, 18));
    }
}
