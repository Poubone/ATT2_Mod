package fr.poubone.att2.client.data;

import java.util.OptionalInt;

/**
 * Resolves the local player's score without {@code /scoreboard}.
 * Reads a score already present in the client's vanilla scoreboard.
 */
public final class ScoreboardLiveRead {
    private ScoreboardLiveRead() {
    }

    /**
     * @param requestedObjective     objective the HUD asked for
     * @param playerName             local player's scoreboard name
     * @param knownObjectiveName     {@code getObjective(requested)} name, or {@code null}
     * @param listDisplayObjective   objective currently on {@code DisplaySlot.LIST}, or {@code null}
     * @param directScore            {@code getPlayerScoreInfo} value, or {@code null} when unsynced
     * @param listed                 scores currently streamed for that objective (TAB entries)
     */
    public static OptionalInt read(
            String requestedObjective,
            String playerName,
            String knownObjectiveName,
            String listDisplayObjective,
            Integer directScore,
            Iterable<PlayerListScore.Entry> listed
    ) {
        if (requestedObjective == null || requestedObjective.isBlank()) return OptionalInt.empty();
        boolean available = requestedObjective.equals(knownObjectiveName)
                || requestedObjective.equals(listDisplayObjective);
        if (!available) return OptionalInt.empty();
        if (directScore != null) return OptionalInt.of(directScore);
        return PlayerListScore.find(playerName, listed);
    }
}
