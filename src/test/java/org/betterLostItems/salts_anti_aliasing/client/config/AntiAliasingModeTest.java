package org.betterLostItems.salts_anti_aliasing.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AntiAliasingModeTest {
    @Test
    void hidesSharpeningCombinationModes() {
        assertFalse(AntiAliasingMode.implementedModes().contains(AntiAliasingMode.NIS_SHARPEN));
        assertFalse(AntiAliasingMode.implementedModes().contains(AntiAliasingMode.SMAA_NIS_SHARPEN));
        assertFalse(AntiAliasingMode.implementedModes().contains(AntiAliasingMode.FSR1_RCAS));
    }

    @Test
    void exposesSsaaFxaaAsASupersamplingModeWithAnFxaaPass() {
        AntiAliasingMode mode = AntiAliasingMode.SSAA_FXAA;

        assertTrue(AntiAliasingMode.implementedModes().contains(mode));
        assertEquals(mode, AntiAliasingMode.clampImplemented(mode));
        assertTrue(mode.usesSupersampling());
        assertTrue(mode.usesSsaaScaleControl());
        assertTrue(mode.usesFxaaPass());
        assertFalse(AntiAliasingMode.SSAA.usesFxaaPass());
        assertFalse(AntiAliasingMode.FXAA.usesSupersampling());
    }

    @Test
    void migratesLegacySharpeningModesToTheirBaseMode() {
        assertEquals(AntiAliasingMode.OFF, AntiAliasingMode.clampImplemented(AntiAliasingMode.NIS_SHARPEN));
        assertEquals(AntiAliasingMode.SMAA, AntiAliasingMode.clampImplemented(AntiAliasingMode.SMAA_NIS_SHARPEN));
        assertEquals(AntiAliasingMode.FSR1_UPSCALE, AntiAliasingMode.clampImplemented(AntiAliasingMode.FSR1_RCAS));
    }
}
