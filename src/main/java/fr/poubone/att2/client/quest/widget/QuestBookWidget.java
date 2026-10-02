package fr.poubone.att2.client.quest.widget;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Base widget of the quest book: silent, accepts left and right clicks, may expose a tooltip. */
public abstract class QuestBookWidget extends AbstractWidget {
    protected QuestBookWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo info) {
        return info.button() == 0 || info.button() == 1;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    /** Lines shown when hovered (empty for none). */
    public List<Component> getTooltipLines() {
        return List.of();
    }
}
