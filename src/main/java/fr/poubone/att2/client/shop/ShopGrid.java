package fr.poubone.att2.client.shop;

/**
 * Merchant cards for a chosen number per row, in the 1440 x 880 panel units of {@link ShopScreen}.
 * Cards keep their shape (312 x 252, or 312 x 218 without the buy hint); rows are added while they fit,
 * shrinking the cards a little if needed.
 */
public record ShopGrid(int columns, int rows, float cardWidth, float cardHeight, float left, float top,
                       boolean hint) {
    public static final int MIN_COLUMNS = 2;
    public static final int MAX_COLUMNS = 6;
    public static final int DEFAULT_COLUMNS = 3;

    private static final int AREA_LEFT = 364, AREA_TOP = 211, AREA_WIDTH = 972, AREA_HEIGHT = 522;
    private static final int GAP = 18;

    public static int clampColumns(int columns) {
        return Math.max(MIN_COLUMNS, Math.min(MAX_COLUMNS, columns));
    }

    public static ShopGrid fitting(int requestedColumns, float pixelsPerUnit) {
        return fitting(requestedColumns, pixelsPerUnit, true);
    }

    /**
     * The requested layout, or fewer cards per row while their price would be drawn below
     * {@link ShopSlotButton#MIN_READABLE_PIXELS}; never fewer than the original three unless asked for.
     *
     * @param pixelsPerUnit physical screen pixels per panel unit
     */
    public static ShopGrid fitting(int requestedColumns, float pixelsPerUnit, boolean hint) {
        int requested = clampColumns(requestedColumns);
        int floor = Math.min(requested, DEFAULT_COLUMNS);
        for (int columns = requested; columns > floor; columns--) {
            ShopGrid grid = of(columns, hint);
            if (grid.textScale(ShopSlotButton.PRICE_TEXT_SCALE) * pixelsPerUnit >= ShopSlotButton.MIN_READABLE_PIXELS) {
                return grid;
            }
        }
        return of(floor, hint);
    }

    public static ShopGrid of(int requestedColumns) {
        return of(requestedColumns, true);
    }

    public static ShopGrid of(int requestedColumns, boolean hint) {
        int columns = clampColumns(requestedColumns);
        int shapeWidth = ShopSlotButton.CARD_WIDTH;
        int shapeHeight = hint ? ShopSlotButton.CARD_HEIGHT : ShopSlotButton.COMPACT_CARD_HEIGHT;
        float width = Math.min(shapeWidth, (AREA_WIDTH - (columns - 1) * GAP) / (float) columns);
        float height = width * shapeHeight / shapeWidth;
        int rows = Math.max(1, Math.round((AREA_HEIGHT + GAP) / (height + GAP)));
        float rowHeight = (AREA_HEIGHT - (rows - 1) * GAP) / (float) rows;
        if (height > rowHeight) {
            height = rowHeight;
            width = height * shapeWidth / shapeHeight;
        }
        float left = AREA_LEFT + (AREA_WIDTH - (columns * width + (columns - 1) * GAP)) / 2f;
        float top = AREA_TOP + (AREA_HEIGHT - (rows * height + (rows - 1) * GAP)) / 2f;
        return new ShopGrid(columns, rows, width, height, left, top, hint);
    }

    /** Panel units per font pixel for card text of the given {@link ShopSlotButton} scale. */
    public float textScale(float cardTextScale) {
        return cardWidth / ShopSlotButton.CARD_WIDTH * cardTextScale;
    }

    /** Whether the buy hint would still be legible at {@code pixelsPerUnit}. */
    public boolean hintReadable(float pixelsPerUnit) {
        return hint && textScale(ShopSlotButton.HINT_TEXT_SCALE) * pixelsPerUnit >= ShopSlotButton.MIN_READABLE_PIXELS;
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
