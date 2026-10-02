package fr.poubone.att2.client.shop;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Vanilla item tooltip (name, lore, attributes) used when hovering a shop slot. */
public final class ShopTooltips {
    private ShopTooltips() {
    }

    public static List<Component> of(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        if (stack == null || stack.isEmpty() || client.level == null) return List.of();
        TooltipFlag flag = client.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
        try {
            return new ArrayList<>(stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, flag));
        } catch (RuntimeException e) {
            return List.of(stack.getHoverName());
        }
    }

    public static List<Component> forOffer(ShopOffer offer, Component displayName) {
        ItemStack stack = offer.stack();
        return merge(of(stack), isMapItem(stack), offer.lore(), offer.price(), displayName);
    }

    static List<Component> merge(List<Component> itemTooltip, boolean mapItem, Component lore,
                                 Component price, Component displayName) {
        List<Component> lines = new ArrayList<>();
        boolean itemHasStats = itemTooltip != null && itemTooltip.size() > 1;
        if (mapItem || itemHasStats) {
            if (itemTooltip != null) lines.addAll(itemTooltip);
            if (lines.isEmpty() && displayName != null) lines.add(displayName);
        } else {
            if (displayName != null && !displayName.getString().isBlank()) lines.add(displayName);
            if (usefulLore(lore)) lines.addAll(flattenLore(lore));
            else if (itemTooltip != null && !itemTooltip.isEmpty()) {
                lines.clear();
                lines.addAll(itemTooltip);
            }
        }
        if (price != null && !price.getString().isBlank()
                && lines.stream().noneMatch(c -> c.getString().equals(price.getString()))) {
            lines.add(price);
        }
        return lines;
    }

    static boolean isMapItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.get(DataComponents.LORE) != null) return true;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return false;
        var tag = data.copyTag();
        return tag.getString("EquipmentType").isPresent()
                || tag.getString("Rarity").isPresent()
                || tag.getString("Coin").isPresent();
    }

    static boolean usefulLore(Component lore) {
        if (lore == null) return false;
        String text = lore.getString().strip();
        if (text.isEmpty()) return false;
        String key = ShopModel.translationKey(lore).toLowerCase(Locale.ROOT);
        if (key.contains("hover_event.buy") || key.contains("chronotons.buy") || key.endsWith(".buy")) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return !(lower.contains("click") && (lower.contains("buy") || lower.contains("acheter")));
    }

    static List<Component> flattenLore(Component lore) {
        List<Component> lines = new ArrayList<>();
        if (lore == null) return lines;
        String raw = lore.getString();
        if (raw.contains("\n")) {
            for (String part : raw.split("\n", -1)) {
                if (!part.isBlank()) lines.add(Component.literal(part));
            }
            return lines;
        }
        lines.add(lore);
        return lines;
    }
}
