package fr.poubone.att2.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class ArmorDurabilityDisplay {

    public static void render(GuiGraphics ctx) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options == null) return;

        HudLayout.Box box = HudLayout.box(HudLayout.ARMOR);
        float boxScale = HudLayout.contentScale(HudLayout.ARMOR, 100, 28);
        float iconYBase = box.y();
        int startX = box.x();

        ItemStack[] stacks = {
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET)
        };

        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            if (stack.isEmpty() || !stack.isDamageableItem()) continue;

            int max = stack.getMaxDamage();
            int current = max - stack.getDamageValue();
            float ratio = (float) current / max;
            int color = getColor(ratio);

            int spacing = Math.max(16, Math.round(24 * boxScale));
            float scale = 0.9f * boxScale;

            float iconX = startX + i * spacing;
            float iconY = iconYBase;

            ctx.pose().pushMatrix();
            ctx.pose().translate(iconX + 8 * (1 - scale), iconY + 8 * (1 - scale));
            ctx.pose().scale(scale, scale);
            ctx.renderItem(stack, 0, 0);
            ctx.pose().popMatrix();

            String text = String.valueOf(current);
            int textWidth = mc.font.width(text);
            float textX = iconX + 8 - (textWidth * scale / 2f);
            float textY = iconY + 16 * scale + 2;

            ctx.pose().pushMatrix();
            ctx.pose().translate(textX, textY);
            ctx.pose().scale(scale, scale);
            ctx.drawString(mc.font, Component.literal(text), 0, 0, color, true);
            ctx.pose().popMatrix();
        }
    }

    private static int getColor(float ratio) {
        if (ratio > 0.70f) return 0xFF55FF55;
        if (ratio > 0.30f) return 0xFFFFFF00;
        return 0xFFFF5555;
    }
}
