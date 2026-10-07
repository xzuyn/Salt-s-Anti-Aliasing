package org.betterLostItems.salts_anti_aliasing.client.gui;

import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.Component;
import org.betterLostItems.salts_anti_aliasing.client.SaltsAntiAliasingClient;
import org.betterLostItems.salts_anti_aliasing.client.config.ConfigManager;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaTranslationKeys;

/** Adds the SSAA control to an options list (Video Settings, or the Mod Menu screen). */
public final class SsaaVideoSettingsSection {
    private static final int MIN_WIDE_ROW_WIDTH = 150;

    private SsaaVideoSettingsSection() {
    }

    /** Adds a section header followed by the SSAA slider. */
    public static void addTo(OptionsList list) {
        if (SaltsAntiAliasingClient.configOrNull() == null) {
            return;
        }

        list.addHeader(Component.translatable(SsaaTranslationKeys.SECTION_HEADER));
        addControlsTo(list);
    }

    /** Adds just the slider; Mod Menu uses this because its screen title already names the mod. */
    public static void addControlsTo(OptionsList list) {
        ConfigManager config = SaltsAntiAliasingClient.configOrNull();
        if (config == null) {
            return;
        }

        SsaaLevelSliderWidget slider = new SsaaLevelSliderWidget(config);
        slider.setWidth(Math.max(MIN_WIDE_ROW_WIDTH, list.getRowWidth()));
        list.addBig(slider);
    }
}
