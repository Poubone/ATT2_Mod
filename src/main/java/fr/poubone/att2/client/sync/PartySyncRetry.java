package fr.poubone.att2.client.sync;

import java.util.function.BooleanSupplier;

/** One bounded retry for throttling, on the IO thread, compatible with older relays. */
final class PartySyncRetry {
    @FunctionalInterface interface Attempt { void run() throws Exception; }
    @FunctionalInterface interface Delay { void waitFor(long milliseconds) throws InterruptedException; }

    static void publish(Attempt attempt, Delay delay, BooleanSupplier currentSession) throws Exception {
        if (!currentSession.getAsBoolean()) return;
        try {
            attempt.run();
        } catch (PartySyncHttp.ApiException failure) {
            if (failure.status != 429) throw failure;
            delay.waitFor(failure.retryAfterMs);
            if (currentSession.getAsBoolean()) attempt.run();
        }
    }
}
