package fr.poubone.att2.client.hud;

/**
 * One HUD box, stored as fractions of the scaled screen so it survives resolution / GUI scale changes.
 */
public final class HudSlot {
    public float x;
    public float y;
    public float w;
    public float h;
    public float scale = 1f;
    /** Extra stride between stat cells (1 = current default). Ignored by non-stat widgets. */
    public float gap = 1f;

    public HudSlot() {
    }

    public HudSlot(float x, float y, float w, float h) {
        this(x, y, w, h, 1f);
    }

    public HudSlot(float x, float y, float w, float h, float scale) {
        this(x, y, w, h, scale, 1f);
    }

    public HudSlot(float x, float y, float w, float h, float scale, float gap) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.scale = scale;
        this.gap = gap;
    }

    public HudSlot copy() {
        return new HudSlot(x, y, w, h, scale, gap);
    }
}
