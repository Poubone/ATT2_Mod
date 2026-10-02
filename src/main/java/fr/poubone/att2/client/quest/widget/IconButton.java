package fr.poubone.att2.client.quest.widget;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.quest.QuestBookTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Small textured button: either a two-state "offset" icon, or an icon on a coloured square (filter buttons). */
public class IconButton extends QuestBookWidget {

    private final QuestBookTextures texture;
    private final boolean offsetIcon;
    private final Consumer<MouseButtonEvent> onPress;
    private final BooleanSupplier enabled;
    private final BooleanSupplier visibleSupplier;
    private final List<Component> tooltip;

    /** Two-state icon (normal / hovered) drawn at the widget size. */
    public static IconButton offset(int x, int y, int width, int height, QuestBookTextures texture, List<Component> tooltip, Runnable onPress) {
        return new IconButton(x, y, width, height, texture, true, tooltip, event -> onPress.run(), null, () -> true);
    }

    public static IconButton offset(int x, int y, int width, int height, QuestBookTextures texture, List<Component> tooltip, Runnable onPress, BooleanSupplier visible) {
        return new IconButton(x, y, width, height, texture, true, tooltip, event -> onPress.run(), null, visible);
    }

    /** Filter button: coloured square (green when enabled) with the icon centred on it. */
    public static IconButton filter(int x, int y, int width, int height, QuestBookTextures texture, List<Component> tooltip, Runnable onPress, BooleanSupplier enabled) {
        return new IconButton(x, y, width, height, texture, false, tooltip, event -> onPress.run(), enabled, () -> true);
    }

    /** Filter button whose action receives the click (to read modifiers such as shift). */
    public static IconButton filter(int x, int y, int width, int height, QuestBookTextures texture, List<Component> tooltip, Consumer<MouseButtonEvent> onPress, BooleanSupplier enabled) {
        return new IconButton(x, y, width, height, texture, false, tooltip, onPress, enabled, () -> true);
    }

    private IconButton(int x, int y, int width, int height, QuestBookTextures texture, boolean offsetIcon,
                       List<Component> tooltip, Consumer<MouseButtonEvent> onPress, BooleanSupplier enabled, BooleanSupplier visible) {
        super(x, y, width, height, Component.literal("Icon Button"));
        this.texture = texture;
        this.offsetIcon = offsetIcon;
        this.tooltip = tooltip;
        this.onPress = onPress;
        this.enabled = enabled;
        this.visibleSupplier = visible;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.visible = visibleSupplier.getAsBoolean();
        if (!this.visible) return;

        if (offsetIcon) {
            texture.drawOffsetIcon(graphics, getX(), getY(), width, height, isHovered());
            return;
        }

        HUDConfig config = HUDConfig.get();
        int color = enabled != null && enabled.getAsBoolean()
                ? HUDConfig.color(config.questBookFilterEnabledColor, 0xFFA4D48E)
                : (isHovered()
                        ? HUDConfig.color(config.questBookFilterHoverColor, 0xFF797465)
                        : HUDConfig.color(config.questBookFilterColor, 0xFFB5AE97));
        graphics.fill(getX(), getY(), getX() + width, getY() + height, color);
        texture.draw(graphics, getX() + (width - texture.width()) / 2, getY() + (height - texture.height()) / 2);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (visibleSupplier.getAsBoolean()) onPress.accept(event);
    }

    @Override
    public List<Component> getTooltipLines() {
        return visibleSupplier.getAsBoolean() ? tooltip : List.of();
    }
}
