package fr.poubone.att2.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Optional replay protection, inspired by Simuciokas/att2-rebuild (MIT). */
public final class FlashbackCompat {
    private static final Detector DETECTOR = detect();

    private FlashbackCompat() {}

    private static Detector detect() {
        Consumer<Throwable> warning = failure -> LoggerFactory.getLogger("att2-flashback").warn(
                "Flashback compatibility failed; ATT2 gameplay integrations are suspended to protect replay playback", failure);
        if (!FabricLoader.getInstance().isModLoaded("flashback")) return new Detector(null, false, warning);
        try {
            return Detector.forClass(Class.forName("com.moulberry.flashback.Flashback"), warning);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            warning.accept(failure);
            return new Detector(null, true, warning);
        }
    }

    /** Also suspends integrations when an installed Flashback has an incompatible API. */
    public static boolean isInReplay() {
        return DETECTOR.isInReplay();
    }

    // Independent of Fabric/Minecraft so the optional API and failure policy can be tested.
    static final class Detector {
        private final Method method;
        private final boolean unavailable;
        private final Consumer<Throwable> warning;
        private final AtomicBoolean failed = new AtomicBoolean();

        Detector(Method method, boolean unavailable, Consumer<Throwable> warning) {
            this.method = method;
            this.unavailable = unavailable;
            this.warning = warning;
        }

        static Detector forClass(Class<?> type, Consumer<Throwable> warning) throws NoSuchMethodException {
            Method method = type.getMethod("isInReplay");
            if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != boolean.class) {
                throw new NoSuchMethodException("Flashback.isInReplay must be public static boolean");
            }
            return new Detector(method, false, warning);
        }

        boolean isInReplay() {
            if (method == null) return unavailable;
            if (failed.get()) return true;
            try {
                return (boolean) method.invoke(null);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
                Throwable cause = failure instanceof InvocationTargetException invocation ? invocation.getCause() : failure;
                if (cause instanceof Error fatal && !(cause instanceof LinkageError)) throw fatal;
                if (failed.compareAndSet(false, true)) warning.accept(cause);
                return true;
            }
        }
    }
}
