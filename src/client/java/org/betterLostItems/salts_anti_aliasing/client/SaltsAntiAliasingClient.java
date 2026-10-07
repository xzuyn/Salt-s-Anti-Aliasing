package org.betterLostItems.salts_anti_aliasing.client;

import net.fabricmc.api.ClientModInitializer;
import org.betterLostItems.salts_anti_aliasing.SaltsAntiAliasing;
import org.betterLostItems.salts_anti_aliasing.client.config.ConfigManager;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaLevel;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaSharpness;

/** Client entrypoint: loads the saved SSAA level so the renderer and the settings UI can use it. */
public final class SaltsAntiAliasingClient implements ClientModInitializer {
    private static ConfigManager configManager;

    /** The loaded config, or null during very early startup. */
    public static ConfigManager configOrNull() {
        return configManager;
    }

    /** The loaded config; throws if the client has not finished initializing. */
    public static ConfigManager config() {
        if (configManager == null) {
            throw new IllegalStateException("Salt's Anti Aliasing has not been initialized yet");
        }

        return configManager;
    }

    /** The SSAA level to render with right now; {@link SsaaLevel#OFF} until the config has loaded. */
    public static SsaaLevel level() {
        return configManager == null ? SsaaLevel.OFF : configManager.level();
    }

    /** The sharpening to apply to the SSAA result right now; the default until the config has loaded. */
    public static SsaaSharpness sharpness() {
        return configManager == null ? SsaaSharpness.defaultSharpness() : configManager.sharpness();
    }

    @Override
    public void onInitializeClient() {
        configManager = ConfigManager.createDefault();
        configManager.load();
        SaltsAntiAliasing.LOGGER.info("SSAA level: {}, sharpness: {}", configManager.level(), configManager.sharpness());
    }
}
