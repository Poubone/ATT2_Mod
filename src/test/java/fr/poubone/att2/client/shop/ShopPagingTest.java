package fr.poubone.att2.client.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopPagingTest {

    @Test
    void wheelDownGoesToTheNextPageLikeTheCodex() {
        assertEquals(1, ShopPaging.afterScroll(0, 3, -1));
        assertEquals(2, ShopPaging.afterScroll(1, 3, -1));
        assertEquals(2, ShopPaging.afterScroll(2, 3, -1));
    }

    @Test
    void wheelUpGoesToThePreviousPage() {
        assertEquals(0, ShopPaging.afterScroll(0, 3, 1));
        assertEquals(0, ShopPaging.afterScroll(1, 3, 1));
    }

    @Test
    void noPageChangeWithoutAWheelTickOrASinglePage() {
        assertEquals(1, ShopPaging.afterScroll(1, 3, 0));
        assertEquals(0, ShopPaging.afterScroll(0, 1, -1));
    }
}
