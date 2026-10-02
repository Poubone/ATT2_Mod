package fr.poubone.att2.client.rune;

import net.minecraft.ChatFormatting;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuneCardLogicTest {
    @Test void pageCountRoundsUpBySix() {
        assertEquals(1, RuneCardLogic.pageCount(0));
        assertEquals(1, RuneCardLogic.pageCount(1));
        assertEquals(1, RuneCardLogic.pageCount(6));
        assertEquals(2, RuneCardLogic.pageCount(7));
        assertEquals(5, RuneCardLogic.pageCount(27));
    }

    @Test void clampPageStaysInRange() {
        assertEquals(0, RuneCardLogic.clampPage(-1, 3));
        assertEquals(2, RuneCardLogic.clampPage(99, 3));
        assertEquals(0, RuneCardLogic.clampPage(5, 0));
    }

    @Test void gridIsThreeColumns() {
        assertEquals(0, RuneCardLogic.gridCol(0));
        assertEquals(1, RuneCardLogic.gridCol(1));
        assertEquals(2, RuneCardLogic.gridCol(2));
        assertEquals(0, RuneCardLogic.gridCol(3));
        assertEquals(0, RuneCardLogic.gridRow(0));
        assertEquals(1, RuneCardLogic.gridRow(3));
    }

    @Test void stockColorMatchesCodexRules() {
        assertEquals(ChatFormatting.GREEN, RuneCardLogic.stockColor(3, 2, OptionalInt.of(0)));
        assertEquals(ChatFormatting.GOLD, RuneCardLogic.stockColor(1, 3, OptionalInt.of(5)));
        assertEquals(ChatFormatting.RED, RuneCardLogic.stockColor(1, 3, OptionalInt.of(1)));
        assertEquals(ChatFormatting.GRAY, RuneCardLogic.stockColor(1, 3, OptionalInt.empty()));
    }
}
