package fr.poubone.att2.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudLayoutClampTest {

    @Test
    void boxSitsFlushAgainstTheLeftEdge() {
        HudSlot slot = new HudSlot(0f, 0.2f, 0.18f, 0.11f, 1f);
        HudLayout.Box box = HudLayout.boxFromSlot(slot, 640, 360);
        assertEquals(0, box.x());
    }

    @Test
    void boxSitsFlushAgainstTheRightEdge() {
        HudSlot slot = new HudSlot(1f, 0.2f, 0.18f, 0.11f, 1f);
        HudLayout.Box box = HudLayout.boxFromSlot(slot, 640, 360);
        assertEquals(box.w(), 640 - box.x());
        assertEquals(0, Math.max(0, box.x() + box.w() - 640));
    }

    @Test
    void dragSnapSticksToTheLeftEdge() {
        assertEquals(0, HudLayout.snapToEdge(0, 0, 500, 2));
        assertEquals(0, HudLayout.snapToEdge(1, 0, 500, 2));
        assertEquals(0, HudLayout.snapToEdge(2, 0, 500, 2));
    }

    @Test
    void dragSnapSticksToTheRightEdge() {
        assertEquals(500, HudLayout.snapToEdge(500, 0, 500, 2));
        assertEquals(500, HudLayout.snapToEdge(499, 0, 500, 2));
        assertEquals(500, HudLayout.snapToEdge(498, 0, 500, 2));
    }

    @Test
    void fractionsWrittenAtPixelZeroStayAtZero() {
        float x = HudLayout.fractionX(0, 100, 640);
        assertEquals(0f, x, 0.00001f);
        HudSlot slot = new HudSlot(x, 0.2f, 100 / 640f, 0.11f, 1f);
        assertEquals(0, HudLayout.boxFromSlot(slot, 640, 360).x());
    }
}
