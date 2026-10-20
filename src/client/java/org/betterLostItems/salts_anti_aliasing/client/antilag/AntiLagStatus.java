package org.betterLostItems.salts_anti_aliasing.client.antilag;

/**
 * What the mod currently knows about AMD Anti-Lag on this machine. Shown to the player in the setting's
 * tooltip so a control that cannot do anything says why instead of silently doing nothing.
 *
 * <p>This type is deliberately free of Minecraft and LWJGL classes so it can be unit tested on its own.</p>
 */
public enum AntiLagStatus {
    /**
     * No Vulkan device has been seen. Either Minecraft is rendering with OpenGL, or the hooks did not
     * apply to this Minecraft build.
     */
    UNKNOWN,
    /** A Vulkan device exists but its driver (or the Vulkan instance) cannot do {@code VK_AMD_anti_lag}. */
    UNSUPPORTED,
    /** The extension was added to device creation; waiting for the first frame to confirm it took effect. */
    REQUESTED,
    /** The device has {@code VK_AMD_anti_lag} enabled and the setting takes effect. */
    SUPPORTED,
    /** Something threw while using Anti-Lag; it stays off for the rest of the session. */
    FAILED;

    /** Whether switching the setting on actually does anything right now. */
    public boolean usable() {
        return this == SUPPORTED;
    }
}
