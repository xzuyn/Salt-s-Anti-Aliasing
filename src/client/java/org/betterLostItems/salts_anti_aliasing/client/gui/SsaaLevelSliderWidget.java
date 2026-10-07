package org.betterLostItems.salts_anti_aliasing.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.betterLostItems.salts_anti_aliasing.client.config.ConfigManager;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaLevel;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaTranslationKeys;

/** Slider that steps through Off, 2x, 4x, 9x, ... and saves each change. */
public final class SsaaLevelSliderWidget extends AbstractSliderButton {
    private static final int ROW_WIDTH = 150;
    private static final int ROW_HEIGHT = 20;

    private final ConfigManager config;

    public SsaaLevelSliderWidget(ConfigManager config) {
        super(0, 0, ROW_WIDTH, ROW_HEIGHT, Component.empty(), normalize(config.level()));
        this.config = config;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        SsaaLevel level = config.level();
        this.setMessage(Component.translatable(SsaaTranslationKeys.SSAA_LABEL, ClientText.label(level)));
        this.setTooltip(Tooltip.create(ClientText.tooltip(level, improvedTransparencyEnabled())));
    }

    @Override
    protected void applyValue() {
        SsaaLevel requested = selectedLevel(this.value);
        // Improved Transparency blocks turning SSAA on or changing it; turning it Off is always allowed.
        if (requested.enabled() && improvedTransparencyEnabled()) {
            requested = config.level();
        }

        this.value = normalize(config.setLevel(requested));
        updateMessage();
    }

    private static boolean improvedTransparencyEnabled() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && (Boolean) minecraft.options.improvedTransparency().get();
    }

    /** Slider position (0..1) of a level. */
    private static double normalize(SsaaLevel level) {
        SsaaLevel[] values = SsaaLevel.values();
        return (double) level.ordinal() / (values.length - 1);
    }

    /** The level nearest to a slider position (0..1). */
    private static SsaaLevel selectedLevel(double sliderValue) {
        SsaaLevel[] values = SsaaLevel.values();
        int maxIndex = values.length - 1;
        int index = (int) Math.round(Math.max(0.0d, Math.min(1.0d, sliderValue)) * maxIndex);
        return values[Math.max(0, Math.min(maxIndex, index))];
    }
}
