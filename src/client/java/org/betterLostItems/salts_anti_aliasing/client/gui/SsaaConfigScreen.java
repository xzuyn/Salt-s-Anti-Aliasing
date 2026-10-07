package org.betterLostItems.salts_anti_aliasing.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaTranslationKeys;

/** Mod Menu's configuration screen: the same SSAA slider as Video Settings. */
public final class SsaaConfigScreen extends OptionsSubScreen {
    public SsaaConfigScreen(Screen lastScreen) {
        super(lastScreen, Minecraft.getInstance().options, Component.translatable(SsaaTranslationKeys.SCREEN_TITLE));
    }

    @Override
    protected void addOptions() {
        SsaaVideoSettingsSection.addControlsTo(this.list);
    }
}
