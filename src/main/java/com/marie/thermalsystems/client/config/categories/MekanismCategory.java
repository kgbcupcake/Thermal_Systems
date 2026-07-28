package com.marie.thermalsystems.client.config.categories;

import com.marie.thermalsystems.data.config.ThermalConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class MekanismCategory {

    private MekanismCategory() {}

    public static void addMekanismCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.thermalsystems.category.mekanism"));

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.mekanismReferenceTemperatureKelvin"),
                                ThermalConfig.MEKANISM_REFERENCE_TEMPERATURE_KELVIN.get()
                        )
                        .setDefaultValue(300.0)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.mekanismReferenceTemperatureKelvin.desc"))
                        .setSaveConsumer(ThermalConfig.MEKANISM_REFERENCE_TEMPERATURE_KELVIN::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.mekanismConversionCoefficient"),
                                ThermalConfig.MEKANISM_CONVERSION_COEFFICIENT.get()
                        )
                        .setDefaultValue(0.05)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.mekanismConversionCoefficient.desc"))
                        .setSaveConsumer(ThermalConfig.MEKANISM_CONVERSION_COEFFICIENT::set)
                        .build()
        );

        category.addEntry(
                eb.startIntField(
                                Component.translatable("config.thermalsystems.mekanismNetworkRecomputeInterval"),
                                ThermalConfig.MEKANISM_NETWORK_RECOMPUTE_INTERVAL.get()
                        )
                        .setDefaultValue(20)
                        .setMin(1)
                        .setTooltip(Component.translatable("config.thermalsystems.mekanismNetworkRecomputeInterval.desc"))
                        .setSaveConsumer(ThermalConfig.MEKANISM_NETWORK_RECOMPUTE_INTERVAL::set)
                        .build()
        );
    }
}
