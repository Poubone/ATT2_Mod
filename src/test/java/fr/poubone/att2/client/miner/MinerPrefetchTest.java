package fr.poubone.att2.client.miner;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class MinerPrefetchTest {
    @Test void sendsMenuTriggersWithDelay() {
        MinerPrefetch q = new MinerPrefetch(new int[]{3489, 3488, 3535}, 2);
        List<Integer> sent = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            q.tick().ifPresent(sent::add);
        }
        assertEquals(List.of(3489, 3488, 3535), sent);
        assertTrue(q.isDone());
        assertTrue(q.tick().isEmpty());
    }

    @Test void cancelStopsFurtherSends() {
        MinerPrefetch q = new MinerPrefetch(MinerPrefetch.DEFAULT_TRIGGERS, 2);
        assertEquals(3489, q.tick().orElse(-1));
        q.cancel();
        assertTrue(q.tick().isEmpty());
        assertTrue(q.isDone());
    }
}
