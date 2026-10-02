package fr.poubone.att2.client.shop;

/** Mouse-wheel paging shared by merchant stalls, Charles, the Codex and Eldric. */
public final class ShopPaging {
    private ShopPaging() {
    }

    /** Wheel up (positive {@code deltaY}) goes to the previous page, like the rune Codex. */
    public static int afterScroll(int page, int pages, double deltaY) {
        if (deltaY == 0.0 || pages <= 1) {
            return Math.max(0, page);
        }
        int next = page - (int) Math.signum(deltaY);
        return Math.max(0, Math.min(next, pages - 1));
    }
}
