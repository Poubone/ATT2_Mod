package fr.poubone.att2.client.rune;

import fr.poubone.att2.client.shop.ShopSkin;
import fr.poubone.att2.client.shop.ShopTheme;
import fr.poubone.att2.client.shop.ShopTooltips;
import fr.poubone.att2.client.shop.ShopType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
interface CraftClickHandler {
    void onCraftClick(int button, boolean shift);
}

/**
 * Shop-style recipe card: big result icon, price button, ingredient badges.
 * Full stocks / lore live in {@link #tooltipLines()}.
 */
public class RuneCardButton extends AbstractWidget {
    private final ItemStack result;
    private final Component displayName;
    private final Component priceLabel;
    private final List<ItemStack> ingredients;
    private final List<Component> extraTooltip;
    private final boolean clickable;
    private final CraftClickHandler onCraft;
    private final ShopType theme;

    public RuneCardButton(int x, int y, int width, int height,
                          ItemStack result, Component displayName, Component priceLabel,
                          List<ItemStack> ingredients, List<Component> extraTooltip,
                          boolean clickable, CraftClickHandler onCraft, ShopType theme) {
        super(x, y, width, height, displayName);
        this.result = result;
        this.displayName = displayName;
        this.priceLabel = priceLabel;
        this.ingredients = List.copyOf(ingredients);
        this.extraTooltip = List.copyOf(extraTooltip);
        this.clickable = clickable;
        this.onCraft = onCraft;
        this.theme = theme;
    }

    public List<Component> tooltipLines() {
        List<Component> lines = new ArrayList<>(ShopTooltips.of(result));
        if (lines.isEmpty()) {
            lines.add(displayName);
        }
        lines.addAll(extraTooltip);
        return lines;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hover = isHovered() && clickable;
        ShopSkin.texture(graphics, theme, hover ? "card_hover" : "card_normal",
                getX(), getY(), width, height, 312, 252);
        float s = width / 312f;

        int iconSize = Math.max(1, Math.round(72 * s));
        int iconX = getX() + (width - iconSize) / 2;
        int iconY = getY() + Math.round(22 * s);
        graphics.pose().pushMatrix();
        graphics.pose().translate(iconX, iconY);
        graphics.pose().scale(iconSize / 16f, iconSize / 16f);
        graphics.renderItem(result, 0, 0);
        graphics.renderItemDecorations(Minecraft.getInstance().font, result, 0, 0);
        graphics.pose().popMatrix();

        ShopTheme.text(graphics, displayName,
                getX() + Math.round(10 * s), getY() + Math.round(100 * s),
                width - Math.round(20 * s), s * 2.0f, 0xFF382B21, true);

        int buyX = getX() + Math.round(14 * s);
        int buyY = getY() + Math.round(128 * s);
        int buyW = Math.round(284 * s);
        int buyH = Math.round(32 * s);
        ShopSkin.texture(graphics, theme, hover ? "button_buy_hover" : "button_buy_normal",
                buyX, buyY, buyW, buyH, 276, 38);
        ShopTheme.text(graphics, priceLabel,
                buyX + 2, buyY + Math.max(1, (int) ((buyH - 8 * s * 1.8f) / 2)),
                buyW - 4, s * 1.8f, hover ? 0xFFF5EAD0 : 0xFF382B21, true);

        int ingSize = Math.max(8, Math.round(18 * s));
        int gap = Math.max(1, Math.round(2 * s));
        int maxPerRow = Math.max(1, (width - Math.round(16 * s)) / (ingSize + gap));
        int ingY0 = getY() + Math.round(168 * s);
        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack stack = ingredients.get(i);
            int col = i % maxPerRow;
            int row = i / maxPerRow;
            int ix = getX() + Math.round(8 * s) + col * (ingSize + gap);
            int iy = ingY0 + row * (ingSize + gap);
            graphics.pose().pushMatrix();
            graphics.pose().translate(ix, iy);
            graphics.pose().scale(ingSize / 16f, ingSize / 16f);
            graphics.renderItem(stack, 0, 0);
            graphics.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0);
            graphics.pose().popMatrix();
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (clickable) {
            onCraft.onCraftClick(event.buttonInfo().button(),
                    Minecraft.getInstance().hasShiftDown());
        }
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == 0 || info.button() == 1;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (!clickable) {
            return;
        }
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
