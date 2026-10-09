package com.lootbeams.managers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class BeamBudgetTest {
    @Test void keepsTheNearest() {
        Map<String, Double> distances = new LinkedHashMap<>();
        distances.put("far", 400.0);
        distances.put("near", 1.0);
        distances.put("mid", 25.0);
        distances.put("farther", 900.0);
        Set<String> kept = BeamBudget.nearest(distances, 2);
        assertEquals(2, kept.size());
        assertTrue(kept.contains("near"));
        assertTrue(kept.contains("mid"));
    }

    @Test void keepsEverythingUnderTheLimit() {
        Map<String, Double> distances = Map.of("a", 4.0, "b", 9.0);
        assertEquals(2, BeamBudget.nearest(distances, 5).size());
    }
}
