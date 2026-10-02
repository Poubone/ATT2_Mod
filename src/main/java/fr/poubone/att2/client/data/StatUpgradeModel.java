package fr.poubone.att2.client.data;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Fills the stat-upgrade screen from the Conscience attribute dialog (trigger 2313).
 * No {@code /scoreboard} — the datapack already bakes skill points and costs into the dialog.
 */
public final class StatUpgradeModel {
    private static boolean listening;
    private static boolean received;
    private static int skillPoints;
    private static final Map<String, Integer> costs = new HashMap<>();
    private static final Map<String, Integer> bases = new HashMap<>();
    private static final Map<String, Map<String, Integer>> sources = new HashMap<>();

    private StatUpgradeModel() {
    }

    public static void open() {
        listening = true;
        Att2Triggers.send(Att2Triggers.CONSCIOUSNESS_ATTRIBUTE);
    }

    public static void close() {
        listening = false;
    }

    public static void reset() {
        listening = false;
        received = false;
        skillPoints = 0;
        costs.clear();
        bases.clear();
        sources.clear();
    }

    public static boolean isListening() {
        return listening;
    }

    public static boolean hasData() {
        return received;
    }

    public static int skillPoints() {
        return skillPoints;
    }

    public static OptionalInt upgradeCost(String statKey) {
        if (!received || statKey == null || !costs.containsKey(statKey)) return OptionalInt.empty();
        return OptionalInt.of(costs.get(statKey));
    }

    public static OptionalInt base(String statKey) {
        if (!received || statKey == null || !bases.containsKey(statKey)) return OptionalInt.empty();
        return OptionalInt.of(bases.get(statKey));
    }

    /** Present source lines for one stat ({@code base}, {@code equipment}, {@code potion}, …). */
    public static Map<String, Integer> sources(String statKey) {
        if (!received || statKey == null) return Map.of();
        return sources.getOrDefault(statKey, Map.of());
    }

    /**
     * Called for every dialog the server shows. Returns true when this was the attribute dialog
     * we asked for (or a refresh after an upgrade while the screen is still open).
     */
    public static boolean onDialog(Holder<Dialog> holder) {
        if (!listening || holder == null) return false;
        Dialog dialog = holder.value();
        if (!(dialog instanceof MultiActionDialog multi)) return false;

        List<AttributeDialogParser.Action> actions = new ArrayList<>();
        Component actionTooltips = Component.empty();
        for (ActionButton button : multi.actions()) {
            Component label = button.button().label();
            Component tooltip = button.button().tooltip().orElse(null);
            if (tooltip != null && DialogComponents.hasKey(label, AttributeDialogParser.POINT_KEY)) {
                actionTooltips = actionTooltips.copy().append(tooltip);
            }
            int trigger = triggerOf(button);
            if (trigger < 0) continue;
            actions.add(new AttributeDialogParser.Action(label, trigger));
        }

        Component body = Component.empty();
        for (DialogBody part : multi.common().body()) {
            if (part instanceof PlainMessage message) {
                body = body.copy().append(message.contents());
            }
        }
        return applyIfListening(multi.common().title(), actions, body, actionTooltips);
    }

    /**
     * Applies skill points and costs from a Conscience attribute dialog, but only while the
     * upgrade screen asked for it — so the vanilla Conscience book is left alone.
     */
    public static boolean applyIfListening(Component title, List<AttributeDialogParser.Action> actions) {
        return applyIfListening(title, actions, Component.empty(), Component.empty());
    }

    public static boolean applyIfListening(
            Component title, List<AttributeDialogParser.Action> actions, Component body) {
        return applyIfListening(title, actions, body, Component.empty());
    }

    public static boolean applyIfListening(
            Component title,
            List<AttributeDialogParser.Action> actions,
            Component body,
            Component actionTooltips) {
        if (!listening) return false;
        var snap = AttributeDialogParser.parse(title, actions);
        if (snap.isEmpty()) return false;
        skillPoints = snap.get().skillPoints();
        costs.clear();
        costs.putAll(snap.get().costs());
        bases.clear();
        Map<String, Integer> parsedBases = AttributeDialogParser.parseBases(actionTooltips);
        if (parsedBases.isEmpty() && body != null && !body.getString().isEmpty()) {
            parsedBases = AttributeDialogParser.parseBases(body);
        }
        bases.putAll(parsedBases);
        sources.clear();
        sources.putAll(AttributeDialogParser.parseSources(actionTooltips));
        received = true;
        return true;
    }

    private static int triggerOf(ActionButton button) {
        if (button.action().isPresent() && button.action().get() instanceof StaticAction staticAction
                && staticAction.value() instanceof ClickEvent.RunCommand runCommand) {
            return Att2Triggers.parseTriggerCommand(runCommand.command());
        }
        return -1;
    }
}
