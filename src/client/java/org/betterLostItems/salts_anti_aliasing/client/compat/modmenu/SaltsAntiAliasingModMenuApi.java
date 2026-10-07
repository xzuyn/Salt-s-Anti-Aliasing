package org.betterLostItems.salts_anti_aliasing.client.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.betterLostItems.salts_anti_aliasing.client.gui.SsaaConfigScreen;

/** Registers the SSAA settings screen with Mod Menu, which is an optional dependency. */
public final class SaltsAntiAliasingModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SsaaConfigScreen::new;
    }
}
