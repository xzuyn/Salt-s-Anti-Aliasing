package org.betterLostItems.salts_anti_aliasing.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SsaaSharpnessTest {
    @Test
    void strengthsStartAtZeroAndGrow() {
        SsaaSharpness[] values = SsaaSharpness.values();

        assertEquals(SsaaSharpness.OFF, values[0]);
        assertEquals(0.0f, SsaaSharpness.OFF.strength());
        assertFalse(SsaaSharpness.OFF.enabled());
        for (int i = 1; i < values.length; i++) {
            assertTrue(values[i].enabled());
            assertTrue(values[i].strength() > values[i - 1].strength());
            assertTrue(values[i].strength() <= 1.0f);
        }
    }

    @Test
    void nullFallsBackToTheDefaultAndTheDefaultIsGentle() {
        assertEquals(SsaaSharpness.defaultSharpness(), SsaaSharpness.clamp(null));
        assertEquals(SsaaSharpness.LOW, SsaaSharpness.defaultSharpness());
    }
}
