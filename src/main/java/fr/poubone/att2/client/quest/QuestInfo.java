package fr.poubone.att2.client.quest;

import net.minecraft.network.chat.Component;

/**
 * One entry of the quest book.
 *
 * @param type             main, side or daily quest
 * @param number           quest number (side quest id, daily quest id within its city, or current step for the main quest)
 * @param name             display name (already translated through the map's resource pack)
 * @param status           progress state
 * @param progressTrigger  ScoreTrigger value that makes the datapack print the quest's current objective, or -1
 * @param city             city of a daily quest (lower-case id used by the datapack), null otherwise
 * @param remainingSeconds time left on a daily quest, or -1 when unknown / not applicable
 */
public record QuestInfo(QuestType type, int number, Component name, QuestStatus status, int progressTrigger,
                        String city, int remainingSeconds) {

    public QuestInfo(QuestType type, int number, Component name, QuestStatus status, int progressTrigger) {
        this(type, number, name, status, progressTrigger, null, -1);
    }

    public boolean isMain() {
        return type == QuestType.MAIN;
    }

    public boolean isDaily() {
        return type == QuestType.DAILY;
    }

    public boolean hasDetails() {
        return progressTrigger > 0;
    }

    /** Stable identity used to match captured descriptions and the selected quest. */
    public String key() {
        return switch (type) {
            case MAIN -> "MAIN:main";
            case SIDE -> "SIDE:" + number;
            case DAILY -> "DAILY:" + city + ":" + number;
        };
    }

    public String plainName() {
        return name.getString();
    }

    /** Translated city name of a daily quest (falls back to the raw id). */
    public Component cityName() {
        if (city == null) return Component.empty();
        Component translated = Component.translatable("consciousness.dailyquest.list." + city);
        return translated.getString().startsWith("consciousness.") ? Component.literal(city) : translated;
    }

    public String formatRemainingTime() {
        if (remainingSeconds < 0) return "";
        return String.format("%d:%02d", remainingSeconds / 60, remainingSeconds % 60);
    }
}
