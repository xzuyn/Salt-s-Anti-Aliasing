package org.betterLostItems.salts_anti_aliasing.client.config;

import java.util.List;

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

    public static final List<String> ALL = List.of(
            SCREEN_TITLE,
            SECTION_HEADER,
            SSAA_LABEL,
            SSAA_NAME,
            SSAA_TOOLTIP,
            SSAA_OFF,
            SSAA_WARNING_LABEL,
            SSAA_WARNING,
            SSAA_IMPROVED_TRANSPARENCY
    );

    private SsaaTranslationKeys() {
    }
}
