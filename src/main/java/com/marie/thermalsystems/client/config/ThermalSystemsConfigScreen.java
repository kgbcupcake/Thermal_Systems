package com.marie.thermalsystems.client.config;

import com.marie.thermalsystems.client.config.categories.ColdSweatCategory;
import com.marie.thermalsystems.client.config.categories.EnderIOCategory;
import com.marie.thermalsystems.client.config.categories.IntegrationCategory;
import com.marie.thermalsystems.client.config.categories.LSOCategory;
import com.marie.thermalsystems.client.config.categories.MekanismCategory;
import com.marie.thermalsystems.client.config.categories.PneumaticCraftCategory;
import com.marie.thermalsystems.client.config.categories.PresetsCategory;
import com.marie.thermalsystems.client.config.categories.SimulationCategory;
import com.marie.thermalsystems.data.config.ThermalConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Thermal Systems' config screen: a Cloth Config screen with its category
 * tabs repositioned into a left sidebar by {@link ThermalConfigSidebarLayout}
 * (Cloth's built-in per-category search field renders at the top of each
 * category's entry list, unaffected by that repositioning) and a Presets
 * category wired to Marie's Lib's generic preset/import-export widgets.
 *
 * <p>Individual fields render as plain Cloth entries rather than
 * MarieComponent input widgets - the sidebar itself only works by
 * repositioning Cloth's own tab buttons, so the screen has to stay a real
 * {@code ClothConfigScreen} underneath.
 */
public final class ThermalSystemsConfigScreen {

    private ThermalSystemsConfigScreen() {}

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.thermalsystems.title"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        PresetsCategory.addPresetsCategory(builder, parent);
        SimulationCategory.addSimulationCategory(builder, entryBuilder);
        IntegrationCategory.addIntegrationCategory(builder, entryBuilder);
        EnderIOCategory.addEnderIOCategory(builder, entryBuilder);
        MekanismCategory.addMekanismCategory(builder, entryBuilder);
        PneumaticCraftCategory.addPneumaticCraftCategory(builder, entryBuilder);
        LSOCategory.addLSOCategory(builder, entryBuilder);
        ColdSweatCategory.addColdSweatCategory(builder, entryBuilder);

        builder.setAlwaysShowTabs(true);
        builder.setAfterInitConsumer(ThermalConfigSidebarLayout::apply);

        // Each entry's setSaveConsumer(ThermalConfig.X::set) only updates the in-memory
        // ModConfigSpec.ConfigValue - per NeoForge's own ConfigValue#set javadoc, it sets
        // "without firing events or writing the config to disk". Cloth's Save & Quit button
        // (ClothConfigScreen's saveAll()) calls entry.save() for that part, then separately
        // runs whatever's registered here as the savingRunnable - without this, values only
        // ever live in memory and silently reset to the on-disk TOML on next launch.
        builder.setSavingRunnable(ThermalConfig.SPEC::save);

        return builder.build();
    }
}
