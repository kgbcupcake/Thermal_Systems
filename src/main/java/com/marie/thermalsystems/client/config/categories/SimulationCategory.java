package com.marie.thermalsystems.client.config.categories;

import com.marie.thermalsystems.data.config.ThermalConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class SimulationCategory {

    private SimulationCategory() {}

    public static void addSimulationCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.thermalsystems.category.simulation"));

        category.addEntry(
                eb.startIntField(Component.translatable("config.thermalsystems.simulationTickInterval"), ThermalConfig.SIMULATION_TICK_INTERVAL.get())
                        .setDefaultValue(20)
                        .setMin(1)
                        .setTooltip(Component.translatable("config.thermalsystems.simulationTickInterval.desc"))
                        .setSaveConsumer(ThermalConfig.SIMULATION_TICK_INTERVAL::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(Component.translatable("config.thermalsystems.heatTransferCoefficient"), ThermalConfig.HEAT_TRANSFER_COEFFICIENT.get())
                        .setDefaultValue(0.05)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.heatTransferCoefficient.desc"))
                        .setSaveConsumer(ThermalConfig.HEAT_TRANSFER_COEFFICIENT::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(Component.translatable("config.thermalsystems.temperatureConvergenceRate"), ThermalConfig.TEMPERATURE_CONVERGENCE_RATE.get())
                        .setDefaultValue(0.02)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.temperatureConvergenceRate.desc"))
                        .setSaveConsumer(ThermalConfig.TEMPERATURE_CONVERGENCE_RATE::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(Component.translatable("config.thermalsystems.minimumTemperature"), ThermalConfig.MINIMUM_TEMPERATURE.get())
                        .setDefaultValue(-50.0)
                        .setTooltip(Component.translatable("config.thermalsystems.minimumTemperature.desc"))
                        .setSaveConsumer(ThermalConfig.MINIMUM_TEMPERATURE::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(Component.translatable("config.thermalsystems.maximumTemperature"), ThermalConfig.MAXIMUM_TEMPERATURE.get())
                        .setDefaultValue(50.0)
                        .setTooltip(Component.translatable("config.thermalsystems.maximumTemperature.desc"))
                        .setSaveConsumer(ThermalConfig.MAXIMUM_TEMPERATURE::set)
                        .build()
        );

        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.thermalsystems.loggingEnabled"), ThermalConfig.LOGGING_ENABLED.get())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.thermalsystems.loggingEnabled.desc"))
                        .setSaveConsumer(ThermalConfig.LOGGING_ENABLED::set)
                        .build()
        );

        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.thermalsystems.radiationLoggingEnabled"), ThermalConfig.RADIATION_LOGGING_ENABLED.get())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.thermalsystems.radiationLoggingEnabled.desc"))
                        .setSaveConsumer(ThermalConfig.RADIATION_LOGGING_ENABLED::set)
                        .build()
        );

        category.addEntry(
                eb.startBooleanToggle(Component.translatable("config.thermalsystems.bindingLoggingEnabled"), ThermalConfig.BINDING_LOGGING_ENABLED.get())
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.thermalsystems.bindingLoggingEnabled.desc"))
                        .setSaveConsumer(ThermalConfig.BINDING_LOGGING_ENABLED::set)
                        .build()
        );
    }
}
