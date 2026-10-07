package org.betterLostItems.salts_anti_aliasing.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaTranslationKeys;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SsaaUiResourceTest {
    private static final String LANGUAGE_RESOURCE = "/assets/salts_anti_aliasing/lang/en_us.json";

    @Test
    void everyKeyTheCodeUsesIsTranslated() throws IOException {
        JsonObject translations = translations();

        for (String key : SsaaTranslationKeys.ALL) {
            assertTrue(translations.has(key), () -> "Missing translation key: " + key);
        }
    }

    @Test
    void langFileHasNoStaleKeysFromRemovedFeatures() throws IOException {
        Set<String> inFile = new HashSet<>(translations().keySet());

        assertEquals(new HashSet<>(SsaaTranslationKeys.ALL), inFile);
    }

    private static JsonObject translations() throws IOException {
        try (InputStream stream = SsaaUiResourceTest.class.getResourceAsStream(LANGUAGE_RESOURCE)) {
            assertNotNull(stream, "Missing " + LANGUAGE_RESOURCE);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
