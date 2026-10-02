package fr.poubone.att2.client.miner;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinerPriceParserTest {

    @Test
    void readsChronotonPriceNextToTheBuyTrigger() {
        Component line = Component.literal(" - | GAL | ")
                .append(Component.literal("[42 Chronotons]").withStyle(style ->
                        style.withClickEvent(new ClickEvent.RunCommand("trigger ScoreTrigger set 3495"))));

        Map<Integer, Integer> prices = MinerPriceParser.parse(line);
        assertEquals(42, prices.get(3495));
    }

    @Test
    void readsAResolvedScoreNestedUnderTheBuyClick() {
        Component clickable = Component.literal(" [")
                .withStyle(style -> style.withClickEvent(
                        new ClickEvent.RunCommand("/trigger ScoreTrigger set 3495")))
                .append(Component.literal("50"))
                .append(Component.literal(" Chronotons]"));
        Component line = Component.literal(" - | GAL | ").append(clickable);

        assertEquals(50, MinerPriceParser.parse(line).get(3495));
    }

    @Test
    void ignoresLinesWithoutABuyTrigger() {
        Component line = Component.literal("[42 Chronotons]").withStyle(style ->
                style.withClickEvent(new ClickEvent.RunCommand("trigger ScoreTrigger set 3491")));
        assertTrue(MinerPriceParser.parse(line).isEmpty());
    }
}
