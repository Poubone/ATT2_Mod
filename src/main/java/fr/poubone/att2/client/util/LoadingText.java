package fr.poubone.att2.client.util;

import java.util.OptionalInt;

/** Shown in place of a value the map has not sent yet (scores arrive a moment after joining). */
public final class LoadingText {
    public static final String PLACEHOLDER = "...";

    private LoadingText() {
    }

    public static String of(OptionalInt value) {
        return value.isPresent() ? Integer.toString(value.getAsInt()) : PLACEHOLDER;
    }

    public static String of(Integer value) {
        return value != null ? value.toString() : PLACEHOLDER;
    }
}
