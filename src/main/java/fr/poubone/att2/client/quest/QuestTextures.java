package fr.poubone.att2.client.quest;

import net.minecraft.resources.Identifier;

/**
 * Chrome of the quest book: a 512×256 open book with two cream pages.
 * Charles' counter still uses {@link QuestBookTextures}.
 */
public final class QuestTextures {
    public static final int PANEL_WIDTH = 512;
    public static final int PANEL_HEIGHT = 256;

    /** Left parchment page, inside the gold corner caps. */
    public static final int LEFT_X = 58;
    public static final int LEFT_Y = 36;
    public static final int LEFT_W = 164;
    public static final int LEFT_H = 176;

    /** Right parchment page, inside the gold corner caps. */
    public static final int RIGHT_X = 290;
    public static final int RIGHT_Y = 36;
    public static final int RIGHT_W = 164;
    public static final int RIGHT_H = 176;

    public static final int TITLE = 0xFFB8862B;
    public static final int TEXT = 0xFF2A1C10;
    public static final int SECONDARY = 0xFF5A4634;
    public static final int ENTRY = 0x332A1C10;
    public static final int ENTRY_HOVER = 0x55402818;
    public static final int SELECTED = 0x66C4A050;
    public static final int SELECTED_HOVER = 0x88D4B060;
    public static final int TRACKED = 0x55608040;
    public static final int TRACKED_HOVER = 0x7470A050;

    public static final int ICON_SRC = 64;

    private QuestTextures() {
    }

    public static Identifier icon(String name, boolean hover) {
        String file = hover ? name + "_hover" : name;
        return Identifier.fromNamespaceAndPath("att2", "textures/quest/icon/" + file + ".png");
    }

    public static int statusColor(QuestStatus status) {
        return switch (status) {
            case STARTED -> 0xFFE8C56A;
            case COMPLETED -> 0xFF6BCB7A;
            case FAILED -> 0xFFC45A4A;
            case UNKNOWN -> 0xFF8A7A68;
        };
    }
}
