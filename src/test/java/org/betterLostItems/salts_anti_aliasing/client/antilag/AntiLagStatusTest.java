package org.betterLostItems.salts_anti_aliasing.client.antilag;

import org.betterLostItems.salts_anti_aliasing.client.config.SsaaTranslationKeys;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AntiLagStatusTest {
    @Test
    void onlyAConfirmedDeviceIsUsable() {
        for (AntiLagStatus status : AntiLagStatus.values()) {
            assertEquals(status == AntiLagStatus.SUPPORTED, status.usable(), status.name());
        }
    }

    @Test
    void everyStatusThatMeansNothingWillHappenExplainsWhy() {
        assertNotNull(SsaaTranslationKeys.antiLagStatusKey(AntiLagStatus.UNKNOWN));
        assertNotNull(SsaaTranslationKeys.antiLagStatusKey(AntiLagStatus.UNSUPPORTED));
        assertNotNull(SsaaTranslationKeys.antiLagStatusKey(AntiLagStatus.FAILED));
    }

    @Test
    void workingAndPendingStatusesAddNoWarning() {
        assertNull(SsaaTranslationKeys.antiLagStatusKey(AntiLagStatus.SUPPORTED));
        assertNull(SsaaTranslationKeys.antiLagStatusKey(AntiLagStatus.REQUESTED));
    }

    @Test
    void statusKeysAreRegisteredSoTheLangFileTestCoversThem() {
        for (AntiLagStatus status : AntiLagStatus.values()) {
            String key = SsaaTranslationKeys.antiLagStatusKey(status);
            if (key != null) {
                assertTrue(SsaaTranslationKeys.ALL.contains(key), key);
            }
        }
        assertFalse(SsaaTranslationKeys.ALL.isEmpty());
    }
}
