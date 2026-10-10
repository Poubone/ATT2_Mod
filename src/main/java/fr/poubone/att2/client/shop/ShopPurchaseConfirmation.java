package fr.poubone.att2.client.shop;

/** A confirmation applies to the exact displayed offer, for a short time. */
final class ShopPurchaseConfirmation {
    static final long WINDOW_MS = 3000;
    private record Key(int trigger, String name, String price, String category) {
        static Key of(ShopOffer offer) {
            return new Key(offer.trigger(), offer.name().getString(),
                    offer.price() == null ? null : offer.price().getString(), offer.category());
        }
    }
    private Key key;
    private long until;

    boolean isArmed(ShopOffer offer, long now) {
        return now < until && Key.of(offer).equals(key);
    }

    /** First click arms, further clicks on the unchanged offer confirm and renew the window. */
    boolean press(ShopOffer offer, long now) {
        boolean confirmed = isArmed(offer, now);
        key = Key.of(offer);
        until = now + WINDOW_MS;
        return confirmed;
    }

    void clear() {
        key = null;
        until = 0;
    }
}
