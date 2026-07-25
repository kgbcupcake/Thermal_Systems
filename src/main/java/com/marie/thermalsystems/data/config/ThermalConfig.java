package com.marie.thermalsystems.data.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge config spec exposing the tunable values for the climate simulation.
 */
public final class ThermalConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue SIMULATION_TICK_INTERVAL;
    public static final ModConfigSpec.DoubleValue HEAT_TRANSFER_COEFFICIENT;
    public static final ModConfigSpec.DoubleValue TEMPERATURE_CONVERGENCE_RATE;
    public static final ModConfigSpec.DoubleValue MINIMUM_TEMPERATURE;
    public static final ModConfigSpec.DoubleValue MAXIMUM_TEMPERATURE;
    public static final ModConfigSpec.BooleanValue LOGGING_ENABLED;

    public static final ModConfigSpec.IntValue STEAM_NETWORK_RECOMPUTE_INTERVAL;
    public static final ModConfigSpec.DoubleValue DEFAULT_BOILER_HEAT_OUTPUT;

    public static final ModConfigSpec.IntValue PLAYER_BRIDGE_INTERVAL;
    public static final ModConfigSpec.DoubleValue DEFAULT_AMBIENT_TEMPERATURE;

    public static final ModConfigSpec.DoubleValue PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN;
    public static final ModConfigSpec.DoubleValue PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT;

    public static final ModConfigSpec.DoubleValue MEKANISM_REFERENCE_TEMPERATURE_KELVIN;
    public static final ModConfigSpec.DoubleValue MEKANISM_CONVERSION_COEFFICIENT;

    public static final ModConfigSpec.DoubleValue ENDERIO_FLUID_TO_HEAT_COEFFICIENT;
    public static final ModConfigSpec.DoubleValue ENDERIO_ENERGY_TO_HEAT_COEFFICIENT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("simulation");

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

        LOGGING_ENABLED = builder
                .comment("Gates per-interval log output.")
                .define("loggingEnabled", true);

        builder.pop();

        builder.push("steam");

        STEAM_NETWORK_RECOMPUTE_INTERVAL = builder
                .comment("Ticks between dirty steam network recomputation.")
                .defineInRange("steamNetworkRecomputeInterval", 20, 1, Integer.MAX_VALUE);

        DEFAULT_BOILER_HEAT_OUTPUT = builder
                .comment("Heat output assigned to a newly placed Boiler before setOutput is used.")
                .defineInRange("defaultBoilerHeatOutput", 0.0, 0.0, Double.MAX_VALUE);

        builder.pop();

        builder.push("integration");

        PLAYER_BRIDGE_INTERVAL = builder
                .comment("Ticks between PlayerTemperatureBridgeHandler runs.")
                .defineInRange("playerBridgeInterval", 20, 1, Integer.MAX_VALUE);

        DEFAULT_AMBIENT_TEMPERATURE = builder
                .comment("Temperature reported to bridges for a player not inside any bounded zone.")
                .defineInRange("defaultAmbientTemperature", 20.0, -Double.MAX_VALUE, Double.MAX_VALUE);

        builder.pop();

        builder.push("pneumaticcraft");

        PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN = builder
                .comment("Baseline temperature, in Kelvin, for Thermal Exchanger conversion.")
                .defineInRange("referenceTemperatureKelvin", 293.15, 0.0, Double.MAX_VALUE);

        PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT = builder
                .comment("Scales the Kelvin difference from referenceTemperatureKelvin into Thermal Systems heat/cooling output.")
                .defineInRange("exchangerConversionCoefficient", 0.05, 0.0, Double.MAX_VALUE);

        builder.pop();

        builder.push("mekanism");

        MEKANISM_REFERENCE_TEMPERATURE_KELVIN = builder
                .comment("Baseline temperature, in Kelvin, for Mekanism Heat Exchanger conversion. Matches Mekanism's own ambient temperature (HeatAPI.AMBIENT_TEMP) by default.")
                .defineInRange("referenceTemperatureKelvin", 300.0, 0.0, Double.MAX_VALUE);

        MEKANISM_CONVERSION_COEFFICIENT = builder
                .comment("Scales the Kelvin difference from referenceTemperatureKelvin into Thermal Systems heat/cooling output.")
                .defineInRange("conversionCoefficient", 0.05, 0.0, Double.MAX_VALUE);

        builder.pop();

        builder.push("enderio");

        ENDERIO_FLUID_TO_HEAT_COEFFICIENT = builder
                .comment("Scales millibuckets of thermalsystems:steam moved through an Ender IO Fluid Conduit into Thermal Systems heat output.")
                .defineInRange("fluidToHeatCoefficient", 0.01, 0.0, Double.MAX_VALUE);

        ENDERIO_ENERGY_TO_HEAT_COEFFICIENT = builder
                .comment("Scales FE received per tick through an Ender IO Energy Conduit into Thermal Systems heat output.")
                .defineInRange("energyToHeatCoefficient", 0.001, 0.0, Double.MAX_VALUE);

        builder.pop();

        SPEC = builder.build();
    }

    private ThermalConfig() {
    }
}
