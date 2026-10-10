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
    /** {@code CHECK} is an action button with a checkbox; {@code selected} ticks it. */
    public enum Chrome { ACTION, NAV, TAB, CLOSE, CHECK }

    /** Pixel tick drawn in the checkbox, one string per row. */
    private static final String[] TICK = {
            ".......#",
            "......##",
            "#....##.",
            "##..##..",
            ".####...",
            "..##....",
    };
    private static final int TICK_COLOR = 0xFF2E9E3E;
    private static final int BOX_BORDER = 0xFF5A4630;
    private static final int BOX_FILL = 0xFFF5EAD0;

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
        if (chrome == Chrome.CHECK) {
            renderCheck(graphics);
            return;
        }
        String state = !active ? "disabled" : selected.getAsBoolean() ? "selected"
                : isHovered() ? "hover" : "normal";
        String asset = switch (chrome) {
            case TAB -> "category_";
            case ACTION, CHECK -> "button_reset_";
            case CLOSE -> "button_close_";
            case NAV -> "button_page_";
        };
        int sw = switch (chrome) { case TAB -> 250; case ACTION, CHECK -> 248; case CLOSE -> 42; case NAV -> 34; };
        int sh = switch (chrome) { case TAB -> 58; case ACTION, CHECK -> 50; case CLOSE -> 42; case NAV -> 34; };
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

    /** An action button whose left end holds a checkbox, ticked while {@code selected}; the label follows it. */
    private void renderCheck(GuiGraphics graphics) {
        String state = !active ? "disabled" : isHovered() ? "hover" : "normal";
        ShopSkin.texture(graphics, theme, "button_reset_" + state, getX(), getY(), width, height, 248, 50);
        int box = Math.max(5, Math.round(height * 0.62f));
        int boxX = getX() + Math.max(2, (height - box) / 2 + 2), boxY = getY() + (height - box) / 2;
        int border = Math.max(1, box / 9);
        graphics.fill(boxX, boxY, boxX + box, boxY + box, BOX_BORDER);
        graphics.fill(boxX + border, boxY + border, boxX + box - border, boxY + box - border, BOX_FILL);
        if (selected.getAsBoolean()) {
            int inner = box - 2 * border;
            int cell = Math.max(1, inner / (TICK[0].length() + 1));
            int tickW = cell * TICK[0].length(), tickH = cell * TICK.length;
            int tx = boxX + border + (inner - tickW) / 2, ty = boxY + border + (inner - tickH) / 2;
            for (int row = 0; row < TICK.length; row++) {
                for (int col = 0; col < TICK[row].length(); col++) {
                    if (TICK[row].charAt(col) == '#') {
                        graphics.fill(tx + col * cell, ty + row * cell, tx + (col + 1) * cell, ty + (row + 1) * cell, TICK_COLOR);
                    }
                }
            }
        }
        int textX = boxX + box + Math.max(2, box / 3);
        float scale = Math.min(height / 16f, width / 420f * 2.7f);
        ShopTheme.text(graphics, action.label(), textX, getY() + Math.max(1, (int) ((height - 8 * scale) / 2)),
                getX() + width - 3 - textX, scale, active ? 0xFF382B21 : 0xFF777777, false);
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
