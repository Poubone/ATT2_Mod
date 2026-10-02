package fr.poubone.att2.client.screen;

import fr.poubone.att2.client.data.Att2Triggers;
import fr.poubone.att2.client.data.RepairDialogSuppressor;
import fr.poubone.att2.client.input.KeybindManager;
import fr.poubone.att2.client.util.ModLanguageManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class RepairMenuScreen extends Screen {
    private static final int OPTION_COUNT = 7;
    private int selectedOption = -1;

    public RepairMenuScreen() {
        super(ModLanguageManager.get("screen.repair.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx = width / 2;
        int cy = height / 2;
        int radius = 70;

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);

        double angle = Math.atan2(dy, dx);
        angle = (angle + 2 * Math.PI) % (2 * Math.PI);

        final double OFFSET = 1.7;
        selectedOption = (distance < radius / 2.5)
                ? -1
                : (int) (((angle / (2 * Math.PI)) * OPTION_COUNT + OFFSET) % OPTION_COUNT);

        ItemStack[] itemIcons = getRepairOptionIcons();

        for (int i = 0; i < OPTION_COUNT; i++) {
            double theta = (2 * Math.PI / OPTION_COUNT) * i - Math.PI / 3;
            int tx = (int) (cx + Math.cos(theta) * radius);
            int ty = (int) (cy + Math.sin(theta) * radius);

            ItemStack stack = itemIcons[i];

            context.pose().pushMatrix();
            context.pose().translate(tx - 8, ty - 8);
            float scale = (i == selectedOption) ? 1.2f : 1.0f;
            context.pose().scale(scale, scale);

            if (i == 6) {
                drawCompositeArmorIcon(context);
            } else {
                context.renderItem(stack, 0, 0);
            }

            context.pose().popMatrix();

            if (i == selectedOption) {
                context.setTooltipForNextFrame(font, ModLanguageManager.get("screen.repair.option." + i), mouseX, mouseY);
            }
        }

        context.drawCenteredString(font, ModLanguageManager.get("screen.repair.title"), cx, cy - 8, 0xFFFFFFFF);
    }

    private void drawCompositeArmorIcon(GuiGraphics context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        ItemStack helmet = getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.HEAD));
        ItemStack chestplate = getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.CHEST));
        ItemStack leggings = getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.LEGS));
        ItemStack boots = getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.FEET));
        context.renderItem(helmet, -6, -6);
        context.renderItem(chestplate, 2, -6);
        context.renderItem(leggings, -6, 2);
        context.renderItem(boots, 2, 2);
    }

    private ItemStack[] getRepairOptionIcons() {
        Minecraft client = Minecraft.getInstance();
        ItemStack[] icons = new ItemStack[7];

        if (client.player != null) {
            icons[0] = getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.HEAD));
            icons[1] = getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.CHEST));
            icons[2] = getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.LEGS));
            icons[3] = getItemOrBarrier(client.player.getItemBySlot(EquipmentSlot.FEET));
            icons[4] = getItemOrBarrier(client.player.getOffhandItem());
            icons[5] = getItemOrBarrier(client.player.getMainHandItem());
        }

        icons[6] = new ItemStack(Items.DIAMOND_HELMET);
        return icons;
    }

    private ItemStack getItemOrBarrier(ItemStack stack) {
        return (stack != null && !stack.isEmpty()) ? new ItemStack(stack.getItem()) : new ItemStack(Items.BARRIER);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            executeOption(selectedOption);
            Minecraft.getInstance().setScreen(null);
            KeybindManager.blockRepairUntilRelease();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void tick() {
        if (!isRepairKeyHeld()) {
            onClose();
            executeOption(selectedOption);
        }
    }

    private boolean isRepairKeyHeld() {
        Minecraft client = Minecraft.getInstance();
        int keyCode = KeyBindingHelper.getBoundKeyOf(KeybindManager.getRepairItemMenuKey()).getValue();
        return GLFW.glfwGetKey(client.getWindow().handle(), keyCode) == GLFW.GLFW_PRESS;
    }

    private void executeOption(int index) {
        if (index < 0) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        switch (index) {
            case 0 -> repair(Att2Triggers.REPAIR_HELMET);
            case 1 -> repair(Att2Triggers.REPAIR_CHESTPLATE);
            case 2 -> repair(Att2Triggers.REPAIR_LEGGINGS);
            case 3 -> repair(Att2Triggers.REPAIR_BOOTS);
            case 4 -> repair(Att2Triggers.REPAIR_OFFHAND);
            case 5 -> repair(Att2Triggers.REPAIR_MAINHAND);
            case 6 -> repair(Att2Triggers.REPAIR_ALL);
            default -> {
            }
        }
    }

    private static void repair(int trigger) {
        RepairDialogSuppressor.expect(trigger);
        Att2Triggers.send(trigger);
    }
}
