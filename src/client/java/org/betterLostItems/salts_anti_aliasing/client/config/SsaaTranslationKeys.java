package org.betterLostItems.salts_anti_aliasing.client.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Every translation key the mod uses, in one Minecraft-free place so the lang file can be checked
 * against it in tests. The UI adapter ({@code ClientText}) turns these into components.
 */
public final class SsaaTranslationKeys {
    public static final String SCREEN_TITLE = "screen.salts_anti_aliasing.config";
    public static final String SECTION_HEADER = "options.salts_anti_aliasing.section";
    /** Slider label with the current level inserted, for example "Supersampling (SSAA): 4x". */
    public static final String SSAA_LABEL = "options.salts_anti_aliasing.ssaa";
    /** Option name without a value, for UIs that show the value separately (Sodium). */
    public static final String SSAA_NAME = "options.salts_anti_aliasing.ssaa.name";
    public static final String SSAA_TOOLTIP = "options.salts_anti_aliasing.ssaa.tooltip";
    public static final String SSAA_OFF = "options.salts_anti_aliasing.ssaa.off";
    public static final String SSAA_WARNING_LABEL = "options.salts_anti_aliasing.ssaa.warning_label";
    public static final String SSAA_WARNING = "options.salts_anti_aliasing.ssaa.warning";
    public static final String SSAA_IMPROVED_TRANSPARENCY = "options.salts_anti_aliasing.ssaa.improved_transparency";

    /** Slider label with the current sharpness inserted, for example "SSAA Sharpness: Low". */
    public static final String SHARPNESS_LABEL = "options.salts_anti_aliasing.sharpness";
    public static final String SHARPNESS_NAME = "options.salts_anti_aliasing.sharpness.name";
    public static final String SHARPNESS_TOOLTIP = "options.salts_anti_aliasing.sharpness.tooltip";
    public static final String SHARPNESS_OFF = "options.salts_anti_aliasing.sharpness.off";
    public static final String SHARPNESS_LOW = "options.salts_anti_aliasing.sharpness.low";
    public static final String SHARPNESS_MEDIUM = "options.salts_anti_aliasing.sharpness.medium";
    public static final String SHARPNESS_HIGH = "options.salts_anti_aliasing.sharpness.high";

    /** Slider label with the current downscale filter inserted, for example "SSAA Filter: Lanczos 3". */
    public static final String FILTER_LABEL = "options.salts_anti_aliasing.filter";
    public static final String FILTER_NAME = "options.salts_anti_aliasing.filter.name";
    public static final String FILTER_TOOLTIP = "options.salts_anti_aliasing.filter.tooltip";
    private static final String FILTER_VALUE_PREFIX = "options.salts_anti_aliasing.filter.";

    public static final List<String> ALL = Stream.concat(Stream.of(
            SCREEN_TITLE,
            SECTION_HEADER,
            SSAA_LABEL,
            SSAA_NAME,
            SSAA_TOOLTIP,
            SSAA_OFF,
            SSAA_WARNING_LABEL,
            SSAA_WARNING,
            SSAA_IMPROVED_TRANSPARENCY,
            SHARPNESS_LABEL,
            SHARPNESS_NAME,
            SHARPNESS_TOOLTIP,
            SHARPNESS_OFF,
            SHARPNESS_LOW,
            SHARPNESS_MEDIUM,
            SHARPNESS_HIGH,
            FILTER_LABEL,
            FILTER_NAME,
            FILTER_TOOLTIP
    ), Arrays.stream(SsaaResolveFilter.values()).map(SsaaTranslationKeys::filterKey)).toList();


    /** Translation key for the name of a sharpness setting. */
    public static String sharpnessKey(SsaaSharpness sharpness) {
        return switch (sharpness) {
            case OFF -> SHARPNESS_OFF;
            case LOW -> SHARPNESS_LOW;
            case MEDIUM -> SHARPNESS_MEDIUM;
            case HIGH -> SHARPNESS_HIGH;
        };
    }

    /** Translation key for the name of a downscale filter. */
    public static String filterKey(SsaaResolveFilter filter) {
        return FILTER_VALUE_PREFIX + filter.id();
    }

    private SsaaTranslationKeys() {
    }
}
