package fr.poubone.att2.client.data;

import java.util.OptionalInt;

/** Looks up the local player's score among TAB-list ({@code setdisplay list}) entries. */
public final class PlayerListScore {
    private PlayerListScore() {
    }

    public record Entry(String owner, int value) {
    }

    public static OptionalInt find(String playerName, Iterable<Entry> listed) {
        if (playerName == null || listed == null) return OptionalInt.empty();
        for (Entry entry : listed) {
            if (entry != null && playerName.equals(entry.owner())) {
                return OptionalInt.of(entry.value());
            }
        }
        return OptionalInt.empty();
    }
}
