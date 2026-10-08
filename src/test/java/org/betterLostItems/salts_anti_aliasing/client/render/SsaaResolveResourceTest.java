package org.betterLostItems.salts_anti_aliasing.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaResolveFilter;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaSharpness;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SsaaResolveResourceTest {
    private static final String EFFECT_RESOURCE = "/assets/salts_anti_aliasing/post_effect/ssaa_resolve_area.json";
    private static final String SHADER_RESOURCE = "/assets/salts_anti_aliasing/shaders/post/ssaa_resolve.fsh";

    @Test
    void everyFilterResolvesTheSceneTargetIntoTheMainTargetWhenSharpnessIsOff() throws IOException {
        for (SsaaResolveFilter filter : SsaaResolveFilter.values()) {
            JsonObject effect = JsonParser.parseString(read(effectResource(filter, SsaaSharpness.OFF))).getAsJsonObject();
            JsonArray passes = effect.getAsJsonArray("passes");

            assertEquals(1, passes.size(), filter.name());
            JsonObject pass = passes.get(0).getAsJsonObject();
            assertEquals("salts_anti_aliasing:post/ssaa_resolve", pass.get("fragment_shader").getAsString());
            assertEquals("minecraft:main", pass.get("output").getAsString());
            assertEquals(filter.shaderIndex(), filterUniform(pass), filter.name());

            JsonObject input = pass.getAsJsonArray("inputs").get(0).getAsJsonObject();
            assertEquals("In", input.get("sampler_name").getAsString());
            assertEquals("salts_anti_aliasing:scene_color", input.get("target").getAsString());
        }
    }

    @Test
    void effectDoesNotUseBilinearFilteringBecauseTheShaderFiltersItself() throws IOException {
        assertFalse(read(EFFECT_RESOURCE).contains("\"bilinear\": true"));
    }

    @Test
    void shaderAveragesEverySourceTexelUnderTheOutputPixel() throws IOException {
        String shader = read(SHADER_RESOURCE);
        assertTrue(shader.contains("uniform SsaaResolveConfig"));
        assertTrue(shader.contains("float Filter;"));

        assertTrue(shader.contains("#version 330"));
        assertTrue(shader.contains("textureSize(InSampler"));
        assertTrue(shader.contains("texelFetch(InSampler"));
        assertTrue(shader.contains("float overlap("));
        assertTrue(shader.contains("sum / total"), "weights must be normalized so brightness is preserved");
    }

    @Test
    void shaderFilterConstantsMatchTheEnumIndices() throws IOException {
        String shader = read(SHADER_RESOURCE);

        for (SsaaResolveFilter filter : SsaaResolveFilter.values()) {
            String constant = "const int FILTER_" + filter.name() + " = " + filter.shaderIndex() + ";";
            assertTrue(shader.contains(constant), "shader is missing: " + constant);
        }
    }

    @Test
    void sharpenedEffectsResolveThenSharpenWithTheEnumsStrength() throws IOException {
        for (SsaaResolveFilter filter : SsaaResolveFilter.values()) {
            for (SsaaSharpness sharpness : SsaaSharpness.values()) {
                if (sharpness.enabled()) {
                    assertSharpenedEffect(filter, sharpness);
                }
            }
        }
    }

    private static void assertSharpenedEffect(SsaaResolveFilter filter, SsaaSharpness sharpness) throws IOException {
        {
            JsonObject effect = JsonParser.parseString(read(effectResource(filter, sharpness))).getAsJsonObject();
            JsonArray passes = effect.getAsJsonArray("passes");
            assertEquals(2, passes.size(), sharpness.name());

            JsonObject resolve = passes.get(0).getAsJsonObject();
            assertEquals("salts_anti_aliasing:post/ssaa_resolve", resolve.get("fragment_shader").getAsString());
            assertEquals("salts_anti_aliasing:scene_color",
                    resolve.getAsJsonArray("inputs").get(0).getAsJsonObject().get("target").getAsString());
            assertEquals("swap", resolve.get("output").getAsString());
            assertEquals(filter.shaderIndex(), filterUniform(resolve), filter.name());

            JsonObject sharpen = passes.get(1).getAsJsonObject();
            assertEquals("salts_anti_aliasing:post/ssaa_sharpen", sharpen.get("fragment_shader").getAsString());
            assertEquals("swap",
                    sharpen.getAsJsonArray("inputs").get(0).getAsJsonObject().get("target").getAsString());
            assertEquals("minecraft:main", sharpen.get("output").getAsString());

            JsonObject uniform = sharpen.getAsJsonObject("uniforms")
                    .getAsJsonArray("SsaaSharpenConfig").get(0).getAsJsonObject();
            assertEquals("Strength", uniform.get("name").getAsString());
            assertEquals(sharpness.strength(), uniform.get("value").getAsFloat(), 1.0e-6f,
                    "effect JSON and SsaaSharpness must agree for " + sharpness.name());
        }
    }

    @Test
    void sharpenShaderUsesTheDeclaredUniformAndStaysInRange() throws IOException {
        String shader = read("/assets/salts_anti_aliasing/shaders/post/ssaa_sharpen.fsh");

        assertTrue(shader.contains("uniform SsaaSharpenConfig"));
        assertTrue(shader.contains("float Strength;"));
        assertTrue(shader.contains("texelFetch(InSampler"));
        assertTrue(shader.contains("clamp(color, 0.0, 1.0)"));
    }

    private static String effectResource(SsaaResolveFilter filter, SsaaSharpness sharpness) {
        return "/assets/salts_anti_aliasing/post_effect/" + filter.effectName(sharpness) + ".json";
    }

    private static int filterUniform(JsonObject resolvePass) {
        JsonObject uniform = resolvePass.getAsJsonObject("uniforms")
                .getAsJsonArray("SsaaResolveConfig").get(0).getAsJsonObject();
        assertEquals("Filter", uniform.get("name").getAsString());
        return Math.round(uniform.get("value").getAsFloat());
    }

    private static String read(String resource) throws IOException {
        try (InputStream stream = SsaaResolveResourceTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
