package com.marie.thermalsystems.climate;

import com.marie.thermalsystems.zone.ClimateZone;

/**
 * The outcome of advancing a single zone by one simulation step.
 */
public record ZoneAdvanceResult(ClimateZone zone, double totalHeatOutput) {
}
