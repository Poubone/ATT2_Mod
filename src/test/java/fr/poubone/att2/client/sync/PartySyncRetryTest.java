package fr.poubone.att2.client.sync;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PartySyncRetryTest {
    @Test void queuedPublishIsCancelledBeforeSending() throws Exception {
        PartySyncRetry.publish(() -> fail("Cancelled session must not send"),
                delay -> fail("Cancelled session must not wait"), () -> false);
    }

    @Test void olderRelayThrottleRetriesOnceAfterDelay() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        PartySyncRetry.publish(() -> {
            if (calls.incrementAndGet() == 1) throw new PartySyncHttp.ApiException(429, 1000);
        }, delay -> assertEquals(1000, delay), () -> true);
        assertEquals(2, calls.get());
    }

    @Test void retryIsCancelledOnWorldChange() throws Exception {
        AtomicBoolean current = new AtomicBoolean(true);
        AtomicInteger calls = new AtomicInteger();
        PartySyncRetry.publish(() -> {
            calls.incrementAndGet();
            throw new PartySyncHttp.ApiException(429, 1000);
        }, delay -> current.set(false), current::get);
        assertEquals(1, calls.get());
    }

    @Test void persistentThrottleDoesNotFloodRelay() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(PartySyncHttp.ApiException.class, () -> PartySyncRetry.publish(() -> {
            calls.incrementAndGet();
            throw new PartySyncHttp.ApiException(429, 1000);
        }, delay -> {}, () -> true));
        assertEquals(2, calls.get());
    }

    @Test void otherFailuresAreNotRetriedAndHeaderIsBounded() {
        assertThrows(PartySyncHttp.ApiException.class, () -> PartySyncRetry.publish(() -> {
            throw new PartySyncHttp.ApiException(400, 1000);
        }, delay -> fail("Must not retry malformed requests"), () -> true));
        assertEquals(1000, PartySyncHttp.retryDelay("invalid"));
        assertEquals(1000, PartySyncHttp.retryDelay("-2"));
        assertEquals(5000, PartySyncHttp.retryDelay("9223372036854775807"));
        assertEquals(2000, PartySyncHttp.retryDelay("2"));
    }
}
