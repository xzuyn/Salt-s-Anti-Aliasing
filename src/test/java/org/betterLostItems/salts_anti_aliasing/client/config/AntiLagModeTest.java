package org.betterLostItems.salts_anti_aliasing.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AntiLagModeTest {
    @Test
    void defaultsToOffSoNothingChangesForExistingPlayers() {
        assertEquals(AntiLagMode.OFF, AntiLagMode.defaultMode());
        assertFalse(AntiLagMode.OFF.enabled());
    }

    @Test
    void onIsTheOnlyEnabledMode() {
        assertTrue(AntiLagMode.ON.enabled());
        assertEquals(2, AntiLagMode.values().length);
    }

    @Test
    void nullFallsBackToTheDefault() {
        assertEquals(AntiLagMode.defaultMode(), AntiLagMode.clamp(null));
        assertEquals(AntiLagMode.ON, AntiLagMode.clamp(AntiLagMode.ON));
    }
}
