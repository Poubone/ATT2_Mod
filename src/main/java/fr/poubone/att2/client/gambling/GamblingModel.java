package fr.poubone.att2.client.gambling;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.hud.HUDConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client-side mirror of Charles' two games (bets and "Treasure Grid" scratch cards).
 * <p>
 * The map drives both games with clickable {@code tellraw} lines and {@code /trigger ScoreTrigger set N}.
 * This model reads those exact lines, rebuilds their content for {@link CharlesScreen}, and the screen sends the
 * exact same triggers a chat click would. Nothing is computed locally: a line is only hidden from the chat once
 * it has been understood and is shown by the screen; anything unrecognised is left in the chat.
 */
public final class GamblingModel {
    private static final GamblingModel INSTANCE = new GamblingModel();

    /** Charles' gambling table (grid flips require the player within 15 blocks). */
    public static final Vec3 TABLE_POS = new Vec3(-4962.0, 97.0, -5803.0);
    public static final double FLIP_MAX_DISTANCE = 15.0;

    public static final int TRIGGER_BETS_MENU = 730;
    public static final int TRIGGER_BET_FIRST = 394;
    public static final int TRIGGER_BET_LAST = 400;
    public static final int TRIGGER_GRID_CONTINUE = 2844;
    public static final int TRIGGER_GRID_FIRST = 2845;
    public static final int TRIGGER_GRID_LAST = 2854;
    public static final int TRIGGER_GRID_RESET = 2855;
    public static final int TRIGGER_GRID_MENU = 2856;
    public static final int TRIGGER_CELL_FIRST = 2332;
    public static final int TRIGGER_CELL_LAST = 2843;

    private static final String KEY_BET_PRICE = "att2.shop.chronotons.buy";
    private static final String KEY_PRICE = "att2.chronotons.price";
    private static final String KEY_LOCK = "att2.lock";
    private static final String KEY_BET_SCORE = "att2.gambling.score";
    private static final String KEY_SELECT_TITLE = "matching_game.select.title";
    private static final String KEY_SELECT_LOCK = "matching_game.select.lock";
    private static final String KEY_CELL_LOCK = "matching_game.lock";
    private static final String KEY_CLICK_COUNT = "matching_game.show_click_count";
    private static final String KEY_CLICK_PRICE = "matching_game.click_price";
    private static final String KEY_BACK_MENU = "matching_game.back_menu";
    private static final String KEY_RESET = "matching_game.reset";
    private static final Identifier NOT_ENOUGH_SOUND = Identifier.withDefaultNamespace("noise4");

    private static final int MENU_WINDOW_TICKS = 40;
    private static final int FEEDBACK_WINDOW_TICKS = 40;

    public enum CellType {
        LOCK("lock", 32), MELEEWEAPON("meleeweapon", 32), RANGEWEAPON("rangeweapon", 32), ARMOR("armor", 32),
        SPELL("spell", 32), CURRENCY("currency", 32), FOOD("food", 32), POTION("potion", 32), RUNE("rune", 20);

        public final String id;
        public final Identifier texture;
        /** Size in pixels of the resource pack texture (all are square). */
        public final int textureSize;

        CellType(String id, int textureSize) {
            this.id = id;
            this.textureSize = textureSize;
            this.texture = Identifier.withDefaultNamespace("textures/item/custom/matching_game/" + id + ".png");
        }

        static CellType fromTranslationKey(String key) {
            if (KEY_CELL_LOCK.equals(key)) return LOCK;
            for (CellType type : values()) {
                if (type != LOCK && key.equals("matching_game." + type.id + ".unlock")) return type;
            }
            return null;
        }

        static CellType fromId(String id) {
            for (CellType type : values()) {
                if (type.id.equals(id)) return type;
            }
            return null;
        }
    }

    /** A bet offered by Charles: {@code trigger} is -1 for tiers still locked by the story. */
    public record BetEntry(int index, Component label, int price, int trigger) {
        public boolean locked() {
            return trigger < 0;
        }
    }

    /** A grid size offered in the scratch card menu (or the "continue" entry). */
    public record GridChoice(Component label, int price, int trigger, boolean locked, boolean isContinue) {
    }

    /** One card of the grid; {@code trigger} is the flip trigger of a locked card, -1 once revealed. */
    public record GridCell(CellType type, int trigger, Component original) {
        public boolean isLocked() {
            return type == CellType.LOCK;
        }
    }

    public record CountEntry(CellType type, int count, TextColor color) {
    }

    private final List<BetEntry> bets = new ArrayList<>();
    private final List<GridChoice> gridChoices = new ArrayList<>();
    private final List<List<GridCell>> grid = new ArrayList<>();
    private final List<CountEntry> counts = new ArrayList<>();
    private int nextFlipPrice = -1;
    private boolean nextFlipAffordable = true;
    private int lastBetScore = -1;
    private long lastBetScoreTime = 0;
    private int betsMenuTicks = 0;
    private int feedbackTicks = 0;
    private boolean notEnoughChronotons = false;
    private boolean betPending = false;
    private int betPendingTicks = 0;
    private boolean waitingForBets = false;
    private boolean waitingForGridMenu = false;
    private boolean waitingForGrid = false;
    private int waitTicks = 0;

    private GamblingModel() {
    }

    public static GamblingModel get() {
        return INSTANCE;
    }

    public void reset() {
        bets.clear();
        gridChoices.clear();
        grid.clear();
        counts.clear();
        nextFlipPrice = -1;
        nextFlipAffordable = true;
        lastBetScore = -1;
        lastBetScoreTime = 0;
        betsMenuTicks = 0;
        feedbackTicks = 0;
        notEnoughChronotons = false;
        betPending = false;
        betPendingTicks = 0;
        waitingForBets = false;
        waitingForGridMenu = false;
        waitingForGrid = false;
        waitTicks = 0;
    }

    // ---------------------------------------------------------------- requests (same triggers as the chat links)

    public void requestBets() {
        waitingForBets = true;
        waitTicks = 60;
        Att2Triggers.send(TRIGGER_BETS_MENU);
    }

    public void placeBet(BetEntry bet) {
        if (bet.locked()) return;
        betPending = true;
        betPendingTicks = 200;
        armFeedback();
        Att2Triggers.send(bet.trigger());
    }

    public void requestGridMenu() {
        hideBoard();
        waitingForGridMenu = true;
        waitTicks = 60;
        Att2Triggers.send(TRIGGER_GRID_MENU);
    }

    public void chooseGrid(GridChoice choice) {
        if (choice.locked()) return;
        waitingForGrid = true;
        waitTicks = 60;
        armFeedback();
        Att2Triggers.send(choice.trigger());
    }

    public void flip(GridCell cell) {
        if (!cell.isLocked() || cell.trigger() < 0 || !isNearTable()) return;
        armFeedback();
        Att2Triggers.send(cell.trigger());
    }

    public void resetGrid() {
        waitingForGrid = true;
        waitTicks = 60;
        armFeedback();
        Att2Triggers.send(TRIGGER_GRID_RESET);
    }

    private void armFeedback() {
        notEnoughChronotons = false;
        feedbackTicks = FEEDBACK_WINDOW_TICKS;
    }

    public void tick(Minecraft client) {
        if (betsMenuTicks > 0) betsMenuTicks--;
        if (feedbackTicks > 0) feedbackTicks--;
        if (waitTicks > 0 && --waitTicks == 0) {
            waitingForBets = false;
            waitingForGridMenu = false;
            waitingForGrid = false;
        }
        if (betPending && --betPendingTicks <= 0) betPending = false;
    }

    // ---------------------------------------------------------------- incoming data

    /** Bet result title ({@code att2.gambling.score}). Never hidden: it is a title, not a chat line. */
    public void onTitle(Component title) {
        TranslatableContents score = findTranslatable(title, KEY_BET_SCORE);
        if (score == null || score.getArgs().length == 0) return;
        parseInt(argText(score.getArgs()[0])).ifPresent(value -> {
            lastBetScore = value;
            lastBetScoreTime = System.currentTimeMillis();
            betPending = false;
        });
    }

    /** {@code noise4} right after one of our requests means "not enough chronotons" (the map prints no text). */
    public void onSound(Identifier sound, SoundSource source) {
        if (feedbackTicks > 0 && NOT_ENOUGH_SOUND.equals(sound) && source == SoundSource.PLAYERS) {
            notEnoughChronotons = true;
            betPending = false;
            waitingForGrid = false;
        }
    }

    /**
     * Inspects a system chat line. Returns true when the line belongs to Charles' games and has been rendered in
     * {@link CharlesScreen} (the chat line is then dropped); false leaves the message untouched.
     */
    public boolean onSystemMessage(Component message) {
        return onSystemMessage(message, fr.poubone.att2.client.hud.HUDConfig.get().charlesAutoOpen);
    }

    /** Disabled menus leave both the map message and their captured state untouched. */
    public boolean onSystemMessage(Component message, boolean enabled) {
        if (!enabled) return false;
        try {
            return handleMessage(message);
        } catch (RuntimeException e) {
            // Any surprise in the map's format: keep the vanilla chat behaviour.
            return false;
        }
    }

    private boolean handleMessage(Component message) {
        List<Integer> triggers = collectTriggers(message);

        // Charles' own speech: offers the bets (730) or the scratch cards (2856)
        if (triggers.contains(TRIGGER_BETS_MENU) && !containsKey(message, KEY_BACK_MENU)) {
            return openFromCharles(CharlesScreen.Tab.BETS);
        }
        if (triggers.contains(TRIGGER_GRID_MENU) && !containsKey(message, KEY_BACK_MENU)
                && !containsKey(message, KEY_CELL_LOCK) && !containsAnyCell(message)) {
            return openFromCharles(CharlesScreen.Tab.GRIDS);
        }

        // Bet lines
        for (int trigger : triggers) {
            if (trigger >= TRIGGER_BET_FIRST && trigger <= TRIGGER_BET_LAST) {
                return handleBetLine(message, trigger);
            }
        }
        if (betsMenuTicks > 0 && containsKey(message, KEY_LOCK) && !containsKey(message, KEY_SELECT_LOCK)
                && message.getString().contains("<???>")) {
            bets.add(new BetEntry(bets.size() + 1, Component.literal("<???>"), -1, -1));
            betsMenuTicks = MENU_WINDOW_TICKS;
            return isScreenShowing();
        }

        // Scratch cards: menu
        if (containsKey(message, KEY_SELECT_TITLE)) {
            hideBoard();
            gridChoices.clear();
            waitingForGridMenu = false;
            return isScreenShowing();
        }
        for (int trigger : triggers) {
            if (trigger == TRIGGER_GRID_CONTINUE || (trigger >= TRIGGER_GRID_FIRST && trigger <= TRIGGER_GRID_LAST)) {
                return handleGridChoiceLine(message, trigger);
            }
        }
        if (containsKey(message, KEY_SELECT_LOCK)) {
            gridChoices.add(new GridChoice(Component.translatable(KEY_SELECT_LOCK), -1, -1, true, false));
            return isScreenShowing();
        }

        // Scratch cards: the grid itself, its counters, the next price, the action line
        if (containsKey(message, KEY_CELL_LOCK) || containsAnyCell(message)) {
            return handleGridLine(message);
        }
        if (containsKey(message, KEY_CLICK_COUNT)) {
            return handleCountLine(message);
        }
        TranslatableContents price = findTranslatable(message, KEY_CLICK_PRICE);
        if (price != null && price.getArgs().length > 0) {
            Component arg = argComponent(price.getArgs()[0]);
            parseInt(argText(price.getArgs()[0])).ifPresent(value -> nextFlipPrice = value);
            TextColor color = arg == null ? null : findColor(arg);
            nextFlipAffordable = color == null || color.getValue() != 0xFF5555;
            return isScreenShowing();
        }
        if (containsKey(message, KEY_BACK_MENU) && containsKey(message, KEY_RESET)) {
            return isScreenShowing();
        }
        return false;
    }

    /** Charles' speech stays in the chat (it is NPC dialogue); the counter simply opens on the matching tab. */
    private boolean openFromCharles(CharlesScreen.Tab tab) {
        maybeOpenCharles(tab);
        if (tab == CharlesScreen.Tab.BETS) {
            requestBets();
        } else {
            requestGridMenu();
        }
        return false;
    }

    private void maybeOpenCharles(CharlesScreen.Tab tab) {
        if (!HUDConfig.get().charlesAutoOpen) return;
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null || client.screen instanceof CharlesScreen
                || client.screen instanceof ChatScreen) {
            CharlesScreen.open(tab);
        }
    }

    private boolean handleBetLine(Component message, int trigger) {
        int index = trigger - TRIGGER_BET_FIRST + 1;
        if (betsMenuTicks == 0 || index == 1) {
            bets.clear();
        }
        Component label = findLabel(message, "<");
        TranslatableContents priceContents = findTranslatable(message, KEY_BET_PRICE);
        int price = priceContents == null || priceContents.getArgs().length == 0 ? -1
                : parseInt(argText(priceContents.getArgs()[0])).orElse(-1);
        bets.removeIf(b -> b.index() == index);
        bets.add(new BetEntry(index, label == null ? Component.literal("<" + index + ">") : label, price, trigger));
        bets.sort((a, b) -> Integer.compare(a.index(), b.index()));
        betsMenuTicks = MENU_WINDOW_TICKS;
        waitingForBets = false;
        maybeOpenCharles(CharlesScreen.Tab.BETS);
        return isScreenShowing();
    }

    private boolean handleGridChoiceLine(Component message, int trigger) {
        Component label = findTranslatableComponent(message, "matching_game.select.");
        TranslatableContents priceContents = findTranslatable(message, KEY_PRICE);
        int price = priceContents == null || priceContents.getArgs().length == 0 ? -1
                : parseInt(argText(priceContents.getArgs()[0])).orElse(-1);
        boolean isContinue = trigger == TRIGGER_GRID_CONTINUE;
        hideBoard();
        gridChoices.removeIf(c -> c.trigger() == trigger);
        gridChoices.add(new GridChoice(label == null ? Component.literal("?") : label, price, trigger, false, isContinue));
        waitingForGridMenu = false;
        maybeOpenCharles(CharlesScreen.Tab.GRIDS);
        return isScreenShowing();
    }

    private boolean handleGridLine(Component message) {
        List<List<GridCell>> rows = new ArrayList<>();
        List<GridCell> row = new ArrayList<>();
        for (Component part : flatten(message)) {
            if (part.getContents() instanceof PlainTextContents plain && plain.text().contains("\n")) {
                if (!row.isEmpty()) rows.add(row);
                row = new ArrayList<>();
                continue;
            }
            if (!(part.getContents() instanceof TranslatableContents translatable)) continue;
            CellType type = CellType.fromTranslationKey(translatable.getKey());
            if (type == null) continue;
            int trigger = -1;
            if (part.getStyle().getClickEvent() instanceof ClickEvent.RunCommand run) {
                trigger = Att2Triggers.parseTriggerCommand(run.command());
            }
            row.add(new GridCell(type, trigger, part));
        }
        if (!row.isEmpty()) rows.add(row);
        if (rows.isEmpty()) return false;

        grid.clear();
        grid.addAll(rows);
        gridChoices.clear();
        waitingForGrid = false;
        return isScreenShowing();
    }

    private boolean handleCountLine(Component message) {
        List<CountEntry> parsed = new ArrayList<>();
        CellType pendingType = null;
        for (Component part : flatten(message)) {
            if (part.getContents() instanceof ObjectContents object && object.contents() instanceof AtlasSprite sprite) {
                String path = sprite.sprite().getPath();
                pendingType = CellType.fromId(path.substring(path.lastIndexOf('/') + 1));
            } else if (part.getContents() instanceof TranslatableContents translatable
                    && translatable.getKey().equals(KEY_CLICK_COUNT) && translatable.getArgs().length > 0) {
                Component arg = argComponent(translatable.getArgs()[0]);
                int count = parseInt(argText(translatable.getArgs()[0])).orElse(0);
                if (pendingType != null) {
                    parsed.add(new CountEntry(pendingType, count, arg == null ? null : findColor(arg)));
                }
                pendingType = null;
            }
        }
        counts.clear();
        counts.addAll(parsed);
        return isScreenShowing();
    }

    private static boolean isScreenShowing() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.screen instanceof CharlesScreen;
    }

    // ---------------------------------------------------------------- queries

    public List<BetEntry> getBets() {
        return Collections.unmodifiableList(bets);
    }

    public List<GridChoice> getGridChoices() {
        return Collections.unmodifiableList(gridChoices);
    }

    public List<List<GridCell>> getGrid() {
        return Collections.unmodifiableList(grid);
    }

    public boolean hasGrid() {
        return !grid.isEmpty();
    }

    /** Leaves the scratch-card board so the size list can be shown again. */
    void hideBoard() {
        grid.clear();
        counts.clear();
        nextFlipPrice = -1;
        nextFlipAffordable = true;
    }

    public boolean isGridComplete() {
        return hasGrid() && grid.stream().flatMap(List::stream).noneMatch(GridCell::isLocked);
    }

    public List<CountEntry> getCounts() {
        return Collections.unmodifiableList(counts);
    }

    public int getNextFlipPrice() {
        return nextFlipPrice;
    }

    public boolean isNextFlipAffordable() {
        return nextFlipAffordable;
    }

    public int getLastBetScore() {
        return lastBetScore;
    }

    public long getLastBetScoreTime() {
        return lastBetScoreTime;
    }

    public boolean isBetPending() {
        return betPending;
    }

    public boolean isWaiting() {
        return waitingForBets || waitingForGridMenu || waitingForGrid;
    }

    public boolean isNotEnoughChronotons() {
        return notEnoughChronotons;
    }

    public void clearNotEnoughChronotons() {
        notEnoughChronotons = false;
    }

    public double distanceToTable() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? Double.MAX_VALUE : Math.sqrt(player.position().distanceToSqr(TABLE_POS));
    }

    public boolean isNearTable() {
        return distanceToTable() <= FLIP_MAX_DISTANCE;
    }

    // ---------------------------------------------------------------- component helpers

    private static List<Integer> collectTriggers(Component message) {
        List<Integer> triggers = new ArrayList<>();
        visit(message, c -> {
            if (c.getStyle().getClickEvent() instanceof ClickEvent.RunCommand run) {
                int trigger = Att2Triggers.parseTriggerCommand(run.command());
                if (trigger >= 0) triggers.add(trigger);
            }
        });
        return triggers;
    }

    private static boolean containsAnyCell(Component message) {
        boolean[] found = {false};
        visit(message, c -> {
            if (!found[0] && c.getContents() instanceof TranslatableContents translatable
                    && CellType.fromTranslationKey(translatable.getKey()) != null) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static boolean containsKey(Component message, String key) {
        return findTranslatable(message, key) != null;
    }

    private static TranslatableContents findTranslatable(Component component, String key) {
        TranslatableContents[] result = {null};
        visit(component, c -> {
            if (result[0] == null && c.getContents() instanceof TranslatableContents translatable
                    && translatable.getKey().equals(key)) {
                result[0] = translatable;
            }
        });
        // Hover texts are not part of the tree: look at them too (used for the "locked" markers).
        if (result[0] == null) {
            visit(component, c -> {
                if (result[0] == null && c.getStyle().getHoverEvent() instanceof HoverEvent.ShowText show) {
                    TranslatableContents inner = findTranslatable(show.value(), key);
                    if (inner != null) result[0] = inner;
                }
            });
        }
        return result[0];
    }

    private static Component findTranslatableComponent(Component component, String keyPrefix) {
        Component[] result = {null};
        visit(component, c -> {
            if (result[0] == null && c.getContents() instanceof TranslatableContents translatable
                    && translatable.getKey().startsWith(keyPrefix)) {
                result[0] = Component.translatable(translatable.getKey()).withStyle(c.getStyle().withClickEvent(null).withHoverEvent(null));
            }
        });
        return result[0];
    }

    /** First plain text part starting with the given prefix (e.g. {@code <Mise 1>}), without its siblings. */
    private static Component findLabel(Component component, String prefix) {
        Component[] result = {null};
        visit(component, c -> {
            if (result[0] == null && c.getContents() instanceof PlainTextContents plain && plain.text().startsWith(prefix)) {
                result[0] = Component.literal(plain.text()).withStyle(c.getStyle().withClickEvent(null).withHoverEvent(null));
            }
        });
        return result[0];
    }

    private static TextColor findColor(Component component) {
        TextColor[] result = {null};
        visit(component, c -> {
            if (result[0] == null && c.getStyle().getColor() != null) result[0] = c.getStyle().getColor();
        });
        return result[0];
    }

    /** Depth-first list of every component of the tree, in reading order. */
    private static List<Component> flatten(Component component) {
        List<Component> list = new ArrayList<>();
        visit(component, list::add);
        return list;
    }

    private static void visit(Component component, Consumer<Component> visitor) {
        if (component == null) return;
        visitor.accept(component);
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component argComponent) visit(argComponent, visitor);
            }
        }
        for (Component sibling : component.getSiblings()) {
            visit(sibling, visitor);
        }
    }

    private static Component argComponent(Object arg) {
        return arg instanceof Component component ? component : null;
    }

    private static String argText(Object arg) {
        return arg instanceof Component component ? component.getString() : String.valueOf(arg);
    }

    private static java.util.OptionalInt parseInt(String text) {
        try {
            return java.util.OptionalInt.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return java.util.OptionalInt.empty();
        }
    }
}
