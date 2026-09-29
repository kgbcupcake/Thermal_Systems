package com.marie.thermalsystems.client.config.categories;

import com.marie.thermalsystems.data.config.ThermalConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public final class ToughAsNailsCategory {

    private ToughAsNailsCategory() {}

    public static void addToughAsNailsCategory(ConfigBuilder builder, ConfigEntryBuilder eb) {
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.thermalsystems.category.toughasnails"));

        category.addEntry(
                eb.startBooleanToggle(
                                Component.translatable("config.thermalsystems.toughasnailsEnabled"),
                                ThermalConfig.TOUGHASNAILS_ENABLED.get()
                        )
                        .setDefaultValue(true)
                        .setTooltip(Component.translatable("config.thermalsystems.toughasnailsEnabled.desc"))
                        .setSaveConsumer(ThermalConfig.TOUGHASNAILS_ENABLED::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.toughasnailsHeatOneStepThreshold"),
                                ThermalConfig.TOUGHASNAILS_HEAT_ONE_STEP_THRESHOLD.get()
                        )
                        .setDefaultValue(5.0)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.toughasnailsHeatOneStepThreshold.desc"))
                        .setSaveConsumer(ThermalConfig.TOUGHASNAILS_HEAT_ONE_STEP_THRESHOLD::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.toughasnailsHeatTwoStepThreshold"),
                                ThermalConfig.TOUGHASNAILS_HEAT_TWO_STEP_THRESHOLD.get()
                        )
                        .setDefaultValue(15.0)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.toughasnailsHeatTwoStepThreshold.desc"))
                        .setSaveConsumer(ThermalConfig.TOUGHASNAILS_HEAT_TWO_STEP_THRESHOLD::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.toughasnailsCoolingOneStepThreshold"),
                                ThermalConfig.TOUGHASNAILS_COOLING_ONE_STEP_THRESHOLD.get()
                        )
                        .setDefaultValue(5.0)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.toughasnailsCoolingOneStepThreshold.desc"))
                        .setSaveConsumer(ThermalConfig.TOUGHASNAILS_COOLING_ONE_STEP_THRESHOLD::set)
                        .build()
        );

        category.addEntry(
                eb.startDoubleField(
                                Component.translatable("config.thermalsystems.toughasnailsCoolingTwoStepThreshold"),
                                ThermalConfig.TOUGHASNAILS_COOLING_TWO_STEP_THRESHOLD.get()
                        )
                        .setDefaultValue(15.0)
                        .setMin(0.0)
                        .setTooltip(Component.translatable("config.thermalsystems.toughasnailsCoolingTwoStepThreshold.desc"))
                        .setSaveConsumer(ThermalConfig.TOUGHASNAILS_COOLING_TWO_STEP_THRESHOLD::set)
                        .build()
        );
    }
}
