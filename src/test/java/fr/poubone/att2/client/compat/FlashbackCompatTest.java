package fr.poubone.att2.client.compat;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

public class FlashbackCompatTest {
    public static class ReplayApi {
        static boolean replay;
        public static boolean isInReplay() { return replay; }
    }
    public static class WrongReturnApi {
        public static String isInReplay() { return "replay"; }
    }
    public static class InstanceApi {
        public boolean isInReplay() { return true; }
    }
    public static class BrokenApi {
        public static boolean isInReplay() { throw new IllegalStateException("broken API"); }
    }
    public static class FatalApi {
        public static boolean isInReplay() { throw new OutOfMemoryError("fatal"); }
    }

    @Test void absentModKeepsIntegrationsEnabled() {
        var detector = new FlashbackCompat.Detector(null, false, failure -> fail(failure));
        assertFalse(detector.isInReplay());
    }

    @Test void installedButUnavailableApiSuspendsIntegrations() {
        var detector = new FlashbackCompat.Detector(null, true, failure -> fail(failure));
        assertTrue(detector.isInReplay());
    }

    @Test void replayEntryExitAndReentryAreObserved() throws Exception {
        var detector = FlashbackCompat.Detector.forClass(ReplayApi.class, failure -> fail(failure));
        ReplayApi.replay = false;
        assertFalse(detector.isInReplay());
        ReplayApi.replay = true;
        assertTrue(detector.isInReplay());
        ReplayApi.replay = false;
        assertFalse(detector.isInReplay());
        ReplayApi.replay = true;
        assertTrue(detector.isInReplay());
        ReplayApi.replay = false;
    }

    @Test void incompatibleMethodShapesAreRejected() {
        assertThrows(NoSuchMethodException.class, () -> FlashbackCompat.Detector.forClass(
                WrongReturnApi.class, failure -> fail(failure)));
        assertThrows(NoSuchMethodException.class, () -> FlashbackCompat.Detector.forClass(
                InstanceApi.class, failure -> fail(failure)));
        assertThrows(NoSuchMethodException.class, () -> FlashbackCompat.Detector.forClass(
                Object.class, failure -> fail(failure)));
    }

    @Test void failingApiSuspendsAndWarnsOnlyOnce() throws Exception {
        AtomicInteger warnings = new AtomicInteger();
        var detector = FlashbackCompat.Detector.forClass(BrokenApi.class, failure -> {
            assertInstanceOf(IllegalStateException.class, failure);
            warnings.incrementAndGet();
        });
        assertTrue(detector.isInReplay());
        assertTrue(detector.isInReplay());
        assertEquals(1, warnings.get());
    }

    @Test void fatalVmErrorsAreNotHidden() throws Exception {
        var detector = FlashbackCompat.Detector.forClass(FatalApi.class, failure -> fail(failure));
        assertThrows(OutOfMemoryError.class, detector::isInReplay);
    }
}
