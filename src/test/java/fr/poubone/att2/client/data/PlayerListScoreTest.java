package fr.poubone.att2.client.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerListScoreTest {

    @Test
    void findsTheLocalPlayersScoreOnTheTabList() {
        int value = PlayerListScore.find("Steve", List.of(
                new PlayerListScore.Entry("Alex", 3),
                new PlayerListScore.Entry("Steve", 120)
        )).orElse(-1);
        assertEquals(120, value);
    }

    @Test
    void missesWhenThePlayerIsNotListed() {
        assertTrue(PlayerListScore.find("Steve", List.of(new PlayerListScore.Entry("Alex", 3))).isEmpty());
    }
}
