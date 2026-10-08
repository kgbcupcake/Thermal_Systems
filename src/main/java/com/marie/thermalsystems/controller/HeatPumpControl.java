package com.marie.thermalsystems.controller;

import com.marie.thermalsystems.zone.ClimateZone;

import java.util.Optional;

/**
 * Decides which way a {@link com.marie.thermalsystems.api.climate.IHeatPump} runs: with a
 * controlling zone it does whatever that zone's thermostat is calling for (nothing at all once it's
 * at target or switched off); with none it runs in its integration's default direction.
 */
public final class HeatPumpControl {

    private HeatPumpControl() {
    }

    public static ClimateDemand direction(Optional<ClimateZone> controllingZone, boolean coolOutsideZones) {
        if (controllingZone.isPresent()) {
            return controllingZone.get().getDemand();
        }
        return coolOutsideZones ? ClimateDemand.COOLING : ClimateDemand.HEATING;
    }
}
