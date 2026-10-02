package fr.poubone.att2.client.data;

import fr.poubone.att2.client.compat.FlashbackCompat;
import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.input.KeybindManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;

/**
 * When the custom stats HUD is on but the map's attribute bossbar is off (or in detailed-value
 * mode), send the same Conscience {@code ScoreTrigger} that turns the point-total bar on.
 * The datapack then {@code $dialog show}s {@code consciousness.attribute_display}; that popup
 * is swallowed so HUD stats still update from the bossbar.
 */
public final class MapStatDisplayEnabler {
    public static final int ALL_RUN = Att2Triggers.STAT_DISPLAY_ALL_RUN;
    public static final int COUNT = Att2Triggers.STAT_DISPLAY_COUNT;
    public static final String ATTRIBUTE_DISPLAY_KEY = "consciousness.attribute_display";
    private static final int WARMUP_TICKS = 40;
    private static final int LISTEN_TICKS = 80;

    private static int warmup = WARMUP_TICKS;
    private static boolean requested;
    private static boolean listening;
    private static int listenTicks;

    private MapStatDisplayEnabler() {
    }

    public static boolean shouldRequest(boolean hudOn, boolean showStats, boolean hasPointTotals) {
        return hudOn && showStats && !hasPointTotals;
    }

    /**
     * Conscience trigger to send, or {@code -1} when the map bar already has point totals
     * (or the custom HUD does not need it).
     */
    public static int triggerToSend(boolean hudOn, boolean showStats, boolean hasPointTotals, boolean hasDetailedValues) {
        if (!shouldRequest(hudOn, showStats, hasPointTotals)) return -1;
        return hasDetailedValues ? Att2Triggers.STAT_DISPLAY_COUNT : Att2Triggers.STAT_DISPLAY_ALL_RUN;
    }

    public static int triggerToSend(boolean hudOn, boolean showStats, boolean hasPointTotals,
                                    boolean hasDetailedValues, boolean mapReady) {
        if (!mapReady) return -1;
        return triggerToSend(hudOn, showStats, hasPointTotals, hasDetailedValues);
    }

    public static void tick(Minecraft client) {
        if (FlashbackCompat.isInReplay()) {
            reset();
            return;
        }
        if (client.player == null || client.level == null) return;
        decayListenWindow();
        if (warmup > 0) {
            warmup--;
            return;
        }
        boolean hudOn = KeybindManager.showCustomHUD;
        boolean showStats = HUDConfig.get().showStats;
        boolean hasPoints = MapStatBar.sawPointTotals();
        boolean hasDetailed = MapStatBar.sawDetailedValues();
        int trigger = triggerToSend(hudOn, showStats, hasPoints, hasDetailed, MapReady.isReady());
        if (trigger < 0) {
            if (!hudOn || !showStats) {
                requested = false;
            }
            return;
        }
        if (requested) return;
        requested = true;
        onSentDisplayTrigger();
        Att2Triggers.send(trigger);
    }

    /** After 556 / 558, cancel the next {@code consciousness.attribute_display} dialog. */
    public static void onSentDisplayTrigger() {
        listening = true;
        listenTicks = LISTEN_TICKS;
    }

    public static boolean applyIfListening(Component title, Component body) {
        if (!listening) return false;
        if (!isAttributeDisplay(title, body)) return false;
        listening = false;
        listenTicks = 0;
        return true;
    }

    public static boolean onDialog(Holder<Dialog> holder) {
        if (!listening || holder == null) return false;
        Dialog dialog = holder.value();
        CommonDialogData common = commonOf(dialog);
        if (common == null) return false;
        return applyIfListening(common.title(), bodyOf(common));
    }

    static boolean isAttributeDisplay(Component title, Component body) {
        return DialogComponents.hasKeyOrChild(title, ATTRIBUTE_DISPLAY_KEY)
                || DialogComponents.hasKeyOrChild(body, ATTRIBUTE_DISPLAY_KEY);
    }

    public static void reset() {
        warmup = WARMUP_TICKS;
        requested = false;
        listening = false;
        listenTicks = 0;
    }

    private static void decayListenWindow() {
        if (listenTicks <= 0) return;
        listenTicks--;
        if (listenTicks == 0) listening = false;
    }

    private static CommonDialogData commonOf(Dialog dialog) {
        if (dialog instanceof MultiActionDialog multi) return multi.common();
        if (dialog instanceof NoticeDialog notice) return notice.common();
        return null;
    }

    private static Component bodyOf(CommonDialogData common) {
        Component body = Component.empty();
        for (DialogBody part : common.body()) {
            if (part instanceof PlainMessage message) {
                body = body.copy().append(message.contents());
            }
        }
        return body;
    }
}
