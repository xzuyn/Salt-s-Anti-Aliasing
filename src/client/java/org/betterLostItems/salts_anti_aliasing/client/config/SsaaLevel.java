package org.betterLostItems.salts_anti_aliasing.client.config;

import java.util.Map;

/**
 * Supersampling level, named the way other games and GPU drivers name it: by how many rendered
 * samples contribute to each output pixel. {@code 4x} renders four times as many pixels as the
 * screen has (twice the width and twice the height), {@code 9x} renders nine times as many, and so
 * on. {@code 2x} is a factor of the square root of two on each axis.
 *
 * <p>This type is deliberately free of Minecraft classes so it can be unit tested on its own.</p>
 */
public enum SsaaLevel {
    OFF(1),
    X2(2),
    X4(4),
    X9(9),
    X16(16),
    X25(25),
    X36(36),
    X64(64);

    /** Levels above this many samples per pixel get a performance and memory warning. */
    private static final int PERFORMANCE_WARNING_THRESHOLD = 16;

    /** Per-axis render scales used by the pre-0.2 percentage slider, keyed by their old enum name. */
    private static final Map<String, Float> LEGACY_PER_AXIS_SCALES = Map.ofEntries(
            Map.entry("X125", 1.25f),
            Map.entry("X150", 1.50f),
            Map.entry("X175", 1.75f),
            Map.entry("X200", 2.00f),
            Map.entry("X250", 2.50f),
            Map.entry("X300", 3.00f),
            Map.entry("X400", 4.00f),
            Map.entry("X500", 5.00f),
            Map.entry("X600", 6.00f),
            Map.entry("X700", 7.00f),
            Map.entry("X800", 8.00f)
    );
    private static final float LEGACY_DEFAULT_PER_AXIS_SCALE = 1.25f;

    private final int samplesPerPixel;

    SsaaLevel(int samplesPerPixel) {
        this.samplesPerPixel = samplesPerPixel;
    }

    /** Number of rendered pixels averaged into each output pixel. */
    public int samplesPerPixel() {
        return samplesPerPixel;
    }

    /** Whether this level renders above native resolution at all. */
    public boolean enabled() {
        return this != OFF;
    }

    /** Render scale applied to each axis: the square root of the sample count. */
    public float scaleFactor() {
        return (float) Math.sqrt(samplesPerPixel);
    }

    /**
     * Size of the supersampled scene along one axis.
     * @param outputSize native size of that axis in pixels
     */
    public int sceneSize(int outputSize) {
        return Math.max(1, Math.round(outputSize * scaleFactor()));
    }

    /** Whether the UI should warn about performance and GPU memory for this level. */
    public boolean requiresPerformanceWarning() {
        return samplesPerPixel > PERFORMANCE_WARNING_THRESHOLD;
    }

    /** Short multiplier text such as {@code "4x"}. Not localized; use the lang file for "Off". */
    public String multiplierLabel() {
        return samplesPerPixel + "x";
    }

    public static SsaaLevel defaultLevel() {
        return OFF;
    }

    public static SsaaLevel clamp(SsaaLevel level) {
        return level == null ? defaultLevel() : level;
    }

    /**
     * Converts a pre-0.2 per-axis percentage preset (for example {@code "X200"}) to the closest
     * sample-count level. Unknown names fall back to the old default of 125%.
     */
    public static SsaaLevel fromLegacyScaleName(String legacyName) {
        // Map.of-style maps reject null keys, so a missing name has to be handled before the lookup.
        Float known = legacyName == null ? null : LEGACY_PER_AXIS_SCALES.get(legacyName);
        float perAxis = known == null ? LEGACY_DEFAULT_PER_AXIS_SCALE : known;
        double legacySamples = (double) perAxis * perAxis;

        SsaaLevel closest = X2;
        double closestDistance = Double.MAX_VALUE;
        for (SsaaLevel level : values()) {
            if (!level.enabled()) {
                continue;
            }
            double distance = Math.abs(Math.log(level.samplesPerPixel / legacySamples));
            if (distance < closestDistance) {
                closest = level;
                closestDistance = distance;
            }
        }
        return closest;
    }
}
