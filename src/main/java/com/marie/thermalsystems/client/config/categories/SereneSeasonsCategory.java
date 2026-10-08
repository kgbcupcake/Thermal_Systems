package com.marie.thermalsystems.client.config.categories;

import com.marie.thermalsystems.data.config.ThermalConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class SereneSeasonsCategory {

    private SereneSeasonsCategory() {}

    public static void addCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.thermalsystems.category.sereneseasons"));

        category.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("config.thermalsystems.sereneseasonsEnabled"),
                                ThermalConfig.SERENE_SEASONS_ENABLED.get()
                        )
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.thermalsystems.sereneseasonsEnabled.desc"))
                        .setSaveConsumer(ThermalConfig.SERENE_SEASONS_ENABLED::set)
                        .build()
        );
    }
}
