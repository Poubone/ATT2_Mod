package fr.poubone.att2.client.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DahalAmountTest {

    @Test
    void reconstructsCurrentDahalFromBossbarProgressAndMax() {
        assertEquals(100, DahalAmount.current(0.25f, 400).orElse(-1));
        assertEquals(77, DahalAmount.current(77 / 200f, 200).orElse(-1));
        assertEquals(0, DahalAmount.current(0f, 80).orElse(-1));
        assertEquals(80, DahalAmount.current(1f, 80).orElse(-1));
    }

    @Test
    void emptyWhenMaxIsMissingOrProgressIsUnknown() {
        assertTrue(DahalAmount.current(-1f, 100).isEmpty());
        assertTrue(DahalAmount.current(0.5f, 0).isEmpty());
    }
}
