package com.marie.thermalsystems.client.config.categories;

import com.marie.thermalsystems.data.config.ThermalConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class ColdSweatCategory {

    private ColdSweatCategory() {}

    public static void addColdSweatCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.thermalsystems.category.coldsweat"));

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.coldsweatTemperatureOffset"),
                                ThermalConfig.COLDSWEAT_TEMPERATURE_OFFSET.get()
                        )
                        .setDefaultValue(20.0)
                        .setTooltip(Component.translatable("config.thermalsystems.coldsweatTemperatureOffset.desc"))
                        .setSaveConsumer(ThermalConfig.COLDSWEAT_TEMPERATURE_OFFSET::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.coldsweatOutputScale"),
                                ThermalConfig.COLDSWEAT_OUTPUT_SCALE.get()
                        )
                        .setDefaultValue(0.1)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.coldsweatOutputScale.desc"))
                        .setSaveConsumer(ThermalConfig.COLDSWEAT_OUTPUT_SCALE::set)
                        .build()
        );
    }
}
