package com.marie.thermalsystems.client.config;

import dev.marie.framework.config.PresetRegistry.PresetValues;
import dev.marie.framework.core.MarieContext;
import net.minecraft.client.gui.screens.Screen;

/**
 * Registers Thermal Systems' {@link MarieContext} with just enough wiring
 * for the config screen, presets, and export/import to work: the config
 * screen factory, export/import screen factories, and the
 * preset/export/import JSON hooks backed by {@link ThermalConfigIO}.
 *
 * <p>Deliberately calls {@link MarieContext#register(MarieContext)} rather
 * than {@code MarieBootstrap.attach(...)} - Thermal Systems has no use for
 * the value-tracking/attribute machinery {@code attach} wires up (that's
 * Nourished's nutrition-tracking domain, not a thermal simulation), so this
 * stays a lighter-weight registration on top of the
 * {@code MarieBootstrap.attachFrameworkServices(...)} call already made from
 * {@code ThermalSystemsMod}.
 *
 * <p>Registering unconditionally (not gated behind {@code Dist.CLIENT}) is
 * safe: the client-only lambdas below (referencing {@link Screen} and
 * {@link ThermalSystemsConfigScreen}) are only ever invoked from client-side
 * callers ({@code ClientScreenFactories}, or the {@code IConfigScreenFactory}
 * extension point registered separately under a dist check in
 * {@code ThermalSystemsMod}) - the same pattern Nourished's own
 * {@code NourishedContextBuilder} uses.
 */
public final class ThermalContextRegistration {

    private ThermalContextRegistration() {}

    public static void register(String modId) {
        MarieContext.register(
                MarieContext.builder(modId)
                        .configScreenFactory(() -> ThermalSystemsConfigScreen.create(null))
                        .exportScreenFactory(parent -> new ThermalExportScreen((Screen) parent))
                        .importScreenFactory(parent -> new ThermalImportScreen((Screen) parent))
                        .currentConfigPresetValues(() -> PresetValues.fromJsonObject(ThermalConfigIO.buildRoot()))
                        .applyPresetValues(values -> ThermalConfigIO.applyRoot(values.toJsonObject()))
                        .configExporter(ThermalConfigIO::buildRoot)
                        .configImporter(ThermalConfigIO::applyRoot)
                        .build()
        );
    }
}
