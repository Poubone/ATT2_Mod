package fr.poubone.att2.client.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HUDConfigLayoutTest {
    @Test void allSixCategoriesFitBetweenTitleAndFooterWithoutOverlapping() {
        for (int height : new int[]{180, 190, 200, 240, 270, 360, 540}) {
            int top = HUDConfigLayout.tabTop(height, 6);
            int step = HUDConfigLayout.tabStep(height, 6);
            assertTrue(top >= 28, "Title overlap at height " + height);
            assertTrue(step >= 20, "Category overlap at height " + height);
            assertTrue(top + 5 * step + 20 <= height - 32, "Footer overlap at height " + height);
        }
    }
}
