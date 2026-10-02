package fr.poubone.att2.client.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrimoireXpTest {
    @Test
    void readsUnlockedLevelsFromResolvedBookDots() {
        Component page = Component.empty()
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(".").withStyle(ChatFormatting.RED))
                .append(Component.literal(".").withStyle(ChatFormatting.GRAY));

        assertEquals(2, GrimoireXp.countUnlockedMarkers(page, 3).orElseThrow());
        assertTrue(GrimoireXp.countUnlockedMarkers(page, 10).isEmpty());
    }
}
