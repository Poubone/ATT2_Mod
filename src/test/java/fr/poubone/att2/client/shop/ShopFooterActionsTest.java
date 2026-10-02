package fr.poubone.att2.client.shop;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopFooterActionsTest {

    @Test
    void catalogResetDoesNotReorderCanonicalButtons() {
        ShopAction longReset = new ShopAction(ShopModel.TRIGGER_RESET,
                Component.literal("[Force store reset for 250 Chronotons -->]"),
                null, ShopAction.Kind.RESET);
        ShopAction price = action(ShopModel.TRIGGER_MENDING_PRICE, ShopAction.Kind.MENDING_PRICE, "Prix de réparation");
        ShopAction repair = action(ShopModel.TRIGGER_MENDING_REPAIR, ShopAction.Kind.MENDING_REPAIR, "Réparer");
        ShopAction tools = action(ShopModel.TRIGGER_MENDING_TOOLS, ShopAction.Kind.MENDING_TOOLS, "Prix des outils");
        ShopAction reset = action(ShopModel.TRIGGER_RESET, ShopAction.Kind.RESET, "Reset 250 chrono");

        List<ShopAction> afterClear = ShopFooterActions.merge(List.of(price, repair, tools, reset), List.of());
        List<ShopAction> afterResetTellraw = ShopFooterActions.merge(
                List.of(price, repair, tools, reset), List.of(longReset));

        assertEquals(labels(afterClear), labels(afterResetTellraw));
        assertEquals("Reset 250 chrono", afterResetTellraw.get(3).label().getString());
    }

    private static ShopAction action(int trigger, ShopAction.Kind kind, String label) {
        return new ShopAction(trigger, Component.literal(label), null, kind);
    }

    private static List<String> labels(List<ShopAction> actions) {
        return actions.stream().map(action -> action.label().getString()).toList();
    }
}
