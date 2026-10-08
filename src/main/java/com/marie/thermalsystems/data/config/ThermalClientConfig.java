package com.marie.thermalsystems.data.config;

import com.marie.thermalsystems.controller.TemperatureUnit;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-player preferences that only affect this client's own display, saved in
 * {@code thermalsystems-client.toml}. Unlike {@link ThermalConfig}, a server never sees or overrides
 * these. Only registered on the physical client.
 */
public final class ThermalClientConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<TemperatureUnit> TEMPERATURE_UNIT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("display");

        TEMPERATURE_UNIT = builder
                .comment("Unit temperatures are shown in on this client: CELSIUS or FAHRENHEIT.",
                        "Display only - zones and the main config always store Celsius.")
                .defineEnum("temperatureUnit", TemperatureUnit.CELSIUS);

        builder.pop();

        SPEC = builder.build();
    }

    private ThermalClientConfig() {
    }

    /** The chosen unit, or Celsius before the client config has loaded (and on a dedicated server, where it never does). */
    public static TemperatureUnit unit() {
        return SPEC.isLoaded() ? TEMPERATURE_UNIT.get() : TemperatureUnit.CELSIUS;
    }
}
