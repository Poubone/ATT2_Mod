package fr.poubone.att2.client.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreboardLiveReadTest {

    @Test
    void prefersTheDirectPlayerScoreWhenTheObjectiveIsSynced() {
        int value = ScoreboardLiveRead.read(
                "CHRONOTON",
                "Steve",
                "CHRONOTON",
                "CHRONOTON",
                42,
                List.of(new PlayerListScore.Entry("Steve", 1))
        ).orElse(-1);
        assertEquals(42, value);
    }

    @Test
    void fallsBackToTheTabListWhenTheDirectScoreIsMissing() {
        int value = ScoreboardLiveRead.read(
                "CHRONOTON",
                "Steve",
                "CHRONOTON",
                "CHRONOTON",
                null,
                List.of(
                        new PlayerListScore.Entry("Alex", 3),
                        new PlayerListScore.Entry("Steve", 120)
                )
        ).orElse(-1);
        assertEquals(120, value);
    }

    @Test
    void readsChronotonsFromTheListDisplaySlotEvenIfGetObjectiveIsNull() {
        int value = ScoreboardLiveRead.read(
                "CHRONOTON",
                "Steve",
                null,
                "CHRONOTON",
                null,
                List.of(new PlayerListScore.Entry("Steve", 77))
        ).orElse(-1);
        assertEquals(77, value);
    }

    @Test
    void ignoresTheTabListWhenItShowsADifferentObjective() {
        assertTrue(ScoreboardLiveRead.read(
                "DAHAL",
                "Steve",
                null,
                "CHRONOTON",
                null,
                List.of(new PlayerListScore.Entry("Steve", 120))
        ).isEmpty());
    }
}
