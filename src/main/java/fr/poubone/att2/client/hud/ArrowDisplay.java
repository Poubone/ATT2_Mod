package fr.poubone.att2.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArrowDisplay {
    public static void render(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options == null) return;

        int arrowCount = 0;
        for (ItemStack stack : client.player.getInventory().getNonEquipmentItems()) {
            if (stack.getItem() == Items.ARROW) {
                arrowCount += stack.getCount();
            }
        }

        if (arrowCount == 0) return;

        HudLayout.Box box = HudLayout.box(HudLayout.ARROWS);
        float scale = HudLayout.contentScale(HudLayout.ARROWS, 56, 20);
        context.pose().pushMatrix();
        context.pose().translate(box.x(), box.y());
        context.pose().scale(scale, scale);
        context.renderItem(HudDrawUtils.icon(Items.ARROW), 0, 0);
        context.drawString(client.font, Component.literal("x" + arrowCount), 20, 4, 0xFFFFFFFF, true);
        context.pose().popMatrix();
    }
}
