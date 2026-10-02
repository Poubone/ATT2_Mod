package fr.poubone.att2.client.data;

import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import java.util.OptionalInt;

/** Body temperature, streamed by the map's TAB objective; these are map units, not Celsius. */
public final class TemperatureModel {
    public static final String OBJECTIVE = "TEMPERATURE";
    public static final int NORMAL_LIMIT = 100;
    public static final int EFFECT_THRESHOLD = 800;

    private TemperatureModel() {}

    /** Never use stale cached scores when TAB stops displaying temperature. */
    public static OptionalInt read(Scoreboard scoreboard, ScoreHolder player) {
        if (scoreboard == null || player == null) return OptionalInt.empty();
        var objective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
        if (objective == null || !OBJECTIVE.equals(objective.getName())) return OptionalInt.empty();
        var score = scoreboard.getPlayerScoreInfo(player, objective);
        return score == null ? OptionalInt.empty() : OptionalInt.of(score.value());
    }

    /** Visual steps: neutral band, mild deviation, halfway to the map's effect threshold, threshold. */
    public static int stage(int value) {
        long magnitude = Math.abs((long) value);
        if (magnitude <= NORMAL_LIMIT) return 0;
        if (magnitude < EFFECT_THRESHOLD / 2) return 1;
        if (magnitude < EFFECT_THRESHOLD) return 2;
        return 3;
    }

    /** Full at the map's hot/cold effect threshold, with overflow-safe magnitude. */
    public static float fillRatio(int value) {
        return Math.min(1f, Math.abs((long) value) / (float) EFFECT_THRESHOLD);
    }

    public static String label(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }
}
