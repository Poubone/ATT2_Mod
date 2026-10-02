package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.shop.ShopTooltips;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.shop.ShopType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * One recipe: result slot, =, ingredient slots (wrap), stock column on the right.
 * Craft click = result slot or empty row chrome, never an ingredient slot.
 */
public class RuneRecipeRow extends AbstractWidget {
    public record Slot(ItemStack stack, List<Component> extraTooltip, boolean craftClick) {
    }

    public record StockLine(ItemStack icon, Component text) {
        public static StockLine textOnly(Component text) {
            return new StockLine(ItemStack.EMPTY, text);
        }
    }

    public static final int SLOT = 32;
    public static final int GAP = 2;
    public static final int STOCK_W = 128;
    public static final int ROW_PAD = 2;
    public static final int STOCK_ICON = 16;

    private static final int PLUS_W = 8;

    private final Slot result;
    private final List<Slot> ingredients;
    private final List<Component> captionLines;
    private final List<StockLine> stockLines;
    private final boolean clickable;
    private final Runnable onCraft;
    private final ShopType theme;

    public RuneRecipeRow(int x, int y, int width, int height,
                         Slot result, List<Slot> ingredients, List<Component> captionLines,
                         List<StockLine> stockLines, boolean clickable, Runnable onCraft) {
        this(x, y, width, height, result, ingredients, captionLines, stockLines, clickable, onCraft, ShopType.DAHAL);
    }

    public RuneRecipeRow(int x, int y, int width, int height,
                         Slot result, List<Slot> ingredients, List<Component> captionLines,
                         List<StockLine> stockLines, boolean clickable, Runnable onCraft, ShopType theme) {
        super(x, y, width, height, result.stack().getHoverName());
        this.result = result;
        this.ingredients = ingredients;
        this.captionLines = captionLines;
        this.stockLines = stockLines;
        this.clickable = clickable;
        this.onCraft = onCraft;
        this.theme = theme;
    }

    public static int preferredHeight(int width, int ingredientCount, List<StockLine> stockLines) {
        int inner = Math.max(SLOT, width - STOCK_W - SLOT - GAP - 10);
        int perLine = Math.max(1, inner / (SLOT + GAP + PLUS_W));
        int lines = ingredientCount == 0 ? 1 : Math.max(1, (int) Math.ceil(ingredientCount / (double) perLine));
        int slotHeight = ROW_PAD * 2 + lines * (SLOT + GAP) - GAP;
        int stockHeight = stockLines.isEmpty() ? 0 : 18;
        int stockUsed = 0;
        for (StockLine line : stockLines) {
            int cellWidth = stockWidth(line);
            if (stockUsed + cellWidth > STOCK_W) { stockHeight += 18; stockUsed = 0; }
            stockUsed += cellWidth;
        }
        if (stockHeight > 0) {
            stockHeight += 4;
        }
        return Math.max(slotHeight, stockHeight);
    }

    public List<Component> tooltipAt(int mouseX, int mouseY) {
        int stockX = getX() + width - STOCK_W;
        int stockY = getY() + ROW_PAD;
        for (StockLine line : stockLines) {
            int cellWidth = stockWidth(line);
            if (stockX + cellWidth > getX() + width) { stockX = getX() + width - STOCK_W; stockY += 18; }
            if (mouseX >= stockX && mouseX < stockX + cellWidth && mouseY >= stockY && mouseY < stockY + 16) {
                return List.of(stockName(line).copy().append(": ").append(line.text()));
            }
            stockX += cellWidth;
        }
        Slot hit = slotAt(mouseX, mouseY);
        if (hit == null) return List.of();
        List<Component> lines = new ArrayList<>();
        RuneItems.inventoryMatch(hit.stack()).ifPresentOrElse(
                matched -> lines.addAll(ShopTooltips.of(matched)),
                () -> lines.add(hit.stack().getHoverName()));
        lines.addAll(hit.extraTooltip());
        return lines;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        fr.poubone.att2.client.shop.ShopSkin.texture(graphics, theme, isHoveredOrFocused() && clickable ? "category_hover" : "category_normal",
                getX() - 3, getY(), width + 6, height, 250, 58);
        Font font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY() + ROW_PAD;
        drawSlot(graphics, x, y, result, contains(mouseX, mouseY, x, y));
        x += SLOT + 2;
        graphics.drawString(font, "=", x, y + 12, 0xFF705E47, false);
        x += font.width("=") + 2;

        int wrapLeft = x;
        int wrapRight = getX() + width - STOCK_W - 2;
        if (ingredients.isEmpty() && !captionLines.isEmpty()) {
            int cy = y + 12;
            for (Component line : captionLines) {
                ShopTheme.text(graphics, line, x, cy, wrapRight - x, 0.85f, 0xFF382B21, false);
                cy += 10;
            }
        } else {
            for (int i = 0; i < ingredients.size(); i++) {
                if (x + SLOT > wrapRight && i > 0) {
                    x = wrapLeft;
                    y += SLOT + GAP;
                }
                if (i > 0) {
                    graphics.drawString(font, "+", x, y + 12, 0xFF705E47, false);
                    x += font.width("+") + 2;
                }
                Slot slot = ingredients.get(i);
                drawSlot(graphics, x, y, slot, contains(mouseX, mouseY, x, y));
                x += SLOT + GAP;
            }
        }

        int sx = getX() + width - STOCK_W;
        int sy = getY() + ROW_PAD;
        for (StockLine line : stockLines) {
            int cellWidth = stockWidth(line);
            if (sx + cellWidth > getX() + width) { sx = getX() + width - STOCK_W; sy += 18; }
            if (!line.icon().isEmpty()) {
                graphics.renderItem(line.icon(), sx, sy);
                ShopTheme.text(graphics, line.text(), sx + STOCK_ICON + 2, sy + 4, cellWidth - 20, 0.85f, 0xFF382B21, false);
            } else {
                ShopTheme.text(graphics, line.text(), sx, sy + 2, cellWidth - 2, 0.85f, 0xFF382B21, false);
            }
            sx += cellWidth;
        }
    }

    private static int stockWidth(StockLine line) {
        return line.icon().isEmpty() ? STOCK_W : STOCK_W / 2;
    }

    private static Component stockName(StockLine line) {
        if (line.icon().is(net.minecraft.world.item.Items.CHEST)) return Component.translatable("container.inventory");
        if (line.icon().is(net.minecraft.world.item.Items.ENDER_EYE)) return Component.translatable("att2.ui.rune_pouch");
        return line.icon().isEmpty() ? Component.empty() : line.icon().getHoverName();
    }

    private void drawSlot(GuiGraphics graphics, int x, int y, Slot slot, boolean hover) {
        fr.poubone.att2.client.shop.ShopSkin.texture(graphics, theme, hover ? "card_hover" : "card_normal", x, y, SLOT, SLOT, 312, 252);
        graphics.renderItem(slot.stack(), x + 8, y + 8);
        graphics.renderItemDecorations(Minecraft.getInstance().font, slot.stack(), x + 8, y + 8);
    }

    private boolean contains(int mx, int my, int x, int y) {
        return mx >= x && my >= y && mx < x + SLOT && my < y + SLOT;
    }

    private Slot slotAt(int mx, int my) {
        int x = getX();
        int y = getY() + ROW_PAD;
        if (contains(mx, my, x, y)) return result;
        Font font = Minecraft.getInstance().font;
        x += SLOT + 2 + font.width("=") + 2;
        int wrapLeft = x;
        int wrapRight = getX() + width - STOCK_W - 2;
        for (int i = 0; i < ingredients.size(); i++) {
            if (x + SLOT > wrapRight && i > 0) {
                x = wrapLeft;
                y += SLOT + GAP;
            }
            if (i > 0) x += font.width("+") + 2;
            if (contains(mx, my, x, y)) return ingredients.get(i);
            x += SLOT + GAP;
        }
        return null;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!clickable || event.x() >= getX() + width - STOCK_W) return;
        Slot hit = slotAt((int) event.x(), (int) event.y());
        if (hit != null && !hit.craftClick()) return;
        onCraft.run();
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == 0;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (clickable) {
            soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
