package org.betterLostItems.salts_anti_aliasing.client.compat.sodium;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.betterLostItems.salts_anti_aliasing.SaltsAntiAliasing;
import org.betterLostItems.salts_anti_aliasing.client.SaltsAntiAliasingClient;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaLevel;
import org.betterLostItems.salts_anti_aliasing.client.config.SsaaTranslationKeys;
import org.betterLostItems.salts_anti_aliasing.client.gui.ClientText;

/**
 * Adds a "Salt's Anti Aliasing" page with the SSAA level to Sodium's video settings. Sodium replaces
 * Minecraft's Video Settings screen, so the slider added there would otherwise not be reachable.
 */
public final class SaltsAntiAliasingSodiumConfig implements ConfigEntryPoint {
    private static final Identifier SSAA_LEVEL_ID = Identifier.fromNamespaceAndPath(SaltsAntiAliasing.MOD_ID, "ssaa_level");

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        builder.registerOwnModOptions()
                .addPage(builder.createOptionPage()
                        .setName(Component.translatable(SsaaTranslationKeys.SCREEN_TITLE))
                        .addOptionGroup(builder.createOptionGroup()
                                .setName(Component.translatable(SsaaTranslationKeys.SECTION_HEADER))
                                .addOption(createSsaaLevelOption(builder))
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

    /** The level is written to disk by the config manager as soon as it is set. */
    private static void afterSave() {
    }

    /** Changing the level is blocked while Minecraft's Improved Transparency option is on. */
    private static boolean ssaaAvailable() {
        return SaltsAntiAliasingClient.configOrNull() != null
                && !(Boolean) Minecraft.getInstance().options.improvedTransparency().get();
    }

    private static SsaaLevel level() {
        return SaltsAntiAliasingClient.level();
    }

    private static void setLevel(SsaaLevel level) {
        SaltsAntiAliasingClient.config().setLevel(level);
    }
}
