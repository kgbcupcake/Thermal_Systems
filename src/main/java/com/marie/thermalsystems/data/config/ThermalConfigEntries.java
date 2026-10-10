package com.marie.thermalsystems.data.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One ordered list describing every {@link ThermalConfig} value - its TOML section/key, type, and the
 * slider range the config GUI offers for it. Shared by the Hub config screen, server-side config sync
 * and {@code ThermalConfigIO}'s export/import, so a value added to {@link ThermalConfig} only needs
 * registering here once to show up in all three.
 *
 * <p>The GUI range is narrower than most spec ranges (which run to {@code Double.MAX_VALUE}); it only
 * bounds what a slider can pick. {@link Entry#write} clamps to the spec's own range, not the GUI's.
 */
public final class ThermalConfigEntries {

    public enum Kind { BOOL, INT, DOUBLE }

    /**
     * Whether a value is a Celsius temperature the GUI may show in another unit: an absolute
     * temperature, or a difference between two (converted without the zero-point offset).
     */
    public enum Temperature { NONE, ABSOLUTE, DELTA }

    public record Entry(String section, String key, ModConfigSpec.ConfigValue<?> value, Kind kind,
                        double guiMin, double guiMax, double step, int decimals, String unit,
                        Temperature temperature) {

        /** {@code section.key}, the id used on the wire and in snapshots. */
        public String path() {
            return section + "." + key;
        }

        /** The existing {@code config.thermalsystems.*} label key: bare key for the core sections, section-prefixed for integrations. */
        public String langKey() {
            if (section.equals("simulation") || section.equals("integration")) {
                return "config.thermalsystems." + key;
            }
            return "config.thermalsystems." + section + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        }

        public double defaultValue() {
            return toDouble(value.getDefault());
        }

        /** The live spec value, as a double (booleans as 0/1). */
        public double read() {
            return toDouble(value.get());
        }

        /** Clamps {@code raw} to the spec's own range and sets it. Does not save; call {@code ThermalConfig.SPEC.save()} after. */
        @SuppressWarnings("unchecked")
        public void write(double raw) {
            switch (kind) {
                case BOOL -> ((ModConfigSpec.ConfigValue<Boolean>) value).set(raw >= 0.5);
                case INT -> ((ModConfigSpec.ConfigValue<Integer>) value).set((int) Math.round(clampToSpec(raw)));
                case DOUBLE -> ((ModConfigSpec.ConfigValue<Double>) value).set(clampToSpec(raw));
            }
        }

        public double clampToSpec(double raw) {
            if (kind == Kind.BOOL) {
                return raw >= 0.5 ? 1 : 0;
            }
            if (Double.isNaN(raw) || Double.isInfinite(raw)) {
                return defaultValue();
            }
            ModConfigSpec.Range<?> range = value.getSpec().getRange();
            if (range == null) {
                return raw;
            }
            double min = ((Number) range.getMin()).doubleValue();
            double max = ((Number) range.getMax()).doubleValue();
            return Math.min(max, Math.max(min, raw));
        }

        private static double toDouble(Object v) {
            if (v instanceof Boolean b) {
                return b ? 1 : 0;
            }
            return ((Number) v).doubleValue();
        }
    }

    private static final String TICKS = "t";
    private static final String BLOCKS = "blocks";
    private static final String CELSIUS = "\u00B0C";
    private static final String KELVIN = "K";

    public static final List<Entry> ALL;
    private static final Map<String, Entry> BY_PATH;

    static {
        List<Entry> e = new ArrayList<>();

        bool(e, "simulation", "systemEnabled", ThermalConfig.SYSTEM_ENABLED);
        ticks(e, "simulation", "simulationTickInterval", ThermalConfig.SIMULATION_TICK_INTERVAL);
        dbl(e, "simulation", "heatTransferCoefficient", ThermalConfig.HEAT_TRANSFER_COEFFICIENT, 0, 1, 0.005, 3, "");
        dbl(e, "simulation", "temperatureConvergenceRate", ThermalConfig.TEMPERATURE_CONVERGENCE_RATE, 0, 1, 0.005, 3, "");
        celsius(e, "simulation", "minimumTemperature", ThermalConfig.MINIMUM_TEMPERATURE);
        celsius(e, "simulation", "maximumTemperature", ThermalConfig.MAXIMUM_TEMPERATURE);
        celsius(e, "simulation", "defaultTargetTemperature", ThermalConfig.DEFAULT_TARGET_TEMPERATURE);
        bool(e, "simulation", "loggingEnabled", ThermalConfig.LOGGING_ENABLED);
        bool(e, "simulation", "radiationLoggingEnabled", ThermalConfig.RADIATION_LOGGING_ENABLED);
        bool(e, "simulation", "bindingLoggingEnabled", ThermalConfig.BINDING_LOGGING_ENABLED);
        bool(e, "simulation", "radiationDebugEnabled", ThermalConfig.RADIATION_DEBUG_ENABLED);
        e.add(new Entry("simulation", "radiationChangeEpsilon", ThermalConfig.RADIATION_CHANGE_EPSILON, Kind.DOUBLE,
                0, 1, 0.01, 2, CELSIUS, Temperature.DELTA));

        ticks(e, "integration", "playerBridgeInterval", ThermalConfig.PLAYER_BRIDGE_INTERVAL);
        celsius(e, "integration", "defaultAmbientTemperature", ThermalConfig.DEFAULT_AMBIENT_TEMPERATURE);
        integer(e, "integration", "sourceBindingRadius", ThermalConfig.SOURCE_BINDING_RADIUS, 1, 64, BLOCKS);
        ticks(e, "integration", "sourceScanInterval", ThermalConfig.SOURCE_BINDING_SCAN_INTERVAL);
        integer(e, "integration", "sourceRadiationRadius", ThermalConfig.SOURCE_RADIATION_RADIUS, 1, 64, BLOCKS);
        ticks(e, "integration", "sourceRadiationInterval", ThermalConfig.SOURCE_RADIATION_INTERVAL);
        integer(e, "integration", "sourceTrackingReverifyInterval", ThermalConfig.SOURCE_TRACKING_REVERIFY_INTERVAL, 1, 6000, TICKS);
        bool(e, "integration", "hoverTooltipsEnabled", ThermalConfig.HOVER_TOOLTIPS_ENABLED);

        integer(e, "zones", "maxZoneVolume", ThermalConfig.MAX_ZONE_VOLUME, 64, 262144, BLOCKS);
        integer(e, "zones", "maxZonesPerPlayer", ThermalConfig.MAX_ZONES_PER_PLAYER, 0, 100, "");
        integer(e, "zones", "playerRoomReach", ThermalConfig.PLAYER_ROOM_REACH, 0, 32, BLOCKS);
        integer(e, "zones", "machineRoomReach", ThermalConfig.MACHINE_ROOM_REACH, 0, 32, BLOCKS);

        bool(e, "pneumaticcraft", "enabled", ThermalConfig.PNEUMATICCRAFT_ENABLED);
        kelvin(e, "pneumaticcraft", "referenceTemperatureKelvin", ThermalConfig.PNEUMATICCRAFT_REFERENCE_TEMPERATURE_KELVIN);
        dbl(e, "pneumaticcraft", "exchangerConversionCoefficient", ThermalConfig.PNEUMATICCRAFT_EXCHANGER_CONVERSION_COEFFICIENT, 0, 1, 0.005, 3, "");

        bool(e, "mekanism", "enabled", ThermalConfig.MEKANISM_ENABLED);
        kelvin(e, "mekanism", "referenceTemperatureKelvin", ThermalConfig.MEKANISM_REFERENCE_TEMPERATURE_KELVIN);
        dbl(e, "mekanism", "conversionCoefficient", ThermalConfig.MEKANISM_CONVERSION_COEFFICIENT, 0, 1, 0.005, 3, "");
        ticks(e, "mekanism", "networkRecomputeInterval", ThermalConfig.MEKANISM_NETWORK_RECOMPUTE_INTERVAL);

        bool(e, "enderio", "enabled", ThermalConfig.ENDERIO_ENABLED);
        bool(e, "enderio", "coolingMode", ThermalConfig.ENDERIO_COOLING_MODE);
        dbl(e, "enderio", "energyToHeatCoefficient", ThermalConfig.ENDERIO_ENERGY_TO_HEAT_COEFFICIENT, 0, 500, 1, 0, CELSIUS + "/s");
        dbl(e, "enderio", "outputMultiplier", ThermalConfig.ENDERIO_OUTPUT_MULTIPLIER, 0, 10, 0.05, 2, "x");
        ticks(e, "enderio", "networkRecomputeInterval", ThermalConfig.ENDERIO_NETWORK_RECOMPUTE_INTERVAL);

        bool(e, "lso", "enabled", ThermalConfig.LSO_ENABLED);
        celsius(e, "lso", "temperatureOffset", ThermalConfig.LSO_TEMPERATURE_OFFSET);

        bool(e, "coldsweat", "enabled", ThermalConfig.COLDSWEAT_ENABLED);
        celsius(e, "coldsweat", "temperatureOffset", ThermalConfig.COLDSWEAT_TEMPERATURE_OFFSET);
        dbl(e, "coldsweat", "outputScale", ThermalConfig.COLDSWEAT_OUTPUT_SCALE, 0, 1, 0.005, 3, "");

        bool(e, "toughasnails", "enabled", ThermalConfig.TOUGHASNAILS_ENABLED);
        threshold(e, "toughasnails", "heatOneStepThreshold", ThermalConfig.TOUGHASNAILS_HEAT_ONE_STEP_THRESHOLD);
        threshold(e, "toughasnails", "heatTwoStepThreshold", ThermalConfig.TOUGHASNAILS_HEAT_TWO_STEP_THRESHOLD);
        threshold(e, "toughasnails", "coolingOneStepThreshold", ThermalConfig.TOUGHASNAILS_COOLING_ONE_STEP_THRESHOLD);
        threshold(e, "toughasnails", "coolingTwoStepThreshold", ThermalConfig.TOUGHASNAILS_COOLING_TWO_STEP_THRESHOLD);

        bool(e, "eclipticseasons", "enabled", ThermalConfig.ECLIPTIC_SEASONS_ENABLED);
        bool(e, "sereneseasons", "enabled", ThermalConfig.SERENE_SEASONS_ENABLED);

        ALL = Collections.unmodifiableList(e);
        Map<String, Entry> byPath = new LinkedHashMap<>();
        for (Entry entry : e) {
            byPath.put(entry.path(), entry);
        }
        BY_PATH = Collections.unmodifiableMap(byPath);
    }

    private ThermalConfigEntries() {
    }

    /** The entry at {@code section.key}, or {@code null} if there is none. */
    public static Entry byPath(String path) {
        return BY_PATH.get(path);
    }

    /** The entry for {@code section}/{@code key}; throws if missing, so a typo fails loudly at screen build time. */
    public static Entry get(String section, String key) {
        Entry entry = BY_PATH.get(section + "." + key);
        if (entry == null) {
            throw new IllegalArgumentException("No Thermal config entry " + section + "." + key);
        }
        return entry;
    }

    /** Every entry in {@code section}, in registration order. */
    public static List<Entry> section(String section) {
        List<Entry> out = new ArrayList<>();
        for (Entry entry : ALL) {
            if (entry.section().equals(section)) {
                out.add(entry);
            }
        }
        return out;
    }

    private static void bool(List<Entry> e, String section, String key, ModConfigSpec.BooleanValue value) {
        e.add(new Entry(section, key, value, Kind.BOOL, 0, 1, 1, 0, "", Temperature.NONE));
    }

    private static void integer(List<Entry> e, String section, String key, ModConfigSpec.IntValue value, int min, int max, String unit) {
        e.add(new Entry(section, key, value, Kind.INT, min, max, 1, 0, unit, Temperature.NONE));
    }

    private static void ticks(List<Entry> e, String section, String key, ModConfigSpec.IntValue value) {
        integer(e, section, key, value, 1, 1200, TICKS);
    }

    private static void dbl(List<Entry> e, String section, String key, ModConfigSpec.DoubleValue value,
                            double min, double max, double step, int decimals, String unit) {
        e.add(new Entry(section, key, value, Kind.DOUBLE, min, max, step, decimals, unit, Temperature.NONE));
    }

    private static void celsius(List<Entry> e, String section, String key, ModConfigSpec.DoubleValue value) {
        e.add(new Entry(section, key, value, Kind.DOUBLE, -100, 100, 0.5, 1, CELSIUS, Temperature.ABSOLUTE));
    }

    private static void kelvin(List<Entry> e, String section, String key, ModConfigSpec.DoubleValue value) {
        dbl(e, section, key, value, 200, 400, 0.05, 2, KELVIN);
    }

    private static void threshold(List<Entry> e, String section, String key, ModConfigSpec.DoubleValue value) {
        e.add(new Entry(section, key, value, Kind.DOUBLE, 0, 50, 0.5, 1, CELSIUS, Temperature.DELTA));
    }
}
