package fr.poubone.att2.client.rune;

public final class CraftSeriesLogic {
    private CraftSeriesLogic() {}
    public static int triggerSends(int qty, boolean wordMode) {
        if (qty < 1) return 0;
        return wordMode ? qty + 1 : qty;
    }
}
