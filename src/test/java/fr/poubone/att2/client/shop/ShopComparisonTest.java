package fr.poubone.att2.client.shop;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    void hintAppearsOnlyForComparableItemsWhileTheCompareKeyIsUp() {
        assertTrue(ShopComparison.shouldShowHint(ShopComparison.Kind.HEAD, false));
        assertTrue(ShopComparison.shouldShowHint(ShopComparison.Kind.MELEE, false));
        assertFalse(ShopComparison.shouldShowHint(ShopComparison.Kind.HEAD, true));
        assertFalse(ShopComparison.shouldShowHint(null, false));
        assertFalse(ShopComparison.shouldShowHint(null, true));
    }

    @Test
    void shieldsAndBowsGoByItemIdEvenThoughTheMapTagsShieldsAsRanged() {
        assertEquals(ShopComparison.Kind.SHIELD, ShopComparison.weaponKind("rangeWeapon", "shield"));
        assertEquals(ShopComparison.Kind.RANGED, ShopComparison.weaponKind("rangeWeapon", "bow"));
        assertEquals(ShopComparison.Kind.RANGED, ShopComparison.weaponKind("rangeWeapon", "crossbow"));
        assertEquals(ShopComparison.Kind.RANGED, ShopComparison.weaponKind("", "bow"));
    }

    @Test
    void theMapsMeleeTagCoversItsScythesAndHammers() {
        assertEquals(ShopComparison.Kind.MELEE, ShopComparison.weaponKind("meleeWeapon", "copper_hoe"));
        assertEquals(ShopComparison.Kind.MELEE, ShopComparison.weaponKind("meleeWeapon", "netherite_shovel"));
        assertEquals(ShopComparison.Kind.MELEE, ShopComparison.weaponKind("meleeWeapon", "iron_pickaxe"));
    }

    @Test
    void untaggedItemsFallBackToVanillaWeaponIdsOnly() {
        assertEquals(ShopComparison.Kind.MELEE, ShopComparison.weaponKind("", "iron_sword"));
        assertEquals(ShopComparison.Kind.MELEE, ShopComparison.weaponKind("", "diamond_axe"));
        assertEquals(ShopComparison.Kind.MELEE, ShopComparison.weaponKind("", "netherite_spear"));
        assertEquals(ShopComparison.Kind.MELEE, ShopComparison.weaponKind("", "trident"));
        assertNull(ShopComparison.weaponKind("", "iron_pickaxe"));
        assertNull(ShopComparison.weaponKind("", "iron_shovel"));
        assertNull(ShopComparison.weaponKind("tool", "iron_sword"));
        assertNull(ShopComparison.weaponKind("potion", "potion"));
        assertNull(ShopComparison.weaponKind("", "bread"));
    }

    @Test
    void hotbarComparisonsStartWithTheItemInHand() {
        assertArrayEquals(new int[] {4, 0, 1, 2, 3, 5, 6, 7, 8}, ShopComparison.hotbarOrder(4));
        assertArrayEquals(new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8}, ShopComparison.hotbarOrder(0));
    }

    @Test
    void stackedComparisonsAreLimitedByScreenHeight() {
        // 24 margin, then each box's height with 10 between boxes; 16 of the screen is kept free.
        assertEquals(4, ShopComparison.fitCount(new int[] {50, 50, 50, 50}, 540));
        assertEquals(3, ShopComparison.fitCount(new int[] {50, 50, 50, 50}, 240));
        assertEquals(1, ShopComparison.fitCount(new int[] {300, 50}, 240));
        assertEquals(1, ShopComparison.fitCount(new int[] {50}, 240));
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
