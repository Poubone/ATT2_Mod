package fr.poubone.att2.client.input;

import fr.poubone.att2.client.compat.FlashbackCompat;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Plays short sequences of vanilla inputs (forced sneak, held right click, swap-hands click)
 * so the map's key combos can be bound to a single key. Only one sequence runs at a time;
 * anything that opens a screen or removes the player aborts it and releases every key.
 */
public final class InputSequencer {
    public enum StepType { FORCE_SNEAK, HOLD_USE, CLICK_SWAP, WAIT }

    public record Step(StepType type, int ticks) {
        public static Step forceSneak(int ticks) { return new Step(StepType.FORCE_SNEAK, ticks); }
        public static Step holdUse(int ticks) { return new Step(StepType.HOLD_USE, ticks); }
        public static Step clickSwap() { return new Step(StepType.CLICK_SWAP, 1); }
        public static Step wait(int ticks) { return new Step(StepType.WAIT, ticks); }
    }

    private static final ArrayDeque<Step> QUEUE = new ArrayDeque<>();
    private static Step current;
    private static int remainingTicks;
    private static int sneakTicks;
    private static boolean holdingUse;

    private InputSequencer() {
    }

    public static boolean isBusy() {
        return current != null || !QUEUE.isEmpty();
    }

    /** True while the KeyboardInput mixin must force the sneak flag. */
    public static boolean isSneakForced() {
        return sneakTicks > 0;
    }

    public static void run(List<Step> steps) {
        if (FlashbackCompat.isInReplay()) return;
        if (!isBusy()) {
            QUEUE.addAll(steps);
        }
    }

    public static void tick(Minecraft client) {
        if (FlashbackCompat.isInReplay()) {
            abort(client);
            return;
        }
        if (client.player == null || client.screen != null) {
            abort(client);
            return;
        }
        if (sneakTicks > 0) sneakTicks--;

        if (current == null) {
            current = QUEUE.pollFirst();
            if (current == null) return;
            remainingTicks = Math.max(1, current.ticks());
            start(client, current);
        }
        remainingTicks--;
        if (remainingTicks <= 0) {
            end(client, current);
            current = null;
        }
    }

    private static void start(Minecraft client, Step step) {
        switch (step.type()) {
            case FORCE_SNEAK -> {
                sneakTicks = step.ticks();
                remainingTicks = 1; // non bloquant : le décompte du sneak court en parallèle
            }
            case HOLD_USE -> {
                client.options.keyUse.setDown(true);
                holdingUse = true;
            }
            case CLICK_SWAP -> KeyMapping.click(KeyBindingHelper.getBoundKeyOf(client.options.keySwapOffhand));
            case WAIT -> {
            }
        }
    }

    private static void end(Minecraft client, Step step) {
        if (step.type() == StepType.HOLD_USE) {
            releaseUse(client);
        }
    }

    public static void abort(Minecraft client) {
        QUEUE.clear();
        current = null;
        remainingTicks = 0;
        sneakTicks = 0;
        releaseUse(client);
    }

    private static void releaseUse(Minecraft client) {
        if (holdingUse) {
            holdingUse = false;
            client.options.keyUse.setDown(false);
        }
    }
}
