package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.quest.QuestBookSkin;
import fr.poubone.att2.client.quest.QuestStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Top-of-screen notice built from Kenney quest-book panels (same family as shops / journal).
 */
public final class ModToast {
    public enum Kind { ERROR, OK, INFO }

    private static final int FADE_IN = 4;
    private static final int HOLD = 70;
    private static final int FADE_OUT = 12;
    private static final int LIFE = FADE_IN + HOLD + FADE_OUT;

    private static Component title;
    private static Component subtitle;
    private static Kind kind = Kind.INFO;
    private static int ticks = -1;

    private ModToast() {
    }

    public static void reset() {
        title = null;
        subtitle = null;
        kind = Kind.INFO;
        ticks = -1;
    }

    public static void showError(Component text) {
        show(text, null, Kind.ERROR);
    }

    public static void showOk(Component text) {
        show(text, null, Kind.OK);
    }

    public static void showInfo(Component text) {
        show(text, null, Kind.INFO);
    }

    /** City-arrival style: title + subtitle on the shared Kenney banner. */
    public static void showBanner(Component titleText, Component subtitleText) {
        show(titleText, subtitleText, Kind.INFO);
    }

    public static void show(Component titleText, Component subtitleText, Kind toastKind) {
        if (titleText == null) {
            return;
        }
        title = titleText;
        subtitle = subtitleText;
        kind = toastKind == null ? Kind.INFO : toastKind;
        ticks = 0;
    }

    public static void tick() {
        if (ticks < 0) {
            return;
        }
        if (++ticks >= LIFE) {
            reset();
        }
    }

    public static boolean isActive() {
        return ticks >= 0 && title != null;
    }

    public static void render(GuiGraphics graphics) {
        if (!isActive() || !visible()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;

        String name = title.getString();
        String tag = subtitle == null ? "" : subtitle.getString();
        int nameW = font.width(name);
        int tagW = tag.isEmpty() ? 0 : font.width(tag);
        int textW = Math.max(nameW, tagW);
        int padX = 18;
        int boxW = Math.min(client.getWindow().getGuiScaledWidth() - 24, Math.max(160, textW + padX * 2));
        int boxH = tag.isEmpty() ? 28 : 40;
        int x = (client.getWindow().getGuiScaledWidth() - boxW) / 2;
        int y = 12;

        // Soft drop shadow under the wooden panel.
        graphics.fill(x + 3, y + 4, x + boxW + 3, y + boxH + 4, 0x50000000);
        QuestBookSkin.panel(graphics, "cover", x, y, boxW, boxH, 100, 100, 6);
        int inset = 4;
        QuestBookSkin.panel(graphics, "page", x + inset, y + inset, boxW - 2 * inset, boxH - 2 * inset, 100, 100, 4);

        int accent = switch (kind) {
            case ERROR -> QuestBookSkin.statusColor(QuestStatus.FAILED);
            case OK -> QuestBookSkin.statusColor(QuestStatus.COMPLETED);
            case INFO -> QuestBookSkin.RULE;
        };
        graphics.fill(x + inset + 2, y + inset + 2, x + boxW - inset - 2, y + inset + 4, accent);

        int nameColor = QuestBookSkin.INK;
        int nameX = x + (boxW - nameW) / 2;
        int nameY = tag.isEmpty() ? y + (boxH - 8) / 2 : y + 9;
        graphics.drawString(font, name, nameX, nameY, nameColor, false);
        if (!tag.isEmpty()) {
            graphics.drawString(font, tag, x + (boxW - tagW) / 2, y + 22, QuestBookSkin.MUTED, false);
        }
    }

    private static boolean visible() {
        if (ticks < 0) {
            return false;
        }
        // Hard cut during fade windows keeps nine-slice sprites crisp (no shader tint).
        return ticks >= FADE_IN && ticks < FADE_IN + HOLD;
    }
}
