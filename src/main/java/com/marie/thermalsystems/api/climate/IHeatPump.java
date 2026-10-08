package com.marie.thermalsystems.api.climate;

import com.marie.thermalsystems.api.zone.IClimateZone;

import java.util.Optional;

/**
 * Marks a heat/cooling source that can run either way - an electric heat pump rather than a fire.
 * Implement it alongside {@link com.marie.thermalsystems.api.heating.IHeatSource} and
 * {@link com.marie.thermalsystems.api.cooling.ICoolingSource}.
 *
 * <p>A heat pump controlled by a climate zone follows that zone's thermostat: it heats while the
 * zone calls for heat, cools while it calls for cooling, and stands by otherwise, so its
 * {@code getHeatOutput()}/{@code getCoolingOutput()} report only the direction it's currently
 * running in (see {@code HeatPumpControl}). With no controlling zone it falls back to whatever
 * default direction its integration configures.
 */
public interface IHeatPump {

    /** What it delivers when running, in degrees Celsius per simulation second, in either direction. */
    double getPumpOutput();

    /**
     * The zone whose thermostat it follows - normally the zone containing it, but a pump spread
     * over a network may follow the zone the rest of its network is in. Empty when no zone
     * controls it.
     */
    default Optional<? extends IClimateZone> getControllingZone() {
        return Optional.empty();
    }
}
