package com.marie.thermalsystems.client.config.categories;

import dev.marie.framework.client.config.importexport.ImportExportButtonsWidget;
import dev.marie.framework.client.config.presets.PresetsWidget;
import dev.marie.framework.config.PresetRegistry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Presets and import/export are wired straight to Marie's Lib's generic
 * widgets ({@link PresetsWidget}, {@link ImportExportButtonsWidget}) - both
 * are mod-agnostic and read/write through whatever {@code MarieContext} the
 * active mod registered, so no Thermal-Systems-specific widget code is
 * needed here.
 */
public final class PresetsCategory {

    private PresetsCategory() {}

    public static void addPresetsCategory(ConfigBuilder builder, Screen reopenParent) {
        PresetRegistry.ensureBuiltInFilesOnDisk();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("config.thermalsystems.category.presets"));
        category.addEntry(new PresetsWidget(reopenParent));
        category.addEntry(new ImportExportButtonsWidget(reopenParent));
    }
}
