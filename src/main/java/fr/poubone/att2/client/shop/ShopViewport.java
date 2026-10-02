package fr.poubone.att2.client.shop;

/** Coordinates in Minecraft GUI units: honor GUI scale, then shrink to fit small windows. */
public record ShopViewport(float scale, float left, float top) {
    public static ShopViewport fit(int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
        float scale = Math.max(0.01f, Math.min(1f,
                Math.min((screenWidth - 12f) / panelWidth, (screenHeight - 12f) / panelHeight)));
        return new ShopViewport(scale, (screenWidth - panelWidth * scale) / 2f,
                (screenHeight - panelHeight * scale) / 2f);
    }
    public double localX(double x) { return (x - left) / scale; }
    public double localY(double y) { return (y - top) / scale; }
    public int screenX(double x) { return Math.round((float) x * scale + left); }
    public int screenY(double y) { return Math.round((float) y * scale + top); }
}
