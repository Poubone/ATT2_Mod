package fr.poubone.att2.client.quest.widget;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.QuestBookScreen;
import fr.poubone.att2.client.quest.QuestBookSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import fr.poubone.att2.client.quest.QuestInfo;
import fr.poubone.att2.client.quest.QuestModel;
import fr.poubone.att2.client.quest.QuestStatus;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** One quest line on the right page. */
public class QuestEntryButton extends QuestBookWidget {

    private final QuestInfo quest;
    private final QuestBookScreen screen;
    private final ItemStack icon;

    public QuestEntryButton(int x, int y, int width, int height, QuestInfo quest, QuestBookScreen screen) {
        super(x, y, width, height, quest.name());
        this.quest = quest;
        this.screen = screen;
        this.icon = new ItemStack(quest.isMain() ? Items.WRITTEN_BOOK : quest.isDaily() ? Items.CLOCK : Items.PAPER);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean selected = quest.key().equals(screen.getSelectedKey());
        boolean hovered = isHoveredOrFocused();
        int background = selected ? 0xFFD8C28D : hovered ? 0xFFE0D2AE : 0x45C7B78F;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, background);
        graphics.fill(getX(), getY(), getX() + 2, getY() + height,
                selected ? 0xFF86602C : QuestBookSkin.statusColor(quest.status()));
        graphics.fill(getX() + 3, getY() + height - 1, getX() + width, getY() + height, 0x4086602C);
        graphics.renderItem(icon, getX() + 6, getY() + (height - 16) / 2);
        Font font = Minecraft.getInstance().font;
        float scale = screen.textScale();
        int maxWidth = (int) ((width - 32) / scale);
        String name = quest.plainName();
        if (font.width(name) > maxWidth) name = font.plainSubstrByWidth(name, maxWidth - font.width("...")) + "...";
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX() + 27, getY() + (height < 25 ? (height - font.lineHeight) / 2 : 3));
        graphics.pose().scale(scale, scale);
        graphics.drawString(font, name, 0, 0, QuestBookSkin.INK, false);
        graphics.pose().popMatrix();
        Component status = ModLanguageManager.get("quest_book.status." + quest.status().name().toLowerCase(java.util.Locale.ROOT));
        if (height >= 25) graphics.drawString(font, status, getX() + 27, getY() + height - font.lineHeight - 2,
                QuestBookSkin.statusColor(quest.status()), false);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        screen.selectQuest(quest);
    }

    @Override
    public List<Component> getTooltipLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(quest.name().copy().withStyle(ChatFormatting.BOLD).withStyle(quest.status().formatting()));
        if (quest.isMain()) {
            int step = QuestModel.get().getMainStep();
            String progress = step < 0 ? "?" : String.valueOf(step);
            lines.add(Component.literal(ModLanguageManager.format("quest_book.main_progress", "step", progress,
                    "max", QuestModel.get().getMainMaxStep())).withStyle(ChatFormatting.GRAY));
        } else if (quest.isDaily()) {
            lines.add(Component.literal(ModLanguageManager.getString("quest_book.daily_city") + " ")
                    .append(quest.cityName()).withStyle(ChatFormatting.GRAY));
            if (quest.remainingSeconds() >= 0) {
                lines.add(Component.literal(ModLanguageManager.format("quest_book.daily_remaining", "time", quest.formatRemainingTime()))
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            lines.add(Component.literal(ModLanguageManager.format("quest_book.side_number", "n", quest.number())).withStyle(ChatFormatting.GRAY));
        }
        lines.add(ModLanguageManager.get("quest_book.status." + quest.status().name().toLowerCase()).withStyle(quest.status().formatting()));
        if (quest.hasDetails() || quest.isMain()) {
            lines.add(Component.literal(""));
            lines.add(ModLanguageManager.get("quest_book.tooltip.left_click").withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD));
        }
        return lines;
    }

    public QuestInfo getQuest() {
        return quest;
    }
}
