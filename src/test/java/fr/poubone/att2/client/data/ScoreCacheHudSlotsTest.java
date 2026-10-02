package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreCacheHudSlotsTest {
    private static final ScoreHolder PLAYER = ScoreHolder.forNameOnly("Tester");

    @Test
    void eachHudScoreAppearsOnlyInItsReservedSlot() {
        Map<String, DisplaySlot> slots = Map.of(
                "CHRONOTON", DisplaySlot.TEAM_BLACK,
                "GAMELEVEL", DisplaySlot.TEAM_DARK_BLUE,
                "LEVELMASTER", DisplaySlot.TEAM_DARK_AQUA,
                "LVL_UPGRADE_REQ", DisplaySlot.TEAM_GRAY,
                "DAHALMAX", DisplaySlot.TEAM_DARK_GRAY
        );
        Scoreboard scoreboard = new Scoreboard();
        int value = 10;
        for (var entry : slots.entrySet()) {
            Objective objective = scoreboard.addObjective(entry.getKey(), ObjectiveCriteria.DUMMY,
                    Component.literal(entry.getKey()), ObjectiveCriteria.RenderType.INTEGER, false, null);
            scoreboard.getOrCreatePlayerScore(PLAYER, objective, true).set(value);
            assertTrue(ScoreCache.readFromScoreboard(scoreboard, PLAYER, entry.getKey()).isEmpty());

            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
            assertTrue(ScoreCache.readFromScoreboard(scoreboard, PLAYER, entry.getKey()).isEmpty());

            scoreboard.setDisplayObjective(entry.getValue(), objective);
            assertEquals(value, ScoreCache.readFromScoreboard(scoreboard, PLAYER, entry.getKey()).orElseThrow());

            scoreboard.setDisplayObjective(entry.getValue(), null);
            assertTrue(ScoreCache.readFromScoreboard(scoreboard, PLAYER, entry.getKey()).isEmpty());
            value++;
        }
    }
}
