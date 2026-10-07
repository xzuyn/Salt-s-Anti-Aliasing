package org.betterLostItems.salts_anti_aliasing.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SsaaResolveResourceTest {
    private static final String EFFECT_RESOURCE = "/assets/salts_anti_aliasing/post_effect/ssaa_resolve.json";
    private static final String SHADER_RESOURCE = "/assets/salts_anti_aliasing/shaders/post/ssaa_resolve.fsh";

    @Test
    void effectResolvesTheSceneTargetIntoTheMainTarget() throws IOException {
        JsonObject effect = JsonParser.parseString(read(EFFECT_RESOURCE)).getAsJsonObject();
        JsonArray passes = effect.getAsJsonArray("passes");

        assertEquals(1, passes.size());
        JsonObject pass = passes.get(0).getAsJsonObject();
        assertEquals("salts_anti_aliasing:post/ssaa_resolve", pass.get("fragment_shader").getAsString());
        assertEquals("minecraft:main", pass.get("output").getAsString());

        JsonObject input = pass.getAsJsonArray("inputs").get(0).getAsJsonObject();
        assertEquals("In", input.get("sampler_name").getAsString());
        assertEquals("salts_anti_aliasing:scene_color", input.get("target").getAsString());
    }

    @Test
    void effectDoesNotUseBilinearFilteringBecauseTheShaderFiltersItself() throws IOException {
        assertFalse(read(EFFECT_RESOURCE).contains("\"bilinear\": true"));
    }

    @Test
    void shaderAveragesEverySourceTexelUnderTheOutputPixel() throws IOException {
        String shader = read(SHADER_RESOURCE);

        assertTrue(shader.contains("#version 330"));
        assertTrue(shader.contains("textureSize(InSampler"));
        assertTrue(shader.contains("texelFetch(InSampler"));
        assertTrue(shader.contains("float overlap("));
        assertTrue(shader.contains("sum / total"), "weights must be normalized so brightness is preserved");
    }

    private static String read(String resource) throws IOException {
        try (InputStream stream = SsaaResolveResourceTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
