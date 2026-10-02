package fr.poubone.att2.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves persisted HUD boxes to pixels and supplies the default arrangement
 * taken from the in-game layout (top-right XP/chronotons, Dahäl above the hotbar,
 * stats bottom-left, spells on the left, armor/arrows on the hotbar flanks).
 */
public final class HudLayout {
    public static final String XP = "xp";
    public static final String CHRONOTON = "chronoton";
    public static final String MANA = "mana";
    public static final String STATS = "stats";
    public static final String ARROWS = "arrows";
    public static final String ARMOR = "armor";
    public static final String SPELLS = "spells";
    public static final String TEMPERATURE = "temperature";

    public static final String[] IDS = {XP, CHRONOTON, MANA, STATS, ARROWS, ARMOR, SPELLS, TEMPERATURE};

    public static final float SCALE_MIN = 0.4f;
    public static final float SCALE_MAX = 3.0f;
    public static final float SCALE_STEP = 0.1f;
    public static final float GAP_MIN = 0.3f;
    public static final float GAP_MAX = 2.0f;
    public static final float GAP_STEP = 0.1f;

    private static final Map<String, HudSlot> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(XP, new HudSlot(0.88056f, 0.00438f, 0.11944f, 0.03501f, 1.9f));
        DEFAULTS.put(CHRONOTON, new HudSlot(0.93677f, 0.05689f, 0.06323f, 0.03939f, 2.6f));
        DEFAULTS.put(MANA, new HudSlot(0.88525f, 0.75930f, 0.07494f, 0.15317f, 1.0f));
        DEFAULTS.put(STATS, new HudSlot(0.01f, 0.86f, 0.18f, 0.11f, 1.1f));
        DEFAULTS.put(ARROWS, new HudSlot(0.28337f, 0.94530f, 0.09016f, 0.05470f, 0.8f));
        DEFAULTS.put(ARMOR, new HudSlot(0.62295f, 0.92779f, 0.14052f, 0.06127f, 1.3f));
        DEFAULTS.put(TEMPERATURE, new HudSlot(0.86f, 0.115f, 0.14f, 0.12f, 1.0f));
        DEFAULTS.put(SPELLS, new HudSlot(0.0f, 0.24508f, 0.03747f, 0.09190f, 0.4f));
    }

    private HudLayout() {
    }

    public static HudSlot slot(String id) {
        HUDConfig config = HUDConfig.get();
        if (config.slots == null) {
            config.slots = new LinkedHashMap<>();
        }
        return config.slots.computeIfAbsent(id, key -> defaultSlot(key).copy());
    }

    public static HudSlot defaultSlot(String id) {
        HudSlot fallback = DEFAULTS.get(id);
        return fallback == null ? new HudSlot(0.4f, 0.4f, 0.15f, 0.08f) : fallback;
    }

    public static void resetAll() {
        HUDConfig config = HUDConfig.get();
        if (config.slots == null) {
            config.slots = new LinkedHashMap<>();
        }
        config.slots.clear();
        for (String id : IDS) {
            config.slots.put(id, defaultSlot(id).copy());
        }
    }

    public static Box box(String id) {
        Minecraft client = Minecraft.getInstance();
        int sw = Math.max(1, client.getWindow().getGuiScaledWidth());
        int sh = Math.max(1, client.getWindow().getGuiScaledHeight());
        return box(id, sw, sh);
    }

    public static Box box(String id, int screenW, int screenH) {
        return boxFromSlot(slot(id), screenW, screenH);
    }

    /** Pixel box for a persisted slot. {@code slot.x == 0} is flush left; {@code slot.x == 1} is flush right. */
    public static Box boxFromSlot(HudSlot slot, int screenW, int screenH) {
        int w = Mth.clamp(Math.round(slot.w * screenW), 24, screenW);
        int h = Mth.clamp(Math.round(slot.h * screenH), 14, screenH);
        int x = Mth.clamp(Math.round(slot.x * screenW), 0, Math.max(0, screenW - w));
        int y = Mth.clamp(Math.round(slot.y * screenH), 0, Math.max(0, screenH - h));
        if (x + w > screenW) x = Math.max(0, screenW - w);
        if (y + h > screenH) y = Math.max(0, screenH - h);
        return new Box(x, y, w, h);
    }

    /**
     * Integer snap that sticks to {@code min}/{@code max} when within {@code snap} pixels,
     * so a box can sit flush on either edge instead of remaining 1–2 px away.
     */
    public static int snapToEdge(int value, int min, int max, int snap) {
        int clamped = Mth.clamp(value, min, max);
        if (clamped - min <= snap) return min;
        if (max - clamped <= snap) return max;
        if (snap <= 0) return clamped;
        return Mth.clamp((clamped / snap) * snap, min, max);
    }

    /** Persisted X fraction. Pixel 0 stays 0; a box flush right is stored as {@code 1}. */
    public static float fractionX(int x, int w, int sw) {
        return edgeFraction(x, w, sw);
    }

    public static float fractionY(int y, int h, int sh) {
        return edgeFraction(y, h, sh);
    }

    private static float edgeFraction(int pos, int size, int screen) {
        if (screen <= 0) return 0f;
        if (pos <= 0) return 0f;
        int maxPos = Math.max(0, screen - Math.max(0, size));
        if (maxPos > 0 && pos >= maxPos) return 1f;
        return Mth.clamp(pos / (float) screen, 0f, 1f);
    }

    public static float contentScale(String id, int intrinsicW, int intrinsicH) {
        float fit = box(id).fit(intrinsicW, intrinsicH);
        float user = slot(id).scale;
        if (user <= 0f) user = 1f;
        user = Mth.clamp(user, SCALE_MIN, SCALE_MAX);
        return fit * user;
    }

    public static void adjustScale(String id, float delta) {
        HudSlot s = slot(id);
        float next = (s.scale <= 0f ? 1f : s.scale) + delta;
        s.scale = Mth.clamp(next, SCALE_MIN, SCALE_MAX);
        s.scale = Math.round(s.scale * 10f) / 10f;
    }

    public static void adjustGap(String id, float delta) {
        HudSlot s = slot(id);
        float next = (s.gap <= 0f ? 1f : s.gap) + delta;
        s.gap = Mth.clamp(next, GAP_MIN, GAP_MAX);
        s.gap = Math.round(s.gap * 10f) / 10f;
    }

    public static void writePixels(String id, int x, int y, int w, int h) {
        Minecraft client = Minecraft.getInstance();
        int sw = Math.max(1, client.getWindow().getGuiScaledWidth());
        int sh = Math.max(1, client.getWindow().getGuiScaledHeight());
        HudSlot slot = slot(id);
        slot.x = fractionX(x, w, sw);
        slot.y = fractionY(y, h, sh);
        slot.w = Mth.clamp(w / (float) sw, 0.03f, 1f);
        slot.h = Mth.clamp(h / (float) sh, 0.025f, 1f);
    }

    public static String labelKey(String id) {
        return "screen.hud_config.slot." + id;
    }

    public record Box(int x, int y, int w, int h) {
        public float fit(int intrinsicW, int intrinsicH) {
            if (intrinsicW <= 0 || intrinsicH <= 0) return 1f;
            return Mth.clamp(Math.min(w / (float) intrinsicW, h / (float) intrinsicH), 0.4f, 3.0f);
        }
    }
}
