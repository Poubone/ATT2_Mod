package fr.poubone.att2.client.screen;

import fr.poubone.att2.client.hud.HUDConfig;
import fr.poubone.att2.client.hud.HudLayout;
import fr.poubone.att2.client.hud.HudRenderer;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Full-screen editor: each HUD widget lives in a box that can be dragged and resized.
 */
public class HudLayoutScreen extends Screen {
    private static final int HANDLE = 8;
    private static final int MIN_W = 32;
    private static final int MIN_H = 16;
    private static final int SNAP = 2;

    private final Screen parent;
    private String selected = HudLayout.XP;
    private String dragging;
    private boolean resizing;
    private int grabDx;
    private int grabDy;
    private int startW;
    private int startH;

    public HudLayoutScreen(Screen parent) {
        super(ModLanguageManager.get("screen.hud_layout.title"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int bw = 110;
        addRenderableWidget(Button.builder(ModLanguageManager.get("screen.hud_layout.reset"), b -> {
            HudLayout.resetAll();
            HUDConfig.save();
        }).bounds(8, height - 28, bw, 20).build());
        addRenderableWidget(Button.builder(ModLanguageManager.get("screen.hud_layout.done"), b -> done())
                .bounds(width - bw - 8, height - 28, bw, 20).build());
    }

    private void done() {
        HUDConfig.save();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, 22, 0xAA000000);
        graphics.fill(0, height - 32, width, height, 0xAA000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        HudRenderer.renderWidgets(graphics, true);
        for (String id : HudLayout.IDS) {
            drawBox(graphics, id, id.equals(selected));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, ModLanguageManager.get("screen.hud_layout.hint"), width / 2, 7, 0xFFE8D5B0);
        HudLayout.Box selectedBox = HudLayout.box(selected);
        var slot = HudLayout.slot(selected);
        float sc = slot.scale;
        if (sc <= 0f) sc = 1f;
        String size = selectedBox.w() + " x " + selectedBox.h() + " · ×" + String.format(java.util.Locale.ROOT, "%.1f", sc);
        if (HudLayout.STATS.equals(selected)) {
            float gap = slot.gap <= 0f ? 1f : slot.gap;
            size += " · " + ModLanguageManager.getString("screen.hud_layout.gap") + " ×"
                    + String.format(java.util.Locale.ROOT, "%.1f", gap);
        }
        graphics.drawCenteredString(font, size, width / 2, height - 22, 0xFFC8B8A0);
    }

    private void drawBox(GuiGraphics graphics, String id, boolean on) {
        HudLayout.Box box = HudLayout.box(id);
        int color = on ? 0xFFE8C86A : 0x88C8B8A0;
        int fill = on ? 0x33E8C86A : 0x22000000;
        graphics.fill(box.x(), box.y(), box.x() + box.w(), box.y() + box.h(), fill);
        drawBorder(graphics, box.x(), box.y(), box.w(), box.h(), color);
        graphics.fill(box.x() + box.w() - HANDLE, box.y() + box.h() - HANDLE,
                box.x() + box.w(), box.y() + box.h(), color);

        String label = ModLanguageManager.getString(HudLayout.labelKey(id));
        int tw = font.width(label);
        int lx = box.x() + Math.max(2, (box.w() - tw) / 2);
        int ly = Math.max(box.y() - 10, 24);
        graphics.drawString(font, label, lx, ly, color, false);
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        int mx = (int) event.x();
        int my = (int) event.y();
        for (int i = HudLayout.IDS.length - 1; i >= 0; i--) {
            String id = HudLayout.IDS[i];
            HudLayout.Box box = HudLayout.box(id);
            if (!contains(box, mx, my)) continue;
            selected = id;
            if (mx >= box.x() + box.w() - HANDLE && my >= box.y() + box.h() - HANDLE) {
                resizing = true;
                dragging = id;
                grabDx = mx;
                grabDy = my;
                startW = box.w();
                startH = box.h();
            } else {
                resizing = false;
                dragging = id;
                grabDx = mx - box.x();
                grabDy = my - box.y();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging == null) return super.mouseDragged(event, dragX, dragY);
        int mx = (int) event.x();
        int my = (int) event.y();
        HudLayout.Box box = HudLayout.box(dragging);
        if (resizing) {
            int w = snap(Mth.clamp(startW + (mx - grabDx), MIN_W, width - box.x()));
            int h = snap(Mth.clamp(startH + (my - grabDy), MIN_H, height - box.y()));
            HudLayout.writePixels(dragging, box.x(), box.y(), w, h);
        } else {
            int maxX = width - box.w();
            int maxY = height - box.h();
            int x = HudLayout.snapToEdge(Mth.clamp(mx - grabDx, 0, maxX), 0, maxX, SNAP);
            int y = HudLayout.snapToEdge(Mth.clamp(my - grabDy, 0, maxY), 0, maxY, SNAP);
            HudLayout.writePixels(dragging, x, y, box.w(), box.h());
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        String id = selected;
        HudLayout.Box box = HudLayout.box(id);
        if (!contains(box, mx, my)) {
            id = null;
            for (int i = HudLayout.IDS.length - 1; i >= 0; i--) {
                String candidate = HudLayout.IDS[i];
                if (contains(HudLayout.box(candidate), mx, my)) {
                    id = candidate;
                    selected = candidate;
                    break;
                }
            }
            if (id == null) return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        if (deltaY == 0) return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        if (HudLayout.STATS.equals(id) && Minecraft.getInstance().hasShiftDown()) {
            float gapDelta = deltaY > 0 ? HudLayout.GAP_STEP : -HudLayout.GAP_STEP;
            HudLayout.adjustGap(id, gapDelta);
        } else {
            float delta = deltaY > 0 ? HudLayout.SCALE_STEP : -HudLayout.SCALE_STEP;
            HudLayout.adjustScale(id, delta);
        }
        HUDConfig.save();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            resizing = false;
            HUDConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            done();
            return true;
        }
        int step = event.hasShiftDown() ? 8 : 2;
        HudLayout.Box box = HudLayout.box(selected);
        int x = box.x();
        int y = box.y();
        int w = box.w();
        int h = box.h();
        boolean moved = switch (event.key()) {
            case GLFW.GLFW_KEY_LEFT -> {
                x = Mth.clamp(x - step, 0, width - w);
                yield true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                x = Mth.clamp(x + step, 0, width - w);
                yield true;
            }
            case GLFW.GLFW_KEY_UP -> {
                y = Mth.clamp(y - step, 0, height - h);
                yield true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                y = Mth.clamp(y + step, 0, height - h);
                yield true;
            }
            default -> false;
        };
        if (moved) {
            HudLayout.writePixels(selected, x, y, w, h);
            HUDConfig.save();
            return true;
        }
        return super.keyPressed(event);
    }

    private static int snap(int value) {
        return (value / SNAP) * SNAP;
    }

    private static boolean contains(HudLayout.Box box, int x, int y) {
        return x >= box.x() && x <= box.x() + box.w() && y >= box.y() && y <= box.y() + box.h();
    }
}
