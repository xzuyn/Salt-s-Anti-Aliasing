package org.betterLostItems.salts_anti_aliasing.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.Component;
import org.betterLostItems.salts_anti_aliasing.client.SaltsAntiAliasingClient;
import org.betterLostItems.salts_anti_aliasing.client.config.ConfigManager;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaLevel;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaResolveFilter;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaSharpness;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaTranslationKeys;

/** Adds the SSAA controls to an options list (Video Settings, or the Mod Menu screen). */
public final class SsaaVideoSettingsSection {
    private static final int MIN_WIDE_ROW_WIDTH = 150;

    private SsaaVideoSettingsSection() {
    }

    /** Adds a section header followed by the SSAA sliders. */
    public static void addTo(OptionsList list) {
        if (SaltsAntiAliasingClient.configOrNull() == null) {
            return;
        }

        list.addHeader(Component.translatable(SsaaTranslationKeys.SECTION_HEADER));
        addControlsTo(list);
    }

    /** Adds just the sliders; Mod Menu uses this because its screen title already names the mod. */
    public static void addControlsTo(OptionsList list) {
        ConfigManager config = SaltsAntiAliasingClient.configOrNull();
        if (config == null) {
            return;
        }

        int width = Math.max(MIN_WIDE_ROW_WIDTH, list.getRowWidth());
        list.addBig(sized(levelSlider(config), width));
        list.addBig(sized(filterSlider(config), width));
        list.addBig(sized(sharpnessSlider(config), width));
    }

    /** Minecraft's Improved Transparency option blocks turning SSAA on or changing its level. */
    public static boolean improvedTransparencyEnabled() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && (Boolean) minecraft.options.improvedTransparency().get();
    }

    private static StepSliderWidget<SsaaLevel> levelSlider(ConfigManager config) {
        return new StepSliderWidget<>(
                SsaaLevel.values(),
                config::level,
                // Turning SSAA Off is always allowed; anything else is refused while Improved Transparency is on.
                requested -> requested.enabled() && improvedTransparencyEnabled()
                        ? config.level()
                        : config.setLevel(requested),
                level -> Component.translatable(SsaaTranslationKeys.SSAA_LABEL, ClientText.label(level)),
                level -> ClientText.tooltip(level, improvedTransparencyEnabled())
        );
    }

    private static StepSliderWidget<SsaaResolveFilter> filterSlider(ConfigManager config) {
        return new StepSliderWidget<>(
                SsaaResolveFilter.values(),
                config::resolveFilter,
                config::setResolveFilter,
                filter -> Component.translatable(SsaaTranslationKeys.FILTER_LABEL, ClientText.filterLabel(filter)),
                ClientText::filterTooltip
        );
    }

    private static StepSliderWidget<SsaaSharpness> sharpnessSlider(ConfigManager config) {
        return new StepSliderWidget<>(
                SsaaSharpness.values(),
                config::sharpness,
                config::setSharpness,
                sharpness -> Component.translatable(SsaaTranslationKeys.SHARPNESS_LABEL, ClientText.sharpnessLabel(sharpness)),
                ClientText::sharpnessTooltip
        );
    }

    private static <T extends StepSliderWidget<?>> T sized(T slider, int width) {
        slider.setWidth(width);
        return slider;
    }
}
