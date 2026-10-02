package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatUpgradeModelTest {

    @AfterEach
    void tearDown() {
        StatUpgradeModel.reset();
        Att2Triggers.reset();
    }

    @Test
    void ignoresTheAttributeDialogUntilTheUpgradeScreenAskedForIt() {
        Component title = Component.translatable("consciousness.stat.title", "4");
        List<AttributeDialogParser.Action> actions = List.of(
                new AttributeDialogParser.Action(Component.translatable("consciousness.stat.point.add", "2"), 1077)
        );

        assertFalse(StatUpgradeModel.applyIfListening(title, actions));
        assertFalse(StatUpgradeModel.hasData());
    }

    @Test
    void fillsSkillPointsAndCostsFromTheConscienceDialog() {
        StatUpgradeModel.open();
        Component title = Component.translatable("consciousness.stat.title", "4");
        List<AttributeDialogParser.Action> actions = List.of(
                new AttributeDialogParser.Action(Component.translatable("consciousness.stat.point.add", "2"), 1077),
                new AttributeDialogParser.Action(Component.translatable("consciousness.stat.point.max"), 1078)
        );

        assertTrue(StatUpgradeModel.applyIfListening(title, actions));
        assertEquals(4, StatUpgradeModel.skillPoints());
        assertEquals(2, StatUpgradeModel.upgradeCost("STR").orElse(-1));
        assertEquals(0, StatUpgradeModel.upgradeCost("RES").orElse(-1));
        assertTrue(StatUpgradeModel.isListening());
    }

    @Test
    void fillsBaseLevelsFromActionTooltips() {
        StatUpgradeModel.open();
        Component title = Component.translatable("consciousness.stat.title", "4");
        List<AttributeDialogParser.Action> actions = List.of(
                new AttributeDialogParser.Action(Component.translatable("consciousness.stat.point.add", "2"), 1077)
        );
        Component tooltips = Component.empty();
        int[] vals = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int v : vals) {
            tooltips = tooltips.copy().append(
                    Component.translatable(AttributeDialogParser.POINT_KEY)
                            .append(Component.translatable("consciousness.stat.point.base", String.valueOf(v))));
        }
        assertTrue(StatUpgradeModel.applyIfListening(title, actions, Component.empty(), tooltips));
        assertEquals(1, StatUpgradeModel.base("STR").orElse(-1));
        assertEquals(4, StatUpgradeModel.base("HAS").orElse(-1));
        assertEquals(5, StatUpgradeModel.base("SPD").orElse(-1));
        assertEquals(9, StatUpgradeModel.base("HUN").orElse(-1));
    }

    @Test
    void keepsPotionAndEquipmentSplitFromTheSameDialog() {
        StatUpgradeModel.open();
        Component title = Component.translatable("consciousness.stat.title", "4");
        List<AttributeDialogParser.Action> actions = List.of(
                new AttributeDialogParser.Action(Component.translatable("consciousness.stat.point.add", "2"), 1077)
        );
        Component tooltips = Component.empty()
                .append(Component.translatable("consciousness.stat.point.base", "3"))
                .append(Component.translatable("consciousness.stat.point.equipment", "2"))
                .append(Component.translatable("consciousness.stat.point.potion", "1"));

        assertTrue(StatUpgradeModel.applyIfListening(title, actions, Component.empty(), tooltips));
        assertEquals(3, StatUpgradeModel.sources("STR").get("base"));
        assertEquals(2, StatUpgradeModel.sources("STR").get("equipment"));
        assertEquals(1, StatUpgradeModel.sources("STR").get("potion"));
    }

    @Test
    void closeStopsSwallowingFurtherConscienceDialogs() {
        StatUpgradeModel.open();
        StatUpgradeModel.close();
        Component title = Component.translatable("consciousness.stat.title", "1");
        assertFalse(StatUpgradeModel.applyIfListening(title, List.of()));
    }
}
