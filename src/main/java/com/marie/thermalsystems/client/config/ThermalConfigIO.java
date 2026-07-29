package com.marie.thermalsystems.client.config;

import com.google.gson.JsonObject;
import com.marie.thermalsystems.data.config.ThermalConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Converts {@link ThermalConfig}'s fields to/from a JSON tree, nested by the
 * same category keys as the underlying TOML spec. Shared by the config
 * screen's presets and export/import wiring so both operate on one
 * definition of "current config values".
 */
public final class ThermalConfigIO {

    private ThermalConfigIO() {}

    public static JsonObject buildRoot() {
        JsonObject root = new JsonObject();

        JsonObject simulation = new JsonObject();
        simulation.addProperty("simulationTickInterval", ThermalConfig.SIMULATION_TICK_INTERVAL.get());
        simulation.addProperty("heatTransferCoefficient", ThermalConfig.HEAT_TRANSFER_COEFFICIENT.get());
        simulation.addProperty("temperatureConvergenceRate", ThermalConfig.TEMPERATURE_CONVERGENCE_RATE.get());
        simulation.addProperty("minimumTemperature", ThermalConfig.MINIMUM_TEMPERATURE.get());
        simulation.addProperty("maximumTemperature", ThermalConfig.MAXIMUM_TEMPERATURE.get());
        simulation.addProperty("loggingEnabled", ThermalConfig.LOGGING_ENABLED.get());
        simulation.addProperty("radiationLoggingEnabled", ThermalConfig.RADIATION_LOGGING_ENABLED.get());
        simulation.addProperty("bindingLoggingEnabled", ThermalConfig.BINDING_LOGGING_ENABLED.get());
        root.add("simulation", simulation);

        JsonObject integration = new JsonObject();
        integration.addProperty("playerBridgeInterval", ThermalConfig.PLAYER_BRIDGE_INTERVAL.get());
        integration.addProperty("defaultAmbientTemperature", ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE.get());
        integration.addProperty("sourceBindingRadius", ThermalConfig.SOURCE_BINDING_RADIUS.get());
        integration.addProperty("sourceScanInterval", ThermalConfig.SOURCE_BINDING_SCAN_INTERVAL.get());
        integration.addProperty("sourceRadiationRadius", ThermalConfig.SOURCE_RADIATION_RADIUS.get());
        integration.addProperty("sourceRadiationInterval", ThermalConfig.SOURCE_RADIATION_INTERVAL.get());
        integration.addProperty("sourceTrackingReverifyInterval", ThermalConfig.SOURCE_TRACKING_REVERIFY_INTERVAL.get());
        integration.addProperty("hoverTooltipsEnabled", ThermalConfig.HOVER_TOOLTIPS_ENABLED.get());
        root.add("integration", integration);

        JsonObject pneumaticcraft = new JsonObject();
        pneumaticcraft.addProperty("referenceTemperatureKelvin", ThermalConfig.PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN.get());
        pneumaticcraft.addProperty("exchangerConversionCoefficient", ThermalConfig.PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT.get());
        root.add("pneumaticcraft", pneumaticcraft);

        JsonObject mekanism = new JsonObject();
        mekanism.addProperty("referenceTemperatureKelvin", ThermalConfig.MEKANISM_REFERENCE_TEMPERATURE_KELVIN.get());
        mekanism.addProperty("conversionCoefficient", ThermalConfig.MEKANISM_CONVERSION_COEFFICIENT.get());
        mekanism.addProperty("networkRecomputeInterval", ThermalConfig.MEKANISM_NETWORK_RECOMPUTE_INTERVAL.get());
        root.add("mekanism", mekanism);

        JsonObject enderio = new JsonObject();
        enderio.addProperty("energyToHeatCoefficient", ThermalConfig.ENDERIO_ENERGY_TO_HEAT_COEFFICIENT.get());
        enderio.addProperty("outputMultiplier", ThermalConfig.ENDERIO_OUTPUT_MULTIPLIER.get());
        enderio.addProperty("networkRecomputeInterval", ThermalConfig.ENDERIO_NETWORK_RECOMPUTE_INTERVAL.get());
        root.add("enderio", enderio);

        JsonObject lso = new JsonObject();
        lso.addProperty("temperatureOffset", ThermalConfig.LSO_TEMPERATURE_OFFSET.get());
        root.add("lso", lso);

        JsonObject coldsweat = new JsonObject();
        coldsweat.addProperty("temperatureOffset", ThermalConfig.COLDSWEAT_TEMPERATURE_OFFSET.get());
        coldsweat.addProperty("outputScale", ThermalConfig.COLDSWEAT_OUTPUT_SCALE.get());
        root.add("coldsweat", coldsweat);

        return root;
    }

    public static void applyRoot(JsonObject root) {
        applySection(root, "simulation", s -> {
            applyInt(s, "simulationTickInterval", ThermalConfig.SIMULATION_TICK_INTERVAL);
            applyDouble(s, "heatTransferCoefficient", ThermalConfig.HEAT_TRANSFER_COEFFICIENT);
            applyDouble(s, "temperatureConvergenceRate", ThermalConfig.TEMPERATURE_CONVERGENCE_RATE);
            applyDouble(s, "minimumTemperature", ThermalConfig.MINIMUM_TEMPERATURE);
            applyDouble(s, "maximumTemperature", ThermalConfig.MAXIMUM_TEMPERATURE);
            applyBoolean(s, "loggingEnabled", ThermalConfig.LOGGING_ENABLED);
            applyBoolean(s, "radiationLoggingEnabled", ThermalConfig.RADIATION_LOGGING_ENABLED);
            applyBoolean(s, "bindingLoggingEnabled", ThermalConfig.BINDING_LOGGING_ENABLED);
        });
        applySection(root, "integration", s -> {
            applyInt(s, "playerBridgeInterval", ThermalConfig.PLAYER_BRIDGE_INTERVAL);
            applyDouble(s, "defaultAmbientTemperature", ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE);
            applyInt(s, "sourceBindingRadius", ThermalConfig.SOURCE_BINDING_RADIUS);
            applyInt(s, "sourceScanInterval", ThermalConfig.SOURCE_BINDING_SCAN_INTERVAL);
            applyInt(s, "sourceRadiationRadius", ThermalConfig.SOURCE_RADIATION_RADIUS);
            applyInt(s, "sourceRadiationInterval", ThermalConfig.SOURCE_RADIATION_INTERVAL);
            applyInt(s, "sourceTrackingReverifyInterval", ThermalConfig.SOURCE_TRACKING_REVERIFY_INTERVAL);
            applyBoolean(s, "hoverTooltipsEnabled", ThermalConfig.HOVER_TOOLTIPS_ENABLED);
        });
        applySection(root, "pneumaticcraft", s -> {
            applyDouble(s, "referenceTemperatureKelvin", ThermalConfig.PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN);
            applyDouble(s, "exchangerConversionCoefficient", ThermalConfig.PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT);
        });
        applySection(root, "mekanism", s -> {
            applyDouble(s, "referenceTemperatureKelvin", ThermalConfig.MEKANISM_REFERENCE_TEMPERATURE_KELVIN);
            applyDouble(s, "conversionCoefficient", ThermalConfig.MEKANISM_CONVERSION_COEFFICIENT);
            applyInt(s, "networkRecomputeInterval", ThermalConfig.MEKANISM_NETWORK_RECOMPUTE_INTERVAL);
        });
        applySection(root, "enderio", s -> {
            applyDouble(s, "energyToHeatCoefficient", ThermalConfig.ENDERIO_ENERGY_TO_HEAT_COEFFICIENT);
            applyDouble(s, "outputMultiplier", ThermalConfig.ENDERIO_OUTPUT_MULTIPLIER);
            applyInt(s, "networkRecomputeInterval", ThermalConfig.ENDERIO_NETWORK_RECOMPUTE_INTERVAL);
        });
        applySection(root, "lso", s ->
                applyDouble(s, "temperatureOffset", ThermalConfig.LSO_TEMPERATURE_OFFSET));
        applySection(root, "coldsweat", s -> {
            applyDouble(s, "temperatureOffset", ThermalConfig.COLDSWEAT_TEMPERATURE_OFFSET);
            applyDouble(s, "outputScale", ThermalConfig.COLDSWEAT_OUTPUT_SCALE);
        });
    }

    private interface SectionApplier {
        void apply(JsonObject section);
    }

    private static void applySection(JsonObject root, String key, SectionApplier applier) {
        if (root.has(key) && root.get(key).isJsonObject()) {
            applier.apply(root.getAsJsonObject(key));
        }
    }

    private static void applyInt(JsonObject section, String key, ModConfigSpec.IntValue target) {
        if (section.has(key)) {
            target.set(section.get(key).getAsInt());
        }
    }

    private static void applyDouble(JsonObject section, String key, ModConfigSpec.DoubleValue target) {
        if (section.has(key)) {
            target.set(section.get(key).getAsDouble());
        }
    }

    private static void applyBoolean(JsonObject section, String key, ModConfigSpec.BooleanValue target) {
        if (section.has(key)) {
            target.set(section.get(key).getAsBoolean());
        }
    }
}
