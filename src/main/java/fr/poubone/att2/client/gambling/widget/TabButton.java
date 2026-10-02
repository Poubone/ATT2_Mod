package fr.poubone.att2.client.gambling.widget;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.widget.QuestBookWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BooleanSupplier;

/** Square tab of the left page, same look as the quest book filters but with an item as icon. */
public class TabButton extends QuestBookWidget {
    private final ItemStack icon;
    private final List<Component> tooltip;
    private final BooleanSupplier selected;
    private final Runnable onPress;

    public TabButton(int x, int y, ItemStack icon, List<Component> tooltip, BooleanSupplier selected, Runnable onPress) {
        this(x, y, 30, icon, tooltip, selected, onPress);
    }

    public TabButton(int x, int y, int size, ItemStack icon, List<Component> tooltip, BooleanSupplier selected, Runnable onPress) {
        super(x, y, size, size, Component.literal("Tab"));
        this.icon = icon;
        this.tooltip = tooltip;
        this.selected = selected;
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        HUDConfig config = HUDConfig.get();
        int color = selected.getAsBoolean()
                ? HUDConfig.color(config.questBookFilterEnabledColor, 0xFFA4D48E)
                : (isHovered()
                        ? HUDConfig.color(config.questBookFilterHoverColor, 0xFF797465)
                        : HUDConfig.color(config.questBookFilterColor, 0xFFB5AE97));
        graphics.fill(getX(), getY(), getX() + width, getY() + height, color);
        graphics.renderItem(icon, getX() + (width - 16) / 2, getY() + (height - 16) / 2);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) onPress.run();
    }

    @Override
    public List<Component> getTooltipLines() {
        return tooltip;
    }
}
