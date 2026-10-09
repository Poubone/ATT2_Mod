package fr.poubone.att2.client.shop;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShopGridTest {
    @Test void threePerRowIsTheOriginalLayout() {
        ShopGrid grid = ShopGrid.of(3);
        assertEquals(6, grid.perPage());
        assertEquals(312f, grid.cardWidth(), 0.001);
        assertEquals(252f, grid.cardHeight(), 0.001);
        for (int i = 0; i < 6; i++) {
            assertEquals(364 + i % 3 * 330, grid.cardX(i));
            assertEquals(211 + i / 3 * 270, grid.cardY(i));
        }
    }

    @Test void everyChoiceKeepsCardsInsideTheGridAreaWithTheirShape() {
        for (int columns = ShopGrid.MIN_COLUMNS; columns <= ShopGrid.MAX_COLUMNS; columns++) {
            ShopGrid grid = ShopGrid.of(columns);
            assertEquals(columns, grid.columns());
            assertEquals(312f / 252f, grid.cardWidth() / grid.cardHeight(), 0.001);
            assertTrue(grid.cardWidth() <= 312f);
            int last = grid.perPage() - 1;
            assertTrue(grid.cardX(0) >= 364 && grid.cardY(0) >= 211, "columns " + columns);
            assertTrue(grid.cardX(last) + grid.cardWidth() <= 1336.5f, "columns " + columns);
            assertTrue(grid.cardY(last) + grid.cardHeight() <= 733.5f, "columns " + columns);
        }
    }

    @Test void morePerRowShowsMoreItemsPerPage() {
        int previous = 0;
        for (int columns = ShopGrid.MIN_COLUMNS; columns <= ShopGrid.MAX_COLUMNS; columns++) {
            int perPage = ShopGrid.of(columns).perPage();
            assertTrue(perPage > previous, "columns " + columns);
            previous = perPage;
        }
    }

    @Test void fittingKeepsTheChoiceWhenPricesStayReadable() {
        // 640 x 360 GUI at scale 4: about 1.6 physical pixels per panel unit.
        for (int columns = ShopGrid.MIN_COLUMNS; columns <= ShopGrid.MAX_COLUMNS; columns++) {
            assertEquals(columns, ShopGrid.fitting(columns, 1.6f).columns());
        }
    }

    @Test void fittingDropsColumnsUntilPricesAreReadable() {
        // 320 x 240 GUI at scale 2: about 0.43 physical pixels per panel unit.
        assertEquals(3, ShopGrid.fitting(6, 0.43f).columns());
        // Five per row would draw prices at 0.796 pixels, four at 0.89.
        assertEquals(4, ShopGrid.fitting(6, 0.6f).columns());
        // 640 x 360 GUI at scale 2 (0.79 pixels per unit) still reads at six.
        assertEquals(6, ShopGrid.fitting(6, 0.79f).columns());
    }

    @Test void fittingNeverGoesBelowAnExplicitSmallChoice() {
        assertEquals(2, ShopGrid.fitting(2, 0.1f).columns());
        assertEquals(3, ShopGrid.fitting(3, 0.1f).columns());
    }

    @Test void outOfRangeChoicesAreClamped() {
        assertEquals(ShopGrid.MIN_COLUMNS, ShopGrid.of(0).columns());
        assertEquals(ShopGrid.MAX_COLUMNS, ShopGrid.of(99).columns());
    }
}
