package org.betterLostItems.salts_anti_aliasing.client.config;

import org.betterLostItems.salts_anti_aliasing.client.antilag.AntiLagStatus;

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

    /** Heading for settings that are about latency rather than image quality. */
    public static final String LATENCY_SECTION = "options.salts_anti_aliasing.latency_section";

    /** Slider label with the current mode inserted, for example "AMD Anti-Lag: On". */
    public static final String ANTI_LAG_LABEL = "options.salts_anti_aliasing.antilag";
    public static final String ANTI_LAG_NAME = "options.salts_anti_aliasing.antilag.name";
    public static final String ANTI_LAG_TOOLTIP = "options.salts_anti_aliasing.antilag.tooltip";
    public static final String ANTI_LAG_OFF = "options.salts_anti_aliasing.antilag.off";
    public static final String ANTI_LAG_ON = "options.salts_anti_aliasing.antilag.on";
    /** Appended to the tooltip when no Vulkan device has been seen (OpenGL, or the hooks did not apply). */
    public static final String ANTI_LAG_STATUS_UNKNOWN = "options.salts_anti_aliasing.antilag.status.unknown";
    /** Appended to the tooltip when the GPU or driver lacks VK_AMD_anti_lag. */
    public static final String ANTI_LAG_STATUS_UNSUPPORTED = "options.salts_anti_aliasing.antilag.status.unsupported";
    /** Appended to the tooltip after Anti-Lag hit an error and switched itself off. */
    public static final String ANTI_LAG_STATUS_FAILED = "options.salts_anti_aliasing.antilag.status.failed";

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
            FILTER_TOOLTIP,
            LATENCY_SECTION,
            ANTI_LAG_LABEL,
            ANTI_LAG_NAME,
            ANTI_LAG_TOOLTIP,
            ANTI_LAG_OFF,
            ANTI_LAG_ON,
            ANTI_LAG_STATUS_UNKNOWN,
            ANTI_LAG_STATUS_UNSUPPORTED,
            ANTI_LAG_STATUS_FAILED
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

    /** Translation key for the name of an Anti-Lag setting. */
    public static String antiLagKey(AntiLagMode mode) {
        return switch (mode) {
            case OFF -> ANTI_LAG_OFF;
            case ON -> ANTI_LAG_ON;
        };
    }

    /**
     * Translation key for the explanation shown when Anti-Lag cannot do anything right now, or null when it
     * can (or is still being confirmed on the first frame) and no note is needed.
     */
    public static String antiLagStatusKey(AntiLagStatus status) {
        return switch (status) {
            case UNKNOWN -> ANTI_LAG_STATUS_UNKNOWN;
            case UNSUPPORTED -> ANTI_LAG_STATUS_UNSUPPORTED;
            case FAILED -> ANTI_LAG_STATUS_FAILED;
            case REQUESTED, SUPPORTED -> null;
        };
    }

    /** Translation key for the name of a downscale filter. */
    public static String filterKey(SsaaResolveFilter filter) {
        return FILTER_VALUE_PREFIX + filter.id();
    }

    private SsaaTranslationKeys() {
    }
}
