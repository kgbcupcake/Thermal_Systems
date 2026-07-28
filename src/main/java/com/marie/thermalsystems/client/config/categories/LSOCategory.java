package com.marie.thermalsystems.client.config.categories;

import com.marie.thermalsystems.data.config.ThermalConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class LSOCategory {

    private LSOCategory() {}

    public static void addLSOCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.thermalsystems.category.lso"));

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.lsoTemperatureOffset"),
                                ThermalConfig.LSO_TEMPERATURE_OFFSET.get()
                        )
                        .setDefaultValue(20.0)
                        .setTooltip(Component.translatable("config.thermalsystems.lsoTemperatureOffset.desc"))
                        .setSaveConsumer(ThermalConfig.LSO_TEMPERATURE_OFFSET::set)
                        .build()
        );
    }
}
