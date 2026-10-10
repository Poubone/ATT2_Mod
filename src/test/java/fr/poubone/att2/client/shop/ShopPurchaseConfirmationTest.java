package fr.poubone.att2.client.shop;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShopPurchaseConfirmationTest {
    private static ShopOffer offer(int trigger, String price, String category) {
        return new ShopOffer(null, Component.literal("Sword"), price == null ? null : Component.literal(price),
                trigger, category, "", null, null);
    }

    @Test void firstClickArmsSecondBuysAndRepeatsRenewTheWindow() {
        var confirmation = new ShopPurchaseConfirmation();
        var offer = offer(42, "100 Chronotons", "weapons");
        assertFalse(confirmation.press(offer, 0));
        assertTrue(confirmation.press(offer, 2000));
        assertTrue(confirmation.press(offer, 4000));
        assertFalse(confirmation.press(offer, 7000));
    }

    @Test void changedPriceCurrencyCategoryOrTriggerNeedsAnotherConfirmation() {
        for (var changed : new ShopOffer[]{offer(42, "100 ESC", "weapons"),
                offer(42, "101 Chronotons", "weapons"), offer(43, "100 Chronotons", "weapons"),
                offer(42, "100 Chronotons", "repair"), offer(42, null, "weapons")}) {
            var confirmation = new ShopPurchaseConfirmation();
            assertFalse(confirmation.press(offer(42, "100 Chronotons", "weapons"), 0));
            assertFalse(confirmation.press(changed, 10));
            assertTrue(confirmation.press(changed, 20));
        }
    }

    @Test void catalogRefreshInvalidatesEvenAnIdenticalOffer() {
        var confirmation = new ShopPurchaseConfirmation();
        var offer = offer(42, "100 Chronotons", "weapons");
        confirmation.press(offer, 0);
        confirmation.clear();
        assertFalse(confirmation.isArmed(offer, 10));
        assertFalse(confirmation.press(offer, 10));
    }

    @Test void changingTheMutablePriceCannotReuseTheOriginalConfirmation() {
        var price = Component.literal("100 Chronotons");
        var offer = new ShopOffer(null, Component.literal("Sword"), price, 42, "weapons", "", null, null);
        var confirmation = new ShopPurchaseConfirmation();
        confirmation.press(offer, 0);
        price.append(" + 100 ESC");
        assertFalse(confirmation.press(offer, 10));
    }
}
