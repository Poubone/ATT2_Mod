package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapStatBarTest {

    @AfterEach
    void tearDown() {
        MapStatBar.reset();
    }

    @Test
    void parseReadsPointTotalsFromTranslationArgs() {
        Component name = Component.literal("§6|")
                .append(Component.translatable("att2.stat.display.str", Component.literal("12")))
                .append(Component.translatable("att2.stat.display.crt", Component.literal("-1")));

        Map<String, Integer> parsed = MapStatBar.parse(name);

        assertEquals(12, parsed.get("STR_TOT"));
        assertEquals(-1, parsed.get("CRT_TOT"));
        assertTrue(MapStatBar.hasPointTotals(name));
    }

    @Test
    void parseFallsBackToFlattenedResolvedText() {
        Component name = Component.literal("§6|§eSTR§7:8§6|§eHAS§7:2§6|");
        Map<String, Integer> parsed = MapStatBar.parse(name);
        assertEquals(8, parsed.get("STR_TOT"));
        assertEquals(2, parsed.get("HAS_TOT"));
        assertTrue(MapStatBar.hasPointTotals(name));
    }

    @Test
    void introDahalLabelIsNotAStatBar() {
        Component name = Component.literal("Dahal").append(Component.literal("§6|"));
        assertTrue(MapStatBar.parse(name).isEmpty());
        assertFalse(MapStatBar.hasPointTotals(name));
        assertTrue(MapStatBar.isDahalBar(name));
    }

    @Test
    void detailedValueKeysAreNotPointTotals() {
        Component name = Component.literal("§6|")
                .append(Component.translatable("att2.stat.display.str", "5"))
                .append(Component.translatable("att2.stat.display.res.value", "40"));
        assertTrue(MapStatBar.parse(name).isEmpty());
        assertFalse(MapStatBar.hasPointTotals(name));
        assertTrue(MapStatBar.hasDetailedValues(name));
    }

    @Test
    void capturesFillProgressFromTheDahalBossbar() {
        BossEvent bar = new BossEvent(
                UUID.randomUUID(),
                Component.literal("Dahal").append(Component.literal("|")),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS) {};
        bar.setProgress(0.25f);
        MapStatBar.capture(Map.of(bar.getId(), bar), true);
        assertTrue(MapStatBar.hasDahalProgress());
        assertEquals(0.25f, MapStatBar.dahalProgress(), 1e-4f);

        MapStatBar.capture(Map.of(), true);
        assertFalse(MapStatBar.hasDahalProgress());
    }
}
