package com.marie.thermalsystems.data.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge config spec exposing the tunable values for the climate simulation.
 */
public final class ThermalConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue SYSTEM_ENABLED;
    public static final ModConfigSpec.IntValue SIMULATION_TICK_INTERVAL;
    public static final ModConfigSpec.DoubleValue HEAT_TRANSFER_COEFFICIENT;
    public static final ModConfigSpec.DoubleValue TEMPERATURE_CONVERGENCE_RATE;
    public static final ModConfigSpec.DoubleValue MINIMUM_TEMPERATURE;
    public static final ModConfigSpec.DoubleValue MAXIMUM_TEMPERATURE;
    public static final ModConfigSpec.DoubleValue DEFAULT_TARGET_TEMPERATURE;
    public static final ModConfigSpec.BooleanValue LOGGING_ENABLED;
    public static final ModConfigSpec.BooleanValue RADIATION_LOGGING_ENABLED;
    public static final ModConfigSpec.BooleanValue BINDING_LOGGING_ENABLED;

    public static final ModConfigSpec.IntValue PLAYER_BRIDGE_INTERVAL;
    public static final ModConfigSpec.DoubleValue DEFAULT_AMBIENT_TEMPERATURE;

    public static final ModConfigSpec.IntValue SOURCE_BINDING_RADIUS;
    public static final ModConfigSpec.IntValue SOURCE_BINDING_SCAN_INTERVAL;

    public static final ModConfigSpec.IntValue SOURCE_RADIATION_RADIUS;
    public static final ModConfigSpec.IntValue SOURCE_RADIATION_INTERVAL;
    public static final ModConfigSpec.IntValue SOURCE_TRACKING_REVERIFY_INTERVAL;

    public static final ModConfigSpec.BooleanValue HOVER_TOOLTIPS_ENABLED;

    public static final ModConfigSpec.BooleanValue PNEUMATICCRAFT_ENABLED;
    public static final ModConfigSpec.DoubleValue PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN;
    public static final ModConfigSpec.DoubleValue PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT;

    public static final ModConfigSpec.BooleanValue MEKANISM_ENABLED;
    public static final ModConfigSpec.DoubleValue MEKANISM_REFERENCE_TEMPERATURE_KELVIN;
    public static final ModConfigSpec.DoubleValue MEKANISM_CONVERSION_COEFFICIENT;
    public static final ModConfigSpec.IntValue MEKANISM_NETWORK_RECOMPUTE_INTERVAL;

    public static final ModConfigSpec.BooleanValue ENDERIO_ENABLED;
    public static final ModConfigSpec.DoubleValue ENDERIO_ENERGY_TO_HEAT_COEFFICIENT;
    public static final ModConfigSpec.DoubleValue ENDERIO_OUTPUT_MULTIPLIER;
    public static final ModConfigSpec.IntValue ENDERIO_NETWORK_RECOMPUTE_INTERVAL;

    public static final ModConfigSpec.BooleanValue LSO_ENABLED;
    public static final ModConfigSpec.DoubleValue LSO_TEMPERATURE_OFFSET;

    public static final ModConfigSpec.BooleanValue COLDSWEAT_ENABLED;
    public static final ModConfigSpec.DoubleValue COLDSWEAT_TEMPERATURE_OFFSET;
    public static final ModConfigSpec.DoubleValue COLDSWEAT_OUTPUT_SCALE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("simulation");

        SYSTEM_ENABLED = builder
                .comment("Master on/off switch surfaced by the persistent Thermal Systems control panel HUD's",
                        "'System Enabled' toggle (see com.marie.thermalsystems.client.hud.ThermalSystemsControlPanel).",
                        "Not yet consumed by ClimateTickHandler or any other tick handler - flipping it here only",
                        "changes what's persisted/displayed until those systems are wired to check it.")
                .define("systemEnabled", true);

        SIMULATION_TICK_INTERVAL = builder
                .comment("Ticks between simulation updates.")
                .defineInRange("simulationTickInterval", 20, 1, Integer.MAX_VALUE);

        HEAT_TRANSFER_COEFFICIENT = builder
                .comment("Scales heat source contribution to effectiveConvergenceRate.")
                .defineInRange("heatTransferCoefficient", 0.05, 0.0, Double.MAX_VALUE);

        TEMPERATURE_CONVERGENCE_RATE = builder
                .comment("Baseline zone drift-to-target rate.")
                .defineInRange("temperatureConvergenceRate", 0.02, 0.0, Double.MAX_VALUE);

        MINIMUM_TEMPERATURE = builder
                .comment("Clamp floor, in Celsius.")
                .defineInRange("minimumTemperature", -50.0, -Double.MAX_VALUE, Double.MAX_VALUE);

        MAXIMUM_TEMPERATURE = builder
                .comment("Clamp ceiling, in Celsius.")
                .defineInRange("maximumTemperature", 50.0, -Double.MAX_VALUE, Double.MAX_VALUE);

        DEFAULT_TARGET_TEMPERATURE = builder
                .comment("Target temperature, in Celsius, for a newly created zone when none is specified.")
                .defineInRange("defaultTargetTemperature", 20.0, -Double.MAX_VALUE, Double.MAX_VALUE);

        LOGGING_ENABLED = builder
                .comment("Master switch gating per-interval log output. When false, silences everything below",
                        "regardless of the sub-flags' own values.")
                .define("loggingEnabled", true);

        RADIATION_LOGGING_ENABLED = builder
                .comment("Sub-flag gating SourceRadiationTickHandler/ActiveSourcePositions logging. Only takes",
                        "effect when loggingEnabled is also true.")
                .define("radiationLoggingEnabled", true);

        BINDING_LOGGING_ENABLED = builder
                .comment("Sub-flag gating ZoneSourceScanner/ZoneSourceBindingTickHandler logging. Only takes effect",
                        "when loggingEnabled is also true.")
                .define("bindingLoggingEnabled", true);

        builder.pop();

        builder.push("integration");

        PLAYER_BRIDGE_INTERVAL = builder
                .comment("Ticks between PlayerTemperatureBridgeHandler runs.")
                .defineInRange("playerBridgeInterval", 20, 1, Integer.MAX_VALUE);

        DEFAULT_AMBIENT_TEMPERATURE = builder
                .comment("Temperature reported to bridges for a player not inside any bounded zone.")
                .defineInRange("defaultAmbientTemperature", 20.0, -Double.MAX_VALUE, Double.MAX_VALUE);

        SOURCE_BINDING_RADIUS = builder
                .comment("Blocks beyond a bounded zone's edges to scan for foreign heat/cooling source capabilities to auto-bind.")
                .defineInRange("sourceBindingRadius", 5, 1, 64);

        SOURCE_BINDING_SCAN_INTERVAL = builder
                .comment("Ticks between automatic zone source-binding scans.")
                .defineInRange("sourceScanInterval", 100, 1, Integer.MAX_VALUE);

        SOURCE_RADIATION_RADIUS = builder
                .comment("Chebyshev-distance blocks a tracked heat/cooling source radiates directly to nearby players, independent of any zone.")
                .defineInRange("sourceRadiationRadius", 5, 1, 64);

        SOURCE_RADIATION_INTERVAL = builder
                .comment("Ticks between direct source-to-player radiation applications.")
                .defineInRange("sourceRadiationInterval", 20, 1, Integer.MAX_VALUE);

        SOURCE_TRACKING_REVERIFY_INTERVAL = builder
                .comment("Ticks between periodic ActiveSourcePositions re-verification passes that re-check",
                        "every currently tracked position still resolves to the expected block type,",
                        "independent of chunk load/unload event timing. Discovery of new positions is handled",
                        "instantly by block place/break events, not by this pass. Set to a very large value to",
                        "effectively disable.")
                .defineInRange("sourceTrackingReverifyInterval", 100, 1, Integer.MAX_VALUE);

        HOVER_TOOLTIPS_ENABLED = builder
                .comment("Gates the integration network hover tooltips (Ender IO, Mekanism, PneumaticCraft).")
                .define("hoverTooltipsEnabled", true);

        builder.pop();

        builder.push("pneumaticcraft");

        PNEUMATICCRAFT_ENABLED = builder
                .comment("Config-level on/off switch for the PneumaticCraft: Repressurized integration. Separate from",
                        "whether PneumaticCraft: Repressurized is actually installed - both must be true for the",
                        "integration to activate.")
                .define("enabled", true);

        PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN = builder
                .comment("Baseline temperature, in Kelvin, for Thermal Exchanger conversion.")
                .defineInRange("referenceTemperatureKelvin", 293.15, 0.0, Double.MAX_VALUE);

        PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT = builder
                .comment("Scales the Kelvin difference from referenceTemperatureKelvin into Thermal Systems heat/cooling output.")
                .defineInRange("exchangerConversionCoefficient", 0.05, 0.0, Double.MAX_VALUE);

        builder.pop();

        builder.push("mekanism");

        MEKANISM_ENABLED = builder
                .comment("Config-level on/off switch for the Mekanism integration. Separate from whether Mekanism is",
                        "actually installed - both must be true for the integration to activate.")
                .define("enabled", true);

        MEKANISM_REFERENCE_TEMPERATURE_KELVIN = builder
                .comment("Baseline temperature, in Kelvin, for Mekanism Heat Exchanger conversion. Matches Mekanism's own ambient temperature (HeatAPI.AMBIENT_TEMP) by default.")
                .defineInRange("referenceTemperatureKelvin", 300.0, 0.0, Double.MAX_VALUE);

        MEKANISM_CONVERSION_COEFFICIENT = builder
                .comment("Scales the Kelvin difference from referenceTemperatureKelvin into Thermal Systems heat/cooling output.")
                .defineInRange("conversionCoefficient", 0.05, 0.0, Double.MAX_VALUE);

        MEKANISM_NETWORK_RECOMPUTE_INTERVAL = builder
                .comment("Ticks a Mekanism cable network's heat/cooling sum is cached for before being recomputed on next read.")
                .defineInRange("networkRecomputeInterval", 20, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.push("enderio");

        ENDERIO_ENABLED = builder
                .comment("Config-level on/off switch for the Ender IO integration. Separate from whether Ender IO is",
                        "actually installed - both must be true for the integration to activate.")
                .define("enabled", true);

        ENDERIO_ENERGY_TO_HEAT_COEFFICIENT = builder
                .comment("Scales the FE an Ender IO Stirling Generator currently holds in storage into Thermal Systems heat output.")
                .defineInRange("energyToHeatCoefficient", 0.001, 0.0, Double.MAX_VALUE);

        ENDERIO_OUTPUT_MULTIPLIER = builder
                .comment("Direct intensity knob applied on top of energyToHeatCoefficient to scale the final Ender IO heat/cooling output hotter or colder, with no pretense of physical accuracy.")
                .defineInRange("outputMultiplier", 1.0, 0.0, Double.MAX_VALUE);

        ENDERIO_NETWORK_RECOMPUTE_INTERVAL = builder
                .comment("Ticks an Ender IO conduit network's heat sum is cached for before being recomputed on next read.")
                .defineInRange("networkRecomputeInterval", 20, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.push("lso");

        LSO_ENABLED = builder
                .comment("Config-level on/off switch for the Legendary Survival Overhaul integration. Separate from",
                        "whether Legendary Survival Overhaul is actually installed - both must be true for the",
                        "integration to activate.")
                .define("enabled", true);

        LSO_TEMPERATURE_OFFSET = builder
                .comment("Neutral ambient temperature, in Celsius, that Legendary Survival Overhaul treats as its zero-delta point. Matches TemperatureEnum.NORMAL's center by default.")
                .defineInRange("temperatureOffset", 20.0, -Double.MAX_VALUE, Double.MAX_VALUE);

        builder.pop();

        builder.push("coldsweat");

        COLDSWEAT_ENABLED = builder
                .comment("Config-level on/off switch for the Cold Sweat integration. Separate from whether Cold",
                        "Sweat is actually installed - both must be true for the integration to activate.")
                .define("enabled", true);

        COLDSWEAT_TEMPERATURE_OFFSET = builder
                .comment("Neutral ambient temperature, in Celsius, that Cold Sweat treats as its zero-delta point.",
                        "Matches defaultAmbientTemperature by default.")
                .defineInRange("temperatureOffset", 20.0, -Double.MAX_VALUE, Double.MAX_VALUE);

        COLDSWEAT_OUTPUT_SCALE = builder
                .comment("Scales the Celsius delta from temperatureOffset into the int strength WarmthTempModifier/",
                        "FrigidnessTempModifier expect. Cold Sweat's own Hearth block (the reference for what",
                        "'meaningfully warm' looks like) typically passes a strength of 0 or 1 - ThermalSourceTempModifier",
                        "further multiplies that by ConfigSettings.THERMAL_SOURCE_STRENGTH (0.75 by default) against a",
                        "MIN_TEMP/MAX_TEMP comfortable range that only spans about 1.2 units - so a strength of 1 is",
                        "already a strong effect. The default here keeps a 10C delta at roughly strength 1.")
                .defineInRange("outputScale", 0.1, 0.0, Double.MAX_VALUE);

        builder.pop();

        SPEC = builder.build();
    }

    private ThermalConfig() {
    }
}
