package fr.poubone.att2.client.shop;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Two adjacent, fully wrapped item tooltips, fitted together into the GUI viewport. */
final class ShopComparison {
    private ShopComparison() {}

    static EquipmentSlot slot(ShopOffer offer) {
        return slot(offer.stack(), offer.equipmentType(), offer.translationKey());
    }

    static EquipmentSlot slot(ItemStack stack) {
        return slot(stack, "", "");
    }

    static EquipmentSlot slot(ItemStack stack, String equipmentType, String translationKey) {
        if (stack != null && !stack.isEmpty()) {
            var equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable != null) {
                EquipmentSlot worn = armorEquipment(equippable.slot());
                if (worn != null) return worn;
            }
        }
        return equipmentSlot(armorSlot(equipmentType, translationKey, itemPath(stack)));
    }

    static String armorSlot(String equipmentType, String translationKey, String itemId) {
        String text = normalize(safe(equipmentType) + " " + safe(translationKey) + " " + safe(itemId));
        if (containsAny(text, "helmet", "casque")) return "HEAD";
        if (containsAny(text, "chestplate", "plastron")) return "CHEST";
        if (containsAny(text, "leggings", "jambiere")) return "LEGS";
        if (containsAny(text, "boots", "bottes")) return "FEET";
        return null;
    }

    static boolean shouldShowHint(String armorSlot, boolean held) {
        return armorSlot != null && !held;
    }

    static boolean holdsCompare(boolean boundKeyDown, int boundGlfwKey, boolean leftShiftDown, boolean rightShiftDown) {
        if (boundKeyDown) return true;
        if (boundGlfwKey == GLFW.GLFW_KEY_LEFT_SHIFT || boundGlfwKey == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            return leftShiftDown || rightShiftDown;
        }
        return false;
    }

    static boolean isHeld(long window, int boundKey) {
        boolean boundDown = boundKey != -1 && GLFW.glfwGetKey(window, boundKey) == GLFW.GLFW_PRESS;
        boolean left = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        return holdsCompare(boundDown, boundKey, left, right);
    }

    static List<Component> wornLines(List<Component> equippedTooltip) {
        List<Component> worn = new ArrayList<>();
        worn.add(Component.translatable("att2.ui.equipped").withStyle(ChatFormatting.GOLD));
        if (equippedTooltip == null || equippedTooltip.isEmpty()) {
            worn.add(Component.translatable("att2.ui.unequipped"));
        } else {
            worn.addAll(equippedTooltip);
        }
        return worn;
    }

    static void render(GuiGraphics g, List<Component> offer, ItemStack equipped, int mouseX, int mouseY, int width, int height) {
        var font = Minecraft.getInstance().font;
        List<Component> worn = wornLines(equipped == null || equipped.isEmpty() ? List.of() : ShopTooltips.of(equipped));
        int column = Math.max(80, Math.min(240, (width - 40) / 2));
        List<FormattedCharSequence> left = new ArrayList<>(), right = new ArrayList<>();
        for (Component line : offer) left.addAll(font.split(line, column));
        for (Component line : worn) right.addAll(font.split(line, column));
        int contentHeight = Math.max(left.size(), right.size()) * 10 + 24;
        float scale = Math.min(1f, Math.min((height - 16f) / contentHeight, (width - 16f) / (2 * column + 52)));
        int totalWidth = Math.round((column * 2 + 52) * scale);
        int x = Math.max(8, Math.min(mouseX + 12, width - totalWidth - 8));
        int y = Math.max(8, Math.min(mouseY - 12, height - Math.round(contentHeight * scale) - 8));
        g.pose().pushMatrix();
        g.pose().translate(x + 12 * scale, y + 12 * scale);
        g.pose().scale(scale, scale);
        draw(g, left, 0, column);
        draw(g, right, column + 28, column);
        g.pose().popMatrix();
    }

    private static void draw(GuiGraphics g, List<FormattedCharSequence> lines, int x, int width) {
        TooltipRenderUtil.renderTooltipBackground(g, x, 0, width, lines.size() * 10, null);
        for (int i = 0; i < lines.size(); i++) g.drawString(Minecraft.getInstance().font, lines.get(i), x, i * 10, 0xFFFFFFFF, true);
    }

    private static EquipmentSlot armorEquipment(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD, CHEST, LEGS, FEET -> slot;
            default -> null;
        };
    }

    private static EquipmentSlot equipmentSlot(String name) {
        if (name == null) return null;
        return switch (name) {
            case "HEAD" -> EquipmentSlot.HEAD;
            case "CHEST" -> EquipmentSlot.CHEST;
            case "LEGS" -> EquipmentSlot.LEGS;
            case "FEET" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static String itemPath(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        try {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = Character.toLowerCase(value.charAt(i));
            switch (c) {
                case 'é', 'è', 'ê', 'ë' -> out.append('e');
                case 'à', 'â', 'ä' -> out.append('a');
                case 'ù', 'û', 'ü' -> out.append('u');
                case 'ô', 'ö' -> out.append('o');
                case 'î', 'ï' -> out.append('i');
                case 'ç' -> out.append('c');
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }
}
