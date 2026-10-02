package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.CurrencyModel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.ScoreContents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class RuneCodexModel {
    private static final RuneCodexModel INSTANCE = new RuneCodexModel();
    private static final int POWDER_WINDOW_TICKS = 40;
    /** Workshop {@code runepowder_display} lands before the six menu ⚙ lines. */
    private static final int POWDER_DEFER_TICKS = 8;
    private static final int SERIES_GAP_TICKS = 2;

    private int powderWindow;
    private boolean openedThisBurst;
    private Component pendingPowder;
    private int pendingPowderTicks;
    private int seriesTrigger;
    private int seriesRemaining;
    private int seriesCooldown;
    private BooleanSupplier seriesCanContinue;
    private final RuneBundleMessage bundleMessage = new RuneBundleMessage();

    private RuneCodexModel() {
    }

    public static RuneCodexModel get() {
        return INSTANCE;
    }

    public void reset() {
        abortCraftSeries();
        powderWindow = 0;
        openedThisBurst = false;
        pendingPowder = null;
        pendingPowderTicks = 0;
        bundleMessage.reset();
        if (Minecraft.getInstance().screen instanceof RuneCodexScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    public void tick() {
        if (pendingPowderTicks > 0) {
            pendingPowderTicks--;
            if (pendingPowderTicks == 0) {
                flushPendingPowder();
            }
        }
        if (powderWindow > 0) powderWindow--;
        if (powderWindow == 0) openedThisBurst = false;
        if (seriesRemaining > 0) {
            if (seriesCooldown > 0) {
                seriesCooldown--;
            } else {
                if (seriesCanContinue != null && !seriesCanContinue.getAsBoolean()) {
                    abortCraftSeries();
                    return;
                }
                Att2Triggers.sendAlways(seriesTrigger);
                seriesRemaining--;
                seriesCooldown = SERIES_GAP_TICKS;
            }
        }
    }

    public void onScreenOpened() {
        CurrencyModel.request();
        bundleMessage.reset();
        Att2Triggers.send(Att2Triggers.RUNE_BUNDLE_INFO);
    }

    public void onScreenClosed() {
        abortCraftSeries();
        CurrencyModel.close();
    }

    public void craft(int trigger) {
        craft(trigger, 1, false);
    }

    public void craft(int trigger, int qty, boolean wordMode) {
        craft(trigger, qty, wordMode, null);
    }

    public void craft(int trigger, int qty, boolean wordMode, BooleanSupplier canContinue) {
        if (seriesRemaining > 0) {
            return;
        }
        int sends = CraftSeriesLogic.triggerSends(qty, wordMode);
        if (sends < 1) return;
        seriesTrigger = trigger;
        seriesRemaining = sends;
        seriesCooldown = 0;
        seriesCanContinue = canContinue;
        Att2Triggers.sendAlways(seriesTrigger);
        seriesRemaining--;
        seriesCooldown = SERIES_GAP_TICKS;
    }

    public void abortCraftSeries() {
        seriesRemaining = 0;
        seriesCooldown = 0;
        seriesTrigger = 0;
        seriesCanContinue = null;
    }

    public boolean onSystemMessage(Component message) {
        return onSystemMessage(message, fr.poubone.att2.client.hud.HUDConfig.get().runeMenuEnabled);
    }

    /** Disabled menus leave both the map message and their captured state untouched. */
    public boolean onSystemMessage(Component message, boolean enabled) {
        if (!enabled) return false;
        try {
            return handle(message);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean handle(Component message) {
        if (bundleMessage.accept(message)) return isScreenShowing();
        List<Integer> triggers = collectTriggers(message);
        for (int trigger : triggers) {
            if (RuneCatalog.isMenuTrigger(trigger)) {
                dropPendingPowder();
                powderWindow = POWDER_WINDOW_TICKS;
                if (!openedThisBurst) {
                    openedThisBurst = true;
                    if (!isScreenShowing()) {
                        RuneCodexScreen.open(RuneCatalog.menuTab(trigger));
                    }
                }
                return true;
            }
        }
        if (isScreenShowing()) {
            for (int trigger : triggers) {
                if (isRecipeDumpTrigger(trigger)) return true;
            }
        }
        if (triggers.isEmpty() && isPowderLine(message)) {
            pendingPowder = message;
            pendingPowderTicks = POWDER_DEFER_TICKS;
            return true;
        }
        return false;
    }

    private void dropPendingPowder() {
        pendingPowder = null;
        pendingPowderTicks = 0;
    }

    private void flushPendingPowder() {
        Component line = pendingPowder;
        pendingPowder = null;
        pendingPowderTicks = 0;
        if (line == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.gui != null) {
            client.gui.getChat().addMessage(line);
        }
    }

    private static boolean isRecipeDumpTrigger(int trigger) {
        return (trigger >= 343 && trigger <= 365)
                || (trigger >= 366 && trigger <= 392);
    }

    private static boolean isPowderLine(Component message) {
        return visitScore(message);
    }

    private static boolean visitScore(Component component) {
        if (component.getContents() instanceof ScoreContents score
                && "RUNE_POWDER".equals(score.objective())) {
            return true;
        }
        for (Component sibling : component.getSiblings()) {
            if (visitScore(sibling)) return true;
        }
        return false;
    }

    private static List<Integer> collectTriggers(Component message) {
        List<Integer> triggers = new ArrayList<>();
        visit(message, component -> {
            if (component.getStyle().getClickEvent() instanceof ClickEvent.RunCommand run) {
                int trigger = Att2Triggers.parseTriggerCommand(run.command());
                if (trigger >= 0) triggers.add(trigger);
            }
        });
        return triggers;
    }

    private static void visit(Component component, java.util.function.Consumer<Component> visitor) {
        visitor.accept(component);
        for (Component sibling : component.getSiblings()) visit(sibling, visitor);
    }

    private static boolean isScreenShowing() {
        return Minecraft.getInstance().screen instanceof RuneCodexScreen;
    }
}
