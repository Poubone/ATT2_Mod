package fr.poubone.att2.client.data;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapStatBarParserTest {

    @Test
    void mapsPointDisplayKeyToTotObjective() {
        assertEquals("STR_TOT", MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.str").orElseThrow());
        assertEquals("CRT_TOT", MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.crt").orElseThrow());
        assertEquals("RES_TOT", MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.res").orElseThrow());
        assertEquals("SPD_TOT", MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.spd").orElseThrow());
        assertEquals("HAS_TOT", MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.has").orElseThrow());
        assertEquals("HER_TOT", MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.her").orElseThrow());
        assertEquals("DAR_TOT", MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.dar").orElseThrow());
        assertEquals("LUC_TOT", MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.luc").orElseThrow());
        assertEquals("HUN_TOT", MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.hun").orElseThrow());
    }

    @Test
    void ignoresDetailedValueKeys() {
        assertTrue(MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.res.value").isEmpty());
        assertTrue(MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.has.value.add").isEmpty());
        assertTrue(MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.spd.value.reduce").isEmpty());
        assertTrue(MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.dar.value").isEmpty());
    }

    @Test
    void ignoresUnrelatedKeys() {
        assertTrue(MapStatBarParser.objectiveFromTranslationKey("att2.stat.display.select.str").isEmpty());
        assertTrue(MapStatBarParser.objectiveFromTranslationKey("minecraft.boss.ender_dragon").isEmpty());
        assertTrue(MapStatBarParser.objectiveFromTranslationKey(null).isEmpty());
    }

    @Test
    void collectWritesFirstNumericArgument() {
        Map<String, Integer> out = new HashMap<>();
        MapStatBarParser.collect("att2.stat.display.str", List.of("12"), out);
        MapStatBarParser.collect("att2.stat.display.spd", List.of("-3"), out);
        MapStatBarParser.collect("att2.stat.display.res.value", List.of("40"), out);
        assertEquals(12, out.get("STR_TOT"));
        assertEquals(-3, out.get("SPD_TOT"));
        assertFalse(out.containsKey("RES_TOT"));
    }

    @Test
    void parseSignedIntStripsFormatting() {
        assertEquals(OptionalInt.of(12), MapStatBarParser.parseSignedInt("12"));
        assertEquals(OptionalInt.of(-4), MapStatBarParser.parseSignedInt("§c-4"));
        assertEquals(OptionalInt.of(7), MapStatBarParser.parseSignedInt("§a7§r"));
        assertTrue(MapStatBarParser.parseSignedInt("").isEmpty());
        assertTrue(MapStatBarParser.parseSignedInt(null).isEmpty());
    }

    @Test
    void collectFromFlattenedReadsPointTotals() {
        Map<String, Integer> out = new HashMap<>();
        MapStatBarParser.collectFromFlattened("§6|§eSTR§7:12§6|§eCRT§7:-1§6|", out);
        assertEquals(12, out.get("STR_TOT"));
        assertEquals(-1, out.get("CRT_TOT"));
        assertFalse(out.containsKey("RES_TOT"));
    }

    @Test
    void collectFromFlattenedIgnoresIntroDahalLabel() {
        Map<String, Integer> out = new HashMap<>();
        MapStatBarParser.collectFromFlattened("Dahal§6|", out);
        assertTrue(out.isEmpty());
    }

    @Test
    void looksLikePointTotalsRequiresStatTokens() {
        assertTrue(MapStatBarParser.looksLikePointTotals("§6|§eSTR§7:12§6|§eCRT§7:1§6|"));
        assertFalse(MapStatBarParser.looksLikePointTotals("Dahal§6|"));
        assertFalse(MapStatBarParser.looksLikePointTotals("Myrath"));
    }

    @Test
    void isStatDisplayTranslation() {
        assertTrue(MapStatBarParser.isStatDisplayTranslation("att2.stat.display.str"));
        assertTrue(MapStatBarParser.isStatDisplayTranslation("att2.stat.display.res.value"));
        assertFalse(MapStatBarParser.isStatDisplayTranslation("att2.mainquest.progress"));
        assertTrue(MapStatBarParser.isDetailedValueTranslation("att2.stat.display.res.value"));
        assertFalse(MapStatBarParser.isDetailedValueTranslation("att2.stat.display.str"));
    }
}
