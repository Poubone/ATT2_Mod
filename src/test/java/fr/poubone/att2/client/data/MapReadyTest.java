package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapReadyTest {

    @BeforeEach
    @AfterEach
    void tearDown() {
        ScoreCache.clear();
        MapStatBar.reset();
    }

    @Test
    void staysQuietUntilAMapStartSignal() {
        assertFalse(MapReady.isReady(false, false, 0));
        assertFalse(MapReady.isReady(false, false, -1));
    }

    @Test
    void isReadyWhenChronotonScoreExists() {
        assertTrue(MapReady.isReady(true, false, 0));
    }

    @Test
    void isReadyWhenDahalOrStatsBossbarIsVisible() {
        assertTrue(MapReady.isReady(false, true, 0));
    }

    @Test
    void isReadyWhenNumeroJoueurIsAssigned() {
        assertTrue(MapReady.isReady(false, false, 1));
        assertTrue(MapReady.isReady(false, false, 4));
    }

    @Test
    void cachedChronotonDoesNotPretendToBeSynced() {
        assertFalse(MapReady.isReady());
        ScoreCache.put("CHRONOTON", 0);
        assertFalse(MapReady.isReady());
    }

    @Test
    void liveHelperSeesNumeroJoueur() {
        ScoreCache.put("NUMEROJOUEUR", 1);
        assertTrue(MapReady.isReady());
    }

    @Test
    void liveHelperSeesDahalBossbar() {
        BossEvent bar = new BossEvent(
                UUID.randomUUID(),
                Component.literal("Dahal").append(Component.literal("|")),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS) {};
        MapStatBar.capture(Map.of(bar.getId(), bar), true);
        assertTrue(MapReady.isReady());
    }
}
