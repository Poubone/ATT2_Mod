package fr.poubone.att2.client.shop;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.hud.ModToast;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Optional;

/** Shared viewport: rendering and hit testing use the same transform. */
public abstract class ShopPanelScreen extends Screen {
    protected final int panelWidth;
    protected final int panelHeight;
    private ShopViewport viewport = new ShopViewport(1, 0, 0);

    protected ShopPanelScreen(Component title, int panelWidth, int panelHeight) {
        super(title);
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }
    protected final void initViewport() {
        viewport = ShopViewport.fit(width, height, panelWidth, panelHeight, HUDConfig.get().menuSize);
    }

    @Override public final void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!HUDConfig.get().menuBackground) return;
        graphics.blurBeforeThisStratum();
        graphics.fillGradient(0, 0, width, height, 0xB0182024, 0xDB080C10);
    }

    protected abstract void renderPanel(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    @Override public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(viewport.left(), viewport.top());
        graphics.pose().scale(viewport.scale(), viewport.scale());
        renderPanel(graphics, (int) viewport.localX(mouseX), (int) viewport.localY(mouseY), partialTick);
        renderContent(graphics, (int) viewport.localX(mouseX), (int) viewport.localY(mouseY), partialTick);
        graphics.pose().popMatrix();
        // Screen-space notices (above the scaled shop panel).
        ModToast.render(graphics);
    }
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    protected final void showTooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(font, lines, Optional.empty(), viewport.screenX(mouseX), viewport.screenY(mouseY));
    }
    private MouseButtonEvent local(MouseButtonEvent event) {
        return new MouseButtonEvent(viewport.localX(event.x()), viewport.localY(event.y()), event.buttonInfo());
    }
    @Override public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return clickContent(local(event), doubleClick);
    }
    protected boolean clickContent(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) { return super.mouseReleased(local(event)); }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return super.mouseDragged(local(event), dx / viewport.scale(), dy / viewport.scale());
    }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(viewport.localX(x), viewport.localY(y)); }

    protected final ShopActionButton chrome(int x, int y, int width, int height, Component label,
                                           ShopActionButton.Chrome style, ShopType theme, Runnable action) {
        return addRenderableWidget(new ShopActionButton(x, y, width, height,
                new ShopAction(-1, label, null, ShopAction.Kind.OTHER), theme.titleColor, 0xFFFFE6BB,
                action, style, () -> false, theme));
    }
    protected final void panel(GuiGraphics graphics, ShopType theme) {
        fr.poubone.att2.client.shop.ShopSkin.texture(graphics, theme, "panel_main", 0, 0, panelWidth, panelHeight, 1440, 880);
        ShopIdentity.render(graphics, theme, panelWidth, panelHeight);
    }
}
