package fr.poubone.att2.client.quest.widget;

import fr.poubone.att2.client.quest.QuestTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.List;
import java.util.function.BooleanSupplier;

/** Small ink label used as a page arrow on the cream pages. */
public class QuestTextButton extends QuestBookWidget {
    private final String label;
    private final Runnable onPress;
    private final BooleanSupplier visibleSupplier;

    public QuestTextButton(int x, int y, int width, int height, String label, Runnable onPress, BooleanSupplier visible) {
        super(x, y, width, height, Component.literal(label));
        this.label = label;
        this.onPress = onPress;
        this.visibleSupplier = visible;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.visible = visibleSupplier.getAsBoolean();
        if (!this.visible) return;
        Font font = Minecraft.getInstance().font;
        int color = isHoveredOrFocused() ? QuestTextures.TITLE : QuestTextures.TEXT;
        int tw = font.width(label);
        graphics.drawString(font, label, getX() + Math.max(0, (width - tw) / 2), getY() + (height - 8) / 2, color, false);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (visibleSupplier.getAsBoolean()) onPress.run();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.15F));
    }

    @Override
    public List<Component> getTooltipLines() {
        return List.of();
    }
}
