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
 * {@code ssaa_resolve_sharp_*.json} post effects; a test checks that they do.</p>
 */
public enum SsaaSharpness {
    OFF(0.0f, "ssaa_resolve"),
    LOW(0.25f, "ssaa_resolve_sharp_low"),
    MEDIUM(0.5f, "ssaa_resolve_sharp_medium"),
    HIGH(1.0f, "ssaa_resolve_sharp_high");

    private final float strength;
    private final String effectName;

    SsaaSharpness(float strength, String effectName) {
        this.strength = strength;
        this.effectName = effectName;
    }

    /** Value of the shader's {@code Strength} uniform. */
    public float strength() {
        return strength;
    }

    public boolean enabled() {
        return this != OFF;
    }

    /** Name of the post effect (under {@code post_effect/}) that resolves and sharpens at this setting. */
    public String effectName() {
        return effectName;
    }

    public static SsaaSharpness defaultSharpness() {
        return LOW;
    }

    public static SsaaSharpness clamp(SsaaSharpness sharpness) {
        return sharpness == null ? defaultSharpness() : sharpness;
    }
}
