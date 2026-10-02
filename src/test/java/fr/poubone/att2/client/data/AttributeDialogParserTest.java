package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeDialogParserTest {

    @AfterEach
    void tearDown() {
        StatUpgradeModel.reset();
    }

    @Test
    void readsSkillPointsAndUpgradeCostsFromConscienceDialog() {
        Component title = Component.translatable("consciousness.stat.title", "4");
        List<AttributeDialogParser.Action> actions = List.of(
                new AttributeDialogParser.Action(Component.translatable("consciousness.stat.point.add", "2"), 1077),
                new AttributeDialogParser.Action(Component.translatable("consciousness.stat.point.add.error", "5"), 3529)
        );

        AttributeDialogParser.Snapshot snap = AttributeDialogParser.parse(title, actions).orElseThrow();

        assertEquals(4, snap.skillPoints());
        assertEquals(2, snap.cost("STR"));
        assertEquals(5, snap.cost("CRT"));
    }

    @Test
    void maxedStatIsStoredAsZeroCost() {
        Component title = Component.translatable("consciousness.stat.title", "1");
        List<AttributeDialogParser.Action> actions = List.of(
                new AttributeDialogParser.Action(Component.translatable("consciousness.stat.point.max"), 1078)
        );

        AttributeDialogParser.Snapshot snap = AttributeDialogParser.parse(title, actions).orElseThrow();

        assertEquals(0, snap.cost("RES"));
    }

    @Test
    void ignoresUnrelatedDialogs() {
        Component title = Component.translatable("consciousness.sidequest.title", "1", "60");
        assertTrue(AttributeDialogParser.parse(title, List.of()).isEmpty());
    }

    @Test
    void fillsBaseLevelsFromDialogBody() {
        Component body = Component.empty();
        int[] vals = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int v : vals) {
            body = body.copy().append(Component.translatable("consciousness.stat.point.base", String.valueOf(v)));
        }
        assertEquals(Map.of(
                "STR", 1, "CRT", 2, "RES", 3, "HAS", 4, "SPD", 5, "HER", 6, "DAR", 7, "LUC", 8, "HUN", 9
        ), AttributeDialogParser.parseBases(body));
    }

    @Test
    void splitsSourceLinesByStatInConscienceOrder() {
        Component tooltips = Component.empty()
                .append(Component.translatable("consciousness.stat.point.base", "3"))
                .append(Component.translatable("consciousness.stat.point.equipment", "2"))
                .append(Component.translatable("consciousness.stat.point.potion", "1"))
                .append(Component.translatable("consciousness.stat.point.base", "0"))
                .append(Component.translatable("consciousness.stat.point.spell", "-1"));

        Map<String, Map<String, Integer>> sources = AttributeDialogParser.parseSources(tooltips);

        assertEquals(Map.of("base", 3, "equipment", 2, "potion", 1), sources.get("STR"));
        assertEquals(Map.of("base", 0, "spell", -1), sources.get("CRT"));
        assertTrue(sources.get("RES") == null);
    }

    @Test
    void parseBasesExtractsBaseFromStatPointTooltip() {
        Component tooltip = Component.translatable(AttributeDialogParser.POINT_KEY)
                .append(Component.translatable("consciousness.stat.point.base", "3"))
                .append(Component.translatable("consciousness.stat.point.eq", "1"));
        assertEquals(Map.of("STR", 3), AttributeDialogParser.parseBases(tooltip));
    }
}
