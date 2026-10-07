package org.betterLostItems.salts_anti_aliasing.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

final class SsaaConfigTest {
    private static final Gson GSON = new GsonBuilder().create();

    private static SsaaConfig parse(String json) {
        SsaaConfig config = GSON.fromJson(json, SsaaConfig.class);
        config.sanitize();
        return config;
    }

    @Test
    void freshConfigIsOffAtTheCurrentVersion() {
        SsaaConfig config = new SsaaConfig();
        config.sanitize();

        assertEquals(SsaaLevel.OFF, config.level);
        assertEquals(SsaaConfig.CURRENT_CONFIG_VERSION, config.configVersion);
    }

    @Test
    void oldSsaaConfigKeepsAnEquivalentLevelAndIgnoresEverythingElse() {
        SsaaConfig config = parse("""
                {
                  "configVersion": 2,
                  "mode": "SSAA",
                  "ssaaScaleLevel": "X200",
                  "msaaSampleLevel": "X8",
                  "sharpenStrength": 0.4,
                  "dlssApplicationId": 12345,
                  "fsrBridgePath": "C:/somewhere"
                }
                """);

        assertEquals(SsaaLevel.X4, config.level);
        assertEquals(SsaaConfig.CURRENT_CONFIG_VERSION, config.configVersion);
    }

    @Test
    void oldConfigsForOtherModesBecomeOff() {
        for (String mode : new String[]{"TAA", "FXAA", "MSAA", "SMAA", "DLSS_SUPER_RESOLUTION", "OFF"}) {
            SsaaConfig config = parse("{\"configVersion\": 2, \"mode\": \"" + mode + "\", \"ssaaScaleLevel\": \"X800\"}");
            assertEquals(SsaaLevel.OFF, config.level, mode);
        }
    }

    @Test
    void configWithoutAVersionIsTreatedAsOld() {
        assertEquals(SsaaLevel.X2, parse("{\"mode\": \"SSAA\"}").level);
    }

    @Test
    void migratedConfigDoesNotWriteLegacyFieldsBack() {
        SsaaConfig config = parse("{\"configVersion\": 2, \"mode\": \"SSAA\", \"ssaaScaleLevel\": \"X300\"}");

        JsonObject written = GSON.toJsonTree(config).getAsJsonObject();

        assertFalse(written.has("mode"));
        assertFalse(written.has("ssaaScaleLevel"));
        assertEquals("X9", written.get("level").getAsString());
    }

    @Test
    void currentConfigKeepsItsLevelEvenIfLegacyFieldsAreStillPresent() {
        SsaaConfig config = parse("{\"configVersion\": 3, \"level\": \"X16\", \"mode\": \"SSAA\", \"ssaaScaleLevel\": \"X200\"}");

        assertEquals(SsaaLevel.X16, config.level);
    }

    @Test
    void unknownLevelNamesFallBackToTheDefault() {
        assertEquals(SsaaLevel.OFF, parse("{\"configVersion\": 3, \"level\": \"X1000\"}").level);
    }

    @Test
    void copyIsIndependentOfTheOriginal() {
        SsaaConfig original = new SsaaConfig();
        original.level = SsaaLevel.X9;
        SsaaConfig copy = original.copy();
        original.level = SsaaLevel.X2;

        assertNotSame(original, copy);
        assertEquals(SsaaLevel.X9, copy.level);
        assertNull(copy.mode);
    }
}
