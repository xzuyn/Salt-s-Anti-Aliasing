package org.betterLostItems.salts_anti_aliasing.client.config;

/**
 * How much sharpening is applied after the supersampled image is averaged down.
 *
 * <p>Averaging is an anti-aliasing filter, so it loses a little fine detail compared with unfiltered
 * rendering. In a test on a Minecraft-style pixel-art texture, a correct resolve ended up about 8%
 * lower in edge contrast than native rendering. {@link #LOW} is calibrated to win that back;
 * {@link #MEDIUM} goes slightly beyond native crispness and {@link #HIGH} is clearly sharpened.</p>
 *
 * <p>The strengths here must match the {@code Strength} values in the
 * {@code ssaa_resolve_*_sharp_*.json} post effects; a test checks that they do.</p>
 */
public enum SsaaSharpness {
    OFF(0.0f),
    LOW(0.25f),
    MEDIUM(0.5f),
    HIGH(1.0f);

    private final float strength;

    SsaaSharpness(float strength) {
        this.strength = strength;
    }

    /** Value of the shader's {@code Strength} uniform. */
    public float strength() {
        return strength;
    }

    public boolean enabled() {
        return this != OFF;
    }

    public static SsaaSharpness defaultSharpness() {
        return LOW;
    }

    public static SsaaSharpness clamp(SsaaSharpness sharpness) {
        return sharpness == null ? defaultSharpness() : sharpness;
    }
}
