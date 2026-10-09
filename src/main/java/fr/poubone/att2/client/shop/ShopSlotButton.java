package fr.poubone.att2.client.shop;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/** Item preview + price. Hover shows the map item's lore / stats. */
public class ShopSlotButton extends AbstractWidget {
    /** Text sizes per card unit (a card is 312 units wide). */
    static final float NAME_TEXT_SCALE = 2.7f;
    static final float HINT_TEXT_SCALE = 1.8f;
    static final float PRICE_TEXT_SCALE = 2.3f;
    /** Below this many physical pixels per font pixel, text is no longer legible. */
    static final float MIN_READABLE_PIXELS = 0.8f;
    /** Card width in card units. */
    static final int CARD_WIDTH = 312;

    private final ShopOffer offer;
    private final Runnable onPress;
    private final ShopType theme;

    public ShopSlotButton(int x, int y, int width, int height, ShopOffer offer, Runnable onPress) {
        this(x, y, width, height, offer, onPress, ShopType.GENERAL);
    }

    public ShopSlotButton(int x, int y, int width, int height, ShopOffer offer, Runnable onPress, ShopType theme) {
        super(x, y, width, height, offer.name());
        this.offer = offer;
        this.onPress = onPress;
        this.theme = theme;
    }

    public ShopOffer offer() {
        return offer;
    }

    public List<Component> tooltipLines() {
        return ShopTooltips.forOffer(offer, plainCardText(displayName()));
    }

    private static Component plainCardText(Component text) {
        return Component.literal(ChatFormatting.stripFormatting(text.getString()));
    }

    private Component displayName() {
        // Named offers can carry the chat's closing bracket and clickable price as siblings.
        // They belong in the price button, not in the card title or its tooltip heading.
        if (offer.name().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents) {
            return ShopIcons.stripInlineObjects(offer.name().plainCopy().withStyle(offer.name().getStyle()));
        }
        return ShopIcons.stripInlineObjects(offer.name());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hover = isHovered();
        ShopSkin.texture(graphics, theme, hover ? "card_hover" : "card_normal", getX(), getY(), width, height, 312, 252);
        float s = width / 312f;
        int iconSize = Math.max(1, Math.round(90 * s));
        int iconX = getX() + (width - iconSize) / 2;
        int iconY = getY() + Math.round(38 * s);
        int spellId = ShopIcons.spellId(offer);
        Identifier spriteTex = spellId <= 0 && ShopIcons.isPackTexture(offer.sprite())
                ? ShopModel.spriteTexture(offer.sprite()) : null;
        graphics.pose().pushMatrix();
        graphics.pose().translate(iconX, iconY);
        graphics.pose().scale(iconSize / 16f, iconSize / 16f);
        if (spriteTex != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, spriteTex, 0, 0, 0f, 0f, 16, 16, 16, 16);
        } else {
            graphics.renderItem(offer.stack(), 0, 0);
            graphics.renderItemDecorations(Minecraft.getInstance().font, offer.stack(), 0, 0);
        }
        graphics.pose().popMatrix();
        if (spellId > 0 && !ShopIcons.isLauncher(offer.stack())) {
            var launcher = ShopIcons.launcher(spellId);
            int badge = Math.max(8, Math.round(iconSize * 0.42f));
            graphics.pose().pushMatrix();
            graphics.pose().translate(iconX + iconSize - badge + Math.round(2 * s),
                    iconY + iconSize - badge + Math.round(2 * s));
            graphics.pose().scale(badge / 16f, badge / 16f);
            graphics.renderItem(launcher, 0, 0);
            graphics.pose().popMatrix();
        }
        float textScale = s * NAME_TEXT_SCALE;
        ShopTheme.text(graphics, plainCardText(displayName()), getX() + Math.round(14 * s), getY() + Math.round(141 * s),
                width - Math.round(28 * s), textScale, 0xFF382B21, true);
        // The hint repeats what the price button says; it is dropped once too small to read.
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        if (s * HINT_TEXT_SCALE * guiScale >= MIN_READABLE_PIXELS) {
            ShopTheme.text(graphics, Component.translatable("att2.shop.hover_event.buy"),
                    getX() + Math.round(14 * s), getY() + Math.round(175 * s), width - Math.round(28 * s),
                    s * HINT_TEXT_SCALE, 0xFF705E47, true);
        }
        int buyX = getX() + Math.round(18 * s);
        int buyY = getY() + Math.round(202 * s);
        int buyW = Math.round(276 * s), buyH = Math.round(38 * s);
        ShopSkin.texture(graphics, theme, hover ? "button_buy_hover" : "button_buy_normal",
                buyX, buyY, buyW, buyH, 276, 38);
        // Preserve the full price, including its currency or exchange components.
        Component price = offer.price() == null ? Component.literal("?")
                : plainCardText(offer.price());
        float priceScale = s * PRICE_TEXT_SCALE;
        ShopTheme.text(graphics, price, buyX + 2, buyY + Math.max(1, (int) ((buyH - 8 * priceScale) / 2)),
                buyW - 4, priceScale, hover ? 0xFFF5EAD0 : 0xFF382B21, true);
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
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
