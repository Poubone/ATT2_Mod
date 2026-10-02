package com.lootbeams.managers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TooltipManagerTest {

    @Test
    void pickupParticleWithoutATrackedStackDoesNotCrash() {
        assertDoesNotThrow(() -> TooltipManager.onEntityRenderEnd(null));
        assertDoesNotThrow(() -> TooltipManager.onEntityRender(null));
    }
}
