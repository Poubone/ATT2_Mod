package com.lootbeams.render;

import com.lootbeams.config.Configuration;
import com.lootbeams.features.BeamOpacityOnApproach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeamRenderFadeTest {

    private static final float DELTA = 0.001f;

    @Test
    void defaultDistancesKeepBeamOpaqueUntilNearMaxRange() {
        Configuration config = new Configuration();
        assertEquals(48.0f, config.renderDistance, DELTA);
        assertEquals(48.0f, config.changeDistance, DELTA);
        assertEquals(40.0f, config.changeOffset, DELTA);
        assertSame(BeamOpacityOnApproach.FADE_IN, config.beamOpacityOnApproach);
    }

    @Test
    void fadeInStaysOpaqueInsideChangeOffset() {
        Configuration config = fadeConfig();
        assertEquals(1.0f, BeamRender.fadeDistanceAlpha(16.0f, config), DELTA);
        assertEquals(1.0f, BeamRender.fadeDistanceAlpha(40.0f, config), DELTA);
    }

    @Test
    void fadeInReachesZeroAtChangeDistance() {
        Configuration config = fadeConfig();
        assertEquals(0.5f, BeamRender.fadeDistanceAlpha(44.0f, config), DELTA);
        assertEquals(0.0f, BeamRender.fadeDistanceAlpha(48.0f, config), DELTA);
    }

    @Test
    void oldPersistedDistancesStayOpaqueAtTwentyBlocks() {
        Configuration config = new Configuration();
        config.renderDistance = 24.0f;
        config.changeDistance = 16.0f;
        config.changeOffset = 2.0f;
        config.beamOpacityOnApproach = BeamOpacityOnApproach.FADE_IN;

        assertEquals(1.0f, BeamRender.fadeDistanceAlpha(20.0f, config), DELTA);
        assertEquals(0.0f, BeamRender.fadeDistanceAlpha(48.0f, config), DELTA);

        assertTrue(config.migrateDistanceDefaults());
        assertEquals(48.0f, config.renderDistance, DELTA);
        assertEquals(48.0f, config.changeDistance, DELTA);
        assertEquals(40.0f, config.changeOffset, DELTA);
        assertEquals(1.0f, BeamRender.fadeDistanceAlpha(20.0f, config), DELTA);
        assertEquals(0.0f, BeamRender.fadeDistanceAlpha(Math.max(config.changeDistance, config.renderDistance), config), DELTA);
    }

    private static Configuration fadeConfig() {
        Configuration config = new Configuration();
        config.beamOpacityOnApproach = BeamOpacityOnApproach.FADE_IN;
        config.renderDistance = 48.0f;
        config.changeOffset = 40.0f;
        config.changeDistance = 48.0f;
        return config;
    }
}
