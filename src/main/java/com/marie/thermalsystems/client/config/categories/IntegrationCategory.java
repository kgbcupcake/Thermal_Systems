package com.marie.thermalsystems.client.config.categories;

import com.marie.thermalsystems.data.config.ThermalConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class IntegrationCategory {

    private IntegrationCategory() {}

    public static void addIntegrationCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.thermalsystems.category.integration"));

        category.addEntry(
                eb.startIntField(Component.translatable("config.thermalsystems.playerBridgeInterval"), ThermalConfig.PLAYER_BRIDGE_INTERVAL.get())
                        .setDefaultValue(20)
                        .setMin(1)
                        .setTooltip(Component.translatable("config.thermalsystems.playerBridgeInterval.desc"))
                        .setSaveConsumer(ThermalConfig.PLAYER_BRIDGE_INTERVAL::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(Component.translatable("config.thermalsystems.defaultAmbientTemperature"), ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE.get())
                        .setDefaultValue(20.0)
                        .setTooltip(Component.translatable("config.thermalsystems.defaultAmbientTemperature.desc"))
                        .setSaveConsumer(ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE::set)
                        .build()
        );

        category.addEntry(
                eb.startIntField(Component.translatable("config.thermalsystems.sourceBindingRadius"), ThermalConfig.SOURCE_BINDING_RADIUS.get())
                        .setDefaultValue(5)
                        .setMin(1)
                        .setMax(64)
                        .setTooltip(Component.translatable("config.thermalsystems.sourceBindingRadius.desc"))
                        .setSaveConsumer(ThermalConfig.SOURCE_BINDING_RADIUS::set)
                        .build()
        );

        category.addEntry(
                eb.startIntField(Component.translatable("config.thermalsystems.sourceScanInterval"), ThermalConfig.SOURCE_BINDING_SCAN_INTERVAL.get())
                        .setDefaultValue(100)
                        .setMin(1)
                        .setTooltip(Component.translatable("config.thermalsystems.sourceScanInterval.desc"))
                        .setSaveConsumer(ThermalConfig.SOURCE_BINDING_SCAN_INTERVAL::set)
                        .build()
        );

        category.addEntry(
                eb.startIntField(Component.translatable("config.thermalsystems.sourceRadiationRadius"), ThermalConfig.SOURCE_RADIATION_RADIUS.get())
                        .setDefaultValue(5)
                        .setMin(1)
                        .setMax(64)
                        .setTooltip(Component.translatable("config.thermalsystems.sourceRadiationRadius.desc"))
                        .setSaveConsumer(ThermalConfig.SOURCE_RADIATION_RADIUS::set)
                        .build()
        );

        category.addEntry(
                eb.startIntField(Component.translatable("config.thermalsystems.sourceRadiationInterval"), ThermalConfig.SOURCE_RADIATION_INTERVAL.get())
                        .setDefaultValue(20)
                        .setMin(1)
                        .setTooltip(Component.translatable("config.thermalsystems.sourceRadiationInterval.desc"))
                        .setSaveConsumer(ThermalConfig.SOURCE_RADIATION_INTERVAL::set)
                        .build()
        );

        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.thermalsystems.hoverTooltipsEnabled"), ThermalConfig.HOVER_TOOLTIPS_ENABLED.get())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.thermalsystems.hoverTooltipsEnabled.desc"))
                        .setSaveConsumer(ThermalConfig.HOVER_TOOLTIPS_ENABLED::set)
                        .build()
        );
    }
}
