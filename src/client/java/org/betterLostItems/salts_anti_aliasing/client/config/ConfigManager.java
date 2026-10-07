package org.betterLostItems.salts_anti_aliasing.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import org.betterLostItems.salts_anti_aliasing.SaltsAntiAliasing;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads, validates, edits and saves {@link SsaaConfig}. All access is synchronized because the
 * render thread reads the level every frame while the UI thread may change it.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;
    private SsaaConfig config = new SsaaConfig();

    private ConfigManager(Path configPath) {
        this.configPath = configPath;
        config.sanitize();
    }

    /** Config manager backed by {@code <config dir>/salts_anti_aliasing.json}. */
    public static ConfigManager createDefault() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return forPath(configDir.resolve(SaltsAntiAliasing.MOD_ID + ".json"));
    }

    /** Config manager backed by an explicit file; used by tests. */
    public static ConfigManager forPath(Path configPath) {
        return new ConfigManager(configPath);
    }

    /** Loads the config from disk, creating or repairing the file when needed. */
    public synchronized void load() {
        if (Files.notExists(configPath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            SsaaConfig loaded = GSON.fromJson(reader, SsaaConfig.class);
            config = loaded == null ? new SsaaConfig() : loaded;
            boolean migrated = config.needsMigration();
            config.sanitize();
            if (migrated) {
                save();
            }
        } catch (IOException | JsonSyntaxException exception) {
            SaltsAntiAliasing.LOGGER.warn("Falling back to default config after failing to read {}", configPath, exception);
            config = new SsaaConfig();
            config.sanitize();
            save();
        }
    }

    /** Returns a defensive copy. */
    public synchronized SsaaConfig snapshot() {
        return config.copy();
    }

    public synchronized SsaaLevel level() {
        return config.level;
    }

    public synchronized SsaaSharpness sharpness() {
        return config.sharpness;
    }

    /**
     * Changes the sharpening applied to the SSAA result and saves it if it actually changed.
     * @return the sharpness now in effect
     */
    public synchronized SsaaSharpness setSharpness(SsaaSharpness sharpness) {
        SsaaSharpness requested = SsaaSharpness.clamp(sharpness);
        if (requested != config.sharpness) {
            config.sharpness = requested;
            config.sanitize();
            save();
        }
        return config.sharpness;
    }

    /**
     * Changes the SSAA level and saves it if it actually changed.
     * @return the level now in effect
     */
    public synchronized SsaaLevel setLevel(SsaaLevel level) {
        SsaaLevel requested = SsaaLevel.clamp(level);
        if (requested != config.level) {
            config.level = requested;
            config.sanitize();
            save();
        }
        return config.level;
    }

    public synchronized void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            SaltsAntiAliasing.LOGGER.error("Failed to save config to {}", configPath, exception);
        }
    }
}
