package fr.poubone.att2.client.hud;

import fr.poubone.att2.client.util.ModTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class HudDrawUtils {
    /** Icons are drawn every frame; one shared stack per item avoids allocating a new one each time. */
    private static final Map<Item, ItemStack> ICONS = new IdentityHashMap<>();
    /** Laid-out text per displayed string, so unchanged values aren't re-laid-out every frame. */
    private static final Map<String, FormattedCharSequence> TEXT = new HashMap<>();
    private static final int TEXT_CACHE_LIMIT = 256;
    private static Language textLanguage;

    static ItemStack icon(Item item) {
        return ICONS.computeIfAbsent(item, ItemStack::new);
    }

    static FormattedCharSequence text(String display) {
        // Visual order depends on the language (right-to-left), so start over when it changes
        Language language = Language.getInstance();
        if (language != textLanguage || TEXT.size() >= TEXT_CACHE_LIMIT) {
            TEXT.clear();
            textLanguage = language;
        }
        return TEXT.computeIfAbsent(display, key -> Component.literal(key).getVisualOrderText());
    }

    public static void drawHUDValue(GuiGraphics context, int value, int animationTimer, int maxTicks,
                                    float x, float y, float scale, Item icon, int color, boolean animate) {
        drawHUDValue(context, String.valueOf(value), animationTimer, maxTicks, x, y, scale, icon, color, animate);
    }

    public static void drawHUDValue(GuiGraphics context, String display, int animationTimer, int maxTicks,
                                    float x, float y, float scale, Item icon, int color, boolean animate) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        FormattedCharSequence text = text(display);
        int iconSize = 16;
        float drawScale = scale;
        if (animate && animationTimer > 0) {
            float progress = (float) (maxTicks - animationTimer) / maxTicks;
            drawScale = scale * (1.0f + (float) Math.sin(progress * Math.PI) * 0.3f);
        }

        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(drawScale, drawScale);
        context.renderItem(icon(icon), 0, 0);
        drawOutlinedText(context, font, text, iconSize + 2, 4, color);
        context.pose().popMatrix();
    }

    public static void drawXPHUDValue(GuiGraphics context, String display, float x, float y, float scale, int color) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        FormattedCharSequence text = text(display);

        int ticks = (int) (System.currentTimeMillis() / 100) % 16;
        int frameX = (ticks % 4) * 16;
        int frameY = (ticks / 4) * 16;

        context.pose().pushMatrix();
        context.pose().translate(x, y);
        context.pose().scale(scale, scale);
        context.blit(RenderPipelines.GUI_TEXTURED, ModTextures.XP_ORB,
                0, -2, (float) frameX, (float) frameY,
                16, 16, 16, 16, 64, 64, 0xFF4DFF4D);
        drawOutlinedText(context, font, text, 18, 2, color);
        context.pose().popMatrix();
    }

    private static void drawOutlinedText(GuiGraphics context, Font font, FormattedCharSequence text, int x, int y, int color) {
        int outline = 0xFF000000;
        context.drawString(font, text, x - 1, y, outline, false);
        context.drawString(font, text, x + 1, y, outline, false);
        context.drawString(font, text, x, y - 1, outline, false);
        context.drawString(font, text, x, y + 1, outline, false);
        context.drawString(font, text, x, y, color, false);
    }
}
