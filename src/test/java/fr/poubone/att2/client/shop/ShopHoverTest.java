package fr.poubone.att2.client.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopHoverTest {

    @Test
    void leftoverClickFocusDoesNotKeepATooltip() {
        assertFalse(ShopHover.showsTooltip(false, true));
        assertTrue(ShopHover.showsTooltip(true, false));
        assertTrue(ShopHover.showsTooltip(true, true));
        assertFalse(ShopHover.showsTooltip(false, false));
    }

    @Test
    void hoveredItemWinsOverFocusedTabAndPageButtons() {
        assertEquals("item", ShopHover.pick(
                new ShopHover.Widget("item", ShopHover.Kind.SLOT, true, false),
                new ShopHover.Widget("weapons", ShopHover.Kind.ACTION, false, true),
                new ShopHover.Widget("page", ShopHover.Kind.ACTION, false, true)));
    }

    @Test
    void noTooltipWhenTheCursorLeftTheClickedButton() {
        assertNull(ShopHover.pick(
                new ShopHover.Widget("armor", ShopHover.Kind.ACTION, false, true)));
    }
}
