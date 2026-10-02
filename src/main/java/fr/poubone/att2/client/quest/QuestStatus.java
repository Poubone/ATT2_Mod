package fr.poubone.att2.client.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;

/** Status of a quest, derived from the colour the map gives to its entry in the Consciousness dialog. */
public enum QuestStatus {
    STARTED(ChatFormatting.YELLOW),
    COMPLETED(ChatFormatting.GREEN),
    FAILED(ChatFormatting.DARK_RED),
    UNKNOWN(ChatFormatting.GRAY);

    private final ChatFormatting formatting;

    QuestStatus(ChatFormatting formatting) {
        this.formatting = formatting;
    }

    public ChatFormatting formatting() {
        return formatting;
    }

    public static QuestStatus fromColor(TextColor color) {
        if (color == null) return UNKNOWN;
        int rgb = color.getValue();
        if (rgb == ChatFormatting.YELLOW.getColor()) return STARTED;
        if (rgb == ChatFormatting.GREEN.getColor()) return COMPLETED;
        if (rgb == ChatFormatting.DARK_RED.getColor() || rgb == ChatFormatting.RED.getColor()) return FAILED;
        return UNKNOWN;
    }
}
