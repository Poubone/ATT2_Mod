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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Item tooltips side by side, fitted together into the GUI viewport: the hovered item, then what it would
 * replace. Armor compares with the worn piece, a shield with the offhand, and a weapon with every weapon of
 * its kind in the hotbar (the one in hand first), as many as fit.
 */
public final class ShopComparison {
    /** What an item is compared against. */
    public enum Kind {
        HEAD(EquipmentSlot.HEAD), CHEST(EquipmentSlot.CHEST), LEGS(EquipmentSlot.LEGS), FEET(EquipmentSlot.FEET),
        SHIELD(EquipmentSlot.OFFHAND), MELEE(null), RANGED(null);

        /** The equipment slot holding the item it replaces, or {@code null} for hotbar weapons. */
        final EquipmentSlot slot;

        Kind(EquipmentSlot slot) {
            this.slot = slot;
        }
    }

    /** One item to compare against; an empty stack shows {@code emptyKey} instead of a tooltip. */
    record Target(Component header, ItemStack stack, String emptyKey) {
    }

    /**
     * Space between the hovered box and the stack, between stacked boxes, the margin around everything,
     * and the wrap width range of a box.
     */
    private static final int GAP = 16;
    private static final int STACK_GAP = 10;
    private static final int PADDING = 12;
    private static final int MIN_COLUMN = 110;
    private static final int MAX_COLUMN = 240;

    private ShopComparison() {}

    static Kind kind(ShopOffer offer) {
        return kind(offer.stack(), offer.equipmentType(), offer.translationKey());
    }

    public static Kind kind(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        String type = data == null ? "" : data.copyTag().getString("EquipmentType").orElse("");
        return kind(stack, type, "");
    }

    static Kind kind(ItemStack stack, String equipmentType, String translationKey) {
        if (stack != null && !stack.isEmpty()) {
            var equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable != null) {
                Kind worn = armorKind(equippable.slot());
                if (worn != null) return worn;
            }
        }
        String path = itemPath(stack);
        Kind armor = armorKind(armorSlot(equipmentType, translationKey, path));
        return armor != null ? armor : weaponKind(equipmentType, path);
    }

    static String armorSlot(String equipmentType, String translationKey, String itemId) {
        String text = normalize(safe(equipmentType) + " " + safe(translationKey) + " " + safe(itemId));
        if (containsAny(text, "helmet", "casque")) return "HEAD";
        if (containsAny(text, "chestplate", "plastron")) return "CHEST";
        if (containsAny(text, "leggings", "jambiere")) return "LEGS";
        if (containsAny(text, "boots", "bottes")) return "FEET";
        return null;
    }

    /**
     * Shields and bows go by item id (the map tags shields "rangeWeapon" too); otherwise the map's
     * EquipmentType decides, and only untagged items fall back to vanilla weapon ids, so map tools are left out.
     */
    static Kind weaponKind(String equipmentType, String itemId) {
        String id = safe(itemId);
        if (id.equals("shield")) return Kind.SHIELD;
        if (id.equals("bow") || id.equals("crossbow")) return Kind.RANGED;
        String type = safe(equipmentType);
        if (!type.isEmpty()) {
            return switch (type) {
                case "meleeWeapon" -> Kind.MELEE;
                case "rangeWeapon" -> Kind.RANGED;
                default -> null;
            };
        }
        if (id.equals("trident") || id.equals("mace")) return Kind.MELEE;
        for (String suffix : new String[] {"_sword", "_axe", "_spear"}) {
            if (id.endsWith(suffix)) return Kind.MELEE;
        }
        return null;
    }

    static boolean shouldShowHint(Kind kind, boolean held) {
        return kind != null && !held;
    }

    static boolean holdsCompare(boolean boundKeyDown, int boundGlfwKey, boolean leftShiftDown, boolean rightShiftDown) {
        if (boundKeyDown) return true;
        if (boundGlfwKey == GLFW.GLFW_KEY_LEFT_SHIFT || boundGlfwKey == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            return leftShiftDown || rightShiftDown;
        }
        return false;
    }

    public static boolean isHeld(long window, int boundKey) {
        boolean boundDown = boundKey != -1 && GLFW.glfwGetKey(window, boundKey) == GLFW.GLFW_PRESS;
        boolean left = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        return holdsCompare(boundDown, boundKey, left, right);
    }

    /**
     * What {@code kind} would replace. {@code hovered} is left out by identity, so an inventory item is not
     * compared with itself.
     */
    static List<Target> targets(Player player, Kind kind, ItemStack hovered) {
        List<Target> targets = new ArrayList<>();
        if (kind.slot != null) {
            ItemStack worn = player.getItemBySlot(kind.slot);
            if (isHovered(worn, hovered)) return targets;
            if (kind == Kind.SHIELD && kind(worn) != Kind.SHIELD) worn = ItemStack.EMPTY;
            targets.add(new Target(Component.translatable("att2.ui.equipped"), worn,
                    kind == Kind.SHIELD ? "att2.ui.unequipped_shield" : "att2.ui.unequipped"));
            return targets;
        }
        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        for (int slot : hotbarOrder(selected)) {
            ItemStack stack = inventory.getItem(slot);
            if (isHovered(stack, hovered) || kind(stack) != kind) continue;
            Component header = slot == selected ? Component.translatable("att2.ui.in_hand")
                    : Component.translatable("att2.ui.hotbar_slot", slot + 1);
            targets.add(new Target(header, stack, "att2.ui.unequipped_weapon"));
        }
        if (targets.isEmpty()) {
            targets.add(new Target(Component.translatable("att2.ui.in_hand"), ItemStack.EMPTY, "att2.ui.unequipped_weapon"));
        }
        return targets;
    }

    /** True when {@code hovered} has something to be compared with (a worn piece is not compared with itself). */
    public static boolean hasComparison(Player player, ItemStack hovered) {
        Kind kind = kind(hovered);
        return kind != null && !targets(player, kind, hovered).isEmpty();
    }

    /** Same stack object, ignoring the shared empty stack (an empty slot is never the hovered item). */
    private static boolean isHovered(ItemStack stack, ItemStack hovered) {
        return !hovered.isEmpty() && stack == hovered;
    }

    /** Hotbar slots with the selected one first, then the rest left to right. */
    static int[] hotbarOrder(int selected) {
        int[] order = new int[Inventory.SELECTION_SIZE];
        order[0] = selected;
        for (int slot = 0, i = 1; slot < Inventory.SELECTION_SIZE; slot++) {
            if (slot != selected) order[i++] = slot;
        }
        return order;
    }

    static List<Component> targetLines(Component header, List<Component> tooltip, String emptyKey) {
        List<Component> lines = new ArrayList<>();
        lines.add(header.copy().withStyle(ChatFormatting.GOLD));
        if (tooltip == null || tooltip.isEmpty()) {
            lines.add(Component.translatable(emptyKey));
        } else {
            lines.addAll(tooltip);
        }
        return lines;
    }

    static List<Component> wornLines(List<Component> equippedTooltip) {
        return targetLines(Component.translatable("att2.ui.equipped"), equippedTooltip, "att2.ui.unequipped");
    }

    /**
     * How many comparison boxes fit stacked beside the hovered item's box, given each box's height. At least
     * one is shown; when even that does not fit, the whole comparison is scaled down instead.
     */
    static int fitCount(int[] targetHeights, int screenHeight) {
        int used = 2 * PADDING - STACK_GAP;
        int shown = 0;
        for (int height : targetHeights) {
            used += STACK_GAP + height;
            if (used > screenHeight - 16 && shown > 0) break;
            shown++;
        }
        return shown;
    }

    public static void render(GuiGraphics g, List<Component> hoveredLines, Player player, Kind kind, ItemStack hovered,
                              int mouseX, int mouseY, int width, int height) {
        List<Target> targets = targets(player, kind, hovered);
        if (targets.isEmpty()) {
            g.setTooltipForNextFrame(Minecraft.getInstance().font, hoveredLines, java.util.Optional.empty(), mouseX, mouseY);
            return;
        }
        render(g, hoveredLines, targets, mouseX, mouseY, width, height);
    }

    static void render(GuiGraphics g, List<Component> hoveredLines, List<Target> targets, int mouseX, int mouseY,
                       int width, int height) {
        // Narrow screens wrap long lines sooner, so two boxes always have room side by side.
        int column = Math.max(MIN_COLUMN, Math.min(MAX_COLUMN, (width - 16 - 2 * PADDING - GAP) / 2));
        List<List<Component>> targetLines = new ArrayList<>();
        int[] targetHeights = new int[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            Target target = targets.get(i);
            targetLines.add(targetLines(target.header(),
                    target.stack().isEmpty() ? List.of() : ShopTooltips.of(target.stack()), target.emptyKey()));
            targetHeights[i] = split(targetLines.get(i), column).size() * 10;
        }
        List<FormattedCharSequence> hoveredBox = split(hoveredLines, column);
        int shown = fitCount(targetHeights, height);
        List<List<FormattedCharSequence>> stack = new ArrayList<>();
        for (int i = 0; i < shown; i++) {
            List<Component> lines = targetLines.get(i);
            if (i == shown - 1 && shown < targets.size()) {
                lines = new ArrayList<>(lines);
                lines.add(Component.translatable("att2.ui.compare_more", targets.size() - shown)
                        .withStyle(ChatFormatting.GRAY));
            }
            stack.add(split(lines, column));
        }
        int hoveredWidth = boxWidth(hoveredBox);
        int stackWidth = 0, stackHeight = -STACK_GAP;
        for (List<FormattedCharSequence> box : stack) {
            stackWidth = Math.max(stackWidth, boxWidth(box));
            stackHeight += box.size() * 10 + STACK_GAP;
        }
        int contentWidth = hoveredWidth + GAP + stackWidth + 2 * PADDING;
        int contentHeight = Math.max(hoveredBox.size() * 10, stackHeight) + 2 * PADDING;
        float scale = Math.min(1f, Math.min((height - 16f) / contentHeight, (width - 16f) / contentWidth));
        int totalWidth = Math.round(contentWidth * scale);
        int x = Math.max(8, Math.min(mouseX + 12, width - totalWidth - 8));
        int y = Math.max(8, Math.min(mouseY - 12, height - Math.round(contentHeight * scale) - 8));
        g.nextStratum();
        g.pose().pushMatrix();
        g.pose().translate(x + PADDING * scale, y + PADDING * scale);
        g.pose().scale(scale, scale);
        draw(g, hoveredBox, 0, 0, hoveredWidth);
        int boxY = 0;
        for (List<FormattedCharSequence> box : stack) {
            draw(g, box, hoveredWidth + GAP, boxY, boxWidth(box));
            boxY += box.size() * 10 + STACK_GAP;
        }
        g.pose().popMatrix();
    }

    private static List<FormattedCharSequence> split(List<Component> lines, int column) {
        var font = Minecraft.getInstance().font;
        List<FormattedCharSequence> out = new ArrayList<>();
        for (Component line : lines) out.addAll(font.split(line, column));
        return out;
    }

    private static int boxWidth(List<FormattedCharSequence> lines) {
        var font = Minecraft.getInstance().font;
        int width = 0;
        for (FormattedCharSequence line : lines) width = Math.max(width, font.width(line));
        return Math.max(1, width);
    }

    private static void draw(GuiGraphics g, List<FormattedCharSequence> lines, int x, int y, int width) {
        TooltipRenderUtil.renderTooltipBackground(g, x, y, width, lines.size() * 10, null);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(Minecraft.getInstance().font, lines.get(i), x, y + i * 10, 0xFFFFFFFF, true);
        }
    }

    private static Kind armorKind(EquipmentSlot slot) {
        if (slot == null) return null;
        return switch (slot) {
            case HEAD -> Kind.HEAD;
            case CHEST -> Kind.CHEST;
            case LEGS -> Kind.LEGS;
            case FEET -> Kind.FEET;
            default -> null;
        };
    }

    private static Kind armorKind(String name) {
        if (name == null) return null;
        return switch (name) {
            case "HEAD" -> Kind.HEAD;
            case "CHEST" -> Kind.CHEST;
            case "LEGS" -> Kind.LEGS;
            case "FEET" -> Kind.FEET;
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
