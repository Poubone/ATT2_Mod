package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemperatureModelTest {
    private static final ScoreHolder PLAYER = ScoreHolder.forNameOnly("Tester");

    @Test void readsOnlyTheLocalPlayersLiveTabScore() {
        Scoreboard scoreboard = new Scoreboard();
        var objective = scoreboard.addObjective("TEMPERATURE", ObjectiveCriteria.DUMMY,
                Component.literal("Temperature"), ObjectiveCriteria.RenderType.INTEGER, false, null);
        scoreboard.setDisplayObjective(DisplaySlot.LIST, objective);
        scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("Other"), objective, true).set(900);
        scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("#Environment"), objective, true).set(-7);
        assertTrue(TemperatureModel.read(scoreboard, PLAYER).isEmpty());
        scoreboard.getOrCreatePlayerScore(PLAYER, objective, true).set(-420);
        assertEquals(-420, TemperatureModel.read(scoreboard, PLAYER).orElseThrow());
        scoreboard.getOrCreatePlayerScore(PLAYER, objective, true).set(650);
        assertEquals(650, TemperatureModel.read(scoreboard, PLAYER).orElseThrow());
        scoreboard.getOrCreatePlayerScore(PLAYER, objective, true).set(0);
        assertEquals(0, TemperatureModel.read(scoreboard, PLAYER).orElseThrow());
    }

    @Test void stopsDisplayingWhenTabObjectiveChangesOrIsCleared() {
        Scoreboard scoreboard = new Scoreboard();
        var temperature = scoreboard.addObjective("TEMPERATURE", ObjectiveCriteria.DUMMY,
                Component.literal("Temperature"), ObjectiveCriteria.RenderType.INTEGER, false, null);
        var other = scoreboard.addObjective("OTHER", ObjectiveCriteria.DUMMY,
                Component.literal("Other"), ObjectiveCriteria.RenderType.INTEGER, false, null);
        scoreboard.getOrCreatePlayerScore(PLAYER, temperature, true).set(800);
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, temperature);
        assertTrue(TemperatureModel.read(scoreboard, PLAYER).isEmpty());
        scoreboard.setDisplayObjective(DisplaySlot.LIST, temperature);
        assertEquals(800, TemperatureModel.read(scoreboard, PLAYER).orElseThrow());
        scoreboard.setDisplayObjective(DisplaySlot.LIST, other);
        assertTrue(TemperatureModel.read(scoreboard, PLAYER).isEmpty());
        scoreboard.setDisplayObjective(DisplaySlot.LIST, null);
        assertTrue(TemperatureModel.read(scoreboard, PLAYER).isEmpty());
        assertTrue(TemperatureModel.read(null, PLAYER).isEmpty());
        assertTrue(TemperatureModel.read(scoreboard, null).isEmpty());
    }

    @Test void stagesRespectNeutralBandAndMapThresholdOnBothSides() {
        int[] values = {0, 1, 100, 101, 399, 400, 799, 800, 1500};
        int[] stages = {0, 0, 0, 1, 1, 2, 2, 3, 3};
        for (int i = 0; i < values.length; i++) {
            assertEquals(stages[i], TemperatureModel.stage(values[i]));
            assertEquals(stages[i], TemperatureModel.stage(-values[i]));
        }
        assertEquals(3, TemperatureModel.stage(Integer.MIN_VALUE));
        assertEquals(3, TemperatureModel.stage(Integer.MAX_VALUE));
    }

    @Test void labelsPreserveExactSignedValuesInMapUnits() {
        assertEquals("+800", TemperatureModel.label(800));
        assertEquals("-800", TemperatureModel.label(-800));
        assertEquals("0", TemperatureModel.label(0));
        assertEquals("-2147483648", TemperatureModel.label(Integer.MIN_VALUE));
    }
    @org.junit.jupiter.api.Test
    void orbFillClampsBothSignsAndExtremeValues() {
        org.junit.jupiter.api.Assertions.assertEquals(0f, TemperatureModel.fillRatio(0));
        org.junit.jupiter.api.Assertions.assertEquals(0.5f, TemperatureModel.fillRatio(400));
        org.junit.jupiter.api.Assertions.assertEquals(0.5f, TemperatureModel.fillRatio(-400));
        org.junit.jupiter.api.Assertions.assertEquals(1f, TemperatureModel.fillRatio(800));
        org.junit.jupiter.api.Assertions.assertEquals(1f, TemperatureModel.fillRatio(-800));
        org.junit.jupiter.api.Assertions.assertEquals(1f, TemperatureModel.fillRatio(Integer.MIN_VALUE));
        org.junit.jupiter.api.Assertions.assertEquals(1f, TemperatureModel.fillRatio(Integer.MAX_VALUE));
    }
}
