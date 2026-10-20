package org.betterLostItems.salts_anti_aliasing.client.antilag;

import org.betterLostItems.salts_anti_aliasing.SaltsAntiAliasing;
import org.betterLostItems.salts_anti_aliasing.client.config.AntiLagMode;

import java.util.function.Supplier;

/**
 * The part of AMD Anti-Lag that does not touch LWJGL: shared state, the on/off switch, and the entry
 * points the mixins call.
 *
 * <p>Every Vulkan type lives in {@link VulkanAntiLag}, which is only loaded after a Vulkan present has
 * actually happened. On OpenGL, or on a machine without LWJGL's Vulkan module, it is never touched.</p>
 *
 * <p>Anti-Lag is strictly best effort. Any failure turns it off for the rest of the session and logs
 * once; it never propagates into the game.</p>
 */
public final class AntiLagRuntime {
    /**
     * Debug kill switch: start the game with {@code -Dsalts_anti_aliasing.antilag=false} to stop the mod
     * from touching Vulkan device creation at all.
     */
    public static final String DISABLE_PROPERTY = SaltsAntiAliasing.MOD_ID + ".antilag";

    private static volatile AntiLagStatus status = AntiLagStatus.UNKNOWN;
    private static volatile Supplier<AntiLagMode> modeSource = AntiLagMode::defaultMode;
    private static volatile boolean ready;
    private static volatile boolean failed;

    private AntiLagRuntime() {
    }

    // ---- State shared with the settings UI ------------------------------------------------------------

    public static AntiLagStatus status() {
        return status;
    }

    static void setStatus(AntiLagStatus newStatus) {
        // A failure is final for the session; later detection must not hide it.
        if (!failed) {
            status = newStatus;
        }
    }

    /** Where to read the player's choice from; set by the client entrypoint once the config is loaded. */
    public static void setModeSource(Supplier<AntiLagMode> source) {
        modeSource = source == null ? AntiLagMode::defaultMode : source;
    }

    /** What the player has asked for right now. Read every frame, so changes apply immediately. */
    static AntiLagMode requestedMode() {
        return AntiLagMode.clamp(modeSource.get());
    }

    /** Whether the device in use has Anti-Lag enabled and nothing has failed. */
    static boolean ready() {
        return ready;
    }

    static void setReady(boolean value) {
        ready = value && !failed;
    }

    /** True unless the player asked for the Vulkan device hooks to stay out of the way. */
    static boolean hooksEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty(DISABLE_PROPERTY));
    }

    /** Turns Anti-Lag off for the rest of the session and logs why, once. */
    public static void fail(Throwable cause) {
        if (failed) {
            return;
        }

        failed = true;
        ready = false;
        status = AntiLagStatus.FAILED;
        SaltsAntiAliasing.LOGGER.error("AMD Anti-Lag hit an error and is disabled for this session", cause);
    }

    // ---- Entry points called by the mixins ------------------------------------------------------------

    /**
     * Called before every {@code glfwPollEvents}: the start of a frame, just before input is sampled.
     * Cheap and Vulkan-free until a Vulkan device with Anti-Lag has been seen.
     */
    public static void beforePollEvents() {
        if (!ready) {
            return;
        }

        try {
            VulkanAntiLag.inputStage();
        } catch (Throwable throwable) {
            fail(throwable);
        }
    }
}
