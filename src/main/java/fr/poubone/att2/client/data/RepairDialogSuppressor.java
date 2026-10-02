package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;

/** Hides the map's quick-repair dialog only for repairs started from our radial menu. */
public final class RepairDialogSuppressor {
    private static final String TITLE_KEY = "consciousness.quick_repair.title";
    private static final long TIMEOUT_NANOS = 10_000_000_000L;
    private static final long BURST_TIMEOUT_NANOS = 2_000_000_000L;
    private static int queuedTrigger = -1;
    private static int remainingDialogs;
    private static long deadline;

    private RepairDialogSuppressor() {
    }

    public static void expect(int trigger) {
        queuedTrigger = trigger;
    }

    public static void onTriggerSent(int trigger) {
        if (trigger != queuedTrigger) return;
        queuedTrigger = -1;
        // trigger_all invokes every slot trigger, each of which shows a dialog, then shows one more.
        remainingDialogs = trigger == Att2Triggers.REPAIR_ALL ? 7 : 1;
        deadline = System.nanoTime() + TIMEOUT_NANOS;
    }

    public static boolean shouldSuppress(Dialog dialog) {
        if (remainingDialogs <= 0) return false;
        if (System.nanoTime() > deadline) {
            remainingDialogs = 0;
            return false;
        }
        for (DialogBody body : dialog.common().body()) {
            if (body instanceof PlainMessage message && hasTitle(message.contents())) {
                remainingDialogs--;
                if (remainingDialogs > 0) deadline = System.nanoTime() + BURST_TIMEOUT_NANOS;
                return true;
            }
        }
        return false;
    }

    public static void reset() {
        queuedTrigger = -1;
        remainingDialogs = 0;
        deadline = 0;
    }

    private static boolean hasTitle(Component component) {
        if (component.getContents() instanceof TranslatableContents translated) {
            if (TITLE_KEY.equals(translated.getKey())) return true;
            for (Object arg : translated.getArgs()) {
                if (arg instanceof Component nested && hasTitle(nested)) return true;
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (hasTitle(sibling)) return true;
        }
        return false;
    }
}
