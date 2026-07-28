package com.marie.thermalsystems.client.config.categories;

import com.marie.thermalsystems.data.config.ThermalConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class EnderIOCategory {

    private EnderIOCategory() {}

    public static void addEnderIOCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.thermalsystems.category.enderio"));

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.enderioEnergyToHeatCoefficient"),
                                ThermalConfig.ENDERIO_ENERGY_TO_HEAT_COEFFICIENT.get()
                        )
                        .setDefaultValue(0.001)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.enderioEnergyToHeatCoefficient.desc"))
                        .setSaveConsumer(ThermalConfig.ENDERIO_ENERGY_TO_HEAT_COEFFICIENT::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.enderioOutputMultiplier"),
                                ThermalConfig.ENDERIO_OUTPUT_MULTIPLIER.get()
                        )
                        .setDefaultValue(1.0)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.enderioOutputMultiplier.desc"))
                        .setSaveConsumer(ThermalConfig.ENDERIO_OUTPUT_MULTIPLIER::set)
                        .build()
        );

        category.addEntry(
                eb.startIntField(
                                Component.translatable("config.thermalsystems.enderioNetworkRecomputeInterval"),
                                ThermalConfig.ENDERIO_NETWORK_RECOMPUTE_INTERVAL.get()
                        )
                        .setDefaultValue(20)
                        .setMin(1)
                        .setTooltip(Component.translatable("config.thermalsystems.enderioNetworkRecomputeInterval.desc"))
                        .setSaveConsumer(ThermalConfig.ENDERIO_NETWORK_RECOMPUTE_INTERVAL::set)
                        .build()
        );
    }
}
