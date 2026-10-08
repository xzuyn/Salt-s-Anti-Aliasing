package org.betterLostItems.salts_anti_aliasing.client.config;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SsaaResolveFilterTest {
    @Test
    void theDefaultIsTheOriginalAreaWeightedBoxFilter() {
        assertEquals(SsaaResolveFilter.AREA, SsaaResolveFilter.defaultFilter());
        assertEquals(SsaaResolveFilter.AREA, SsaaResolveFilter.clamp(null));
    }

    @Test
    void shaderIndicesAndIdsAreUnique() {
        Set<Integer> indices = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (SsaaResolveFilter filter : SsaaResolveFilter.values()) {
            assertTrue(indices.add(filter.shaderIndex()), filter.name());
            assertTrue(ids.add(filter.id()), filter.name());
        }
    }

    @Test
    void effectNamesCombineFilterAndSharpness() {
        assertEquals("ssaa_resolve_area", SsaaResolveFilter.AREA.effectName(SsaaSharpness.OFF));
        assertEquals("ssaa_resolve_lanczos3_sharp_low", SsaaResolveFilter.LANCZOS3.effectName(SsaaSharpness.LOW));
        assertEquals("ssaa_resolve_catmull_rom_sharp_high", SsaaResolveFilter.CATMULL_ROM.effectName(SsaaSharpness.HIGH));
        assertEquals("ssaa_resolve_area_sharp_low", SsaaResolveFilter.AREA.effectName(null));
    }
}
