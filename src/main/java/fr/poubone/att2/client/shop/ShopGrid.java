package fr.poubone.att2.client.shop;

/**
 * Merchant cards for a chosen number per row, in the 1440 x 880 panel units of {@link ShopScreen}.
 * Cards keep their 312 x 252 shape; rows are added while they fit, shrinking the cards a little if needed.
 */
public record ShopGrid(int columns, int rows, float cardWidth, float cardHeight, float left, float top) {
    public static final int MIN_COLUMNS = 2;
    public static final int MAX_COLUMNS = 6;
    public static final int DEFAULT_COLUMNS = 3;

    private static final int AREA_LEFT = 364, AREA_TOP = 211, AREA_WIDTH = 972, AREA_HEIGHT = 522;
    private static final int CARD_WIDTH = 312, CARD_HEIGHT = 252, GAP = 18;

    public static int clampColumns(int columns) {
        return Math.max(MIN_COLUMNS, Math.min(MAX_COLUMNS, columns));
    }

    /**
     * The requested layout, or fewer cards per row while their price would be drawn below
     * {@link ShopSlotButton#MIN_READABLE_PIXELS}; never fewer than the original three unless asked for.
     *
     * @param pixelsPerUnit physical screen pixels per panel unit
     */
    public static ShopGrid fitting(int requestedColumns, float pixelsPerUnit) {
        int requested = clampColumns(requestedColumns);
        int floor = Math.min(requested, DEFAULT_COLUMNS);
        for (int columns = requested; columns > floor; columns--) {
            ShopGrid grid = of(columns);
            if (grid.cardWidth() / CARD_WIDTH * ShopSlotButton.PRICE_TEXT_SCALE * pixelsPerUnit
                    >= ShopSlotButton.MIN_READABLE_PIXELS) {
                return grid;
            }
        }
        return of(floor);
    }

    public static ShopGrid of(int requestedColumns) {
        int columns = clampColumns(requestedColumns);
        float width = Math.min(CARD_WIDTH, (AREA_WIDTH - (columns - 1) * GAP) / (float) columns);
        float height = width * CARD_HEIGHT / CARD_WIDTH;
        int rows = Math.max(1, Math.round((AREA_HEIGHT + GAP) / (height + GAP)));
        float rowHeight = (AREA_HEIGHT - (rows - 1) * GAP) / (float) rows;
        if (height > rowHeight) {
            height = rowHeight;
            width = height * CARD_WIDTH / CARD_HEIGHT;
        }
        float left = AREA_LEFT + (AREA_WIDTH - (columns * width + (columns - 1) * GAP)) / 2f;
        float top = AREA_TOP + (AREA_HEIGHT - (rows * height + (rows - 1) * GAP)) / 2f;
        return new ShopGrid(columns, rows, width, height, left, top);
    }

    public int perPage() {
        return columns * rows;
    }

    /** Left edge of the card at {@code index} on the page. */
    public int cardX(int index) {
        return Math.round(left + index % columns * (cardWidth + GAP));
    }

    /** Top edge of the card at {@code index} on the page. */
    public int cardY(int index) {
        return Math.round(top + index / columns * (cardHeight + GAP));
    }
}
