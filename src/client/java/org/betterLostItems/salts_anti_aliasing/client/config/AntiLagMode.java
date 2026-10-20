package org.betterLostItems.salts_anti_aliasing.client.config;

/**
 * Whether AMD Anti-Lag ({@code VK_AMD_anti_lag}) is used.
 *
 * <p>This is independent of SSAA. It only has an effect while Minecraft renders with Vulkan on a GPU
 * whose driver exposes the extension; everywhere else it is silently inert. The extension also has a
 * "driver control" mode that follows the Radeon Software setting; it is deliberately not offered
 * because the player is already choosing here.</p>
 *
 * <p>This type is deliberately free of Minecraft and LWJGL classes so it can be unit tested on its own.</p>
 */
public enum AntiLagMode {
    OFF,
    ON;

    public boolean enabled() {
        return this == ON;
    }

    public static AntiLagMode defaultMode() {
        return OFF;
    }

    public static AntiLagMode clamp(AntiLagMode mode) {
        return mode == null ? defaultMode() : mode;
    }
}
