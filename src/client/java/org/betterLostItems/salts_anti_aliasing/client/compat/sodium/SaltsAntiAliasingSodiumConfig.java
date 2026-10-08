package org.betterLostItems.salts_anti_aliasing.client.compat.sodium;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.betterLostItems.salts_anti_aliasing.SaltsAntiAliasing;
import org.betterLostItems.salts_anti_aliasing.client.SaltsAntiAliasingClient;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaLevel;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaResolveFilter;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaSharpness;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaTranslationKeys;
import org.betterLostItems.salts_anti_aliasing.client.gui.ClientText;
import org.betterLostItems.salts_anti_aliasing.client.gui.SsaaVideoSettingsSection;

/**
 * Adds a "Salt's Anti Aliasing" page with the SSAA level, downscale filter and sharpness to Sodium's video settings. Sodium replaces
 * Minecraft's Video Settings screen, so the slider added there would otherwise not be reachable.
 */
public final class SaltsAntiAliasingSodiumConfig implements ConfigEntryPoint {
    private static final Identifier SSAA_LEVEL_ID = Identifier.fromNamespaceAndPath(SaltsAntiAliasing.MOD_ID, "ssaa_level");
    private static final Identifier SSAA_FILTER_ID = Identifier.fromNamespaceAndPath(SaltsAntiAliasing.MOD_ID, "ssaa_filter");
    private static final Identifier SSAA_SHARPNESS_ID = Identifier.fromNamespaceAndPath(SaltsAntiAliasing.MOD_ID, "ssaa_sharpness");

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        builder.registerOwnModOptions()
                .addPage(builder.createOptionPage()
                        .setName(Component.translatable(SsaaTranslationKeys.SCREEN_TITLE))
                        .addOptionGroup(builder.createOptionGroup()
                                .setName(Component.translatable(SsaaTranslationKeys.SECTION_HEADER))
                                .addOption(createSsaaLevelOption(builder))
                                .addOption(createFilterOption(builder))
                                .addOption(createSharpnessOption(builder))
                        ));
    }

    private static EnumOptionBuilder<SsaaLevel> createSsaaLevelOption(ConfigBuilder builder) {
        return builder.createEnumOption(SSAA_LEVEL_ID, SsaaLevel.class)
                .setName(Component.translatable(SsaaTranslationKeys.SSAA_NAME))
                .setTooltip(ClientText::tooltip)
                .setStorageHandler(SaltsAntiAliasingSodiumConfig::afterSave)
                .setBinding(SaltsAntiAliasingSodiumConfig::setLevel, SaltsAntiAliasingSodiumConfig::level)
                .setDefaultValue(SsaaLevel.defaultLevel())
                .setElementNameProvider(ClientText::label)
                .setEnabledProvider(state -> ssaaAvailable(), ConfigState.UPDATE_ON_REBUILD);
    }

    private static EnumOptionBuilder<SsaaResolveFilter> createFilterOption(ConfigBuilder builder) {
        return builder.createEnumOption(SSAA_FILTER_ID, SsaaResolveFilter.class)
                .setName(Component.translatable(SsaaTranslationKeys.FILTER_NAME))
                .setTooltip(ClientText::filterTooltip)
                .setStorageHandler(SaltsAntiAliasingSodiumConfig::afterSave)
                .setBinding(SaltsAntiAliasingSodiumConfig::setResolveFilter, SaltsAntiAliasingSodiumConfig::resolveFilter)
                .setDefaultValue(SsaaResolveFilter.defaultFilter())
                .setElementNameProvider(ClientText::filterLabel)
                .setEnabledProvider(state -> SaltsAntiAliasingClient.configOrNull() != null, ConfigState.UPDATE_ON_REBUILD);
    }

    private static EnumOptionBuilder<SsaaSharpness> createSharpnessOption(ConfigBuilder builder) {
        return builder.createEnumOption(SSAA_SHARPNESS_ID, SsaaSharpness.class)
                .setName(Component.translatable(SsaaTranslationKeys.SHARPNESS_NAME))
                .setTooltip(ClientText::sharpnessTooltip)
                .setStorageHandler(SaltsAntiAliasingSodiumConfig::afterSave)
                .setBinding(SaltsAntiAliasingSodiumConfig::setSharpness, SaltsAntiAliasingSodiumConfig::sharpness)
                .setDefaultValue(SsaaSharpness.defaultSharpness())
                .setElementNameProvider(ClientText::sharpnessLabel)
                .setEnabledProvider(state -> SaltsAntiAliasingClient.configOrNull() != null, ConfigState.UPDATE_ON_REBUILD);
    }

    /** The level is written to disk by the config manager as soon as it is set. */
    private static void afterSave() {
    }

    /** Changing the level is blocked while Minecraft's Improved Transparency option is on. */
    private static boolean ssaaAvailable() {
        return SaltsAntiAliasingClient.configOrNull() != null
                && !SsaaVideoSettingsSection.improvedTransparencyEnabled();
    }

    private static SsaaLevel level() {
        return SaltsAntiAliasingClient.level();
    }

    private static void setLevel(SsaaLevel level) {
        SaltsAntiAliasingClient.config().setLevel(level);
    }

    private static SsaaResolveFilter resolveFilter() {
        return SaltsAntiAliasingClient.resolveFilter();
    }

    private static void setResolveFilter(SsaaResolveFilter filter) {
        SaltsAntiAliasingClient.config().setResolveFilter(filter);
    }

    private static SsaaSharpness sharpness() {
        return SaltsAntiAliasingClient.sharpness();
    }

    private static void setSharpness(SsaaSharpness sharpness) {
        SaltsAntiAliasingClient.config().setSharpness(sharpness);
    }
}
