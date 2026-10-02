package fr.poubone.att2.client.gambling.widget;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.widget.QuestBookWidget;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.shop.ShopType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BooleanSupplier;

/** One clickable line of the right page: a label on the left, an optional value on the right. */
public class ListEntryButton extends QuestBookWidget {
    private final Component label;
    private final Component value;
    private final List<Component> tooltip;
    private final BooleanSupplier enabled;
    private final Runnable onPress;

    public ListEntryButton(int x, int y, int width, int height, Component label, Component value,
                           List<Component> tooltip, BooleanSupplier enabled, Runnable onPress) {
        super(x, y, width, height, label);
        this.label = label;
        this.value = value;
        this.tooltip = tooltip;
        this.enabled = enabled;
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean active = enabled.getAsBoolean();
        fr.poubone.att2.client.shop.ShopSkin.texture(graphics, ShopType.CHARLES, active && isHovered() ? "category_hover" : "category_normal",
                getX(), getY(), width, height, 250, 58);

        Font font = Minecraft.getInstance().font;
        int textColor = active ? 0xFF382B21 : 0xFF858A89;
        int valueWidth = value == null ? 0 : font.width(value) + 4;
        String name = font.plainSubstrByWidth(label.getString(), width - 4 - valueWidth);
        graphics.drawString(font, Component.literal(name).withStyle(label.getStyle()), getX() + 6, getY() + (height - 8) / 2,
                label.getStyle().getColor() == null ? textColor : 0xFFFFFFFF, false);
        if (value != null) {
            graphics.drawString(font, value, getX() + width - valueWidth - 4, getY() + (height - 8) / 2, 0xFF705E47, false);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && enabled.getAsBoolean()) onPress.run();
    }

    @Override
    public List<Component> getTooltipLines() {
        return tooltip;
    }
}
