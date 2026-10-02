package fr.poubone.att2.client.renderer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ItemRarityTest {

    @Test
    void mythicRgbIsOriginalPaleWhite() {
        assertEquals(0xF5F3FF, ItemRarity.MYT.rgb);
    }

    @Test
    void ultimateRgbMatchesMinecraftGreen() {
        assertEquals(0x55FF55, ItemRarity.ULT.rgb);
    }

    @Test
    void specialRgbStaysUnchanged() {
        assertEquals(0xF472B6, ItemRarity.SPE.rgb);
    }

    @Test
    void fromIdResolvesUltAndMyt() {
        assertSame(ItemRarity.ULT, ItemRarity.fromId("ult"));
        assertSame(ItemRarity.MYT, ItemRarity.fromId("myt"));
    }
}
