package org.betterLostItems.salts_anti_aliasing.client;

import net.fabricmc.api.ClientModInitializer;
import org.betterLostItems.salts_anti_aliasing.SaltsAntiAliasing;
import org.betterLostItems.salts_anti_aliasing.client.antilag.AntiLagRuntime;
import org.betterLostItems.salts_anti_aliasing.client.config.AntiLagMode;
import org.betterLostItems.salts_anti_aliasing.client.config.ConfigManager;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaLevel;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaResolveFilter;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaSharpness;

/** Client entrypoint: loads the saved settings so the renderer and the settings UI can use them. */
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

    /** The downscale filter to resolve with right now; the default until the config has loaded. */
    public static SsaaResolveFilter resolveFilter() {
        return configManager == null ? SsaaResolveFilter.defaultFilter() : configManager.resolveFilter();
    }

    /** Whether AMD Anti-Lag is switched on right now; Off until the config has loaded. */
    public static AntiLagMode antiLag() {
        return configManager == null ? AntiLagMode.defaultMode() : configManager.antiLag();
    }

    @Override
    public void onInitializeClient() {
        configManager = ConfigManager.createDefault();
        configManager.load();
        // The Anti-Lag hooks live in library classes and cannot see this class's config on their own.
        AntiLagRuntime.setModeSource(SaltsAntiAliasingClient::antiLag);
        SaltsAntiAliasing.LOGGER.info("SSAA level: {}, filter: {}, sharpness: {}, AMD Anti-Lag: {}",
                configManager.level(), configManager.resolveFilter(), configManager.sharpness(),
                configManager.antiLag());
    }
}
