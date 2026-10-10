package fr.poubone.att2.client.update;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Dismissible update notice over the main menu; it never installs files. */
public final class ModUpdateScreen extends Screen {
    private final Screen parent;
    private final ModUpdateChecker.Release release;

    public ModUpdateScreen(Screen parent, ModUpdateChecker.Release release) {
        super(ModLanguageManager.get("update.title"));
        this.parent = parent;
        this.release = release;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(300, width - 24);
        int x = (width - buttonWidth) / 2;
        int y = height / 2;
        addRenderableWidget(Button.builder(ModLanguageManager.get("update.open_page"), button -> {
            Util.getPlatform().openUri(release.pageUrl());
            onClose();
        }).bounds(x, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(ModLanguageManager.get("update.later"), button -> onClose())
                .bounds(x, y + 24, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(ModLanguageManager.get("update.ignore"), button -> {
            HUDConfig.get().ignoredModUpdateVersion = release.version();
            HUDConfig.save();
            onClose();
        }).bounds(x, y + 48, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 58, 0xFFFFFFFF);
        Component message = Component.literal(ModLanguageManager.format("update.available", "version", release.version()));
        int y = height / 2 - 36;
        for (var line : font.split(message, Math.max(100, Math.min(360, width - 24)))) {
            graphics.drawCenteredString(font, line, width / 2, y, 0xFFE0E0E0);
            y += font.lineHeight;
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
