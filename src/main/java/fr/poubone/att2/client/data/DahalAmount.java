package fr.poubone.att2.client.data;

import java.util.OptionalInt;

/** Reconstructs current Dahäl from the map bossbar's 0–1 progress and a known DAHALMAX. */
public final class DahalAmount {
    private DahalAmount() {
    }

    public static OptionalInt current(float progress, int max) {
        if (max <= 0 || progress < 0f || Float.isNaN(progress)) return OptionalInt.empty();
        float clamped = Math.min(1f, progress);
        return OptionalInt.of(Math.round(clamped * max));
    }
}
