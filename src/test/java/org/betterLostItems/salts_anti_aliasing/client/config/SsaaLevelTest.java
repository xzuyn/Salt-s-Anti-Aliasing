package org.betterLostItems.salts_anti_aliasing.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SsaaLevelTest {
    @Test
    void levelsStartAtOffAndGrowInSampleCount() {
        SsaaLevel[] levels = SsaaLevel.values();

        assertEquals(SsaaLevel.OFF, levels[0]);
        assertFalse(SsaaLevel.OFF.enabled());
        for (int i = 1; i < levels.length; i++) {
            assertTrue(levels[i].enabled());
            assertTrue(levels[i].samplesPerPixel() > levels[i - 1].samplesPerPixel());
        }
    }

    @Test
    void levelsAreNamedByTheirSampleCountLikeOtherGames() {
        assertEquals("2x", SsaaLevel.X2.multiplierLabel());
        assertEquals("4x", SsaaLevel.X4.multiplierLabel());
        assertEquals("9x", SsaaLevel.X9.multiplierLabel());
        assertEquals("64x", SsaaLevel.X64.multiplierLabel());
    }

    @Test
    void fourTimesIsTwiceTheWidthAndHeight() {
        assertEquals(4, SsaaLevel.X4.samplesPerPixel());
        assertEquals(2.0f, SsaaLevel.X4.scaleFactor());
        assertEquals(3.0f, SsaaLevel.X9.scaleFactor());
        assertEquals(8.0f, SsaaLevel.X64.scaleFactor());
    }

    @Test
    void perAxisScaleSquaredIsTheSampleCountForEveryLevel() {
        for (SsaaLevel level : SsaaLevel.values()) {
            float scale = level.scaleFactor();
            assertEquals(level.samplesPerPixel(), scale * scale, 1.0e-4f, level.name());
        }
    }

    @Test
    void sceneSizeScalesBothAxesAndNeverDropsBelowOne() {
        assertEquals(1920, SsaaLevel.OFF.sceneSize(1920));
        assertEquals(3840, SsaaLevel.X4.sceneSize(1920));
        assertEquals(2160, SsaaLevel.X4.sceneSize(1080));
        assertEquals(5760, SsaaLevel.X9.sceneSize(1920));
        assertEquals(15360, SsaaLevel.X64.sceneSize(1920));
        assertEquals(2715, SsaaLevel.X2.sceneSize(1920));
        assertEquals(1, SsaaLevel.X64.sceneSize(0));
    }

    @Test
    void warnsOnlyAboveSixteenTimes() {
        assertFalse(SsaaLevel.X16.requiresPerformanceWarning());
        assertTrue(SsaaLevel.X25.requiresPerformanceWarning());
        assertTrue(SsaaLevel.X64.requiresPerformanceWarning());
    }

    @Test
    void nullFallsBackToTheDefaultLevel() {
        assertEquals(SsaaLevel.defaultLevel(), SsaaLevel.clamp(null));
        assertEquals(SsaaLevel.OFF, SsaaLevel.defaultLevel());
    }

    @Test
    void oldPercentagePresetsMapToTheNearestSampleCount() {
        assertEquals(SsaaLevel.X2, SsaaLevel.fromLegacyScaleName("X125"));
        assertEquals(SsaaLevel.X2, SsaaLevel.fromLegacyScaleName("X150"));
        assertEquals(SsaaLevel.X4, SsaaLevel.fromLegacyScaleName("X175"));
        assertEquals(SsaaLevel.X4, SsaaLevel.fromLegacyScaleName("X200"));
        assertEquals(SsaaLevel.X9, SsaaLevel.fromLegacyScaleName("X250"));
        assertEquals(SsaaLevel.X9, SsaaLevel.fromLegacyScaleName("X300"));
        assertEquals(SsaaLevel.X16, SsaaLevel.fromLegacyScaleName("X400"));
        assertEquals(SsaaLevel.X25, SsaaLevel.fromLegacyScaleName("X500"));
        assertEquals(SsaaLevel.X36, SsaaLevel.fromLegacyScaleName("X600"));
        assertEquals(SsaaLevel.X64, SsaaLevel.fromLegacyScaleName("X700"));
        assertEquals(SsaaLevel.X64, SsaaLevel.fromLegacyScaleName("X800"));
    }

    @Test
    void unknownOldPresetNamesUseTheOldDefault() {
        assertEquals(SsaaLevel.X2, SsaaLevel.fromLegacyScaleName(null));
        assertEquals(SsaaLevel.X2, SsaaLevel.fromLegacyScaleName("bogus"));
    }
}
