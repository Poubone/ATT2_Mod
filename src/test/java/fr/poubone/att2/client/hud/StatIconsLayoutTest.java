package fr.poubone.att2.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatIconsLayoutTest {

    @Test
    void numberSitsToTheRightOfTheIconInsteadOfOnTop() {
        StatIconsLayout layout = StatIconsLayout.of(2.0f, 1.0f, 12);
        assertTrue(layout.textOffsetX() >= layout.iconSize() + 1,
                "text should start after the icon, not over it");
    }

    @Test
    void defaultGapKeepsAComfortableColumnStride() {
        StatIconsLayout layout = StatIconsLayout.of(1.0f, 1.0f, 12);
        assertEquals(42, layout.spacingX());
        assertEquals(12, layout.spacingY());
    }

    @Test
    void smallerGapPacksColumnsWithoutOverlappingIconAndNumber() {
        StatIconsLayout loose = StatIconsLayout.of(1.0f, 1.0f, 12);
        StatIconsLayout tight = StatIconsLayout.of(1.0f, 0.4f, 12);
        assertTrue(tight.spacingX() < loose.spacingX());
        assertTrue(tight.spacingX() >= tight.textOffsetX() + 12);
    }

    @Test
    void cellOriginIsIconThenNumberOnTheSameRow() {
        StatIconsLayout layout = StatIconsLayout.of(1.5f, 1.0f, 10);
        int[] cell = layout.cell(2, 20, 40);
        assertEquals(20 + 2 * layout.spacingX(), cell[0]);
        assertEquals(40, cell[1]);
        assertTrue(layout.textOffsetX() > layout.iconSize() / 2);
    }
}
