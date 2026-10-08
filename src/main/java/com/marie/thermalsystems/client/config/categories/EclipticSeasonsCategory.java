package com.marie.thermalsystems.client.config.categories;

import com.marie.thermalsystems.data.config.ThermalConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class EclipticSeasonsCategory {

    private EclipticSeasonsCategory() {}

    public static void addCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.thermalsystems.category.eclipticseasons"));

        category.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("config.thermalsystems.eclipticseasonsEnabled"),
                                ThermalConfig.ECLIPTIC_SEASONS_ENABLED.get()
                        )
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.thermalsystems.eclipticseasonsEnabled.desc"))
                        .setSaveConsumer(ThermalConfig.ECLIPTIC_SEASONS_ENABLED::set)
                        .build()
        );
    }
}
