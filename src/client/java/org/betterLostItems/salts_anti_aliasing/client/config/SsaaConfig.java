package org.betterLostItems.salts_anti_aliasing.client.config;

/**
 * Mutable settings persisted to disk: the SSAA level, the filter that downscales it, the sharpening applied to the
 * result, and the independent AMD Anti-Lag switch.
 *
 * <p>{@code antiLag} was added after config version 3 shipped. Older files simply do not contain it, so it
 * reads as its default (Off) without needing a version bump or migration.</p>
 *
 * <p>Releases before 0.2 stored a multi-mode config. Their {@code mode} and
 * {@code ssaaScaleLevel} fields are still read once so that players who used SSAA keep an
 * equivalent level; everything else in an old file is ignored.</p>
 */
public final class SsaaConfig {
    public static final int CURRENT_CONFIG_VERSION = 3;

    public Integer configVersion;
    public SsaaLevel level = SsaaLevel.defaultLevel();
    public SsaaSharpness sharpness = SsaaSharpness.defaultSharpness();
    public SsaaResolveFilter resolveFilter = SsaaResolveFilter.defaultFilter();
    public AntiLagMode antiLag = AntiLagMode.defaultMode();

    // Legacy fields (config version < 3). Read for migration, then cleared so they are not rewritten.
    public String mode;
    public String ssaaScaleLevel;

    public SsaaConfig copy() {
        SsaaConfig copy = new SsaaConfig();
        copy.configVersion = configVersion;
        copy.level = level;
        copy.sharpness = sharpness;
        copy.resolveFilter = resolveFilter;
        copy.antiLag = antiLag;
        copy.mode = mode;
        copy.ssaaScaleLevel = ssaaScaleLevel;
        return copy;
    }

    boolean needsMigration() {
        return configVersion == null || configVersion < CURRENT_CONFIG_VERSION;
    }

    /** Migrates old files, then normalizes values so render code never sees invalid input. */
    public void sanitize() {
        if (needsMigration()) {
            migrateLegacyConfig();
        }
        level = SsaaLevel.clamp(level);
        sharpness = SsaaSharpness.clamp(sharpness);
        resolveFilter = SsaaResolveFilter.clamp(resolveFilter);
        antiLag = AntiLagMode.clamp(antiLag);
        configVersion = CURRENT_CONFIG_VERSION;
        mode = null;
        ssaaScaleLevel = null;
    }

    private void migrateLegacyConfig() {
        level = "SSAA".equals(mode)
                ? SsaaLevel.fromLegacyScaleName(ssaaScaleLevel)
                : SsaaLevel.OFF;
    }
}
