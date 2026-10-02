package fr.poubone.att2.client.shop;

import fr.poubone.att2.client.data.Att2Triggers;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopModelResetTest {

    @BeforeEach
    @AfterEach
    void isolate() {
        ShopModel.get().reset();
        Att2Triggers.reset();
    }

    @Test
    void resetActionClearsDisplayedCatalog() {
        ShopModel model = ShopModel.get();
        model.catalog().addCategory("WEAPONS");
        model.catalog().setDiscount(Component.literal("Current discount : -9%"));

        model.run(new ShopAction(ShopModel.TRIGGER_RESET, Component.literal("Reset"),
                null, ShopAction.Kind.RESET));

        assertTrue(model.catalog().offers().isEmpty(), "reset must drop the previous shop lines");
        assertTrue(model.catalog().categories().contains("WEAPONS"), "forge tabs stay while items reload");
        org.junit.jupiter.api.Assertions.assertEquals("Current discount : -9%", model.catalog().discount().getString());
        assertTrue(model.isCollecting(), "reset should wait for the restocked catalog");
    }
}
