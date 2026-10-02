package fr.poubone.att2.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import fr.poubone.att2.client.data.GrimoireXp;
import fr.poubone.att2.client.data.SpellLauncherTracker;
import fr.poubone.att2.client.data.SpellLevelSelector;
import fr.poubone.att2.client.data.SpellSelectTriggers;
import fr.poubone.att2.client.data.SpellXpRefresh;
import fr.poubone.att2.client.input.KeybindManager;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.OptionalInt;

/**
 * Radial level picker for the spell launcher held in the main hand. Hold the bound key to open,
 * aim at a level, release (or left click) to select: the mod sends the same selectlvl trigger the
 * grimoire's chat links use, then Refresh ({@code obtain}) and the map's drop cycle so the held
 * launcher is rewritten at the new level. The server still enforces which levels are unlocked.
 */
public class SpellLevelRadialScreen extends Screen {
    private static final int RADIUS = 70;

    private final SpellLauncherTracker.LauncherState launcher;
    private final int levelCount;
    private int selectedOption = -1;
    private boolean refreshStarted;

    public SpellLevelRadialScreen(SpellLauncherTracker.LauncherState launcher) {
        super(Component.translatable("att2.spell" + launcher.spellId + ".name"));
        this.launcher = launcher;
        this.levelCount = SpellSelectTriggers.levelCount(launcher.spellId);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        if (!refreshStarted) {
            refreshStarted = true;
            SpellXpRefresh.start(launcher.spellId);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx = width / 2;
        int cy = height / 2;

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        // Angle measured from the top segment, clockwise, so segment i sits at 2*pi*i/levelCount.
        double fromTop = (Math.atan2(dy, dx) + Math.PI / 2 + 2 * Math.PI) % (2 * Math.PI);
        selectedOption = (distance < RADIUS / 2.5)
                ? -1
                : (int) (Math.round(fromTop / (2 * Math.PI) * levelCount) % levelCount);

        OptionalInt unlockedLevel = Minecraft.getInstance().player == null
                ? OptionalInt.empty()
                : GrimoireXp.readUnlockedLevel(Minecraft.getInstance().player, launcher.spellId);

        for (int i = 0; i < levelCount; i++) {
            double theta = (2 * Math.PI / levelCount) * i - Math.PI / 2;
            int tx = (int) (cx + Math.cos(theta) * RADIUS);
            int ty = (int) (cy + Math.sin(theta) * RADIUS);
            int level = i + 1;

            boolean locked = unlockedLevel.isPresent()
                    ? level > unlockedLevel.getAsInt()
                    : SpellLevelSelector.isLocked(launcher.spellId, level);
            boolean selected = i == selectedOption;
            boolean equipped = level == launcher.level;
            context.pose().pushMatrix();
            context.pose().translate(tx, ty - 4);
            context.pose().scale(selected ? 1.35f : 1f, selected ? 1.35f : 1f);
            context.drawCenteredString(font, String.valueOf(level), locked ? -4 : 0, 0,
                    locked ? 0xFFB5B9BF : selected ? 0xFFFFFFFF : equipped ? 0xFF7EE0A0 : 0xFFE8EDF0);
            context.pose().popMatrix();
            if (locked) drawLock(context, tx + 8, ty - 5);

            if (selected && locked) {
                context.setTooltipForNextFrame(font, ModLanguageManager.get("spell_radial.locked"), mouseX, mouseY);
            }
        }

        context.drawCenteredString(font, title, cx, cy - 20, 0xFFFFFFFF);
        context.drawCenteredString(font,
                ModLanguageManager.format("spell_radial.level", "n", launcher.level), cx, cy - 6, 0xFFAAAAAA);
        GrimoireXp.XpState xp = SpellXpRefresh.isRefreshing(launcher.spellId) ? null : xp();
        if (SpellXpRefresh.isRefreshing(launcher.spellId)) {
            context.drawCenteredString(font, "…", cx, cy + 8, 0xFF77DFFF);
        } else if (xp != null) {
            context.drawCenteredString(font, xp.xp() + "/" + xp.nextThreshold(), cx, cy + 8, 0xFF55FF55);
        } else {
            context.drawCenteredString(font, ModLanguageManager.get("spell_radial.xp_hint"), cx, cy + 8, 0xFF888888);
        }
    }

    /** Small pixel lock, readable without relying on a font glyph. */
    private static void drawLock(GuiGraphics context, int x, int y) {
        int color = 0xFFE5A6A6;
        context.fill(x + 2, y, x + 6, y + 1, color);
        context.fill(x + 1, y + 1, x + 2, y + 5, color);
        context.fill(x + 6, y + 1, x + 7, y + 5, color);
        context.fill(x, y + 4, x + 8, y + 10, color);
        context.fill(x + 3, y + 6, x + 5, y + 8, 0xFF51363D);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            validate();
            Minecraft.getInstance().setScreen(null);
            KeybindManager.blockSpellLevelUntilRelease();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void tick() {
        if (!isMenuKeyHeld()) {
            onClose();
            validate();
        }
    }

    private GrimoireXp.XpState xp() {
        Minecraft client = Minecraft.getInstance();
        return client.player == null ? null : GrimoireXp.read(client.player, launcher.spellId);
    }

    private void validate() {
        if (selectedOption < 0) return;
        int level = selectedOption + 1;
        if (level == launcher.level) return;
        SpellXpRefresh.selectWhenReady(launcher.spellId, level);
    }

    /** Unlike RepairMenuScreen this also supports mouse-bound keys (typically middle click). */
    private boolean isMenuKeyHeld() {
        Minecraft client = Minecraft.getInstance();
        InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(KeybindManager.getSpellLevelMenuKey());
        long window = client.getWindow().handle();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(window, key.getValue()) == GLFW.GLFW_PRESS;
    }
}
