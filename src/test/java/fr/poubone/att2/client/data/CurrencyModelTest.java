package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyModelTest {

    @AfterEach
    void tearDown() {
        CurrencyModel.reset();
        Att2Triggers.reset();
        ScoreCache.clear();
    }

    @Test
    void ignoresTheDialogUntilAShopAskedForIt() {
        Component body = Component.translatable("consciousness.currency.esc", "3");
        assertFalse(CurrencyModel.applyIfListening(body));
        assertFalse(ScoreCache.has("ESC"));
    }

    @Test
    void storesEscAndRunePowder() {
        CurrencyModel.request();
        Component body = Component.translatable("consciousness.currency.title")
                .append(Component.translatable("consciousness.currency.chronotons", "42"))
                .append(Component.translatable("consciousness.currency.esc", "3"))
                .append(Component.translatable("consciousness.currency.rune_material", "80"));
        assertTrue(CurrencyModel.applyIfListening(body));
        assertEquals(3, ScoreCache.getOrDefault("ESC", -1));
        assertEquals(-1, ScoreCache.getOrDefault("CHRONOTON", -1));
        assertEquals(80, ScoreCache.getHolder("RUNE_POWDER", "#stock").orElse(-1));
        assertFalse(CurrencyModel.isListening());
    }
}
