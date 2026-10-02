package fr.poubone.att2.client.quest;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.NoticeDialog;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-side view of the player's quests.
 * <p>
 * We do not compute anything ourselves: we ask the map for
 * its own quest UI and read the answer. The side quest list comes from the 1.21 dialog the datapack builds in
 * {@code gameplay/consciousness/sidequest}, and quest descriptions are the chat lines printed by
 * {@code gameplay/quest/mainquest/go} / {@code sidequest/sqN/display_progress}.
 */
public final class QuestModel {
    private static final QuestModel INSTANCE = new QuestModel();

    private static final String SIDEQUEST_TITLE_KEY = "consciousness.sidequest.title";
    private static final Pattern SIDEQUEST_NAME_KEY = Pattern.compile("consciousness\\.sidequest\\.(\\d+)");
    private static final String MAINQUEST_PROGRESS_KEY = "att2.mainquest.progress";
    private static final String DAILYQUEST_LIST_TITLE_KEY = "consciousness.dailyquest.title";
    private static final Pattern DAILYQUEST_CITY_KEY = Pattern.compile("consciousness\\.dailyquest\\.list\\.([a-z_]+)");
    private static final Pattern DAILYQUEST_CITY_TITLE_KEY = Pattern.compile("consciousness\\.dailyquest\\.([a-z_]+)\\.title");
    private static final Pattern DAILYQUEST_NAME_KEY = Pattern.compile("att2\\.dailyquest\\.([a-z_]+)\\.(\\d+)\\.name");
    private static final String DAILYQUEST_REMAINING_KEY = "att2.dailyquest.remaining_time";
    private static final int CITY_REQUEST_INTERVAL_TICKS = 8;
    private static final int MAIN_QUEST_MAX_STEP = 300;
    private static final int DIALOG_TIMEOUT_TICKS = 80;
    private static final int CAPTURE_TICKS = 30;

    private final List<QuestInfo> sideQuests = new ArrayList<>();
    /** City id -> daily quests of that city, in the order the cities were listed by the map. */
    private final Map<String, List<QuestInfo>> dailyQuests = new java.util.LinkedHashMap<>();
    private final Map<String, List<Component>> descriptions = new HashMap<>();
    private int sideCompleted = 0;
    private int sideTotal = 60;
    private int mainStep = -1;
    private long lastSideQuestUpdate = 0;

    private int dialogWaitTicks = 0;
    private int dailyDialogWaitTicks = 0;
    private final java.util.ArrayDeque<Integer> pendingCityTriggers = new java.util.ArrayDeque<>();
    private int cityRequestCooldown = 0;
    private boolean hasDailyData = false;
    private int captureTicks = 0;
    private String captureKey = null;
    private final List<Component> captureBuffer = new ArrayList<>();
    private String selectedKey = null;

    private QuestModel() {
    }

    public static QuestModel get() {
        return INSTANCE;
    }

    public void reset() {
        sideQuests.clear();
        dailyQuests.clear();
        descriptions.clear();
        sideCompleted = 0;
        mainStep = -1;
        lastSideQuestUpdate = 0;
        dialogWaitTicks = 0;
        dailyDialogWaitTicks = 0;
        pendingCityTriggers.clear();
        cityRequestCooldown = 0;
        hasDailyData = false;
        captureTicks = 0;
        captureKey = null;
        captureBuffer.clear();
        selectedKey = null;
    }

    // ---------------------------------------------------------------- requests

    /** Asks the datapack for its side quest dialog; the answer is intercepted in {@link #onDialog}. */
    public void requestSideQuests() {
        dialogWaitTicks = DIALOG_TIMEOUT_TICKS;
        Att2Triggers.send(Att2Triggers.CONSCIOUSNESS_SIDEQUEST_LIST);
    }

    /**
     * Asks the datapack for its daily quest menu. The answer is a list of cities; each city is then requested in
     * turn (see {@link #tick}) and its dialog parsed in {@link #onDialog}.
     */
    public void requestDailyQuests() {
        dailyDialogWaitTicks = DIALOG_TIMEOUT_TICKS;
        pendingCityTriggers.clear();
        Att2Triggers.send(Att2Triggers.CONSCIOUSNESS_DAILYQUEST_LIST);
    }

    /** Asks the datapack to print the current objective of a quest and captures the chat lines. */
    public void requestDetails(QuestInfo quest) {
        if (quest.isMain()) {
            beginCapture(quest.key());
            Att2Triggers.send(Att2Triggers.MAIN_QUEST_GO);
        } else if (quest.hasDetails()) {
            beginCapture(quest.key());
            Att2Triggers.send(quest.progressTrigger());
        }
    }

    private void beginCapture(String key) {
        captureKey = key;
        captureTicks = CAPTURE_TICKS;
        captureBuffer.clear();
    }

    public void tick(Minecraft client) {
        if (dialogWaitTicks > 0) dialogWaitTicks--;
        if (dailyDialogWaitTicks > 0) dailyDialogWaitTicks--;
        if (cityRequestCooldown > 0) cityRequestCooldown--;
        if (cityRequestCooldown == 0 && !pendingCityTriggers.isEmpty() && dailyDialogWaitTicks == 0) {
            dailyDialogWaitTicks = DIALOG_TIMEOUT_TICKS;
            cityRequestCooldown = CITY_REQUEST_INTERVAL_TICKS;
            Att2Triggers.send(pendingCityTriggers.pollFirst());
        }
        if (captureTicks > 0) {
            captureTicks--;
            if (captureTicks == 0) finishCapture();
        }
    }

    private void finishCapture() {
        if (captureKey != null && !captureBuffer.isEmpty()) {
            descriptions.put(captureKey, new ArrayList<>(captureBuffer));
        }
        captureKey = null;
        captureBuffer.clear();
    }

    // ---------------------------------------------------------------- incoming data

    /**
     * Called for every dialog the server shows. Returns true when the dialog was the side quest list we asked for
     * (the vanilla dialog screen is then skipped).
     */
    public boolean onDialog(Holder<Dialog> holder) {
        if (!fr.poubone.att2.client.hud.HUDConfig.get().questMenuEnabled) return false;
        Dialog dialog = holder.value();
        boolean requested = dialogWaitTicks > 0;

        if (dialog instanceof NoticeDialog notice && containsKey(notice.common().title(), SIDEQUEST_TITLE_KEY)) {
            // "No side quest started yet" notice
            sideQuests.clear();
            sideCompleted = 0;
            lastSideQuestUpdate = System.currentTimeMillis();
            dialogWaitTicks = 0;
            return requested;
        }

        if (!(dialog instanceof MultiActionDialog multi)) {
            return false;
        }

        if (bodyContainsKey(multi.common(), SIDEQUEST_TITLE_KEY)) {
            parseSideQuestDialog(multi);
            dialogWaitTicks = 0;
            return requested;
        }

        boolean dailyRequested = dailyDialogWaitTicks > 0;
        if (bodyContainsKey(multi.common(), DAILYQUEST_LIST_TITLE_KEY)) {
            parseDailyCityList(multi);
            dailyDialogWaitTicks = 0;
            return dailyRequested;
        }

        String city = findDailyCityTitle(multi.common());
        if (city != null) {
            parseDailyCityDialog(city, multi);
            dailyDialogWaitTicks = 0;
            return dailyRequested;
        }

        return false;
    }

    /** The city list only contains cities where at least one daily quest was discovered. */
    private void parseDailyCityList(MultiActionDialog dialog) {
        pendingCityTriggers.clear();
        List<String> listedCities = new ArrayList<>();
        for (ActionButton button : dialog.actions()) {
            String city = findKeyGroup(button.button().label(), DAILYQUEST_CITY_KEY, 1);
            int trigger = triggerOf(button);
            if (city == null || city.equals("error") || trigger < 0) continue;
            listedCities.add(city);
            pendingCityTriggers.addLast(trigger);
        }
        dailyQuests.keySet().retainAll(listedCities);
        for (String city : listedCities) {
            dailyQuests.putIfAbsent(city, new ArrayList<>());
        }
        hasDailyData = true;
        cityRequestCooldown = 0;
    }

    private void parseDailyCityDialog(String city, MultiActionDialog dialog) {
        List<QuestInfo> parsed = new ArrayList<>();
        for (ActionButton button : dialog.actions()) {
            Component label = button.button().label();
            String labelCity = findKeyGroup(label, DAILYQUEST_NAME_KEY, 1);
            String numberText = findKeyGroup(label, DAILYQUEST_NAME_KEY, 2);
            if (labelCity == null || numberText == null) continue;

            int number = Integer.parseInt(numberText);
            QuestStatus status = QuestStatus.fromColor(findColor(label));
            int remaining = -1;
            if (button.button().tooltip().isPresent()) {
                TranslatableContents time = findTranslatable(button.button().tooltip().get(), DAILYQUEST_REMAINING_KEY);
                if (time != null && time.getArgs().length >= 2) {
                    int minutes = argToInt(time.getArgs()[0]).orElse(0);
                    int seconds = argToInt(time.getArgs()[1]).orElse(0);
                    remaining = minutes * 60 + seconds;
                }
            }
            Component name = Component.translatable("att2.dailyquest." + labelCity + "." + number + ".name");
            parsed.add(new QuestInfo(QuestType.DAILY, number, name, status, triggerOf(button), labelCity, remaining));
        }
        parsed.sort((a, b) -> Integer.compare(a.number(), b.number()));
        dailyQuests.put(city, parsed);
        hasDailyData = true;
    }

    private static int triggerOf(ActionButton button) {
        if (button.action().isPresent() && button.action().get() instanceof StaticAction staticAction
                && staticAction.value() instanceof ClickEvent.RunCommand runCommand) {
            int trigger = Att2Triggers.parseTriggerCommand(runCommand.command());
            return trigger == Att2Triggers.CONSCIOUSNESS_CLEAR ? -1 : trigger;
        }
        return -1;
    }

    private static String findDailyCityTitle(CommonDialogData common) {
        String fromTitle = findKeyGroup(common.title(), DAILYQUEST_CITY_TITLE_KEY, 1);
        if (fromTitle != null) return fromTitle;
        for (DialogBody body : common.body()) {
            if (body instanceof PlainMessage message) {
                String city = findKeyGroup(message.contents(), DAILYQUEST_CITY_TITLE_KEY, 1);
                if (city != null) return city;
            }
        }
        return null;
    }

    /** First translation key of the component tree matching the pattern; returns the requested group. */
    private static String findKeyGroup(Component component, Pattern pattern, int group) {
        String[] result = {null};
        visit(component, c -> {
            if (result[0] != null) return;
            if (c.getContents() instanceof TranslatableContents translatable) {
                Matcher matcher = pattern.matcher(translatable.getKey());
                if (matcher.matches()) result[0] = matcher.group(group);
            }
        });
        return result[0];
    }

    private void parseSideQuestDialog(MultiActionDialog dialog) {
        List<QuestInfo> parsed = new ArrayList<>();
        for (ActionButton button : dialog.actions()) {
            Component label = button.button().label();
            int number = findSideQuestNumber(label);
            if (number < 0) continue;

            QuestStatus status = QuestStatus.fromColor(findColor(label));
            int trigger = -1;
            if (button.action().isPresent() && button.action().get() instanceof StaticAction staticAction
                    && staticAction.value() instanceof ClickEvent.RunCommand runCommand) {
                trigger = Att2Triggers.parseTriggerCommand(runCommand.command());
                if (trigger == Att2Triggers.CONSCIOUSNESS_CLEAR) trigger = -1;
            }
            Component name = Component.translatable("consciousness.sidequest." + number);
            parsed.add(new QuestInfo(QuestType.SIDE, number, name, status, trigger));
        }
        parsed.sort((a, b) -> Integer.compare(a.number(), b.number()));

        sideQuests.clear();
        sideQuests.addAll(parsed);
        sideCompleted = (int) parsed.stream().filter(q -> q.status() == QuestStatus.COMPLETED).count();
        readTitleCounts(dialog.common());
        lastSideQuestUpdate = System.currentTimeMillis();
    }

    private void readTitleCounts(CommonDialogData common) {
        for (DialogBody body : common.body()) {
            if (!(body instanceof PlainMessage message)) continue;
            TranslatableContents contents = findTranslatable(message.contents(), SIDEQUEST_TITLE_KEY);
            if (contents == null || contents.getArgs().length < 2) continue;
            Optional<Integer> completed = argToInt(contents.getArgs()[0]);
            Optional<Integer> total = argToInt(contents.getArgs()[1]);
            completed.ifPresent(v -> sideCompleted = v);
            total.ifPresent(v -> sideTotal = v);
        }
    }

    /**
     * Called for every system chat message. Returns false to hide the message from the chat while the quest
     * book is capturing it.
     */
    public boolean onSystemMessage(Component message, boolean overlay) {
        if (!fr.poubone.att2.client.hud.HUDConfig.get().questMenuEnabled) return true;
        if (overlay) return true;

        TranslatableContents progress = findTranslatable(message, MAINQUEST_PROGRESS_KEY);
        if (progress != null && progress.getArgs().length > 0) {
            argToInt(progress.getArgs()[0]).ifPresent(step -> mainStep = step);
        }

        if (captureTicks <= 0 || captureKey == null) return true;

        // The book already shows the main quest progress; keep only the objective text.
        if (progress == null) {
            captureBuffer.add(message);
        }
        // Give the datapack a couple more ticks to finish printing.
        captureTicks = Math.max(captureTicks, 5);
        return !(Minecraft.getInstance().screen instanceof QuestBookScreen);
    }

    // ---------------------------------------------------------------- queries

    public List<QuestInfo> getSideQuests() {
        return Collections.unmodifiableList(sideQuests);
    }

    public boolean hasSideQuestData() {
        return lastSideQuestUpdate > 0;
    }

    /** Daily quests grouped by city, cities in the map's order. */
    public List<QuestInfo> getDailyQuests() {
        List<QuestInfo> all = new ArrayList<>();
        dailyQuests.values().forEach(all::addAll);
        return all;
    }

    public boolean hasDailyQuestData() {
        return hasDailyData;
    }

    public boolean isWaitingForDailyQuests() {
        return dailyDialogWaitTicks > 0 || !pendingCityTriggers.isEmpty();
    }

    public boolean isWaitingForSideQuests() {
        return dialogWaitTicks > 0;
    }

    public boolean isCapturing() {
        return captureTicks > 0;
    }

    public int getSideCompleted() {
        return sideCompleted;
    }

    public int getSideTotal() {
        return sideTotal;
    }

    /** Current main quest step, or -1 when unknown. */
    public int getMainStep() {
        return mainStep;
    }

    public int getMainMaxStep() {
        return MAIN_QUEST_MAX_STEP;
    }

    public QuestInfo getMainQuest() {
        Component name = ModLanguageManager.get("quest_book.main_quest");
        return new QuestInfo(QuestType.MAIN, Math.max(mainStep, 0), name,
                mainStep >= MAIN_QUEST_MAX_STEP ? QuestStatus.COMPLETED : QuestStatus.STARTED, Att2Triggers.MAIN_QUEST_GO);
    }

    public List<Component> getDescription(QuestInfo quest) {
        if (quest == null) return List.of();
        if (captureKey != null && captureKey.equals(quest.key()) && !captureBuffer.isEmpty()) {
            return Collections.unmodifiableList(captureBuffer);
        }
        return descriptions.getOrDefault(quest.key(), List.of());
    }

    /** Last quest opened in the book (also used by Discord Rich Presence). */
    public String getSelectedKey() {
        return selectedKey;
    }

    public void setSelected(QuestInfo quest) {
        selectedKey = quest == null ? null : quest.key();
    }

    public QuestInfo findByKey(String key) {
        if (key == null) return null;
        if (key.equals(getMainQuest().key())) return getMainQuest();
        QuestInfo side = sideQuests.stream().filter(q -> q.key().equals(key)).findFirst().orElse(null);
        if (side != null) return side;
        return getDailyQuests().stream().filter(q -> q.key().equals(key)).findFirst().orElse(null);
    }

    // ---------------------------------------------------------------- component helpers

    private static int findSideQuestNumber(Component component) {
        int[] result = {-1};
        visit(component, c -> {
            if (result[0] >= 0) return;
            if (c.getContents() instanceof TranslatableContents translatable) {
                Matcher matcher = SIDEQUEST_NAME_KEY.matcher(translatable.getKey());
                if (matcher.matches()) {
                    result[0] = Integer.parseInt(matcher.group(1));
                }
            }
        });
        return result[0];
    }

    private static TextColor findColor(Component component) {
        TextColor[] result = {null};
        visit(component, c -> {
            if (result[0] == null && c.getStyle().getColor() != null) {
                result[0] = c.getStyle().getColor();
            }
        });
        return result[0];
    }

    private static boolean bodyContainsKey(CommonDialogData common, String key) {
        if (containsKey(common.title(), key)) return true;
        for (DialogBody body : common.body()) {
            if (body instanceof PlainMessage message && containsKey(message.contents(), key)) return true;
        }
        return false;
    }

    private static boolean containsKey(Component component, String key) {
        return findTranslatable(component, key) != null;
    }

    private static TranslatableContents findTranslatable(Component component, String key) {
        TranslatableContents[] result = {null};
        visit(component, c -> {
            if (result[0] == null && c.getContents() instanceof TranslatableContents translatable
                    && translatable.getKey().equals(key)) {
                result[0] = translatable;
            }
        });
        return result[0];
    }

    private static void visit(Component component, java.util.function.Consumer<Component> visitor) {
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

    private static Optional<Integer> argToInt(Object arg) {
        String text = arg instanceof Component component ? component.getString() : String.valueOf(arg);
        try {
            return Optional.of(Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
