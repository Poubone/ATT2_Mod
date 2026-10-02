package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.data.SpellLauncherTracker;
import fr.poubone.att2.client.data.SpellLauncherTracker.LauncherState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Column of spell launchers the player carries. Lives in a movable HUD box:
 * item icon, dark overlay proportional to the remaining cooldown, level badge,
 * golden flash when a spell gets ready. Columns wrap after {@link #PER_COLUMN} icons.
 */
public class SpellBarDisplay {
    private static final int CELL = 18;
    private static final int GAP = 2;
    private static final int PER_COLUMN = 8;

    public static void render(GuiGraphics ctx) {
        List<LauncherState> states = SpellLauncherTracker.states();
        if (states.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();

        HudLayout.Box box = HudLayout.box(HudLayout.SPELLS);
        int columns = Math.max(1, (states.size() + PER_COLUMN - 1) / PER_COLUMN);
        int rows = Math.min(PER_COLUMN, states.size());
        float scale = HudLayout.contentScale(HudLayout.SPELLS, columns * (CELL + GAP), rows * (CELL + GAP));
        int cell = Math.max(12, Math.round(CELL * scale));
        int gap = Math.max(1, Math.round(GAP * scale));
        int icon = Math.max(10, cell - 2);
        int stride = cell + gap;
        int screenW = Math.max(1, mc.getWindow().getGuiScaledWidth());

        int baseY = box.y() + box.h() - cell;
        for (int i = 0; i < states.size(); i++) {
            LauncherState state = states.get(i);
            int row = i % PER_COLUMN;
            int col = i / PER_COLUMN;
            int x = SpellBarLayout.iconX(box.x(), box.w(), cell, screenW, col, stride);
            int y = baseY - row * stride;

            ctx.fill(x - 1, y - 1, x + cell - 1, y + cell - 1, 0x66000000);
            ctx.pose().pushMatrix();
            ctx.pose().translate(x, y);
            float iconScale = icon / 16f;
            ctx.pose().scale(iconScale, iconScale);
            ctx.renderItem(state.stack, 0, 0);
            ctx.pose().popMatrix();

            if (!state.ready()) {
                int coveredHeight = (icon * state.cooldownTenths) / 10;
                ctx.fill(x, y + icon - coveredHeight, x + icon, y + icon, 0xAA000000);
            } else if (state.flashTicks > 0) {
                int alpha = (int) (160f * state.flashTicks / SpellLauncherTracker.FLASH_TICKS);
                ctx.fill(x - 1, y - 1, x + cell - 1, y + cell - 1, (alpha << 24) | 0x00FFD75A);
            }

            if (state.level > 0) {
                String level = String.valueOf(state.level);
                float badge = Math.max(0.6f, 0.75f * scale);
                ctx.pose().pushMatrix();
                ctx.pose().translate(x + cell - 2 - mc.font.width(level) * badge, y + cell - 8);
                ctx.pose().scale(badge, badge);
                ctx.drawString(mc.font, level, 0, 0, 0xFFFFFF55, true);
                ctx.pose().popMatrix();
            }
        }
    }
}
