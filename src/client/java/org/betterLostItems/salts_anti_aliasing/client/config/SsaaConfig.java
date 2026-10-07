package org.betterLostItems.salts_anti_aliasing.client.config;

/**
 * Mutable settings persisted to disk. Only one real setting exists now: the SSAA level.
 *
 * <p>Releases before 0.2 stored a multi-mode config. Their {@code mode} and
 * {@code ssaaScaleLevel} fields are still read once so that players who used SSAA keep an
 * equivalent level; everything else in an old file is ignored.</p>
 */
public final class SsaaConfig {
    public static final int CURRENT_CONFIG_VERSION = 3;

    public Integer configVersion;
    public SsaaLevel level = SsaaLevel.defaultLevel();

    // Legacy fields (config version < 3). Read for migration, then cleared so they are not rewritten.
    public String mode;
    public String ssaaScaleLevel;

    public SsaaConfig copy() {
        SsaaConfig copy = new SsaaConfig();
        copy.configVersion = configVersion;
        copy.level = level;
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
