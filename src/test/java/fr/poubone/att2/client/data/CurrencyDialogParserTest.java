package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyDialogParserTest {

    @Test
    void readsEscPowderAndChronotonsFromCurrencyBody() {
        Component body = Component.translatable("consciousness.currency.title")
                .append("\n")
                .append(Component.translatable("consciousness.currency.chronotons", "120"))
                .append("\n")
                .append(Component.translatable("consciousness.currency.esc", "7"))
                .append("\n")
                .append(Component.translatable("consciousness.currency.rune_material", "350"));

        CurrencyDialogParser.Snapshot snap = CurrencyDialogParser.parse(body).orElseThrow();
        assertEquals(120, snap.chronotons());
        assertEquals(7, snap.esc());
        assertEquals(350, snap.runePowder());
    }

    @Test
    void ignoresUnrelatedBodies() {
        assertTrue(CurrencyDialogParser.parse(Component.translatable("consciousness.stat.title", "4")).isEmpty());
    }
}
