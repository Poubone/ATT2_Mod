package fr.poubone.att2.client.gambling;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamblingModelTest {

    @BeforeEach
    @AfterEach
    void isolate() {
        GamblingModel.get().reset();
    }

    @Test
    void disabledMenuLeavesMapMessagesAndStateAlone() {
        GamblingModel model = GamblingModel.get();
        model.onSystemMessage(Component.translatable("matching_game.lock"), true);
        assertTrue(model.hasGrid());
        assertFalse(model.onSystemMessage(Component.translatable("matching_game.select.title"), false));
        assertTrue(model.hasGrid(), "disabled menu must not consume the map catalog");
    }

    @Test
    void openingTheGridListHidesTheActiveBoard() {
        GamblingModel model = GamblingModel.get();
        model.onSystemMessage(Component.translatable("matching_game.lock"), true);
        assertTrue(model.hasGrid(), "a lock cell is an active board");

        model.onSystemMessage(Component.translatable("matching_game.select.title"), true);
        assertFalse(model.hasGrid(), "back to the size list must drop the board");
    }
}
