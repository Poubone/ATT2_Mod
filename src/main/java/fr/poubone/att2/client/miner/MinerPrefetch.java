package fr.poubone.att2.client.miner;

import java.util.Arrays;
import java.util.OptionalInt;

public final class MinerPrefetch {

    public static final int[] DEFAULT_TRIGGERS = {3489, 3488, 3535};
    public static final int DEFAULT_DELAY_TICKS = 2;

    private final int[] triggers;
    private final int delayTicks;
    private int nextIndex;
    private int ticksUntilNext;
    private boolean cancelled;
    private boolean done;

    public MinerPrefetch() {
        this(DEFAULT_TRIGGERS, DEFAULT_DELAY_TICKS);
    }

    public MinerPrefetch(int[] triggers, int delayTicks) {
        this.triggers = Arrays.copyOf(triggers, triggers.length);
        this.delayTicks = delayTicks;
        this.ticksUntilNext = 0;
    }

    public boolean isActive() {
        return !done && !cancelled;
    }

    public OptionalInt tick() {
        if (done || cancelled) {
            return OptionalInt.empty();
        }
        if (ticksUntilNext > 0) {
            ticksUntilNext--;
            return OptionalInt.empty();
        }
        int trigger = triggers[nextIndex++];
        if (nextIndex >= triggers.length) {
            done = true;
        } else {
            ticksUntilNext = delayTicks;
        }
        return OptionalInt.of(trigger);
    }

    public void cancel() {
        cancelled = true;
        done = true;
    }

    public boolean isDone() {
        return done;
    }
}
