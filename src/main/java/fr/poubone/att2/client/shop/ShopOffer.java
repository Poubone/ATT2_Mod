package fr.poubone.att2.client.shop;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * One purchasable line parsed from a shop {@code tellraw} (item preview + price + the map's trigger).
 */
public record ShopOffer(ItemStack stack, Component name, Component price, int trigger, String category,
                        String translationKey, Identifier sprite, Component lore) {
    public String equipmentType() {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return "";
        return data.copyTag().getString("EquipmentType").orElse("");
    }

    public String rarityId() {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) return "";
        return data.copyTag().getString("Rarity").orElse("");
    }
}
