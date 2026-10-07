package org.betterLostItems.salts_anti_aliasing.client.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConfigManagerTest {
    @TempDir
    Path directory;

    private ConfigManager manager(String fileName) {
        return ConfigManager.forPath(directory.resolve(fileName));
    }

    @Test
    void missingFileIsCreatedWithDefaults() {
        ConfigManager manager = manager("missing.json");

        manager.load();

        assertEquals(SsaaLevel.OFF, manager.level());
        assertTrue(Files.exists(directory.resolve("missing.json")));
    }

    @Test
    void levelSurvivesARestart() {
        ConfigManager first = manager("round_trip.json");
        first.load();
        assertEquals(SsaaLevel.X9, first.setLevel(SsaaLevel.X9));

        ConfigManager second = manager("round_trip.json");
        second.load();

        assertEquals(SsaaLevel.X9, second.level());
    }

    @Test
    void settingNullFallsBackToTheDefault() {
        ConfigManager manager = manager("null.json");
        manager.load();
        manager.setLevel(SsaaLevel.X4);

        assertEquals(SsaaLevel.OFF, manager.setLevel(null));
    }

    @Test
    void unchangedLevelDoesNotRewriteTheFile() throws IOException {
        ConfigManager manager = manager("unchanged.json");
        manager.load();
        manager.setLevel(SsaaLevel.X4);
        Path file = directory.resolve("unchanged.json");
        Files.writeString(file, "SENTINEL");

        manager.setLevel(SsaaLevel.X4);

        assertEquals("SENTINEL", Files.readString(file));
    }

    @Test
    void corruptFileFallsBackToDefaultsAndIsRepaired() throws IOException {
        Path file = directory.resolve("corrupt.json");
        Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);
        ConfigManager manager = manager("corrupt.json");

        manager.load();

        assertEquals(SsaaLevel.OFF, manager.level());
        assertTrue(Files.readString(file).contains("\"level\""));
    }

    @Test
    void oldMultiModeFileIsMigratedAndRewritten() throws IOException {
        Path file = directory.resolve("legacy.json");
        Files.writeString(file, """
                {
                  "configVersion": 2,
                  "mode": "SSAA",
                  "ssaaScaleLevel": "X300",
                  "sharpenStrength": 0.5
                }
                """, StandardCharsets.UTF_8);
        ConfigManager manager = manager("legacy.json");

        manager.load();

        assertEquals(SsaaLevel.X9, manager.level());
        String rewritten = Files.readString(file);
        assertTrue(rewritten.contains("\"level\": \"X9\""));
        assertFalse(rewritten.contains("\"mode\""));
        assertFalse(rewritten.contains("sharpenStrength"));
    }
}
