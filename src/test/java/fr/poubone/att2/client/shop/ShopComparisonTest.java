package fr.poubone.att2.client.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopComparisonTest {

    @Test
    void armorItemIdsMapToWornSlots() {
        assertEquals("HEAD", ShopComparison.armorSlot("", "", "leather_helmet"));
        assertEquals("CHEST", ShopComparison.armorSlot("", "", "iron_chestplate"));
        assertEquals("LEGS", ShopComparison.armorSlot("", "", "diamond_leggings"));
        assertEquals("FEET", ShopComparison.armorSlot("", "", "netherite_boots"));
    }

    @Test
    void mapEquipmentTypeAndTranslationKeysResolveArmorSlots() {
        assertEquals("HEAD", ShopComparison.armorSlot("helmet", "armor.human.iron.helmet.name", ""));
        assertEquals("CHEST", ShopComparison.armorSlot("chestplate", "", ""));
        assertEquals("LEGS", ShopComparison.armorSlot("", "armor.legion.leggings", ""));
        assertEquals("FEET", ShopComparison.armorSlot("boots", "item.armor.bottes", ""));
        assertEquals("HEAD", ShopComparison.armorSlot("", "att2.armor.casque.fer", ""));
        assertEquals("CHEST", ShopComparison.armorSlot("", "armure.plastron", ""));
        assertEquals("LEGS", ShopComparison.armorSlot("", "armure.jambieres", ""));
    }

    @Test
    void genericArmorAndNonArmorOffersAreNotComparable() {
        assertNull(ShopComparison.armorSlot("armor", "armor.human.set.name", ""));
        assertNull(ShopComparison.armorSlot("meleeWeapon", "weapon.sword.iron", "iron_sword"));
        assertNull(ShopComparison.armorSlot("spell", "att2.spell1.name", "enchanted_book"));
        assertNull(ShopComparison.armorSlot("", "att2.shop.beef", "beef"));
        assertNull(ShopComparison.armorSlot("rune", "", "glowstone_dust"));
        assertNull(ShopComparison.armorSlot("", "", "leather_horse_armor"));
    }

    @Test
    void hintAppearsOnlyForArmorWhileTheCompareKeyIsUp() {
        assertTrue(ShopComparison.shouldShowHint("HEAD", false));
        assertFalse(ShopComparison.shouldShowHint("HEAD", true));
        assertFalse(ShopComparison.shouldShowHint(null, false));
        assertFalse(ShopComparison.shouldShowHint(null, true));
    }

    @Test
    void wornLinesMarkAnEmptyArmorSlot() {
        List<Component> lines = ShopComparison.wornLines(List.of());
        assertEquals("att2.ui.equipped", translationKey(lines.get(0)));
        assertEquals("att2.ui.unequipped", translationKey(lines.get(1)));
        assertEquals(2, lines.size());
    }

    @Test
    void wornLinesKeepTheEquippedItemTooltip() {
        List<Component> lines = ShopComparison.wornLines(List.of(Component.literal("Iron Helmet")));
        assertEquals("att2.ui.equipped", translationKey(lines.get(0)));
        assertEquals("Iron Helmet", lines.get(1).getString());
    }

    @Test
    void eitherShiftCountsWhenTheBoundKeyIsShift() {
        assertTrue(ShopComparison.holdsCompare(false, GLFW.GLFW_KEY_LEFT_SHIFT, false, true));
        assertTrue(ShopComparison.holdsCompare(true, GLFW.GLFW_KEY_C, false, false));
        assertFalse(ShopComparison.holdsCompare(false, GLFW.GLFW_KEY_C, true, true));
    }

    private static String translationKey(Component component) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return component.getString();
    }
}
