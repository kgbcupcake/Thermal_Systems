package com.marie.thermalsystems.controller;

/**
 * What a zone's thermostat is currently asking for, decided each simulation step by
 * {@link Thermostat#demand}. Drives which bound sources count toward the zone's temperature,
 * the wall thermostat's display, and its redstone output.
 */
public enum ClimateDemand {
    IDLE,
    HEATING,
    COOLING
}
