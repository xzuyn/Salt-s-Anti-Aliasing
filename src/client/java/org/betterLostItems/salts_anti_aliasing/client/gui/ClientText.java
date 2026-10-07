package org.betterLostItems.salts_anti_aliasing.client.gui;

import net.minecraft.network.chat.Component;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaLevel;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaTranslationKeys;

/** Turns {@link SsaaLevel} values and translation keys into Minecraft text components. */
public final class ClientText {
    private ClientText() {
    }

    /** "Off", "2x", "4x", ...; levels above the performance threshold are marked with "(!)". */
    public static Component label(SsaaLevel level) {
        Component label = level.enabled()
                ? Component.literal(level.multiplierLabel())
                : Component.translatable(SsaaTranslationKeys.SSAA_OFF);
        return level.requiresPerformanceWarning()
                ? Component.translatable(SsaaTranslationKeys.SSAA_WARNING_LABEL, label)
                : label;
    }

    /**
     * Tooltip for the SSAA control: what it does, plus a warning for very high levels and a note
     * when Minecraft's Improved Transparency option blocks changing it.
     */
    public static Component tooltip(SsaaLevel level, boolean improvedTransparencyEnabled) {
        Component tooltip = Component.translatable(SsaaTranslationKeys.SSAA_TOOLTIP);
        if (level.requiresPerformanceWarning()) {
            tooltip = tooltip.copy()
                    .append(Component.literal("\n\n"))
                    .append(Component.translatable(
                            SsaaTranslationKeys.SSAA_WARNING,
                            level.multiplierLabel(),
                            String.valueOf(level.samplesPerPixel())
                    ));
        }
        if (improvedTransparencyEnabled) {
            tooltip = tooltip.copy()
                    .append(Component.literal("\n\n"))
                    .append(Component.translatable(SsaaTranslationKeys.SSAA_IMPROVED_TRANSPARENCY));
        }
        return tooltip;
    }

    /** Tooltip without the Improved Transparency note, for UIs that disable the control instead. */
    public static Component tooltip(SsaaLevel level) {
        return tooltip(level, false);
    }
}
