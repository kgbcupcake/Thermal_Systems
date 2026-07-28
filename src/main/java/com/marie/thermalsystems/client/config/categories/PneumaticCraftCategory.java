package com.marie.thermalsystems.client.config.categories;

import com.marie.thermalsystems.data.config.ThermalConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class PneumaticCraftCategory {

    private PneumaticCraftCategory() {}

    public static void addPneumaticCraftCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.thermalsystems.category.pneumaticcraft"));

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.pneumaticcraftReferenceTemperatureKelvin"),
                                ThermalConfig.PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN.get()
                        )
                        .setDefaultValue(293.15)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.pneumaticcraftReferenceTemperatureKelvin.desc"))
                        .setSaveConsumer(ThermalConfig.PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.pneumaticcraftExchangerConversionCoefficient"),
                                ThermalConfig.PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT.get()
                        )
                        .setDefaultValue(0.05)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.pneumaticcraftExchangerConversionCoefficient.desc"))
                        .setSaveConsumer(ThermalConfig.PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT::set)
                        .build()
        );
    }
}
