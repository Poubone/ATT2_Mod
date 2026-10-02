package fr.poubone.att2.client.shop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Shop chrome: ink labels, page chevrons and category tabs — no raw black boxes. */
public class ShopActionButton extends AbstractWidget {
    public enum Chrome { ACTION, NAV, TAB, CLOSE }

    private final ShopAction action;
    private final int idleColor;
    private final int hoverColor;
    private final Runnable onPress;
    private final Chrome chrome;
    private final BooleanSupplier selected;
    private final ShopType theme;

    public ShopActionButton(int x, int y, int width, int height, ShopAction action,
                            int idleColor, int hoverColor, Runnable onPress) {
        this(x, y, width, height, action, idleColor, hoverColor, onPress, Chrome.ACTION, () -> false);
    }

    public ShopActionButton(int x, int y, int width, int height, ShopAction action,
                            int idleColor, int hoverColor, Runnable onPress,
                            Chrome chrome, BooleanSupplier selected) {
        this(x, y, width, height, action, idleColor, hoverColor, onPress, chrome, selected, null);
    }

    public ShopActionButton(int x, int y, int width, int height, ShopAction action,
                            int idleColor, int hoverColor, Runnable onPress,
                            Chrome chrome, BooleanSupplier selected, ShopType theme) {
        super(x, y, width, height, action.label());
        this.action = action;
        this.idleColor = idleColor;
        this.hoverColor = hoverColor;
        this.onPress = onPress;
        this.chrome = chrome;
        this.selected = selected;
        this.theme = theme;
    }

    public ShopAction action() {
        return action;
    }

    public List<Component> tooltipLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(action.label());
        if (action.tip() != null && !action.tip().getString().isBlank()) {
            lines.add(action.tip());
        }
        return lines;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (theme != null) {
            renderThemed(graphics);
            return;
        }
        boolean hover = isHovered();
        boolean on = selected.getAsBoolean();
        int color = hover || on ? hoverColor : idleColor;
        Font font = Minecraft.getInstance().font;
        String label = font.plainSubstrByWidth(action.label().getString(), width - 4);
        int tw = font.width(label);
        int tx = getX() + Math.max(1, (width - tw) / 2);
        int ty = getY() + Math.max(0, (height - 8) / 2);

        if (chrome == Chrome.NAV) {
            graphics.drawString(font, label, tx, ty, color, false);
            return;
        }

        if (hover && !on) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0x22000000);
        }
        graphics.drawString(font, label, tx, ty, color, false);
        if (on || hover) {
            int lineY = getY() + height - 2;
            int pad = Math.max(2, (width - tw) / 2);
            graphics.fill(getX() + pad, lineY, getX() + width - pad, lineY + 1, color);
            int mid = getX() + width / 2;
            graphics.fill(mid - 1, lineY - 1, mid + 1, lineY + 2, color);
        }
    }

    private void renderThemed(GuiGraphics graphics) {
        String state = !active ? "disabled" : selected.getAsBoolean() ? "selected"
                : isHovered() ? "hover" : "normal";
        String asset = switch (chrome) {
            case TAB -> "category_";
            case ACTION -> "button_reset_";
            case CLOSE -> "button_close_";
            case NAV -> "button_page_";
        };
        int sw = switch (chrome) { case TAB -> 250; case ACTION -> 248; case CLOSE -> 42; case NAV -> 34; };
        int sh = switch (chrome) { case TAB -> 58; case ACTION -> 50; case CLOSE -> 42; case NAV -> 34; };
        ShopSkin.texture(graphics, theme, asset + state, getX(), getY(), width, height, sw, sh);
        if (chrome == Chrome.CLOSE || chrome == Chrome.NAV) {
            String icon = chrome == Chrome.CLOSE ? "close_icon" : action.label().getString().equals("‹") ? "arrow_left" : "arrow_right";
            int iconSize = Math.max(3, height * 2 / 5);
            ShopSkin.texture(graphics, theme, icon, getX() + (width - iconSize) / 2,
                    getY() + (height - iconSize) / 2, iconSize, iconSize,
                    chrome == Chrome.CLOSE ? 18 : 14, chrome == Chrome.CLOSE ? 18 : 14);
            return;
        }
        float scale = Math.min(height / 16f, width / (chrome == Chrome.TAB ? 250f : 420f) * 2.7f);
        ShopTheme.text(graphics, action.label(), getX() + 3, getY() + Math.max(1, (int) ((height - 8 * scale) / 2)),
                width - 6, scale, active ? (selected.getAsBoolean() ? 0xFFF5EAD0 : 0xFF382B21) : 0xFF777777, true);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onPress.run();
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == 0;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.85F));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
