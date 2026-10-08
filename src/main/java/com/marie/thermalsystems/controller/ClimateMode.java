package com.marie.thermalsystems.controller;

/**
 * The operating mode of a climate zone, set from a thermostat. See {@link Thermostat#demand} for
 * how each mode decides whether the zone heats, cools, or idles.
 */
public enum ClimateMode {
    OFF,
    HEAT,
    COOL,
    AUTO;

    /** The next mode in thermostat-button order: OFF, HEAT, COOL, AUTO, then back to OFF. */
    public ClimateMode next() {
        ClimateMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** {@link #valueOf}, falling back to {@link #AUTO} for an unknown or missing name. */
    public static ClimateMode parse(String name) {
        for (ClimateMode mode : values()) {
            if (mode.name().equals(name)) {
                return mode;
            }
        }
        return AUTO;
    }
}
