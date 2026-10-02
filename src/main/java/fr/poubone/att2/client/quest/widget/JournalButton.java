package fr.poubone.att2.client.quest.widget;

import fr.poubone.att2.client.quest.QuestBookSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Labelled book control with a generous hit target and visible keyboard focus. */
public class JournalButton extends QuestBookWidget {
    private final Consumer<MouseButtonEvent> press;
    private final BooleanSupplier selected;
    private final BooleanSupplier available;
    private final List<Component> tooltip;
    private final ItemStack item;
    private final String icon;

    public JournalButton(int x, int y, int w, int h, Component label, ItemStack item, String icon,
                         List<Component> tooltip, BooleanSupplier selected, BooleanSupplier available,
                         Consumer<MouseButtonEvent> press) {
        super(x, y, w, h, label);
        this.item = item; this.icon = icon; this.tooltip = tooltip;
        this.selected = selected; this.available = available; this.press = press;
    }

    @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        active = available.getAsBoolean();
        boolean lit = selected.getAsBoolean();
        QuestBookSkin.button(g, getX(), getY(), width, height, lit, active && isHoveredOrFocused());
        var font = Minecraft.getInstance().font;
        if (icon != null) {
            QuestBookSkin.icon(g, icon, getX() + (width - 12) / 2, getY() + (height - 12) / 2, 12, 12);
        } else if (font.width(getMessage()) <= width - 8) {
            g.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2,
                    getY() + (height - font.lineHeight) / 2, lit ? QuestBookSkin.CREAM : QuestBookSkin.INK, false);
        } else if (!item.isEmpty()) {
            g.renderItem(item, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
        } else {
            String shortText = font.plainSubstrByWidth(getMessage().getString(), width - 12);
            g.drawString(font, shortText, getX() + 6, getY() + (height - font.lineHeight) / 2, QuestBookSkin.INK, false);
        }
        if (!active) g.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0x70EDE2C8);
        if (isFocused()) {
            g.fill(getX() + 4, getY() + height - 4, getX() + width - 4, getY() + height - 3, QuestBookSkin.INK);
        }
    }

    @Override public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (available.getAsBoolean()) press.accept(event);
    }
    @Override public List<Component> getTooltipLines() { return tooltip; }
}
