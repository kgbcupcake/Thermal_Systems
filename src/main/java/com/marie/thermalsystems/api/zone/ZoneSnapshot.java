package com.marie.thermalsystems.api.zone;

import com.marie.thermalsystems.controller.ClimateMode;

import java.util.UUID;

/**
 * Immutable, read-only snapshot of a ClimateZone's state at the moment it
 * was queried. Returned by {@link com.marie.thermalsystems.api.ThermalSystemsAPI}
 * queries; never the live internal ClimateZone, so external mods cannot
 * mutate zone state by holding one.
 */
public record ZoneSnapshot(UUID id, String name, double currentTemp, double targetTemp, ClimateMode mode) {
}
