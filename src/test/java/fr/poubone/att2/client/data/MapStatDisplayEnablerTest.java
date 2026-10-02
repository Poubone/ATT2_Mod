package fr.poubone.att2.client.data;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapStatDisplayEnablerTest {

    @AfterEach
    void tearDown() {
        MapStatDisplayEnabler.reset();
        Att2Triggers.reset();
    }

    @Test
    void requestsConscienceAllRunWhenHudWantsStatsButMapBarIsOff() {
        assertEquals(556, MapStatDisplayEnabler.triggerToSend(true, true, false, false));
        assertEquals(Att2Triggers.STAT_DISPLAY_ALL_RUN, MapStatDisplayEnabler.triggerToSend(true, true, false, false));
    }

    @Test
    void switchesDetailedValueModeToPointTotals() {
        assertEquals(558, MapStatDisplayEnabler.triggerToSend(true, true, false, true));
        assertEquals(Att2Triggers.STAT_DISPLAY_COUNT, MapStatDisplayEnabler.triggerToSend(true, true, false, true));
    }

    @Test
    void staysQuietWhenHudIsOffStatsHiddenOrBarAlreadyOn() {
        assertEquals(-1, MapStatDisplayEnabler.triggerToSend(false, true, false, false));
        assertEquals(-1, MapStatDisplayEnabler.triggerToSend(true, false, false, false));
        assertEquals(-1, MapStatDisplayEnabler.triggerToSend(true, true, true, false));
        assertEquals(-1, MapStatDisplayEnabler.triggerToSend(true, true, true, true));
        assertFalse(MapStatDisplayEnabler.shouldRequest(true, false, false));
        assertTrue(MapStatDisplayEnabler.shouldRequest(true, true, false));
    }

    @Test
    void staysQuietUntilTheMapIsReady() {
        assertEquals(-1, MapStatDisplayEnabler.triggerToSend(true, true, false, false, false));
        assertEquals(556, MapStatDisplayEnabler.triggerToSend(true, true, false, false, true));
        assertEquals(558, MapStatDisplayEnabler.triggerToSend(true, true, false, true, true));
        assertEquals(-1, MapStatDisplayEnabler.triggerToSend(true, true, true, false, true));
    }

    @Test
    void swallowsTheNextAttributeDisplayDialogAfterAnAutoSend() {
        Component title = Component.translatable("consciousness.attribute_display.title");
        Component body = Component.translatable("consciousness.attribute_display");
        assertFalse(MapStatDisplayEnabler.applyIfListening(title, body));

        MapStatDisplayEnabler.onSentDisplayTrigger();
        assertTrue(MapStatDisplayEnabler.applyIfListening(title, body));
        assertFalse(MapStatDisplayEnabler.applyIfListening(title, body));
    }

    @Test
    void doesNotSwallowUnrelatedConscienceDialogs() {
        MapStatDisplayEnabler.onSentDisplayTrigger();
        Component title = Component.translatable("consciousness.stat.title", "4");
        assertFalse(MapStatDisplayEnabler.applyIfListening(title, Component.empty()));
        assertTrue(MapStatDisplayEnabler.applyIfListening(
                Component.translatable("consciousness.attribute_display.title"),
                Component.empty()));
    }
}
