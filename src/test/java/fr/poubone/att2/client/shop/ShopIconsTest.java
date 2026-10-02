package fr.poubone.att2.client.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopIconsTest {

    @Test
    void vanillaTellrawSpritesMapToItemIds() {
        assertEquals("tnt", ShopIcons.vanillaItemPath("item/tnt"));
        assertEquals("fire_charge", ShopIcons.vanillaItemPath("item/fire_charge"));
        assertNull(ShopIcons.vanillaItemPath("item/custom/spell/fireball"));
        assertNull(ShopIcons.vanillaItemPath("item/spell/rayon"));
    }

    @Test
    void packSpellSpritesUseTextureFiles() {
        assertTrue(ShopIcons.isPackTexture("item/custom/spell/fireball"));
        assertTrue(ShopIcons.isPackTexture("item/spell/dahal_1"));
        assertFalse(ShopIcons.isPackTexture("item/tnt"));
        assertFalse(ShopIcons.isPackTexture("item/beef"));
    }

    @Test
    void spellIdComesFromTheMapKeyNotTheChatHint() {
        assertEquals(1, ShopIcons.spellId("att2.spell1.name", "item/tnt"));
        assertEquals(10, ShopIcons.spellId("att2.spell10.name", "item/fire_charge"));
        assertEquals(4, ShopIcons.spellId("att2.spell4.launcher", (String) null));
        assertEquals(46, ShopIcons.spellId("", "item/custom/magic_sphere_uncast/gui/spell46"));
        assertEquals(0, ShopIcons.spellId("att2.shop.beef", "item/beef"));
    }

    @Test
    void launcherModelMatchesThePackInventoryItem() {
        assertEquals("spell/1", ShopIcons.launcherModelPath(1));
        assertEquals("spell/46", ShopIcons.launcherModelPath(46));
    }
}
