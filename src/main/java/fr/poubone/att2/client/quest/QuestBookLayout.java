package fr.poubone.att2.client.quest;

/** Responsive geometry in GUI pixels. Text is never reduced below Minecraft's native size. */
public record QuestBookLayout(int x, int y, int width, int height) {
    public static QuestBookLayout fit(int screenWidth, int screenHeight) {
        int width = Math.min(660, Math.max(1, screenWidth - 16));
        int height = Math.min(390, Math.max(1, screenHeight - 16));
        return new QuestBookLayout((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }
    public int pageWidth() { return (width - 26) / 2; }
    public int contentWidth() { return pageWidth() - 24; }
    public int leftX() { return x + 20; }
    public int rightX() { return x + width / 2 + 17; }
    public int detailTop() { return y + 70; }
    public int detailBottom() { return y + height - 39; }
    public int searchY() { return y + 44; }
    public int filterY() { return y + 70; }
    public int filterColumns() { return contentWidth() < 236 && height >= 210 ? 2 : 4; }
    public int filterWidth() { return (contentWidth() - (filterColumns() - 1) * 4) / filterColumns(); }
    public int listTop() { return filterY() + (4 / filterColumns()) * 22 + 6; }
    public int footerY() { return y + height - 34; }
    public int rowHeight() { return height < 210 ? 24 : width >= 500 ? 34 : 30; }
    public int rowsPerPage() { return Math.max(1, (footerY() - 8 - listTop()) / rowHeight()); }
    public float textScale() { return width >= 500 ? 1.15f : 1f; }
}
